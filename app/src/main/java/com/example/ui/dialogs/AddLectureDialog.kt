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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.LectureEntity
import com.example.notifications.AlarmToneOption
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.AuraTimePickerField
import com.example.ui.components.computeLeaveByTime12Hour
import com.example.ui.components.formatTime12Hour
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.LocalAuraColors
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun AddLectureDialog(
    courses: List<CourseEntity>,
    initialLecture: LectureEntity? = null,
    onManageCourses: (() -> Unit)? = null,
    previewingToneId: String? = null,
    onPreviewTone: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (LectureEntity) -> Unit
) {
    var useManualNewCourse by remember { mutableStateOf(false) }
    var selectedCourse by remember(courses, initialLecture) {
        mutableStateOf(
            courses.find { it.id == initialLecture?.courseId }
                ?: courses.find { it.code.equals(initialLecture?.courseCode, ignoreCase = true) }
                ?: courses.firstOrNull()
        )
    }
    var manualCourseCode by remember { mutableStateOf(initialLecture?.courseCode ?: "") }
    var manualCourseName by remember { mutableStateOf(initialLecture?.courseName ?: "") }
    var dayOfWeek by remember {
        mutableIntStateOf(initialLecture?.dayOfWeek ?: LocalDate.now().dayOfWeek.value)
    }
    var startTime by remember { mutableStateOf(initialLecture?.startTime ?: "08:30") }
    var endTime by remember { mutableStateOf(initialLecture?.endTime ?: "10:30") }
    var venue by remember { mutableStateOf(initialLecture?.venue ?: (selectedCourse?.venue ?: "Lecture Hall 1")) }
    var lecturer by remember { mutableStateOf(initialLecture?.lecturer ?: (selectedCourse?.lecturer ?: "")) }
    var reminderEnabled by remember { mutableStateOf(initialLecture?.reminderEnabled ?: true) }
    var reminderMinutes by remember { mutableIntStateOf(initialLecture?.reminderMinutesBefore ?: 15) }
    var walkingMinutes by remember { mutableIntStateOf(initialLecture?.walkingMinutes ?: 10) }
    var selectedAlarmToneId by remember { mutableStateOf(initialLecture?.alarmToneId ?: "") }

    val days = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed",
        4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
    )

    val reminderOptions = listOf(0, 5, 10, 15, 30, 60)
    val walkingOptions = listOf(5, 10, 15, 20, 30)
    val quickStartTimes = listOf("08:00", "10:00", "12:00", "14:00", "16:00")
    val lectureToneOptions = remember {
        listOf("" to "Default Sound") + AlarmToneOption.entries.map { it.id to it.displayName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialLecture == null) "Schedule Lecture" else "Edit Lecture",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
                Text(
                    text = "Tap Start or End time to pick AM / PM",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (courses.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Course",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalAuraColors.current.textSecondary
                        )
                        if (onManageCourses != null) {
                            Text(
                                text = "Edit / Delete Courses",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier
                                    .clickable {
                                        onDismiss()
                                        onManageCourses()
                                    }
                                    .padding(4.dp)
                                    .testTag("lecture_dialog_manage_courses_link")
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (useManualNewCourse) AuraMintAccent.copy(alpha = 0.28f) else Color(0x1EFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (useManualNewCourse) AuraMintAccent else Color(0x38FFFFFF)
                                ),
                                modifier = Modifier
                                    .clickable { useManualNewCourse = !useManualNewCourse }
                                    .testTag("lecture_dialog_new_course_chip")
                            ) {
                                Text(
                                    text = "+ New Course",
                                    color = if (useManualNewCourse) AuraMintAccent else LocalAuraColors.current.secondaryAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                        items(courses) { course ->
                            val isSelected = !useManualNewCourse && selectedCourse?.id == course.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xAAFFFFFF) else Color(0x35FFFFFF)
                                ),
                                modifier = Modifier.clickable {
                                    useManualNewCourse = false
                                    selectedCourse = course
                                    if (venue.isEmpty()) venue = course.venue
                                    if (lecturer.isEmpty()) lecturer = course.lecturer
                                }
                            ) {
                                Text(
                                    text = course.code,
                                    color = if (isSelected) Color.White else LocalAuraColors.current.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                if (courses.isEmpty() || useManualNewCourse) {
                    OutlinedTextField(
                        value = manualCourseCode,
                        onValueChange = { manualCourseCode = it },
                        label = { Text("Course Code (e.g. CSCD 301)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_lecture_course_code"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                            unfocusedBorderColor = Color(0x35FFFFFF),
                            focusedTextColor = LocalAuraColors.current.textPrimary,
                            unfocusedTextColor = LocalAuraColors.current.textPrimary
                        )
                    )
                    OutlinedTextField(
                        value = manualCourseName,
                        onValueChange = { manualCourseName = it },
                        label = { Text("Course Title (e.g. Operating Systems)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_lecture_course_name"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                            unfocusedBorderColor = Color(0x35FFFFFF),
                            focusedTextColor = LocalAuraColors.current.textPrimary,
                            unfocusedTextColor = LocalAuraColors.current.textPrimary
                        )
                    )
                }

                // Day of Week Picker
                Text(
                    text = "Lecture Day",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAuraColors.current.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEach { (num, name) ->
                        val isSelected = dayOfWeek == num
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) LocalAuraColors.current.primaryAccent
                                    else Color(0x22FFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xBBFFFFFF) else Color(0x30FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { dayOfWeek = num }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else LocalAuraColors.current.textSecondary
                            )
                        }
                    }
                }

                // 12-Hour AM / PM Time Selectors (No manual typing needed!)
                Text(
                    text = "Lecture Time (Select AM / PM)",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAuraColors.current.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraTimePickerField(
                        label = "Start Time",
                        time24 = startTime,
                        onTimeSelected = { newStart ->
                            startTime = newStart
                            // Auto-adjust endTime +2h if endTime <= startTime
                            try {
                                val sp = newStart.split(":")
                                val st = LocalTime.of(sp[0].toInt(), sp[1].toInt())
                                val ep = endTime.split(":")
                                val et = LocalTime.of(ep[0].toInt(), ep[1].toInt())
                                if (!et.isAfter(st)) {
                                    val autoEnd = st.plusHours(2)
                                    endTime = String.format(Locale.US, "%02d:%02d", autoEnd.hour, autoEnd.minute)
                                }
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "input_lecture_start_time"
                    )
                    AuraTimePickerField(
                        label = "End Time",
                        time24 = endTime,
                        onTimeSelected = { endTime = it },
                        modifier = Modifier.weight(1f),
                        testTag = "input_lecture_end_time"
                    )
                }

                // Quick One-Tap Time Slot Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickStartTimes) { slot ->
                        val isSlotSelected = startTime == slot
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSlotSelected) LocalAuraColors.current.secondaryAccent.copy(alpha = 0.28f) else Color(0x18FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSlotSelected) LocalAuraColors.current.secondaryAccent else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier.clickable {
                                startTime = slot
                                val parts = slot.split(":")
                                val endHour = ((parts[0].toIntOrNull() ?: 8) + 2).coerceAtMost(23)
                                endTime = String.format(Locale.US, "%02d:00", endHour)
                            }
                        ) {
                            Text(
                                text = formatTime12Hour(slot),
                                color = if (isSlotSelected) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Venue
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Room") },
                    modifier = Modifier.fillMaxWidth().testTag("input_lecture_venue"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                        unfocusedBorderColor = Color(0x35FFFFFF),
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                // Lecturer
                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    label = { Text("Lecturer Name (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_lecture_lecturer"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAuraColors.current.secondaryAccent,
                        unfocusedBorderColor = Color(0x35FFFFFF),
                        focusedTextColor = LocalAuraColors.current.textPrimary,
                        unfocusedTextColor = LocalAuraColors.current.textPrimary
                    )
                )

                // Smart Campus Walk Departure Buffer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1810B981))
                        .border(1.dp, AuraMintAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = AuraMintAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Campus Walk Buffer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Leave by ${computeLeaveByTime12Hour(startTime, walkingMinutes)} (${walkingMinutes}m walk)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraMintAccent,
                            modifier = Modifier.testTag("lecture_leave_by_preview")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(walkingOptions) { mins ->
                            val isSelected = walkingMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AuraMintAccent.copy(alpha = 0.28f) else Color(0x18FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AuraMintAccent else Color(0x30FFFFFF)
                                ),
                                modifier = Modifier
                                    .clickable { walkingMinutes = mins }
                                    .testTag("walk_buffer_${mins}m")
                            ) {
                                Text(
                                    text = "${mins}m walk",
                                    color = if (isSelected) AuraMintAccent else LocalAuraColors.current.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Ringing Alarm Toggle + Reminder Interval
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1EFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audible Lecture Alarm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = if (reminderEnabled) "Rings & vibrates on schedule" else "Alarm muted for this lecture",
                            fontSize = 11.sp,
                            color = LocalAuraColors.current.textSecondary
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = LocalAuraColors.current.primaryAccent),
                        modifier = Modifier.testTag("lecture_dialog_alarm_switch")
                    )
                }

                if (reminderEnabled) {
                    Text(
                        text = "When to Ring",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalAuraColors.current.textSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(reminderOptions) { mins ->
                            val isSelected = reminderMinutes == mins
                            val label = if (mins == 0) "At exact time" else "${mins}m before"
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Alarm Sound for This Lecture",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalAuraColors.current.textSecondary
                        )
                        if (onPreviewTone != null) {
                            val effectivePreviewKey = selectedAlarmToneId.ifBlank { AlarmToneOption.AURORA_CHIME.id }
                            val isPreviewing = previewingToneId == effectivePreviewKey
                            AuraGlassIconButton(
                                icon = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Preview Lecture Alarm Tone",
                                onClick = { onPreviewTone(selectedAlarmToneId) },
                                size = 34.dp,
                                iconSize = 16.dp,
                                tint = LocalAuraColors.current.secondaryAccent,
                                testTag = "lecture_dialog_preview_tone_button"
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(lectureToneOptions) { (toneId, toneLabel) ->
                            val isSelected = selectedAlarmToneId == toneId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) LocalAuraColors.current.secondaryAccent.copy(alpha = 0.28f) else Color(0x18FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LocalAuraColors.current.secondaryAccent else Color(0x28FFFFFF)
                                ),
                                modifier = Modifier.clickable { selectedAlarmToneId = toneId }
                            ) {
                                Text(
                                    text = toneLabel,
                                    color = if (isSelected) Color.White else LocalAuraColors.current.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Save Lecture",
                isPrimary = true,
                onClick = {
                    val chosenCourse = if (useManualNewCourse) null else selectedCourse
                    val cCode = chosenCourse?.code ?: manualCourseCode.ifBlank { "CSCD 301" }
                    val cName = chosenCourse?.name ?: manualCourseName.ifBlank { cCode }
                    val cId = chosenCourse?.id ?: 0L
                    val lecture = LectureEntity(
                        id = initialLecture?.id ?: 0L,
                        courseId = cId,
                        courseCode = cCode.trim().uppercase(),
                        courseName = cName.trim(),
                        dayOfWeek = dayOfWeek,
                        startTime = startTime.ifBlank { "08:30" },
                        endTime = endTime.ifBlank { "10:30" },
                        venue = venue.ifBlank { "Lecture Hall 1" },
                        lecturer = lecturer.trim(),
                        reminderMinutesBefore = reminderMinutes,
                        reminderEnabled = reminderEnabled,
                        isRecurring = initialLecture?.isRecurring ?: true,
                        colorHex = chosenCourse?.colorHex ?: initialLecture?.colorHex ?: "#0A84FF",
                        alarmToneId = selectedAlarmToneId,
                        customAlarmPath = initialLecture?.customAlarmPath ?: "",
                        customAlarmLabel = initialLecture?.customAlarmLabel ?: "",
                        walkingMinutes = walkingMinutes
                    )
                    onSave(lecture)
                },
                testTag = "save_lecture_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_lecture_button"
            )
        },
        containerColor = Color(0xF0101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3364D2FF))),
                RoundedCornerShape(24.dp)
            )
            .testTag("add_lecture_dialog")
    )
}
