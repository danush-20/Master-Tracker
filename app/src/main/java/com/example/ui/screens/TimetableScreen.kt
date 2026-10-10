package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.data.TimetableSlot
import com.example.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val timetableSlots by viewModel.timetableSlots.collectAsState()
    val subjectsList by viewModel.subjects.collectAsState()

    var selectedDayIndex by remember { mutableStateOf(1) } // 1 = Mon, ..., 6 = Sat
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    var showAddSlotDialog by remember { mutableStateOf(false) }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    var inputPeriodName by remember { mutableStateOf("Period 1") }
    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }

    val customPeriods by viewModel.customPeriods.collectAsState()
    var selectedPeriodId by remember { mutableStateOf<Int?>(null) }

    // When dialog shows up, initialize selectedPeriodId if empty
    LaunchedEffect(showAddSlotDialog) {
        if (showAddSlotDialog && customPeriods.isNotEmpty()) {
            selectedPeriodId = customPeriods.first().id
        }
    }

    val slotsForSelectedDay = remember(timetableSlots, selectedDayIndex) {
        timetableSlots.filter { it.dayOfWeek == selectedDayIndex }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weekly Plan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212)),
                actions = {
                    IconButton(
                        onClick = {
                            slotToEdit = null
                            if (subjectsList.isNotEmpty()) {
                                selectedSubjectId = subjectsList.first().id
                                showAddSlotDialog = true
                            } else {
                                showAddSlotDialog = true
                            }
                        },
                        modifier = Modifier.testTag("add_timetable_slot")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Timetable Slot", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Day selection bar (Mon - Sat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                daysOfWeek.forEachIndexed { i, dayName ->
                    val dayNum = i + 1
                    val isSelected = selectedDayIndex == dayNum
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedDayIndex = dayNum },
                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = dayName,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timetable lists
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (slotsForSelectedDay.isEmpty()) {
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
                                    text = "🗓️",
                                    fontSize = 44.sp
                                )
                                Text(
                                    text = "Empty Schedule",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "You don't have any classes scheduled for this day yet. Click the '+' icon at the top to add one.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(slotsForSelectedDay, key = { it.id }) { slot ->
                        val subject = subjectsList.find { it.id == slot.subjectId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Period",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = subject?.name ?: "Unknown Subject",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${slot.periodName} (${slot.startTime} - ${slot.endTime})",
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = { 
                                        slotToEdit = slot 
                                        showAddSlotDialog = true 
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Slot",
                                            tint = Color(0xFF4F46E5)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteTimetableSlot(slot) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Slot",
                                            tint = Color(0xFFE91E63)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add slot dialog
    if (showAddSlotDialog) {
        var inputPeriodName by remember(slotToEdit) { mutableStateOf(slotToEdit?.periodName ?: "Period 1") }
        var slotEditStartTime by remember(slotToEdit) { mutableStateOf(slotToEdit?.startTime ?: "09:00 AM") }
        var slotEditEndTime by remember(slotToEdit) { mutableStateOf(slotToEdit?.endTime ?: "10:00 AM") }
        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text(if (slotToEdit != null) "Edit Timetable Slot" else "Add Timetable Slot", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                if (subjectsList.isEmpty()) {
                    Text("Before defining a weekly schedule, you must add subjects in the Subj tab first.", color = Color.Gray)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Day: ${daysOfWeek[selectedDayIndex - 1]}", color = Color.LightGray, fontWeight = FontWeight.Medium)
                        
                        Text("Course:", color = Color.Gray, fontSize = 12.sp)
                        // Dropdown Selector simple version
                        var expanded by remember { mutableStateOf(false) }
                        val currentSubject = subjectsList.find { it.id == selectedSubjectId } ?: subjectsList.first()
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { expanded = true },
                                color = Color(0xFF2C323F),
                                border = BorderStroke(1.dp, Color.Gray)
                            ) {
                                Text(
                                    currentSubject.name,
                                    color = Color.White,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF2C323F))
                            ) {
                                subjectsList.forEach { subj ->
                                    DropdownMenuItem(
                                        text = { Text(subj.name, color = Color.White) },
                                        onClick = {
                                            selectedSubjectId = subj.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Custom Period Selection (Auto-fill)
                        Text("Auto-fill from Custom Periods:", color = Color.Gray, fontSize = 12.sp)
                        if (customPeriods.isEmpty()) {
                            Text(
                                "No Custom Periods configured in Settings.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            var periodExpanded by remember { mutableStateOf(false) }
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { periodExpanded = true },
                                    color = Color(0xFF2C323F),
                                    border = BorderStroke(1.dp, Color.Gray)
                                ) {
                                    Text("Tap to select and auto-fill", color = Color.White, modifier = Modifier.padding(14.dp))
                                }
                                DropdownMenu(
                                    expanded = periodExpanded,
                                    onDismissRequest = { periodExpanded = false },
                                    modifier = Modifier.background(Color(0xFF2C323F))
                                ) {
                                    customPeriods.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(p.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text("${p.startTime} - ${p.endTime}", color = Color.Gray, fontSize = 12.sp)
                                                }
                                            },
                                            onClick = {
                                                inputPeriodName = p.name
                                                slotEditStartTime = p.startTime
                                                slotEditEndTime = p.endTime
                                                periodExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputPeriodName,
                            onValueChange = { inputPeriodName = it },
                            label = { Text("Period Name", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start:", color = Color.Gray, fontSize = 12.sp)
                                Surface(
                                    onClick = { showTimePicker(context, slotEditStartTime) { slotEditStartTime = it } },
                                    color = Color(0xFF2C323F),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(slotEditStartTime, color = Color.White, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End:", color = Color.Gray, fontSize = 12.sp)
                                Surface(
                                    onClick = { showTimePicker(context, slotEditEndTime) { slotEditEndTime = it } },
                                    color = Color(0xFF2C323F),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(slotEditEndTime, color = Color.White, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (subjectsList.isNotEmpty()) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        onClick = {
                            val subId = selectedSubjectId ?: subjectsList.first().id
                            if (slotToEdit != null) {
                                viewModel.updateTimetableSlot(
                                    slotToEdit!!.copy(
                                        subjectId = subId,
                                        startTime = slotEditStartTime,
                                        periodName = inputPeriodName,
                                        endTime = slotEditEndTime
                                    )
                                )
                            } else {
                                viewModel.addTimetableSlot(
                                    dayOfWeek = selectedDayIndex,
                                    subjectId = subId,
                                    startTime = slotEditStartTime,
                                    periodName = inputPeriodName,
                                    endTime = slotEditEndTime
                                )
                            }
                            showAddSlotDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
