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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attendance History",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A22)
                )
            )
        },
        containerColor = Color(0xFF0F1216)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Month navigation card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF282F3B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Previous Month",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = currentYearMonth.format(monthFormatter),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Next Month",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Monthly Summary Pill Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2430), RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthStats.attended}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text("Attended", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthStats.missed}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63)
                                )
                                Text("Missed", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthStats.off}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBC02D)
                                )
                                Text("Off/Leave", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val isSafe = monthStats.percent >= targetPct
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", monthStats.percent),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSafe) Color(0xFF4CAF50) else Color(0xFFE91E63)
                                )
                                Text("Monthly %", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Day of week header row (Mon - Sun)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                            dayNames.forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF232936), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid Weeks
                        val totalCells = startDayOffset + daysInMonth
                        val numRows = (totalCells + 6) / 7

                        for (r in 0 until numRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - startDayOffset + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val thisDate = currentYearMonth.atDay(dayNum)
                                        val isSelected = thisDate == focusedDate
                                        val isToday = thisDate == LocalDate.now()
                                        val dateStr = thisDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                                        val dayRecords = allRecords.filter { it.dateString == dateStr }
                                        val hasAttended = dayRecords.any { it.status == "ATTENDED" }
                                        val hasMissed = dayRecords.any { it.status == "MISSED" }
                                        val hasOff = dayRecords.any { it.status == "OFF" }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFF4F46E5)
                                                        isToday -> Color(0xFF262C38)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                    color = if (isToday && !isSelected) Color(0xFFBA68C8) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    focusedDate = thisDate
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> Color(0xFFBA68C8)
                                                        else -> Color.White
                                                    }
                                                )

                                                if (dayRecords.isNotEmpty()) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        if (hasAttended) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFF4CAF50))
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                        }
                                                        if (hasMissed) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFE91E63))
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                        }
                                                        if (hasOff) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFFBC02D))
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Focused Date Detail Card
            item {
                val focusedDateStr = focusedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayOfWeek = focusedDate.dayOfWeek.value
                val slotsForDay = timetableSlots.filter { it.dayOfWeek == dayOfWeek }
                val recordsForDay = allRecords.filter { it.dateString == focusedDateStr }

                val dayAttended = recordsForDay.count { it.status == "ATTENDED" }
                val dayMissed = recordsForDay.count { it.status == "MISSED" }
                val dayOff = recordsForDay.count { it.status == "OFF" }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF282F3B))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DAY DETAILS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBA68C8),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = focusedDate.format(dayDetailFormatter),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (focusedDate == LocalDate.now()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4F46E5)
                                ) {
                                    Text(
                                        text = "Today",
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

                        HorizontalDivider(color = Color(0xFF232936), thickness = 0.8.dp)

                        // Classes / Periods list on this day (Showing Lecture vs Lab separately)
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
                                    val isLab = slot.classType.equals("Lab", ignoreCase = true)
                                    val componentTag = if (isLab) " [Lab]" else " [Lecture]"

                                    DayPeriodRow(
                                        subjectName = "${subj?.name ?: "Unknown Subject"}$componentTag",
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

                        // Redirect Button to Schedule View
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

                        // Quick action buttons
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
                                        containerColor = Color(0xFF1B5E20),
                                        contentColor = Color(0xFF81C784)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Mark All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.markFullDayStatusForDate(focusedDate, "OFF")
                                        Toast.makeText(context, "Marked full day as Off", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF78350F),
                                        contentColor = Color(0xFFFDE68A)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Mark Full Day Off", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        color = Color(0xFF1E2430),
        border = BorderStroke(1.dp, Color(0xFF263044))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = periodInfo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            val statusColor = when (status) {
                "ATTENDED" -> Color(0xFF4CAF50)
                "MISSED" -> Color(0xFFE91E63)
                "OFF" -> Color(0xFFFBC02D)
                else -> Color.Gray
            }
            val statusLabel = when (status) {
                "ATTENDED" -> "Attended"
                "MISSED" -> "Missed"
                "OFF" -> "Off"
                else -> "Not Marked"
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
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
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E2430),
        border = BorderStroke(1.dp, Color(0xFF263044))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
