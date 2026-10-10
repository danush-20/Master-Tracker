package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SubjectStats(
    val subject: Subject,
    val attendedCount: Int,
    val missedCount: Int,
    val offCount: Int,
    val totCount: Int, // attended + missed
    val percentage: Double,
    val infoText: String
)

data class TodaySubjectState(
    val id: String, // "regular_${slotId}" or "extra_${recordId}"
    val subject: Subject,
    val isExtra: Boolean,
    val slot: TimetableSlot?,
    val stats: SubjectStats,
    val record: AttendanceRecord?
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AttendanceDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database.attendanceDao())

    // Date state
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val selectedDateString: Flow<String> = _selectedDate.map { 
        it.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // Target percentage - default 75%
    private val _targetPercentage = MutableStateFlow(75.0)
    val targetPercentage: StateFlow<Double> = _targetPercentage.asStateFlow()

    private val _morningTime = MutableStateFlow("08:00 AM")
    val morningTime: StateFlow<String> = _morningTime.asStateFlow()
    private val _morningEnabled = MutableStateFlow(true)
    val morningEnabled: StateFlow<Boolean> = _morningEnabled.asStateFlow()

    private val _eveningTime = MutableStateFlow("05:00 PM")
    val eveningTime: StateFlow<String> = _eveningTime.asStateFlow()
    private val _eveningEnabled = MutableStateFlow(true)
    val eveningEnabled: StateFlow<Boolean> = _eveningEnabled.asStateFlow()

    private val _nightTime = MutableStateFlow("09:00 PM")
    val nightTime: StateFlow<String> = _nightTime.asStateFlow()
    private val _nightEnabled = MutableStateFlow(true)
    val nightEnabled: StateFlow<Boolean> = _nightEnabled.asStateFlow()

    private val _defaultPeriodDuration = MutableStateFlow(50)
    val defaultPeriodDuration: StateFlow<Int> = _defaultPeriodDuration.asStateFlow()

    init {
        viewModelScope.launch {
            _targetPercentage.value = repository.getTargetPercentage()
            _morningTime.value = repository.getSetting("interval_morning", "08:00 AM")
            _morningEnabled.value = repository.getSetting("interval_morning_enabled", "true") == "true"
            _eveningTime.value = repository.getSetting("interval_evening", "05:00 PM")
            _eveningEnabled.value = repository.getSetting("interval_evening_enabled", "true") == "true"
            _nightTime.value = repository.getSetting("interval_night", "09:00 PM")
            _nightEnabled.value = repository.getSetting("interval_night_enabled", "true") == "true"
            _defaultPeriodDuration.value = repository.getSetting("default_period_duration", "50").toIntOrNull() ?: 50
        }
    }

    // Expose raw data
    val subjects = repository.allSubjects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val timetableSlots = repository.allTimetableSlots.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val attendanceRecords = repository.allAttendanceRecords.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customPeriods = repository.allCustomPeriods.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Derived subject stats
    val subjectStats: StateFlow<List<SubjectStats>> = combine(
        subjects,
        attendanceRecords,
        _targetPercentage
    ) { subList, recList, target ->
        subList.map { subject ->
            val records = recList.filter { it.subjectId == subject.id }
            val attended = subject.baseAttended + records.count { it.status == "ATTENDED" }
            val missed = subject.baseMissed + records.count { it.status == "MISSED" }
            val off = records.count { it.status == "OFF" }
            val tot = attended + missed
            val pct = if (tot > 0) (attended.toDouble() / tot) * 100.0 else 100.0
            
            val info = if (tot == 0) {
                "No classes recorded yet"
            } else if (pct >= target) {
                if (target <= 0.0) {
                    "Always above 0%"
                } else {
                    val maxMiss = ((100.0 * attended - target * tot) / target).toInt()
                    "can miss $maxMiss lectures"
                }
            } else {
                if (target >= 100.0) {
                    "Cannot satisfy 100%"
                } else {
                    val diff = (target * tot - 100.0 * attended) / (100.0 - target)
                    val minAttend = kotlin.math.ceil(diff).toInt()
                    "need to attend $minAttend lectures"
                }
            }

            SubjectStats(
                subject = subject,
                attendedCount = attended,
                missedCount = missed,
                offCount = off,
                totCount = tot,
                percentage = pct,
                infoText = info
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Overall stats
    val overallStats: StateFlow<SubjectStats?> = combine(
        subjectStats,
        _targetPercentage
    ) { statsList, target ->
        if (statsList.isEmpty()) null
        else {
            val totalAttended = statsList.sumOf { it.attendedCount }
            val totalMissed = statsList.sumOf { it.missedCount }
            val totalOff = statsList.sumOf { it.offCount }
            val totalTot = totalAttended + totalMissed
            val pct = if (totalTot > 0) (totalAttended.toDouble() / totalTot) * 100.0 else 100.0
            
            val info = if (totalTot == 0) {
                "No classes recorded yet"
            } else if (pct >= target) {
                if (target <= 0.0) "Always above 0%"
                else {
                    val maxMiss = ((100.0 * totalAttended - target * totalTot) / target).toInt()
                    "can miss $maxMiss lectures"
                }
            } else {
                if (target >= 100.0) "Cannot satisfy 100%"
                else {
                    val diff = (target * totalTot - 100.0 * totalAttended) / (100.0 - target)
                    val minAttend = kotlin.math.ceil(diff).toInt()
                    "need to attend $minAttend lectures"
                }
            }

            SubjectStats(
                subject = Subject(name = "Overall"),
                attendedCount = totalAttended,
                missedCount = totalMissed,
                offCount = totalOff,
                totCount = totalTot,
                percentage = pct,
                infoText = info
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Bunked counter (e.g., total missed)
    val totalBunkedCount: StateFlow<Int> = attendanceRecords.map { records ->
        records.count { it.status == "MISSED" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Today's subject state list
    val todaySubjects: StateFlow<List<TodaySubjectState>> = combine(
        _selectedDate,
        subjects,
        timetableSlots,
        attendanceRecords,
        subjectStats
    ) { date, subList, slots, records, statsList ->
        val dayOfWeek = date.dayOfWeek.value // 1 = Mon, ..., 7 = Sun (we support 6 days)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // Find slots scheduled for this day
        val slotsToday = slots.filter { it.dayOfWeek == dayOfWeek }
        
        // Match slots with subjects and records
        val scheduledStates = slotsToday.mapNotNull { slot ->
            val subj = subList.find { it.id == slot.subjectId } ?: return@mapNotNull null
            val stats = statsList.find { it.subject.id == subj.id } ?: return@mapNotNull null
            
            // Fetch regular records for today
            val regularRec = records.find { 
                it.subjectId == subj.id && it.dateString == dateStr && it.slotId == slot.id && !it.isExtra 
            }
            
            TodaySubjectState(
                id = "regular_${slot.id}",
                subject = subj,
                isExtra = false,
                slot = slot,
                stats = stats,
                record = regularRec
            )
        }
        
        // Fetch any extra records for today
        val extraStates = records.filter { it.dateString == dateStr && it.isExtra }.mapNotNull { rec ->
            val subj = subList.find { it.id == rec.subjectId } ?: return@mapNotNull null
            val stats = statsList.find { it.subject.id == subj.id } ?: return@mapNotNull null
            
            TodaySubjectState(
                id = "extra_${rec.id}",
                subject = subj,
                isExtra = true,
                slot = null,
                stats = stats,
                record = rec
            )
        }
        
        scheduledStates + extraStates
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Date operations
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun navigateDate(days: Int) {
        _selectedDate.value = _selectedDate.value.plusDays(days.toLong())
    }

    // Settings
    fun updateTargetPercentage(percentage: Double) {
        _targetPercentage.value = percentage
        viewModelScope.launch {
            repository.setTargetPercentage(percentage)
        }
    }

    fun updateMorningTime(time: String) {
        _morningTime.value = time
        viewModelScope.launch { repository.setSetting("interval_morning", time) }
    }

    fun toggleMorningEnabled(enabled: Boolean) {
        _morningEnabled.value = enabled
        viewModelScope.launch { repository.setSetting("interval_morning_enabled", enabled.toString()) }
    }

    fun updateEveningTime(time: String) {
        _eveningTime.value = time
        viewModelScope.launch { repository.setSetting("interval_evening", time) }
    }

    fun toggleEveningEnabled(enabled: Boolean) {
        _eveningEnabled.value = enabled
        viewModelScope.launch { repository.setSetting("interval_evening_enabled", enabled.toString()) }
    }

    fun updateNightTime(time: String) {
        _nightTime.value = time
        viewModelScope.launch { repository.setSetting("interval_night", time) }
    }

    fun toggleNightEnabled(enabled: Boolean) {
        _nightEnabled.value = enabled
        viewModelScope.launch { repository.setSetting("interval_night_enabled", enabled.toString()) }
    }

    fun updateDefaultPeriodDuration(duration: Int) {
        _defaultPeriodDuration.value = duration
        viewModelScope.launch { repository.setSetting("default_period_duration", duration.toString()) }
    }

    // Subjects
    fun addSubject(name: String, baseAttended: Int = 0, baseMissed: Int = 0) {
        viewModelScope.launch {
            repository.insertSubject(Subject(name = name, baseAttended = baseAttended, baseMissed = baseMissed))
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    // Custom Periods
    fun addCustomPeriod(name: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.insertCustomPeriod(name, startTime, endTime)
        }
    }

    fun updateCustomPeriod(period: CustomPeriod) {
        viewModelScope.launch {
            repository.updateCustomPeriod(period)
        }
    }

    fun deleteCustomPeriod(period: CustomPeriod) {
        viewModelScope.launch {
            repository.deleteCustomPeriod(period)
        }
    }

    // Timetable
    fun addTimetableSlot(dayOfWeek: Int, subjectId: Int, startTime: String, periodName: String, endTime: String = "10:00 AM") {
        viewModelScope.launch {
            repository.insertTimetableSlot(dayOfWeek, subjectId, startTime, periodName, endTime)
        }
    }

    fun updateTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.updateTimetableSlot(slot)
        }
    }

    fun deleteTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(slot)
        }
    }

    // Attendance Operations
    fun markRegularAttendance(subjectId: Int, slotId: Int, status: String) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.markAttendance(
                dateString = dateStr,
                subjectId = subjectId,
                slotId = slotId,
                isExtra = false,
                status = status
            )
            notifyWidgets()
        }
    }

    fun clearRegularAttendance(subjectId: Int, slotId: Int) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.clearAttendance(
                dateString = dateStr,
                subjectId = subjectId,
                slotId = slotId,
                isExtra = false
            )
            notifyWidgets()
        }
    }

    fun markExtraAttendance(subjectId: Int, status: String, extraPeriodName: String? = null, extraStartTime: String? = null, extraEndTime: String? = null) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.markExtraAttendance(
                dateString = dateStr,
                subjectId = subjectId,
                status = status,
                extraPeriodName = extraPeriodName,
                extraStartTime = extraStartTime,
                extraEndTime = extraEndTime
            )
            notifyWidgets()
        }
    }

    fun updateExtraAttendance(recordId: Int, status: String) {
        viewModelScope.launch {
            repository.updateAttendanceStatusById(recordId, status)
            notifyWidgets()
        }
    }

    fun clearExtraAttendanceById(recordId: Int) {
        viewModelScope.launch {
            repository.deleteAttendanceById(recordId)
            notifyWidgets()
        }
    }

    // Full day quick actions
    fun markFullDayStatus(status: String) {
        markFullDayStatusForDate(_selectedDate.value, status)
    }

    fun markFullDayStatusForDate(date: LocalDate, status: String) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayOfWeek = date.dayOfWeek.value
            val slots = timetableSlots.value.filter { it.dayOfWeek == dayOfWeek }
            for (slot in slots) {
                repository.markAttendance(
                    dateString = dateStr,
                    subjectId = slot.subjectId,
                    slotId = slot.id,
                    isExtra = false,
                    status = status
                )
            }
            val extraRecs = attendanceRecords.value.filter { it.dateString == dateStr && it.isExtra }
            for (rec in extraRecs) {
                repository.updateAttendanceStatusById(rec.id, status)
            }
            notifyWidgets()
        }
    }

    fun clearFullDayAttendance(date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayOfWeek = date.dayOfWeek.value
            val slots = timetableSlots.value.filter { it.dayOfWeek == dayOfWeek }
            for (slot in slots) {
                repository.clearAttendance(
                    dateString = dateStr,
                    subjectId = slot.subjectId,
                    slotId = slot.id,
                    isExtra = false
                )
            }
            val extraRecs = attendanceRecords.value.filter { it.dateString == dateStr && it.isExtra }
            for (rec in extraRecs) {
                repository.deleteAttendanceById(rec.id)
            }
            notifyWidgets()
        }
    }

    fun notifyWidgets() {
        try {
            val context = getApplication<android.app.Application>()
            val manager = android.appwidget.AppWidgetManager.getInstance(context)
            val attCmp = android.content.ComponentName(context, com.example.widget.AttendanceWidgetProvider::class.java)
            val attWidgetIds = manager.getAppWidgetIds(attCmp)
            if (attWidgetIds.isNotEmpty()) {
                val refreshIntent = android.content.Intent(context, com.example.widget.AttendanceWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, attWidgetIds)
                }
                context.sendBroadcast(refreshIntent)
                manager.notifyAppWidgetViewDataChanged(attWidgetIds, com.example.R.id.widget_list_view)
            }
            val currCmp = android.content.ComponentName(context, com.example.widget.CurrentClassWidgetProvider::class.java)
            val currWidgetIds = manager.getAppWidgetIds(currCmp)
            if (currWidgetIds.isNotEmpty()) {
                val currIntent = android.content.Intent(context, com.example.widget.CurrentClassWidgetProvider::class.java).apply {
                    action = com.example.widget.CurrentClassWidgetProvider.ACTION_WIDGET_REFRESH
                }
                context.sendBroadcast(currIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- TASK MANAGER STATE & OPERATIONS ---
    val tasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun addTask(title: String, subjectId: Int?, priority: String, fuzzyPeriod: String?, note: String) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    subjectId = subjectId,
                    priority = priority,
                    fuzzyPeriod = fuzzyPeriod,
                    note = note,
                    isCompleted = false
                )
            )
        }
    }

    fun updateTaskDetails(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val completed = tasks.value.filter { it.isCompleted }
            for (task in completed) {
                repository.deleteTask(task)
            }
        }
    }

    // Populate standard sample entries for fresh visual appeal
    fun populateDemoData() {
        viewModelScope.launch {
            // Task Samples
            repository.insertTask(Task(title = "Revise Physics Lecture notes", subjectId = null, priority = "HIGH", fuzzyPeriod = "Today Night", note = "Read chapters 3 to 5", isCompleted = false))
            repository.insertTask(Task(title = "Submit Calculus Assignment", subjectId = null, priority = "MEDIUM", fuzzyPeriod = "Tomorrow Morning", note = "Problems 1-10", isCompleted = true))
            repository.insertTask(Task(title = "Pay Semester Club Fee", subjectId = null, priority = "LOW", fuzzyPeriod = "Tomorrow Evening", note = "", isCompleted = false))
            
            // Add live class tracker demo for current time
            val subjId = repository.insertSubject(Subject(name = "Computer Networks")).toInt()
            
            val now = java.time.LocalTime.now()
            val startLocal = now.minusMinutes(10)
            val endLocal = now.plusMinutes(50)
            
            val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US)
            val startStr = startLocal.format(formatter)
            val endStr = endLocal.format(formatter)
            
            val dayOfWeek = java.time.LocalDate.now().dayOfWeek.value
            repository.insertTimetableSlot(dayOfWeek, subjId, startStr, "Live Lecture", endStr)
        }
    }

    suspend fun getExportDataJson(): String {
        val exportData = repository.getExportData()
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter(ExportData::class.java)
        return adapter.toJson(exportData)
    }

    suspend fun importDataFromJson(json: String) {
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter(ExportData::class.java)
        val exportData = adapter.fromJson(json)
        if (exportData != null) {
            repository.importData(exportData)
        } else {
            throw Exception("Failed to parse JSON")
        }
    }
}
