package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CourseEntity
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraTimePickerField
import com.example.ui.theme.LocalAuraColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun AddAssessmentDialog(
    courses: List<CourseEntity>,
    initialAssessment: AssessmentEntity? = null,
    onDismiss: () -> Unit,
    onSave: (AssessmentEntity) -> Unit
) {
    val initialDateTime = remember(initialAssessment) {
        initialAssessment?.dueEpochMillis?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }
    }

    var selectedCourse by remember {
        mutableStateOf(courses.find { it.id == initialAssessment?.courseId } ?: courses.firstOrNull())
    }
    var manualCourseCode by remember { mutableStateOf(initialAssessment?.courseCode ?: "CSCD 301") }
    var title by remember { mutableStateOf(initialAssessment?.title ?: "") }
    var type by remember { mutableStateOf(initialAssessment?.type ?: "ASSIGNMENT") }
    var priority by remember { mutableStateOf(initialAssessment?.priority ?: "HIGH") }
    var daysAhead by remember {
        mutableIntStateOf(
            if (initialDateTime != null) {
                ChronoUnit.DAYS.between(LocalDate.now(), initialDateTime.toLocalDate()).toInt().coerceAtLeast(0)
            } else 3
        )
    }
    var timeStr by remember {
        mutableStateOf(
            if (initialDateTime != null) {
                String.format(Locale.US, "%02d:%02d", initialDateTime.hour, initialDateTime.minute)
            } else "17:00"
        )
    }
    var reminderMinutes by remember { mutableIntStateOf(initialAssessment?.reminderMinutesBefore ?: 60) }
    var description by remember { mutableStateOf(initialAssessment?.description ?: "") }
    var notes by remember { mutableStateOf(initialAssessment?.notes ?: "") }
    var examVenue by remember { mutableStateOf(initialAssessment?.examVenue ?: "") }
    var seatNumber by remember { mutableStateOf(initialAssessment?.seatNumber ?: "") }
    var indexNumber by remember { mutableStateOf(initialAssessment?.indexNumber ?: "") }
    var materialsChecklist by remember {
        mutableStateOf(
            initialAssessment?.materialsChecklist?.ifBlank { "Student ID, Pens, Scientific Calculator" }
                ?: "Student ID, Pens, Scientific Calculator"
        )
    }
    val isExamType = type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST", "QUIZ")

    val types = listOf(
        "ASSIGNMENT" to "Assignment",
        "QUIZ" to "Quiz",
        "CLASS_TEST" to "Class Test",
        "MID_SEM" to "Mid-Semester",
        "FINAL_EXAM" to "Final Exam",
        "PROJECT" to "Project",
        "PRESENTATION" to "Presentation"
    )

    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")
    val daysOptions = listOf(0, 1, 2, 3, 5, 7, 14, 30)
    val reminderOptions = listOf(
        0 to "At due time",
        15 to "15m before",
        60 to "1h before",
        360 to "6h before",
        1440 to "1 day before"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialAssessment == null) "New Assessment" else "Edit Assessment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
                Text(
                    text = "Set due date, AM/PM time & alarm reminder",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.secondaryAccent
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (courses.isNotEmpty()) {
                    Text("Select Course", style = MaterialTheme.typography.labelMedium, color = LocalAuraColors.current.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(courses) { course ->
                            val isSelected = selectedCourse?.id == course.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xAAFFFFFF) else Color(0x30FFFFFF)
                                ),
                                modifier = Modifier.clickable { selectedCourse = course }
                            ) {
                                Text(
                                    text = course.code,
                                    color = if (isSelected) Color.White else LocalAuraColors.current.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = manualCourseCode,
                        onValueChange = { manualCourseCode = it },
                        label = { Text("Course Code (e.g. CSCD 301)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_assessment_course_code"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                            unfocusedBorderColor = Color(0x35FFFFFF),
                            focusedTextColor = LocalAuraColors.current.textPrimary,
                            unfocusedTextColor = LocalAuraColors.current.textPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Lab Assignment 2)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_assessment_title"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                        unfocusedBorderColor = Color(0x35FFFFFF),
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                Text("Type", style = MaterialTheme.typography.labelMedium, color = LocalAuraColors.current.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { (key, label) ->
                        val isSelected = type == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xAAFFFFFF) else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier.clickable { type = key }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else LocalAuraColors.current.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text("Priority Level", style = MaterialTheme.typography.labelMedium, color = LocalAuraColors.current.textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    priorities.forEach { p ->
                        val isSelected = priority == p
                        val pColor = when (p) {
                            "URGENT" -> LocalAuraColors.current.danger
                            "HIGH" -> LocalAuraColors.current.warning
                            "MEDIUM" -> LocalAuraColors.current.primaryAccent
                            else -> LocalAuraColors.current.textMuted
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) pColor.copy(alpha = 0.28f) else Color(0x18FFFFFF))
                                .border(1.dp, if (isSelected) pColor else Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { priority = p }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) pColor else LocalAuraColors.current.textSecondary
                            )
                        }
                    }
                }

                Text("Due Date & AM/PM Time", style = MaterialTheme.typography.labelMedium, color = LocalAuraColors.current.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(daysOptions) { d ->
                        val isSelected = daysAhead == d
                        val dayLabel = when (d) {
                            0 -> "Today"
                            1 -> "Tomorrow"
                            else -> "$d days"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xAAFFFFFF) else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier.clickable { daysAhead = d }
                        ) {
                            Text(
                                text = dayLabel,
                                color = if (isSelected) Color.White else LocalAuraColors.current.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                AuraTimePickerField(
                    label = "Due / Exam Time",
                    time24 = timeStr,
                    onTimeSelected = { timeStr = it },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "input_assessment_due_time"
                )

                Text("Alarm Reminder", style = MaterialTheme.typography.labelMedium, color = LocalAuraColors.current.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reminderOptions) { (mins, label) ->
                        val isSelected = reminderMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xAAFFFFFF) else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier.clickable { reminderMinutes = mins }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else LocalAuraColors.current.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isExamType) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1C38BDF8))
                            .border(1.dp, Color(0x555CE1FF), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Official Exam Hall Pass & Seat Card",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = LocalAuraColors.current.textPrimary
                            )
                        }
                        OutlinedTextField(
                            value = examVenue,
                            onValueChange = { examVenue = it },
                            label = { Text("Exam Hall / Venue (e.g. Great Hall B)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_exam_venue"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                                unfocusedBorderColor = Color(0x35FFFFFF),
                                focusedTextColor = LocalAuraColors.current.textPrimary,
                                unfocusedTextColor = LocalAuraColors.current.textPrimary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = seatNumber,
                                onValueChange = { seatNumber = it },
                                label = { Text("Seat # (e.g. R4-18)") },
                                modifier = Modifier.weight(1f).testTag("input_exam_seat"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                                    unfocusedBorderColor = Color(0x35FFFFFF),
                                    focusedTextColor = LocalAuraColors.current.textPrimary,
                                    unfocusedTextColor = LocalAuraColors.current.textPrimary
                                )
                            )
                            OutlinedTextField(
                                value = indexNumber,
                                onValueChange = { indexNumber = it },
                                label = { Text("Index / ID #") },
                                modifier = Modifier.weight(1f).testTag("input_exam_index"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                                    unfocusedBorderColor = Color(0x35FFFFFF),
                                    focusedTextColor = LocalAuraColors.current.textPrimary,
                                    unfocusedTextColor = LocalAuraColors.current.textPrimary
                                )
                            )
                        }
                        OutlinedTextField(
                            value = materialsChecklist,
                            onValueChange = { materialsChecklist = it },
                            label = { Text("Required Exam Materials Checklist") },
                            modifier = Modifier.fillMaxWidth().testTag("input_exam_materials"),
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                                unfocusedBorderColor = Color(0x35FFFFFF),
                                focusedTextColor = LocalAuraColors.current.textPrimary,
                                unfocusedTextColor = LocalAuraColors.current.textPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details / Submission Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_assessment_description"),
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                        unfocusedBorderColor = Color(0x35FFFFFF),
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Save Assessment",
                isPrimary = true,
                onClick = {
                    val cCode = selectedCourse?.code ?: manualCourseCode.ifBlank { "CSCD 301" }
                    val cName = selectedCourse?.name ?: cCode
                    val cId = selectedCourse?.id ?: 0L
                    val safeTitle = title.ifBlank {
                        "${type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} for $cCode"
                    }

                    val targetDate = LocalDate.now().plusDays(daysAhead.toLong())
                    val parts = timeStr.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 17
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val dueMillis = targetDate.atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val assessment = AssessmentEntity(
                        id = initialAssessment?.id ?: 0L,
                        courseId = cId,
                        courseCode = cCode.trim().uppercase(),
                        courseName = cName.trim(),
                        title = safeTitle.trim(),
                        type = type,
                        description = description.trim(),
                        dueEpochMillis = dueMillis,
                        priority = priority,
                        status = initialAssessment?.status ?: "PENDING",
                        notes = notes.trim(),
                        reminderMinutesBefore = reminderMinutes,
                        reminderEnabled = true,
                        examVenue = examVenue.trim(),
                        seatNumber = seatNumber.trim(),
                        indexNumber = indexNumber.trim(),
                        materialsChecklist = materialsChecklist.trim()
                    )
                    onSave(assessment)
                },
                testTag = "save_assessment_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_assessment_button"
            )
        },
        containerColor = Color(0xF0101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
            RoundedCornerShape(24.dp)
        )
    )
}
