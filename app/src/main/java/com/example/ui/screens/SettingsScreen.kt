package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomPeriod
import com.example.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun showTimePicker(
    context: Context,
    initialTimeStr: String,
    onTimeSelected: (String) -> Unit
) {
    var hour = 9
    var minute = 0
    try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        val localTime = LocalTime.parse(initialTimeStr.uppercase(Locale.US), formatter)
        hour = localTime.hour
        minute = localTime.minute
    } catch (e: Exception) {
        // fallback
    }

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            val formattedTime = LocalTime.of(selectedHour, selectedMinute).format(formatter)
            onTimeSelected(formattedTime)
        },
        hour,
        minute,
        false
    ).show()
}

fun addMinutesToTime(timeStr: String, minutes: Int): String {
    try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        val localTime = LocalTime.parse(timeStr.uppercase(Locale.US), formatter)
        return localTime.plusMinutes(minutes.toLong()).format(formatter)
    } catch(e: Exception) {
        return timeStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val targetPct by viewModel.targetPercentage.collectAsState()
    var sliderValue by remember(targetPct) { mutableStateOf(targetPct.toFloat()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val json = viewModel.getExportDataJson()
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Export Successful!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Export Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(it)?.bufferedReader().use { reader ->
                        reader?.readText() ?: throw Exception("Cannot read file")
                    }
                    viewModel.importDataFromJson(json)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Import Successful!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Import Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // About Us / Developer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                border = BorderStroke(1.dp, Color(0xFF282F3B)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ABOUT US",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBA68C8),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "A Danush",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF4F46E5).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "MCA-SS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5B4FC),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "TechOps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "2025-27",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Target percentage configuration card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Attendance Goal",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Percentage",
                            color = Color.Gray
                        )
                        Text(
                            text = "${sliderValue.toInt()}%",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.updateTargetPercentage(sliderValue.toDouble())
                        },
                        valueRange = 50f..100f,
                        steps = 50,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4CAF50),
                            activeTrackColor = Color(0xFF4CAF50),
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("target_percentage_slider")
                    )
                }
            }

            // Custom Periods Management Card
            val customPeriods by viewModel.customPeriods.collectAsState()
            var showAddPeriodDialog by remember { mutableStateOf(false) }
            var periodToEdit by remember { mutableStateOf<CustomPeriod?>(null) }
            val defaultDuration by viewModel.defaultPeriodDuration.collectAsState()
            var inputDurationStr by remember(defaultDuration) { mutableStateOf(defaultDuration.toString()) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Standard Periods",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Define reusable class slots for the weekly plan",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { showAddPeriodDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ Add")
                        }
                    }

                    // Default Duration setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Period Duration:", color = Color.LightGray, fontSize = 14.sp)
                        OutlinedTextField(
                            value = inputDurationStr,
                            onValueChange = {
                                val filtered = it.filter { char -> char.isDigit() }
                                inputDurationStr = filtered
                                filtered.toIntOrNull()?.let { duration ->
                                    if (duration in 10..180) {
                                        viewModel.updateDefaultPeriodDuration(duration)
                                    }
                                }
                            },
                            label = { Text("Mins", color = Color.Gray) },
                            modifier = Modifier.width(90.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    if (customPeriods.isEmpty()) {
                        Text(
                            text = "No periods configured. Tap '+ Add' above to configure standard periods.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customPeriods.forEach { period ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2C323F))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(period.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${period.startTime} - ${period.endTime}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { periodToEdit = period },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Period",
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteCustomPeriod(period) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Period",
                                                tint = Color(0xFFE91E63),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Edit Custom Period Dialog
            if (periodToEdit != null) {
                var editName by remember(periodToEdit) { mutableStateOf(periodToEdit!!.name) }
                var editStart by remember(periodToEdit) { mutableStateOf(periodToEdit!!.startTime) }
                var editEnd by remember(periodToEdit) { mutableStateOf(periodToEdit!!.endTime) }

                AlertDialog(
                    onDismissRequest = { periodToEdit = null },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Edit Period", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
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
                                        onClick = {
                                            showTimePicker(context, editStart) { selected ->
                                                editStart = selected
                                                editEnd = addMinutesToTime(selected, defaultDuration)
                                            }
                                        },
                                        color = Color(0xFF2C323F),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(editStart, color = Color.White, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End:", color = Color.Gray, fontSize = 12.sp)
                                    Surface(
                                        onClick = {
                                            showTimePicker(context, editEnd) { selected -> editEnd = selected }
                                        },
                                        color = Color(0xFF2C323F),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(editEnd, color = Color.White, modifier = Modifier.padding(10.dp))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateCustomPeriod(
                                    periodToEdit!!.copy(name = editName.trim(), startTime = editStart, endTime = editEnd)
                                )
                                periodToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { periodToEdit = null }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }

            // Add Custom Period Dialog
            if (showAddPeriodDialog) {
                var newName by remember { mutableStateOf("Period ${(customPeriods.size + 1)}") }
                var newStart by remember { mutableStateOf("09:00 AM") }
                var newEnd by remember { mutableStateOf(addMinutesToTime("09:00 AM", defaultDuration)) }

                AlertDialog(
                    onDismissRequest = { showAddPeriodDialog = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Add Standard Period", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Period Name (e.g., Period 1)", color = Color.Gray) },
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
                                        onClick = {
                                            showTimePicker(context, newStart) { selected ->
                                                newStart = selected
                                                newEnd = addMinutesToTime(selected, defaultDuration)
                                            }
                                        },
                                        color = Color(0xFF2C323F),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(newStart, color = Color.White, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End:", color = Color.Gray, fontSize = 12.sp)
                                    Surface(
                                        onClick = {
                                            showTimePicker(context, newEnd) { selected -> newEnd = selected }
                                        },
                                        color = Color(0xFF2C323F),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(newEnd, color = Color.White, modifier = Modifier.padding(10.dp))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.addCustomPeriod(newName.trim(), newStart, newEnd)
                                    showAddPeriodDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPeriodDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }

            // Backup & Restore (Full export & import including all semester courses, grades, lab integrated papers)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Data Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Export all your course attendance, weekly timetables, and semester grades to a JSON backup, or restore a saved file.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("master_tracker_backup.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export Data", color = Color.White)
                        }

                        Button(
                            onClick = {
                                importLauncher.launch("application/json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F3F46)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Data", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
