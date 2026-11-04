package com.example

import com.example.ai.AuraAcademicAdvisor
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.LectureEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTimetableConflictDetection_overlappingLectures() {
        val existingLectures = listOf(
            LectureEntity(
                id = 1,
                courseId = 1,
                courseCode = "CSCD 301",
                courseName = "Software Engineering",
                dayOfWeek = 1, // Monday
                startTime = "08:30",
                endTime = "10:30",
                venue = "Lab 2"
            )
        )

        // Case 1: Overlaps (09:00 - 11:00 on Monday)
        val conflict1 = AuraAcademicAdvisor.checkTimetableConflict(
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "11:00",
            existingLectures = existingLectures
        )
        assertTrue(conflict1.hasConflict)
        assertTrue(conflict1.conflictingEventTitle.contains("CSCD 301"))

        // Case 2: Adjacent non-overlapping (10:30 - 12:30 on Monday)
        val conflict2 = AuraAcademicAdvisor.checkTimetableConflict(
            dayOfWeek = 1,
            startTime = "10:30",
            endTime = "12:30",
            existingLectures = existingLectures
        )
        assertFalse(conflict2.hasConflict)

        // Case 3: Different day (Tuesday 08:30 - 10:30)
        val conflict3 = AuraAcademicAdvisor.checkTimetableConflict(
            dayOfWeek = 2,
            startTime = "08:30",
            endTime = "10:30",
            existingLectures = existingLectures
        )
        assertFalse(conflict3.hasConflict)
    }

    @Test
    fun testDailyBriefingGeneration() {
        val briefing = AuraAcademicAdvisor.generateDailyBriefing(
            studentName = "Collins",
            todayLectures = listOf(
                LectureEntity(
                    id = 1,
                    courseId = 1,
                    courseCode = "CSCD 301",
                    courseName = "Software Engineering",
                    dayOfWeek = 1,
                    startTime = "08:30",
                    endTime = "10:30"
                )
            ),
            tomorrowLectures = emptyList(),
            pendingAssessments = listOf(
                AssessmentEntity(
                    id = 1,
                    courseId = 4,
                    courseCode = "MATH 355",
                    courseName = "Computational Mathematics",
                    title = "Calculus Problem Set",
                    type = "ASSIGNMENT",
                    dueEpochMillis = System.currentTimeMillis() + (12 * 3600 * 1000L), // 12 hrs
                    priority = "URGENT"
                )
            ),
            studyHoursThisWeek = 4.5f,
            studyGoalHours = 18
        )

        assertTrue(briefing.greeting.contains("Collins"))
        assertTrue(briefing.summary.contains("CSCD 301"))
        assertTrue(briefing.urgentAlerts.isNotEmpty())
    }
}
