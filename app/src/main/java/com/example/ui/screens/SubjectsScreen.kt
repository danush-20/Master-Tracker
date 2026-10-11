package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.SubjectStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val subjectsList by viewModel.subjects.collectAsState()
    val subjectStatsList by viewModel.subjectStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val targetPct by viewModel.targetPercentage.collectAsState()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var inputSubjectName by remember { mutableStateOf("") }
    var inputBaseAttended by remember { mutableStateOf("0") }
    var inputBaseMissed by remember { mutableStateOf("0") }
    var inputIsLabIntegrated by remember { mutableStateOf(false) }

    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var statsToEdit by remember { mutableStateOf<SubjectStats?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Subjects Overview", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212)),
                actions = {
                    IconButton(
                        onClick = { 
                            inputSubjectName = ""
                            inputBaseAttended = "0"
                            inputBaseMissed = "0"
                            inputIsLabIntegrated = false
                            showAddSubjectDialog = true 
                        },
                        modifier = Modifier.testTag("add_subject_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFF121212),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    inputSubjectName = ""
                    inputBaseAttended = "0"
                    inputBaseMissed = "0"
                    inputIsLabIntegrated = false
                    showAddSubjectDialog = true 
                },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall statistics card
            overallStats?.let { stats ->
                item {
                    Text(
                        text = "GLOBAL SUMMARY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Gauge
                            Box(
                                modifier = Modifier.size(68.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val progressValue = stats.percentage / 100.0
                                val barColor = if (stats.percentage >= targetPct) Color(0xFF4CAF50) else Color(0xFFE91E63)

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color(0xFF2C323F),
                                        style = Stroke(width = 6.dp.toPx())
                                    )
                                    drawArc(
                                        color = barColor,
                                        startAngle = -90f,
                                        sweepAngle = (progressValue * 360f).toFloat(),
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx())
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format("%.1f", stats.percentage),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .padding(vertical = 1.dp),
                                        color = Color.Gray,
                                        thickness = 1.dp
                                    )
                                    Text(
                                        text = "${targetPct.toInt()}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Overall Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overall Attendance",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stats.infoText,
                                    fontSize = 12.sp,
                                    color = if (stats.percentage >= targetPct) Color(0xFF4CAF50) else Color(0xFFE91E63),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Attended: ${stats.attendedCount}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "Bunked: ${stats.missedCount}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE91E63)
                                    )
                                    Text(
                                        text = "Total: ${stats.totCount}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subject Item List Header
            item {
                Text(
                    text = "COURSE SUBJECTS (${subjectsList.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Empty state placeholder
            if (subjectStatsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📚", fontSize = 44.sp)
                            Text("No Subjects Found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Click the '+' icon in the top right to start adding subjects.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Button(
                                onClick = { showAddSubjectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("Add Your First Subject")
                            }
                        }
                    }
                }
            }

            // Dynamic Subject Cards List
            items(subjectStatsList, key = { it.subject.id }) { stats ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Gauge for single subject
                            Box(
                                modifier = Modifier.size(68.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val progressValue = stats.percentage / 100.0
                                val barColor = if (stats.percentage >= targetPct) Color(0xFF4CAF50) else Color(0xFFE91E63)

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color(0xFF2C323F),
                                        style = Stroke(width = 6.dp.toPx())
                                    )
                                    drawArc(
                                        color = barColor,
                                        startAngle = -90f,
                                        sweepAngle = (progressValue * 360f).toFloat(),
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx())
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format("%.1f", stats.percentage),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .padding(vertical = 1.dp),
                                        color = Color.Gray,
                                        thickness = 1.dp
                                    )
                                    Text(
                                        text = "${targetPct.toInt()}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Stats descriptions and delete triggers
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1.0f)
                                    ) {
                                        Text(
                                            text = stats.subject.name,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (stats.subject.isLabIntegrated) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF4F46E5).copy(alpha = 0.25f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5))
                                            ) {
                                                Text(
                                                    text = "Lab Integrated",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFA5B4FC),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                inputSubjectName = stats.subject.name
                                                inputBaseAttended = stats.subject.baseAttended.toString()
                                                inputBaseMissed = stats.subject.baseMissed.toString()
                                                inputIsLabIntegrated = stats.subject.isLabIntegrated
                                                subjectToEdit = stats.subject
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Subject",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { subjectToDelete = stats.subject },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Subject",
                                                tint = Color(0xFFE91E63),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Bunk Tracking Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    val isSafe = stats.percentage >= targetPct
                                    val badgeBgColor = if (isSafe) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                    val badgeTextColor = if (isSafe) Color(0xFF81C784) else Color(0xFFE57373)
                                    val badgeIcon = if (isSafe) "✓" else "⚠️"
                                    
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBgColor,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = badgeIcon,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextColor
                                            )
                                            Text(
                                                text = if (isSafe) "Safe" else "Bunk Limit Alert",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeTextColor
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = stats.infoText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSafe) Color(0xFF4CAF50) else Color(0xFFE91E63)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))

                                // Detailed attendance itemization (Combined)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            statsToEdit = stats
                                            inputBaseAttended = stats.attendedCount.toString()
                                            inputBaseMissed = stats.missedCount.toString()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "Attended: ${stats.attendedCount}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Bunked: ${stats.missedCount}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE91E63),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (stats.offCount > 0) {
                                        Text(
                                            text = "Off: ${stats.offCount}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFFB300),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "Total classes: ${stats.totCount}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Stats",
                                        tint = Color.Gray.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // Lab Integrated Separate Breakdown Row in Stats
                        if (stats.subject.isLabIntegrated) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF282F3B), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Lecture stats
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Lecture",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Attended: ${stats.lectureAttended} | Bunked: ${stats.lectureMissed} (Tot: ${stats.lectureTot})",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }

                                // Lab stats
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF7C3AED).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Lab",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA78BFA),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Attended: ${stats.labAttended} | Bunked: ${stats.labMissed} (Tot: ${stats.labTot})",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add / Edit Subject dialog
    if (showAddSubjectDialog || subjectToEdit != null) {
        val isEdit = subjectToEdit != null
        AlertDialog(
            onDismissRequest = { 
                showAddSubjectDialog = false 
                subjectToEdit = null
            },
            containerColor = Color(0xFF1E1E1E),
            title = { Text(if (isEdit) "Edit Subject" else "Add New Subject", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputSubjectName,
                        onValueChange = { inputSubjectName = it },
                        label = { Text("Subject Name (e.g. IoT)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Lab Integrated Paper Checkbox
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF262C38),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF384252)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inputIsLabIntegrated = !inputIsLabIntegrated }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = inputIsLabIntegrated,
                                onCheckedChange = { inputIsLabIntegrated = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4F46E5),
                                    checkmarkColor = Color.White,
                                    uncheckedColor = Color.Gray
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lab integrated paper",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Generates separate Lecture & Lab slots in Schedule with combined attendance stats",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputBaseAttended,
                        onValueChange = { inputBaseAttended = it.filter { char -> char.isDigit() } },
                        label = { Text("Previous Attended Classes", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputBaseMissed,
                        onValueChange = { inputBaseMissed = it.filter { char -> char.isDigit() } },
                        label = { Text("Previous Bunked Classes", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    onClick = {
                        if (inputSubjectName.isNotBlank()) {
                            val attended = inputBaseAttended.toIntOrNull() ?: 0
                            val missed = inputBaseMissed.toIntOrNull() ?: 0
                            if (isEdit) {
                                viewModel.updateSubject(subjectToEdit!!.copy(
                                    name = inputSubjectName.trim(),
                                    baseAttended = attended,
                                    baseMissed = missed,
                                    isLabIntegrated = inputIsLabIntegrated
                                ))
                            } else {
                                viewModel.addSubject(
                                    name = inputSubjectName.trim(),
                                    baseAttended = attended,
                                    baseMissed = missed,
                                    isLabIntegrated = inputIsLabIntegrated
                                )
                            }
                            inputSubjectName = ""
                            inputBaseAttended = "0"
                            inputBaseMissed = "0"
                            inputIsLabIntegrated = false
                            showAddSubjectDialog = false
                            subjectToEdit = null
                        }
                    }
                ) {
                    Text(if (isEdit) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddSubjectDialog = false 
                    subjectToEdit = null
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Delete confirmation dialog
    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Delete Subject", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete '${subject.name}'? This will permanently remove its timetable slots and attendance history.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    onClick = {
                        viewModel.deleteSubject(subject)
                        subjectToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Edit cumulative stats dialog
    statsToEdit?.let { currentStats ->
        AlertDialog(
            onDismissRequest = { statsToEdit = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Edit Stats: ${currentStats.subject.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Update the total attended and bunked counts for this subject:",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = inputBaseAttended,
                        onValueChange = { inputBaseAttended = it.filter { char -> char.isDigit() } },
                        label = { Text("Total Attended Classes", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputBaseMissed,
                        onValueChange = { inputBaseMissed = it.filter { char -> char.isDigit() } },
                        label = { Text("Total Bunked (Missed) Classes", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    onClick = {
                        val newAttended = inputBaseAttended.toIntOrNull() ?: currentStats.attendedCount
                        val newMissed = inputBaseMissed.toIntOrNull() ?: currentStats.missedCount
                        val deltaAttended = newAttended - (currentStats.attendedCount - currentStats.subject.baseAttended)
                        val deltaMissed = newMissed - (currentStats.missedCount - currentStats.subject.baseMissed)

                        viewModel.updateSubject(currentStats.subject.copy(
                            baseAttended = if (deltaAttended >= 0) deltaAttended else 0,
                            baseMissed = if (deltaMissed >= 0) deltaMissed else 0
                        ))
                        statsToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { statsToEdit = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
