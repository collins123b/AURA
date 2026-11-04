package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.OfficeHourEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.AuraTimePickerField
import com.example.ui.components.formatTime12Hour
import com.example.ui.theme.LocalAuraColors
import java.time.LocalDate

@Composable
fun OfficeHoursView(
    viewModel: AuraViewModel
) {
    val context = LocalContext.current
    val officeHours by viewModel.officeHours.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var selectedDayFilter by remember { mutableIntStateOf(0) } // 0 = All days, 1..7 = Mon..Sun
    var editingOfficeHour by remember { mutableStateOf<OfficeHourEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val todayDow = LocalDate.now().dayOfWeek.value

    val dayFilters = listOf(
        0 to "All Days",
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    val filteredSlots = remember(officeHours, selectedDayFilter) {
        if (selectedDayFilter == 0) officeHours
        else officeHours.filter { it.dayOfWeek == selectedDayFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("office_hours_hub_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AuraCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LocalAuraColors.current.secondaryAccent.copy(alpha = 0.22f))
                            .border(1.dp, Color(0x66FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lecturer Office Hours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        val todayCount = officeHours.count { it.dayOfWeek == todayDow }
                        Text(
                            text = "${officeHours.size} slots • $todayCount available today",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                    }
                    AuraGlassButton(
                        text = "Add Slot",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = {
                            editingOfficeHour = null
                            showDialog = true
                        },
                        testTag = "add_office_hour_button"
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayFilters) { (dayNum, label) ->
                    val isSelected = selectedDayFilter == dayNum
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xBBFFFFFF) else Color(0x35FFFFFF)
                        ),
                        modifier = Modifier.clickable { selectedDayFilter = dayNum }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else LocalAuraColors.current.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (filteredSlots.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Office Hours Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save professor & TA consultation hours with AM/PM time pickers, office rooms, consultation agendas, and 15-minute wakeup reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredSlots, key = { it.id }) { slot ->
                OfficeHourGlassCard(
                    slot = slot,
                    isToday = slot.dayOfWeek == todayDow,
                    onEdit = {
                        editingOfficeHour = slot
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteOfficeHour(slot) },
                    onEmail = {
                        if (slot.email.isBlank()) {
                            Toast.makeText(context, "Add an email address to compose directly", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${slot.email.trim()}")
                                    putExtra(Intent.EXTRA_SUBJECT, "[${slot.courseCode}] Office Hours Inquiry")
                                    if (slot.agendaNotes.isNotBlank()) {
                                        putExtra(Intent.EXTRA_TEXT, "Dear ${slot.instructorName},\n\nRegarding:\n${slot.agendaNotes}\n")
                                    }
                                }
                                context.startActivity(mailIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Email: ${slot.email}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showDialog) {
        var courseCode by remember(editingOfficeHour) {
            mutableStateOf(editingOfficeHour?.courseCode ?: (courses.firstOrNull()?.code ?: "CSCD 301"))
        }
        var instructorName by remember(editingOfficeHour) {
            mutableStateOf(editingOfficeHour?.instructorName ?: (courses.firstOrNull()?.lecturer ?: ""))
        }
        var role by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.role ?: "Lecturer") }
        var officeLocation by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.officeLocation ?: "Room 204, Dept. Building") }
        var email by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.email ?: "") }
        var dayOfWeek by remember(editingOfficeHour) { mutableIntStateOf(editingOfficeHour?.dayOfWeek ?: todayDow) }
        var startTime by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.startTime ?: "14:00") }
        var endTime by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.endTime ?: "15:30") }
        var agendaNotes by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.agendaNotes ?: "") }
        var reminderEnabled by remember(editingOfficeHour) { mutableStateOf(editingOfficeHour?.reminderEnabled ?: true) }

        val roles = listOf("Lecturer", "Professor", "TA", "Advisor")
        val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Column {
                    Text(
                        text = if (editingOfficeHour == null) "Add Lecturer Office Hours" else "Edit Office Hours",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = "Select consultation day & AM / PM hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (courses.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(courses) { c ->
                                val selected = courseCode == c.code
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                                    modifier = Modifier.clickable {
                                        courseCode = c.code
                                        if (instructorName.isBlank() && c.lecturer.isNotBlank()) {
                                            instructorName = c.lecturer
                                        }
                                    }
                                ) {
                                    Text(
                                        text = c.code,
                                        color = if (selected) Color.White else LocalAuraColors.current.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            label = { Text("Course Code") },
                            modifier = Modifier.fillMaxWidth().testTag("input_office_course_code"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        roles.forEach { r ->
                            val selected = role == r
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) LocalAuraColors.current.primaryAccent else Color(0x1EFFFFFF))
                                    .clickable { role = r }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = r,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else LocalAuraColors.current.textSecondary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = instructorName,
                        onValueChange = { instructorName = it },
                        label = { Text("Instructor Name (e.g. Dr. Mensah)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_office_instructor"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = officeLocation,
                        onValueChange = { officeLocation = it },
                        label = { Text("Office Room / Building") },
                        modifier = Modifier.fillMaxWidth().testTag("input_office_location"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Instructor Email (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_office_email"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { (num, name) ->
                            val selected = dayOfWeek == num
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) LocalAuraColors.current.primaryAccent else Color(0x1EFFFFFF))
                                    .clickable { dayOfWeek = num }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else LocalAuraColors.current.textSecondary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraTimePickerField(
                            label = "Start Time",
                            time24 = startTime,
                            onTimeSelected = { startTime = it },
                            modifier = Modifier.weight(1f),
                            testTag = "input_office_start_time"
                        )
                        AuraTimePickerField(
                            label = "End Time",
                            time24 = endTime,
                            onTimeSelected = { endTime = it },
                            modifier = Modifier.weight(1f),
                            testTag = "input_office_end_time"
                        )
                    }

                    OutlinedTextField(
                        value = agendaNotes,
                        onValueChange = { agendaNotes = it },
                        label = { Text("Questions / Consultation Agenda (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_office_agenda"),
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "15m Consultation Alarm",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalAuraColors.current.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = LocalAuraColors.current.primaryAccent)
                        )
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Office Hours",
                    isPrimary = true,
                    onClick = {
                        if (instructorName.isNotBlank()) {
                            viewModel.saveOfficeHour(
                                OfficeHourEntity(
                                    id = editingOfficeHour?.id ?: 0L,
                                    courseCode = courseCode.trim().uppercase().ifBlank { "GENERAL" },
                                    instructorName = instructorName.trim(),
                                    role = role,
                                    officeLocation = officeLocation.trim().ifBlank { "Department Office" },
                                    email = email.trim(),
                                    dayOfWeek = dayOfWeek,
                                    startTime = startTime,
                                    endTime = endTime,
                                    agendaNotes = agendaNotes.trim(),
                                    reminderEnabled = reminderEnabled
                                )
                            )
                            showDialog = false
                        }
                    },
                    testTag = "save_office_hour_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showDialog = false },
                    testTag = "cancel_office_hour_button"
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
}

@Composable
private fun OfficeHourGlassCard(
    slot: OfficeHourEntity,
    isToday: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEmail: () -> Unit
) {
    val dayName = when (slot.dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Sunday"
    }

    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isToday) LocalAuraColors.current.success.copy(alpha = 0.65f) else null,
        testTag = "office_hour_card_${slot.id}"
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraBadge(text = slot.courseCode)
                Spacer(modifier = Modifier.width(6.dp))
                AuraBadge(
                    text = slot.role,
                    backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                    textColor = LocalAuraColors.current.secondaryAccent
                )
                if (isToday) {
                    Spacer(modifier = Modifier.width(6.dp))
                    AuraBadge(
                        text = "Available Today",
                        backgroundColor = LocalAuraColors.current.success.copy(alpha = 0.22f),
                        textColor = LocalAuraColors.current.success
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AuraGlassIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit Office Hours",
                    onClick = onEdit,
                    size = 36.dp,
                    iconSize = 16.dp,
                    tint = LocalAuraColors.current.textPrimary,
                    testTag = "edit_office_hour_${slot.id}"
                )
                Spacer(modifier = Modifier.width(6.dp))
                AuraGlassIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete Office Hours",
                    onClick = onDelete,
                    size = 36.dp,
                    iconSize = 16.dp,
                    tint = LocalAuraColors.current.danger,
                    testTag = "delete_office_hour_${slot.id}"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = slot.instructorName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = LocalAuraColors.current.secondaryAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$dayName • ${formatTime12Hour(slot.startTime)} – ${formatTime12Hour(slot.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalAuraColors.current.secondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = LocalAuraColors.current.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = slot.officeLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary
                )
            }

            if (slot.agendaNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x16FFFFFF))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Agenda: ${slot.agendaNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textPrimary
                    )
                }
            }

            if (slot.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AuraGlassButton(
                    text = "Email ${slot.instructorName}",
                    icon = Icons.Default.Email,
                    isPrimary = true,
                    onClick = onEmail,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    testTag = "email_instructor_${slot.id}"
                )
            }
        }
    }
}
