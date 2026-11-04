package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val courseCode: String = "",
    val title: String,
    val scheduledStartEpochMillis: Long,
    val scheduledEndEpochMillis: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val actualDurationMinutes: Int = 0,
    val isAiSuggested: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "focus_records")
data class FocusRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val courseCode: String = "",
    val durationMinutes: Int,
    val timestampEpochMillis: Long,
    val mode: String = "POMODORO_25", // POMODORO_25, FOCUS_50, CUSTOM, SHORT_BREAK, LONG_BREAK
    val completed: Boolean = true
)

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val lectureId: Long? = null,
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val status: String, // PRESENT, ABSENT, LATE, EXCUSED
    val note: String = ""
)

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String, // LECTURE, ASSIGNMENT, EXAMINATION, STUDY, FOCUS
    val triggerEpochMillis: Long,
    val isRead: Boolean = false
)
