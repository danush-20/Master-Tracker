package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AttendanceViewModel

enum class MainTab {
    ATTENDANCE, CA_MARKS, SETTINGS
}

enum class AttendanceSubTab {
    TODAY, TIMETABLE, CALENDAR, SUBJECTS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme(darkTheme = true) { // Enforce standard beautiful dark slate UI
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1216)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(MainTab.ATTENDANCE) }
    var currentSubTab by remember { mutableStateOf(AttendanceSubTab.TODAY) }
    val viewModel: AttendanceViewModel = viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1216),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E222B),
                tonalElevation = 8.dp,
                modifier = Modifier.height(80.dp)
            ) {
                // ATTENDANCE TAB
                NavigationBarItem(
                    selected = currentTab == MainTab.ATTENDANCE,
                    onClick = { currentTab = MainTab.ATTENDANCE },
                    icon = { Icon(Icons.Default.School, contentDescription = "Attendance") },
                    label = { Text("Attendance", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.testTag("nav_attendance")
                )

                // CA MARKS TAB
                NavigationBarItem(
                    selected = currentTab == MainTab.CA_MARKS,
                    onClick = { currentTab = MainTab.CA_MARKS },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "CA Marks") },
                    label = { Text("CA Marks", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.testTag("nav_ca_marks")
                )

                // SETTINGS TAB
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF4A5568)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Attendance Sub-navigation row - only shown when Attendance tab is active!
            if (currentTab == MainTab.ATTENDANCE) {
                Surface(
                    color = Color(0xFF0F1216), // Deep rich background
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // All elements housed in a single beautiful capsule layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E222B), RoundedCornerShape(24.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AttendanceSubTab.values().forEach { tab ->
                                val isSelected = currentSubTab == tab
                                val activeGradient = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF8B2C82), // Deep purple-magenta
                                        Color(0xFFBA68C8)  // Soft orchid purple
                                    )
                                )
                                val displayLabel = when (tab) {
                                    AttendanceSubTab.TODAY -> "Today"
                                    AttendanceSubTab.TIMETABLE -> "Schedule"
                                    AttendanceSubTab.CALENDAR -> "History"
                                    AttendanceSubTab.SUBJECTS -> "Stats"
                                }
                                val tabIcon = when (tab) {
                                    AttendanceSubTab.TODAY -> Icons.Default.Today
                                    AttendanceSubTab.TIMETABLE -> Icons.Default.Schedule
                                    AttendanceSubTab.CALENDAR -> Icons.Default.History
                                    AttendanceSubTab.SUBJECTS -> Icons.Default.BarChart
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .let {
                                            if (isSelected) it.background(activeGradient) else it
                                        }
                                        .clickable { currentSubTab = tab }
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = tabIcon,
                                            contentDescription = displayLabel,
                                            tint = if (isSelected) Color.White else Color.Gray,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = displayLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    MainTab.ATTENDANCE -> {
                        when (currentSubTab) {
                            AttendanceSubTab.TODAY -> TodayScreen(
                                viewModel = viewModel,
                                onNavigateToSubjects = { currentSubTab = AttendanceSubTab.TIMETABLE }
                            )
                            AttendanceSubTab.TIMETABLE -> TimetableScreen(viewModel = viewModel)
                            AttendanceSubTab.CALENDAR -> CalendarScreen(
                                viewModel = viewModel,
                                onNavigateToScheduleDate = { date ->
                                    viewModel.selectDate(date)
                                    currentSubTab = AttendanceSubTab.TODAY
                                }
                            )
                            AttendanceSubTab.SUBJECTS -> SubjectsScreen(viewModel = viewModel)
                        }
                    }
                    MainTab.CA_MARKS -> CAMarksScreen(viewModel = viewModel)
                    MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
