package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CourseEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.LocalAuraColors

@Composable
fun CourseManagerDialog(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit,
    onOpenCourseSlides: ((String) -> Unit)? = null
) {
    val courses by viewModel.courses.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val resources by viewModel.courseResources.collectAsState()

    var showCourseForm by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<CourseEntity?>(null) }
    var coursePendingDelete by remember { mutableStateOf<CourseEntity?>(null) }
    var alsoDeleteCourseSlides by remember { mutableStateOf(false) }

    val totalCredits = remember(courses) { courses.sumOf { it.creditHours } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .border(
                    1.2.dp,
                    Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x4064D2FF))),
                    RoundedCornerShape(28.dp)
                )
                .testTag("course_manager_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF20D172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enrolled Courses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = "${courses.size} courses • $totalCredits total credit hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                    }
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Course Manager",
                        onClick = onDismiss,
                        size = 48.dp,
                        tint = LocalAuraColors.current.textPrimary,
                        testTag = "close_course_manager_button"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Add Course Button
                AuraGlassButton(
                    text = "Add New Course",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = {
                        courseToEdit = null
                        showCourseForm = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "course_manager_add_button"
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (courses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = LocalAuraColors.current.secondaryAccent,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Courses Added Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap 'Add New Course' above to register your semester courses. You can edit or delete any course here anytime.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("course_manager_list"),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(courses, key = { it.id }) { course ->
                            val courseColor = remember(course.colorHex) {
                                runCatching { Color(android.graphics.Color.parseColor(course.colorHex)) }
                                    .getOrDefault(Color(0xFF0A84FF))
                            }
                            val lectureCount = remember(lectures, course.id) {
                                lectures.count { it.courseId == course.id }
                            }
                            val slideCount = remember(resources, course.id, course.code) {
                                resources.count {
                                    it.courseId == course.id || it.courseCode.equals(course.code, ignoreCase = true)
                                }
                            }

                            AuraCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("course_item_${course.id}"),
                                cornerRadius = 20.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(courseColor)
                                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AuraBadge(
                                            text = course.code,
                                            backgroundColor = courseColor.copy(alpha = 0.24f),
                                            textColor = LocalAuraColors.current.secondaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AuraBadge(
                                            text = "${course.creditHours} Cr • ${course.attendanceTargetPercent}% Target",
                                            backgroundColor = Color(0x1FFFFFFF),
                                            textColor = LocalAuraColors.current.textSecondary
                                        )
                                        Spacer(modifier = Modifier.weight(1f))

                                        AuraGlassIconButton(
                                            icon = Icons.Default.Edit,
                                            contentDescription = "Edit ${course.code}",
                                            onClick = {
                                                courseToEdit = course
                                                showCourseForm = true
                                            },
                                            size = 48.dp,
                                            iconSize = 18.dp,
                                            tint = LocalAuraColors.current.secondaryAccent,
                                            testTag = "edit_course_${course.id}"
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        AuraGlassIconButton(
                                            icon = Icons.Default.Delete,
                                            contentDescription = "Delete ${course.code}",
                                            onClick = {
                                                alsoDeleteCourseSlides = false
                                                coursePendingDelete = course
                                            },
                                            size = 48.dp,
                                            iconSize = 18.dp,
                                            tint = LocalAuraColors.current.danger,
                                            testTag = "delete_course_${course.id}"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.textPrimary
                                    )

                                    if (course.lecturer.isNotBlank() || course.venue.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (course.lecturer.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = LocalAuraColors.current.aiAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = course.lecturer,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = LocalAuraColors.current.textSecondary
                                                    )
                                                }
                                            }
                                            if (course.venue.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = LocalAuraColors.current.secondaryAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = course.venue,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = LocalAuraColors.current.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$lectureCount lecture(s) • $slideCount slide deck(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LocalAuraColors.current.textMuted
                                        )

                                        if (onOpenCourseSlides != null) {
                                            AuraGlassButton(
                                                text = "Course Slides ($slideCount)",
                                                icon = Icons.Default.FolderOpen,
                                                onClick = {
                                                    onDismiss()
                                                    onOpenCourseSlides(course.code)
                                                },
                                                cornerRadius = 14.dp,
                                                testTag = "open_course_slides_${course.id}"
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
    }

    // Add or Edit Course Modal
    if (showCourseForm) {
        AddCourseDialog(
            initialCourse = courseToEdit,
            onDismiss = {
                showCourseForm = false
                courseToEdit = null
            },
            onSave = { savedCourse ->
                viewModel.saveCourse(savedCourse)
                showCourseForm = false
                courseToEdit = null
            }
        )
    }

    // Confirm Course Deletion Modal
    val targetDelete = coursePendingDelete
    if (targetDelete != null) {
        AlertDialog(
            onDismissRequest = { coursePendingDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LocalAuraColors.current.danger,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete ${targetDelete.code}?",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Removing '${targetDelete.name}' will also cancel and remove its scheduled timetable lectures and attendance records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalAuraColors.current.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF))
                            .clickable { alsoDeleteCourseSlides = !alsoDeleteCourseSlides }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alsoDeleteCourseSlides,
                            onCheckedChange = { alsoDeleteCourseSlides = it },
                            colors = CheckboxDefaults.colors(checkedColor = LocalAuraColors.current.danger),
                            modifier = Modifier.testTag("checkbox_delete_course_slides")
                        )
                        Text(
                            text = "Also delete uploaded slides & resources for ${targetDelete.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Delete Course",
                    isPrimary = true,
                    containerColor = LocalAuraColors.current.danger,
                    onClick = {
                        viewModel.deleteCourse(targetDelete, deleteUploadedSlides = alsoDeleteCourseSlides)
                        coursePendingDelete = null
                    },
                    testTag = "confirm_delete_course_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { coursePendingDelete = null },
                    testTag = "cancel_delete_course_button"
                )
            },
            containerColor = Color(0xF2101B30),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .border(
                    1.2.dp,
                    Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x33FF453A))),
                    RoundedCornerShape(24.dp)
                )
                .testTag("confirm_delete_course_dialog")
        )
    }
}
