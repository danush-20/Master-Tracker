package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceRecord
import com.example.data.Subject
import com.example.data.TimetableSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_MARK_INDIVIDUAL = "com.example.widget.ACTION_MARK_INDIVIDUAL"
        const val ACTION_CLEAR_INDIVIDUAL = "com.example.widget.ACTION_CLEAR_INDIVIDUAL"
        const val ACTION_WIDGET_ADD_EXTRA = "com.example.widget.ACTION_WIDGET_ADD_EXTRA"
        const val ACTION_WIDGET_REFRESH = "com.example.widget.ACTION_WIDGET_REFRESH"

        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SLOT_ID = "extra_slot_id"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_DATE_STRING = "extra_date_string"
        const val EXTRA_IS_EXTRA = "extra_is_extra"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { widgetId ->
                buildWidgetUpdate(context, appWidgetManager, widgetId)
            }
            pendingResult.finish()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        
        if (action == ACTION_MARK_INDIVIDUAL || action == ACTION_CLEAR_INDIVIDUAL || action == ACTION_WIDGET_ADD_EXTRA || action == ACTION_WIDGET_REFRESH) {
            val pendingResult = goAsync()
            val db = AttendanceDatabase.getDatabase(context)
            val dao = db.attendanceDao()
            
            if (action == ACTION_WIDGET_REFRESH) {
                CoroutineScope(Dispatchers.IO).launch {
                    val manager = AppWidgetManager.getInstance(context)
                    val cmpName = ComponentName(context, AttendanceWidgetProvider::class.java)
                    val widgetIds = manager.getAppWidgetIds(cmpName)
                    widgetIds.forEach { widgetId ->
                        buildWidgetUpdate(context, manager, widgetId)
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Refreshed widget", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    pendingResult.finish()
                }
                return
            }

            val subjectId = intent.getIntExtra(EXTRA_SUBJECT_ID, -1)
            val slotId = intent.getIntExtra(EXTRA_SLOT_ID, -1)
            val status = intent.getStringExtra(EXTRA_STATUS) ?: "ATTENDED"
            val dateStr = intent.getStringExtra(EXTRA_DATE_STRING) ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isExtra = intent.getBooleanExtra(EXTRA_IS_EXTRA, false)

            if (subjectId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    val resolvedSlotId = if (slotId == -1) null else slotId

                    if (action == ACTION_CLEAR_INDIVIDUAL) {
                        dao.deleteAttendanceRecord(dateStr, subjectId, resolvedSlotId, isExtra)
                    } else {
                        val record = AttendanceRecord(
                            subjectId = subjectId,
                            dateString = dateStr,
                            status = status,
                            slotId = resolvedSlotId,
                            isExtra = isExtra
                        )
                        dao.insertOrUpdateAttendanceRecord(record)
                    }

                    // Trigger manual refresh for all active widget views
                    val manager = AppWidgetManager.getInstance(context)
                    val cmpName = ComponentName(context, AttendanceWidgetProvider::class.java)
                    val widgetIds = manager.getAppWidgetIds(cmpName)
                    widgetIds.forEach { widgetId ->
                        buildWidgetUpdate(context, manager, widgetId)
                    }
                    val currIntent = Intent(context, CurrentClassWidgetProvider::class.java).apply {
                        this.action = CurrentClassWidgetProvider.ACTION_WIDGET_REFRESH
                    }
                    context.sendBroadcast(currIntent)
                    pendingResult.finish()
                }
            } else {
                pendingResult.finish()
            }
        }
    }

    private suspend fun buildWidgetUpdate(context: Context, widgetManager: AppWidgetManager, widgetId: Int) {
        val date = LocalDate.now()
        val dayOfWeek = date.dayOfWeek.value
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val formattedDateText = date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

        val db = AttendanceDatabase.getDatabase(context)
        val dao = db.attendanceDao()

        // Fetch DB lists
        val slots = dao.getAllTimetableSlotsList().filter { it.dayOfWeek == dayOfWeek }
        val records = dao.getAttendanceRecordsForDate(dateStr)

        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setTextViewText(R.id.widget_date_title, formattedDateText)

        val attendedCount = records.count { it.status == "ATTENDED" }
        val missedCount = records.count { it.status == "MISSED" }
        val offCount = records.count { it.status == "OFF" }

        val summaryText = if (slots.isEmpty() && records.isEmpty()) {
            "No classes scheduled today"
        } else if (offCount > 0 && attendedCount == 0 && missedCount == 0 && (slots.isEmpty() || offCount >= slots.size)) {
            "Day Off / Leave • All $offCount classes off"
        } else {
            "Attended: $attendedCount • Missed: $missedCount • Off: $offCount"
        }
        views.setTextViewText(R.id.widget_summary_text, summaryText)

        // Click pending intent to launch main app
        val appIntent = Intent(context, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            context, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title_layout, pendingAppIntent)
        
        // Refresh Button click intent
        val refreshIntent = Intent(context, AttendanceWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_REFRESH
        }
        val pendingRefreshIntent = PendingIntent.getBroadcast(
            context, 1001, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingRefreshIntent)

        if (slots.isEmpty() && records.isEmpty()) {
            // Show empty view
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
            views.setViewVisibility(R.id.widget_list_view, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_empty_text, View.GONE)
            views.setViewVisibility(R.id.widget_list_view, View.VISIBLE)

            val serviceIntent = Intent(context, AttendanceWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)

            val templateIntent = Intent(context, AttendanceWidgetProvider::class.java).apply {
                action = ACTION_MARK_INDIVIDUAL
            }
            val pendingTemplateIntent = PendingIntent.getBroadcast(
                context, 0, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, pendingTemplateIntent)
        }

        widgetManager.updateAppWidget(widgetId, views)
        widgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view)
    }
}
