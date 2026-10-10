package com.example.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "attendance_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Attendance Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to mark your attendance"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val requestCode = intent.getIntExtra("REQUEST_CODE", -1)
        val timeString = intent.getStringExtra("TIME_STRING")

        val messageText = when (requestCode) {
            1 -> "Reminder to complete morning work's"
            2 -> "Reminder to complete evening work's"
            3 -> "Reminder to complete night work's"
            else -> "Reminder to complete your work's"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentTitle("Master Tracker")
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }

        // Reschedule for the next day
        if (requestCode != -1 && timeString != null) {
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val db = com.example.data.AttendanceDatabase.getDatabase(context)
                    val dao = db.attendanceDao()
                    val enabledStr = when (requestCode) {
                        1 -> dao.getSettingValue("interval_morning_enabled") ?: "true"
                        2 -> dao.getSettingValue("interval_evening_enabled") ?: "true"
                        3 -> dao.getSettingValue("interval_night_enabled") ?: "true"
                        else -> "true"
                    }
                    val enabled = enabledStr == "true"
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    AlarmScheduler.scheduleAlarm(context, alarmManager, timeString, enabled, requestCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
