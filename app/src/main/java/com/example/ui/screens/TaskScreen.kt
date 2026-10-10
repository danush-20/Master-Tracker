package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.viewmodel.AttendanceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val context = LocalContext.current

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "COMPLETED"
    var selectedPriorityFilter by remember { mutableStateOf("ALL") } // "ALL", "HIGH", "MEDIUM", "LOW"
    var selectedSubjectIdFilter by remember { mutableStateOf<Int?>(null) } // null = All Subjects

    // Calculations
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val activeTasks = totalTasks - completedTasks
    val highPriorityActive = tasks.count { !it.isCompleted && it.priority == "HIGH" }
    val completionRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    // Filtered tasks
    val filteredTasks = remember(tasks, searchQuery, selectedFilter, selectedPriorityFilter, selectedSubjectIdFilter) {
        tasks.filter { task ->
            // Search query filter
            val matchesQuery = searchQuery.isBlank() ||
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.note.contains(searchQuery, ignoreCase = true)

            // Status filter
            val matchesStatus = when (selectedFilter) {
                "ACTIVE" -> !task.isCompleted
                "COMPLETED" -> task.isCompleted
                else -> true
            }

            // Priority filter
            val matchesPriority = when (selectedPriorityFilter) {
                "HIGH" -> task.priority == "HIGH"
                "MEDIUM" -> task.priority == "MEDIUM"
                "LOW" -> task.priority == "LOW"
                else -> true
            }

            // Subject filter
            val matchesSubject = selectedSubjectIdFilter == null || task.subjectId == selectedSubjectIdFilter

            matchesQuery && matchesStatus && matchesPriority && matchesSubject
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Study & Course Tasks",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$activeTasks active • $completedTasks finished",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    if (completedTasks > 0) {
                        IconButton(
                            onClick = { showClearCompletedDialog = true },
                            modifier = Modifier.testTag("clear_completed_tasks_btn")
                        ) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = "Clear Completed",
                                tint = Color(0xFFBA68C8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showAddTaskDialog = true
                },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
            }
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
            // Productivity Summary Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF311B92),
                                        Color(0xFF512DA8),
                                        Color(0xFF7B1FA2)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TASK COMPLETION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xB3FFFFFF),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$completedTasks of $totalTasks Finished",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    if (highPriorityActive > 0) {
                                        Text(
                                            text = "⚠️ $highPriorityActive urgent task(s) remaining",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF8A80)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${(completionRatio * 100).toInt()}%",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { completionRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00E676),
                                trackColor = Color(0x33FFFFFF),
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search assignments, notes...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("task_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFBA68C8),
                        unfocusedBorderColor = Color(0xFF282F3B),
                        focusedContainerColor = Color(0xFF161A22),
                        unfocusedContainerColor = Color(0xFF161A22),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All ($totalTasks)", fontSize = 12.sp) },
                        colors = ChipColors(selectedFilter == "ALL")
                    )
                    FilterChip(
                        selected = selectedFilter == "ACTIVE",
                        onClick = { selectedFilter = "ACTIVE" },
                        label = { Text("Active ($activeTasks)", fontSize = 12.sp) },
                        colors = ChipColors(selectedFilter == "ACTIVE")
                    )
                    FilterChip(
                        selected = selectedFilter == "COMPLETED",
                        onClick = { selectedFilter = "COMPLETED" },
                        label = { Text("Done ($completedTasks)", fontSize = 12.sp) },
                        colors = ChipColors(selectedFilter == "COMPLETED")
                    )
                }
            }

            // Priority & Subject Filters Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedPriorityFilter == "ALL",
                            onClick = { selectedPriorityFilter = "ALL" },
                            label = { Text("Any Priority", fontSize = 11.sp) },
                            colors = ChipColors(selectedPriorityFilter == "ALL")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPriorityFilter == "HIGH",
                            onClick = { selectedPriorityFilter = if (selectedPriorityFilter == "HIGH") "ALL" else "HIGH" },
                            label = { Text("🔴 High Priority", fontSize = 11.sp) },
                            colors = ChipColors(selectedPriorityFilter == "HIGH")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPriorityFilter == "MEDIUM",
                            onClick = { selectedPriorityFilter = if (selectedPriorityFilter == "MEDIUM") "ALL" else "MEDIUM" },
                            label = { Text("🟡 Medium", fontSize = 11.sp) },
                            colors = ChipColors(selectedPriorityFilter == "MEDIUM")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPriorityFilter == "LOW",
                            onClick = { selectedPriorityFilter = if (selectedPriorityFilter == "LOW") "ALL" else "LOW" },
                            label = { Text("🔵 Low", fontSize = 11.sp) },
                            colors = ChipColors(selectedPriorityFilter == "LOW")
                        )
                    }

                    // Subjects filter chips
                    subjects.forEach { subj ->
                        item {
                            FilterChip(
                                selected = selectedSubjectIdFilter == subj.id,
                                onClick = {
                                    selectedSubjectIdFilter = if (selectedSubjectIdFilter == subj.id) null else subj.id
                                },
                                label = { Text(subj.name, fontSize = 11.sp) },
                                colors = ChipColors(selectedSubjectIdFilter == subj.id)
                            )
                        }
                    }
                }
            }

            // Task List
            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                        border = BorderStroke(1.dp, Color(0xFF232936)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🎯", fontSize = 40.sp, textAlign = TextAlign.Center)
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching tasks found" else "All Tasks Caught Up!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try a different search keyword" else "Add homework assignments, lab prep, or test review items.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    editingTask = null
                                    showAddTaskDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF121212)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Create First Task", fontWeight = FontWeight.Bold, color = Color(0xFF121212))
                            }
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val subjectName = subjects.find { it.id == task.subjectId }?.name ?: "General"
                    TaskItemRow(
                        task = task,
                        subjectName = subjectName,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onEdit = {
                            editingTask = task
                            showAddTaskDialog = true
                        },
                        onDelete = {
                            viewModel.deleteTask(task)
                            Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Clear completed tasks dialog
    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text("Clear Completed Tasks?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will permanently remove all $completedTasks completed tasks from your list.", color = Color.LightGray)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedTasks()
                        showClearCompletedDialog = false
                        Toast.makeText(context, "Completed tasks cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Done", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Add / Edit Task Dialog
    if (showAddTaskDialog) {
        var selectedTag by remember {
            mutableStateOf(
                Regex("""\[TAG:([^\]]+)\]""").find(editingTask?.note ?: "")?.groupValues?.get(1)
                    ?: Regex("""\[TAG:([^\]]+)\]""").find(editingTask?.title ?: "")?.groupValues?.get(1)
                    ?: Regex("""\[([^\]]+)\]""").find(editingTask?.title ?: "")?.groupValues?.get(1)
                    ?: Regex("""\[([^\]]+)\]""").find(editingTask?.note ?: "")?.groupValues?.get(1)
            )
        }
        var title by remember {
            mutableStateOf(
                editingTask?.title?.replace(Regex("""\[[^\]]+\]\s*"""), "")?.trim() ?: ""
            )
        }
        var subjectId by remember { mutableStateOf(editingTask?.subjectId) }
        var priority by remember { mutableStateOf(editingTask?.priority ?: "MEDIUM") }
        var fuzzyPeriod by remember { mutableStateOf(editingTask?.fuzzyPeriod ?: "Today") }
        var note by remember {
            mutableStateOf(
                editingTask?.note?.replace(Regex("""\[TAG:[^\]]+\]"""), "")?.replace(Regex("""\[[^\]]+\]\s*"""), "")?.trim() ?: ""
            )
        }

        val priorities = listOf("HIGH", "MEDIUM", "LOW")
        val quickDuePresets = listOf("Today", "Tomorrow", "In 3 Days", "This Weekend", "Next Week", "No Due Date")
        val quickTags = listOf("Assignment", "Exam Prep", "Lab Work", "Homework", "Project", "Submission")

        AlertDialog(
            onDismissRequest = {
                showAddTaskDialog = false
                editingTask = null
            },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = if (editingTask != null) "Edit Task" else "Add New Task",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title input
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Description") },
                            placeholder = { Text("e.g. Solve Physics Problem Set 4") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFBA68C8),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFFBA68C8),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_input_title")
                        )
                    }

                    // Quick category tag buttons
                    item {
                        Text("Tag / Category:", fontSize = 11.sp, color = Color.Gray)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            quickTags.forEach { tag ->
                                item {
                                    val isTagActive = selectedTag == tag
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isTagActive) Color(0xFF6366F1) else Color(0xFF2C323F),
                                        border = if (isTagActive) BorderStroke(1.dp, Color(0xFFA5B4FC)) else null,
                                        modifier = Modifier.clickable {
                                            selectedTag = if (isTagActive) null else tag
                                        }
                                    ) {
                                        Text(
                                            text = if (isTagActive) "✓ $tag" else tag,
                                            fontSize = 11.sp,
                                            fontWeight = if (isTagActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isTagActive) Color.White else Color.LightGray,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Priority selector
                    item {
                        Text("Priority", fontSize = 11.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            priorities.forEach { pri ->
                                val activeCol = when (pri) {
                                    "HIGH" -> Color(0xFFFF1744)
                                    "MEDIUM" -> Color(0xFFFFB300)
                                    else -> Color(0xFF29B6F6)
                                }
                                val isChosen = priority == pri

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) activeCol else Color(0xFF2C323F))
                                        .clickable { priority = pri }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pri,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.Black else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Subject selector
                    item {
                        Text("Course / Subject", fontSize = 11.sp, color = Color.Gray)
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val selectedSubjName = subjects.find { it.id == subjectId }?.name ?: "General (No Subject)"

                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dropdownExpanded = true },
                                color = Color(0xFF2C323F),
                                border = BorderStroke(1.dp, Color(0xFF384050))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = selectedSubjName, color = Color.White, fontSize = 13.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF2C323F))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("General (No Subject)", color = Color.White) },
                                    onClick = {
                                        subjectId = null
                                        dropdownExpanded = false
                                    }
                                )
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name, color = Color.White) },
                                        onClick = {
                                            subjectId = sub.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Due timing presets
                    item {
                        Text("Target / Due", fontSize = 11.sp, color = Color.Gray)
                        var periodExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { periodExpanded = true },
                                color = Color(0xFF2C323F),
                                border = BorderStroke(1.dp, Color(0xFF384050))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = fuzzyPeriod, color = Color.White, fontSize = 13.sp)
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = periodExpanded,
                                onDismissRequest = { periodExpanded = false },
                                modifier = Modifier.background(Color(0xFF2C323F))
                            ) {
                                quickDuePresets.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p, color = Color.White) },
                                        onClick = {
                                            fuzzyPeriod = p
                                            periodExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Notes input
                    item {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Notes & Details") },
                            placeholder = { Text("Page numbers, requirements, reminders...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFF4F46E5),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = title.replace(Regex("""\[[^\]]+\]\s*"""), "").trim()
                        if (cleanTitle.isNotBlank()) {
                            val savedPeriod = if (fuzzyPeriod == "No Due Date") null else fuzzyPeriod
                            val baseNote = note.replace(Regex("""\[TAG:[^\]]+\]"""), "").replace(Regex("""\[[^\]]+\]\s*"""), "").trim()
                            val finalNote = if (selectedTag != null) {
                                if (baseNote.isNotBlank()) "$baseNote\n[TAG:$selectedTag]" else "[TAG:$selectedTag]"
                            } else {
                                baseNote
                            }

                            if (editingTask != null) {
                                viewModel.updateTaskDetails(
                                    editingTask!!.copy(
                                        title = cleanTitle,
                                        subjectId = subjectId,
                                        priority = priority,
                                        fuzzyPeriod = savedPeriod,
                                        note = finalNote
                                    )
                                )
                                Toast.makeText(context, "Task updated", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addTask(
                                    title = cleanTitle,
                                    subjectId = subjectId,
                                    priority = priority,
                                    fuzzyPeriod = savedPeriod,
                                    note = finalNote
                                )
                                Toast.makeText(context, "Task created", Toast.LENGTH_SHORT).show()
                            }
                            showAddTaskDialog = false
                            editingTask = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier.testTag("submit_task_btn")
                ) {
                    Text(if (editingTask != null) "Save" else "Add Task", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTaskDialog = false
                    editingTask = null
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TaskItemRow(
    task: Task,
    subjectName: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "HIGH" -> Color(0xFFFF1744)
        "MEDIUM" -> Color(0xFFFFB300)
        else -> Color(0xFF29B6F6)
    }

    val displayTag = remember(task) {
        Regex("""\[TAG:([^\]]+)\]""").find(task.note)?.groupValues?.get(1)
            ?: Regex("""\[TAG:([^\]]+)\]""").find(task.title)?.groupValues?.get(1)
            ?: Regex("""\[([^\]]+)\]""").find(task.title)?.groupValues?.get(1)
            ?: Regex("""\[([^\]]+)\]""").find(task.note)?.groupValues?.get(1)
    }
    val cleanTitle = remember(task) {
        task.title.replace(Regex("""\[[^\]]+\]\s*"""), "").trim()
    }
    val cleanNote = remember(task) {
        task.note.replace(Regex("""\[TAG:[^\]]+\]"""), "").replace(Regex("""\[[^\]]+\]\s*"""), "").trim()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) Color(0xFF14171E) else Color(0xFF1B1F28)
        ),
        border = BorderStroke(1.dp, if (task.priority == "HIGH" && !task.isCompleted) Color(0xFFFF1744).copy(alpha = 0.4f) else Color(0xFF282F3B)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Tag indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Completion checkmark
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle completion",
                    tint = if (task.isCompleted) Color(0xFF00E676) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (task.isCompleted) Color.Gray else Color.White,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tag badge
                    if (displayTag != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = displayTag,
                                fontSize = 10.sp,
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Subject tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF282F3B)
                    ) {
                        Text(
                            text = subjectName,
                            fontSize = 10.sp,
                            color = Color(0xFFB0BEC5),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }

                    // Priority tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 9.sp,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Due info badge
                    if (task.fuzzyPeriod != null) {
                        val dueText = task.fuzzyPeriod
                        val dueColor = when {
                            dueText.contains("Today", ignoreCase = true) -> Color(0xFFFFB74D)
                            dueText.contains("Tomorrow", ignoreCase = true) -> Color(0xFF81D4FA)
                            else -> Color.Gray
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF232936)
                        ) {
                            Text(
                                text = "⏰ $dueText",
                                fontSize = 10.sp,
                                color = dueColor,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                if (cleanNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cleanNote,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }

            // Action icons
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit task",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChipColors(isSelected: Boolean): SelectableChipColors {
    if (isSelected) {
        return FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF4F46E5).copy(alpha = 0.25f),
            labelColor = Color(0xFF818CF8),
            selectedContainerColor = Color(0xFF4F46E5).copy(alpha = 0.25f),
            selectedLabelColor = Color(0xFF818CF8)
        )
    } else {
        return FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF1E222B),
            labelColor = Color.LightGray,
            selectedContainerColor = Color(0xFF1E222B),
            selectedLabelColor = Color.LightGray
        )
    }
}
