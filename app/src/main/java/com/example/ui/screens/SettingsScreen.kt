package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun showTimePicker(context: Context, initialTime: String, onTimeSelected: (String) -> Unit) {
    var hour = 8
    var minute = 0
    try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        val localTime = LocalTime.parse(initialTime.uppercase(Locale.US), formatter)
        hour = localTime.hour
        minute = localTime.minute
    } catch(e: Exception) {
        e.printStackTrace()
    }
    
    android.app.TimePickerDialog(
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
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282F3B)),
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
                        text = "A danush",
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f))
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "TechOps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Attendance Goal",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Customize the minimum attendance criteria for your courses. Default threshold is set to 75%.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Goal:", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "${sliderValue.toInt()}%",
                            color = Color(0xFF4F46E5),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Slider
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.updateTargetPercentage(sliderValue.toDouble())
                        },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4F46E5),
                            activeTrackColor = Color(0xFF4F46E5),
                            inactiveTrackColor = Color(0xFF2C323F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val defaultPeriodDuration by viewModel.defaultPeriodDuration.collectAsState()
            var periodDurationInput by remember(defaultPeriodDuration) { mutableStateOf(defaultPeriodDuration.toString()) }

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
                        text = "Timetable Defaults",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Configure the default duration (in minutes) for each period when creating a timetable slot.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = periodDurationInput,
                        onValueChange = { 
                            periodDurationInput = it.filter { char -> char.isDigit() }
                            val num = periodDurationInput.toIntOrNull()
                            if (num != null && num > 0) {
                                viewModel.updateDefaultPeriodDuration(num)
                            }
                        },
                        label = { Text("Default Duration (mins)", color = Color.Gray) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Custom Periods Management Card
            val customPeriods by viewModel.customPeriods.collectAsState()
            var newPeriodName by remember { mutableStateOf("") }
            var newStartTime by remember { mutableStateOf("09:00 AM") }
            var newEndTime by remember { mutableStateOf("09:50 AM") }
            var periodToEdit by remember { mutableStateOf<com.example.data.CustomPeriod?>(null) }
            
            if (periodToEdit != null) {
                var editPeriodName by remember { mutableStateOf(periodToEdit!!.name) }
                var editStartTime by remember { mutableStateOf(periodToEdit!!.startTime) }
                var editEndTime by remember { mutableStateOf(periodToEdit!!.endTime) }

                AlertDialog(
                    onDismissRequest = { periodToEdit = null },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Edit Period", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editPeriodName,
                                onValueChange = { editPeriodName = it },
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
                                        onClick = { showTimePicker(context, editStartTime) { editStartTime = it } },
                                        color = Color(0xFF2C323F),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(editStartTime, color = Color.White, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End:", color = Color.Gray, fontSize = 12.sp)
                                    Surface(
                                        onClick = { showTimePicker(context, editEndTime) { editEndTime = it } },
                                        color = Color(0xFF2C323F),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(editEndTime, color = Color.White, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (editPeriodName.isNotBlank()) {
                                    viewModel.updateCustomPeriod(periodToEdit!!.copy(name = editPeriodName.trim(), startTime = editStartTime, endTime = editEndTime))
                                    periodToEdit = null
                                }
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
                        text = "Manage Custom Periods",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Create predefined slots with start and end times to select when scheduling your weekly timetable or extra classes.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    // Add custom period inputs
                    OutlinedTextField(
                        value = newPeriodName,
                        onValueChange = { newPeriodName = it },
                        label = { Text("Period Name (e.g., Period 1)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Start Time Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Time:", color = Color.LightGray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                onClick = {
                                    showTimePicker(context, newStartTime) { 
                                        newStartTime = it 
                                        newEndTime = addMinutesToTime(it, defaultPeriodDuration)
                                    }
                                },
                                color = Color(0xFF2C323F),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = newStartTime,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // End Time Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Time:", color = Color.LightGray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                onClick = {
                                    showTimePicker(context, newEndTime) { newEndTime = it }
                                },
                                color = Color(0xFF2C323F),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = newEndTime,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newPeriodName.isNotBlank()) {
                                viewModel.addCustomPeriod(newPeriodName.trim(), newStartTime, newEndTime)
                                newPeriodName = ""
                            } else {
                                android.widget.Toast.makeText(context, "Please enter a period name", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Period Slot", color = Color.White)
                    }

                    if (customPeriods.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFF2C323F), thickness = 1.dp)
                        Text("Configured Periods", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customPeriods.forEach { period ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF161A22), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(period.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
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
            
            // Backup & Restore
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
                        text = "Export your attendance data to share with friends, or import a shared file.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("attendance_export.json")
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
