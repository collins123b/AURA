package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AcademicHabitEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalAuraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AcademicHabitsView(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val todayHabit by viewModel.todayAcademicHabit.collectAsState()
    val recentHabits by viewModel.recentAcademicHabits.collectAsState()

    val habit = todayHabit ?: AcademicHabitEntity(dateString = LocalDate.now().toString())

    // Calculate completion count today
    val completedCount = listOf(
        habit.attendedLectures,
        habit.flashcardsReviewed,
        habit.deepFocusCompleted,
        habit.syllabusProgressMade
    ).count { it }

    val totalHabits = 4
    val progress = completedCount.toFloat() / totalHabits
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "habit_progress")

    // Calculate streak
    val streakDays = remember(recentHabits) {
        var count = 0
        for (h in recentHabits) {
            val done = listOf(h.attendedLectures, h.flashcardsReviewed, h.deepFocusCompleted, h.syllabusProgressMade).count { it }
            if (done >= 2) count++ else break
        }
        count.coerceAtLeast(if (completedCount >= 2) 1 else 0)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("academic_habits_view"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak & XP Hero Banner
        item {
            AuraCard(
                modifier = Modifier.fillMaxWidth().testTag("habit_streak_hero_card"),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2564D2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFF64D2FF),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$streakDays Day Streak",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AuraBadge(
                                    text = "${habit.xpEarned} XP",
                                    backgroundColor = Color(0x200A84FF),
                                    textColor = LocalAuraColors.current.primaryAccent
                                )
                            }
                            Text(
                                text = if (completedCount == totalHabits) "Perfect Academic Day! Keep the rhythm going." else "$completedCount of $totalHabits core academic habits completed today",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = LocalAuraColors.current.primaryAccent,
                        trackColor = Color(0x18FFFFFF)
                    )
                }
            }
        }

        // 7-Day Consistency Row
        item {
            AuraCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Habit Consistency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val today = LocalDate.now()
                        for (i in 6 downTo 0) {
                            val targetDate = today.minusDays(i.toLong())
                            val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val dayHabit = recentHabits.find { it.dateString == dateStr } ?: if (i == 0) habit else null
                            val isDone = dayHabit != null && listOf(
                                dayHabit.attendedLectures,
                                dayHabit.flashcardsReviewed,
                                dayHabit.deepFocusCompleted,
                                dayHabit.syllabusProgressMade
                            ).count { it } >= 2

                            val dayLetter = targetDate.dayOfWeek.name.take(1)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDone) LocalAuraColors.current.primaryAccent else Color(0x14FFFFFF))
                                        .border(
                                            1.dp,
                                            if (i == 0) LocalAuraColors.current.secondaryAccent else Color(0x18FFFFFF),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(text = dayLetter, fontSize = 11.sp, color = LocalAuraColors.current.textMuted)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = targetDate.dayOfMonth.toString(),
                                    fontSize = 10.sp,
                                    color = if (i == 0) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Academic Habit Checklist
        item {
            SectionHeader(
                title = "Today's Core Academic Habits",
                subtitle = "Tap to toggle daily accountability"
            )
        }

        // Habit 1: Lectures Attended
        item {
            HabitCheckItem(
                title = "Attend Lectures & Practical Labs",
                subtitle = "Present in all timetable slots for today",
                isCompleted = habit.attendedLectures,
                onToggle = {
                    viewModel.toggleTodayHabit {
                        val next = !it.attendedLectures
                        it.copy(attendedLectures = next, xpEarned = it.xpEarned + (if (next) 25 else -25))
                    }
                },
                testTag = "habit_attend_lectures"
            )
        }

        // Habit 2: Flashcards Active Recall
        item {
            HabitCheckItem(
                title = "Active Recall & Flashcard Review",
                subtitle = "Spaced repetition practice on key lecture concepts",
                isCompleted = habit.flashcardsReviewed,
                onToggle = {
                    viewModel.toggleTodayHabit {
                        val next = !it.flashcardsReviewed
                        it.copy(flashcardsReviewed = next, xpEarned = it.xpEarned + (if (next) 25 else -25))
                    }
                },
                testTag = "habit_flashcards_recall"
            )
        }

        // Habit 3: Deep Focus Session
        item {
            HabitCheckItem(
                title = "Deep Focus Study Session (45m+)",
                subtitle = "Distraction-free high-retention study session",
                isCompleted = habit.deepFocusCompleted,
                onToggle = {
                    viewModel.toggleTodayHabit {
                        val next = !it.deepFocusCompleted
                        it.copy(deepFocusCompleted = next, xpEarned = it.xpEarned + (if (next) 25 else -25))
                    }
                },
                testTag = "habit_deep_focus"
            )
        }

        // Habit 4: Syllabus Progress Made
        item {
            HabitCheckItem(
                title = "Course Syllabus & Problem Sets",
                subtitle = "Advance lecture topics or solve tutorial exercises",
                isCompleted = habit.syllabusProgressMade,
                onToggle = {
                    viewModel.toggleTodayHabit {
                        val next = !it.syllabusProgressMade
                        it.copy(syllabusProgressMade = next, xpEarned = it.xpEarned + (if (next) 25 else -25))
                    }
                },
                testTag = "habit_syllabus_progress"
            )
        }
    }
}

@Composable
private fun HabitCheckItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag(testTag),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCompleted) LocalAuraColors.current.primaryAccent else Color(0x14FFFFFF)
                    )
                    .border(
                        1.dp,
                        if (isCompleted) LocalAuraColors.current.primaryAccent else Color(0x25FFFFFF),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) LocalAuraColors.current.textPrimary else LocalAuraColors.current.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary,
                    fontSize = 12.sp
                )
            }

            AuraBadge(
                text = "+25 XP",
                backgroundColor = if (isCompleted) Color(0x2542C99A) else Color(0x14FFFFFF),
                textColor = if (isCompleted) LocalAuraColors.current.success else LocalAuraColors.current.textMuted
            )
        }
    }
}
