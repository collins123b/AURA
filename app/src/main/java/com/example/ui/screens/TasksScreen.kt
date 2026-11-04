package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.AssessmentEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.dialogs.AddAssessmentDialog
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.util.SubtaskChecklistHelper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(
    viewModel: AuraViewModel,
    initialSection: Int = 0,
    onOpenCitationManager: () -> Unit = {},
    onOpenGpaPredictor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val assessments by viewModel.assessments.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var selectedSection by remember { mutableIntStateOf(initialSection.coerceIn(0, 1)) } // 0 = Assessments, 1 = Group Projects
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, EXAMS, COMPLETED
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedForEdit by remember { mutableStateOf<AssessmentEntity?>(null) }

    LaunchedEffect(initialSection) {
        selectedSection = initialSection.coerceIn(0, 1)
    }

    val filteredList = remember(assessments, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> assessments.filter { it.status != "COMPLETED" && it.status != "SUBMITTED" }
            "EXAMS" -> assessments.filter { it.type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST") }
            "COMPLETED" -> assessments.filter { it.status == "COMPLETED" || it.status == "SUBMITTED" }
            else -> assessments
        }.sortedWith(compareBy({ it.status == "COMPLETED" }, { it.dueEpochMillis }))
    }

    val completedCount = remember(assessments) {
        assessments.count { it.status == "COMPLETED" || it.status == "SUBMITTED" }
    }
    val pendingCount = remember(assessments) {
        assessments.count { it.status != "COMPLETED" && it.status != "SUBMITTED" }
    }
    val completionRatio = remember(assessments, completedCount) {
        if (assessments.isNotEmpty()) completedCount.toFloat() / assessments.size.toFloat() else 0f
    }

    FrostedGlassBackground(
        modifier = modifier.fillMaxSize()
    ) {
        if (selectedSection == 1) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraGlassButton(
                        text = "Assessments",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        isPrimary = false,
                        onClick = { selectedSection = 0 },
                        modifier = Modifier.weight(1f),
                        testTag = "tasks_mode_assessments"
                    )
                    AuraGlassButton(
                        text = "Group Projects",
                        icon = Icons.Default.Groups,
                        isPrimary = true,
                        onClick = { selectedSection = 1 },
                        modifier = Modifier.weight(1f),
                        testTag = "tasks_mode_group_projects"
                    )
                }

                GroupProjectsView(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("tasks_assessments_lazy_list"),
                contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Sleek Horizontal Assessments Header Card
                item(key = "tasks_header") {
                    AuraCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        accentGlow = LocalAuraColors.current.warning
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.aura_prism_hero_1793569866167),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(if (LocalAuraColors.current.isDark) 0.26f else 0.10f)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Assessments & Deadlines",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LocalAuraColors.current.textPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AuraBadge(
                                        text = "$pendingCount Pending",
                                        backgroundColor = Color(0x2838BDF8),
                                        textColor = LocalAuraColors.current.secondaryAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Assignments, quizzes, tests & exams with AM/PM alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.secondaryAccent,
                                    maxLines = 1
                                )

                                if (assessments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Semester Completion",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = LocalAuraColors.current.textSecondary
                                        )
                                        Text(
                                            text = "$completedCount / ${assessments.size} Done",
                                            fontFamily = AuraMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AuraMintAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x22FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(completionRatio.coerceAtLeast(0.03f))
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            AuraMintAccent,
                                                            LocalAuraColors.current.secondaryAccent,
                                                            LocalAuraColors.current.primaryAccent
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Section Switcher: Assessments vs Group Projects
                item(key = "tasks_switcher") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraGlassButton(
                            text = "Assessments",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            isPrimary = true,
                            onClick = { selectedSection = 0 },
                            modifier = Modifier.weight(1f),
                            testTag = "tasks_mode_assessments"
                        )
                        AuraGlassButton(
                            text = "Group Projects",
                            icon = Icons.Default.Groups,
                            isPrimary = false,
                            onClick = { selectedSection = 1 },
                            modifier = Modifier.weight(1f),
                            testTag = "tasks_mode_group_projects"
                        )
                    }
                }

                // 3. Academic Writing & Exam Matrix Quick Strip
                item(key = "tasks_quick_tools") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraGlassButton(
                            text = "Citations",
                            icon = Icons.Default.FormatQuote,
                            cornerRadius = 14.dp,
                            onClick = onOpenCitationManager,
                            modifier = Modifier.weight(1f),
                            testTag = "tasks_open_citations_button"
                        )
                        AuraGlassButton(
                            text = "Exam Matrix",
                            icon = Icons.Default.Calculate,
                            cornerRadius = 14.dp,
                            onClick = onOpenGpaPredictor,
                            modifier = Modifier.weight(1f),
                            testTag = "tasks_open_exam_predictor_button"
                        )
                    }
                }

                // 4. Filter Chips Row
                item(key = "tasks_filters") {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("assessment_filter_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to "All Tasks",
                            "PENDING" to "Pending",
                            "EXAMS" to "Examinations",
                            "COMPLETED" to "Completed"
                        )
                        items(filters) { (key, label) ->
                            val isSelected = selectedFilter == key
                            AuraGlassButton(
                                text = label,
                                isPrimary = isSelected,
                                onClick = { selectedFilter = key },
                                cornerRadius = 15.dp,
                                testTag = "filter_$key"
                            )
                        }
                    }
                }

                // 5. Assessments List
                if (filteredList.isEmpty()) {
                    item(key = "tasks_empty") {
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No assessments matching this category.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap '+' to create a new assignment or exam reminder.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.id }) { item ->
                        AssessmentCard(
                            assessment = item,
                            onToggleComplete = { viewModel.toggleAssessmentCompleted(item) },
                            onInitDefaultSubtasks = { viewModel.initDefaultAssessmentSubtasks(item) },
                            onAddSubtask = { stepTitle -> viewModel.addAssessmentSubtask(item, stepTitle) },
                            onToggleSubtask = { subId -> viewModel.toggleAssessmentSubtask(item, subId) },
                            onRemoveSubtask = { subId -> viewModel.removeAssessmentSubtask(item, subId) },
                            onEdit = { selectedForEdit = item },
                            onDelete = { viewModel.deleteAssessment(item) }
                        )
                    }
                }
            }
        }

        // Liquid Glass FAB to add assessment
        if (selectedSection == 0) {
            AuraGlassIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add Assessment",
                onClick = { showAddDialog = true },
                size = 58.dp,
                iconSize = 26.dp,
                isPrimary = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                testTag = "fab_add_assessment"
            )
        }
    }

    if (showAddDialog || selectedForEdit != null) {
        AddAssessmentDialog(
            courses = courses,
            initialAssessment = selectedForEdit,
            onDismiss = {
                showAddDialog = false
                selectedForEdit = null
            },
            onSave = { assessment ->
                viewModel.saveAssessment(assessment)
                showAddDialog = false
                selectedForEdit = null
            }
        )
    }
}

@Composable
private fun AssessmentCard(
    assessment: AssessmentEntity,
    onToggleComplete: () -> Unit,
    onInitDefaultSubtasks: () -> Unit = {},
    onAddSubtask: (String) -> Unit = {},
    onToggleSubtask: (String) -> Unit = {},
    onRemoveSubtask: (String) -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = assessment.status == "COMPLETED" || assessment.status == "SUBMITTED"
    val subtasks = remember(assessment.subtasksJson) {
        SubtaskChecklistHelper.parseSubtasks(assessment.subtasksJson)
    }
    var checklistExpanded by remember { mutableStateOf(false) }
    var newStepText by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val isOverdue = !isCompleted && assessment.dueEpochMillis < now

    val priorityColor = when (assessment.priority) {
        "URGENT" -> LocalAuraColors.current.danger
        "HIGH" -> LocalAuraColors.current.warning
        "MEDIUM" -> LocalAuraColors.current.primaryAccent
        else -> LocalAuraColors.current.success
    }

    val dueDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(assessment.dueEpochMillis),
        ZoneId.systemDefault()
    )
    val formattedDate = dueDate.format(DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a", Locale.US))

    val countdownText = remember(assessment.dueEpochMillis, isCompleted, isOverdue) {
        if (isCompleted) "Completed"
        else if (isOverdue) "Overdue"
        else {
            val diffHours = ((assessment.dueEpochMillis - now) / (1000 * 3600)).coerceAtLeast(1)
            val diffDays = diffHours / 24
            if (diffDays > 0) "Due in $diffDays days" else "Due in $diffHours hrs"
        }
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assessment_card_${assessment.id}"),
        backgroundColor = if (isCompleted) LocalAuraColors.current.surfaceElevated.copy(alpha = 0.5f) else null,
        borderColor = if (isOverdue) LocalAuraColors.current.danger.copy(alpha = 0.65f) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox toggle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompleted) LocalAuraColors.current.success else Color(0x28FFFFFF))
                        .clickable(onClick = onToggleComplete),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                AuraBadge(
                    text = assessment.courseCode,
                    backgroundColor = LocalAuraColors.current.surfaceElevated,
                    textColor = LocalAuraColors.current.secondaryAccent
                )

                Spacer(modifier = Modifier.width(6.dp))

                AuraBadge(
                    text = assessment.type.replace('_', ' '),
                    backgroundColor = LocalAuraColors.current.surfaceElevated,
                    textColor = LocalAuraColors.current.primaryAccent
                )

                Spacer(modifier = Modifier.weight(1f))

                AuraBadge(
                    text = assessment.priority,
                    backgroundColor = priorityColor.copy(alpha = 0.18f),
                    textColor = priorityColor
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = LocalAuraColors.current.secondaryAccent, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = LocalAuraColors.current.danger, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = assessment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) LocalAuraColors.current.textMuted else LocalAuraColors.current.textPrimary,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )

            if (assessment.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = assessment.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary,
                    maxLines = 2
                )
            }

            if (assessment.examVenue.isNotBlank() || assessment.seatNumber.isNotBlank() || assessment.indexNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1F10B981))
                        .border(1.dp, AuraMintAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("task_hall_pass_strip_${assessment.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = AuraMintAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val passSummary = buildList {
                        if (assessment.examVenue.isNotBlank()) add(assessment.examVenue)
                        if (assessment.seatNumber.isNotBlank()) add("Seat ${assessment.seatNumber}")
                        if (assessment.indexNumber.isNotBlank()) add("ID ${assessment.indexNumber}")
                    }.joinToString(" • ")
                    Text(
                        text = "Hall Pass: $passSummary",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraMintAccent,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Assignment Milestone Sub-Checklist Strip
            val doneSubtasks = subtasks.count { it.isDone }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x18FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checklistExpanded = !checklistExpanded }
                            .testTag("assessment_checklist_toggle_${assessment.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (subtasks.isEmpty()) {
                                "Milestone Sub-Checklist"
                            } else {
                                "Milestones: $doneSubtasks / ${subtasks.size} steps done"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (subtasks.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LocalAuraColors.current.primaryAccent.copy(alpha = 0.24f),
                                modifier = Modifier
                                    .clickable {
                                        onInitDefaultSubtasks()
                                        checklistExpanded = true
                                    }
                                    .testTag("assessment_autofill_subtasks_${assessment.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = LocalAuraColors.current.secondaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto-Fill Steps",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.secondaryAccent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = if (checklistExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Milestones",
                            tint = LocalAuraColors.current.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val stepRatio = doneSubtasks.toFloat() / subtasks.size.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(stepRatio.coerceAtLeast(0.04f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(AuraMintAccent)
                            )
                        }
                    }

                    if (checklistExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        subtasks.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = step.isDone,
                                    onCheckedChange = { onToggleSubtask(step.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AuraMintAccent,
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = step.title,
                                    fontSize = 12.sp,
                                    color = if (step.isDone) LocalAuraColors.current.textMuted else LocalAuraColors.current.textPrimary,
                                    textDecoration = if (step.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRemoveSubtask(step.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove milestone",
                                        tint = LocalAuraColors.current.textMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newStepText,
                                onValueChange = { newStepText = it },
                                placeholder = { Text("Add milestone step...", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("assessment_new_subtask_input_${assessment.id}"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            AuraGlassButton(
                                text = "Add",
                                icon = Icons.Default.Add,
                                isPrimary = true,
                                cornerRadius = 10.dp,
                                onClick = {
                                    if (newStepText.isNotBlank()) {
                                        onAddSubtask(newStepText)
                                        newStepText = ""
                                    }
                                },
                                testTag = "assessment_add_subtask_button_${assessment.id}"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (isOverdue) LocalAuraColors.current.danger else LocalAuraColors.current.warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) LocalAuraColors.current.danger else LocalAuraColors.current.secondaryAccent
                )
            }
        }
    }
}
