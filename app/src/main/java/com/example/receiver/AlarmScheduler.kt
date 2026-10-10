package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

object AlarmScheduler {
    fun scheduleAlarms(context: Context, morningTime: String, morningEnabled: Boolean, eveningTime: String, eveningEnabled: Boolean, nightTime: String, nightEnabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        scheduleAlarm(context, alarmManager, morningTime, morningEnabled, 1)
        scheduleAlarm(context, alarmManager, eveningTime, eveningEnabled, 2)
        scheduleAlarm(context, alarmManager, nightTime, nightEnabled, 3)
    }

    fun scheduleAlarm(context: Context, alarmManager: AlarmManager, timeString: String, enabled: Boolean, requestCode: Int) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("REQUEST_CODE", requestCode)
                putExtra("TIME_STRING", timeString)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!enabled) {
                alarmManager.cancel(pendingIntent)
                return
            }

            val formatter = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US)
            val localTime = LocalTime.parse(timeString.uppercase(java.util.Locale.US), formatter)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, localTime.hour)
                set(Calendar.MINUTE, localTime.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // If permission is denied, fallback to inexact
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
