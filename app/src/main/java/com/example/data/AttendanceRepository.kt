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

    suspend fun getAllSubjectsList(): List<Subject> = dao.getAllSubjectsList()
    suspend fun getAllCustomPeriodsList(): List<CustomPeriod> = dao.getAllCustomPeriodsList()
    suspend fun getAllTimetableSlotsList(): List<TimetableSlot> = dao.getAllTimetableSlotsList()
    suspend fun getAllAttendanceRecordsList(): List<AttendanceRecord> = dao.getAllAttendanceRecordsList()
    suspend fun getAllCaMarksList(): List<CaMark> = dao.getAllCaMarksList()

    // CA Marks operations
    suspend fun saveCaMark(subjectId: Int, ca1Marks: Double?, ca1Total: Double?, ca2Marks: Double?, ca2Total: Double?) {
        val existing = dao.getCaMarkForSubject(subjectId)
        val mark = CaMark(
            id = existing?.id ?: 0,
            subjectId = subjectId,
            ca1Marks = ca1Marks,
            ca1Total = ca1Total,
            ca2Marks = ca2Marks,
            ca2Total = ca2Total
        )
        dao.insertOrUpdateCaMark(mark)
    }

    suspend fun deleteCaMarkBySubject(subjectId: Int) {
        dao.deleteCaMarkBySubjectId(subjectId)
    }

    suspend fun deleteCaMarkById(id: Int) {
        dao.deleteCaMarkById(id)
    }

    // Tasks operations
    suspend fun insertTask(task: Task) {
        dao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        dao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        dao.deleteTask(task)
    }

    suspend fun insertSubject(subject: Subject): Long {
        return dao.insertSubject(subject)
    }

    suspend fun updateSubject(subject: Subject) {
        dao.updateSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        dao.deleteSubject(subject)
    }

    suspend fun insertCustomPeriod(name: String, startTime: String, endTime: String): Long {
        return dao.insertCustomPeriod(CustomPeriod(name = name, startTime = startTime, endTime = endTime))
    }

    suspend fun updateCustomPeriod(period: CustomPeriod) {
        val oldPeriod = dao.getCustomPeriodById(period.id)
        dao.updateCustomPeriod(period)
        if (oldPeriod != null) {
            dao.updateTimetableSlotTimes(oldPeriod.name, period.name, period.startTime, period.endTime)
            dao.updateExtraAttendanceTimes(oldPeriod.name, period.name, period.startTime, period.endTime)
        }
    }

    suspend fun deleteCustomPeriod(period: CustomPeriod) {
        dao.deleteCustomPeriod(period)
    }

    suspend fun insertTimetableSlot(dayOfWeek: Int, subjectId: Int, startTime: String, periodName: String, endTime: String) {
        dao.insertTimetableSlot(
            TimetableSlot(
                dayOfWeek = dayOfWeek,
                subjectId = subjectId,
                startTime = startTime,
                periodName = periodName,
                endTime = endTime
            )
        )
    }

    suspend fun updateTimetableSlot(slot: TimetableSlot) {
        dao.updateTimetableSlot(slot)
    }

    suspend fun deleteTimetableSlot(slot: TimetableSlot) {
        dao.deleteTimetableSlot(slot)
    }

    suspend fun markAttendance(dateString: String, subjectId: Int, slotId: Int?, isExtra: Boolean, status: String) {
        val record = AttendanceRecord(
            subjectId = subjectId,
            dateString = dateString,
            status = status,
            slotId = slotId,
            isExtra = isExtra
        )
        dao.insertOrUpdateAttendanceRecord(record)
    }

    suspend fun markExtraAttendance(dateString: String, subjectId: Int, status: String, extraPeriodName: String?, extraStartTime: String?, extraEndTime: String?) {
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

    suspend fun clearAttendance(dateString: String, subjectId: Int, slotId: Int?, isExtra: Boolean) {
        dao.deleteAttendanceRecord(dateString, subjectId, slotId, isExtra)
    }

    suspend fun updateAttendanceStatusById(recordId: Int, status: String) {
        dao.updateAttendanceRecordStatus(recordId, status)
    }

    suspend fun deleteAttendanceById(recordId: Int) {
        dao.deleteAttendanceRecordById(recordId)
    }

    suspend fun getTargetPercentage(): Double {
        val value = dao.getSettingValue("target_percentage")
        return value?.toDoubleOrNull() ?: 75.0
    }

    suspend fun setTargetPercentage(percentage: Double) {
        dao.insertSetting(AppSetting("target_percentage", percentage.toString()))
    }

    suspend fun getSetting(key: String, default: String): String {
        return dao.getSettingValue(key) ?: default
    }

    suspend fun setSetting(key: String, value: String) {
        dao.insertSetting(AppSetting(key, value))
    }

    suspend fun getExportData(): ExportData {
        return ExportData(
            subjects = dao.getAllSubjectsList(),
            timetableSlots = dao.getAllTimetableSlotsList(),
            attendanceRecords = dao.getAllAttendanceRecordsList(),
            settings = dao.getAllSettingsList(),
            tasks = dao.getAllTasksList(),
            customPeriods = dao.getAllCustomPeriodsList(),
            caMarks = dao.getAllCaMarksList()
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

        dao.insertSubjects(exportData.subjects)
        dao.insertTimetableSlots(exportData.timetableSlots)
        dao.insertAttendanceRecords(exportData.attendanceRecords)
        dao.insertSettings(exportData.settings)
        dao.insertTasks(exportData.tasks)
        dao.insertCustomPeriods(exportData.customPeriods)
        if (exportData.caMarks.isNotEmpty()) {
            dao.insertCaMarks(exportData.caMarks)
        }
    }
}

