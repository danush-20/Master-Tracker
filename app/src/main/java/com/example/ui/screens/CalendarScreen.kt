package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AttendanceViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AttendanceViewModel,
    onNavigateToScheduleDate: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.attendanceRecords.collectAsState()
    val timetableSlots by viewModel.timetableSlots.collectAsState()
    val subjectsList by viewModel.subjects.collectAsState()
    val targetPct by viewModel.targetPercentage.collectAsState()
    val context = LocalContext.current

    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var focusedDate by remember { mutableStateOf(LocalDate.now()) }

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    val dayDetailFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US)

    // Monthly stats calculations
    val monthStats = remember(allRecords, currentYearMonth) {
        var offCount = 0
        var missedCount = 0
        var attendedCount = 0

        allRecords.forEach { rec ->
            try {
                val recDate = LocalDate.parse(rec.dateString)
                if (recDate.year == currentYearMonth.year && recDate.monthValue == currentYearMonth.monthValue) {
                    when (rec.status) {
                        "ATTENDED" -> attendedCount++
                        "MISSED" -> missedCount++
                        "OFF" -> offCount++
                    }
                }
            } catch (e: Exception) {}
        }

        val tot = attendedCount + missedCount
        val pct = if (tot > 0) (attendedCount.toDouble() / tot) * 100.0 else 0.0

        object {
            val off = offCount
            val missed = missedCount
            val attended = attendedCount
            val total = tot
            val percent = pct
        }
    }

    // Days in current selected month
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstOfMonth = currentYearMonth.atDay(1)
    // Offset for Monday-start (Mon=1 -> 0, Sun=7 -> 6)
    val startDayOffset = firstOfMonth.dayOfWeek.value - 1

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFBA68C8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attendance History", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212)),
                actions = {
                    // Quick target percentage badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Target: ${targetPct.toInt()}%",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Switcher Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF232936)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Prev Month", tint = Color.LightGray)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentYearMonth.format(monthFormatter),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (currentYearMonth != YearMonth.now()) {
                                Text(
                                    text = "Current Month",
                                    fontSize = 11.sp,
                                    color = Color(0xFFBA68C8),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            currentYearMonth = YearMonth.now()
                                            focusedDate = LocalDate.now()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", tint = Color.LightGray)
                        }
                    }
                }
            }

            // Monthly Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF232936)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MONTH AT A GLANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniColumn(title = "Off", count = monthStats.off.toString(), dotColor = Color(0xFFFBC02D))
                            StatMiniColumn(title = "Missed", count = monthStats.missed.toString(), dotColor = Color(0xFFE91E63))
                            StatMiniColumn(title = "Attended", count = monthStats.attended.toString(), dotColor = Color(0xFF4CAF50))
                            StatMiniColumn(title = "Total", count = monthStats.total.toString())
                            StatMiniColumn(
                                title = "Rate",
                                count = String.format(Locale.US, "%.1f%%", monthStats.percent),
                                highlightColor = if (monthStats.percent >= targetPct) Color(0xFF4CAF50) else Color(0xFFE91E63)
                            )
                        }
                    }
                }
            }

            // Calendar Grid Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF232936)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Weekday Names
                        val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekDays.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dayName == "Sun") Color(0xFFEF5350) else Color(0xFF90A4AE)
                                )
                            }
                        }

                        Divider(color = Color(0xFF232936), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Grid Weeks
                        val totalCells = startDayOffset + daysInMonth
                        val numRows = (totalCells + 6) / 7

                        for (row in 0 until numRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - startDayOffset + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val cellDate = currentYearMonth.atDay(dayNum)
                                        val isSelected = cellDate == focusedDate
                                        val isToday = cellDate == LocalDate.now()

                                        // Attendance records for this specific day
                                        val cellDateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        val recordsOnCell = allRecords.filter { it.dateString == cellDateStr }
                                        val slotsOnDay = timetableSlots.filter { it.dayOfWeek == cellDate.dayOfWeek.value }

                                        val attendedCount = recordsOnCell.count { it.status == "ATTENDED" }
                                        val missedCount = recordsOnCell.count { it.status == "MISSED" }
                                        val offCount = recordsOnCell.count { it.status == "OFF" }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.background(Color(0xFF4F46E5))
                                                    } else if (isToday) {
                                                        Modifier
                                                            .background(Color(0xFF1E222B))
                                                            .border(1.dp, Color(0xFFBA68C8), RoundedCornerShape(10.dp))
                                                    } else {
                                                        Modifier.background(Color.Transparent)
                                                    }
                                                )
                                                .clickable {
                                                    focusedDate = cellDate
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> Color(0xFFBA68C8)
                                                        col == 6 -> Color(0xFFE57373)
                                                        else -> Color.White
                                                    }
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                // Attendance status dot indicator
                                                if (recordsOnCell.isNotEmpty()) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (attendedCount > 0) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else Color(0xFF4CAF50))
                                                            )
                                                        }
                                                        if (missedCount > 0) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.Yellow else Color(0xFFE91E63))
                                                            )
                                                        }
                                                        if (offCount > 0 && attendedCount == 0 && missedCount == 0) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else Color(0xFFFBC02D))
                                                            )
                                                        }
                                                    }
                                                } else if (slotsOnDay.isNotEmpty() && cellDate.isBefore(LocalDate.now())) {
                                                    // Scheduled classes were missed/unrecorded in past
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Gray.copy(alpha = 0.5f))
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        // Empty space for offset
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Clicked Day Detail Section
            item {
                val focusedDateStr = focusedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayOfWeek = focusedDate.dayOfWeek.value
                val slotsForDay = timetableSlots.filter { it.dayOfWeek == dayOfWeek }
                val recordsForDay = allRecords.filter { it.dateString == focusedDateStr }

                val dayAttended = recordsForDay.count { it.status == "ATTENDED" }
                val dayMissed = recordsForDay.count { it.status == "MISSED" }
                val dayOff = recordsForDay.count { it.status == "OFF" }
                val dayTotal = dayAttended + dayMissed
                val dayPct = if (dayTotal > 0) (dayAttended.toDouble() / dayTotal) * 100.0 else 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_detail_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF232936)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Date Heading & Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = focusedDate.format(dayDetailFormatter),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (focusedDate == LocalDate.now()) "Today" else if (focusedDate.isBefore(LocalDate.now())) "Past Record" else "Upcoming",
                                    fontSize = 11.sp,
                                    color = if (focusedDate == LocalDate.now()) Color(0xFFBA68C8) else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (dayTotal > 0) {
                                Surface(
                                    color = if (dayPct >= targetPct) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f%%", dayPct),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Day attendance breakdown pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayStatusPill(label = "Attended", count = dayAttended, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                            DayStatusPill(label = "Missed", count = dayMissed, color = Color(0xFFE91E63), modifier = Modifier.weight(1f))
                            DayStatusPill(label = "Off", count = dayOff, color = Color(0xFFFBC02D), modifier = Modifier.weight(1f))
                        }

                        Divider(color = Color(0xFF232936), thickness = 0.8.dp)

                        // Classes / Periods list on this day
                        if (slotsForDay.isEmpty() && recordsForDay.isEmpty()) {
                            Text(
                                text = "No timetable slots or classes scheduled for this day.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "PERIODS & ATTENDANCE RECORD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 0.5.sp
                                )

                                // Regular slots
                                slotsForDay.forEach { slot ->
                                    val subj = subjectsList.find { it.id == slot.subjectId }
                                    val rec = recordsForDay.find { it.slotId == slot.id && !it.isExtra }

                                    DayPeriodRow(
                                        subjectName = subj?.name ?: "Unknown Subject",
                                        periodInfo = "${slot.periodName} (${slot.startTime} - ${slot.endTime})",
                                        status = rec?.status,
                                        onClick = {
                                            onNavigateToScheduleDate(focusedDate)
                                        }
                                    )
                                }

                                // Extra classes on this day
                                recordsForDay.filter { it.isExtra }.forEach { extraRec ->
                                    val subj = subjectsList.find { it.id == extraRec.subjectId }
                                    val pName = extraRec.extraPeriodName ?: "Extra Class"
                                    val pTime = if (extraRec.extraStartTime != null) " (${extraRec.extraStartTime} - ${extraRec.extraEndTime})" else ""

                                    DayPeriodRow(
                                        subjectName = subj?.name ?: "Unknown Subject",
                                        periodInfo = "$pName$pTime (Extra)",
                                        status = extraRec.status,
                                        onClick = {
                                            onNavigateToScheduleDate(focusedDate)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // REDIRECT BUTTON TO SCHEDULE VIEW - Modern Elevated Style
                        Button(
                            onClick = { onNavigateToScheduleDate(focusedDate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_edit_schedule_date"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Records in Schedule View",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Quick action buttons with high-contrast, modern M3 UI
                        if (slotsForDay.isNotEmpty() || recordsForDay.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.markFullDayStatusForDate(focusedDate, "ATTENDED")
                                        Toast.makeText(context, "Marked full day as Attended", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Full Attended",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.markFullDayStatusForDate(focusedDate, "OFF")
                                        Toast.makeText(context, "Marked full day as Leave / Off", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE65100),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Full Leave",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun DayStatusPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E222B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun DayPeriodRow(
    subjectName: String,
    periodInfo: String,
    status: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = Color(0xFF1E222B),
        border = BorderStroke(1.dp, Color(0xFF282F3B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = periodInfo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status tag
            when (status) {
                "ATTENDED" -> {
                    Surface(
                        color = Color(0xFF1B5E20),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ Attended",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                "MISSED" -> {
                    Surface(
                        color = Color(0xFFB71C1C),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✕ Missed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF9A9A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                "OFF" -> {
                    Surface(
                        color = Color(0xFFF57F17),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "– Off",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                else -> {
                    Surface(
                        color = Color(0xFF2C323F),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Not Marked",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatMiniColumn(
    title: String,
    count: String,
    dotColor: Color? = null,
    highlightColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: Color.White
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
