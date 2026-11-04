package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val courseCode: String = "General",
    val deckTitle: String = "Core Concepts",
    val question: String,
    val answer: String,
    val boxLevel: Int = 1, // Leitner / SM-2 mastery tier 1 to 5
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 1,
    val repetitions: Int = 0,
    val lastReviewedEpochMillis: Long = 0L,
    val nextReviewEpochMillis: Long = 0L
)

@Entity(tableName = "audio_notes")
data class AudioNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val courseCode: String = "General",
    val title: String,
    val filePath: String,
    val durationSeconds: Int = 0,
    val recordedEpochMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "syllabus_topics")
data class SyllabusTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val topicName: String,
    val isCompleted: Boolean = false,
    val confidenceLevel: Int = 3, // 1 to 5
    val notes: String = ""
)

@Entity(tableName = "course_grades")
data class CourseGradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val courseCode: String,
    val courseName: String,
    val creditHours: Int = 3,
    val letterGrade: String = "A",
    val gradePoint: Double = 4.0,
    val semester: String = "Semester 1"
)

@Entity(tableName = "course_weight_items")
data class CourseWeightItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val componentName: String, // e.g. "Assignments", "Midterm Exam", "Final Exam"
    val weightPercent: Double = 20.0, // e.g. 20%
    val scoreAchieved: Double = 0.0, // e.g. 85.0
    val maxScore: Double = 100.0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "exam_milestones")
data class ExamMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long? = null,
    val courseCode: String,
    val examTitle: String,
    val phaseNumber: Int, // 1, 2, or 3
    val phaseTitle: String, // e.g. "Phase 1: Conceptual Mastery & Lecture Slides"
    val targetDateEpochMillis: Long,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "academic_habits")
data class AcademicHabitEntity(
    @PrimaryKey val dateString: String, // e.g. "2026-10-31"
    val attendedLectures: Boolean = false,
    val flashcardsReviewed: Boolean = false,
    val deepFocusCompleted: Boolean = false,
    val syllabusProgressMade: Boolean = false,
    val focusMinutesLogged: Int = 0,
    val xpEarned: Int = 0
)

@Entity(tableName = "focus_lockout_records")
data class FocusLockoutRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionTitle: String = "Deep Work Session",
    val plannedMinutes: Int,
    val elapsedMinutes: Int,
    val completedSuccessfully: Boolean,
    val abortedAtEpochMillis: Long? = null,
    val timestampEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String = "General",
    val title: String,
    val content: String,
    val category: String = "CONCEPT", // "FORMULA", "CONCEPT", "SUMMARY", "CHEAT_SHEET"
    val isPinned: Boolean = false,
    val updatedEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "office_hours")
data class OfficeHourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val instructorName: String,
    val role: String = "Lecturer", // "Lecturer", "Professor", "TA", "Advisor"
    val officeLocation: String,
    val email: String = "",
    val dayOfWeek: Int = 1, // 1=Mon .. 7=Sun
    val startTime: String = "14:00", // HH:mm
    val endTime: String = "15:30",   // HH:mm
    val agendaNotes: String = "",
    val reminderEnabled: Boolean = true
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String = "General",
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int = 0, // 0..3
    val explanation: String = "",
    val sourceTag: String = "CUSTOM" // "AI", "NOTES", "CUSTOM"
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String = "General",
    val quizTitle: String = "Practice Quiz",
    val scoreCount: Int,
    val totalQuestions: Int,
    val durationSeconds: Int = 0,
    val timestampEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_projects")
data class GroupProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val projectTitle: String,
    val teamName: String = "Team AURA",
    val objective: String = "",
    val dueEpochMillis: Long,
    val reminderEnabled: Boolean = true
)

@Entity(tableName = "group_tasks")
data class GroupTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val memberName: String,
    val memberContact: String = "", // Email or phone/WhatsApp
    val taskTitle: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "academic_expenses")
data class AcademicExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "PRINTING", // PRINTING, TEXTBOOKS, TRANSPORT, FEEDING, DATA, OTHER
    val amount: Double,
    val courseCode: String = "General",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "course_resources")
data class CourseResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long = 0L,
    val courseCode: String = "General",
    val title: String,
    val moduleFolder: String = "Week 1", // e.g., "Week 1", "Lecture Slides", "Handouts", "Past Papers"
    val fileType: String = "PDF", // "PDF", "PPTX", "DOCX", "PPT", "DOC", "TXT", "IMAGE", "OTHER"
    val mimeType: String = "application/octet-stream",
    val originalFileName: String = "",
    val localFilePath: String = "",
    val fileSizeBytes: Long = 0L,
    val totalPages: Int = 1,
    val lastReadPage: Int = 0,
    val extractedText: String = "",
    val personalNotes: String = "",
    val bookmarkedPagesCsv: String = "",
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val lastOpenedEpochMillis: Long = System.currentTimeMillis(),
    val addedEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_tracks")
data class FocusTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artistOrCategory: String = "Personal Study Track",
    val localFilePath: String,
    val mimeType: String = "audio/mpeg",
    val durationSeconds: Int = 0,
    val isFavorite: Boolean = false,
    val addedEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_distractions")
data class FocusDistractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String = "General Study",
    val thoughtText: String,
    val isResolved: Boolean = false,
    val createdEpochMillis: Long = System.currentTimeMillis()
)
