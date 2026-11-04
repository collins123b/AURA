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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.LocalAuraColors

@Composable
fun AddCourseDialog(
    initialCourse: CourseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit
) {
    val keyId = initialCourse?.id ?: -1L
    var code by remember(keyId) { mutableStateOf(initialCourse?.code ?: "") }
    var name by remember(keyId) { mutableStateOf(initialCourse?.name ?: "") }
    var lecturer by remember(keyId) { mutableStateOf(initialCourse?.lecturer ?: "") }
    var venue by remember(keyId) { mutableStateOf(initialCourse?.venue ?: "") }
    var creditHours by remember(keyId) { mutableIntStateOf(initialCourse?.creditHours ?: 3) }
    var attendanceTarget by remember(keyId) { mutableIntStateOf(initialCourse?.attendanceTargetPercent ?: 75) }
    var selectedColor by remember(keyId) { mutableStateOf(initialCourse?.colorHex ?: "#0A84FF") }

    val colors = listOf("#0A84FF", "#64D2FF", "#38BDF8", "#2563EB", "#6366F1", "#2DD4BF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCourse == null) "Add University Course" else "Edit Course",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. CSCD 301)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_course_code"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.primaryAccent,
                        unfocusedBorderColor = LocalAuraColors.current.border,
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Title") },
                    modifier = Modifier.fillMaxWidth().testTag("input_course_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.primaryAccent,
                        unfocusedBorderColor = LocalAuraColors.current.border,
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    label = { Text("Lecturer") },
                    modifier = Modifier.fillMaxWidth().testTag("input_course_lecturer"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.primaryAccent,
                        unfocusedBorderColor = LocalAuraColors.current.border,
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Default Venue") },
                    modifier = Modifier.fillMaxWidth().testTag("input_course_venue"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.primaryAccent,
                        unfocusedBorderColor = LocalAuraColors.current.border,
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = creditHours.toString(),
                        onValueChange = { creditHours = it.toIntOrNull() ?: 3 },
                        label = { Text("Credits") },
                        modifier = Modifier.weight(1f).testTag("input_course_credits"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAuraColors.current.primaryAccent,
                            unfocusedBorderColor = LocalAuraColors.current.border,
                            focusedTextColor = LocalAuraColors.current.textPrimary,
                            unfocusedTextColor = LocalAuraColors.current.textPrimary
                        )
                    )
                    OutlinedTextField(
                        value = attendanceTarget.toString(),
                        onValueChange = { attendanceTarget = it.toIntOrNull() ?: 75 },
                        label = { Text("Target %") },
                        modifier = Modifier.weight(1f).testTag("input_course_target_attendance"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAuraColors.current.primaryAccent,
                            unfocusedBorderColor = LocalAuraColors.current.border,
                            focusedTextColor = LocalAuraColors.current.textPrimary,
                            unfocusedTextColor = LocalAuraColors.current.textPrimary
                        )
                    )
                }

                Text(
                    text = "Course Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAuraColors.current.textSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(colors) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 3.dp else 0.dp,
                                    Color.White,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Save Course",
                isPrimary = true,
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onSave(
                            CourseEntity(
                                id = initialCourse?.id ?: 0L,
                                code = code.trim().uppercase(),
                                name = name.trim(),
                                lecturer = lecturer.trim(),
                                venue = venue.trim(),
                                creditHours = creditHours,
                                attendanceTargetPercent = attendanceTarget,
                                colorHex = selectedColor
                            )
                        )
                    }
                },
                testTag = "save_course_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_course_button"
            )
        },
        containerColor = Color(0xF0101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
            RoundedCornerShape(24.dp)
        )
    )
}
