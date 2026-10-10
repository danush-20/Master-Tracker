package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.AttendanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CurrentClassWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.example.widget.ACTION_CURRENT_CLASS_REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, CurrentClassWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (widgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_class)

        // Refresh action
        val refreshIntent = Intent(context, CurrentClassWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_REFRESH
        }
        val pendingRefreshIntent = PendingIntent.getBroadcast(
            context, widgetId, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_curr_btn_refresh, pendingRefreshIntent)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AttendanceDatabase.getDatabase(context)
            val dao = db.attendanceDao()
            val subjects = dao.getAllSubjectsList()
            
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek.value
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val slots = dao.getAllTimetableSlotsList().filter { it.dayOfWeek == dayOfWeek }
            val records = dao.getAttendanceRecordsForDate(todayStr)

            val now = LocalTime.now()

            val parseTime: (String) -> LocalTime? = { timeStr ->
                val upper = timeStr.uppercase(Locale.US).trim()
                var res: LocalTime? = null
                val patterns = listOf("hh:mm a", "h:mm a", "HH:mm", "hh:mma", "h:mma")
                for (p in patterns) {
                    try {
                        val f = DateTimeFormatter.ofPattern(p, Locale.US)
                        res = LocalTime.parse(upper, f)
                        break
                    } catch (e: Exception) {}
                }
                res
            }

            var currentSlot: com.example.data.TimetableSlot? = null
            var nextSlot: com.example.data.TimetableSlot? = null
            var isOngoing = false

            // Find ongoing or next class
            val sortedSlots = slots.mapNotNull { slot ->
                val st = parseTime(slot.startTime)
                val et = parseTime(slot.endTime)
                if (st != null && et != null) Triple(slot, st, et) else null
            }.sortedBy { it.second }

            for (triple in sortedSlots) {
                val (slot, st, et) = triple
                if (!now.isBefore(st) && now.isBefore(et)) {
                    currentSlot = slot
                    isOngoing = true
                    break
                }
                if (now.isBefore(st)) {
                    if (nextSlot == null) {
                        nextSlot = slot
                    }
                }
            }

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                val allOff = slots.isNotEmpty() && slots.all { slot ->
                    records.any { it.slotId == slot.id && it.status == "OFF" }
                }

                if (allOff) {
                    views.setTextViewText(R.id.widget_curr_subject, "Day Off / Leave")
                    views.setViewVisibility(R.id.widget_curr_status, View.VISIBLE)
                    views.setTextViewText(R.id.widget_curr_status, "All classes cancelled today")
                    views.setTextColor(R.id.widget_curr_status, android.graphics.Color.parseColor("#FFB300"))
                    views.setViewVisibility(R.id.widget_curr_countdown, View.GONE)
                } else if (currentSlot != null && isOngoing) {
                    val subject = subjects.find { it.id == currentSlot!!.subjectId }
                    views.setTextViewText(R.id.widget_curr_subject, subject?.name ?: "Current Class")

                    val currentRec = records.find { it.slotId == currentSlot!!.id && !it.isExtra }
                    if (currentRec != null) {
                        views.setViewVisibility(R.id.widget_curr_status, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_curr_countdown, View.GONE)
                        when (currentRec.status) {
                            "OFF" -> {
                                views.setTextViewText(R.id.widget_curr_status, "Class Off / Cancelled")
                                views.setTextColor(R.id.widget_curr_status, android.graphics.Color.parseColor("#FFB300"))
                            }
                            "MISSED" -> {
                                views.setTextViewText(R.id.widget_curr_status, "Missed / Bunked")
                                views.setTextColor(R.id.widget_curr_status, android.graphics.Color.parseColor("#E91E63"))
                            }
                            "ATTENDED" -> {
                                views.setTextViewText(R.id.widget_curr_status, "Attended ✓")
                                views.setTextColor(R.id.widget_curr_status, android.graphics.Color.parseColor("#4CAF50"))
                            }
                            else -> {
                                views.setTextViewText(R.id.widget_curr_status, currentRec.status)
                                views.setTextColor(R.id.widget_curr_status, android.graphics.Color.WHITE)
                            }
                        }
                    } else {
                        val et = parseTime(currentSlot!!.endTime)
                        views.setViewVisibility(R.id.widget_curr_status, View.GONE)
                        views.setViewVisibility(R.id.widget_curr_countdown, View.VISIBLE)
                        
                        if (et != null) {
                            val diffSec = Duration.between(now, et).seconds
                            val base = SystemClock.elapsedRealtime() + (diffSec * 1000)
                            views.setChronometer(R.id.widget_curr_countdown, base, "Ends in %s", true)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                views.setChronometerCountDown(R.id.widget_curr_countdown, true)
                            }
                        }
                    }
                } else if (nextSlot != null) {
                    val subject = subjects.find { it.id == nextSlot!!.subjectId }
                    val nextRec = records.find { it.slotId == nextSlot!!.id && !it.isExtra }
                    views.setTextViewText(R.id.widget_curr_subject, subject?.name ?: "Next Class")

                    if (nextRec?.status == "OFF") {
                        views.setViewVisibility(R.id.widget_curr_status, View.VISIBLE)
                        views.setTextViewText(R.id.widget_curr_status, "Next class is Off")
                        views.setTextColor(R.id.widget_curr_status, android.graphics.Color.parseColor("#FFB300"))
                        views.setViewVisibility(R.id.widget_curr_countdown, View.GONE)
                    } else {
                        val st = parseTime(nextSlot!!.startTime)
                        views.setViewVisibility(R.id.widget_curr_status, View.GONE)
                        views.setViewVisibility(R.id.widget_curr_countdown, View.VISIBLE)

                        if (st != null) {
                            val diffSec = Duration.between(now, st).seconds
                            val base = SystemClock.elapsedRealtime() + (diffSec * 1000)
                            views.setChronometer(R.id.widget_curr_countdown, base, "Starts in %s", true)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                views.setChronometerCountDown(R.id.widget_curr_countdown, true)
                            }
                        }
                    }
                } else {
                    views.setTextViewText(R.id.widget_curr_subject, "No Classes")
                    views.setViewVisibility(R.id.widget_curr_status, View.VISIBLE)
                    views.setTextViewText(R.id.widget_curr_status, "All classes over")
                    views.setTextColor(R.id.widget_curr_status, android.graphics.Color.parseColor("#4CAF50"))
                    views.setViewVisibility(R.id.widget_curr_countdown, View.GONE)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
