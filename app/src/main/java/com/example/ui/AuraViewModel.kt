package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.ai.AuraAcademicAdvisor
import com.example.ai.AuraAiService
import com.example.ai.SlideExtractionResult
import com.example.ai.TimetableScanResult
import com.example.data.cloud.UserBackup
import com.example.data.local.AuraDatabase
import com.example.data.local.entity.AcademicExpenseEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AudioNoteEntity
import com.example.data.local.entity.CitationEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseGradeEntity
import com.example.data.local.entity.CourseResourceEntity
import com.example.data.local.entity.CourseWeightItemEntity
import com.example.data.local.entity.ExamMilestoneEntity
import com.example.data.local.entity.AcademicHabitEntity
import com.example.data.local.entity.FocusDistractionEntity
import com.example.data.local.entity.FocusLockoutRecordEntity
import com.example.data.local.entity.FocusTrackEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.FocusRecordEntity
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
import com.example.data.preferences.AuraPreferences
import com.example.data.preferences.UserProfile
import com.example.notifications.ActiveAlarmPayload
import com.example.notifications.AlarmScheduler
import com.example.notifications.AlarmToneOption
import com.example.notifications.LectureAlarmRinger
import com.example.notifications.NotificationHelper
import com.example.util.AudioRecorderHelper
import com.example.util.CitationFormatter
import com.example.util.ExportData
import com.example.util.FocusMusicPlayer
import com.example.util.MediaStorageHelper
import com.example.util.ParsedCitationDraft
import com.example.util.SlideDocumentParser
import com.example.util.SoundscapeSynthesizer
import com.example.util.SoundscapeType
import com.example.util.SubtaskChecklistHelper
import com.example.util.TimetableJsonHelper
import com.example.widget.AuraUpNextWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "aura"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TimetableViewMode {
    DAILY,
    WEEKLY,
    MONTHLY,
    AGENDA,
    OFFICE_HOURS
}

enum class FocusMode(val label: String, val defaultMinutes: Int) {
    POMODORO_25("25m Focus", 25),
    FOCUS_50("50m Deep Work", 50),
    FLOW_52("52m Flow", 52),
    ULTRADIAN_90("90m Ultradian", 90),
    SHORT_BREAK("5m Break", 5),
    LONG_BREAK("15m Long Break", 15),
    CUSTOM("Custom", 30)
}

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AuraDatabase.getInstance(application)
    private val prefs = AuraPreferences(application)
    private val aiService = AuraAiService(application)
    val audioRecorder = AudioRecorderHelper(application)
    val musicPlayer = FocusMusicPlayer()

    init {
        loadTodayHabit()
        viewModelScope.launch {
            db.focusTrackDao().getAllTracks().collect { tracks ->
                musicPlayer.updatePlaylist(tracks)
            }
        }
    }

    // User Profile
    val userProfile: StateFlow<UserProfile> = prefs.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    // Database Streams
    val courses: StateFlow<List<CourseEntity>> = db.courseDao().getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lectures: StateFlow<List<LectureEntity>> = db.lectureDao().getAllLectures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assessments: StateFlow<List<AssessmentEntity>> = db.assessmentDao().getAllAssessments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySessionEntity>> = db.studySessionDao().getAllStudySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusRecords: StateFlow<List<FocusRecordEntity>> = db.focusRecordDao().getAllFocusRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = db.attendanceDao().getAllAttendanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationHistoryEntity>> = db.notificationHistoryDao().getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5 Extra Features Streams
    val flashcards: StateFlow<List<FlashcardEntity>> = db.flashcardDao().getAllFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioNotes: StateFlow<List<AudioNoteEntity>> = db.audioNoteDao().getAllAudioNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syllabusTopics: StateFlow<List<SyllabusTopicEntity>> = db.syllabusTopicDao().getAllTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courseGrades: StateFlow<List<CourseGradeEntity>> = db.courseGradeDao().getAllGrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courseWeightItems: StateFlow<List<CourseWeightItemEntity>> = db.courseWeightItemDao().getAllWeightItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examMilestones: StateFlow<List<ExamMilestoneEntity>> = db.examMilestoneDao().getAllMilestones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAcademicHabits: StateFlow<List<AcademicHabitEntity>> = db.academicHabitDao().getRecentHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyNotes: StateFlow<List<StudyNoteEntity>> = db.studyNoteDao().getAllStudyNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val officeHours: StateFlow<List<OfficeHourEntity>> = db.officeHourDao().getAllOfficeHours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizQuestions: StateFlow<List<QuizQuestionEntity>> = db.quizDao().getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizAttempts: StateFlow<List<QuizAttemptEntity>> = db.quizDao().getAllAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupProjects: StateFlow<List<GroupProjectEntity>> = db.groupProjectDao().getAllGroupProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupTasks: StateFlow<List<GroupTaskEntity>> = db.groupProjectDao().getAllGroupTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val academicExpenses: StateFlow<List<AcademicExpenseEntity>> = db.academicExpenseDao().getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courseResources: StateFlow<List<CourseResourceEntity>> = db.courseResourceDao().getAllResources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusTracks: StateFlow<List<FocusTrackEntity>> = db.focusTrackDao().getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusDistractions: StateFlow<List<FocusDistractionEntity>> = db.focusDistractionDao().getAllDistractions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val citations: StateFlow<List<CitationEntity>> = db.citationDao().getAllCitations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isParsingCitation = MutableStateFlow(false)
    val isParsingCitation: StateFlow<Boolean> = _isParsingCitation.asStateFlow()

    private val _selectedResourceCourseFilter = MutableStateFlow("ALL")
    val selectedResourceCourseFilter: StateFlow<String> = _selectedResourceCourseFilter.asStateFlow()

    private val _isSynthesizingResource = MutableStateFlow(false)
    val isSynthesizingResource: StateFlow<Boolean> = _isSynthesizingResource.asStateFlow()

    private val _isImportingResource = MutableStateFlow(false)
    val isImportingResource: StateFlow<Boolean> = _isImportingResource.asStateFlow()

    private val _isTimetableLocked = MutableStateFlow(false)
    val isTimetableLocked: StateFlow<Boolean> = _isTimetableLocked.asStateFlow()

    private val _recentlyDeletedLecture = MutableStateFlow<LectureEntity?>(null)
    val recentlyDeletedLecture: StateFlow<LectureEntity?> = _recentlyDeletedLecture.asStateFlow()

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _isScanningSlide = MutableStateFlow(false)
    val isScanningSlide: StateFlow<Boolean> = _isScanningSlide.asStateFlow()

    private val _isScanningTimetable = MutableStateFlow(false)
    val isScanningTimetable: StateFlow<Boolean> = _isScanningTimetable.asStateFlow()

    val activeRingingAlarm: StateFlow<ActiveAlarmPayload?> = LectureAlarmRinger.activeAlarmFlow
    val previewingAlarmToneId: StateFlow<String?> = LectureAlarmRinger.previewingToneId

    private val _todayAcademicHabit = MutableStateFlow<AcademicHabitEntity?>(null)
    val todayAcademicHabit: StateFlow<AcademicHabitEntity?> = _todayAcademicHabit.asStateFlow()

    // Soundscape, Ambient Volume & Strict Lockout
    private val soundscapeSynthesizer = SoundscapeSynthesizer()
    private val _activeSoundscape = MutableStateFlow(SoundscapeType.OFF)
    val activeSoundscape: StateFlow<SoundscapeType> = _activeSoundscape.asStateFlow()

    private val _ambientVolume = MutableStateFlow(0.60f)
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()

    private val _sessionGoalText = MutableStateFlow("")
    val sessionGoalText: StateFlow<String> = _sessionGoalText.asStateFlow()

    private val _isStrictLockoutActive = MutableStateFlow(false)
    val isStrictLockoutActive: StateFlow<Boolean> = _isStrictLockoutActive.asStateFlow()

    // Timetable View Mode
    private val _timetableViewMode = MutableStateFlow(TimetableViewMode.DAILY)
    val timetableViewMode: StateFlow<TimetableViewMode> = _timetableViewMode.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    // Conflict Dialog State
    private val _conflictWarning = MutableStateFlow<String?>(null)
    val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    // Focus Timer State
    private val _focusMode = MutableStateFlow(FocusMode.POMODORO_25)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    private val _focusTotalSeconds = MutableStateFlow(25 * 60)
    val focusTotalSeconds: StateFlow<Int> = _focusTotalSeconds.asStateFlow()

    private val _focusRemainingSeconds = MutableStateFlow(25 * 60)
    val focusRemainingSeconds: StateFlow<Int> = _focusRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    val isFocusRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    val focusDurationMinutes: StateFlow<Int> = _focusTotalSeconds.map { it / 60 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    private val _selectedFocusCourseCode = MutableStateFlow("General Study")
    val selectedFocusCourseCode: StateFlow<String> = _selectedFocusCourseCode.asStateFlow()
    val focusSelectedCourse: StateFlow<String?> = _selectedFocusCourseCode.asStateFlow()

    private var timerJob: Job? = null

    // AI Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "aura",
                text = "Welcome to AURA. I am your academic companion. Ask me anything about your timetable, upcoming deadlines, study planning, or syllabus topics."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // AI Study Recommendations
    private val _aiStudyPlan = MutableStateFlow<List<AuraAcademicAdvisor.StudyPlanRecommendation>>(emptyList())
    val aiStudyPlan: StateFlow<List<AuraAcademicAdvisor.StudyPlanRecommendation>> = _aiStudyPlan.asStateFlow()

    // Computed: Next Upcoming Lecture
    val nextUpcomingLecture: StateFlow<LectureEntity?> = combine(lectures, _selectedCalendarDate) { lecs, _ ->
        val currentDay = LocalDate.now().dayOfWeek.value
        val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val todayUpcoming = lecs.filter { it.dayOfWeek == currentDay && it.startTime > nowTimeStr }
            .minByOrNull { it.startTime }

        todayUpcoming ?: lecs.filter { it.dayOfWeek > currentDay }
            .minWithOrNull(compareBy({ it.dayOfWeek }, { it.startTime }))
        ?: lecs.minWithOrNull(compareBy({ it.dayOfWeek }, { it.startTime }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTimetableViewMode(mode: TimetableViewMode) {
        _timetableViewMode.value = mode
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedCalendarDate.value = date
    }

    fun clearConflictWarning() {
        _conflictWarning.value = null
    }

    // Course Actions (Add, Edit & Safe Delete)
    fun saveCourse(course: CourseEntity) {
        viewModelScope.launch {
            val normalized = course.copy(
                code = course.code.trim().uppercase(),
                name = course.name.trim(),
                lecturer = course.lecturer.trim(),
                venue = course.venue.trim()
            )
            val isEditingExisting = normalized.id > 0L
            val savedId = db.courseDao().insertCourse(normalized)
            val targetId = if (isEditingExisting) normalized.id else savedId

            if (isEditingExisting) {
                db.lectureDao().updateCourseInfoForLectures(
                    courseId = targetId,
                    courseCode = normalized.code,
                    courseName = normalized.name,
                    colorHex = normalized.colorHex
                )
                db.assessmentDao().updateCourseInfoForAssessments(
                    courseId = targetId,
                    courseCode = normalized.code,
                    courseName = normalized.name
                )
                db.attendanceDao().updateCourseCodeForAttendance(targetId, normalized.code)
                db.syllabusTopicDao().updateCourseCodeForTopics(targetId, normalized.code)
                db.courseResourceDao().updateCourseCodeForResources(targetId, normalized.code)

                // Reschedule updated lecture alarms so notification banners reflect the edited course code/title
                val updatedCourseLectures = db.lectureDao().getAllLecturesList().filter { it.courseId == targetId }
                updatedCourseLectures.forEach { lec ->
                    if (lec.reminderEnabled) {
                        AlarmScheduler.scheduleLectureAlarm(getApplication(), lec)
                    }
                }
            }
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun addCourse(course: CourseEntity) {
        saveCourse(course)
    }

    fun deleteCourse(course: CourseEntity, deleteUploadedSlides: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel scheduled lecture alarms for this course before removing lectures
            val courseLectures = db.lectureDao().getAllLecturesList().filter { it.courseId == course.id }
            courseLectures.forEach { lec ->
                AlarmScheduler.cancelAlarm(getApplication(), 100000 + lec.id.toInt())
            }

            db.lectureDao().deleteLecturesByCourse(course.id)
            db.attendanceDao().deleteAttendanceByCourse(course.id)
            db.syllabusTopicDao().deleteTopicsByCourse(course.id)

            if (deleteUploadedSlides) {
                val resourcesToDelete = db.courseResourceDao().getResourcesByCourseList(course.id)
                resourcesToDelete.forEach { res ->
                    SlideDocumentParser.deleteLocalResourceFile(res.localFilePath)
                }
                db.courseResourceDao().deleteResourcesByCourse(course.id)
            }

            db.courseDao().deleteCourse(course)
            if (_selectedResourceCourseFilter.value.equals(course.code, ignoreCase = true)) {
                _selectedResourceCourseFilter.value = "ALL"
            }
        }
    }

    fun setResourceCourseFilter(courseCode: String) {
        _selectedResourceCourseFilter.value = courseCode.ifBlank { "ALL" }
    }

    // Lecture Actions with Conflict Detection
    fun saveLecture(lecture: LectureEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            val allLectures = db.lectureDao().getAllLecturesList()
            val conflict = AuraAcademicAdvisor.checkTimetableConflict(
                dayOfWeek = lecture.dayOfWeek,
                startTime = lecture.startTime,
                endTime = lecture.endTime,
                existingLectures = allLectures,
                ignoreLectureId = lecture.id
            )

            if (conflict.hasConflict) {
                _conflictWarning.value = "Schedule Note: Overlaps with ${conflict.conflictingEventTitle} (${conflict.timeDescription})."
                return@launch
            }

            _conflictWarning.value = null
            persistAndScheduleLecture(lecture)
            onSaved()
        }
    }

    fun forceSaveLecture(lecture: LectureEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            _conflictWarning.value = null
            persistAndScheduleLecture(lecture)
            onSaved()
        }
    }

    private suspend fun persistAndScheduleLecture(lecture: LectureEntity) {
        val existingCourses = db.courseDao().getAllCoursesList()
        var cId = lecture.courseId
        if (existingCourses.none { it.id == cId } || cId == 0L) {
            val matched = existingCourses.find { it.code.equals(lecture.courseCode, ignoreCase = true) }
            cId = matched?.id ?: db.courseDao().insertCourse(
                CourseEntity(
                    code = lecture.courseCode,
                    name = lecture.courseName,
                    lecturer = lecture.lecturer,
                    venue = lecture.venue,
                    colorHex = lecture.colorHex
                )
            )
        }

        val lectureToSave = lecture.copy(courseId = cId)
        val requestCode = 100000 + lectureToSave.id.toInt()
        if (lectureToSave.id != 0L && !lectureToSave.reminderEnabled) {
            AlarmScheduler.cancelAlarm(getApplication(), requestCode)
        }
        val id = db.lectureDao().insertLecture(lectureToSave)
        val savedLecture = lectureToSave.copy(id = if (lectureToSave.id == 0L) id else lectureToSave.id)
        if (savedLecture.reminderEnabled) {
            AlarmScheduler.scheduleLectureAlarm(getApplication(), savedLecture)
        }
        AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
    }

    fun toggleTimetableLocked() {
        _isTimetableLocked.value = !_isTimetableLocked.value
    }

    fun deleteLecture(lecture: LectureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _recentlyDeletedLecture.value = lecture.copy(id = 0L)
            AlarmScheduler.cancelAlarm(getApplication(), 100000 + lecture.id.toInt())
            db.lectureDao().deleteLecture(lecture)
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun restoreRecentlyDeletedLecture() {
        val backup = _recentlyDeletedLecture.value ?: return
        _recentlyDeletedLecture.value = null
        viewModelScope.launch(Dispatchers.IO) {
            persistAndScheduleLecture(backup)
        }
    }

    fun dismissRecentlyDeletedLecture() {
        _recentlyDeletedLecture.value = null
    }

    fun deleteCourseByCodeFromTimetable(courseCode: String, deleteUploadedSlides: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = courseCode.trim()
            val matchingCourses = db.courseDao().getAllCoursesList().filter {
                it.code.equals(cleanCode, ignoreCase = true)
            }
            val matchingLectures = db.lectureDao().getAllLecturesList().filter { lec ->
                lec.courseCode.equals(cleanCode, ignoreCase = true) ||
                    matchingCourses.any { c -> c.id == lec.courseId }
            }
            matchingLectures.forEach { lec ->
                AlarmScheduler.cancelAlarm(getApplication(), 100000 + lec.id.toInt())
                db.lectureDao().deleteLecture(lec)
            }
            matchingCourses.forEach { course ->
                db.attendanceDao().deleteAttendanceByCourse(course.id)
                db.syllabusTopicDao().deleteTopicsByCourse(course.id)
                if (deleteUploadedSlides) {
                    val resourcesToDelete = db.courseResourceDao().getResourcesByCourseList(course.id)
                    resourcesToDelete.forEach { res ->
                        SlideDocumentParser.deleteLocalResourceFile(res.localFilePath)
                    }
                    db.courseResourceDao().deleteResourcesByCourse(course.id)
                }
                db.courseDao().deleteCourse(course)
            }
            if (_selectedResourceCourseFilter.value.equals(cleanCode, ignoreCase = true)) {
                _selectedResourceCourseFilter.value = "ALL"
            }
            _recentlyDeletedLecture.value = null
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // Per-Lecture Alarm Selection & Configuration
    fun toggleLectureAlarm(lecture: LectureEntity) {
        viewModelScope.launch {
            val updated = lecture.copy(reminderEnabled = !lecture.reminderEnabled)
            db.lectureDao().updateLecture(updated)
            AlarmScheduler.scheduleLectureAlarm(getApplication(), updated)
        }
    }

    fun setAllLectureAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val allLectures = db.lectureDao().getAllLecturesList()
            allLectures.forEach { lec ->
                val updated = lec.copy(reminderEnabled = enabled)
                db.lectureDao().updateLecture(updated)
                AlarmScheduler.scheduleLectureAlarm(getApplication(), updated)
            }
        }
    }

    fun updateLectureAlarmConfig(
        lecture: LectureEntity,
        enabled: Boolean,
        minutesBefore: Int = lecture.reminderMinutesBefore,
        toneId: String = lecture.alarmToneId
    ) {
        viewModelScope.launch {
            val updated = lecture.copy(
                reminderEnabled = enabled,
                reminderMinutesBefore = minutesBefore,
                alarmToneId = toneId
            )
            db.lectureDao().updateLecture(updated)
            AlarmScheduler.scheduleLectureAlarm(getApplication(), updated)
        }
    }

    // Assessment Actions
    fun saveAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            // Ensure course exists in database
            val existingCourses = db.courseDao().getAllCoursesList()
            var cId = assessment.courseId
            if (existingCourses.none { it.id == cId } || cId == 0L) {
                val matched = existingCourses.find { it.code.equals(assessment.courseCode, ignoreCase = true) }
                cId = matched?.id ?: db.courseDao().insertCourse(
                    CourseEntity(
                        code = assessment.courseCode,
                        name = assessment.courseName,
                        colorHex = "#0A84FF"
                    )
                )
            }

            val assessmentToSave = assessment.copy(courseId = cId)
            val id = db.assessmentDao().insertAssessment(assessmentToSave)
            AlarmScheduler.scheduleAssessmentAlarm(getApplication(), assessmentToSave.copy(id = if (assessmentToSave.id == 0L) id else assessmentToSave.id))
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleAssessmentCompleted(assessment: AssessmentEntity) {
        viewModelScope.launch {
            val newStatus = if (assessment.status == "COMPLETED") "PENDING" else "COMPLETED"
            db.assessmentDao().updateStatus(assessment.id, newStatus)
            if (newStatus == "COMPLETED") {
                AlarmScheduler.cancelAlarm(getApplication(), 200000 + assessment.id.toInt())
            }
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), 200000 + assessment.id.toInt())
            db.assessmentDao().deleteAssessment(assessment)
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun initDefaultAssessmentSubtasks(assessment: AssessmentEntity) {
        viewModelScope.launch {
            val defaults = SubtaskChecklistHelper.generateDefaultSubtasksForType(assessment.type)
            db.assessmentDao().updateAssessment(
                assessment.copy(subtasksJson = SubtaskChecklistHelper.serializeSubtasks(defaults))
            )
        }
    }

    fun addAssessmentSubtask(assessment: AssessmentEntity, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val current = SubtaskChecklistHelper.parseSubtasks(assessment.subtasksJson)
            val updated = current + com.example.util.AssessmentSubtask(title = clean, isDone = false)
            db.assessmentDao().updateAssessment(
                assessment.copy(subtasksJson = SubtaskChecklistHelper.serializeSubtasks(updated))
            )
        }
    }

    fun toggleAssessmentSubtask(assessment: AssessmentEntity, subtaskId: String) {
        viewModelScope.launch {
            val current = SubtaskChecklistHelper.parseSubtasks(assessment.subtasksJson)
            val updated = current.map { item ->
                if (item.id == subtaskId) item.copy(isDone = !item.isDone) else item
            }
            db.assessmentDao().updateAssessment(
                assessment.copy(subtasksJson = SubtaskChecklistHelper.serializeSubtasks(updated))
            )
        }
    }

    fun removeAssessmentSubtask(assessment: AssessmentEntity, subtaskId: String) {
        viewModelScope.launch {
            val current = SubtaskChecklistHelper.parseSubtasks(assessment.subtasksJson)
            val updated = current.filterNot { it.id == subtaskId }
            db.assessmentDao().updateAssessment(
                assessment.copy(subtasksJson = SubtaskChecklistHelper.serializeSubtasks(updated))
            )
        }
    }

    // Attendance Actions
    fun recordAttendance(
        courseId: Long,
        courseCode: String,
        status: String,
        note: String = "",
        lectureId: Long? = null
    ) {
        viewModelScope.launch {
            val todayDay = LocalDate.now().toEpochDay()
            db.attendanceDao().insertAttendance(
                AttendanceRecordEntity(
                    courseId = courseId,
                    courseCode = courseCode,
                    lectureId = lectureId,
                    dateEpochDay = todayDay,
                    status = status,
                    note = note
                )
            )
            if (status == "PRESENT" || status == "LATE") {
                toggleTodayHabit { habit ->
                    if (!habit.attendedLectures) {
                        habit.copy(attendedLectures = true, xpEarned = habit.xpEarned + 25)
                    } else habit
                }
            }
        }
    }

    fun deleteLatestCourseAttendance(courseId: Long) {
        viewModelScope.launch {
            val latest = attendanceRecords.value
                .filter { it.courseId == courseId }
                .maxByOrNull { it.id }
            if (latest != null) {
                db.attendanceDao().deleteAttendance(latest)
            }
        }
    }

    // Focus & Pomodoro Timer (supports any custom duration from 10 seconds up to 12 hours)
    fun setFocusMode(mode: FocusMode, customMinutes: Int = 30) {
        pauseTimer()
        _focusMode.value = mode
        val mins = if (mode == FocusMode.CUSTOM) customMinutes.coerceIn(1, 720) else mode.defaultMinutes
        _focusTotalSeconds.value = mins * 60
        _focusRemainingSeconds.value = mins * 60
    }

    fun setCustomFocusDurationSeconds(totalSeconds: Int) {
        pauseTimer()
        val clamped = totalSeconds.coerceIn(10, 12 * 3600)
        _focusMode.value = FocusMode.CUSTOM
        _focusTotalSeconds.value = clamped
        _focusRemainingSeconds.value = clamped
    }

    fun adjustFocusDurationMinutes(deltaMinutes: Int) {
        val deltaSec = deltaMinutes * 60
        val newTotal = (_focusTotalSeconds.value + deltaSec).coerceIn(60, 12 * 3600)
        val newRem = (_focusRemainingSeconds.value + deltaSec).coerceIn(10, newTotal)
        _focusMode.value = FocusMode.CUSTOM
        _focusTotalSeconds.value = newTotal
        _focusRemainingSeconds.value = newRem
    }

    fun setSelectedFocusCourse(courseCode: String) {
        _selectedFocusCourseCode.value = courseCode
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_focusRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _focusRemainingSeconds.value -= 1
            }
            if (_focusRemainingSeconds.value <= 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _focusRemainingSeconds.value = _focusTotalSeconds.value
    }

    fun setSoundscape(type: SoundscapeType) {
        _activeSoundscape.value = type
        soundscapeSynthesizer.play(type)
    }

    fun setAmbientVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _ambientVolume.value = clamped
        soundscapeSynthesizer.volumeFactor = clamped
    }

    fun setSessionGoalText(goal: String) {
        _sessionGoalText.value = goal
    }

    fun importFocusMusicFromUri(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val meta = withContext(Dispatchers.IO) {
                MediaStorageHelper.importAudioFile(
                    context = getApplication(),
                    uri = uri,
                    subfolder = "focus_music",
                    fallbackPrefix = "Study Track"
                )
            }
            if (meta == null) {
                onResult(false, "Could not import audio file. Please pick an MP3, WAV, M4A, or OGG file.")
                return@launch
            }
            val entity = FocusTrackEntity(
                title = meta.title,
                artistOrCategory = meta.artist,
                localFilePath = meta.localFilePath,
                mimeType = meta.mimeType,
                durationSeconds = meta.durationSeconds
            )
            val insertedId = db.focusTrackDao().insertTrack(entity)
            val savedTrack = entity.copy(id = insertedId)
            val updatedList = db.focusTrackDao().getAllTracksList()
            musicPlayer.playTrack(savedTrack, updatedList)
            onResult(true, "Added '${meta.title}' to your Focus Study Music!")
        }
    }

    fun toggleFavoriteFocusTrack(track: FocusTrackEntity) {
        viewModelScope.launch {
            db.focusTrackDao().updateTrack(track.copy(isFavorite = !track.isFavorite))
        }
    }

    fun deleteFocusTrack(track: FocusTrackEntity) {
        viewModelScope.launch {
            if (musicPlayer.currentTrack.value?.id == track.id) {
                musicPlayer.stop()
            }
            withContext(Dispatchers.IO) {
                MediaStorageHelper.deleteLocalMediaFile(getApplication(), track.localFilePath)
            }
            db.focusTrackDao().deleteTrackById(track.id)
        }
    }

    fun addFocusDistraction(thought: String) {
        val trimmed = thought.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            db.focusDistractionDao().insertDistraction(
                FocusDistractionEntity(
                    courseCode = _selectedFocusCourseCode.value,
                    thoughtText = trimmed
                )
            )
        }
    }

    fun toggleFocusDistractionResolved(item: FocusDistractionEntity) {
        viewModelScope.launch {
            db.focusDistractionDao().updateDistraction(item.copy(isResolved = !item.isResolved))
        }
    }

    fun deleteFocusDistraction(item: FocusDistractionEntity) {
        viewModelScope.launch {
            db.focusDistractionDao().deleteDistraction(item)
        }
    }

    fun clearResolvedFocusDistractions() {
        viewModelScope.launch {
            db.focusDistractionDao().clearResolvedDistractions()
        }
    }

    fun convertDistractionToStudyNote(item: FocusDistractionEntity) {
        viewModelScope.launch {
            db.studyNoteDao().insertStudyNote(
                StudyNoteEntity(
                    courseCode = item.courseCode.ifBlank { "General" },
                    title = "Focus Note: ${item.thoughtText.take(36)}",
                    content = item.thoughtText,
                    category = "SUMMARY",
                    isPinned = false,
                    updatedEpochMillis = System.currentTimeMillis()
                )
            )
            db.focusDistractionDao().updateDistraction(item.copy(isResolved = true))
        }
    }

    fun startStrictLockout() {
        _isStrictLockoutActive.value = true
        startTimer()
    }

    fun abortStrictLockout() {
        val plannedMins = _focusTotalSeconds.value / 60
        val remainingMins = _focusRemainingSeconds.value / 60
        val elapsedMins = (plannedMins - remainingMins).coerceAtLeast(0)
        pauseTimer()
        _isStrictLockoutActive.value = false
        soundscapeSynthesizer.stop()
        _activeSoundscape.value = SoundscapeType.OFF

        viewModelScope.launch {
            db.focusLockoutRecordDao().insertLockoutRecord(
                FocusLockoutRecordEntity(
                    sessionTitle = _selectedFocusCourseCode.value,
                    plannedMinutes = plannedMins,
                    elapsedMinutes = elapsedMins,
                    completedSuccessfully = false,
                    abortedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private fun onTimerFinished() {
        _isTimerRunning.value = false
        val durationMins = (_focusTotalSeconds.value / 60).coerceAtLeast(1)
        val wasLockout = _isStrictLockoutActive.value
        _isStrictLockoutActive.value = false
        soundscapeSynthesizer.stop()
        _activeSoundscape.value = SoundscapeType.OFF

        val isStudy = _focusMode.value != FocusMode.SHORT_BREAK && _focusMode.value != FocusMode.LONG_BREAK

        viewModelScope.launch {
            if (isStudy) {
                db.focusRecordDao().insertFocusRecord(
                    FocusRecordEntity(
                        courseCode = _selectedFocusCourseCode.value,
                        durationMinutes = durationMins,
                        timestampEpochMillis = System.currentTimeMillis(),
                        mode = _focusMode.value.name,
                        completed = true
                    )
                )

                if (wasLockout) {
                    db.focusLockoutRecordDao().insertLockoutRecord(
                        FocusLockoutRecordEntity(
                            sessionTitle = _selectedFocusCourseCode.value,
                            plannedMinutes = durationMins,
                            elapsedMinutes = durationMins,
                            completedSuccessfully = true
                        )
                    )
                    toggleTodayHabit { habit ->
                        habit.copy(
                            deepFocusCompleted = true,
                            focusMinutesLogged = habit.focusMinutesLogged + durationMins,
                            xpEarned = habit.xpEarned + 100
                        )
                    }
                }
            }

            NotificationHelper.showNotification(
                context = getApplication(),
                notificationId = 9999,
                title = if (wasLockout) "Strict Lockout Completed!" else "Focus Session Complete!",
                message = "Great effort! You finished your $durationMins-minute session for ${_selectedFocusCourseCode.value}.",
                category = "FOCUS"
            )
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // AI Chat & Study Planning
    fun sendAiMessage(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = query)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val studentName = userProfile.value.name
            val totalFocusMins = focusRecords.value.filter {
                it.timestampEpochMillis > System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
            }.sumOf { it.durationMinutes }
            val studyHours = totalFocusMins / 60f

            val replyText = aiService.getAiResponse(
                prompt = query,
                studentName = studentName,
                courses = courses.value,
                lectures = lectures.value,
                assessments = assessments.value,
                attendance = attendanceRecords.value,
                weeklyStudyHours = studyHours
            )

            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "aura", text = replyText)
            _isAiThinking.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "aura",
                text = "Conversation refreshed. Ask me anything about your lectures, assignments, examinations, or study plan."
            )
        )
    }

    fun generateAiStudyPlan() {
        viewModelScope.launch {
            _aiStudyPlan.value = AuraAcademicAdvisor.generateRecommendedStudyPlan(
                courses = courses.value,
                lectures = lectures.value,
                pendingAssessments = assessments.value,
                attendanceList = attendanceRecords.value
            )
        }
    }

    fun acceptStudyRecommendation(recommendation: AuraAcademicAdvisor.StudyPlanRecommendation) {
        viewModelScope.launch {
            val now = LocalDate.now()
            val targetDate = now.plusDays((recommendation.dayOfWeek - now.dayOfWeek.value + 7).toLong() % 7)
            val parts = recommendation.startTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 18
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val startMillis = targetDate.atTime(h, m).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            val session = StudySessionEntity(
                courseCode = recommendation.courseCode,
                title = recommendation.title,
                scheduledStartEpochMillis = startMillis,
                scheduledEndEpochMillis = startMillis + (recommendation.durationMinutes * 60_000L),
                durationMinutes = recommendation.durationMinutes,
                isCompleted = false,
                isAiSuggested = true,
                notes = recommendation.rationale
            )
            val id = db.studySessionDao().insertSession(session)
            AlarmScheduler.scheduleStudySessionAlarm(getApplication(), session.copy(id = id))

            _aiStudyPlan.value = _aiStudyPlan.value.filter { it != recommendation }
        }
    }

    // Extra Feature 1: Flashcards & Spaced Repetition (Leitner System)
    fun addFlashcard(courseCode: String, deckTitle: String, question: String, answer: String) {
        viewModelScope.launch {
            db.flashcardDao().insertFlashcard(
                FlashcardEntity(
                    courseCode = courseCode,
                    deckTitle = deckTitle,
                    question = question,
                    answer = answer,
                    boxLevel = 1,
                    lastReviewedEpochMillis = System.currentTimeMillis()
                )
            )
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun reviewFlashcard(flashcard: FlashcardEntity, recallRating: String) {
        // SuperMemo-2 (SM-2) Spaced-Repetition Scheduling
        viewModelScope.launch {
            val quality = when (recallRating) {
                "AGAIN" -> 1
                "HARD" -> 3
                "GOOD" -> 4
                "EASY" -> 5
                else -> 4
            }
            val prevEf = flashcard.easeFactor.coerceIn(1.3, 3.5)
            val prevReps = flashcard.repetitions.coerceAtLeast(0)
            val prevInterval = flashcard.intervalDays.coerceAtLeast(1)

            val newReps: Int
            val newIntervalDays: Int
            if (quality < 3) {
                newReps = 0
                newIntervalDays = 1
            } else {
                newReps = prevReps + 1
                newIntervalDays = when (newReps) {
                    1 -> if (recallRating == "EASY") 3 else 1
                    2 -> when (recallRating) {
                        "HARD" -> 3
                        "EASY" -> 7
                        else -> 6
                    }
                    else -> {
                        val multiplier = if (recallRating == "EASY") prevEf * 1.25 else if (recallRating == "HARD") 1.2 else prevEf
                        (prevInterval * multiplier).toInt().coerceIn(2, 180)
                    }
                }
            }

            val efDelta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
            val newEf = (prevEf + efDelta).coerceIn(1.3, 3.2)
            val nextBox = when {
                newIntervalDays >= 21 -> 5
                newIntervalDays >= 10 -> 4
                newIntervalDays >= 5 -> 3
                newIntervalDays >= 2 -> 2
                else -> 1
            }

            val nextReview = System.currentTimeMillis() + (newIntervalDays * 24L * 3600L * 1000L)
            db.flashcardDao().updateFlashcard(
                flashcard.copy(
                    boxLevel = nextBox,
                    easeFactor = newEf,
                    intervalDays = newIntervalDays,
                    repetitions = newReps,
                    lastReviewedEpochMillis = System.currentTimeMillis(),
                    nextReviewEpochMillis = nextReview
                )
            )
            toggleTodayHabit { habit ->
                habit.copy(
                    flashcardsReviewed = true,
                    xpEarned = habit.xpEarned + 10
                )
            }
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteFlashcard(flashcard: FlashcardEntity) {
        viewModelScope.launch {
            db.flashcardDao().deleteFlashcard(flashcard)
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // Extra Feature 2: Audio Lecture Notes
    fun saveAudioNote(title: String, courseCode: String, filePath: String, durationSeconds: Int, notes: String) {
        viewModelScope.launch {
            db.audioNoteDao().insertAudioNote(
                AudioNoteEntity(
                    title = title,
                    courseCode = courseCode,
                    filePath = filePath,
                    durationSeconds = durationSeconds,
                    recordedEpochMillis = System.currentTimeMillis(),
                    notes = notes
                )
            )
        }
    }

    fun deleteAudioNote(note: AudioNoteEntity) {
        viewModelScope.launch {
            audioRecorder.stopPlaying()
            val file = java.io.File(note.filePath)
            if (file.exists()) file.delete()
            db.audioNoteDao().deleteAudioNote(note)
        }
    }

    // Extra Feature 3: Syllabus Topics & Exam Readiness
    fun addSyllabusTopic(courseId: Long, courseCode: String, topicName: String, confidence: Int) {
        viewModelScope.launch {
            db.syllabusTopicDao().insertTopic(
                SyllabusTopicEntity(
                    courseId = courseId,
                    courseCode = courseCode,
                    topicName = topicName,
                    isCompleted = false,
                    confidenceLevel = confidence
                )
            )
        }
    }

    fun toggleTopicCompleted(topic: SyllabusTopicEntity) {
        viewModelScope.launch {
            db.syllabusTopicDao().updateTopic(topic.copy(isCompleted = !topic.isCompleted))
        }
    }

    fun deleteSyllabusTopic(topic: SyllabusTopicEntity) {
        viewModelScope.launch { db.syllabusTopicDao().deleteTopic(topic) }
    }

    // Extra Feature 4: GPA & Target Grade Simulator
    fun addCourseGrade(courseCode: String, courseName: String, creditHours: Int, letterGrade: String, semester: String) {
        viewModelScope.launch {
            val point = when (letterGrade) {
                "A" -> 4.0
                "B+" -> 3.5
                "B" -> 3.0
                "C+" -> 2.5
                "C" -> 2.0
                "D+" -> 1.5
                "D" -> 1.0
                else -> 0.0
            }
            db.courseGradeDao().insertGrade(
                CourseGradeEntity(
                    courseCode = courseCode,
                    courseName = courseName,
                    creditHours = creditHours,
                    letterGrade = letterGrade,
                    gradePoint = point,
                    semester = semester
                )
            )
        }
    }

    fun deleteCourseGrade(grade: CourseGradeEntity) {
        viewModelScope.launch { db.courseGradeDao().deleteGrade(grade) }
    }

    // Extra Feature 5: JSON Timetable Export & Import
    suspend fun exportTimetableJson(): String {
        val coursesList = db.courseDao().getAllCoursesList()
        val lecturesList = db.lectureDao().getAllLecturesList()
        val assessmentsList = db.assessmentDao().getAllAssessmentsList()
        return TimetableJsonHelper.exportToJson(
            ExportData(courses = coursesList, lectures = lecturesList, assessments = assessmentsList)
        )
    }

    fun importTimetableJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val data = TimetableJsonHelper.parseJson(jsonString)
            if (data == null) {
                onComplete(false, "Invalid JSON structure. Please check the timetable format.")
                return@launch
            }
            try {
                for (course in data.courses) {
                    val cId = db.courseDao().insertCourse(course)
                    // Update matching lectures
                    val matchingLecs = data.lectures.filter { it.courseCode.equals(course.code, ignoreCase = true) }
                    for (lec in matchingLecs) {
                        db.lectureDao().insertLecture(lec.copy(courseId = cId))
                    }
                }
                for (assessment in data.assessments) {
                    db.assessmentDao().insertAssessment(assessment)
                }
                AlarmScheduler.rescheduleAll(getApplication())
                onComplete(true, "Successfully imported ${data.courses.size} courses and ${data.lectures.size} lectures!")
            } catch (e: Exception) {
                onComplete(false, "Import failed: ${e.message}")
            }
        }
    }

    // Profile & Settings
    fun updateProfile(
        name: String,
        institution: String,
        programme: String,
        level: String,
        semester: String,
        timeZone: String,
        targetAttendance: Int,
        studyGoalHours: Int
    ) {
        viewModelScope.launch {
            prefs.updateProfile(
                name = name,
                institution = institution,
                programme = programme,
                level = level,
                semester = semester,
                timeZone = timeZone,
                attendanceTarget = targetAttendance,
                studyGoalHours = studyGoalHours
            )
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch { prefs.setDarkTheme(isDark) }
    }

    fun setAccentPreset(presetId: String) {
        viewModelScope.launch {
            prefs.setAccentPreset(presetId)
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun refreshAllHomeWidgets() {
        AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
    }

    fun setAlarmGentleFadeIn(enabled: Boolean) {
        viewModelScope.launch { prefs.setAlarmGentleFadeIn(enabled) }
    }

    fun setAlarmWakeUpBrainCheck(enabled: Boolean) {
        viewModelScope.launch { prefs.setAlarmWakeUpBrainCheck(enabled) }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setDefaultSnoozeMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationsEnabled(enabled)
            if (enabled) {
                AlarmScheduler.rescheduleAll(getApplication())
            } else {
                LectureAlarmRinger.stopRinging(getApplication())
            }
        }
    }

    fun setAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAlarmsEnabled(enabled)
            if (enabled) {
                AlarmScheduler.rescheduleAll(getApplication())
            } else {
                LectureAlarmRinger.stopRinging(getApplication())
            }
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAiEnabled(enabled) }
    }

    fun triggerTestAlarm() {
        val profile = userProfile.value
        AlarmScheduler.triggerTestLectureAlarmNow(
            context = getApplication(),
            studentName = profile.name,
            toneId = profile.defaultAlarmToneId,
            customAlarmPath = profile.customAlarmFilePath
        )
    }

    fun setDefaultAlarmTone(toneId: String) {
        viewModelScope.launch {
            prefs.setDefaultAlarmTone(toneId)
            AlarmScheduler.rescheduleAll(getApplication())
        }
    }

    fun importCustomAlarmSoundFromUri(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val previousCustom = userProfile.value.customAlarmFilePath
            val meta = withContext(Dispatchers.IO) {
                MediaStorageHelper.importAudioFile(
                    context = getApplication(),
                    uri = uri,
                    subfolder = "alarm_tones",
                    fallbackPrefix = "Custom Alarm"
                )
            }
            if (meta == null) {
                onResult(false, "Could not load selected audio file.")
                return@launch
            }
            if (previousCustom.isNotBlank() && previousCustom != meta.localFilePath) {
                withContext(Dispatchers.IO) {
                    MediaStorageHelper.deleteLocalMediaFile(getApplication(), previousCustom)
                }
            }
            prefs.setCustomAlarmSound(meta.localFilePath, meta.title)
            AlarmScheduler.rescheduleAll(getApplication())
            onResult(true, "Set '${meta.title}' as your lecture alarm tone!")
        }
    }

    fun clearCustomAlarmSound() {
        viewModelScope.launch {
            val prev = userProfile.value.customAlarmFilePath
            if (prev.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    MediaStorageHelper.deleteLocalMediaFile(getApplication(), prev)
                }
            }
            prefs.setCustomAlarmSound("", "")
            AlarmScheduler.rescheduleAll(getApplication())
        }
    }

    fun previewAlarmTone(
        toneId: String,
        customPath: String = userProfile.value.customAlarmFilePath
    ) {
        val effectiveTone = if (toneId.isBlank()) userProfile.value.defaultAlarmToneId else toneId
        LectureAlarmRinger.previewTone(
            context = getApplication(),
            toneId = effectiveTone,
            customFilePath = customPath
        )
    }

    fun stopAlarmPreview() {
        LectureAlarmRinger.stopPreview(getApplication())
    }

    // Profile Picture & Timetable Picture Uploads
    fun importProfilePictureFromUri(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val prevPath = userProfile.value.profilePicturePath
            val savedPath = withContext(Dispatchers.IO) {
                MediaStorageHelper.importProfilePicture(getApplication(), uri, prevPath)
            }
            if (savedPath == null) {
                onResult(false, "Could not load the selected image.")
                return@launch
            }
            prefs.setProfilePicturePath(savedPath)
            onResult(true, "Profile photo updated!")
        }
    }

    fun removeProfilePicture() {
        viewModelScope.launch {
            val prevPath = userProfile.value.profilePicturePath
            if (prevPath.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    MediaStorageHelper.deleteLocalMediaFile(getApplication(), prevPath)
                }
            }
            prefs.setProfilePicturePath("")
        }
    }

    fun importTimetablePictureFromUri(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val prevPath = userProfile.value.timetableImagePath
            val savedPath = withContext(Dispatchers.IO) {
                MediaStorageHelper.importTimetablePicture(getApplication(), uri, prevPath)
            }
            if (savedPath == null) {
                onResult(false, "Could not load the selected timetable image.")
                return@launch
            }
            prefs.setTimetableImagePath(savedPath)
            onResult(true, "Official timetable photo saved!")
        }
    }

    fun removeTimetablePicture() {
        viewModelScope.launch {
            val prevPath = userProfile.value.timetableImagePath
            if (prevPath.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    MediaStorageHelper.deleteLocalMediaFile(getApplication(), prevPath)
                }
            }
            prefs.setTimetableImagePath("")
        }
    }

    fun dismissActiveAlarm(notificationId: Int? = null) {
        val idToCancel = notificationId ?: activeRingingAlarm.value?.notificationId
        LectureAlarmRinger.stopRinging(getApplication())
        if (idToCancel != null && idToCancel != 0) {
            NotificationHelper.cancelNotification(getApplication(), idToCancel)
        }
    }

    fun snoozeActiveAlarm(
        notificationId: Int,
        title: String,
        message: String,
        category: String,
        eventId: Long,
        courseCode: String,
        snoozeMinutes: Int = userProfile.value.defaultSnoozeMinutes
    ) {
        LectureAlarmRinger.stopRinging(getApplication())
        NotificationHelper.cancelNotification(getApplication(), notificationId)
        AlarmScheduler.scheduleSnoozeAlarm(
            context = getApplication(),
            notificationId = notificationId,
            title = title,
            message = message,
            category = category,
            eventId = eventId,
            courseCode = courseCode,
            snoozeMinutes = snoozeMinutes
        )
    }

    // Academic Citation & Reference Formatter Actions
    fun saveCitation(citation: CitationEntity) {
        viewModelScope.launch {
            val formatted = CitationFormatter.formatCitation(
                styleCode = citation.style,
                sourceTypeCode = citation.sourceType,
                authors = citation.authors,
                year = citation.year,
                title = citation.title,
                publicationOrPublisher = citation.publicationOrPublisher,
                volumeOrPages = citation.volumeOrPages,
                urlOrDoi = citation.urlOrDoi
            )
            db.citationDao().insertCitation(citation.copy(formattedCitation = formatted))
        }
    }

    fun deleteCitation(citation: CitationEntity) {
        viewModelScope.launch {
            db.citationDao().deleteCitation(citation)
        }
    }

    fun parseRawCitationSource(rawInput: String, onParsed: (ParsedCitationDraft) -> Unit) {
        if (rawInput.isBlank()) return
        viewModelScope.launch {
            _isParsingCitation.value = true
            try {
                val parsed = aiService.parseCitationSource(rawInput.trim())
                onParsed(parsed)
            } finally {
                _isParsingCitation.value = false
            }
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch { db.notificationHistoryDao().markAsRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { db.notificationHistoryDao().markAllAsRead() }
    }

    fun clearNotifications() {
        viewModelScope.launch { db.notificationHistoryDao().clearAll() }
    }

    // Course Weights & Grade Simulation
    fun addCourseWeightItem(item: CourseWeightItemEntity) {
        viewModelScope.launch { db.courseWeightItemDao().insertWeightItem(item) }
    }

    fun deleteCourseWeightItem(item: CourseWeightItemEntity) {
        viewModelScope.launch { db.courseWeightItemDao().deleteWeightItem(item) }
    }

    // Exam Milestones & Countdown Roadmaps
    fun addExamMilestone(milestone: ExamMilestoneEntity) {
        viewModelScope.launch { db.examMilestoneDao().insertMilestone(milestone) }
    }

    fun toggleMilestone(milestone: ExamMilestoneEntity) {
        viewModelScope.launch { db.examMilestoneDao().updateMilestone(milestone) }
    }

    fun deleteExamMilestone(milestone: ExamMilestoneEntity) {
        viewModelScope.launch { db.examMilestoneDao().deleteMilestone(milestone) }
    }

    fun generateDefaultMilestonesForExam(exam: AssessmentEntity) {
        viewModelScope.launch {
            val examDate = exam.dueEpochMillis
            val m1 = ExamMilestoneEntity(
                assessmentId = exam.id,
                courseCode = exam.courseCode,
                examTitle = exam.title,
                phaseNumber = 1,
                phaseTitle = "Phase 1: Conceptual Mastery & Slide Review",
                targetDateEpochMillis = examDate - (14 * 24 * 3600 * 1000L),
                isCompleted = false
            )
            val m2 = ExamMilestoneEntity(
                assessmentId = exam.id,
                courseCode = exam.courseCode,
                examTitle = exam.title,
                phaseNumber = 2,
                phaseTitle = "Phase 2: Question Banks & Past Paper Blitz",
                targetDateEpochMillis = examDate - (7 * 24 * 3600 * 1000L),
                isCompleted = false
            )
            val m3 = ExamMilestoneEntity(
                assessmentId = exam.id,
                courseCode = exam.courseCode,
                examTitle = exam.title,
                phaseNumber = 3,
                phaseTitle = "Phase 3: High-Yield Formula Flash Review & Mocks",
                targetDateEpochMillis = examDate - (2 * 24 * 3600 * 1000L),
                isCompleted = false
            )
            db.examMilestoneDao().insertMilestone(m1)
            db.examMilestoneDao().insertMilestone(m2)
            db.examMilestoneDao().insertMilestone(m3)
        }
    }

    // Academic Habits & Streak Tracking
    fun loadTodayHabit() {
        viewModelScope.launch {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val habit = db.academicHabitDao().getHabitForDate(todayStr) ?: AcademicHabitEntity(dateString = todayStr)
            _todayAcademicHabit.value = habit
        }
    }

    fun toggleTodayHabit(updater: (AcademicHabitEntity) -> AcademicHabitEntity) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val current = _todayAcademicHabit.value ?: AcademicHabitEntity(dateString = todayStr)
            val updated = updater(current)
            _todayAcademicHabit.value = updated
            db.academicHabitDao().insertOrUpdateHabit(updated)
        }
    }

    // New Feature 1: Smart Study Notes & Formula Vault
    fun saveStudyNote(
        id: Long = 0L,
        courseCode: String,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            db.studyNoteDao().insertStudyNote(
                StudyNoteEntity(
                    id = id,
                    courseCode = courseCode.ifBlank { "General" },
                    title = title.trim(),
                    content = content.trim(),
                    category = category,
                    isPinned = isPinned,
                    updatedEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun togglePinStudyNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            db.studyNoteDao().updateStudyNote(
                note.copy(
                    isPinned = !note.isPinned,
                    updatedEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteStudyNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            db.studyNoteDao().deleteStudyNote(note)
        }
    }

    fun convertNoteToFlashcard(note: StudyNoteEntity) {
        viewModelScope.launch {
            db.flashcardDao().insertFlashcard(
                FlashcardEntity(
                    courseCode = note.courseCode,
                    deckTitle = if (note.category == "FORMULA") "Formulas & Equations" else "Lecture Notes",
                    question = note.title,
                    answer = note.content,
                    boxLevel = 1,
                    lastReviewedEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    // New Feature 2: Lecturer Office Hours & Consultation Hub
    fun saveOfficeHour(officeHour: OfficeHourEntity) {
        viewModelScope.launch {
            val id = db.officeHourDao().insertOfficeHour(officeHour)
            val saved = officeHour.copy(id = if (officeHour.id == 0L) id else officeHour.id)
            if (saved.reminderEnabled) {
                AlarmScheduler.scheduleOfficeHourAlarm(getApplication(), saved)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), 400000 + saved.id.toInt())
            }
        }
    }

    fun deleteOfficeHour(officeHour: OfficeHourEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), 400000 + officeHour.id.toInt())
            db.officeHourDao().deleteOfficeHour(officeHour)
        }
    }

    // Practice Quiz & Mock Exam Engine
    fun generateQuizForCourse(courseCode: String, onGenerated: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            try {
                val courseName = courses.value.find { it.code.equals(courseCode, ignoreCase = true) }?.name ?: courseCode
                val generated = aiService.generatePracticeQuiz(
                    courseCode = courseCode,
                    courseName = courseName,
                    notes = studyNotes.value,
                    flashcards = flashcards.value,
                    topics = syllabusTopics.value
                )
                db.quizDao().insertQuestions(generated)
                onGenerated(generated.size)
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    fun addCustomQuizQuestion(question: QuizQuestionEntity) {
        viewModelScope.launch {
            db.quizDao().insertQuestion(question)
        }
    }

    fun deleteQuizQuestion(question: QuizQuestionEntity) {
        viewModelScope.launch {
            db.quizDao().deleteQuestion(question)
        }
    }

    fun recordQuizAttempt(
        courseCode: String,
        quizTitle: String,
        scoreCount: Int,
        totalQuestions: Int,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            db.quizDao().insertAttempt(
                QuizAttemptEntity(
                    courseCode = courseCode,
                    quizTitle = quizTitle,
                    scoreCount = scoreCount,
                    totalQuestions = totalQuestions,
                    durationSeconds = durationSeconds,
                    timestampEpochMillis = System.currentTimeMillis()
                )
            )
            toggleTodayHabit { habit ->
                habit.copy(
                    flashcardsReviewed = true,
                    xpEarned = habit.xpEarned + (scoreCount * 15).coerceAtLeast(20)
                )
            }
        }
    }

    // Photo / Slide Scanner to Study Notes & Flashcards
    fun scanSlideBitmap(
        bitmap: Bitmap,
        courseCode: String,
        onResult: (SlideExtractionResult) -> Unit
    ) {
        viewModelScope.launch {
            _isScanningSlide.value = true
            try {
                val result = aiService.extractNotesFromSlideBitmap(bitmap, courseCode)
                onResult(result)
            } finally {
                _isScanningSlide.value = false
            }
        }
    }

    fun saveScannedSlideMaterial(
        courseCode: String,
        title: String,
        category: String,
        extractedNotes: String,
        flashcardsToCreate: List<Pair<String, String>>
    ) {
        viewModelScope.launch {
            db.studyNoteDao().insertStudyNote(
                StudyNoteEntity(
                    courseCode = courseCode.ifBlank { "General" },
                    title = title.trim().ifBlank { "$courseCode Slide Notes" },
                    content = extractedNotes.trim(),
                    category = category,
                    isPinned = true,
                    updatedEpochMillis = System.currentTimeMillis()
                )
            )
            flashcardsToCreate.forEach { (q, a) ->
                if (q.isNotBlank() && a.isNotBlank()) {
                    db.flashcardDao().insertFlashcard(
                        FlashcardEntity(
                            courseCode = courseCode.ifBlank { "General" },
                            deckTitle = "Scanned Slides",
                            question = q.trim(),
                            answer = a.trim(),
                            boxLevel = 1,
                            lastReviewedEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    // Group Project & Study Group Splitter
    fun saveGroupProject(project: GroupProjectEntity) {
        viewModelScope.launch {
            val id = db.groupProjectDao().insertGroupProject(project)
            val saved = project.copy(id = if (project.id == 0L) id else project.id)
            if (saved.reminderEnabled) {
                AlarmScheduler.scheduleAssessmentAlarm(
                    getApplication(),
                    AssessmentEntity(
                        id = 50000L + saved.id,
                        courseId = 0L,
                        courseCode = saved.courseCode,
                        courseName = saved.teamName,
                        title = "Group Project: ${saved.projectTitle}",
                        type = "ASSIGNMENT",
                        dueEpochMillis = saved.dueEpochMillis,
                        priority = "HIGH"
                    )
                )
            }
        }
    }

    fun deleteGroupProject(project: GroupProjectEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), 250000 + project.id.toInt())
            db.groupProjectDao().deleteTasksForProject(project.id)
            db.groupProjectDao().deleteGroupProject(project)
        }
    }

    fun addGroupTask(task: GroupTaskEntity) {
        viewModelScope.launch {
            db.groupProjectDao().insertGroupTask(task)
        }
    }

    fun toggleGroupTaskCompleted(task: GroupTaskEntity) {
        viewModelScope.launch {
            db.groupProjectDao().updateGroupTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteGroupTask(task: GroupTaskEntity) {
        viewModelScope.launch {
            db.groupProjectDao().deleteGroupTask(task)
        }
    }

    // Academic Expense & Semester Budget Tracker
    fun addAcademicExpense(expense: AcademicExpenseEntity) {
        viewModelScope.launch {
            db.academicExpenseDao().insertExpense(expense)
        }
    }

    fun deleteAcademicExpense(expense: AcademicExpenseEntity) {
        viewModelScope.launch {
            db.academicExpenseDao().deleteExpense(expense)
        }
    }

    fun updateBudgetSettings(limit: Double, currencySymbol: String) {
        viewModelScope.launch {
            prefs.setBudgetConfig(limit, currencySymbol)
        }
    }

    // Course Slides & Multi-Format Resource Hub (PDF, PPTX, DOCX, TXT, Images & More)
    fun importCourseResourceFromUri(
        uri: Uri,
        courseId: Long,
        courseCode: String,
        customTitle: String,
        moduleFolder: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isImportingResource.value = true
            try {
                val payload = withContext(Dispatchers.IO) {
                    SlideDocumentParser.importDocumentFromUri(
                        context = getApplication(),
                        uri = uri,
                        customTitle = customTitle
                    )
                }
                if (payload == null) {
                    onResult(false, "Could not open or copy the selected document. Please make sure the file is downloaded on your device.")
                    return@launch
                }
                val normalizedCode = courseCode.trim().uppercase().ifBlank { "GENERAL" }
                val safeDbText = SlideDocumentParser.truncateForSqlite(payload.serializedText)
                val entity = CourseResourceEntity(
                    courseId = courseId,
                    courseCode = normalizedCode,
                    title = payload.title.take(160),
                    moduleFolder = moduleFolder.trim().ifBlank { "Week 1" }.take(80),
                    fileType = payload.fileType,
                    mimeType = payload.mimeType,
                    originalFileName = payload.originalFileName.take(180),
                    localFilePath = payload.localFilePath,
                    fileSizeBytes = payload.fileSizeBytes,
                    totalPages = payload.totalPages.coerceAtLeast(1),
                    lastReadPage = 0,
                    extractedText = safeDbText,
                    lastOpenedEpochMillis = System.currentTimeMillis(),
                    addedEpochMillis = System.currentTimeMillis()
                )
                db.courseResourceDao().insertResource(entity)
                toggleTodayHabit { habit ->
                    habit.copy(syllabusProgressMade = true, xpEarned = habit.xpEarned + 20)
                }
                AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
                onResult(true, "Added '${payload.title}' (${payload.fileType}, ${payload.totalPages} slide/page(s)).")
            } catch (t: Throwable) {
                onResult(false, "Unable to import slide deck (${t.javaClass.simpleName}). Try selecting a local copy of the file.")
            } finally {
                _isImportingResource.value = false
            }
        }
    }

    fun createCustomTextSlideDeck(
        courseId: Long,
        courseCode: String,
        title: String,
        moduleFolder: String,
        rawSlideContent: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isImportingResource.value = true
            try {
                val payload = withContext(Dispatchers.IO) {
                    SlideDocumentParser.buildTextSlideDeckPayload(
                        context = getApplication(),
                        title = title,
                        rawContent = rawSlideContent
                    )
                }
                val entity = CourseResourceEntity(
                    courseId = courseId,
                    courseCode = courseCode.trim().uppercase().ifBlank { "GENERAL" },
                    title = payload.title.take(160),
                    moduleFolder = moduleFolder.trim().ifBlank { "Lecture Slides" }.take(80),
                    fileType = payload.fileType,
                    mimeType = payload.mimeType,
                    originalFileName = payload.originalFileName,
                    localFilePath = payload.localFilePath,
                    fileSizeBytes = payload.fileSizeBytes,
                    totalPages = payload.totalPages.coerceAtLeast(1),
                    lastReadPage = 0,
                    extractedText = SlideDocumentParser.truncateForSqlite(payload.serializedText)
                )
                db.courseResourceDao().insertResource(entity)
                AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
                onSaved()
            } catch (_: Throwable) {
            } finally {
                _isImportingResource.value = false
            }
        }
    }

    fun createFlashcardFromSlidePage(
        courseCode: String,
        deckTitle: String,
        slideTitle: String,
        slideBody: String,
        onCreated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cleanQuestion = if (slideTitle.endsWith("?")) {
                    slideTitle
                } else {
                    "Explain key takeaways from: $slideTitle"
                }
                val cleanAnswer = slideBody.trim().take(700).ifBlank { slideTitle }
                db.flashcardDao().insertFlashcard(
                    FlashcardEntity(
                        courseCode = courseCode.trim().uppercase().ifBlank { "GENERAL" },
                        deckTitle = deckTitle.take(40).ifBlank { "Lecture Slides" },
                        question = cleanQuestion.take(240),
                        answer = cleanAnswer,
                        boxLevel = 1,
                        lastReviewedEpochMillis = System.currentTimeMillis()
                    )
                )
                toggleTodayHabit { habit ->
                    habit.copy(flashcardsReviewed = true, xpEarned = habit.xpEarned + 10)
                }
                onCreated()
            } catch (_: Throwable) {
            }
        }
    }

    fun updateCourseResourceMetadata(
        resource: CourseResourceEntity,
        newTitle: String,
        newModuleFolder: String,
        newCourseId: Long,
        newCourseCode: String
    ) {
        viewModelScope.launch {
            db.courseResourceDao().updateResource(
                resource.copy(
                    title = newTitle.trim().ifBlank { resource.title },
                    moduleFolder = newModuleFolder.trim().ifBlank { resource.moduleFolder },
                    courseId = newCourseId,
                    courseCode = newCourseCode.trim().uppercase().ifBlank { "GENERAL" },
                    lastOpenedEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateResourceReadingProgress(resource: CourseResourceEntity, pageIndex: Int) {
        viewModelScope.launch {
            val clamped = pageIndex.coerceIn(0, (resource.totalPages - 1).coerceAtLeast(0))
            val reachedEnd = clamped >= (resource.totalPages - 1).coerceAtLeast(0)
            db.courseResourceDao().updateResource(
                resource.copy(
                    lastReadPage = clamped,
                    isCompleted = resource.isCompleted || (reachedEnd && resource.totalPages > 1),
                    lastOpenedEpochMillis = System.currentTimeMillis()
                )
            )
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleResourceBookmark(resource: CourseResourceEntity, pageIndex: Int) {
        viewModelScope.launch {
            val existing = resource.bookmarkedPagesCsv
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toMutableSet()
            if (existing.contains(pageIndex)) {
                existing.remove(pageIndex)
            } else {
                existing.add(pageIndex)
            }
            val updatedCsv = existing.sorted().joinToString(",")
            db.courseResourceDao().updateResource(
                resource.copy(
                    bookmarkedPagesCsv = updatedCsv,
                    lastOpenedEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveResourcePersonalNotes(
        resource: CourseResourceEntity,
        notesText: String,
        alsoSaveToVault: Boolean = false
    ) {
        viewModelScope.launch {
            db.courseResourceDao().updateResource(
                resource.copy(
                    personalNotes = notesText.trim(),
                    lastOpenedEpochMillis = System.currentTimeMillis()
                )
            )
            if (alsoSaveToVault && notesText.isNotBlank()) {
                db.studyNoteDao().insertStudyNote(
                    StudyNoteEntity(
                        courseCode = resource.courseCode,
                        title = "${resource.title} — Slide Takeaways",
                        content = notesText.trim(),
                        category = "SUMMARY",
                        isPinned = true,
                        updatedEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun togglePinCourseResource(resource: CourseResourceEntity) {
        viewModelScope.launch {
            db.courseResourceDao().updateResource(resource.copy(isPinned = !resource.isPinned))
        }
    }

    fun toggleMasteredCourseResource(resource: CourseResourceEntity) {
        viewModelScope.launch {
            val nowCompleted = !resource.isCompleted
            db.courseResourceDao().updateResource(resource.copy(isCompleted = nowCompleted))
            if (nowCompleted) {
                toggleTodayHabit { habit ->
                    habit.copy(syllabusProgressMade = true, xpEarned = habit.xpEarned + 30)
                }
            }
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteCourseResource(resource: CourseResourceEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SlideDocumentParser.deleteLocalResourceFile(resource.localFilePath)
            }
            db.courseResourceDao().deleteResourceById(resource.id)
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun synthesizeStudyPackFromResource(
        resource: CourseResourceEntity,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSynthesizingResource.value = true
            try {
                val plainText = withContext(Dispatchers.IO) {
                    SlideDocumentParser.loadPlainTextForResource(resource).take(24_000)
                }
                val optionalBitmap = withContext(Dispatchers.IO) {
                    val file = java.io.File(resource.localFilePath)
                    when {
                        resource.fileType == "IMAGE" && file.exists() ->
                            MediaStorageHelper.loadBitmapSafe(file.absolutePath, maxDimension = 1280)
                        resource.fileType == "PDF" && file.exists() -> SlideDocumentParser.renderPdfPageBitmap(
                            file = file,
                            pageIndex = resource.lastReadPage,
                            targetWidthPx = 900
                        )
                        else -> null
                    }
                }

                val pack = aiService.synthesizeStudyPackFromSlides(
                    courseCode = resource.courseCode,
                    resourceTitle = resource.title,
                    plainSlideText = plainText,
                    optionalBitmap = optionalBitmap
                )

                db.studyNoteDao().insertStudyNote(
                    StudyNoteEntity(
                        courseCode = resource.courseCode,
                        title = pack.summaryTitle,
                        content = pack.summaryContent,
                        category = "SUMMARY",
                        isPinned = true,
                        updatedEpochMillis = System.currentTimeMillis()
                    )
                )

                pack.flashcards.forEach { (q, a) ->
                    db.flashcardDao().insertFlashcard(
                        FlashcardEntity(
                            courseCode = resource.courseCode,
                            deckTitle = resource.title.take(40),
                            question = q,
                            answer = a,
                            boxLevel = 1,
                            lastReviewedEpochMillis = System.currentTimeMillis()
                        )
                    )
                }

                if (pack.quizQuestions.isNotEmpty()) {
                    db.quizDao().insertQuestions(pack.quizQuestions)
                }

                toggleTodayHabit { habit ->
                    habit.copy(
                        flashcardsReviewed = true,
                        syllabusProgressMade = true,
                        xpEarned = habit.xpEarned + 40
                    )
                }

                onComplete(
                    "Created 1 Summary Note, ${pack.flashcards.size} Flashcards & ${pack.quizQuestions.size} Practice Quiz MCQs from '${resource.title}'!"
                )
            } catch (t: Throwable) {
                onComplete("Could not synthesize pack right now (${t.javaClass.simpleName}).")
            } finally {
                _isSynthesizingResource.value = false
            }
        }
    }

    // Feature: Smart Timetable & Syllabus Camera/PDF Scanner
    fun scanTimetableOrSyllabus(
        bitmap: Bitmap?,
        rawText: String,
        onResult: (TimetableScanResult) -> Unit
    ) {
        viewModelScope.launch {
            _isScanningTimetable.value = true
            try {
                val res = aiService.scanTimetableOrSyllabus(bitmap, rawText)
                onResult(res)
            } finally {
                _isScanningTimetable.value = false
            }
        }
    }

    fun importScannedTimetable(
        scanResult: TimetableScanResult,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val palette = listOf("#0A84FF", "#38BDF8", "#2DD4BF", "#818CF8", "#34D399")
            val existingCourses = db.courseDao().getAllCoursesList().toMutableList()
            val codeToId = existingCourses.associate { it.code.uppercase() to it.id }.toMutableMap()

            scanResult.courses.forEachIndexed { idx, draft ->
                val cleanCode = draft.code.trim().uppercase()
                if (!codeToId.containsKey(cleanCode)) {
                    val insertedId = db.courseDao().insertCourse(
                        CourseEntity(
                            code = cleanCode,
                            name = draft.name.trim().ifBlank { cleanCode },
                            creditHours = draft.creditHours.coerceIn(1, 6),
                            lecturer = draft.lecturer.trim(),
                            venue = draft.venue.trim(),
                            colorHex = palette[idx % palette.size]
                        )
                    )
                    codeToId[cleanCode] = insertedId
                }
            }

            scanResult.lectures.forEachIndexed { idx, lec ->
                val cleanCode = lec.courseCode.trim().uppercase()
                val cId = codeToId.getOrPut(cleanCode) {
                    db.courseDao().insertCourse(
                        CourseEntity(
                            code = cleanCode,
                            name = lec.courseName.ifBlank { cleanCode },
                            venue = lec.venue,
                            colorHex = palette[idx % palette.size]
                        )
                    )
                }
                persistAndScheduleLecture(
                    LectureEntity(
                        courseId = cId,
                        courseCode = cleanCode,
                        courseName = lec.courseName.ifBlank { cleanCode },
                        dayOfWeek = lec.dayOfWeek.coerceIn(1, 7),
                        startTime = lec.startTime,
                        endTime = lec.endTime,
                        venue = lec.venue,
                        walkingMinutes = lec.walkingMinutes,
                        colorHex = palette[idx % palette.size]
                    )
                )
            }

            scanResult.exams.forEach { ex ->
                val cleanCode = ex.courseCode.trim().uppercase()
                val cId = codeToId.getOrPut(cleanCode) {
                    db.courseDao().insertCourse(
                        CourseEntity(
                            code = cleanCode,
                            name = ex.courseName.ifBlank { cleanCode },
                            colorHex = "#38BDF8"
                        )
                    )
                }
                val dueMillis = System.currentTimeMillis() + ex.daysFromNow.coerceAtLeast(1) * 24L * 3600L * 1000L
                val examEntity = AssessmentEntity(
                    courseId = cId,
                    courseCode = cleanCode,
                    courseName = ex.courseName.ifBlank { cleanCode },
                    title = ex.title.ifBlank { "$cleanCode Examination" },
                    type = "MID_SEM",
                    dueEpochMillis = dueMillis,
                    priority = "HIGH",
                    examVenue = ex.examVenue
                )
                val insertedId = db.assessmentDao().insertAssessment(examEntity)
                AlarmScheduler.scheduleAssessmentAlarm(getApplication(), examEntity.copy(id = insertedId))
            }

            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
            onComplete(
                "Imported ${scanResult.courses.size} course(s), ${scanResult.lectures.size} lecture(s) & ${scanResult.exams.size} exam(s)!"
            )
        }
    }

    fun setWidgetGlassStyle(styleId: String) {
        viewModelScope.launch {
            prefs.setWidgetGlassStyle(styleId)
            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setWebPortalUrl(url: String) {
        viewModelScope.launch {
            prefs.setWebPortalUrl(url)
        }
    }

    suspend fun shouldAutoRestoreForUid(uid: String): Boolean = !prefs.hasAutoRestoredUid(uid)

    fun markUidAutoRestored(uid: String) {
        viewModelScope.launch { prefs.markUidAutoRestored(uid) }
    }

    fun evaluateOralViva(
        courseCode: String,
        question: String,
        expectedAnswer: String,
        studentSpokenAnswer: String,
        onResult: (AuraAiService.VivaEvaluationResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = aiService.evaluateVivaAnswer(courseCode, question, expectedAnswer, studentSpokenAnswer)
            onResult(res)
        }
    }

    fun critiqueStudyNoteDraft(
        courseCode: String,
        title: String,
        content: String,
        onResult: (AuraAiService.RubricCritiqueResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = aiService.critiqueAcademicDraft(courseCode, title, content)
            onResult(res)
        }
    }

    // Feature: Complete Cloud Account Backup & Automatic Cross-Device Restore
    suspend fun buildCloudBackupPayload(
        userId: String,
        accountEmail: String = ""
    ): UserBackup = withContext(Dispatchers.IO) {
        val profile = prefs.userProfileFlow.first()
        val exportedSettings = prefs.exportProfileSettingsList(accountEmail)
        val allCourses = db.courseDao().getAllCoursesList()
        val allLectures = db.lectureDao().getAllLecturesList()
        val allAssessments = db.assessmentDao().getAllAssessmentsList()
        val allCards = db.flashcardDao().getAllFlashcardsList()
        val allNotes = db.studyNoteDao().getAllStudyNotes().first()
        val allGrades = db.courseGradeDao().getAllGrades().first()
        val allExpenses = db.academicExpenseDao().getAllExpenses().first()
        val totalFocusMins = db.focusRecordDao().getAllFocusRecordsList().sumOf { it.durationMinutes }.toLong()

        val delim = "||"
        UserBackup(
            id = userId,
            userId = userId,
            studentName = profile.name,
            institution = profile.institution,
            department = profile.programme,
            targetGpa = 4.0,
            coursesCount = allCourses.size.toLong(),
            lecturesCount = allLectures.size.toLong(),
            assessmentsCount = allAssessments.size.toLong(),
            flashcardsCount = allCards.size.toLong(),
            totalFocusMinutes = totalFocusMins,
            profileSettings = exportedSettings,
            courseRecords = allCourses.take(50).map { c ->
                listOf(c.code, c.name, c.creditHours.toString(), c.lecturer, c.venue, c.colorHex)
                    .joinToString(delim) { it.replace(delim, " ") }
                    .take(490)
            },
            lectureRecords = allLectures.take(80).map { l ->
                listOf(
                    l.courseCode,
                    l.courseName,
                    l.dayOfWeek.toString(),
                    l.startTime,
                    l.endTime,
                    l.venue,
                    l.walkingMinutes.toString(),
                    l.colorHex,
                    l.lecturer,
                    l.reminderEnabled.toString(),
                    l.reminderMinutesBefore.toString(),
                    l.alarmToneId
                ).joinToString(delim) { it.replace(delim, " ") }.take(490)
            },
            assessmentRecords = allAssessments.take(80).map { a ->
                listOf(
                    a.courseCode,
                    a.courseName,
                    a.title,
                    a.type,
                    a.dueEpochMillis.toString(),
                    a.status,
                    a.examVenue,
                    a.seatNumber,
                    a.indexNumber,
                    a.priority,
                    a.reminderEnabled.toString()
                ).joinToString(delim) { it.replace(delim, " ") }.take(490)
            },
            flashcardRecords = allCards.take(100).map { f ->
                listOf(
                    f.courseCode,
                    f.deckTitle,
                    f.question.take(160),
                    f.answer.take(220),
                    f.easeFactor.toString(),
                    f.intervalDays.toString(),
                    f.repetitions.toString()
                ).joinToString(delim) { it.replace(delim, " ") }.take(490)
            },
            noteRecords = allNotes.take(60).map { n ->
                listOf(
                    n.courseCode,
                    n.title.take(80),
                    n.category,
                    n.isPinned.toString(),
                    n.content.replace("\n", "\\n").take(420)
                ).joinToString(delim) { it.replace(delim, " ") }.take(590)
            },
            gradeRecords = allGrades.take(50).map { g ->
                listOf(
                    g.courseCode,
                    g.courseName.take(90),
                    g.creditHours.toString(),
                    g.letterGrade,
                    g.gradePoint.toString(),
                    g.semester
                ).joinToString(delim) { it.replace(delim, " ") }.take(290)
            },
            expenseRecords = allExpenses.take(60).map { e ->
                listOf(
                    e.title.take(80),
                    e.category,
                    e.amount.toString(),
                    e.courseCode,
                    e.dateEpochMillis.toString()
                ).joinToString(delim) { it.replace(delim, " ") }.take(290)
            }
        )
    }

    fun restoreFromCloudBackup(
        backup: UserBackup,
        markUidRestored: String? = null,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            // 1. Restore complete UserProfile, Theme, Alarm, Budget & Widget settings
            prefs.restoreFromSettingsList(
                entries = backup.profileSettings,
                studentName = backup.studentName,
                institution = backup.institution,
                programme = backup.department
            )

            val delim = "||"
            val existingCourses = db.courseDao().getAllCoursesList()
            val codeToId = existingCourses.associate { it.code.uppercase() to it.id }.toMutableMap()
            var restoredCourses = 0
            var restoredLectures = 0
            var restoredAssessments = 0
            var restoredCards = 0
            var restoredNotes = 0
            var restoredGrades = 0

            // 2. Restore Courses
            backup.courseRecords.forEach { row ->
                val p = row.split(delim)
                val code = p.getOrNull(0)?.trim()?.uppercase().orEmpty()
                if (code.isNotBlank() && !codeToId.containsKey(code)) {
                    val cId = db.courseDao().insertCourse(
                        CourseEntity(
                            code = code,
                            name = p.getOrNull(1).orEmpty().ifBlank { code },
                            creditHours = p.getOrNull(2)?.toIntOrNull() ?: 3,
                            lecturer = p.getOrNull(3).orEmpty(),
                            venue = p.getOrNull(4).orEmpty(),
                            colorHex = p.getOrNull(5).orEmpty().ifBlank { "#0A84FF" }
                        )
                    )
                    codeToId[code] = cId
                    restoredCourses++
                }
            }

            // 3. Restore Timetable Lectures + Schedule Alarms
            val currentLectures = db.lectureDao().getAllLecturesList()
            backup.lectureRecords.forEach { row ->
                val p = row.split(delim)
                val code = p.getOrNull(0)?.trim()?.uppercase().orEmpty()
                val day = p.getOrNull(2)?.toIntOrNull() ?: 1
                val start = p.getOrNull(3).orEmpty().ifBlank { "08:30" }
                if (code.isNotBlank() && currentLectures.none { it.courseCode.equals(code, true) && it.dayOfWeek == day && it.startTime == start }) {
                    val cId = codeToId[code] ?: db.courseDao().insertCourse(
                        CourseEntity(code = code, name = p.getOrNull(1).orEmpty().ifBlank { code })
                    ).also { codeToId[code] = it }
                    persistAndScheduleLecture(
                        LectureEntity(
                            courseId = cId,
                            courseCode = code,
                            courseName = p.getOrNull(1).orEmpty().ifBlank { code },
                            dayOfWeek = day,
                            startTime = start,
                            endTime = p.getOrNull(4).orEmpty().ifBlank { "10:30" },
                            venue = p.getOrNull(5).orEmpty(),
                            walkingMinutes = p.getOrNull(6)?.toIntOrNull() ?: 10,
                            colorHex = p.getOrNull(7).orEmpty().ifBlank { "#0A84FF" },
                            lecturer = p.getOrNull(8).orEmpty(),
                            reminderEnabled = p.getOrNull(9)?.toBooleanStrictOrNull() ?: true,
                            reminderMinutesBefore = p.getOrNull(10)?.toIntOrNull() ?: 15,
                            alarmToneId = p.getOrNull(11).orEmpty()
                        )
                    )
                    restoredLectures++
                }
            }

            // 4. Restore Assessments / Exams + Schedule Reminders
            val currentAssessments = db.assessmentDao().getAllAssessmentsList()
            backup.assessmentRecords.forEach { row ->
                val p = row.split(delim)
                val code = p.getOrNull(0)?.trim()?.uppercase().orEmpty().ifBlank { "GEN" }
                val title = p.getOrNull(2)?.trim().orEmpty()
                val due = p.getOrNull(4)?.toLongOrNull() ?: (System.currentTimeMillis() + 86_400_000L)
                if (title.isNotBlank() && currentAssessments.none { it.title.equals(title, true) && it.courseCode.equals(code, true) }) {
                    val cId = codeToId[code] ?: 0L
                    val entity = AssessmentEntity(
                        courseId = cId,
                        courseCode = code,
                        courseName = p.getOrNull(1).orEmpty().ifBlank { code },
                        title = title,
                        type = p.getOrNull(3).orEmpty().ifBlank { "ASSIGNMENT" },
                        dueEpochMillis = due,
                        status = p.getOrNull(5).orEmpty().ifBlank { "PENDING" },
                        examVenue = p.getOrNull(6).orEmpty(),
                        seatNumber = p.getOrNull(7).orEmpty(),
                        indexNumber = p.getOrNull(8).orEmpty(),
                        priority = p.getOrNull(9).orEmpty().ifBlank { "HIGH" },
                        reminderEnabled = p.getOrNull(10)?.toBooleanStrictOrNull() ?: true
                    )
                    val insertedId = db.assessmentDao().insertAssessment(entity)
                    AlarmScheduler.scheduleAssessmentAlarm(getApplication(), entity.copy(id = insertedId))
                    restoredAssessments++
                }
            }

            // 5. Restore SM-2 Flashcards
            val currentCards = db.flashcardDao().getAllFlashcardsList()
            backup.flashcardRecords.forEach { row ->
                val p = row.split(delim)
                val code = p.getOrNull(0)?.trim().orEmpty().ifBlank { "General" }
                val q = p.getOrNull(2)?.trim().orEmpty()
                val a = p.getOrNull(3)?.trim().orEmpty()
                if (q.isNotBlank() && a.isNotBlank() && currentCards.none { it.question == q && it.courseCode == code }) {
                    db.flashcardDao().insertFlashcard(
                        FlashcardEntity(
                            courseCode = code,
                            deckTitle = p.getOrNull(1).orEmpty().ifBlank { "Cloud Deck" },
                            question = q,
                            answer = a,
                            easeFactor = p.getOrNull(4)?.toDoubleOrNull() ?: 2.5,
                            intervalDays = p.getOrNull(5)?.toIntOrNull() ?: 1,
                            repetitions = p.getOrNull(6)?.toIntOrNull() ?: 0
                        )
                    )
                    restoredCards++
                }
            }

            // 6. Restore Study Notes Vault
            val currentNotes = db.studyNoteDao().getAllStudyNotes().first()
            backup.noteRecords.forEach { row ->
                val p = row.split(delim)
                val code = p.getOrNull(0)?.trim().orEmpty().ifBlank { "General" }
                val title = p.getOrNull(1)?.trim().orEmpty()
                val content = p.getOrNull(4).orEmpty().replace("\\n", "\n")
                if (title.isNotBlank() && currentNotes.none { it.title.equals(title, true) && it.courseCode.equals(code, true) }) {
                    db.studyNoteDao().insertStudyNote(
                        StudyNoteEntity(
                            courseCode = code,
                            title = title,
                            category = p.getOrNull(2).orEmpty().ifBlank { "CONCEPT" },
                            isPinned = p.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
                            content = content
                        )
                    )
                    restoredNotes++
                }
            }

            // 7. Restore GPA Course Grades
            val currentGrades = db.courseGradeDao().getAllGrades().first()
            backup.gradeRecords.forEach { row ->
                val p = row.split(delim)
                val code = p.getOrNull(0)?.trim().orEmpty()
                val semester = p.getOrNull(5).orEmpty().ifBlank { "Semester 1" }
                if (code.isNotBlank() && currentGrades.none { it.courseCode.equals(code, true) && it.semester.equals(semester, true) }) {
                    db.courseGradeDao().insertGrade(
                        CourseGradeEntity(
                            courseCode = code,
                            courseName = p.getOrNull(1).orEmpty().ifBlank { code },
                            creditHours = p.getOrNull(2)?.toIntOrNull() ?: 3,
                            letterGrade = p.getOrNull(3).orEmpty().ifBlank { "A" },
                            gradePoint = p.getOrNull(4)?.toDoubleOrNull() ?: 4.0,
                            semester = semester
                        )
                    )
                    restoredGrades++
                }
            }

            // 8. Restore Academic Expenses
            val currentExpenses = db.academicExpenseDao().getAllExpenses().first()
            backup.expenseRecords.forEach { row ->
                val p = row.split(delim)
                val title = p.getOrNull(0)?.trim().orEmpty()
                val amount = p.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                val epoch = p.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()
                if (title.isNotBlank() && amount > 0.0 && currentExpenses.none { it.title == title && it.dateEpochMillis == epoch }) {
                    db.academicExpenseDao().insertExpense(
                        AcademicExpenseEntity(
                            title = title,
                            category = p.getOrNull(1).orEmpty().ifBlank { "OTHER" },
                            amount = amount,
                            courseCode = p.getOrNull(3).orEmpty().ifBlank { "General" },
                            dateEpochMillis = epoch
                        )
                    )
                }
            }

            if (!markUidRestored.isNullOrBlank()) {
                prefs.markUidAutoRestored(markUidRestored)
            }

            AuraUpNextWidgetProvider.updateAllWidgets(getApplication())
            onComplete(
                "Profile, settings & cloud vault restored! ($restoredCourses courses, $restoredLectures lectures, $restoredAssessments exams/tasks, $restoredCards cards, $restoredNotes notes)."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundscapeSynthesizer.stop()
        musicPlayer.stop()
        LectureAlarmRinger.stopPreview(getApplication())
        audioRecorder.cleanup()
    }
}
