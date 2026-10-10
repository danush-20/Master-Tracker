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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.SubjectCaMarkItem
import java.util.Locale

enum class CaViewMode {
    TABLE, CARDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CAMarksScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsState()
    val caItems by viewModel.subjectCaMarkItems.collectAsState()
    val overallMetrics by viewModel.caOverallMetrics.collectAsState()

    var viewMode by remember { mutableStateOf(CaViewMode.TABLE) }
    var selectedItemForEdit by remember { mutableStateOf<SubjectCaMarkItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F1216),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CA Marks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Continuous Assessment 1 & 2 Performance",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A22)
                ),
                actions = {
                    if (subjects.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedItemForEdit = null
                                showAddDialog = true
                            },
                            modifier = Modifier.testTag("btn_add_ca_marks")
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = "Add CA Marks",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            )
        }
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

            // Hero Summary Card
            item {
                CaOverallSummaryCard(
                    metrics = overallMetrics,
                    targetGoal = 75.0
                )
            }

            // View Mode Selector & Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject Marks Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    // Pill View Switcher (Table vs Cards)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E2430), RoundedCornerShape(20.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (viewMode == CaViewMode.TABLE) Color(0xFF4F46E5) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewMode = CaViewMode.TABLE }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = "Table",
                                    tint = if (viewMode == CaViewMode.TABLE) Color.White else Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Table",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == CaViewMode.TABLE) Color.White else Color.Gray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (viewMode == CaViewMode.CARDS) Color(0xFF4F46E5) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewMode = CaViewMode.CARDS }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ViewAgenda,
                                    contentDescription = "Cards",
                                    tint = if (viewMode == CaViewMode.CARDS) Color.White else Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cards",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == CaViewMode.CARDS) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Content List / Table
            if (subjects.isEmpty()) {
                item {
                    CaEmptySubjectsCard()
                }
            } else if (viewMode == CaViewMode.TABLE) {
                item {
                    CaMarksTableView(
                        items = caItems,
                        onEdit = { selectedItemForEdit = it },
                        onDelete = { item ->
                            viewModel.deleteCaMarks(item.subject.id)
                            android.widget.Toast.makeText(context, "Cleared marks for ${item.subject.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                items(caItems, key = { it.subject.id }) { item ->
                    CaMarksSubjectCard(
                        item = item,
                        onEdit = { selectedItemForEdit = item },
                        onDelete = {
                            viewModel.deleteCaMarks(item.subject.id)
                            android.widget.Toast.makeText(context, "Cleared marks for ${item.subject.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add / Edit Marks Dialog
    if (showAddDialog || selectedItemForEdit != null) {
        val editingSubjectId = selectedItemForEdit?.subject?.id ?: subjects.firstOrNull()?.id ?: 0
        CaMarkEditDialog(
            subjects = subjects,
            initialSubjectId = editingSubjectId,
            existingItem = selectedItemForEdit,
            onDismiss = {
                showAddDialog = false
                selectedItemForEdit = null
            },
            onSave = { subjId, ca1M, ca1T, ca2M, ca2T ->
                viewModel.saveCaMarks(subjId, ca1M, ca1T, ca2M, ca2T)
                showAddDialog = false
                selectedItemForEdit = null
                android.widget.Toast.makeText(context, "Saved CA marks successfully", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CaOverallSummaryCard(
    metrics: com.example.viewmodel.CaOverallMetrics,
    targetGoal: Double
) {
    val overallPct = metrics.overallPercentage
    val pctText = if (overallPct != null) String.format(Locale.US, "%.1f%%", overallPct) else "--%"
    
    val tierColor = when {
        overallPct == null -> Color(0xFF64748B)
        overallPct >= 75.0 -> Color(0xFF10B981) // Emerald
        overallPct >= 60.0 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Rose
    }

    val tierLabel = when {
        overallPct == null -> "No Marks Added Yet"
        overallPct >= 75.0 -> "Distinction Tier (≥75%)"
        overallPct >= 60.0 -> "First Class Tier (60-74%)"
        else -> "Needs Attention (<60%)"
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL CA PERCENTAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "All Subjects Combined",
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
                        text = tierLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Big Score Display Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = pctText,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = tierColor
                    )
                    if (overallPct != null) {
                        Text(
                            text = " Overall",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }

                // Marks stats fraction
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Score",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    val obtStr = if (metrics.totalObtained % 1.0 == 0.0) metrics.totalObtained.toInt().toString() else String.format(Locale.US, "%.1f", metrics.totalObtained)
                    val maxStr = if (metrics.totalMax % 1.0 == 0.0) metrics.totalMax.toInt().toString() else String.format(Locale.US, "%.1f", metrics.totalMax)
                    
                    Text(
                        text = if (metrics.totalMax > 0) "$obtStr / $maxStr" else "0 / 0",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${metrics.subjectsEvaluated} of ${metrics.totalSubjects} subjects evaluated",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = {
                    if (overallPct != null) (overallPct.toFloat() / 100f).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = tierColor,
                trackColor = Color(0xFF263044)
            )
        }
    }
}

@Composable
fun CaMarksTableView(
    items: List<SubjectCaMarkItem>,
    onEdit: (SubjectCaMarkItem) -> Unit,
    onDelete: (SubjectCaMarkItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = BorderStroke(1.dp, Color(0xFF283244))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2430))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = "CA 1",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "CA 2",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Total %",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF283244), thickness = 1.dp)

            // Table Body Rows
            items.forEachIndexed { index, item ->
                val rowBg = if (index % 2 == 0) Color(0xFF161A22) else Color(0xFF12151C)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable { onEdit(item) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subject Name
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = item.subject.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!item.hasMarks) {
                            Text(
                                text = "Tap to enter",
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // CA 1
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.ca1Marks != null && item.ca1Total != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${formatNum(item.ca1Marks)}/${formatNum(item.ca1Total)}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", item.ca1Pct ?: 0.0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        } else {
                            Text(text = "—", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }

                    // CA 2
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.ca2Marks != null && item.ca2Total != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${formatNum(item.ca2Marks)}/${formatNum(item.ca2Total)}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", item.ca2Pct ?: 0.0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC084FC)
                                )
                            }
                        } else {
                            Text(text = "—", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }

                    // Total %
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.overallPct != null) {
                            val color = when {
                                item.overallPct >= 75.0 -> Color(0xFF10B981)
                                item.overallPct >= 60.0 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = color.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", item.overallPct),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(text = "—", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }

                    // Edit button
                    Box(
                        modifier = Modifier.width(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { onEdit(item) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Marks",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (index < items.size - 1) {
                    HorizontalDivider(color = Color(0xFF222B3A), thickness = 0.8.dp)
                }
            }
        }
    }
}

@Composable
fun CaMarksSubjectCard(
    item: SubjectCaMarkItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24)),
        border = BorderStroke(1.dp, Color(0xFF263044))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Subject Name & Total Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subject.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (item.hasMarks) "Total Marks: ${formatNum(item.totalObtained)} / ${formatNum(item.totalMax)}" else "No marks recorded",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (item.overallPct != null) {
                    val color = when {
                        item.overallPct >= 75.0 -> Color(0xFF10B981)
                        item.overallPct >= 60.0 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = color.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", item.overallPct),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Text("+ Add", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // CA1 & CA2 Sub-Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CA 1
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2432),
                    border = BorderStroke(1.dp, Color(0xFF2F3B52))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CA 1", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                            Text(
                                text = if (item.ca1Pct != null) String.format(Locale.US, "%.0f%%", item.ca1Pct) else "—",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.ca1Marks != null) "${formatNum(item.ca1Marks)} / ${formatNum(item.ca1Total ?: 50.0)}" else "Not Entered",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // CA 2
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2432),
                    border = BorderStroke(1.dp, Color(0xFF2F3B52))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CA 2", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC084FC))
                            Text(
                                text = if (item.ca2Pct != null) String.format(Locale.US, "%.0f%%", item.ca2Pct) else "—",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.ca2Marks != null) "${formatNum(item.ca2Marks)} / ${formatNum(item.ca2Total ?: 50.0)}" else "Not Entered",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaMarkEditDialog(
    subjects: List<Subject>,
    initialSubjectId: Int,
    existingItem: SubjectCaMarkItem?,
    onDismiss: () -> Unit,
    onSave: (subjectId: Int, ca1Marks: Double?, ca1Total: Double?, ca2Marks: Double?, ca2Total: Double?) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(initialSubjectId) }
    
    // Find current existing mark if user switches subject in dialog
    val currentSubj = subjects.find { it.id == selectedSubjectId }

    var ca1MarksInput by remember(selectedSubjectId, existingItem) {
        val m = existingItem?.ca1Marks ?: ""
        mutableStateOf(if (m != "") formatNum(m as Double) else "")
    }
    var ca1TotalInput by remember(selectedSubjectId, existingItem) {
        val t = existingItem?.ca1Total ?: 50.0
        mutableStateOf(formatNum(t))
    }

    var ca2MarksInput by remember(selectedSubjectId, existingItem) {
        val m = existingItem?.ca2Marks ?: ""
        mutableStateOf(if (m != "") formatNum(m as Double) else "")
    }
    var ca2TotalInput by remember(selectedSubjectId, existingItem) {
        val t = existingItem?.ca2Total ?: 50.0
        mutableStateOf(formatNum(t))
    }

    var isSubjectDropdownOpen by remember { mutableStateOf(false) }

    // Live preview calculations
    val ca1M = ca1MarksInput.toDoubleOrNull()
    val ca1T = ca1TotalInput.toDoubleOrNull()
    val ca1Pct = if (ca1M != null && ca1T != null && ca1T > 0) (ca1M / ca1T) * 100.0 else null

    val ca2M = ca2MarksInput.toDoubleOrNull()
    val ca2T = ca2TotalInput.toDoubleOrNull()
    val ca2Pct = if (ca2M != null && ca2T != null && ca2T > 0) (ca2M / ca2T) * 100.0 else null

    var liveObt = 0.0
    var liveMax = 0.0
    if (ca1M != null && ca1T != null && ca1T > 0) {
        liveObt += ca1M
        liveMax += ca1T
    }
    if (ca2M != null && ca2T != null && ca2T > 0) {
        liveObt += ca2M
        liveMax += ca2T
    }
    val liveOverall = if (liveMax > 0) (liveObt / liveMax) * 100.0 else null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161A22),
        title = {
            Text(
                text = if (existingItem?.hasMarks == true) "Edit CA Marks" else "Record CA Marks",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject Selector Dropdown
                Text("Select Subject:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                
                ExposedDropdownMenuBox(
                    expanded = isSubjectDropdownOpen,
                    onExpandedChange = { isSubjectDropdownOpen = it }
                ) {
                    OutlinedTextField(
                        value = currentSubj?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownOpen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF283244)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isSubjectDropdownOpen,
                        onDismissRequest = { isSubjectDropdownOpen = false },
                        modifier = Modifier.background(Color(0xFF1E2430))
                    ) {
                        subjects.forEach { subj ->
                            DropdownMenuItem(
                                text = { Text(subj.name, color = Color.White) },
                                onClick = {
                                    selectedSubjectId = subj.id
                                    isSubjectDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF283244), thickness = 1.dp)

                // Continuous Assessment 1 (CA1) Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2430),
                    border = BorderStroke(1.dp, Color(0xFF2F3B52)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Continuous Assessment 1 (CA 1)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF38BDF8))
                            if (ca1Pct != null) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", ca1Pct),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ca1MarksInput,
                                onValueChange = { ca1MarksInput = it },
                                label = { Text("Marks Scored", fontSize = 11.sp, color = Color.Gray) },
                                placeholder = { Text("e.g. 42", color = Color.DarkGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = ca1TotalInput,
                                onValueChange = { ca1TotalInput = it },
                                label = { Text("Total Marks", fontSize = 11.sp, color = Color.Gray) },
                                placeholder = { Text("50", color = Color.DarkGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Continuous Assessment 2 (CA2) Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2430),
                    border = BorderStroke(1.dp, Color(0xFF2F3B52)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Continuous Assessment 2 (CA 2)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC084FC))
                            if (ca2Pct != null) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", ca2Pct),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC084FC)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ca2MarksInput,
                                onValueChange = { ca2MarksInput = it },
                                label = { Text("Marks Scored", fontSize = 11.sp, color = Color.Gray) },
                                placeholder = { Text("e.g. 45", color = Color.DarkGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = ca2TotalInput,
                                onValueChange = { ca2TotalInput = it },
                                label = { Text("Total Marks", fontSize = 11.sp, color = Color.Gray) },
                                placeholder = { Text("50", color = Color.DarkGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Live Preview Summary
                if (liveOverall != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Combined Subject %:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%%  (%s / %s)", liveOverall, formatNum(liveObt), formatNum(liveMax)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedSubjectId, ca1M, ca1T, ca2M, ca2T)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier.testTag("btn_save_ca_marks")
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

@Composable
fun CaEmptySubjectsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
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
            Text(text = "📊", fontSize = 44.sp)
            Text(
                text = "No Subjects Configured",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = "Add your course subjects first from the Attendance > Stats section to record and calculate CA 1 and CA 2 percentages.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatNum(n: Double): String {
    return if (n % 1.0 == 0.0) n.toInt().toString() else String.format(Locale.US, "%.1f", n)
}
