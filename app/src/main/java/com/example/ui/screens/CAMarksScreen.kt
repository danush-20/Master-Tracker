package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SemesterCourse
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.SubjectCaMarkItem
import java.util.Locale

enum class AcademicSubTab {
    CA_MARKS,
    END_SEM_GRADES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CAMarksScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(AcademicSubTab.CA_MARKS) }

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1216),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedSection == AcademicSubTab.CA_MARKS) "Continuous Assessment (CA)" else "End Semester Results",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (selectedSection == AcademicSubTab.CA_MARKS) "2 CA tests conducted per semester • Sourced from courses" else "Official Letter Grades & CGPA Calculator (Semesters 1–4)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A22)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Elegant Section Switcher (CA Marks vs End Semester)
            Surface(
                color = Color(0xFF161A22),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF1E2430), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // CA Marks Tab Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedSection == AcademicSubTab.CA_MARKS) Color(0xFF4F46E5) else Color.Transparent)
                            .clickable { selectedSection = AcademicSubTab.CA_MARKS },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = null,
                                tint = if (selectedSection == AcademicSubTab.CA_MARKS) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CA Marks (CA1 & CA2)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSection == AcademicSubTab.CA_MARKS) Color.White else Color.Gray
                            )
                        }
                    }

                    // End Semester Tab Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedSection == AcademicSubTab.END_SEM_GRADES) Color(0xFF4F46E5) else Color.Transparent)
                            .clickable { selectedSection = AcademicSubTab.END_SEM_GRADES },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = if (selectedSection == AcademicSubTab.END_SEM_GRADES) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "End Sem Grades",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSection == AcademicSubTab.END_SEM_GRADES) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }

            // Display active view
            when (selectedSection) {
                AcademicSubTab.CA_MARKS -> {
                    CaMarksSectionView(viewModel = viewModel)
                }
                AcademicSubTab.END_SEM_GRADES -> {
                    EndSemGradesSectionView(viewModel = viewModel)
                }
            }
        }
    }
}

// =========================================================================
// SECTION 1: CA MARKS (2 CAs conducted per semester, sourced from courses)
// Preserved even when active subjects are deleted after the semester!
// =========================================================================

@Composable
fun CaMarksSectionView(
    viewModel: AttendanceViewModel
) {
    val caItems by viewModel.subjectCaMarkItems.collectAsState()
    val activeSubjects by viewModel.subjects.collectAsState()
    val overallMetrics by viewModel.caOverallMetrics.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItemToEdit by remember { mutableStateOf<SubjectCaMarkItem?>(null) }
    var showCustomSubjectDialog by remember { mutableStateOf(false) }
    var customSubjectNameInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Hero Summary Card for CA Marks
        item {
            CaSummaryCard(metrics = overallMetrics)
        }

        // Section header and Add Mark button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Semester CA Evaluation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Evaluates 2 Continuous Assessments per course",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = {
                        if (activeSubjects.isEmpty()) {
                            showCustomSubjectDialog = true
                        } else {
                            selectedItemToEdit = null
                            showEditDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add / Score",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // If no subjects found at all
        if (caItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "📝", fontSize = 44.sp)
                        Text(
                            text = "No Subjects Configured",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Subjects are automatically sourced from your course list in Attendance > Stats, or you can add a course name directly.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showCustomSubjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Course For CA")
                        }
                    }
                }
            }
        } else {
            // Highly polished, clear CA Table Matrix
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF282F3B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "CA 1 & CA 2 PERFORMANCE TABLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Scrollable table
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column {
                                // Header
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFF1E2430), RoundedCornerShape(8.dp))
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Course Subject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(160.dp))
                                    Text("CA 1 (50)", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                                    Text("CA 2 (50)", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                                    Text("Total (100)", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(100.dp))
                                    Text("Overall %", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                                    Text("Actions", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                caItems.forEach { item ->
                                    val ca1Str = if (item.ca1Marks != null && item.ca1Total != null) "${formatNum(item.ca1Marks)}/${formatNum(item.ca1Total)}" else "--"
                                    val ca2Str = if (item.ca2Marks != null && item.ca2Total != null) "${formatNum(item.ca2Marks)}/${formatNum(item.ca2Total)}" else "--"
                                    val totStr = if (item.hasMarks && item.totalMax > 0) "${formatNum(item.totalObtained)}/${formatNum(item.totalMax)}" else "--"
                                    val pctStr = if (item.overallPct != null) String.format(Locale.US, "%.1f%%", item.overallPct) else "--"

                                    val pctColor = when {
                                        item.overallPct == null -> Color.Gray
                                        item.overallPct >= 75.0 -> Color(0xFF10B981)
                                        item.overallPct >= 60.0 -> Color(0xFFF59E0B)
                                        else -> Color(0xFFEF4444)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.width(160.dp)) {
                                            Text(
                                                text = item.subjectName,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.activeSubject == null) {
                                                Text(
                                                    text = "Previous semester (Preserved)",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(ca1Str, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                                        Text(ca2Str, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                                        Text(totStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(100.dp))
                                        
                                        Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = pctColor.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, pctColor.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = pctStr,
                                                    color = pctColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.Center) {
                                            IconButton(
                                                onClick = {
                                                    selectedItemToEdit = item
                                                    showEditDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteCaMarks(item.subjectName)
                                                    android.widget.Toast.makeText(context, "Cleared marks for ${item.subjectName}", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFF202632), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Cards list of CA items
            items(caItems, key = { it.subjectName }) { item ->
                CaMarkItemCard(
                    item = item,
                    onEdit = {
                        selectedItemToEdit = item
                        showEditDialog = true
                    },
                    onDelete = {
                        viewModel.deleteCaMarks(item.subjectName)
                        android.widget.Toast.makeText(context, "Cleared marks for ${item.subjectName}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Add / Edit CA Dialog
    if (showEditDialog) {
        val courseNamesList = remember(activeSubjects, caItems) {
            (activeSubjects.map { it.name } + caItems.map { it.subjectName }).distinct().filter { it.isNotBlank() }
        }

        CaMarkEditSimpleDialog(
            courseNames = courseNamesList,
            initialItem = selectedItemToEdit,
            onDismiss = { showEditDialog = false },
            onSave = { name, ca1M, ca1T, ca2M, ca2T ->
                val activeSubj = activeSubjects.find { it.name.equals(name, ignoreCase = true) }
                viewModel.saveCaMarks(name, activeSubj?.id, ca1M, ca1T, ca2M, ca2T)
                showEditDialog = false
                android.widget.Toast.makeText(context, "Saved CA marks for $name", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Custom Subject Name Dialog if starting without courses
    if (showCustomSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSubjectDialog = false },
            containerColor = Color(0xFF1E222B),
            title = { Text("Add Course Subject", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter course name to record CA marks for:", color = Color.Gray, fontSize = 13.sp)
                    OutlinedTextField(
                        value = customSubjectNameInput,
                        onValueChange = { customSubjectNameInput = it },
                        label = { Text("Course Name (e.g. Distributed Systems)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customSubjectNameInput.isNotBlank()) {
                            viewModel.saveCaMarks(customSubjectNameInput.trim(), null, null, null, null, null)
                            customSubjectNameInput = ""
                            showCustomSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSubjectDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CaSummaryCard(
    metrics: com.example.viewmodel.CaOverallMetrics
) {
    val overallPct = metrics.overallPercentage
    val pctText = if (overallPct != null) String.format(Locale.US, "%.1f%%", overallPct) else "--%"

    val tierColor = when {
        overallPct == null -> Color(0xFF64748B)
        overallPct >= 75.0 -> Color(0xFF10B981)
        overallPct >= 60.0 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24)),
        border = BorderStroke(1.dp, Color(0xFF263044))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OVERALL CA PERCENTAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "All Continuous Assessments Combined",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tierColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (overallPct != null) "Evaluated" else "Pending Marks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = pctText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = tierColor
                    )
                    if (overallPct != null) {
                        Text(
                            text = " Total CA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Score: ${formatNum(metrics.totalObtained)} / ${formatNum(metrics.totalMax)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${metrics.subjectsEvaluated} courses evaluated",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun CaMarkItemCard(
    item: SubjectCaMarkItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = BorderStroke(1.dp, Color(0xFF282F3B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subjectName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (item.activeSubject == null) {
                        Text(
                            text = "Preserved from prior semester",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // CA1 & CA2 score display boxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2430), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CA 1", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    val ca1Str = if (item.ca1Marks != null && item.ca1Total != null) "${formatNum(item.ca1Marks)}/${formatNum(item.ca1Total)}" else "--"
                    Text(ca1Str, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CA 2", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    val ca2Str = if (item.ca2Marks != null && item.ca2Total != null) "${formatNum(item.ca2Marks)}/${formatNum(item.ca2Total)}" else "--"
                    Text(ca2Str, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Percentage", fontSize = 11.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold)
                    val pctStr = if (item.overallPct != null) String.format(Locale.US, "%.1f%%", item.overallPct) else "--"
                    Text(pctStr, fontSize = 14.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun CaMarkEditSimpleDialog(
    courseNames: List<String>,
    initialItem: SubjectCaMarkItem?,
    onDismiss: () -> Unit,
    onSave: (String, Double?, Double?, Double?, Double?) -> Unit
) {
    var selectedName by remember { mutableStateOf(initialItem?.subjectName ?: courseNames.firstOrNull() ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var ca1MarksStr by remember { mutableStateOf(initialItem?.ca1Marks?.let { formatNum(it) } ?: "") }
    var ca1TotalStr by remember { mutableStateOf(initialItem?.ca1Total?.let { formatNum(it) } ?: "50") }

    var ca2MarksStr by remember { mutableStateOf(initialItem?.ca2Marks?.let { formatNum(it) } ?: "") }
    var ca2TotalStr by remember { mutableStateOf(initialItem?.ca2Total?.let { formatNum(it) } ?: "50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E222B),
        title = { Text("Record CA Marks", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Course Subject:", color = Color.Gray, fontSize = 12.sp)

                // Dropdown to pick from available course subjects
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF262C38),
                        border = BorderStroke(1.dp, Color(0xFF384252)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedName.isNotBlank()) selectedName else "Select Course",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF262C38))
                    ) {
                        courseNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name, color = Color.White) },
                                onClick = {
                                    selectedName = name
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF282F3B), thickness = 0.8.dp)

                // CA 1
                Text("Continuous Assessment 1 (CA1):", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ca1MarksStr,
                        onValueChange = { ca1MarksStr = it },
                        label = { Text("Marks Scored", fontSize = 11.sp, color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                    )
                    OutlinedTextField(
                        value = ca1TotalStr,
                        onValueChange = { ca1TotalStr = it },
                        label = { Text("Total Marks", fontSize = 11.sp, color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                    )
                }

                // CA 2
                Text("Continuous Assessment 2 (CA2):", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ca2MarksStr,
                        onValueChange = { ca2MarksStr = it },
                        label = { Text("Marks Scored", fontSize = 11.sp, color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                    )
                    OutlinedTextField(
                        value = ca2TotalStr,
                        onValueChange = { ca2TotalStr = it },
                        label = { Text("Total Marks", fontSize = 11.sp, color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedName.isNotBlank()) {
                        val ca1M = ca1MarksStr.toDoubleOrNull()
                        val ca1T = ca1TotalStr.toDoubleOrNull()
                        val ca2M = ca2MarksStr.toDoubleOrNull()
                        val ca2T = ca2TotalStr.toDoubleOrNull()
                        onSave(selectedName, ca1M, ca1T, ca2M, ca2T)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text("Save Marks", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// =========================================================================
// SECTION 2: END SEMESTER RESULTS (Result is Grade only, Sems 1-4, GPA & CGPA)
// Completely separate from CA marks!
// =========================================================================

@Composable
fun EndSemGradesSectionView(
    viewModel: AttendanceViewModel
) {
    val semesterCourses by viewModel.semesterCourses.collectAsState()
    val activeSemester by viewModel.activeSemesterTab.collectAsState()
    val selectedSemesters by viewModel.selectedSemesters.collectAsState()
    val overallCgpaResult by viewModel.overallCgpaResult.collectAsState()

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<SemesterCourse?>(null) }
    var courseToDelete by remember { mutableStateOf<SemesterCourse?>(null) }

    val context = LocalContext.current

    val currentSemCourses = remember(semesterCourses, activeSemester) {
        semesterCourses.filter { it.semester == activeSemester }
    }

    val currentSemGpaResult = remember(currentSemCourses, activeSemester) {
        viewModel.getSemesterGpa(activeSemester, currentSemCourses)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Summary Card: GPA & CGPA
        item {
            CgpaSummaryCard(
                activeSemester = activeSemester,
                semGpaResult = currentSemGpaResult,
                overallCgpaResult = overallCgpaResult,
                selectedSemesters = selectedSemesters,
                onToggleSemester = { sem -> viewModel.toggleSemesterSelection(sem) }
            )
        }

        // Semester Selector Tabs (Semesters 1 to 4)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT SEMESTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A22), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3, 4).forEach { sem ->
                        val isSelected = activeSemester == sem
                        val semGpa = viewModel.getSemesterGpa(sem, semesterCourses).gpa
                        val gpaLabel = if (semGpa != null) String.format(Locale.US, " (%.2f)", semGpa) else ""

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF4F46E5) else Color.Transparent)
                                .clickable { viewModel.setActiveSemesterTab(sem) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Sem $sem",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                                if (gpaLabel.isNotEmpty()) {
                                    Text(
                                        text = gpaLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Header for active semester's course list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Semester $activeSemester End Sem Results (${currentSemCourses.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Total Credits: ${formatNum(currentSemGpaResult.totalCredits)}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = {
                        courseToEdit = null
                        showAddCourseDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Course",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Empty state for this semester
        if (currentSemCourses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🎓", fontSize = 44.sp)
                        Text(
                            text = "No End Sem Results in Semester $activeSemester",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "In End Sem, only letter grades (O, A+, A, B+, B, C, U) and credits (1.5, 3, 4, 4.5) determine your SGPA and CGPA.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                courseToEdit = null
                                showAddCourseDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Add Course Grade")
                        }
                    }
                }
            }
        } else {
            // Clear, elegant End Sem Grade Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                    border = BorderStroke(1.dp, Color(0xFF282F3B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "SEMESTER $activeSemester GRADE MATRIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA68C8),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Clear End Sem Table
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFF1E2430), RoundedCornerShape(8.dp))
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Course Subject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(180.dp))
                                    Text("Credits", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
                                    Text("Grade", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
                                    Text("Grade Points", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                                    Text("Actions", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                currentSemCourses.forEach { c ->
                                    val gradePoints = AttendanceViewModel.gradeToPoints(c.grade)
                                    val gradeColor = when (c.grade?.uppercase()) {
                                        "O", "A+" -> Color(0xFF10B981)
                                        "A", "B+" -> Color(0xFF38BDF8)
                                        "B", "C" -> Color(0xFFF59E0B)
                                        "U" -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(c.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(180.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formatNum(c.credits), color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
                                        
                                        Box(modifier = Modifier.width(70.dp), contentAlignment = Alignment.Center) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = gradeColor.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = c.grade ?: "--",
                                                    color = gradeColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        val ptsStr = if (gradePoints != null) formatNum(gradePoints) else "--"
                                        Text(ptsStr, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))

                                        Row(modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.Center) {
                                            IconButton(onClick = { courseToEdit = c }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { courseToDelete = c }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFF202632), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Cards for mobile readability
            items(currentSemCourses, key = { it.id }) { course ->
                EndSemCourseCard(
                    course = course,
                    onEdit = { courseToEdit = course },
                    onDelete = { courseToDelete = course }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Add / Edit End Sem Course Dialog
    if (showAddCourseDialog || courseToEdit != null) {
        val isEditing = courseToEdit != null
        EndSemGradeEditDialog(
            activeSemester = activeSemester,
            courseToEdit = courseToEdit,
            onDismiss = {
                showAddCourseDialog = false
                courseToEdit = null
            },
            onSave = { course ->
                if (isEditing) {
                    viewModel.updateSemesterCourse(course)
                    android.widget.Toast.makeText(context, "Updated ${course.name}", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addSemesterCourse(
                        semester = course.semester,
                        name = course.name,
                        credits = course.credits,
                        grade = course.grade
                    )
                    android.widget.Toast.makeText(context, "Added ${course.name}", android.widget.Toast.LENGTH_SHORT).show()
                }
                showAddCourseDialog = false
                courseToEdit = null
            }
        )
    }

    // Delete Course Confirmation Dialog
    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = "Delete Course Result",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${course.name}' from Semester ${course.semester}? This will update the semester GPA and CGPA calculations.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSemesterCourse(course)
                        courseToDelete = null
                        android.widget.Toast.makeText(context, "Deleted ${course.name}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CgpaSummaryCard(
    activeSemester: Int,
    semGpaResult: com.example.viewmodel.SemesterGpaResult,
    overallCgpaResult: com.example.viewmodel.OverallCgpaResult,
    selectedSemesters: Set<Int>,
    onToggleSemester: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24)),
        border = BorderStroke(1.dp, Color(0xFF263044))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Active Semester GPA vs Overall CGPA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Semester GPA
                Column {
                    Text(
                        text = "SEMESTER $activeSemester GPA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 0.8.sp
                    )
                    val gpaStr = if (semGpaResult.gpa != null) String.format(Locale.US, "%.2f", semGpaResult.gpa) else "--"
                    Text(
                        text = gpaStr,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "${semGpaResult.gradedCount}/${semGpaResult.coursesCount} graded • ${formatNum(semGpaResult.totalCredits)} credits",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(Color(0xFF282F3B))
                )

                // Overall CGPA
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OVERALL CGPA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 0.8.sp
                    )
                    val cgpaStr = if (overallCgpaResult.cgpa != null) String.format(Locale.US, "%.2f", overallCgpaResult.cgpa) else "--"
                    Text(
                        text = cgpaStr,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "${overallCgpaResult.gradedCoursesCount}/${overallCgpaResult.totalCoursesCount} graded • ${formatNum(overallCgpaResult.totalCredits)} credits",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF282F3B), thickness = 0.8.dp)

            // Multi-semester selection checkboxes for CGPA formula
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Calculate CGPA across selected semesters:",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3, 4).forEach { sem ->
                        val isChecked = selectedSemesters.contains(sem)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E2430),
                            border = BorderStroke(1.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF282F3B)),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .clickable { onToggleSemester(sem) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isChecked) "✓ Sem $sem" else "Sem $sem",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) Color(0xFF6EE7B7) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EndSemCourseCard(
    course: SemesterCourse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val gradePoints = AttendanceViewModel.gradeToPoints(course.grade)
    val gradeColor = when (course.grade?.uppercase()) {
        "O", "A+" -> Color(0xFF10B981)
        "A", "B+" -> Color(0xFF38BDF8)
        "B", "C" -> Color(0xFFF59E0B)
        "U" -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = BorderStroke(1.dp, Color(0xFF282F3B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${formatNum(course.credits)} Credits",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = gradeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, gradeColor)
                ) {
                    Text(
                        text = if (course.grade != null) "Grade ${course.grade} (${formatNum(gradePoints ?: 0.0)} pts)" else "No Grade",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndSemGradeEditDialog(
    activeSemester: Int,
    courseToEdit: SemesterCourse?,
    onDismiss: () -> Unit,
    onSave: (SemesterCourse) -> Unit
) {
    var name by remember { mutableStateOf(courseToEdit?.name ?: "") }
    var selectedCredits by remember { mutableStateOf(courseToEdit?.credits ?: 3.0) }
    var selectedGrade by remember { mutableStateOf(courseToEdit?.grade) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var creditsExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E222B),
        title = {
            Text(
                text = if (courseToEdit != null) "Edit Course Grade (Sem $activeSemester)" else "Add Course Grade (Sem $activeSemester)",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Course Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_endsem_name")
                )

                // Credits & Grade Dropdown Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Credits Dropdown (Only 1.5, 3, 4, 4.5)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Credits:", color = Color.Gray, fontSize = 12.sp)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF262C38),
                                border = BorderStroke(1.dp, Color(0xFF384252)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { creditsExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatNum(selectedCredits),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            DropdownMenu(
                                expanded = creditsExpanded,
                                onDismissRequest = { creditsExpanded = false },
                                modifier = Modifier.background(Color(0xFF262C38))
                            ) {
                                AttendanceViewModel.CREDIT_OPTIONS.forEach { cred ->
                                    DropdownMenuItem(
                                        text = { Text("${formatNum(cred)} Credits", color = Color.White) },
                                        onClick = {
                                            selectedCredits = cred
                                            creditsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Grade Dropdown: O (10), A+ (9), A (8), B+ (7), B (6), C (5), U (0)
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("Result Letter Grade:", color = Color.Gray, fontSize = 12.sp)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF262C38),
                                border = BorderStroke(1.dp, Color(0xFF384252)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { gradeExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val gradePoints = AttendanceViewModel.gradeToPoints(selectedGrade)
                                    Text(
                                        text = if (selectedGrade != null) "$selectedGrade (${formatNum(gradePoints ?: 0.0)})" else "Select Grade",
                                        color = if (selectedGrade != null) Color(0xFF10B981) else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            DropdownMenu(
                                expanded = gradeExpanded,
                                onDismissRequest = { gradeExpanded = false },
                                modifier = Modifier.background(Color(0xFF262C38))
                            ) {
                                AttendanceViewModel.GRADE_OPTIONS.forEach { gr ->
                                    val pts = AttendanceViewModel.gradeToPoints(gr)
                                    DropdownMenuItem(
                                        text = { Text("$gr (${formatNum(pts ?: 0.0)} points)", color = Color.White) },
                                        onClick = {
                                            selectedGrade = gr
                                            gradeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                errorMsg?.let {
                    Text(
                        text = it,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Please enter course name"
                        return@Button
                    }
                    val resultCourse = (courseToEdit ?: SemesterCourse(semester = activeSemester, name = "")).copy(
                        semester = activeSemester,
                        name = name.trim(),
                        credits = selectedCredits,
                        grade = selectedGrade
                    )
                    onSave(resultCourse)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text(if (courseToEdit != null) "Update" else "Save Grade", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

private fun formatNum(n: Double): String {
    return if (n % 1.0 == 0.0) n.toInt().toString() else String.format(Locale.US, "%.1f", n)
}
