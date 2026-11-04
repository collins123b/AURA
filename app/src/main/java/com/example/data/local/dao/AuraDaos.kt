package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.FocusRecordEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.NotificationHistoryEntity
import com.example.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY code ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY code ASC")
    suspend fun getAllCoursesList(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>): List<Long>

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Long)
}

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getLecturesForDay(dayOfWeek: Int): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getLecturesForDayDirect(dayOfWeek: Int): List<LectureEntity>

    @Query("SELECT * FROM lectures WHERE courseId = :courseId")
    fun getLecturesForCourse(courseId: Long): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    suspend fun getLectureById(id: Long): LectureEntity?

    @Query("SELECT * FROM lectures")
    suspend fun getAllLecturesList(): List<LectureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<LectureEntity>): List<Long>

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    @Query("DELETE FROM lectures WHERE courseId = :courseId")
    suspend fun deleteLecturesByCourse(courseId: Long)

    @Query("UPDATE lectures SET courseCode = :courseCode, courseName = :courseName, colorHex = :colorHex WHERE courseId = :courseId")
    suspend fun updateCourseInfoForLectures(courseId: Long, courseCode: String, courseName: String, colorHex: String)
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessments ORDER BY dueEpochMillis ASC")
    fun getAllAssessments(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE dueEpochMillis >= :fromEpochMillis AND dueEpochMillis <= :toEpochMillis ORDER BY dueEpochMillis ASC")
    fun getAssessmentsInRange(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE status != 'COMPLETED' AND status != 'SUBMITTED' ORDER BY dueEpochMillis ASC")
    fun getPendingAssessments(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE type IN ('MID_SEM', 'FINAL_EXAM', 'CLASS_TEST') ORDER BY dueEpochMillis ASC")
    fun getExaminations(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments")
    suspend fun getAllAssessmentsList(): List<AssessmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<AssessmentEntity>): List<Long>

    @Update
    suspend fun updateAssessment(assessment: AssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: AssessmentEntity)

    @Query("UPDATE assessments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE assessments SET courseCode = :courseCode, courseName = :courseName WHERE courseId = :courseId")
    suspend fun updateCourseInfoForAssessments(courseId: Long, courseCode: String, courseName: String)

    @Query("DELETE FROM assessments WHERE courseId = :courseId")
    suspend fun deleteAssessmentsByCourse(courseId: Long)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY scheduledStartEpochMillis ASC")
    fun getAllStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE scheduledStartEpochMillis >= :startOfDay AND scheduledEndEpochMillis <= :endOfDay ORDER BY scheduledStartEpochMillis ASC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions")
    suspend fun getAllSessionsList(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<StudySessionEntity>): List<Long>

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Delete
    suspend fun deleteSession(session: StudySessionEntity)
}

@Dao
interface FocusRecordDao {
    @Query("SELECT * FROM focus_records ORDER BY timestampEpochMillis DESC")
    fun getAllFocusRecords(): Flow<List<FocusRecordEntity>>

    @Query("SELECT * FROM focus_records WHERE timestampEpochMillis >= :sinceMillis")
    fun getFocusRecordsSince(sinceMillis: Long): Flow<List<FocusRecordEntity>>

    @Query("SELECT SUM(durationMinutes) FROM focus_records WHERE timestampEpochMillis >= :sinceMillis AND completed = 1")
    fun getTotalFocusMinutesSince(sinceMillis: Long): Flow<Int?>

    @Query("SELECT * FROM focus_records")
    suspend fun getAllFocusRecordsList(): List<FocusRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusRecord(record: FocusRecordEntity): Long
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY dateEpochDay DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE courseId = :courseId ORDER BY dateEpochDay DESC")
    fun getAttendanceForCourse(courseId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateEpochDay = :epochDay")
    fun getAttendanceForDate(epochDay: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceList(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecordEntity): Long

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecordEntity)

    @Query("UPDATE attendance_records SET courseCode = :courseCode WHERE courseId = :courseId")
    suspend fun updateCourseCodeForAttendance(courseId: Long, courseCode: String)

    @Query("DELETE FROM attendance_records WHERE courseId = :courseId")
    suspend fun deleteAttendanceByCourse(courseId: Long)
}

@Dao
interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history ORDER BY triggerEpochMillis DESC")
    fun getAllNotifications(): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT * FROM notification_history WHERE isRead = 0 ORDER BY triggerEpochMillis DESC")
    fun getUnreadNotifications(): Flow<List<NotificationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationHistoryEntity): Long

    @Query("UPDATE notification_history SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notification_history SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notification_history")
    suspend fun clearAll()
}
