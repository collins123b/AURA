package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.data.local.entity.QuizQuestionEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.dialogs.OralVivaDialog
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.LocalAuraColors
import kotlinx.coroutines.delay

@Composable
fun PracticeQuizView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val allQuestions by viewModel.quizQuestions.collectAsState()
    val attempts by viewModel.quizAttempts.collectAsState()
    val isGenerating by viewModel.isGeneratingQuiz.collectAsState()

    val courseCodes = remember(courses) {
        listOf("All Courses") + courses.map { it.code }.distinct()
    }
    var selectedCourse by remember { mutableStateOf("All Courses") }
    var showAddQuestionDialog by remember { mutableStateOf(false) }

    // Live Timed Quiz & Strict Exam Hall Simulator State
    var isQuizActive by remember { mutableStateOf(false) }
    var examSimulatorMode by remember { mutableStateOf(false) }
    var activeDeck by remember { mutableStateOf<List<QuizQuestionEntity>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var scoreCount by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(45) }
    var totalElapsedSecs by remember { mutableIntStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }
    var flaggedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var perCourseCorrect by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var vivaQuestion by remember { mutableStateOf<QuizQuestionEntity?>(null) }

    val filteredQuestions = remember(allQuestions, selectedCourse) {
        if (selectedCourse == "All Courses") allQuestions
        else allQuestions.filter { it.courseCode.equals(selectedCourse, ignoreCase = true) }
    }

    LaunchedEffect(isQuizActive, currentIndex, selectedOption, isQuizFinished, examSimulatorMode) {
        if (isQuizActive && !isQuizFinished) {
            if (examSimulatorMode) {
                while (secondsLeft > 0 && isQuizActive && !isQuizFinished) {
                    delay(1000L)
                    secondsLeft -= 1
                    totalElapsedSecs += 1
                }
                if (secondsLeft == 0 && !isQuizFinished) {
                    isQuizFinished = true
                }
            } else if (selectedOption == -1) {
                secondsLeft = 45
                while (secondsLeft > 0 && selectedOption == -1 && isQuizActive) {
                    delay(1000L)
                    secondsLeft -= 1
                    totalElapsedSecs += 1
                }
                if (secondsLeft == 0 && selectedOption == -1) {
                    selectedOption = 99 // Time expired marker
                }
            }
        }
    }

    if (isQuizActive) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("active_practice_quiz_session"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isQuizFinished) {
                val pct = if (activeDeck.isNotEmpty()) (scoreCount * 100) / activeDeck.size else 0
                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.warning.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = LocalAuraColors.current.warning,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Mock Exam Complete!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$scoreCount / ${activeDeck.size} Correct ($pct%)",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 70) LocalAuraColors.current.success else LocalAuraColors.current.warning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed in ${totalElapsedSecs}s • +${(scoreCount * 15).coerceAtLeast(20)} Academic XP Awarded",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.secondaryAccent,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (perCourseCorrect.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                perCourseCorrect.forEach { (course, pair) ->
                                    Text(
                                        text = "$course: ${pair.first}/${pair.second} correct",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AuraMintAccent
                                    )
                                }
                            }
                        }
                        if (flaggedIndices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Flagged for revision: ${flaggedIndices.size} question(s)",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.warning
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AuraGlassButton(
                                text = "Retry Deck",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    currentIndex = 0
                                    selectedOption = -1
                                    scoreCount = 0
                                    totalElapsedSecs = 0
                                    flaggedIndices = emptySet()
                                    perCourseCorrect = emptyMap()
                                    secondsLeft = if (examSimulatorMode) activeDeck.size * 45 else 45
                                    isQuizFinished = false
                                }
                            )
                            AuraGlassButton(
                                text = "Back to Quiz Hub",
                                isPrimary = true,
                                onClick = { isQuizActive = false }
                            )
                        }
                    }
                }
            } else {
                val q = activeDeck[currentIndex]
                val isFlagged = flaggedIndices.contains(currentIndex)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AuraBadge(
                        text = if (examSimulatorMode) "EXAM HALL • Q${currentIndex + 1}/${activeDeck.size}" else "Q${currentIndex + 1} / ${activeDeck.size} • ${q.courseCode}",
                        textColor = LocalAuraColors.current.secondaryAccent
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AuraGlassButton(
                        text = if (isFlagged) "Flagged" else "Flag",
                        icon = Icons.Default.Flag,
                        isPrimary = isFlagged,
                        cornerRadius = 12.dp,
                        onClick = {
                            flaggedIndices = if (isFlagged) flaggedIndices - currentIndex else flaggedIndices + currentIndex
                        },
                        testTag = "quiz_flag_question_button"
                    )
                    AuraBadge(
                        text = if (secondsLeft >= 60) "${secondsLeft / 60}m ${secondsLeft % 60}s" else "${secondsLeft}s",
                        icon = Icons.Default.Timer,
                        textColor = if (secondsLeft <= 15) LocalAuraColors.current.danger else LocalAuraColors.current.warning
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / activeDeck.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = LocalAuraColors.current.primaryAccent,
                    trackColor = Color(0x22FFFFFF)
                )

                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary,
                            lineHeight = 23.sp
                        )
                    }
                }

                val options = listOf(
                    "A" to q.optionA,
                    "B" to q.optionB,
                    "C" to q.optionC,
                    "D" to q.optionD
                )

                options.forEachIndexed { idx, (letter, optText) ->
                    val isAnswered = selectedOption != -1
                    val isCorrect = idx == q.correctOptionIndex
                    val isChosen = idx == selectedOption

                    val bgTint = when {
                        !isAnswered -> Color(0x24FFFFFF)
                        isCorrect -> LocalAuraColors.current.success.copy(alpha = 0.30f)
                        isChosen && !isCorrect -> LocalAuraColors.current.danger.copy(alpha = 0.28f)
                        else -> Color(0x16FFFFFF)
                    }
                    val borderTint = when {
                        !isAnswered -> Color(0x45FFFFFF)
                        isCorrect -> LocalAuraColors.current.success
                        isChosen && !isCorrect -> LocalAuraColors.current.danger
                        else -> Color(0x24FFFFFF)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgTint)
                            .border(1.2.dp, borderTint, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isAnswered) {
                                selectedOption = idx
                                val hit = idx == q.correctOptionIndex
                                if (hit) {
                                    scoreCount += 1
                                }
                                val prev = perCourseCorrect[q.courseCode] ?: (0 to 0)
                                perCourseCorrect = perCourseCorrect + (q.courseCode to ((prev.first + if (hit) 1 else 0) to (prev.second + 1)))
                            }
                            .padding(14.dp)
                            .testTag("quiz_option_$idx")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAnswered && isCorrect) LocalAuraColors.current.success
                                        else LocalAuraColors.current.primaryAccent.copy(alpha = 0.28f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalAuraColors.current.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (selectedOption != -1) {
                    AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = when {
                                    selectedOption == q.correctOptionIndex -> "Correct Answer!"
                                    selectedOption == 99 -> "Time's Up!"
                                    else -> "Review Explanation"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOption == q.correctOptionIndex) LocalAuraColors.current.success else LocalAuraColors.current.warning,
                                fontSize = 13.sp
                            )
                            if (q.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalAuraColors.current.textSecondary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AuraGlassButton(
                            text = "Exit Quiz",
                            onClick = { isQuizActive = false },
                            modifier = Modifier.weight(1f)
                        )
                        AuraGlassButton(
                            text = if (currentIndex + 1 < activeDeck.size) "Next Question" else "Finish & Grade",
                            isPrimary = true,
                            onClick = {
                                if (currentIndex + 1 < activeDeck.size) {
                                    currentIndex += 1
                                    selectedOption = -1
                                } else {
                                    val cCode = if (selectedCourse == "All Courses") q.courseCode else selectedCourse
                                    viewModel.recordQuizAttempt(
                                        courseCode = cCode,
                                        quizTitle = "$cCode Practice Mock",
                                        scoreCount = scoreCount,
                                        totalQuestions = activeDeck.size,
                                        durationSeconds = totalElapsedSecs
                                    )
                                    isQuizFinished = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "quiz_next_button"
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice_quiz_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Banner & Action Controls
        item {
            AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.24f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Practice Quiz & Mock Exam Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = "${filteredQuestions.size} questions ready • Timed active-recall testing",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.secondaryAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraGlassButton(
                            text = if (isGenerating) "Generating..." else "AI / Vault Quiz",
                            icon = Icons.Default.AutoAwesome,
                            isPrimary = true,
                            enabled = !isGenerating,
                            onClick = {
                                val targetCourse = if (selectedCourse == "All Courses") {
                                    courses.firstOrNull()?.code ?: "CSCD 301"
                                } else selectedCourse
                                viewModel.generateQuizForCourse(targetCourse)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "generate_ai_quiz_button"
                        )
                        AuraGlassButton(
                            text = "Add Question",
                            icon = Icons.Default.Add,
                            onClick = { showAddQuestionDialog = true },
                            modifier = Modifier.weight(1f),
                            testTag = "add_custom_quiz_button"
                        )
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LocalAuraColors.current.secondaryAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Synthesizing exam questions from your Notes Vault, Flashcards & Gemini AI...",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                    }

                    if (filteredQuestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x18FFFFFF))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Strict Exam Hall Simulator",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Text(
                                    text = "Full-paper countdown + question flagging & course breakdown",
                                    fontSize = 10.sp,
                                    color = LocalAuraColors.current.textSecondary
                                )
                            }
                            Switch(
                                checked = examSimulatorMode,
                                onCheckedChange = { examSimulatorMode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = AuraMintAccent),
                                modifier = Modifier.testTag("exam_simulator_mode_toggle")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuraGlassButton(
                                text = if (examSimulatorMode) {
                                    "Enter Exam Hall (${filteredQuestions.size.coerceAtMost(10)} Qs)"
                                } else {
                                    "Start Timed Quiz (${filteredQuestions.size.coerceAtMost(10)} Qs)"
                                },
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                onClick = {
                                    val chosen = filteredQuestions.shuffled().take(10)
                                    activeDeck = chosen
                                    currentIndex = 0
                                    selectedOption = -1
                                    scoreCount = 0
                                    totalElapsedSecs = 0
                                    flaggedIndices = emptySet()
                                    perCourseCorrect = emptyMap()
                                    secondsLeft = if (examSimulatorMode) chosen.size * 45 else 45
                                    isQuizFinished = false
                                    isQuizActive = true
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "start_mock_quiz_button"
                            )
                            AuraGlassButton(
                                text = "Viva",
                                icon = Icons.Default.RecordVoiceOver,
                                onClick = { vivaQuestion = filteredQuestions.firstOrNull() },
                                testTag = "quiz_open_viva_button"
                            )
                        }
                    }
                }
            }
        }

        // Course Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(courseCodes) { code ->
                    AuraGlassButton(
                        text = code,
                        isPrimary = selectedCourse == code,
                        onClick = { selectedCourse = code },
                        cornerRadius = 14.dp
                    )
                }
            }
        }

        // Recent Attempt Analytics
        if (attempts.isNotEmpty()) {
            item {
                val avgPct = attempts.map {
                    if (it.totalQuestions > 0) (it.scoreCount * 100) / it.totalQuestions else 0
                }.average().toInt()
                val weakestCourse = attempts
                    .groupBy { it.courseCode }
                    .minByOrNull { (_, list) ->
                        list.sumOf { it.scoreCount }.toDouble() / list.sumOf { it.totalQuestions }.coerceAtLeast(1)
                    }?.key ?: "None"

                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LocalAuraColors.current.success,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mock Score Average: $avgPct% (${attempts.size} attempts)",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Focus Priority Course: $weakestCourse",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.warning
                            )
                        }
                    }
                }
            }
        }

        // Question Bank Preview
        if (filteredQuestions.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Quiz Questions Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'AI / Vault Quiz' above to generate exam questions from your courses, Notes Vault, and Flashcards—or add your own custom questions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredQuestions, key = { it.id }) { q ->
                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AuraBadge(text = q.courseCode)
                                Spacer(modifier = Modifier.width(6.dp))
                                AuraBadge(
                                    text = q.sourceTag,
                                    textColor = LocalAuraColors.current.warning
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = q.question,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalAuraColors.current.textPrimary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteQuizQuestion(q) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Question",
                                tint = LocalAuraColors.current.danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    vivaQuestion?.let { q ->
        val correctText = listOf(q.optionA, q.optionB, q.optionC, q.optionD)
            .getOrElse(q.correctOptionIndex.coerceIn(0, 3)) { q.optionA }
        OralVivaDialog(
            viewModel = viewModel,
            courseCode = q.courseCode,
            question = q.question,
            expectedAnswer = "$correctText. ${q.explanation}",
            onDismiss = { vivaQuestion = null }
        )
    }

    if (showAddQuestionDialog) {
        var cCode by remember {
            mutableStateOf(if (selectedCourse == "All Courses") (courses.firstOrNull()?.code ?: "CSCD 301") else selectedCourse)
        }
        var question by remember { mutableStateOf("") }
        var optA by remember { mutableStateOf("") }
        var optB by remember { mutableStateOf("") }
        var optC by remember { mutableStateOf("") }
        var optD by remember { mutableStateOf("") }
        var correctIdx by remember { mutableIntStateOf(0) }
        var explanation by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddQuestionDialog = false },
            title = {
                Text(
                    "Add Practice Exam Question",
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cCode,
                        onValueChange = { cCode = it },
                        label = { Text("Course Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Question Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text("Correct Answer:", fontSize = 12.sp, color = LocalAuraColors.current.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C", "D").forEachIndexed { idx, lbl ->
                            AuraGlassButton(
                                text = lbl,
                                isPrimary = correctIdx == idx,
                                onClick = { correctIdx = idx },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Explanation (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Save Question",
                    isPrimary = true,
                    onClick = {
                        if (question.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
                            viewModel.addCustomQuizQuestion(
                                QuizQuestionEntity(
                                    courseCode = cCode.trim().uppercase().ifBlank { "GENERAL" },
                                    question = question.trim(),
                                    optionA = optA.trim(),
                                    optionB = optB.trim(),
                                    optionC = optC.trim().ifBlank { "None of the above" },
                                    optionD = optD.trim().ifBlank { "All of the above" },
                                    correctOptionIndex = correctIdx,
                                    explanation = explanation.trim(),
                                    sourceTag = "CUSTOM"
                                )
                            )
                            showAddQuestionDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                AuraGlassButton(text = "Cancel", onClick = { showAddQuestionDialog = false })
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
