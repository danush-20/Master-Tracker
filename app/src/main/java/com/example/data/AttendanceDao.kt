package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsList(): List<Subject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllTimetableSlots(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots")
    suspend fun getAllTimetableSlotsList(): List<TimetableSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlot): Long

    @Update
    suspend fun updateTimetableSlot(slot: TimetableSlot)

    @Delete
    suspend fun deleteTimetableSlot(slot: TimetableSlot)

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceRecordsList(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString")
    suspend fun getAttendanceRecordsForDate(dateString: String): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendanceRecord(record: AttendanceRecord): Long

    @Query("DELETE FROM attendance_records WHERE dateString = :dateString AND subjectId = :subjectId AND slotId = :slotId AND isExtra = :isExtra")
    suspend fun deleteAttendanceRecord(dateString: String, subjectId: Int, slotId: Int?, isExtra: Boolean)

    @Query("DELETE FROM attendance_records WHERE id = :recordId")
    suspend fun deleteAttendanceRecordById(recordId: Int)

    @Query("UPDATE attendance_records SET status = :status WHERE id = :recordId")
    suspend fun updateAttendanceRecordStatus(recordId: Int, status: String)

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Delete
    suspend fun deleteTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    // Custom Periods
    @Query("SELECT * FROM custom_periods ORDER BY id ASC")
    fun getAllCustomPeriods(): Flow<List<CustomPeriod>>

    @Query("SELECT * FROM custom_periods")
    suspend fun getAllCustomPeriodsList(): List<CustomPeriod>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPeriod(period: CustomPeriod): Long

    @Update
    suspend fun updateCustomPeriod(period: CustomPeriod)

    @Query("SELECT * FROM custom_periods WHERE id = :id")
    suspend fun getCustomPeriodById(id: Int): CustomPeriod?

    @Query("UPDATE timetable_slots SET periodName = :newName, startTime = :newStartTime, endTime = :newEndTime WHERE periodName = :oldName")
    suspend fun updateTimetableSlotTimes(oldName: String, newName: String, newStartTime: String, newEndTime: String)

    @Query("UPDATE attendance_records SET extraPeriodName = :newName, extraStartTime = :newStartTime, extraEndTime = :newEndTime WHERE extraPeriodName = :oldName AND isExtra = 1")
    suspend fun updateExtraAttendanceTimes(oldName: String, newName: String, newStartTime: String, newEndTime: String)

    @Delete
    suspend fun deleteCustomPeriod(period: CustomPeriod)

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("DELETE FROM timetable_slots")
    suspend fun clearTimetableSlots()

    @Query("DELETE FROM attendance_records")
    suspend fun clearAttendanceRecords()

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM custom_periods")
    suspend fun clearCustomPeriods()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlots(slots: List<TimetableSlot>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSetting>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPeriods(periods: List<CustomPeriod>)

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsList(): List<AppSetting>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<Task>

    // CA Marks queries
    @Query("SELECT * FROM ca_marks ORDER BY id ASC")
    fun getAllCaMarks(): Flow<List<CaMark>>

    @Query("SELECT * FROM ca_marks")
    suspend fun getAllCaMarksList(): List<CaMark>

    @Query("SELECT * FROM ca_marks WHERE subjectName = :name LIMIT 1")
    suspend fun getCaMarkBySubjectName(name: String): CaMark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCaMark(caMark: CaMark): Long

    @Query("DELETE FROM ca_marks WHERE id = :id")
    suspend fun deleteCaMarkById(id: Int)

    @Query("DELETE FROM ca_marks WHERE subjectName = :name")
    suspend fun deleteCaMarkBySubjectName(name: String)

    @Query("DELETE FROM ca_marks")
    suspend fun clearCaMarks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaMarks(marks: List<CaMark>)

    // Semester Courses queries (Semesters 1-4)
    @Query("SELECT * FROM semester_courses ORDER BY semester ASC, id ASC")
    fun getAllSemesterCourses(): Flow<List<SemesterCourse>>

    @Query("SELECT * FROM semester_courses WHERE semester = :semester ORDER BY id ASC")
    fun getSemesterCourses(semester: Int): Flow<List<SemesterCourse>>

    @Query("SELECT * FROM semester_courses")
    suspend fun getAllSemesterCoursesList(): List<SemesterCourse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesterCourse(course: SemesterCourse): Long

    @Update
    suspend fun updateSemesterCourse(course: SemesterCourse)

    @Delete
    suspend fun deleteSemesterCourse(course: SemesterCourse)

    @Query("DELETE FROM semester_courses WHERE id = :id")
    suspend fun deleteSemesterCourseById(id: Int)

    @Query("DELETE FROM semester_courses WHERE semester = :semester")
    suspend fun deleteSemesterCoursesBySemester(semester: Int)

    @Query("DELETE FROM semester_courses")
    suspend fun clearSemesterCourses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesterCourses(courses: List<SemesterCourse>)
}
