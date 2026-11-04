package com.example.util

import com.example.data.local.entity.AttendanceRecordEntity
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class CourseAttendanceSummary(
    val attendedCount: Int,
    val absentCount: Int,
    val excusedCount: Int,
    val totalCountable: Int,
    val percentage: Int,
    val safeMissAllowance: Int,
    val classesNeededToRecover: Int,
    val isBelowTarget: Boolean,
    val guidanceText: String
)

data class FinalExamPrediction(
    val caWeightPercent: Double,
    val examWeightPercent: Double,
    val caScoreEarned: Double,
    val targetTotalScore: Double,
    val requiredExamMarks: Double,
    val requiredExamPercentage: Double,
    val maxPossibleTotal: Double,
    val isAchievable: Boolean,
    val alreadyGuaranteed: Boolean,
    val statusHeadline: String
)

object AttendanceCalculator {

    /**
     * Computes exact attendance %, how many upcoming lectures can be safely missed while
     * remaining >= targetPercent, OR how many consecutive classes must be attended to recover.
     */
    fun computeCourseAttendance(
        records: List<AttendanceRecordEntity>,
        targetPercent: Int
    ): CourseAttendanceSummary {
        val clampedTarget = targetPercent.coerceIn(1, 99)
        val t = clampedTarget / 100.0

        val attended = records.count { it.status == "PRESENT" || it.status == "LATE" }
        val absent = records.count { it.status == "ABSENT" }
        val excused = records.count { it.status == "EXCUSED" }
        val countable = attended + absent

        if (countable == 0) {
            return CourseAttendanceSummary(
                attendedCount = 0,
                absentCount = 0,
                excusedCount = excused,
                totalCountable = 0,
                percentage = 100,
                safeMissAllowance = 0,
                classesNeededToRecover = 0,
                isBelowTarget = false,
                guidanceText = "No countable classes logged yet • Log Present or Absent to activate Skip-Safe."
            )
        }

        val rawPercent = (attended.toDouble() * 100.0) / countable.toDouble()
        val displayPercent = rawPercent.roundToInt()

        return if (rawPercent + 1e-9 >= clampedTarget.toDouble()) {
            // Safe to miss X classes: attended / (countable + X) >= t  =>  X <= (attended - t*countable) / t
            val safeSkips = floor((attended.toDouble() - t * countable.toDouble()) / t)
                .toInt()
                .coerceAtLeast(0)
            val text = if (safeSkips > 0) {
                "Skip-Safe: Can miss up to $safeSkips upcoming ${if (safeSkips == 1) "class" else "classes"} and stay ≥ $clampedTarget%."
            } else {
                "On the edge ($displayPercent%): Attend next class to build a skip buffer."
            }
            CourseAttendanceSummary(
                attendedCount = attended,
                absentCount = absent,
                excusedCount = excused,
                totalCountable = countable,
                percentage = displayPercent,
                safeMissAllowance = safeSkips,
                classesNeededToRecover = 0,
                isBelowTarget = false,
                guidanceText = text
            )
        } else {
            // Recovery: (attended + Y) / (countable + Y) >= t  =>  Y >= (t*countable - attended) / (1 - t)
            val needed = ceil((t * countable.toDouble() - attended.toDouble()) / (1.0 - t))
                .toInt()
                .coerceAtLeast(1)
            CourseAttendanceSummary(
                attendedCount = attended,
                absentCount = absent,
                excusedCount = excused,
                totalCountable = countable,
                percentage = displayPercent,
                safeMissAllowance = 0,
                classesNeededToRecover = needed,
                isBelowTarget = true,
                guidanceText = "Recovery Needed: Attend next $needed consecutive ${if (needed == 1) "class" else "classes"} to reach $clampedTarget%."
            )
        }
    }

    /**
     * Predicts the exact Final Exam score required given Continuous Assessment (CA) marks earned.
     * Example: CA Weight = 30%, CA Earned = 24 / 30, Target Total = 80% (Grade A) ->
     * Exam Weight = 70%, Required Exam Marks = 56 / 70 (80.0% on the exam paper).
     */
    fun predictFinalExamScore(
        caWeightPercent: Double,
        caScoreEarnedOutOfWeight: Double,
        targetTotalScore: Double
    ): FinalExamPrediction {
        val caWeight = caWeightPercent.coerceIn(10.0, 90.0)
        val examWeight = 100.0 - caWeight
        val caEarned = caScoreEarnedOutOfWeight.coerceIn(0.0, caWeight)
        val target = targetTotalScore.coerceIn(40.0, 100.0)

        val rawNeededMarks = target - caEarned
        val maxPossibleTotal = caEarned + examWeight

        val alreadyGuaranteed = rawNeededMarks <= 0.0
        val isAchievable = rawNeededMarks <= examWeight + 1e-6

        val clampedMarks = rawNeededMarks.coerceIn(0.0, examWeight)
        val requiredExamPct = if (examWeight > 0.0) (clampedMarks / examWeight) * 100.0 else 0.0

        val headline = when {
            alreadyGuaranteed -> "Target Secured! Your CA marks (${String.format("%.1f", caEarned)}/${caWeight.toInt()}) already meet ${target.toInt()}%."
            !isAchievable -> "Max Possible Total is ${String.format("%.1f", maxPossibleTotal)}% (even with 100% on the Final Exam)."
            requiredExamPct >= 85.0 -> "High-Intensity Target: Need ${String.format("%.1f", clampedMarks)}/${examWeight.toInt()} (${String.format("%.1f", requiredExamPct)}% on Exam)."
            else -> "Achievable Target: Score ${String.format("%.1f", clampedMarks)}/${examWeight.toInt()} (${String.format("%.1f", requiredExamPct)}% on Exam)."
        }

        return FinalExamPrediction(
            caWeightPercent = caWeight,
            examWeightPercent = examWeight,
            caScoreEarned = caEarned,
            targetTotalScore = target,
            requiredExamMarks = clampedMarks,
            requiredExamPercentage = requiredExamPct,
            maxPossibleTotal = maxPossibleTotal,
            isAchievable = isAchievable,
            alreadyGuaranteed = alreadyGuaranteed,
            statusHeadline = headline
        )
    }
}
