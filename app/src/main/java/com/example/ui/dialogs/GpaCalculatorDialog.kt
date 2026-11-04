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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.data.local.entity.CourseWeightItemEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.LocalAuraColors
import com.example.util.AttendanceCalculator

@Composable
fun GpaCalculatorDialog(
    viewModel: AuraViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val courseGrades by viewModel.courseGrades.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val weightItems by viewModel.courseWeightItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var showAddGradeDialog by remember { mutableStateOf(false) }
    var targetGpa by remember { mutableDoubleStateOf(3.60) }
    var upcomingCredits by remember { mutableIntStateOf(15) }

    // Final Exam Score Predictor state
    var predictorCourseCode by remember(courses) {
        mutableStateOf(courses.firstOrNull()?.code ?: "CSCD 301")
    }
    var caWeightPercent by remember { mutableDoubleStateOf(30.0) }
    var caScoreEarned by remember { mutableDoubleStateOf(24.0) }
    var targetFinalPercent by remember { mutableDoubleStateOf(80.0) } // Grade A

    val totalCredits = remember(courseGrades) { courseGrades.sumOf { it.creditHours } }
    val totalQualityPoints = remember(courseGrades) { courseGrades.sumOf { it.creditHours * it.gradePoint } }
    val currentCgpa = remember(totalCredits, totalQualityPoints) {
        if (totalCredits > 0) totalQualityPoints / totalCredits else 0.0
    }

    val requiredUpcomingGpa = remember(totalCredits, totalQualityPoints, targetGpa, upcomingCredits) {
        if (upcomingCredits <= 0) 0.0
        else {
            val neededTotalQp = targetGpa * (totalCredits + upcomingCredits)
            ((neededTotalQp - totalQualityPoints) / upcomingCredits).coerceAtLeast(0.0)
        }
    }

    val degreeClass = when {
        currentCgpa >= 3.60 -> "First Class Honours"
        currentCgpa >= 3.00 -> "Second Class Upper"
        currentCgpa >= 2.00 -> "Second Class Lower"
        currentCgpa >= 1.50 -> "Third Class / Pass"
        else -> "Enter Grades Below"
    }

    val examPrediction = remember(caWeightPercent, caScoreEarned, targetFinalPercent) {
        AttendanceCalculator.predictFinalExamScore(
            caWeightPercent = caWeightPercent,
            caScoreEarnedOutOfWeight = caScoreEarned.coerceIn(0.0, caWeightPercent),
            targetTotalScore = targetFinalPercent
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.School else Icons.Default.Calculate,
                        contentDescription = null,
                        tint = LocalAuraColors.current.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPA & Final Exam Predictor",
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "What-If CGPA Simulator & CA-to-Exam Calculator",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LocalAuraColors.current.textMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x18FFFFFF),
                    contentColor = LocalAuraColors.current.primaryAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = LocalAuraColors.current.primaryAccent
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("CGPA & What-If", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("gpa_tab_cgpa")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Exam Predictor", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("gpa_tab_exam_predictor")
                    )
                }

                if (selectedTab == 0) {
                    // CGPA Result Box
                    AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Cumulative GPA", fontSize = 12.sp, color = LocalAuraColors.current.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.2f", currentCgpa),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.primaryAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AuraBadge(
                                text = degreeClass,
                                backgroundColor = LocalAuraColors.current.secondaryAccent.copy(alpha = 0.18f),
                                textColor = LocalAuraColors.current.secondaryAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$totalCredits Total Credits • ${String.format("%.1f", totalQualityPoints)} Quality Points",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textMuted
                            )
                        }
                    }

                    // Target What-If Simulator
                    AuraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Target CGPA What-If", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(String.format("%.2f", targetGpa), fontWeight = FontWeight.Bold, color = LocalAuraColors.current.secondaryAccent)
                            }
                            Slider(
                                value = targetGpa.toFloat(),
                                onValueChange = { targetGpa = it.toDouble() },
                                valueRange = 2.0f..4.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = LocalAuraColors.current.secondaryAccent,
                                    activeTrackColor = LocalAuraColors.current.secondaryAccent
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Next Semester Credits:", fontSize = 11.sp, color = LocalAuraColors.current.textSecondary)
                                listOf(12, 15, 18, 21).forEach { cred ->
                                    val active = upcomingCredits == cred
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (active) LocalAuraColors.current.primaryAccent else Color(0x20FFFFFF),
                                        modifier = Modifier.clickable { upcomingCredits = cred }
                                    ) {
                                        Text(
                                            text = "${cred}cr",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else LocalAuraColors.current.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val advice = when {
                                totalCredits == 0 -> "Add your completed courses below, or aim for ${String.format("%.2f", targetGpa)} GPA across $upcomingCredits credits."
                                requiredUpcomingGpa <= currentCgpa && currentCgpa >= targetGpa ->
                                    "On Track! Maintain ≥ ${String.format("%.2f", requiredUpcomingGpa.coerceAtLeast(1.0))} GPA next semester to stay above ${String.format("%.2f", targetGpa)}."
                                requiredUpcomingGpa > 4.0 ->
                                    "Needs ${String.format("%.2f", requiredUpcomingGpa)} GPA in $upcomingCredits credits (>4.00 max). Spread target across 2 semesters."
                                else ->
                                    "Required Semester GPA: ${String.format("%.2f", requiredUpcomingGpa)} across your next $upcomingCredits credit hours."
                            }
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (requiredUpcomingGpa > 4.0) LocalAuraColors.current.warning else LocalAuraColors.current.secondaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Add Course Grade Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Courses & Grades (${courseGrades.size})",
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AuraGlassButton(
                            text = "Add Grade",
                            icon = Icons.Default.Add,
                            isPrimary = true,
                            onClick = { showAddGradeDialog = true },
                            testTag = "gpa_add_grade_button"
                        )
                    }

                    if (courseGrades.isEmpty()) {
                        Text(
                            text = "No course grades entered yet. Tap 'Add Grade' to calculate your CGPA.",
                            fontSize = 12.sp,
                            color = LocalAuraColors.current.textMuted
                        )
                    } else {
                        courseGrades.forEach { grade ->
                            AuraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraBadge(text = grade.courseCode)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(grade.courseName, fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary, fontSize = 13.sp)
                                        Text("${grade.creditHours} credits", fontSize = 11.sp, color = LocalAuraColors.current.textMuted)
                                    }
                                    AuraBadge(text = "${grade.letterGrade} (${grade.gradePoint})", textColor = LocalAuraColors.current.warning)
                                    IconButton(onClick = { viewModel.deleteCourseGrade(grade) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Grade", tint = LocalAuraColors.current.danger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: FINAL EXAM SCORE PREDICTOR
                    if (courses.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(courses) { c ->
                                val selected = predictorCourseCode == c.code
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF),
                                    modifier = Modifier.clickable { predictorCourseCode = c.code }
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
                            value = predictorCourseCode,
                            onValueChange = { predictorCourseCode = it },
                            label = { Text("Course Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // CA Weight Split Selector
                    AuraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "1. Continuous Assessment (CA) vs Final Exam Split",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(30.0 to "30 / 70", 40.0 to "40 / 60", 50.0 to "50 / 50").forEach { (w, label) ->
                                    val selected = caWeightPercent == w
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF))
                                            .clickable {
                                                caWeightPercent = w
                                                if (caScoreEarned > w) caScoreEarned = w
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$label%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else LocalAuraColors.current.textPrimary
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "2. CA Marks Earned:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${String.format("%.1f", caScoreEarned.coerceIn(0.0, caWeightPercent))} / ${caWeightPercent.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.secondaryAccent,
                                    fontSize = 13.sp
                                )
                            }
                            Slider(
                                value = caScoreEarned.coerceIn(0.0, caWeightPercent).toFloat(),
                                onValueChange = { caScoreEarned = it.toDouble() },
                                valueRange = 0f..caWeightPercent.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = LocalAuraColors.current.secondaryAccent,
                                    activeTrackColor = LocalAuraColors.current.secondaryAccent
                                ),
                                modifier = Modifier.testTag("ca_earned_slider")
                            )

                            Text(
                                text = "3. Target Final Course Grade:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = LocalAuraColors.current.textPrimary
                            )
                            val targetGrades = listOf(
                                80.0 to "A (80%)",
                                75.0 to "B+ (75%)",
                                70.0 to "B (70%)",
                                65.0 to "C+ (65%)",
                                60.0 to "C (60%)",
                                50.0 to "Pass (50%)"
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(targetGrades) { (pct, label) ->
                                    val selected = targetFinalPercent == pct
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) LocalAuraColors.current.warning.copy(alpha = 0.25f) else Color(0x1EFFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) LocalAuraColors.current.warning else Color(0x30FFFFFF)
                                        ),
                                        modifier = Modifier.clickable { targetFinalPercent = pct }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) LocalAuraColors.current.warning else LocalAuraColors.current.textSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Final Exam Prediction Result Card
                    val statusTint = when {
                        examPrediction.alreadyGuaranteed -> LocalAuraColors.current.success
                        !examPrediction.isAchievable -> LocalAuraColors.current.danger
                        examPrediction.requiredExamPercentage >= 85.0 -> LocalAuraColors.current.warning
                        else -> LocalAuraColors.current.secondaryAccent
                    }

                    AuraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exam_prediction_result_card"),
                        cornerRadius = 18.dp,
                        borderColor = statusTint.copy(alpha = 0.65f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "REQUIRED ON FINAL EXAM ($predictorCourseCode)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (examPrediction.isAchievable) {
                                    "${String.format("%.1f", examPrediction.requiredExamMarks)} / ${examPrediction.examWeightPercent.toInt()}"
                                } else {
                                    "Max ${String.format("%.1f", examPrediction.maxPossibleTotal)}%"
                                },
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusTint
                            )
                            Text(
                                text = if (examPrediction.isAchievable) {
                                    "(${String.format("%.1f", examPrediction.requiredExamPercentage)}% needed on the Final Exam paper)"
                                } else {
                                    "Lower target grade or check CA bonus options"
                                },
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (examPrediction.requiredExamPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = statusTint,
                                trackColor = Color(0x26FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = examPrediction.statusHeadline,
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AuraGlassButton(
                                text = "Save CA Record for $predictorCourseCode",
                                icon = Icons.Default.Add,
                                isPrimary = true,
                                onClick = {
                                    viewModel.addCourseWeightItem(
                                        CourseWeightItemEntity(
                                            courseCode = predictorCourseCode.trim().ifBlank { "COURSE" },
                                            componentName = "Continuous Assessment (${caWeightPercent.toInt()}%)",
                                            weightPercent = caWeightPercent,
                                            scoreAchieved = caScoreEarned.coerceIn(0.0, caWeightPercent),
                                            maxScore = caWeightPercent,
                                            isCompleted = true
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "save_ca_breakdown_button"
                            )
                        }
                    }

                    // Multi-Grade Target Matrix ("What Do I Need on the Final Exam?")
                    val matrixTargets = remember(caWeightPercent, caScoreEarned) {
                        listOf(
                            Triple("A", 80.0, "First Class"),
                            Triple("B+", 75.0, "Very Good"),
                            Triple("B", 70.0, "Second Upper"),
                            Triple("C+", 65.0, "Good Credit"),
                            Triple("C", 60.0, "Credit"),
                            Triple("Pass", 50.0, "Minimum Pass")
                        ).map { (letter, cutoff, desc) ->
                            val pred = AttendanceCalculator.predictFinalExamScore(
                                caWeightPercent = caWeightPercent,
                                caScoreEarnedOutOfWeight = caScoreEarned.coerceIn(0.0, caWeightPercent),
                                targetTotalScore = cutoff
                            )
                            Triple(letter, "$cutoff% ($desc)", pred)
                        }
                    }

                    AuraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exam_grade_matrix_card"),
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Full Grade Target Matrix (Out of ${(100.0 - caWeightPercent).toInt()} Exam Marks)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = LocalAuraColors.current.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            matrixTargets.forEach { (letter, subtitle, pred) ->
                                val rowTint = when {
                                    pred.alreadyGuaranteed -> LocalAuraColors.current.success
                                    !pred.isAchievable -> LocalAuraColors.current.danger
                                    pred.requiredExamPercentage >= 85.0 -> LocalAuraColors.current.warning
                                    else -> LocalAuraColors.current.secondaryAccent
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x16FFFFFF))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraBadge(
                                        text = letter,
                                        backgroundColor = rowTint.copy(alpha = 0.22f),
                                        textColor = rowTint
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 11.sp,
                                        color = LocalAuraColors.current.textSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = when {
                                            pred.alreadyGuaranteed -> "Secured ✓"
                                            !pred.isAchievable -> "Unreachable"
                                            else -> "${String.format("%.1f", pred.requiredExamMarks)}/${pred.examWeightPercent.toInt()} (${String.format("%.0f", pred.requiredExamPercentage)}%)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = rowTint
                                    )
                                }
                            }
                        }
                    }

                    if (weightItems.isNotEmpty()) {
                        Text(
                            text = "Saved CA Records (${weightItems.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = LocalAuraColors.current.textPrimary
                        )
                        weightItems.take(6).forEach { item ->
                            AuraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraBadge(text = item.courseCode)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.componentName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary)
                                        Text("Earned: ${String.format("%.1f", item.scoreAchieved)} / ${item.maxScore.toInt()}", fontSize = 11.sp, color = LocalAuraColors.current.secondaryAccent)
                                    }
                                    IconButton(onClick = { viewModel.deleteCourseWeightItem(item) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete CA Record", tint = LocalAuraColors.current.danger, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Close",
                onClick = onDismiss,
                testTag = "gpa_close_button"
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

    if (showAddGradeDialog) {
        var code by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var credits by remember { mutableStateOf("3") }
        var selectedGrade by remember { mutableStateOf("A") }

        val grades = listOf("A", "B+", "B", "C+", "C", "D+", "D", "F")

        AlertDialog(
            onDismissRequest = { showAddGradeDialog = false },
            title = { Text("Add Completed Course Grade", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. CSCD 301)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = credits, onValueChange = { credits = it }, label = { Text("Credit Hours") }, modifier = Modifier.fillMaxWidth())
                    Text("Select Letter Grade:", style = MaterialTheme.typography.labelSmall, color = LocalAuraColors.current.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        grades.take(4).forEach { g ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedGrade == g) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF))
                                    .clickable { selectedGrade = g }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(g, fontWeight = FontWeight.Bold, color = if (selectedGrade == g) Color.White else LocalAuraColors.current.textPrimary)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        grades.drop(4).forEach { g ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedGrade == g) LocalAuraColors.current.primaryAccent else Color(0x22FFFFFF))
                                    .clickable { selectedGrade = g }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(g, fontWeight = FontWeight.Bold, color = if (selectedGrade == g) Color.White else LocalAuraColors.current.textPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Grade",
                    isPrimary = true,
                    onClick = {
                        if (code.isNotBlank()) {
                            viewModel.addCourseGrade(
                                courseCode = code.trim().uppercase(),
                                courseName = if (title.isBlank()) code else title.trim(),
                                creditHours = credits.toIntOrNull() ?: 3,
                                letterGrade = selectedGrade,
                                semester = "Current"
                            )
                            showAddGradeDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showAddGradeDialog = false }
                )
            },
            containerColor = Color(0xFF141923),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
        )
    }
}
