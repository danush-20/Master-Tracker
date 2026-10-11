package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceDao) {

    val allSubjects: Flow<List<Subject>> = dao.getAllSubjects()
    val allTimetableSlots: Flow<List<TimetableSlot>> = dao.getAllTimetableSlots()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = dao.getAllAttendanceRecords()
    val allSettings: Flow<List<AppSetting>> = dao.getAllSettings()
    val allTasks: Flow<List<Task>> = dao.getAllTasks()
    val allCustomPeriods: Flow<List<CustomPeriod>> = dao.getAllCustomPeriods()
    val allCaMarks: Flow<List<CaMark>> = dao.getAllCaMarks()
    val allSemesterCourses: Flow<List<SemesterCourse>> = dao.getAllSemesterCourses()

    suspend fun getAllSubjectsList(): List<Subject> = dao.getAllSubjectsList()
    suspend fun getAllTimetableSlotsList(): List<TimetableSlot> = dao.getAllTimetableSlotsList()
    suspend fun getAllAttendanceRecordsList(): List<AttendanceRecord> = dao.getAllAttendanceRecordsList()
    suspend fun getAllCustomPeriodsList(): List<CustomPeriod> = dao.getAllCustomPeriodsList()
    suspend fun getAllCaMarksList(): List<CaMark> = dao.getAllCaMarksList()
    suspend fun getAllSemesterCoursesList(): List<SemesterCourse> = dao.getAllSemesterCoursesList()

    fun getSemesterCourses(semester: Int): Flow<List<SemesterCourse>> = dao.getSemesterCourses(semester)

    // CA Marks operations (Stored by Subject Name so deleting active attendance subjects does not erase CA records!)
    suspend fun saveCaMark(subjectName: String, subjectId: Int?, ca1Marks: Double?, ca1Total: Double?, ca2Marks: Double?, ca2Total: Double?) {
        val existing = dao.getCaMarkBySubjectName(subjectName)
        val markToSave = existing?.copy(
            subjectId = subjectId ?: existing.subjectId,
            subjectName = subjectName,
            ca1Marks = ca1Marks,
            ca1Total = ca1Total,
            ca2Marks = ca2Marks,
            ca2Total = ca2Total
        ) ?: CaMark(
            subjectId = subjectId,
            subjectName = subjectName,
            ca1Marks = ca1Marks,
            ca1Total = ca1Total,
            ca2Marks = ca2Marks,
            ca2Total = ca2Total
        )
        dao.insertOrUpdateCaMark(markToSave)
    }

    suspend fun deleteCaMarkBySubjectName(subjectName: String) {
        dao.deleteCaMarkBySubjectName(subjectName)
    }

    suspend fun deleteCaMarkById(id: Int) {
        dao.deleteCaMarkById(id)
    }

    // Semester Courses operations (End Semester Grades & Courses for Sems 1-4)
    suspend fun insertSemesterCourse(course: SemesterCourse): Long = dao.insertSemesterCourse(course)
    suspend fun updateSemesterCourse(course: SemesterCourse) = dao.updateSemesterCourse(course)
    suspend fun deleteSemesterCourse(course: SemesterCourse) = dao.deleteSemesterCourse(course)
    suspend fun deleteSemesterCourseById(id: Int) = dao.deleteSemesterCourseById(id)

    // Subject operations
    suspend fun insertSubject(subject: Subject): Long = dao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = dao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = dao.deleteSubject(subject)

    // Timetable operations
    suspend fun insertTimetableSlot(slot: TimetableSlot): Long = dao.insertTimetableSlot(slot)
    suspend fun updateTimetableSlot(slot: TimetableSlot) = dao.updateTimetableSlot(slot)
    suspend fun deleteTimetableSlot(slot: TimetableSlot) = dao.deleteTimetableSlot(slot)

    // Attendance record operations
    suspend fun markAttendance(
        dateString: String,
        subjectId: Int,
        slotId: Int?,
        isExtra: Boolean,
        status: String,
        extraPeriodName: String? = null,
        extraStartTime: String? = null,
        extraEndTime: String? = null
    ) {
        val existingRecords = dao.getAttendanceRecordsForDate(dateString)
        val existing = existingRecords.find { 
            it.subjectId == subjectId && it.slotId == slotId && it.isExtra == isExtra 
        }

        val recordToSave = existing?.copy(status = status) ?: AttendanceRecord(
            subjectId = subjectId,
            dateString = dateString,
            status = status,
            slotId = slotId,
            isExtra = isExtra,
            extraPeriodName = extraPeriodName,
            extraStartTime = extraStartTime,
            extraEndTime = extraEndTime
        )
        dao.insertOrUpdateAttendanceRecord(recordToSave)
    }

    suspend fun clearAttendance(
        dateString: String,
        subjectId: Int,
        slotId: Int?,
        isExtra: Boolean
    ) {
        dao.deleteAttendanceRecord(dateString, subjectId, slotId, isExtra)
    }

    suspend fun markExtraAttendance(
        dateString: String,
        subjectId: Int,
        status: String,
        extraPeriodName: String?,
        extraStartTime: String?,
        extraEndTime: String?
    ) {
        val record = AttendanceRecord(
            subjectId = subjectId,
            dateString = dateString,
            status = status,
            slotId = null,
            isExtra = true,
            extraPeriodName = extraPeriodName,
            extraStartTime = extraStartTime,
            extraEndTime = extraEndTime
        )
        dao.insertOrUpdateAttendanceRecord(record)
    }

    suspend fun deleteAttendanceRecordById(recordId: Int) {
        dao.deleteAttendanceRecordById(recordId)
    }

    // App Settings
    suspend fun getTargetPercentage(): Double {
        return dao.getSettingValue("target_percentage")?.toDoubleOrNull() ?: 75.0
    }

    suspend fun setTargetPercentage(percentage: Double) {
        dao.insertSetting(AppSetting("target_percentage", percentage.toString()))
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return dao.getSettingValue(key) ?: defaultValue
    }

    suspend fun setSetting(key: String, value: String) {
        dao.insertSetting(AppSetting(key, value))
    }

    // Tasks operations
    suspend fun insertTask(task: Task): Long = dao.insertTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
    suspend fun updateTask(task: Task) = dao.updateTask(task)

    // Custom Periods operations
    suspend fun insertCustomPeriod(name: String, startTime: String, endTime: String): Long {
        return dao.insertCustomPeriod(CustomPeriod(name = name, startTime = startTime, endTime = endTime))
    }

    suspend fun updateCustomPeriod(period: CustomPeriod) {
        val old = dao.getCustomPeriodById(period.id)
        if (old != null) {
            dao.updateTimetableSlotTimes(old.name, period.name, period.startTime, period.endTime)
            dao.updateExtraAttendanceTimes(old.name, period.name, period.startTime, period.endTime)
        }
        dao.updateCustomPeriod(period)
    }

    suspend fun deleteCustomPeriod(period: CustomPeriod) = dao.deleteCustomPeriod(period)

    // Export & Import
    suspend fun getExportData(): ExportData {
        return ExportData(
            subjects = dao.getAllSubjectsList(),
            timetableSlots = dao.getAllTimetableSlotsList(),
            attendanceRecords = dao.getAllAttendanceRecordsList(),
            settings = dao.getAllSettingsList(),
            tasks = dao.getAllTasksList(),
            customPeriods = dao.getAllCustomPeriodsList(),
            caMarks = dao.getAllCaMarksList(),
            semesterCourses = dao.getAllSemesterCoursesList()
        )
    }

    @androidx.room.Transaction
    suspend fun importData(exportData: ExportData) {
        dao.clearTasks()
        dao.clearAttendanceRecords()
        dao.clearTimetableSlots()
        dao.clearSubjects()
        dao.clearSettings()
        dao.clearCustomPeriods()
        dao.clearCaMarks()
        dao.clearSemesterCourses()

        if (exportData.subjects.isNotEmpty()) {
            dao.insertSubjects(exportData.subjects)
        }
        if (exportData.timetableSlots.isNotEmpty()) {
            dao.insertTimetableSlots(exportData.timetableSlots)
        }
        if (exportData.attendanceRecords.isNotEmpty()) {
            dao.insertAttendanceRecords(exportData.attendanceRecords)
        }
        if (exportData.settings.isNotEmpty()) {
            dao.insertSettings(exportData.settings)
        }
        if (exportData.tasks.isNotEmpty()) {
            dao.insertTasks(exportData.tasks)
        }
        if (exportData.customPeriods.isNotEmpty()) {
            dao.insertCustomPeriods(exportData.customPeriods)
        }
        if (exportData.caMarks.isNotEmpty()) {
            dao.insertCaMarks(exportData.caMarks)
        }
        if (exportData.semesterCourses.isNotEmpty()) {
            dao.insertSemesterCourses(exportData.semesterCourses)
        }
    }
}
