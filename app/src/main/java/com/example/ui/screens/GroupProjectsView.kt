package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GroupProjectEntity
import com.example.data.local.entity.GroupTaskEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraTimePickerField
import com.example.ui.theme.LocalAuraColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GroupProjectsView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.groupProjects.collectAsState()
    val allTasks by viewModel.groupTasks.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var addMemberTaskProjectId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("group_projects_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Group Project & Team Splitter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = "Assign deliverables, track member progress & contact teammates",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AuraGlassButton(
                        text = "New Team",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = { showCreateProjectDialog = true },
                        testTag = "add_group_project_button"
                    )
                }
            }
        }

        if (projects.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Group Projects Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'New Team' to create a group assignment, divide milestones across coursemates, and ping teammates via Email or WhatsApp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                val projectTasks = allTasks.filter { it.projectId == project.id }
                val doneCount = projectTasks.count { it.isCompleted }
                val progress = if (projectTasks.isEmpty()) 0f else doneCount.toFloat() / projectTasks.size.toFloat()
                val dueDt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(project.dueEpochMillis),
                    ZoneId.systemDefault()
                )
                val dueFmt = dueDt.format(DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.US))

                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AuraBadge(text = project.courseCode)
                            Spacer(modifier = Modifier.width(6.dp))
                            AuraBadge(
                                text = project.teamName,
                                textColor = LocalAuraColors.current.warning
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteGroupProject(project) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Project",
                                    tint = LocalAuraColors.current.danger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = project.projectTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        if (project.objective.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = project.objective,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Submission Due: $dueFmt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalAuraColors.current.warning
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = LocalAuraColors.current.success,
                                trackColor = Color(0x22FFFFFF)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$doneCount/${projectTasks.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.secondaryAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        projectTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x16FFFFFF))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            if (task.isCompleted) LocalAuraColors.current.success
                                            else Color(0x26FFFFFF)
                                        )
                                        .clickable { viewModel.toggleGroupTaskCompleted(task) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (task.isCompleted) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.taskTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LocalAuraColors.current.textPrimary,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = "Assigned: ${task.memberName}${if (task.memberContact.isNotBlank()) " • ${task.memberContact}" else ""}",
                                        fontSize = 11.sp,
                                        color = LocalAuraColors.current.secondaryAccent
                                    )
                                }
                                if (task.memberContact.isNotBlank()) {
                                    IconButton(
                                        onClick = { contactTeammate(context, task.memberContact, project.projectTitle, task.taskTitle) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Contact Teammate",
                                            tint = LocalAuraColors.current.primaryAccent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteGroupTask(task) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Task",
                                        tint = LocalAuraColors.current.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        AuraGlassButton(
                            text = "Assign Member Deliverable",
                            icon = Icons.Default.PersonAdd,
                            onClick = { addMemberTaskProjectId = project.id },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showCreateProjectDialog) {
        var courseCode by remember { mutableStateOf(courses.firstOrNull()?.code ?: "CSCD 301") }
        var title by remember { mutableStateOf("") }
        var teamName by remember { mutableStateOf("Group 1") }
        var objective by remember { mutableStateOf("") }
        var daysUntilDue by remember { mutableStateOf("7") }
        var dueTime24 by remember { mutableStateOf("17:00") }

        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("Create Group Project", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = courseCode, onValueChange = { courseCode = it }, label = { Text("Course Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = teamName, onValueChange = { teamName = it }, label = { Text("Team / Group Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("Deliverable Summary") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = daysUntilDue, onValueChange = { daysUntilDue = it }, label = { Text("Days Until Submission") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    AuraTimePickerField(
                        label = "Submission Time (AM / PM)",
                        time24 = dueTime24,
                        onTimeSelected = { dueTime24 = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Create Project",
                    isPrimary = true,
                    onClick = {
                        if (title.isNotBlank()) {
                            val days = daysUntilDue.toLongOrNull()?.coerceAtLeast(1L) ?: 7L
                            val parts = dueTime24.split(":")
                            val t = LocalTime.of(
                                parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 17,
                                parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            )
                            val dueEpoch = LocalDate.now().plusDays(days).atTime(t)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            viewModel.saveGroupProject(
                                GroupProjectEntity(
                                    courseCode = courseCode.trim().uppercase().ifBlank { "GENERAL" },
                                    projectTitle = title.trim(),
                                    teamName = teamName.trim().ifBlank { "Group 1" },
                                    objective = objective.trim(),
                                    dueEpochMillis = dueEpoch
                                )
                            )
                            showCreateProjectDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                AuraGlassButton(text = "Cancel", onClick = { showCreateProjectDialog = false })
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

    val targetPid = addMemberTaskProjectId
    if (targetPid != null) {
        var memberName by remember { mutableStateOf("") }
        var memberContact by remember { mutableStateOf("") }
        var deliverable by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { addMemberTaskProjectId = null },
            title = { Text("Assign Member Task", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = memberName, onValueChange = { memberName = it }, label = { Text("Teammate Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = memberContact, onValueChange = { memberContact = it }, label = { Text("Email or WhatsApp/Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = deliverable, onValueChange = { deliverable = it }, label = { Text("Assigned Task / Section") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Assign Task",
                    isPrimary = true,
                    onClick = {
                        if (memberName.isNotBlank() && deliverable.isNotBlank()) {
                            viewModel.addGroupTask(
                                GroupTaskEntity(
                                    projectId = targetPid,
                                    memberName = memberName.trim(),
                                    memberContact = memberContact.trim(),
                                    taskTitle = deliverable.trim()
                                )
                            )
                            addMemberTaskProjectId = null
                        }
                    }
                )
            },
            dismissButton = {
                AuraGlassButton(text = "Cancel", onClick = { addMemberTaskProjectId = null })
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

private fun contactTeammate(context: Context, contact: String, projectTitle: String, taskTitle: String) {
    try {
        val intent = if (contact.contains("@")) {
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contact")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "[$projectTitle] Update on: $taskTitle")
            }
        } else {
            val digits = contact.filter { it.isDigit() || it == '+' }
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:$digits")).apply {
                putExtra("sms_body", "Hi! Checking in on '$taskTitle' for our $projectTitle group project.")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Contact: $contact", Toast.LENGTH_SHORT).show()
    }
}
