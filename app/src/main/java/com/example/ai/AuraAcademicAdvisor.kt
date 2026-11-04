package com.example.ai

import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.StudySessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object AuraAcademicAdvisor {

    data class DailyBriefing(
        val greeting: String,
        val summary: String,
        val recommendations: List<String>,
        val urgentAlerts: List<String>
    )

    data class ConflictResult(
        val hasConflict: Boolean,
        val conflictingEventTitle: String = "",
        val timeDescription: String = ""
    )

    data class StudyPlanRecommendation(
        val courseCode: String,
        val courseName: String,
        val title: String,
        val dayOfWeek: Int,
        val startTime: String,
        val endTime: String,
        val durationMinutes: Int,
        val rationale: String
    )

    fun generateDailyBriefing(
        studentName: String,
        todayLectures: List<LectureEntity>,
        tomorrowLectures: List<LectureEntity>,
        pendingAssessments: List<AssessmentEntity>,
        studyHoursThisWeek: Float,
        studyGoalHours: Int
    ): DailyBriefing {
        val now = LocalTime.now()
        val greetingTime = when (now.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        val greeting = "$greetingTime, $studentName."

        val urgentAssessments = pendingAssessments.filter {
            val diffHours = (it.dueEpochMillis - System.currentTimeMillis()) / (1000 * 60 * 60)
            diffHours in 0..48
        }

        val upcomingExams = pendingAssessments.filter {
            it.type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST") &&
                    it.dueEpochMillis > System.currentTimeMillis()
        }.sortedBy { it.dueEpochMillis }

        val summaryBuilder = StringBuilder()
        if (todayLectures.isEmpty()) {
            summaryBuilder.append("You have no lectures scheduled for today. It's a great opportunity to make progress on upcoming coursework.")
        } else {
            summaryBuilder.append("You have ${todayLectures.size} ${if (todayLectures.size == 1) "lecture" else "lectures"} today")
            val firstLecture = todayLectures.firstOrNull()
            if (firstLecture != null) {
                summaryBuilder.append(", starting with ${firstLecture.courseCode} at ${firstLecture.startTime}")
            }
            summaryBuilder.append(".")
        }

        val recs = mutableListOf<String>()
        val alerts = mutableListOf<String>()

        if (urgentAssessments.isNotEmpty()) {
            val urgent = urgentAssessments.first()
            val hoursLeft = ((urgent.dueEpochMillis - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(1)
            alerts.add("Deadline in $hoursLeft hrs: ${urgent.courseCode} — ${urgent.title}")
            recs.add("Prioritize ${urgent.courseCode} revision. Dedicate a 45–60 min focus block before the deadline.")
        }

        if (upcomingExams.isNotEmpty()) {
            val nextExam = upcomingExams.first()
            val daysLeft = ((nextExam.dueEpochMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            recs.add("${nextExam.courseCode} ${nextExam.type.replace('_', ' ')} is in $daysLeft days. Review core concepts this week.")
        }

        if (studyHoursThisWeek < (studyGoalHours / 2f) && LocalDate.now().dayOfWeek.value >= 4) {
            recs.add("You have logged ${String.format("%.1f", studyHoursThisWeek)}h of your ${studyGoalHours}h weekly study target. Schedule a focused evening session to stay on track.")
        } else {
            recs.add("Maintain consistency. High-priority focus blocks improve recall and lower pre-exam anxiety.")
        }

        return DailyBriefing(
            greeting = greeting,
            summary = summaryBuilder.toString(),
            recommendations = recs,
            urgentAlerts = alerts
        )
    }

    fun checkTimetableConflict(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        existingLectures: List<LectureEntity>,
        ignoreLectureId: Long = 0L
    ): ConflictResult {
        val newStartMinutes = timeToMinutes(startTime)
        val newEndMinutes = timeToMinutes(endTime)

        for (lecture in existingLectures) {
            if (lecture.id == ignoreLectureId) continue
            if (lecture.dayOfWeek != dayOfWeek) continue

            val exStartMinutes = timeToMinutes(lecture.startTime)
            val exEndMinutes = timeToMinutes(lecture.endTime)

            // Check overlap
            if (newStartMinutes < exEndMinutes && newEndMinutes > exStartMinutes) {
                return ConflictResult(
                    hasConflict = true,
                    conflictingEventTitle = "${lecture.courseCode} (${lecture.courseName})",
                    timeDescription = "${lecture.startTime} – ${lecture.endTime} at ${lecture.venue}"
                )
            }
        }
        return ConflictResult(hasConflict = false)
    }

    fun generateRecommendedStudyPlan(
        courses: List<CourseEntity>,
        lectures: List<LectureEntity>,
        pendingAssessments: List<AssessmentEntity>,
        attendanceList: List<AttendanceRecordEntity>
    ): List<StudyPlanRecommendation> {
        val recommendations = mutableListOf<StudyPlanRecommendation>()
        if (courses.isEmpty()) return recommendations

        // Determine course priorities based on upcoming deadlines, credit hours, and attendance
        val priorityMap = courses.associateWith { course ->
            var score = course.creditHours * 2
            val hasNearDeadline = pendingAssessments.any {
                it.courseCode.equals(course.code, ignoreCase = true) &&
                        it.dueEpochMillis - System.currentTimeMillis() < (4 * 24 * 3600 * 1000L)
            }
            if (hasNearDeadline) score += 10
            val hasExam = pendingAssessments.any {
                it.courseCode.equals(course.code, ignoreCase = true) &&
                        it.type in listOf("MID_SEM", "FINAL_EXAM")
            }
            if (hasExam) score += 8
            score
        }

        val sortedCourses = courses.sortedByDescending { priorityMap[it] ?: 0 }

        // Candidate time slots for study outside typical lectures (e.g. 16:30 - 18:00, 19:00 - 20:30)
        val days = listOf(1, 2, 3, 4, 5, 6) // Mon - Sat
        var slotIndex = 0

        for (day in days) {
            val dayLectures = lectures.filter { it.dayOfWeek == day }
            val course = sortedCourses[slotIndex % sortedCourses.size]
            slotIndex++

            val startTime = if (dayLectures.any { timeToMinutes(it.endTime) > 960 }) "19:00" else "16:30"
            val endTime = if (startTime == "19:00") "20:30" else "18:00"

            val conflict = checkTimetableConflict(day, startTime, endTime, dayLectures)
            if (!conflict.hasConflict) {
                recommendations.add(
                    StudyPlanRecommendation(
                        courseCode = course.code,
                        courseName = course.name,
                        title = "${course.code} Intensive Revision",
                        dayOfWeek = day,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = 90,
                        rationale = "High academic priority based on credit weight (${course.creditHours} credits) and upcoming milestones."
                    )
                )
            }
            if (recommendations.size >= 5) break
        }

        return recommendations
    }

    fun answerAcademicQuery(
        query: String,
        studentName: String,
        courses: List<CourseEntity>,
        lectures: List<LectureEntity>,
        assessments: List<AssessmentEntity>,
        studyHoursThisWeek: Float,
        attendanceList: List<AttendanceRecordEntity>
    ): String {
        val q = query.lowercase().trim()

        return when {
            q.contains("lecture") && (q.contains("tomorrow") || q.contains("next day")) -> {
                val tomorrowDay = LocalDate.now().plusDays(1).dayOfWeek.value
                val dayName = LocalDate.now().plusDays(1).dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                val tomLectures = lectures.filter { it.dayOfWeek == tomorrowDay }.sortedBy { it.startTime }
                if (tomLectures.isEmpty()) {
                    "Tomorrow ($dayName), you have no scheduled lectures. Use this open time for assignment preparation and personal study."
                } else {
                    val listStr = tomLectures.joinToString("\n") { "• ${it.startTime} – ${it.endTime}: ${it.courseCode} (${it.courseName}) at ${it.venue} with ${it.lecturer}" }
                    "Here is your lecture schedule for tomorrow ($dayName):\n$listStr"
                }
            }

            q.contains("lecture") && (q.contains("today") || q.contains("now")) -> {
                val todayDay = LocalDate.now().dayOfWeek.value
                val todayName = LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                val todLectures = lectures.filter { it.dayOfWeek == todayDay }.sortedBy { it.startTime }
                if (todLectures.isEmpty()) {
                    "You have no lectures scheduled for today ($todayName)."
                } else {
                    val listStr = todLectures.joinToString("\n") { "• ${it.startTime} – ${it.endTime}: ${it.courseCode} (${it.venue})" }
                    "Here is your lecture schedule for today ($todayName):\n$listStr"
                }
            }

            q.contains("assignment") || (q.contains("due") && q.contains("week")) || q.contains("deadline") -> {
                val pending = assessments.filter { it.status != "COMPLETED" && it.status != "SUBMITTED" }
                    .sortedBy { it.dueEpochMillis }
                if (pending.isEmpty()) {
                    "Great news! You have no pending assignments or deadlines logged."
                } else {
                    val listStr = pending.take(4).joinToString("\n") {
                        val hoursLeft = ((it.dueEpochMillis - System.currentTimeMillis()) / (1000 * 3600)).coerceAtLeast(1)
                        val daysLeft = hoursLeft / 24
                        val timeDesc = if (daysLeft > 0) "in $daysLeft days" else "in $hoursLeft hours"
                        "• [${it.priority}] ${it.courseCode}: ${it.title} (Due $timeDesc)"
                    }
                    "Here are your upcoming deadlines:\n$listStr"
                }
            }

            q.contains("exam") || q.contains("examination") -> {
                val exams = assessments.filter {
                    it.type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST") &&
                            it.dueEpochMillis >= System.currentTimeMillis()
                }.sortedBy { it.dueEpochMillis }
                if (exams.isEmpty()) {
                    "You have no upcoming examinations in your schedule."
                } else {
                    val next = exams.first()
                    val daysLeft = ((next.dueEpochMillis - System.currentTimeMillis()) / (1000 * 3600 * 24)).coerceAtLeast(1)
                    "Your next examination is ${next.courseCode} — ${next.title} (${next.type.replace('_', ' ')}), scheduled in $daysLeft days."
                }
            }

            q.contains("prioritize") || q.contains("priority") || q.contains("focus") -> {
                val pending = assessments.filter { it.status != "COMPLETED" && it.status != "SUBMITTED" }
                    .sortedBy { it.dueEpochMillis }
                if (pending.isNotEmpty()) {
                    val top = pending.first()
                    "You should prioritize **${top.courseCode}** (${top.title}). It has the closest deadline with '${top.priority}' urgency. Scheduling a 45-minute focus session today will give you a significant head start."
                } else {
                    "With no urgent deadlines pressing, prioritize your highest credit-hour courses (such as Software Engineering or Computational Mathematics) for early revision."
                }
            }

            q.contains("hour") || q.contains("study time") || q.contains("studied") -> {
                "You have logged approximately ${String.format("%.1f", studyHoursThisWeek)} hours of focused study this week. Keep up the momentum!"
            }

            q.contains("organize") || q.contains("plan tomorrow") -> {
                val tomorrowDay = LocalDate.now().plusDays(1).dayOfWeek.value
                val tomLectures = lectures.filter { it.dayOfWeek == tomorrowDay }.sortedBy { it.startTime }
                val plan = StringBuilder("Recommended Plan for Tomorrow:\n")
                if (tomLectures.isNotEmpty()) {
                    plan.append("1. Attend lectures: ${tomLectures.joinToString(", ") { "${it.courseCode} (${it.startTime})" }}\n")
                    plan.append("2. 16:30 – 17:30: Post-lecture recap & note consolidation (Pomodoro 25m x 2)\n")
                } else {
                    plan.append("1. 09:30 – 11:30: Deep work block on upcoming assignment deliverables\n")
                }
                plan.append("3. 19:00 – 20:00: Light revision and previewing materials for the next day.")
                plan.toString()
            }

            q.contains("attendance") -> {
                if (attendanceList.isEmpty()) {
                    "No attendance records logged yet. Use the Attendance tab to track your lecture presence."
                } else {
                    val presentCount = attendanceList.count { it.status == "PRESENT" || it.status == "LATE" }
                    val percent = (presentCount * 100) / attendanceList.size
                    "Your overall recorded attendance is $percent% ($presentCount of ${attendanceList.size} lectures). Keep it above 75% to meet exam eligibility."
                }
            }

            else -> {
                "Hello $studentName! I am AURA AI, your intelligent academic companion. You can ask me about your lectures for today or tomorrow, pending assignment deadlines, upcoming examinations, study priority suggestions, or timetable conflict explanations."
            }
        }
    }

    private fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}
