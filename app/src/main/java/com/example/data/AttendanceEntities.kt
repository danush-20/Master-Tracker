package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val baseAttended: Int = 0,
    val baseMissed: Int = 0
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: Int, // 1 = Mon, 2 = Tue, 3 = Wed, 4 = Thu, 5 = Fri, 6 = Sat
    val subjectId: Int,
    val startTime: String = "09:00 AM", // "hh:mm a"
    val periodName: String = "Period", // e.g. "Period 1"
    val endTime: String = "10:00 AM"     // "hh:mm a"
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("subjectId"),
        Index(value = ["dateString", "subjectId", "slotId", "isExtra"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val dateString: String, // "YYYY-MM-DD"
    val status: String, // "ATTENDED", "MISSED", "OFF"
    val slotId: Int? = null, // Link to TimetableSlot.id, null for extra classes
    val isExtra: Boolean = false,
    val extraPeriodName: String? = null,
    val extraStartTime: String? = null,
    val extraEndTime: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "custom_periods")
data class CustomPeriod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startTime: String, // "hh:mm a"
    val endTime: String    // "hh:mm a"
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subjectId: Int? = null, // null means "General"
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val fuzzyPeriod: String? = null, // e.g. "Today Night", "Tomorrow Morning", "Tomorrow Evening"
    val note: String = "",
    val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "ca_marks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId", unique = true)]
)
data class CaMark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val ca1Marks: Double? = null,
    val ca1Total: Double? = null,
    val ca2Marks: Double? = null,
    val ca2Total: Double? = null
)

@JsonClass(generateAdapter = true)
data class ExportData(
    val subjects: List<Subject>,
    val timetableSlots: List<TimetableSlot>,
    val attendanceRecords: List<AttendanceRecord>,
    val settings: List<AppSetting>,
    val tasks: List<Task> = emptyList(),
    val customPeriods: List<CustomPeriod> = emptyList(),
    val caMarks: List<CaMark> = emptyList()
)


