package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AudioNoteDao
import com.example.data.local.dao.CourseDao
import com.example.data.local.dao.CourseGradeDao
import com.example.data.local.dao.FlashcardDao
import com.example.data.local.dao.FocusRecordDao
import com.example.data.local.dao.LectureDao
import com.example.data.local.dao.NotificationHistoryDao
import com.example.data.local.dao.StudySessionDao
import com.example.data.local.dao.SyllabusTopicDao
import com.example.data.local.entity.AcademicExpenseEntity
import com.example.data.local.entity.AcademicHabitEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AudioNoteEntity
import com.example.data.local.entity.CitationEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseGradeEntity
import com.example.data.local.entity.CourseResourceEntity
import com.example.data.local.entity.CourseWeightItemEntity
import com.example.data.local.entity.ExamMilestoneEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.FocusDistractionEntity
import com.example.data.local.entity.FocusLockoutRecordEntity
import com.example.data.local.entity.FocusRecordEntity
import com.example.data.local.entity.FocusTrackEntity
import com.example.data.local.entity.GroupProjectEntity
import com.example.data.local.entity.GroupTaskEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.NotificationHistoryEntity
import com.example.data.local.entity.OfficeHourEntity
import com.example.data.local.entity.QuizAttemptEntity
import com.example.data.local.entity.QuizQuestionEntity
import com.example.data.local.entity.StudyNoteEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.SyllabusTopicEntity

@Database(
    entities = [
        CourseEntity::class,
        LectureEntity::class,
        AssessmentEntity::class,
        StudySessionEntity::class,
        FocusRecordEntity::class,
        AttendanceRecordEntity::class,
        NotificationHistoryEntity::class,
        FlashcardEntity::class,
        AudioNoteEntity::class,
        SyllabusTopicEntity::class,
        CourseGradeEntity::class,
        CourseWeightItemEntity::class,
        ExamMilestoneEntity::class,
        AcademicHabitEntity::class,
        FocusLockoutRecordEntity::class,
        StudyNoteEntity::class,
        OfficeHourEntity::class,
        QuizQuestionEntity::class,
        QuizAttemptEntity::class,
        GroupProjectEntity::class,
        GroupTaskEntity::class,
        AcademicExpenseEntity::class,
        CourseResourceEntity::class,
        FocusTrackEntity::class,
        FocusDistractionEntity::class,
        CitationEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun lectureDao(): LectureDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun focusRecordDao(): FocusRecordDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun audioNoteDao(): AudioNoteDao
    abstract fun syllabusTopicDao(): SyllabusTopicDao
    abstract fun courseGradeDao(): CourseGradeDao
    abstract fun courseWeightItemDao(): com.example.data.local.dao.CourseWeightItemDao
    abstract fun examMilestoneDao(): com.example.data.local.dao.ExamMilestoneDao
    abstract fun academicHabitDao(): com.example.data.local.dao.AcademicHabitDao
    abstract fun focusLockoutRecordDao(): com.example.data.local.dao.FocusLockoutRecordDao
    abstract fun studyNoteDao(): com.example.data.local.dao.StudyNoteDao
    abstract fun officeHourDao(): com.example.data.local.dao.OfficeHourDao
    abstract fun quizDao(): com.example.data.local.dao.QuizDao
    abstract fun groupProjectDao(): com.example.data.local.dao.GroupProjectDao
    abstract fun academicExpenseDao(): com.example.data.local.dao.AcademicExpenseDao
    abstract fun courseResourceDao(): com.example.data.local.dao.CourseResourceDao
    abstract fun focusTrackDao(): com.example.data.local.dao.FocusTrackDao
    abstract fun focusDistractionDao(): com.example.data.local.dao.FocusDistractionDao
    abstract fun citationDao(): com.example.data.local.dao.CitationDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura_academic.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
