package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AttendanceDatabase.getDatabase(context)
                    val repository = AttendanceRepository(db.attendanceDao())
                    
                    val mTime = repository.getSetting("interval_morning", "08:00 AM")
                    val mEnabled = repository.getSetting("interval_morning_enabled", "true") == "true"
                    val eTime = repository.getSetting("interval_evening", "05:00 PM")
                    val eEnabled = repository.getSetting("interval_evening_enabled", "true") == "true"
                    val nTime = repository.getSetting("interval_night", "09:00 PM")
                    val nEnabled = repository.getSetting("interval_night_enabled", "true") == "true"
                    
                    AlarmScheduler.scheduleAlarms(context, mTime, mEnabled, eTime, eEnabled, nTime, nEnabled)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
