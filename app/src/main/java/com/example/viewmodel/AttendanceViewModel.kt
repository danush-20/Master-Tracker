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
    val infoText: String,
    val lectureAttended: Int = 0,
    val lectureMissed: Int = 0,
    val lectureTot: Int = 0,
    val labAttended: Int = 0,
    val labMissed: Int = 0,
    val labTot: Int = 0
)

data class TodaySubjectState(
    val id: String, // "regular_${slotId}" or "extra_${recordId}"
    val subject: Subject,
    val isExtra: Boolean,
    val slot: TimetableSlot?,
    val stats: SubjectStats,
    val record: AttendanceRecord?
)

data class SubjectCaMarkItem(
    val subjectName: String,
    val activeSubject: Subject?,
    val caMark: CaMark?,
    val ca1Marks: Double?,
    val ca1Total: Double?,
    val ca1Pct: Double?,
    val ca2Marks: Double?,
    val ca2Total: Double?,
    val ca2Pct: Double?,
    val totalObtained: Double,
    val totalMax: Double,
    val overallPct: Double?,
    val hasMarks: Boolean
)

data class CaOverallMetrics(
    val totalObtained: Double,
    val totalMax: Double,
    val overallPercentage: Double?,
    val subjectsEvaluated: Int,
    val totalSubjects: Int
)

data class SemesterGpaResult(
    val semester: Int,
    val totalCredits: Double,
    val gpa: Double?,
    val coursesCount: Int,
    val gradedCount: Int
)

data class OverallCgpaResult(
    val totalCredits: Double,
    val cgpa: Double?,
    val selectedSemesters: List<Int>,
    val totalCoursesCount: Int,
    val gradedCoursesCount: Int
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

    // Selected semesters for CGPA calculation (Semesters 1-4 supported, default all selected)
    private val _selectedSemesters = MutableStateFlow(setOf(1, 2, 3, 4))
    val selectedSemesters: StateFlow<Set<Int>> = _selectedSemesters.asStateFlow()

    // Active semester tab in Grades screen (1, 2, 3, or 4)
    private val _activeSemesterTab = MutableStateFlow(1)
    val activeSemesterTab: StateFlow<Int> = _activeSemesterTab.asStateFlow()

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
    val semesterCourses = repository.allSemesterCourses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Derived subject stats (with Lecture vs Lab breakdown for lab-integrated subjects)
    val subjectStats: StateFlow<List<SubjectStats>> = combine(
        subjects,
        timetableSlots,
        attendanceRecords,
        _targetPercentage
    ) { subList, slotList, recList, target ->
        subList.map { subject ->
            val records = recList.filter { it.subjectId == subject.id }
            val attended = subject.baseAttended + records.count { it.status == "ATTENDED" }
            val missed = subject.baseMissed + records.count { it.status == "MISSED" }
            val off = records.count { it.status == "OFF" }
            val tot = attended + missed
            val pct = if (tot > 0) (attended.toDouble() / tot) * 100.0 else 100.0
            
            // Lecture vs Lab breakdown
            var lecAtt = 0
            var lecMiss = 0
            var labAtt = 0
            var labMiss = 0

            records.forEach { r ->
                val slot = if (r.slotId != null) slotList.find { it.id == r.slotId } else null
                val isLab = when {
                    slot != null -> slot.classType.equals("Lab", ignoreCase = true)
                    r.extraPeriodName != null -> r.extraPeriodName.contains("Lab", ignoreCase = true)
                    else -> false
                }
                when (r.status) {
                    "ATTENDED" -> if (isLab) labAtt++ else lecAtt++
                    "MISSED" -> if (isLab) labMiss++ else lecMiss++
                }
            }

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
                infoText = info,
                lectureAttended = lecAtt,
                lectureMissed = lecMiss,
                lectureTot = lecAtt + lecMiss,
                labAttended = labAtt,
                labMissed = labMiss,
                labTot = labAtt + labMiss
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

    // Bunked counter
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
        val dayOfWeek = date.dayOfWeek.value // 1 = Mon, ..., 7 = Sun
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val slotsToday = slots.filter { it.dayOfWeek == dayOfWeek }
        
        val scheduledStates = slotsToday.mapNotNull { slot ->
            val subj = subList.find { it.id == slot.subjectId } ?: return@mapNotNull null
            val stats = statsList.find { it.subject.id == subj.id } ?: return@mapNotNull null
            
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
    fun addSubject(name: String, baseAttended: Int = 0, baseMissed: Int = 0, isLabIntegrated: Boolean = false) {
        viewModelScope.launch {
            repository.insertSubject(
                Subject(
                    name = name,
                    baseAttended = baseAttended,
                    baseMissed = baseMissed,
                    isLabIntegrated = isLabIntegrated
                )
            )
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
    fun addTimetableSlot(
        dayOfWeek: Int,
        subjectId: Int,
        startTime: String,
        periodName: String,
        endTime: String = "10:00 AM",
        classType: String = "Lecture"
    ) {
        viewModelScope.launch {
            repository.insertTimetableSlot(
                TimetableSlot(
                    dayOfWeek = dayOfWeek,
                    subjectId = subjectId,
                    startTime = startTime,
                    periodName = periodName,
                    endTime = endTime,
                    classType = classType
                )
            )
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

    fun markExtraAttendance(
        subjectId: Int,
        status: String,
        extraPeriodName: String? = null,
        extraStartTime: String? = null,
        extraEndTime: String? = null
    ) {
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
            val rec = repository.getAllAttendanceRecordsList().find { it.id == recordId }
            if (rec != null) {
                repository.markAttendance(
                    dateString = rec.dateString,
                    subjectId = rec.subjectId,
                    slotId = rec.slotId,
                    isExtra = rec.isExtra,
                    status = status,
                    extraPeriodName = rec.extraPeriodName,
                    extraStartTime = rec.extraStartTime,
                    extraEndTime = rec.extraEndTime
                )
            }
            notifyWidgets()
        }
    }

    fun clearExtraAttendanceById(recordId: Int) {
        viewModelScope.launch {
            repository.deleteAttendanceRecordById(recordId)
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
                repository.markAttendance(
                    dateString = rec.dateString,
                    subjectId = rec.subjectId,
                    slotId = rec.slotId,
                    isExtra = rec.isExtra,
                    status = status,
                    extraPeriodName = rec.extraPeriodName,
                    extraStartTime = rec.extraStartTime,
                    extraEndTime = rec.extraEndTime
                )
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
                repository.deleteAttendanceRecordById(rec.id)
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

    // --- CA MARKS STATE & OPERATIONS ---
    // CA marks are saved by subjectName so deleting subjects from attendance never erases past CA scores!
    // The subject list is derived dynamically from current course subjects + previously scored subjects
    val caMarks: StateFlow<List<CaMark>> = repository.allCaMarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val subjectCaMarkItems: StateFlow<List<SubjectCaMarkItem>> = combine(
        subjects,
        caMarks
    ) { subList, markList ->
        // Distinct subject names: active course subjects + any preserved subjects with marks
        val allNames = (subList.map { it.name } + markList.map { it.subjectName }).distinct().filter { it.isNotBlank() }

        allNames.map { name ->
            val activeSubj = subList.find { it.name.equals(name, ignoreCase = true) }
            val mark = markList.find { it.subjectName.equals(name, ignoreCase = true) }

            val ca1M = mark?.ca1Marks
            val ca1T = mark?.ca1Total
            val ca1Pct = if (ca1M != null && ca1T != null && ca1T > 0) (ca1M / ca1T) * 100.0 else null

            val ca2M = mark?.ca2Marks
            val ca2T = mark?.ca2Total
            val ca2Pct = if (ca2M != null && ca2T != null && ca2T > 0) (ca2M / ca2T) * 100.0 else null

            var obtained = 0.0
            var maxTot = 0.0
            var hasAny = false
            if (ca1M != null && ca1T != null && ca1T > 0) {
                obtained += ca1M
                maxTot += ca1T
                hasAny = true
            }
            if (ca2M != null && ca2T != null && ca2T > 0) {
                obtained += ca2M
                maxTot += ca2T
                hasAny = true
            }
            val overall = if (hasAny && maxTot > 0) (obtained / maxTot) * 100.0 else null

            SubjectCaMarkItem(
                subjectName = name,
                activeSubject = activeSubj,
                caMark = mark,
                ca1Marks = ca1M,
                ca1Total = ca1T,
                ca1Pct = ca1Pct,
                ca2Marks = ca2M,
                ca2Total = ca2T,
                ca2Pct = ca2Pct,
                totalObtained = obtained,
                totalMax = maxTot,
                overallPct = overall,
                hasMarks = hasAny
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val caOverallMetrics: StateFlow<CaOverallMetrics> = subjectCaMarkItems.map { items ->
        var totalObt = 0.0
        var totalMax = 0.0
        var count = 0
        for (item in items) {
            if (item.hasMarks && item.totalMax > 0) {
                totalObt += item.totalObtained
                totalMax += item.totalMax
                count++
            }
        }
        val overallPct = if (totalMax > 0) (totalObt / totalMax) * 100.0 else null
        CaOverallMetrics(
            totalObtained = totalObt,
            totalMax = totalMax,
            overallPercentage = overallPct,
            subjectsEvaluated = count,
            totalSubjects = items.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CaOverallMetrics(0.0, 0.0, null, 0, 0))

    fun saveCaMarks(subjectName: String, subjectId: Int?, ca1Marks: Double?, ca1Total: Double?, ca2Marks: Double?, ca2Total: Double?) {
        viewModelScope.launch {
            repository.saveCaMark(subjectName.trim(), subjectId, ca1Marks, ca1Total, ca2Marks, ca2Total)
        }
    }

    fun deleteCaMarks(subjectName: String) {
        viewModelScope.launch {
            repository.deleteCaMarkBySubjectName(subjectName.trim())
        }
    }

    // --- SEMESTER & GPA/CGPA CALCULATIONS (END SEMESTER GRADES) ---
    fun setActiveSemesterTab(sem: Int) {
        if (sem in 1..4) {
            _activeSemesterTab.value = sem
        }
    }

    fun toggleSemesterSelection(sem: Int) {
        val current = _selectedSemesters.value.toMutableSet()
        if (current.contains(sem)) {
            if (current.size > 1) { // keep at least 1
                current.remove(sem)
            }
        } else {
            current.add(sem)
        }
        _selectedSemesters.value = current
    }

    fun addSemesterCourse(
        semester: Int,
        name: String,
        credits: Double,
        grade: String? = null
    ) {
        viewModelScope.launch {
            repository.insertSemesterCourse(
                SemesterCourse(
                    semester = semester,
                    name = name.trim(),
                    credits = credits,
                    grade = grade
                )
            )
        }
    }

    fun updateSemesterCourse(course: SemesterCourse) {
        viewModelScope.launch {
            repository.updateSemesterCourse(course)
        }
    }

    fun deleteSemesterCourse(course: SemesterCourse) {
        viewModelScope.launch {
            repository.deleteSemesterCourse(course)
        }
    }

    // Semester GPA per semester (1..4)
    fun getSemesterGpa(semester: Int, courses: List<SemesterCourse>): SemesterGpaResult {
        val semCourses = courses.filter { it.semester == semester }
        var sumCreditPoints = 0.0
        var sumCredits = 0.0
        var graded = 0
        var totalCreds = 0.0

        for (c in semCourses) {
            totalCreds += c.credits
            val gp = gradeToPoints(c.grade)
            if (gp != null) {
                sumCreditPoints += (c.credits * gp)
                sumCredits += c.credits
                graded++
            }
        }

        val gpa = if (sumCredits > 0.0) sumCreditPoints / sumCredits else null
        return SemesterGpaResult(
            semester = semester,
            totalCredits = totalCreds,
            gpa = gpa,
            coursesCount = semCourses.size,
            gradedCount = graded
        )
    }

    // Credit-weighted CGPA across selected semesters: sum(credits * gradePoints) / sum(credits)
    val overallCgpaResult: StateFlow<OverallCgpaResult> = combine(
        semesterCourses,
        _selectedSemesters
    ) { courses, selectedSems ->
        val filtered = courses.filter { selectedSems.contains(it.semester) }
        var sumCreditPoints = 0.0
        var sumCredits = 0.0
        var gradedCount = 0
        var totalCreds = 0.0

        for (c in filtered) {
            totalCreds += c.credits
            val gp = gradeToPoints(c.grade)
            if (gp != null) {
                sumCreditPoints += (c.credits * gp)
                sumCredits += c.credits
                gradedCount++
            }
        }

        val cgpa = if (sumCredits > 0.0) sumCreditPoints / sumCredits else null
        OverallCgpaResult(
            totalCredits = totalCreds,
            cgpa = cgpa,
            selectedSemesters = selectedSems.sorted(),
            totalCoursesCount = filtered.size,
            gradedCoursesCount = gradedCount
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OverallCgpaResult(0.0, null, listOf(1, 2, 3, 4), 0, 0))

    companion object {
        fun gradeToPoints(grade: String?): Double? {
            return when (grade?.trim()?.uppercase()) {
                "O" -> 10.0
                "A+" -> 9.0
                "A" -> 8.0
                "B+" -> 7.0
                "B" -> 6.0
                "C" -> 5.0
                "U" -> 0.0
                else -> null
            }
        }

        val GRADE_OPTIONS = listOf("O", "A+", "A", "B+", "B", "C", "U")
        val CREDIT_OPTIONS = listOf(1.5, 3.0, 4.0, 4.5)
    }

    // Retained for database compatibility if needed
    val tasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

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
