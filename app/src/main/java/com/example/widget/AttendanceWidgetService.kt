package com.example.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceRecord
import com.example.data.Subject
import com.example.data.TimetableSlot
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AttendanceWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AttendanceWidgetFactory(this.applicationContext, intent)
    }
}

class AttendanceWidgetFactory(private val context: Context, private val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    
    private var slots: List<TimetableSlot> = emptyList()
    private var records: List<AttendanceRecord> = emptyList()
    private var subjects: List<Subject> = emptyList()
    private var extraRecords: List<AttendanceRecord> = emptyList()
    private var dateStr: String = ""

    override fun onCreate() {
        // Setup done in onDataSetChanged
    }

    override fun onDataSetChanged() {
        // Fetch DB lists synchronously
        val date = LocalDate.now()
        val dayOfWeek = date.dayOfWeek.value
        dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val db = AttendanceDatabase.getDatabase(context)
        val dao = db.attendanceDao()

        runBlocking {
            subjects = dao.getAllSubjectsList()
            slots = dao.getAllTimetableSlotsList().filter { it.dayOfWeek == dayOfWeek }
            
            val allRecords = dao.getAttendanceRecordsForDate(dateStr)
            records = allRecords.filter { !it.isExtra }
            extraRecords = allRecords.filter { it.isExtra }
        }
    }

    override fun onDestroy() {
        slots = emptyList()
        records = emptyList()
        subjects = emptyList()
        extraRecords = emptyList()
    }

    override fun getCount(): Int {
        return slots.size + extraRecords.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= count) return null

        val isExtra = position >= slots.size
        
        if (!isExtra) {
            val slot = slots[position]
            val subject = subjects.find { it.id == slot.subjectId } ?: return null
            val record = records.find { it.subjectId == subject.id && it.slotId == slot.id }

            val itemViews = RemoteViews(context.packageName, R.layout.widget_timetable_item)
            itemViews.setTextViewText(R.id.widget_item_subject, subject.name)
            itemViews.setTextViewText(R.id.widget_item_period, slot.periodName)
            
            if (record?.status == "ATTENDED") {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_att)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.GONE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                itemViews.setTextViewText(R.id.widget_status_text, "Attended")
                itemViews.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor("#4CAF50"))
            } else if (record?.status == "MISSED") {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_miss)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.GONE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                itemViews.setTextViewText(R.id.widget_status_text, "Missed")
                itemViews.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor("#E91E63"))
            } else if (record?.status == "OFF") {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_off)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.GONE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                itemViews.setTextViewText(R.id.widget_status_text, "Off / Leave")
                itemViews.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor("#FFB300"))
            } else {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.VISIBLE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.GONE)
            }

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

            val startLocal = parseTime(slot.startTime)
            val endLocal = parseTime(slot.endTime)

            if (startLocal != null && endLocal != null && !now.isBefore(startLocal) && now.isBefore(endLocal)) {
                val diffSec = Duration.between(now, endLocal).seconds
                val base = SystemClock.elapsedRealtime() + (diffSec * 1000)
                itemViews.setViewVisibility(R.id.widget_item_countdown, View.VISIBLE)
                itemViews.setChronometer(R.id.widget_item_countdown, base, "Ends in %s", true)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    itemViews.setChronometerCountDown(R.id.widget_item_countdown, true)
                }
            } else {
                itemViews.setViewVisibility(R.id.widget_item_countdown, View.GONE)
            }

            // Click listeners using FillInIntent
            val missFillInIntent = Intent().apply {
                putExtra(AttendanceWidgetProvider.EXTRA_SUBJECT_ID, subject.id)
                putExtra(AttendanceWidgetProvider.EXTRA_SLOT_ID, slot.id)
                putExtra(AttendanceWidgetProvider.EXTRA_STATUS, "MISSED")
                putExtra(AttendanceWidgetProvider.EXTRA_DATE_STRING, dateStr)
                putExtra(AttendanceWidgetProvider.EXTRA_IS_EXTRA, false)
            }
            itemViews.setOnClickFillInIntent(R.id.widget_btn_miss, missFillInIntent)

            val offFillInIntent = Intent().apply {
                putExtra(AttendanceWidgetProvider.EXTRA_SUBJECT_ID, subject.id)
                putExtra(AttendanceWidgetProvider.EXTRA_SLOT_ID, slot.id)
                putExtra(AttendanceWidgetProvider.EXTRA_STATUS, "OFF")
                putExtra(AttendanceWidgetProvider.EXTRA_DATE_STRING, dateStr)
                putExtra(AttendanceWidgetProvider.EXTRA_IS_EXTRA, false)
            }
            itemViews.setOnClickFillInIntent(R.id.widget_btn_off, offFillInIntent)

            val attFillInIntent = Intent().apply {
                putExtra(AttendanceWidgetProvider.EXTRA_SUBJECT_ID, subject.id)
                putExtra(AttendanceWidgetProvider.EXTRA_SLOT_ID, slot.id)
                putExtra(AttendanceWidgetProvider.EXTRA_STATUS, "ATTENDED")
                putExtra(AttendanceWidgetProvider.EXTRA_DATE_STRING, dateStr)
                putExtra(AttendanceWidgetProvider.EXTRA_IS_EXTRA, false)
            }
            itemViews.setOnClickFillInIntent(R.id.widget_btn_att, attFillInIntent)

            return itemViews

        } else {
            val extRec = extraRecords[position - slots.size]
            val subject = subjects.find { it.id == extRec.subjectId } ?: return null

            val itemViews = RemoteViews(context.packageName, R.layout.widget_timetable_item)
            itemViews.setTextViewText(R.id.widget_item_subject, "${subject.name} (Extra)")
            itemViews.setTextViewText(R.id.widget_item_period, "Extra Class Hours Today")
            
            if (extRec.status == "ATTENDED") {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_att)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.GONE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                itemViews.setTextViewText(R.id.widget_status_text, "+1 Attended")
                itemViews.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor("#4CAF50"))
            } else if (extRec.status == "MISSED") {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_miss)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.GONE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                itemViews.setTextViewText(R.id.widget_status_text, "+1 Bunked")
                itemViews.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor("#E91E63"))
            } else if (extRec.status == "OFF") {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg_off)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.GONE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.VISIBLE)
                itemViews.setTextViewText(R.id.widget_status_text, "Off / Leave")
                itemViews.setTextColor(R.id.widget_status_text, android.graphics.Color.parseColor("#FFB300"))
            } else {
                itemViews.setInt(R.id.widget_item_container, "setBackgroundResource", R.drawable.widget_item_bg)
                itemViews.setViewVisibility(R.id.widget_actions_container, View.VISIBLE)
                itemViews.setViewVisibility(R.id.widget_status_text, View.GONE)
            }

            itemViews.setViewVisibility(R.id.widget_item_countdown, View.GONE)

            val missFillInIntent = Intent().apply {
                putExtra(AttendanceWidgetProvider.EXTRA_SUBJECT_ID, subject.id)
                putExtra(AttendanceWidgetProvider.EXTRA_STATUS, "MISSED")
                putExtra(AttendanceWidgetProvider.EXTRA_DATE_STRING, dateStr)
                putExtra(AttendanceWidgetProvider.EXTRA_IS_EXTRA, true)
            }
            itemViews.setOnClickFillInIntent(R.id.widget_btn_miss, missFillInIntent)

            val offFillInIntent = Intent().apply {
                putExtra(AttendanceWidgetProvider.EXTRA_SUBJECT_ID, subject.id)
                putExtra(AttendanceWidgetProvider.EXTRA_STATUS, "OFF")
                putExtra(AttendanceWidgetProvider.EXTRA_DATE_STRING, dateStr)
                putExtra(AttendanceWidgetProvider.EXTRA_IS_EXTRA, true)
            }
            itemViews.setOnClickFillInIntent(R.id.widget_btn_off, offFillInIntent)

            val attFillInIntent = Intent().apply {
                putExtra(AttendanceWidgetProvider.EXTRA_SUBJECT_ID, subject.id)
                putExtra(AttendanceWidgetProvider.EXTRA_STATUS, "ATTENDED")
                putExtra(AttendanceWidgetProvider.EXTRA_DATE_STRING, dateStr)
                putExtra(AttendanceWidgetProvider.EXTRA_IS_EXTRA, true)
            }
            itemViews.setOnClickFillInIntent(R.id.widget_btn_att, attFillInIntent)

            return itemViews
        }
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
