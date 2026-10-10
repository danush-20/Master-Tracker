package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, TimetableSlot::class, AttendanceRecord::class, AppSetting::class, Task::class, CustomPeriod::class],
    version = 5,
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE subjects ADD COLUMN baseAttended INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE subjects ADD COLUMN baseMissed INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_database"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
