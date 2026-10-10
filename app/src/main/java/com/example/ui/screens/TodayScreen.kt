package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import com.example.data.Subject
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.TodaySubjectState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: AttendanceViewModel,
    onNavigateToSubjects: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val targetPct by viewModel.targetPercentage.collectAsState()
    val todaySubjects by viewModel.todaySubjects.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val totalBunked by viewModel.totalBunkedCount.collectAsState()
    val subjectsList by viewModel.subjects.collectAsState()
    val customPeriods by viewModel.customPeriods.collectAsState()

    val context = LocalContext.current
    var showAddExtraDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    var selectedExtraSubjectId by remember { mutableStateOf<Int?>(null) }
    var selectedExtraPeriodId by remember { mutableStateOf<Int?>(null) }

    val isToday = selectedDate == LocalDate.now()

    LaunchedEffect(showAddExtraDialog) {
        if (showAddExtraDialog) {
            if (subjectsList.isNotEmpty()) selectedExtraSubjectId = subjectsList.first().id
            if (customPeriods.isNotEmpty()) selectedExtraPeriodId = customPeriods.first().id
        }
    }

    val formattedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isToday) "Today's Schedule" else "Schedule",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!isToday) {
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = Color(0xFFBA68C8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    if (!isToday) {
                        TextButton(
                            onClick = { viewModel.selectDate(LocalDate.now()) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA68C8))
                        ) {
                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Overall Percentage Badge
                    overallStats?.let { stats ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%.2f%%", stats.percentage),
                                    fontWeight = FontWeight.Bold,
                                    color = if (stats.percentage >= targetPct) Color(0xFF4CAF50) else Color(0xFFE91E63),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "|",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${targetPct.toInt()}%",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showAddExtraDialog = true },
                        modifier = Modifier.testTag("add_extra_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Extra Class", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Day Actions (Moved to Top of Screen, Compact & Modern Pill UI)
            if (todaySubjects.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Full Day Attended Pill
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.markFullDayStatus("ATTENDED")
                                    android.widget.Toast.makeText(context, "Full day marked as Attended!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .testTag("btn_full_day_attended"),
                            color = Color(0xFF1B3822),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Full Attended",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC8E6C9),
                                    maxLines = 1
                                )
                            }
                        }

                        // Full Day Leave Pill
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showLeaveDialog = true
                                }
                                .testTag("btn_full_day_leave"),
                            color = Color(0xFF382314),
                            border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Full Leave",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE0B2),
                                    maxLines = 1
                                )
                            }
                        }

                        // Compact Reset Pill
                        Surface(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.clearFullDayAttendance()
                                    android.widget.Toast.makeText(context, "Cleared attendance for this day", android.widget.Toast.LENGTH_SHORT).show()
                                },
                            color = Color(0xFF1B1F28),
                            border = BorderStroke(1.dp, Color(0xFF2E384D)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }
            }

            // Date Navigation Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateDate(-1) }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Day", tint = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (totalBunked > 0) Color(0xFFE91E63) else Color(0xFF4CAF50))
                            )
                            Text(
                                text = "Bunked total: $totalBunked classes",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.navigateDate(1) }) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", tint = Color.Gray)
                    }
                }
            }

            // Subject cards listed today
            if (todaySubjects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📅",
                                fontSize = 48.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "No Scheduled Classes Today",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "You haven't set up a timetable slot for this day or added any extra classes yet.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onNavigateToSubjects,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Configure Timetable")
                            }
                        }
                    }
                }
            } else {
                items(todaySubjects, key = { itemState -> itemState.id }) { itemState ->
                    TodaySubjectCard(
                        itemState = itemState,
                        targetPct = targetPct,
                        isToday = isToday,
                        onMark = { status ->
                            if (itemState.isExtra) {
                                itemState.record?.let { rec ->
                                    viewModel.updateExtraAttendance(rec.id, status)
                                }
                            } else {
                                itemState.slot?.let {
                                    viewModel.markRegularAttendance(itemState.subject.id, it.id, status)
                                }
                            }
                        },
                        onClear = {
                            if (itemState.isExtra) {
                                itemState.record?.let { rec ->
                                    viewModel.clearExtraAttendanceById(rec.id)
                                }
                            } else {
                                itemState.slot?.let {
                                    viewModel.clearRegularAttendance(itemState.subject.id, it.id)
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add extra class dialog - One-tap subject select without asking for time slot or period
    if (showAddExtraDialog) {
        AlertDialog(
            onDismissRequest = { showAddExtraDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text("Add Extra Class", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                if (subjectsList.isEmpty()) {
                    Text("Please add subjects first in the 'Subjects' section before adding extra classes.", color = Color.Gray)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select subject to add an extra class today:", color = Color.LightGray, fontSize = 13.sp)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            items(subjectsList, key = { it.id }) { subj ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF262C38),
                                    border = BorderStroke(1.dp, Color(0xFF384252)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.markExtraAttendance(
                                                subjectId = subj.id,
                                                status = "ATTENDED",
                                                extraPeriodName = "Extra Class",
                                                extraStartTime = null,
                                                extraEndTime = null
                                            )
                                            showAddExtraDialog = false
                                            android.widget.Toast.makeText(context, "Added extra class: ${subj.name}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = subj.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF4F46E5)
                                        ) {
                                            Text(
                                                text = "+ Add",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddExtraDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Full day leave confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text("Full Day Leave", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "How would you like to record leave for this day?",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.markFullDayStatus("OFF")
                            showLeaveDialog = false
                            android.widget.Toast.makeText(context, "Full day marked as Off / Leave", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mark as Off / Holiday (Exempt)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.markFullDayStatus("MISSED")
                            showLeaveDialog = false
                            android.widget.Toast.makeText(context, "Full day marked as Absent / Missed", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        border = BorderStroke(1.dp, Color(0xFFEF5350)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mark as Missed / Absent", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TodaySubjectCard(
    itemState: TodaySubjectState,
    targetPct: Double,
    isToday: Boolean,
    onMark: (String) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232936)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upper Area: Stats ring and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Attendance Ring
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progressValue = itemState.stats.percentage / 100.0
                    val barColor = if (itemState.stats.percentage >= targetPct) Color(0xFF388E3C) else Color(0xFFD32F2F)
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF1E1E1E),
                            style = Stroke(width = 5.dp.toPx())
                        )
                        drawArc(
                            color = barColor,
                            startAngle = -90f,
                            sweepAngle = (progressValue * 360f).toFloat(),
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx())
                        )
                    }
                    Text(
                        text = "${itemState.stats.percentage.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Subject Title & Estimate
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = itemState.subject.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (itemState.slot != null) {
                        Text(
                            text = "${itemState.slot.periodName} (${itemState.slot.startTime} - ${itemState.slot.endTime})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else if (itemState.isExtra) {
                        val pName = itemState.record?.extraPeriodName ?: "Extra Class"
                        val pTimeStr = if (itemState.record?.extraStartTime != null) {
                            " (${itemState.record.extraStartTime} - ${itemState.record.extraEndTime})"
                        } else {
                            ""
                        }
                        Text(
                            text = "$pName$pTimeStr",
                            fontSize = 12.sp,
                            color = Color(0xFFBA68C8)
                        )
                    }
                    Text(
                        text = itemState.stats.infoText,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Countdown Display - ONLY shown for today's schedule!
                    if (isToday) {
                        if (itemState.slot != null) {
                            PeriodCountdownTimer(
                                startTimeStr = itemState.slot.startTime,
                                endTimeStr = itemState.slot.endTime
                            )
                        } else if (itemState.isExtra && itemState.record != null) {
                            PeriodCountdownTimer(
                                startTimeStr = itemState.record.extraStartTime,
                                endTimeStr = itemState.record.extraEndTime
                            )
                        }
                    }
                }
            }

            // Uniform Daily status controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentStatus = itemState.record?.status
                
                ElegantStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Attended",
                    isSelected = currentStatus == "ATTENDED",
                    selectedColor = Color(0xFF2E7D32),
                    selectedBgColor = Color(0xFF1B5E20).copy(alpha = 0.3f),
                    onClick = { if (currentStatus != "ATTENDED") onMark("ATTENDED") }
                )
                ElegantStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Missed",
                    isSelected = currentStatus == "MISSED",
                    selectedColor = Color(0xFFD32F2F),
                    selectedBgColor = Color(0xFFB71C1C).copy(alpha = 0.3f),
                    onClick = { if (currentStatus != "MISSED") onMark("MISSED") }
                )
                ElegantStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Off",
                    isSelected = currentStatus == "OFF",
                    selectedColor = Color(0xFFFBC02D),
                    selectedBgColor = Color(0xFFF9A825).copy(alpha = 0.3f),
                    onClick = { if (currentStatus != "OFF") onMark("OFF") }
                )
                ElegantStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Clear",
                    isSelected = currentStatus == null,
                    selectedColor = Color(0xFFB0BEC5),
                    selectedBgColor = Color(0xFF455A64).copy(alpha = 0.3f),
                    onClick = { if (currentStatus != null) onClear() }
                )
            }
        }
    }
}

@Composable
fun ElegantStatusButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    selectedBgColor: Color,
    onClick: () -> Unit
) {
        Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        color = if (isSelected) selectedBgColor else Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, selectedColor.copy(alpha = 0.5f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else Color.Gray
            )
        }
    }
}

@Composable
fun PeriodCountdownTimer(startTimeStr: String?, endTimeStr: String?) {
    if (startTimeStr == null || endTimeStr == null) return

    var remainingText by remember { mutableStateOf("") }
    var activeStateColor by remember { mutableStateOf(Color.Gray) }

    LaunchedEffect(startTimeStr, endTimeStr) {
        val parseTime: (String) -> java.time.LocalTime = { timeStr ->
            val upper = timeStr.uppercase(java.util.Locale.US).trim()
            var res: java.time.LocalTime? = null
            val patterns = listOf("hh:mm a", "h:mm a", "HH:mm", "hh:mma", "h:mma")
            for (p in patterns) {
                try {
                    val f = java.time.format.DateTimeFormatter.ofPattern(p, java.util.Locale.US)
                    res = java.time.LocalTime.parse(upper, f)
                    break
                } catch (e: Exception) {}
            }
            res ?: throw Exception("Unparseable time: $timeStr")
        }

        try {
            val startLocal = parseTime(startTimeStr)
            val endLocal = parseTime(endTimeStr)

            while (true) {
                val now = java.time.LocalTime.now()
                if (now.isBefore(startLocal)) {
                    val diffSec = java.time.Duration.between(now, startLocal).seconds
                    val hours = diffSec / 3600
                    val minutes = (diffSec % 3600) / 60
                    val seconds = diffSec % 60
                    remainingText = String.format(java.util.Locale.US, "Starts in: %02d:%02d:%02d", hours, minutes, seconds)
                    activeStateColor = Color(0xFF4FA6E5)
                } else if (now.isAfter(endLocal)) {
                    remainingText = "Ended"
                    activeStateColor = Color.Gray
                } else {
                    val diffSec = java.time.Duration.between(now, endLocal).seconds
                    val hours = diffSec / 3600
                    val minutes = (diffSec % 3600) / 60
                    val seconds = diffSec % 60
                    remainingText = String.format(java.util.Locale.US, "Ends in: %02d:%02d:%02d (Ongoing)", hours, minutes, seconds)
                    activeStateColor = Color(0xFF4CAF50)
                }
                kotlinx.coroutines.delay(1000L)
            }
        } catch (e: Exception) {
            remainingText = "$startTimeStr - $endTimeStr"
            activeStateColor = Color.Gray
        }
    }

    if (remainingText.isNotEmpty()) {
        Text(
            text = remainingText,
            color = activeStateColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
