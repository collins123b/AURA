package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CourseWeightItemEntity
import com.example.data.local.entity.ExamMilestoneEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExamWarRoomView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val assessments by viewModel.assessments.collectAsState()
    val milestones by viewModel.examMilestones.collectAsState()
    val pastPaperItems by viewModel.courseWeightItems.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var showAddPastPaperDialog by remember { mutableStateOf(false) }
    var editingHallPassExam by remember { mutableStateOf<AssessmentEntity?>(null) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live ticker update every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Filter exams
    val upcomingExams = remember(assessments) {
        assessments.filter {
            it.type in listOf("FINAL_EXAM", "MID_SEM", "CLASS_TEST") && it.dueEpochMillis >= System.currentTimeMillis()
        }.sortedBy { it.dueEpochMillis }
    }

    val nearestExam = upcomingExams.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("exam_war_room_view"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Live Countdown Clock
        item {
            if (nearestExam != null) {
                val diffMillis = (nearestExam.dueEpochMillis - currentTimeMillis).coerceAtLeast(0L)
                val days = diffMillis / (24 * 3600 * 1000L)
                val hours = (diffMillis % (24 * 3600 * 1000L)) / (3600 * 1000L)
                val minutes = (diffMillis % (3600 * 1000L)) / (60 * 1000L)
                val seconds = (diffMillis % (60 * 1000L)) / 1000L

                val examDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(nearestExam.dueEpochMillis), ZoneId.systemDefault())
                val reportingTime = examDate.minusMinutes(30).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

                AuraCard(
                    modifier = Modifier.fillMaxWidth().testTag("exam_countdown_hero_card"),
                    cornerRadius = 24.dp,
                    accentGlow = LocalAuraColors.current.secondaryAccent
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.aura_prism_hero_1793569866167),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(if (LocalAuraColors.current.isDark) 0.34f else 0.14f)
                        )
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AuraBadge(
                                    text = nearestExam.courseCode,
                                    backgroundColor = Color(0x280A84FF),
                                    textColor = LocalAuraColors.current.primaryAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AuraBadge(
                                    text = nearestExam.type.replace("_", " "),
                                    backgroundColor = Color(0x2838BDF8),
                                    textColor = LocalAuraColors.current.secondaryAccent
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = examDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LocalAuraColors.current.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = nearestExam.title,
                                fontFamily = OutfitFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = LocalAuraColors.current.textPrimary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Countdown Grid (Days, Hours, Minutes, Seconds)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                CountdownUnitBox(value = "$days", label = "DAYS")
                                CountdownUnitBox(value = String.format(Locale.US, "%02d", hours), label = "HRS")
                                CountdownUnitBox(value = String.format(Locale.US, "%02d", minutes), label = "MINS")
                                CountdownUnitBox(value = String.format(Locale.US, "%02d", seconds), label = "SECS")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AuraGlassButton(
                                text = "Generate 3-Phase Revision Milestones",
                                icon = Icons.Default.AutoAwesome,
                                isPrimary = true,
                                onClick = { viewModel.generateDefaultMilestonesForExam(nearestExam) },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "generate_revision_phases_button"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Official Exam Hall Pass & Seat Card
                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_hall_pass_card"),
                    cornerRadius = 22.dp,
                    accentGlow = AuraMintAccent
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = AuraMintAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OFFICIAL EXAM HALL PASS",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "Report by $reportingTime • Exam starts ${examDate.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))}",
                                    fontSize = 11.sp,
                                    color = AuraMintAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            AuraGlassButton(
                                text = "Edit Pass",
                                icon = Icons.Default.Edit,
                                cornerRadius = 14.dp,
                                onClick = { editingHallPassExam = nearestExam },
                                testTag = "edit_hall_pass_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HallPassInfoTile(
                                label = "EXAM VENUE",
                                value = nearestExam.examVenue.ifBlank { "Set Hall / Room" },
                                modifier = Modifier.weight(1.2f)
                            )
                            HallPassInfoTile(
                                label = "SEAT #",
                                value = nearestExam.seatNumber.ifBlank { "TBA" },
                                modifier = Modifier.weight(0.8f)
                            )
                            HallPassInfoTile(
                                label = "INDEX #",
                                value = nearestExam.indexNumber.ifBlank { "Add ID" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Required Materials: ${nearestExam.materialsChecklist.ifBlank { "Student ID, Pens, Scientific Calculator" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.HourglassBottom, null, tint = LocalAuraColors.current.primaryAccent, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Upcoming Exams Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = "Add Final Exams or Mid-Terms in the Tasks screen to activate live countdowns and official hall passes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Revision Timeline & Milestones Section
        item {
            SectionHeader(
                title = "3-Phase Revision Protocol",
                subtitle = "${milestones.count { it.isCompleted }} of ${milestones.size} revision phases completed"
            )
        }

        if (milestones.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No revision milestones yet.\nGenerate milestones from your upcoming exam above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(milestones, key = { it.id }) { milestone ->
                val targetDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(milestone.targetDateEpochMillis), ZoneId.systemDefault())

                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleMilestone(milestone.copy(isCompleted = !milestone.isCompleted)) }
                        .testTag("milestone_card_${milestone.id}"),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (milestone.isCompleted) LocalAuraColors.current.success else Color(0x18FFFFFF))
                                .border(1.dp, if (milestone.isCompleted) LocalAuraColors.current.success else Color(0x30FFFFFF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (milestone.isCompleted) {
                                Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AuraBadge(text = milestone.courseCode)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Phase ${milestone.phaseNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.secondaryAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = milestone.phaseTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = "Target: ${targetDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textMuted
                            )
                        }

                        IconButton(onClick = { viewModel.deleteExamMilestone(milestone) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Past Paper Mock Exam Score Tracker
        item {
            SectionHeader(
                title = "Past Paper Score Tracker",
                subtitle = "Record mock exams, benchmarks and mistakes log",
                actionLabel = "Add Paper",
                onActionClick = { showAddPastPaperDialog = true },
                testTag = "add_past_paper_button"
            )
        }

        if (pastPaperItems.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No past paper scores logged yet.\nSimulate a timed past exam and log your score here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(pastPaperItems, key = { it.id }) { item ->
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x200A84FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.scoreAchieved.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.primaryAccent,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AuraBadge(text = item.courseCode)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.scoreAchieved >= 70.0) "Pass (First Class)" else "Needs Review",
                                    fontSize = 11.sp,
                                    color = if (item.scoreAchieved >= 70.0) LocalAuraColors.current.success else LocalAuraColors.current.warning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.componentName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = "Score: ${item.scoreAchieved} / ${item.maxScore} (Target: 80%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textMuted
                            )
                        }

                        IconButton(onClick = { viewModel.deleteCourseWeightItem(item) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddPastPaperDialog) {
        var paperName by remember { mutableStateOf("2024 Past Paper Mock") }
        var courseCode by remember { mutableStateOf(courses.firstOrNull()?.code ?: "General") }
        var scoreStr by remember { mutableStateOf("75") }

        AlertDialog(
            onDismissRequest = { showAddPastPaperDialog = false },
            title = { Text("Log Past Paper Score", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = paperName,
                        onValueChange = { paperName = it },
                        label = { Text("Paper Year / Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = { scoreStr = it },
                        label = { Text("Score Achieved (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Score",
                    isPrimary = true,
                    onClick = {
                        val score = scoreStr.toDoubleOrNull() ?: 0.0
                        viewModel.addCourseWeightItem(
                            CourseWeightItemEntity(
                                courseCode = courseCode,
                                componentName = paperName,
                                scoreAchieved = score,
                                maxScore = 100.0,
                                isCompleted = true
                            )
                        )
                        showAddPastPaperDialog = false
                    },
                    testTag = "save_past_paper_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showAddPastPaperDialog = false },
                    testTag = "cancel_past_paper_button"
                )
            },
        containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(24.dp)
        )
    }

    val examToEditPass = editingHallPassExam
    if (examToEditPass != null) {
        var venue by remember(examToEditPass) { mutableStateOf(examToEditPass.examVenue) }
        var seat by remember(examToEditPass) { mutableStateOf(examToEditPass.seatNumber) }
        var indexNum by remember(examToEditPass) { mutableStateOf(examToEditPass.indexNumber) }
        var materials by remember(examToEditPass) {
            mutableStateOf(examToEditPass.materialsChecklist.ifBlank { "Student ID, Pens, Scientific Calculator" })
        }

        AlertDialog(
            onDismissRequest = { editingHallPassExam = null },
            title = {
                Text(
                    text = "Official Exam Hall Pass • ${examToEditPass.courseCode}",
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Exam Hall / Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("war_room_exam_venue_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = seat,
                            onValueChange = { seat = it },
                            label = { Text("Seat #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("war_room_exam_seat_input")
                        )
                        OutlinedTextField(
                            value = indexNum,
                            onValueChange = { indexNum = it },
                            label = { Text("Index #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("war_room_exam_index_input")
                        )
                    }
                    OutlinedTextField(
                        value = materials,
                        onValueChange = { materials = it },
                        label = { Text("Required Materials Checklist") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("war_room_exam_materials_input")
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Hall Pass",
                    isPrimary = true,
                    onClick = {
                        viewModel.saveAssessment(
                            examToEditPass.copy(
                                examVenue = venue.trim(),
                                seatNumber = seat.trim(),
                                indexNumber = indexNum.trim(),
                                materialsChecklist = materials.trim()
                            )
                        )
                        editingHallPassExam = null
                    },
                    testTag = "save_hall_pass_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { editingHallPassExam = null }
                )
            },
            containerColor = Color(0xF0101B30),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun HallPassInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1EFFFFFF))
            .border(1.dp, Color(0x36FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAuraColors.current.secondaryAccent,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = AuraMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAuraColors.current.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun CountdownUnitBox(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            fontFamily = AuraMonoFontFamily,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LocalAuraColors.current.textPrimary
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = LocalAuraColors.current.secondaryAccent,
            letterSpacing = 1.sp
        )
    }
}
