package com.example.util

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.LectureEntity
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object AcademicCompletionTools {

    data class SleepCycleOption(
        val cycles: Int,
        val sleepHoursLabel: String,
        val bedtime12Hour: String,
        val qualityBadge: String
    )

    data class WakeUpSleepPlan(
        val targetDayLabel: String,
        val firstLectureTitle: String,
        val firstLectureStart12Hour: String,
        val leaveHostel12Hour: String,
        val recommendedWakeUp12Hour: String,
        val prepMinutes: Int,
        val walkMinutes: Int,
        val bedtimeOptions: List<SleepCycleOption>
    )

    data class SemesterWorkloadReport(
        val totalCreditHours: Int,
        val weeklyLectureHours: Double,
        val recommendedSelfStudyHours: Int,
        val loadStatus: String,
        val marathonDays: List<String>,
        val examClusterAlerts: List<String>,
        val actionAdvice: String
    )

    private val time12Formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    private fun parseLocalTimeSafe(hhMm: String, fallback: LocalTime = LocalTime.of(8, 0)): LocalTime {
        return runCatching {
            val parts = hhMm.trim().split(":")
            LocalTime.of(
                parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: fallback.hour,
                parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: fallback.minute
            )
        }.getOrDefault(fallback)
    }

    /**
     * Completion Feature 1: Smart 90-Minute REM Sleep Cycle & First-Lecture Wake-Up Planner.
     * Calculates exact wake-up time before tomorrow's (or next scheduled day's) earliest lecture
     * factoring campus walk + morning prep buffer, plus 9h (6 cycles), 7.5h (5 cycles), and 6h (4 cycles)
     * REM sleep bedtimes (including 15-minute sleep onset latency).
     */
    fun computeWakeUpAndSleepPlan(
        lectures: List<LectureEntity>,
        morningPrepMinutes: Int = 35,
        referenceDate: LocalDate = LocalDate.now()
    ): WakeUpSleepPlan {
        val tomorrowDow = referenceDate.plusDays(1).dayOfWeek.value
        val tomorrowLectures = lectures.filter { it.dayOfWeek == tomorrowDow }.sortedBy { it.startTime }

        val chosenLecture = tomorrowLectures.firstOrNull()
            ?: (1..7).map { offset ->
                val dow = referenceDate.plusDays(offset.toLong()).dayOfWeek.value
                lectures.filter { it.dayOfWeek == dow }.minByOrNull { it.startTime }
            }.firstOrNull { it != null }

        val startLocal = chosenLecture?.let { parseLocalTimeSafe(it.startTime) } ?: LocalTime.of(8, 30)
        val walkMins = (chosenLecture?.walkingMinutes ?: 12).coerceIn(0, 90)
        val prepMins = morningPrepMinutes.coerceIn(10, 120)
        val leaveTime = startLocal.minusMinutes(walkMins.toLong())
        val wakeUpTime = leaveTime.minusMinutes(prepMins.toLong())

        val dayName = if (chosenLecture != null) {
            val dowName = DayOfWeek.of(chosenLecture.dayOfWeek.coerceIn(1, 7))
                .name.lowercase().replaceFirstChar { it.uppercase() }
            if (chosenLecture.dayOfWeek == tomorrowDow) "Tomorrow ($dowName)" else dowName
        } else {
            "Tomorrow Morning (Default 8:30 AM)"
        }

        // Each REM cycle = 90 mins + 15 mins fall-asleep latency
        val options = listOf(
            Triple(6, "9.0 hrs", "Peak Recovery"),
            Triple(5, "7.5 hrs", "Optimal Scholar"),
            Triple(4, "6.0 hrs", "Minimum Sharp")
        ).map { (cycles, hrsLabel, badge) ->
            val sleepTotalMins = cycles * 90L + 15L
            val bedtime = wakeUpTime.minusMinutes(sleepTotalMins)
            SleepCycleOption(
                cycles = cycles,
                sleepHoursLabel = hrsLabel,
                bedtime12Hour = bedtime.format(time12Formatter),
                qualityBadge = badge
            )
        }

        return WakeUpSleepPlan(
            targetDayLabel = dayName,
            firstLectureTitle = chosenLecture?.let { "${it.courseCode} • ${it.venue.ifBlank { "Main Hall" }}" }
                ?: "Morning Revision Block",
            firstLectureStart12Hour = startLocal.format(time12Formatter),
            leaveHostel12Hour = leaveTime.format(time12Formatter),
            recommendedWakeUp12Hour = wakeUpTime.format(time12Formatter),
            prepMinutes = prepMins,
            walkMinutes = walkMins,
            bedtimeOptions = options
        )
    }

    /**
     * Completion Feature 2: Smart Semester Workload, Marathon Lecture Day & Exam Cluster Advisor.
     */
    fun analyzeSemesterWorkload(
        courses: List<CourseEntity>,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>
    ): SemesterWorkloadReport {
        val totalCredits = courses.sumOf { it.creditHours.coerceAtLeast(1) }
        val lectureMinutes = lectures.sumOf { lec ->
            val s = parseLocalTimeSafe(lec.startTime, LocalTime.of(8, 0))
            val e = parseLocalTimeSafe(lec.endTime, LocalTime.of(10, 0))
            ((e.toSecondOfDay() - s.toSecondOfDay()) / 60).coerceAtLeast(50)
        }
        val weeklyLectureHrs = lectureMinutes / 60.0
        val recommendedSelfStudy = (totalCredits * 2).coerceIn(8, 45)

        val marathonDays = (1..6).mapNotNull { dow ->
            val dayLecs = lectures.filter { it.dayOfWeek == dow }.sortedBy { it.startTime }
            val dayMins = dayLecs.sumOf { lec ->
                val s = parseLocalTimeSafe(lec.startTime)
                val e = parseLocalTimeSafe(lec.endTime)
                ((e.toSecondOfDay() - s.toSecondOfDay()) / 60).coerceAtLeast(50)
            }
            if (dayMins >= 240 || dayLecs.size >= 4) {
                val dayLabel = DayOfWeek.of(dow).name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                "$dayLabel (${String.format(Locale.US, "%.1fh", dayMins / 60.0)} across ${dayLecs.size} classes)"
            } else null
        }

        val pendingExams = assessments.filter {
            it.status != "COMPLETED" && it.status != "SUBMITTED" &&
                (it.type.contains("EXAM", true) || it.type.contains("MID", true) || it.type.contains("TEST", true))
        }.sortedBy { it.dueEpochMillis }

        val examClusters = mutableListOf<String>()
        for (i in 0 until pendingExams.size - 1) {
            val a = pendingExams[i]
            val b = pendingExams[i + 1]
            val diffHours = abs(b.dueEpochMillis - a.dueEpochMillis) / (1000L * 3600L)
            if (diffHours <= 28L) {
                examClusters.add("${a.courseCode} & ${b.courseCode} within ${diffHours}h")
            }
        }

        val status = when {
            totalCredits >= 21 || examClusters.isNotEmpty() -> "High-Intensity Load"
            totalCredits >= 15 || marathonDays.isNotEmpty() -> "Balanced Honours Load"
            totalCredits > 0 -> "Light Flexible Load"
            else -> "Ready for Course Enrolment"
        }

        val advice = when {
            examClusters.isNotEmpty() ->
                "Back-to-back exam cluster detected (${examClusters.first()}). Start Phase-1 revision 10 days early so you never cram two papers overnight."
            marathonDays.isNotEmpty() ->
                "Heavy lecture day(s) on ${marathonDays.joinToString(", ")}. Pack hydration/snacks and use 25m flashcards instead of 90m deep blocks on those evenings."
            totalCredits > 0 ->
                "Target ~$recommendedSelfStudy hrs/week of active recall across your $totalCredits credit hours (2:1 study-to-credit university rule)."
            else ->
                "Add your semester courses in Timetable to unlock credit-hour load balancing and exam cluster alerts."
        }

        return SemesterWorkloadReport(
            totalCreditHours = totalCredits,
            weeklyLectureHours = weeklyLectureHrs,
            recommendedSelfStudyHours = recommendedSelfStudy,
            loadStatus = status,
            marathonDays = marathonDays,
            examClusterAlerts = examClusters.distinct(),
            actionAdvice = advice
        )
    }
}

@Composable
fun SmartSleepAndWakePlannerCard(
    lectures: List<LectureEntity>,
    modifier: Modifier = Modifier
) {
    val auraColors = LocalAuraColors.current
    var prepMinutes by remember { mutableIntStateOf(35) }
    val plan = remember(lectures, prepMinutes) {
        AcademicCompletionTools.computeWakeUpAndSleepPlan(lectures, prepMinutes)
    }

    AuraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_sleep_wake_planner_card"),
        cornerRadius = 20.dp,
        accentGlow = auraColors.secondaryAccent
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "90-Min REM Sleep & Wake-Up Planner",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = "${plan.targetDayLabel} • ${plan.firstLectureTitle} (${plan.firstLectureStart12Hour})",
                            fontSize = 11.sp,
                            color = auraColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1EFFFFFF))
                    .border(1.dp, auraColors.secondaryAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("IDEAL WAKE-UP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = auraColors.textMuted)
                    Text(
                        text = plan.recommendedWakeUp12Hour,
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraMintAccent
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Leave by ${plan.leaveHostel12Hour}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.secondaryAccent
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Prep: ${prepMinutes}m", fontSize = 10.sp, color = auraColors.textSecondary)
                        Surface(
                            shape = CircleShape,
                            color = Color(0x28FFFFFF),
                            modifier = Modifier.clickable { prepMinutes = (prepMinutes - 10).coerceAtLeast(15) }
                        ) {
                            Text("−", modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = auraColors.textPrimary)
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0x28FFFFFF),
                            modifier = Modifier.clickable { prepMinutes = (prepMinutes + 10).coerceAtMost(90) }
                        ) {
                            Text("+", modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = auraColors.textPrimary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plan.bedtimeOptions.forEach { opt ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (opt.cycles == 5) auraColors.primaryAccent.copy(alpha = 0.22f) else Color(0x16FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (opt.cycles == 5) auraColors.secondaryAccent else Color(0x2EFFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = opt.bedtime12Hour,
                                fontFamily = AuraMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = auraColors.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${opt.cycles} REM (${opt.sleepHoursLabel})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = auraColors.secondaryAccent,
                                maxLines = 1
                            )
                            Text(
                                text = opt.qualityBadge,
                                fontSize = 8.5.sp,
                                color = auraColors.textMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SemesterWorkloadAdvisorCard(
    courses: List<CourseEntity>,
    lectures: List<LectureEntity>,
    assessments: List<AssessmentEntity>,
    modifier: Modifier = Modifier
) {
    val auraColors = LocalAuraColors.current
    val report = remember(courses, lectures, assessments) {
        AcademicCompletionTools.analyzeSemesterWorkload(courses, lectures, assessments)
    }

    AuraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("semester_workload_advisor_card"),
        cornerRadius = 20.dp,
        accentGlow = auraColors.aiAccent
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = auraColors.aiAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Workload & Clash Advisor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = auraColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AuraBadge(
                    text = report.loadStatus,
                    backgroundColor = auraColors.aiAccent.copy(alpha = 0.20f),
                    textColor = auraColors.aiAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkloadMetricPill("${report.totalCreditHours}", "Credits", Modifier.weight(1f))
                WorkloadMetricPill(String.format(Locale.US, "%.1fh", report.weeklyLectureHours), "Class/Wk", Modifier.weight(1f))
                WorkloadMetricPill("${report.recommendedSelfStudyHours}h", "Target Study", Modifier.weight(1f))
            }

            if (report.examClusterAlerts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = auraColors.danger.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, auraColors.danger.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Exam Cluster Alert: ${report.examClusterAlerts.joinToString(" • ")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.danger,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = report.actionAdvice,
                style = MaterialTheme.typography.bodySmall,
                color = auraColors.textSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun WorkloadMetricPill(value: String, label: String, modifier: Modifier = Modifier) {
    val auraColors = LocalAuraColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(12.dp))
            .padding(vertical = 7.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = AuraMonoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = auraColors.textPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = auraColors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun AuraSemesterWrappedDialog(
    studentName: String,
    institution: String,
    programme: String,
    semester: String,
    cgpa: Double,
    totalFocusMinutes: Int,
    streakDays: Int,
    attendancePercent: Int,
    masteredCards: Int,
    coursesCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current
    val focusHours = totalFocusMinutes / 60.0

    val scholarArchetype = when {
        focusHours >= 25 && cgpa >= 3.6 -> "Grandmaster Dean's Scholar"
        focusHours >= 10 || masteredCards >= 20 -> "Deep-Work Vanguard"
        attendancePercent >= 85 -> "Consistent Campus Tactician"
        else -> "Rising Aurora Scholar"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF50B1426),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(
                1.4.dp,
                Brush.verticalGradient(listOf(auraColors.secondaryAccent, auraColors.aiAccent, AuraMintAccent))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("aura_wrapped_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraBadge(
                        text = "AURA SEMESTER WRAPPED",
                        backgroundColor = auraColors.aiAccent.copy(alpha = 0.24f),
                        textColor = auraColors.secondaryAccent
                    )
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Semester Wrapped",
                        onClick = onDismiss,
                        size = 34.dp
                    )
                }

                Text(
                    text = studentName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "$programme • $institution ($semester)",
                    style = MaterialTheme.typography.bodySmall,
                    color = auraColors.textSecondary
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = auraColors.primaryAccent.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, auraColors.secondaryAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("SCHOLAR ARCHETYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = auraColors.secondaryAccent)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = scholarArchetype,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraMintAccent
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkloadMetricPill(String.format(Locale.US, "%.1f hrs", focusHours), "Deep Focus", Modifier.weight(1f))
                    WorkloadMetricPill(String.format(Locale.US, "%.2f", cgpa), "Projected GPA", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkloadMetricPill("$attendancePercent%", "Attendance", Modifier.weight(1f))
                    WorkloadMetricPill("$masteredCards", "Mastered Cards", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkloadMetricPill("${streakDays}d", "Study Streak", Modifier.weight(1f))
                    WorkloadMetricPill("$coursesCount", "Active Courses", Modifier.weight(1f))
                }

                AuraGlassButton(
                    text = "Share My AURA Wrapped Card",
                    icon = Icons.Default.Share,
                    isPrimary = true,
                    onClick = {
                        val summary = buildString {
                            appendLine("✨ MY AURA SEMESTER WRAPPED ($semester)")
                            appendLine("Scholar: $studentName ($institution)")
                            appendLine("Archetype: $scholarArchetype")
                            appendLine("• Deep Focus Logged: ${String.format(Locale.US, "%.1f", focusHours)} hrs (${streakDays}-day streak)")
                            appendLine("• Projected CGPA: ${String.format(Locale.US, "%.2f", cgpa)} | Attendance: $attendancePercent%")
                            appendLine("• SM-2 Flashcards Mastered: $masteredCards across $coursesCount courses")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, "Share AURA Wrapped").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "share_aura_wrapped_button"
                )
            }
        }
    }
}
