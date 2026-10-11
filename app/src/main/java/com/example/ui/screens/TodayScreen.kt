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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
    var selectedExtraClassType by remember { mutableStateOf("Lecture") } // "Lecture" or "Lab"

    val isToday = selectedDate == LocalDate.now()

    LaunchedEffect(showAddExtraDialog) {
        if (showAddExtraDialog && subjectsList.isNotEmpty()) {
            selectedExtraSubjectId = subjectsList.first().id
            selectedExtraClassType = "Lecture"
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
                                val isSafe = stats.percentage >= targetPct
                                Text(
                                    text = String.format("%.1f%%", stats.percentage),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSafe) Color(0xFF4CAF50) else Color(0xFFE91E63)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "/ ${targetPct.toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Top Action Bar: Add Extra Class and Full Leave
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (subjectsList.isEmpty()) {
                                android.widget.Toast.makeText(context, "Add subjects first in Stats tab", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                showAddExtraDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_top_extra_class"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Extra Class", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_top_full_leave"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full Leave", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Date Navigation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateDate(-1) },
                            modifier = Modifier.testTag("prev_day_button")
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Day", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                val now = selectedDate
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        viewModel.selectDate(LocalDate.of(year, month + 1, dayOfMonth))
                                    },
                                    now.year,
                                    now.monthValue - 1,
                                    now.dayOfMonth
                                ).show()
                            }
                        ) {
                            Text(
                                text = formattedDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isToday) "Today" else "Tap to change date",
                                fontSize = 11.sp,
                                color = if (isToday) Color(0xFF4CAF50) else Color.Gray,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateDate(1) },
                            modifier = Modifier.testTag("next_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next Day", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Quick Mark All Actions on the Screen
            if (todaySubjects.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.markFullDayStatus("ATTENDED") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        }
                        Button(
                            onClick = { viewModel.markFullDayStatus("MISSED") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("All Missed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
                        }
                        Button(
                            onClick = { viewModel.clearFullDayAttendance() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        }
                    }
                }
            }

            // Empty state placeholder
            if (todaySubjects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🎉", fontSize = 44.sp)
                            Text(
                                text = "No Classes Scheduled Today",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Enjoy your day off, or add an extra class above.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // List of Today's Scheduled and Extra Classes
            items(todaySubjects, key = { it.id }) { itemState ->
                TodaySubjectCard(
                    itemState = itemState,
                    isToday = isToday,
                    targetPct = targetPct,
                    onMark = { status ->
                        if (itemState.isExtra && itemState.record != null) {
                            viewModel.updateExtraAttendance(itemState.record.id, status)
                        } else if (itemState.slot != null) {
                            viewModel.markRegularAttendance(itemState.subject.id, itemState.slot.id, status)
                        }
                    },
                    onClear = {
                        if (itemState.isExtra && itemState.record != null) {
                            viewModel.clearExtraAttendanceById(itemState.record.id)
                        } else if (itemState.slot != null) {
                            viewModel.clearRegularAttendance(itemState.subject.id, itemState.slot.id)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add Extra Class Dialog
    if (showAddExtraDialog) {
        val currentExtraSubj = subjectsList.find { it.id == selectedExtraSubjectId } ?: subjectsList.firstOrNull()

        AlertDialog(
            onDismissRequest = { showAddExtraDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text("Add Extra Class", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select course subject to add:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(subjectsList, key = { it.id }) { subj ->
                            val isSelected = selectedExtraSubjectId == subj.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF374151) else Color(0xFF262C38),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFF384252)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExtraSubjectId = subj.id
                                        if (!subj.isLabIntegrated) {
                                            selectedExtraClassType = "Lecture"
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        if (subj.isLabIntegrated) {
                                            Text(
                                                text = "Lab Integrated",
                                                color = Color(0xFFA5B4FC),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedExtraSubjectId = subj.id
                                            if (!subj.isLabIntegrated) {
                                                selectedExtraClassType = "Lecture"
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5), unselectedColor = Color.Gray)
                                    )
                                }
                            }
                        }
                    }

                    // Component Selection: ONLY shown if selected subject is Lab Integrated!
                    if (currentExtraSubj?.isLabIntegrated == true) {
                        Text(
                            text = "Component for Lab Integrated Paper:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedExtraClassType == "Lecture") Color(0xFF0284C7) else Color(0xFF262C38),
                                border = BorderStroke(1.dp, if (selectedExtraClassType == "Lecture") Color(0xFF38BDF8) else Color.Gray),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedExtraClassType = "Lecture" }
                            ) {
                                Text(
                                    text = "📖 Lecture",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedExtraClassType == "Lab") Color(0xFF7C3AED) else Color(0xFF262C38),
                                border = BorderStroke(1.dp, if (selectedExtraClassType == "Lab") Color(0xFFA78BFA) else Color.Gray),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedExtraClassType = "Lab" }
                            ) {
                                Text(
                                    text = "🧪 Lab",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val subj = currentExtraSubj
                        if (subj != null) {
                            val compLabel = if (subj.isLabIntegrated && selectedExtraClassType == "Lab") "Lab Extra Class" else "Extra Class"
                            viewModel.markExtraAttendance(
                                subjectId = subj.id,
                                status = "ATTENDED",
                                extraPeriodName = compLabel,
                                extraStartTime = null,
                                extraEndTime = null
                            )
                            showAddExtraDialog = false
                            android.widget.Toast.makeText(context, "Added $compLabel: ${subj.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("+ Add Extra Class", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExtraDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Full day leave confirmation dialog (Restored original options including All Present, Day Off, and Leave)
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = Color(0xFF1E222B),
            title = {
                Text(
                    text = "Full Day Status",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select daily record action for $formattedDate:",
                        color = Color(0xFFD1D5DB),
                        fontSize = 14.sp
                    )

                    // 1. Mark All Present (Attended)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF14532D),
                        border = BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markFullDayStatus("ATTENDED")
                                showLeaveDialog = false
                                android.widget.Toast.makeText(context, "All classes marked as Present", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Mark All Present",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF0FDF4)
                                )
                                Text(
                                    text = "All scheduled periods attended today",
                                    fontSize = 11.sp,
                                    color = Color(0xFFBBF7D0)
                                )
                            }
                        }
                    }

                    // 2. Day Off (College Holiday)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF78350F),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markFullDayStatus("OFF")
                                showLeaveDialog = false
                                android.widget.Toast.makeText(context, "Day marked as Off (no % penalty)", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Day Off (College / Holiday)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFFBEB)
                                )
                                Text(
                                    text = "Excluded from total percentage calculation",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }
                    }

                    // 3. Personal Leave (Counted as Missed)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7F1D1D),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markFullDayStatus("MISSED")
                                showLeaveDialog = false
                                android.widget.Toast.makeText(context, "Day marked as Leave (counted as missed)", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Full Day Leave (Missed)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFEF2F2)
                                )
                                Text(
                                    text = "Counted as bunked/missed lectures",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFECACA)
                                )
                            }
                        }
                    }

                    // 4. Reset / Clear
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1F2937),
                        border = BorderStroke(1.dp, Color(0xFF4B5563)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.clearFullDayAttendance()
                                showLeaveDialog = false
                                android.widget.Toast.makeText(context, "Reset today's records", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Reset All Records",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Clear status for all periods today",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TodaySubjectCard(
    itemState: TodaySubjectState,
    isToday: Boolean,
    targetPct: Double,
    onMark: (String) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Gauge
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progressValue = itemState.stats.percentage / 100.0
                    val barColor = if (itemState.stats.percentage >= targetPct) Color(0xFF4CAF50) else Color(0xFFE91E63)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF2C323F),
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
                        text = String.format("%.0f%%", itemState.stats.percentage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Subject Title & Component
                Column(modifier = Modifier.weight(1f)) {
                    val isLabSlot = itemState.slot?.classType?.equals("Lab", ignoreCase = true) == true ||
                            (itemState.isExtra && itemState.record?.extraPeriodName?.contains("Lab", ignoreCase = true) == true)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = itemState.subject.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        // Pill badge: Lecture or Lab
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isLabSlot) Color(0xFF7C3AED).copy(alpha = 0.25f) else Color(0xFF0284C7).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, if (isLabSlot) Color(0xFF7C3AED) else Color(0xFF0284C7))
                        ) {
                            Text(
                                text = if (isLabSlot) "LAB" else "LECTURE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLabSlot) Color(0xFFA78BFA) else Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (itemState.slot != null) {
                        Text(
                            text = "${itemState.slot.periodName} (${itemState.slot.startTime} - ${itemState.slot.endTime})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else if (itemState.isExtra) {
                        val pName = itemState.record?.extraPeriodName ?: "Extra Class"
                        Text(
                            text = pName,
                            fontSize = 12.sp,
                            color = Color(0xFFBA68C8)
                        )
                    }

                    Text(
                        text = "Total Subject Attendance: ${String.format("%.1f%%", itemState.stats.percentage)} (${itemState.stats.attendedCount}/${itemState.stats.totCount})",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }

            // Attendance Action Buttons
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) selectedBgColor else Color(0xFF262C38),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) selectedColor else Color(0xFF384252)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.Gray
            )
        }
    }
}
