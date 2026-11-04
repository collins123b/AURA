package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AudioNoteEntity
import com.example.data.local.entity.CourseGradeEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.SyllabusTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    suspend fun getAllFlashcardsList(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE courseCode = :courseCode ORDER BY id DESC")
    fun getFlashcardsForCourse(courseCode: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)
}

@Dao
interface AudioNoteDao {
    @Query("SELECT * FROM audio_notes ORDER BY recordedEpochMillis DESC")
    fun getAllAudioNotes(): Flow<List<AudioNoteEntity>>

    @Query("SELECT * FROM audio_notes WHERE courseCode = :courseCode ORDER BY recordedEpochMillis DESC")
    fun getAudioNotesForCourse(courseCode: String): Flow<List<AudioNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioNote(note: AudioNoteEntity): Long

    @Delete
    suspend fun deleteAudioNote(note: AudioNoteEntity)
}

@Dao
interface SyllabusTopicDao {
    @Query("SELECT * FROM syllabus_topics ORDER BY id ASC")
    fun getAllTopics(): Flow<List<SyllabusTopicEntity>>

    @Query("SELECT * FROM syllabus_topics WHERE courseId = :courseId ORDER BY id ASC")
    fun getTopicsForCourse(courseId: Long): Flow<List<SyllabusTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: SyllabusTopicEntity): Long

    @Update
    suspend fun updateTopic(topic: SyllabusTopicEntity)

    @Delete
    suspend fun deleteTopic(topic: SyllabusTopicEntity)

    @Query("UPDATE syllabus_topics SET courseCode = :courseCode WHERE courseId = :courseId")
    suspend fun updateCourseCodeForTopics(courseId: Long, courseCode: String)

    @Query("DELETE FROM syllabus_topics WHERE courseId = :courseId")
    suspend fun deleteTopicsByCourse(courseId: Long)
}

@Dao
interface CourseGradeDao {
    @Query("SELECT * FROM course_grades ORDER BY id ASC")
    fun getAllGrades(): Flow<List<CourseGradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: CourseGradeEntity): Long

    @Update
    suspend fun updateGrade(grade: CourseGradeEntity)

    @Delete
    suspend fun deleteGrade(grade: CourseGradeEntity)
}

@Dao
interface CourseWeightItemDao {
    @Query("SELECT * FROM course_weight_items ORDER BY id ASC")
    fun getAllWeightItems(): Flow<List<com.example.data.local.entity.CourseWeightItemEntity>>

    @Query("SELECT * FROM course_weight_items WHERE courseCode = :courseCode ORDER BY id ASC")
    fun getWeightItemsForCourse(courseCode: String): Flow<List<com.example.data.local.entity.CourseWeightItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightItem(item: com.example.data.local.entity.CourseWeightItemEntity): Long

    @Update
    suspend fun updateWeightItem(item: com.example.data.local.entity.CourseWeightItemEntity)

    @Delete
    suspend fun deleteWeightItem(item: com.example.data.local.entity.CourseWeightItemEntity)
}

@Dao
interface ExamMilestoneDao {
    @Query("SELECT * FROM exam_milestones ORDER BY targetDateEpochMillis ASC")
    fun getAllMilestones(): Flow<List<com.example.data.local.entity.ExamMilestoneEntity>>

    @Query("SELECT * FROM exam_milestones WHERE courseCode = :courseCode ORDER BY phaseNumber ASC")
    fun getMilestonesForCourse(courseCode: String): Flow<List<com.example.data.local.entity.ExamMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: com.example.data.local.entity.ExamMilestoneEntity): Long

    @Update
    suspend fun updateMilestone(milestone: com.example.data.local.entity.ExamMilestoneEntity)

    @Delete
    suspend fun deleteMilestone(milestone: com.example.data.local.entity.ExamMilestoneEntity)
}

@Dao
interface AcademicHabitDao {
    @Query("SELECT * FROM academic_habits ORDER BY dateString DESC LIMIT 35")
    fun getRecentHabits(): Flow<List<com.example.data.local.entity.AcademicHabitEntity>>

    @Query("SELECT * FROM academic_habits WHERE dateString = :dateString LIMIT 1")
    suspend fun getHabitForDate(dateString: String): com.example.data.local.entity.AcademicHabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHabit(habit: com.example.data.local.entity.AcademicHabitEntity)
}

@Dao
interface FocusLockoutRecordDao {
    @Query("SELECT * FROM focus_lockout_records ORDER BY timestampEpochMillis DESC")
    fun getAllLockoutRecords(): Flow<List<com.example.data.local.entity.FocusLockoutRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockoutRecord(record: com.example.data.local.entity.FocusLockoutRecordEntity): Long
}

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes ORDER BY isPinned DESC, updatedEpochMillis DESC")
    fun getAllStudyNotes(): Flow<List<com.example.data.local.entity.StudyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: com.example.data.local.entity.StudyNoteEntity): Long

    @Update
    suspend fun updateStudyNote(note: com.example.data.local.entity.StudyNoteEntity)

    @Delete
    suspend fun deleteStudyNote(note: com.example.data.local.entity.StudyNoteEntity)
}

@Dao
interface OfficeHourDao {
    @Query("SELECT * FROM office_hours ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllOfficeHours(): Flow<List<com.example.data.local.entity.OfficeHourEntity>>

    @Query("SELECT * FROM office_hours ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllOfficeHoursList(): List<com.example.data.local.entity.OfficeHourEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfficeHour(officeHour: com.example.data.local.entity.OfficeHourEntity): Long

    @Update
    suspend fun updateOfficeHour(officeHour: com.example.data.local.entity.OfficeHourEntity)

    @Delete
    suspend fun deleteOfficeHour(officeHour: com.example.data.local.entity.OfficeHourEntity)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<com.example.data.local.entity.QuizQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: com.example.data.local.entity.QuizQuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<com.example.data.local.entity.QuizQuestionEntity>): List<Long>

    @Delete
    suspend fun deleteQuestion(question: com.example.data.local.entity.QuizQuestionEntity)

    @Query("SELECT * FROM quiz_attempts ORDER BY timestampEpochMillis DESC")
    fun getAllAttempts(): Flow<List<com.example.data.local.entity.QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: com.example.data.local.entity.QuizAttemptEntity): Long
}

@Dao
interface GroupProjectDao {
    @Query("SELECT * FROM group_projects ORDER BY dueEpochMillis ASC")
    fun getAllGroupProjects(): Flow<List<com.example.data.local.entity.GroupProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupProject(project: com.example.data.local.entity.GroupProjectEntity): Long

    @Update
    suspend fun updateGroupProject(project: com.example.data.local.entity.GroupProjectEntity)

    @Delete
    suspend fun deleteGroupProject(project: com.example.data.local.entity.GroupProjectEntity)

    @Query("SELECT * FROM group_tasks ORDER BY id ASC")
    fun getAllGroupTasks(): Flow<List<com.example.data.local.entity.GroupTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupTask(task: com.example.data.local.entity.GroupTaskEntity): Long

    @Update
    suspend fun updateGroupTask(task: com.example.data.local.entity.GroupTaskEntity)

    @Delete
    suspend fun deleteGroupTask(task: com.example.data.local.entity.GroupTaskEntity)

    @Query("DELETE FROM group_tasks WHERE projectId = :projectId")
    suspend fun deleteTasksForProject(projectId: Long)
}

@Dao
interface AcademicExpenseDao {
    @Query("SELECT * FROM academic_expenses ORDER BY dateEpochMillis DESC")
    fun getAllExpenses(): Flow<List<com.example.data.local.entity.AcademicExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: com.example.data.local.entity.AcademicExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: com.example.data.local.entity.AcademicExpenseEntity)
}

@Dao
interface CourseResourceDao {
    @Query("SELECT * FROM course_resources ORDER BY isPinned DESC, lastOpenedEpochMillis DESC")
    fun getAllResources(): Flow<List<com.example.data.local.entity.CourseResourceEntity>>

    @Query("SELECT * FROM course_resources WHERE courseId = :courseId ORDER BY isPinned DESC, addedEpochMillis DESC")
    suspend fun getResourcesByCourseList(courseId: Long): List<com.example.data.local.entity.CourseResourceEntity>

    @Query("SELECT * FROM course_resources WHERE id = :id LIMIT 1")
    suspend fun getResourceById(id: Long): com.example.data.local.entity.CourseResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: com.example.data.local.entity.CourseResourceEntity): Long

    @Update
    suspend fun updateResource(resource: com.example.data.local.entity.CourseResourceEntity)

    @Delete
    suspend fun deleteResource(resource: com.example.data.local.entity.CourseResourceEntity)

    @Query("DELETE FROM course_resources WHERE id = :id")
    suspend fun deleteResourceById(id: Long)

    @Query("UPDATE course_resources SET courseCode = :courseCode WHERE courseId = :courseId")
    suspend fun updateCourseCodeForResources(courseId: Long, courseCode: String)

    @Query("DELETE FROM course_resources WHERE courseId = :courseId")
    suspend fun deleteResourcesByCourse(courseId: Long)
}

@Dao
interface FocusTrackDao {
    @Query("SELECT * FROM focus_tracks ORDER BY isFavorite DESC, addedEpochMillis DESC")
    fun getAllTracks(): Flow<List<com.example.data.local.entity.FocusTrackEntity>>

    @Query("SELECT * FROM focus_tracks ORDER BY isFavorite DESC, addedEpochMillis DESC")
    suspend fun getAllTracksList(): List<com.example.data.local.entity.FocusTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: com.example.data.local.entity.FocusTrackEntity): Long

    @Update
    suspend fun updateTrack(track: com.example.data.local.entity.FocusTrackEntity)

    @Delete
    suspend fun deleteTrack(track: com.example.data.local.entity.FocusTrackEntity)

    @Query("DELETE FROM focus_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Long)
}

@Dao
interface FocusDistractionDao {
    @Query("SELECT * FROM focus_distractions ORDER BY isResolved ASC, createdEpochMillis DESC")
    fun getAllDistractions(): Flow<List<com.example.data.local.entity.FocusDistractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistraction(distraction: com.example.data.local.entity.FocusDistractionEntity): Long

    @Update
    suspend fun updateDistraction(distraction: com.example.data.local.entity.FocusDistractionEntity)

    @Delete
    suspend fun deleteDistraction(distraction: com.example.data.local.entity.FocusDistractionEntity)

    @Query("DELETE FROM focus_distractions WHERE isResolved = 1")
    suspend fun clearResolvedDistractions()
}
