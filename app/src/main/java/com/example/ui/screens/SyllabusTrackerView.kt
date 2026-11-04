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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SyllabusTopicEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalAuraColors

@Composable
fun SyllabusTrackerView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val topics by viewModel.syllabusTopics.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val readinessPercent = remember(topics) {
        if (topics.isEmpty()) 0
        else (topics.count { it.isCompleted } * 100) / topics.size
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("syllabus_tracker_view"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Readiness Banner Card
        item {
            AuraCard(
                modifier = Modifier.fillMaxWidth().testTag("exam_readiness_card"),
                cornerRadius = 22.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(LocalAuraColors.current.secondaryAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$readinessPercent%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exam Syllabus Readiness",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = "${topics.count { it.isCompleted }} of ${topics.size} revision topics mastered",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary
                        )
                    }

                    AuraGlassButton(
                        text = "Topic",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = { showAddDialog = true },
                        testTag = "add_syllabus_topic_button"
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Syllabus Checklist",
                subtitle = "Mark covered topics and rate your mastery"
            )
        }

        if (topics.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No syllabus topics added.\nTap '+ Topic' to outline your course syllabus for structured revision.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(topics) { topic ->
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (topic.isCompleted) LocalAuraColors.current.success else Color(0x22FFFFFF)
                                )
                                .clickable { viewModel.toggleTopicCompleted(topic) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (topic.isCompleted) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            AuraBadge(text = topic.courseCode)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = topic.topicName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (topic.isCompleted) LocalAuraColors.current.textMuted else LocalAuraColors.current.textPrimary,
                                textDecoration = if (topic.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(topic.confidenceLevel.coerceIn(1, 5)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = LocalAuraColors.current.warning,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteSyllabusTopic(topic) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var selectedCourse by remember { mutableStateOf(courses.firstOrNull()) }
        var topicName by remember { mutableStateOf("") }
        var confidence by remember { mutableIntStateOf(3) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Syllabus Topic", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = topicName,
                        onValueChange = { topicName = it },
                        label = { Text("Topic Name (e.g. Asymptotic Complexity)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Confidence Level (1-5 Stars):", style = MaterialTheme.typography.labelSmall, color = LocalAuraColors.current.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..5) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (confidence == i) LocalAuraColors.current.warning else Color(0x22FFFFFF))
                                    .clickable { confidence = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$i", color = if (confidence == i) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Add Topic",
                    isPrimary = true,
                    onClick = {
                        if (topicName.isNotBlank()) {
                            val cId = selectedCourse?.id ?: 1L
                            val cCode = selectedCourse?.code ?: "General"
                            viewModel.addSyllabusTopic(cId, cCode, topicName, confidence)
                            showAddDialog = false
                        }
                    },
                    testTag = "confirm_syllabus_topic_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showAddDialog = false },
                    testTag = "cancel_syllabus_topic_button"
                )
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(24.dp)
        )
    }
}
