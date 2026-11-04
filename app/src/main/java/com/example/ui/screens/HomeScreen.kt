package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AuraAcademicAdvisor
import com.example.data.local.entity.LectureEntity
import com.example.notifications.AlarmToneOption
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.AuraUserAvatar
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.AuraCircularProgressRing
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.computeLeaveByTime12Hour
import com.example.ui.components.computeLiveCountdownToLecture
import com.example.ui.components.formatTime12Hour
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.AuraVioletAccent
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AuraViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToOfficeHours: () -> Unit = onNavigateToTimetable,
    onNavigateToStudy: (Int) -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenGroupProjects: () -> Unit = onNavigateToTasks,
    onOpenManageCourses: () -> Unit = onNavigateToTimetable,
    onOpenGpaPredictor: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onOpenExportHub: () -> Unit = {},
    onOpenAlarmStudio: () -> Unit = {},
    onOpenTimetableScanner: () -> Unit = {},
    onOpenCitationManager: () -> Unit = {},
    onOpenWhiteboardScanner: () -> Unit = {},
    onOpenGlobalSearch: () -> Unit = {},
    onOpenAiAssistant: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAddLecture: () -> Unit,
    onOpenAddAssessment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val focusRecords by viewModel.focusRecords.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val nextLecture by viewModel.nextUpcomingLecture.collectAsState()
    val todayHabit by viewModel.todayAcademicHabit.collectAsState()
    val courseResources by viewModel.courseResources.collectAsState()

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importProfilePictureFromUri(uri)
        }
    }

    val todayDate = remember { LocalDate.now() }
    val todayDayOfWeek = todayDate.dayOfWeek.value
    val todayLectures = remember(lectures, todayDayOfWeek) {
        lectures.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.startTime }
    }
    val tomorrowLectures = remember(lectures, todayDayOfWeek) {
        val tomorrow = (todayDayOfWeek % 7) + 1
        lectures.filter { it.dayOfWeek == tomorrow }.sortedBy { it.startTime }
    }

    val unreadNotificationCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val totalWeeklyFocusMins = remember(focusRecords) {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
        focusRecords.filter { it.timestampEpochMillis >= weekAgo }.sumOf { it.durationMinutes }
    }
    val weeklyStudyHours = totalWeeklyFocusMins / 60f

    val pendingAssessments = remember(assessments) {
        assessments.filter { it.status != "COMPLETED" && it.status != "SUBMITTED" }
    }

    val upcomingExams = remember(assessments) {
        assessments.filter {
            it.type in listOf("MID_SEM", "FINAL_EXAM", "CLASS_TEST") &&
                    it.dueEpochMillis >= System.currentTimeMillis()
        }
    }

    val dailyBriefing = remember(userProfile.name, todayLectures, tomorrowLectures, pendingAssessments, weeklyStudyHours) {
        AuraAcademicAdvisor.generateDailyBriefing(
            studentName = userProfile.name,
            todayLectures = todayLectures,
            tomorrowLectures = tomorrowLectures,
            pendingAssessments = pendingAssessments,
            studyHoursThisWeek = weeklyStudyHours,
            studyGoalHours = userProfile.studyGoalHoursPerWeek
        )
    }

    FrostedGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("home_lazy_list"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Showcase 3D Prism Hero Card with Avatar, Momentum Progress & Alarm Studio Pill
            item {
                HeaderSection(
                    studentName = userProfile.name,
                    institution = userProfile.institution,
                    profilePicturePath = userProfile.profilePicturePath,
                    ringingAlarmsCount = lectures.count { it.reminderEnabled },
                    totalLecturesCount = lectures.size,
                    weeklyStudyHours = weeklyStudyHours,
                    targetStudyHours = userProfile.studyGoalHoursPerWeek,
                    todayDate = todayDate,
                    unreadCount = unreadNotificationCount,
                    onUploadAvatar = {
                        profilePhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenAlarmStudio = onOpenAlarmStudio,
                    onOpenGlobalSearch = onOpenGlobalSearch,
                    onOpenNotifications = onOpenNotifications,
                    onOpenAiAssistant = onOpenAiAssistant
                )
            }

            // 2. Bento Prism Academic Overview Stats (4 Cards)
            item {
                AcademicOverviewGrid(
                    lecturesTodayCount = todayLectures.size,
                    pendingAssignmentsCount = pendingAssessments.size,
                    upcomingExamsCount = upcomingExams.size,
                    loggedStudyHours = weeklyStudyHours,
                    targetStudyHours = userProfile.studyGoalHoursPerWeek,
                    onOpenLectures = onNavigateToTimetable,
                    onOpenAssignments = onNavigateToTasks,
                    onOpenExams = { onNavigateToStudy(StudyTabs.WAR_ROOM) },
                    onOpenStudy = { onNavigateToStudy(StudyTabs.FOCUS) }
                )
            }

            // 3. Spotlight Next Lecture Card
            item {
                NextLectureCard(
                    nextLecture = nextLecture,
                    defaultAlarmToneId = userProfile.defaultAlarmToneId,
                    onToggleAlarm = { lec -> viewModel.toggleLectureAlarm(lec) },
                    onViewFullTimetable = onNavigateToTimetable,
                    onAddLecture = onOpenAddLecture
                )
            }

            // 3b. Course Slides & PDFs Study Deck Spotlight Card
            item {
                HomeCourseSlidesSpotlightCard(
                    resources = courseResources,
                    onOpenCourseSlides = { courseCode ->
                        if (!courseCode.isNullOrBlank()) {
                            viewModel.setResourceCourseFilter(courseCode)
                        }
                        onNavigateToStudy(StudyTabs.COURSE_SLIDES)
                    }
                )
            }

            // 4. Daily Habits Streak & XP Strip
            item {
                val currentHabit = todayHabit ?: com.example.data.local.entity.AcademicHabitEntity(dateString = todayDate.toString())
                DailyHabitsHomeCard(
                    habit = currentHabit,
                    onToggleLectures = {
                        viewModel.toggleTodayHabit { it.copy(attendedLectures = !it.attendedLectures, xpEarned = it.xpEarned + if (!it.attendedLectures) 25 else -25) }
                    },
                    onToggleFlashcards = {
                        viewModel.toggleTodayHabit { it.copy(flashcardsReviewed = !it.flashcardsReviewed, xpEarned = it.xpEarned + if (!it.flashcardsReviewed) 25 else -25) }
                    },
                    onToggleFocus = {
                        viewModel.toggleTodayHabit { it.copy(deepFocusCompleted = !it.deepFocusCompleted, xpEarned = it.xpEarned + if (!it.deepFocusCompleted) 25 else -25) }
                    },
                    onToggleSyllabus = {
                        viewModel.toggleTodayHabit { it.copy(syllabusProgressMade = !it.syllabusProgressMade, xpEarned = it.xpEarned + if (!it.syllabusProgressMade) 25 else -25) }
                    },
                    onOpenHabits = { onNavigateToStudy(StudyTabs.HABITS) }
                )
            }

            // 5. Exam Countdown Hero (if exam upcoming)
            if (upcomingExams.isNotEmpty()) {
                val nearestExam = upcomingExams.minByOrNull { it.dueEpochMillis }
                if (nearestExam != null) {
                    item {
                        ExamCountdownQuickCard(
                            exam = nearestExam,
                            onOpenWarRoom = { onNavigateToStudy(StudyTabs.WAR_ROOM) }
                        )
                    }
                }
            }

            // 6. "Your AURA Briefing" Sanctuary Card
            item {
                AuraBriefingCard(
                    briefing = dailyBriefing,
                    onAskAiClick = onOpenAiAssistant
                )
            }

            // 7. VisionOS Bento Quick-Launch Carousel
            item {
                QuickActionsSection(
                    onAddLecture = onOpenAddLecture,
                    onOpenTimetableScanner = onOpenTimetableScanner,
                    onOpenWhiteboardScanner = onOpenWhiteboardScanner,
                    onOpenCitationManager = onOpenCitationManager,
                    onOpenCloudRooms = { onNavigateToStudy(StudyTabs.CLOUD_ROOMS) },
                    onOpenAlarmStudio = onOpenAlarmStudio,
                    onOpenCourseSlides = { onNavigateToStudy(StudyTabs.COURSE_SLIDES) },
                    onOpenManageCourses = onOpenManageCourses,
                    onOpenPracticeQuiz = { onNavigateToStudy(StudyTabs.PRACTICE_QUIZ) },
                    onOpenNotesVault = { onNavigateToStudy(StudyTabs.NOTES_VAULT) },
                    onOpenGroupProjects = onOpenGroupProjects,
                    onOpenGpaPredictor = onOpenGpaPredictor,
                    onOpenBudget = onOpenBudget,
                    onOpenExportHub = onOpenExportHub,
                    onOpenOfficeHours = onNavigateToOfficeHours,
                    onTestAlarm = { viewModel.triggerTestAlarm() },
                    onAddAssessment = onOpenAddAssessment,
                    onStartFocus = { onNavigateToStudy(StudyTabs.FOCUS) },
                    onOpenCalendar = onNavigateToTimetable
                )
            }

            // 8. Today's Chronological Schedule
            item {
                SectionHeader(
                    title = "Today's Schedule",
                    subtitle = todayDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)),
                    actionLabel = "Timetable",
                    onActionClick = onNavigateToTimetable,
                    testTag = "todays_schedule_header"
                )
            }

            if (todayLectures.isEmpty()) {
                item {
                    EmptyTodayScheduleCard(onAddLecture = onOpenAddLecture)
                }
            } else {
                items(todayLectures, key = { it.id }) { lecture ->
                    val todayEpoch = todayDate.toEpochDay()
                    val loggedTodayStatus = attendanceRecords.lastOrNull {
                        it.courseId == lecture.courseId && it.dateEpochDay == todayEpoch
                    }?.status
                    TodayScheduleItem(
                        lecture = lecture,
                        loggedTodayStatus = loggedTodayStatus,
                        onToggleAlarm = { viewModel.toggleLectureAlarm(lecture) },
                        onMarkPresent = {
                            viewModel.recordAttendance(lecture.courseId, lecture.courseCode, "PRESENT", lectureId = lecture.id)
                        },
                        onMarkAbsent = {
                            viewModel.recordAttendance(lecture.courseId, lecture.courseCode, "ABSENT", lectureId = lecture.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    studentName: String,
    institution: String,
    profilePicturePath: String,
    ringingAlarmsCount: Int,
    totalLecturesCount: Int,
    weeklyStudyHours: Float,
    targetStudyHours: Int,
    todayDate: LocalDate,
    unreadCount: Int,
    onUploadAvatar: () -> Unit,
    onOpenAlarmStudio: () -> Unit,
    onOpenGlobalSearch: () -> Unit = {},
    onOpenNotifications: () -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    val formattedDate = todayDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.US))
    val auraColors = LocalAuraColors.current
    val greetingBadge = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "MORNING COMMAND"
            hour < 17 -> "AFTERNOON FLOW"
            else -> "EVENING SANCTUARY"
        }
    }
    val momentumProgress = remember(weeklyStudyHours, targetStudyHours) {
        if (targetStudyHours > 0) (weeklyStudyHours / targetStudyHours.toFloat()).coerceIn(0f, 1f) else 0f
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_header"),
        cornerRadius = 28.dp,
        accentGlow = auraColors.primaryAccent
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Bespoke 3D Iridescent Liquid Glass Sculpture Hero Backdrop
            Image(
                painter = painterResource(id = R.drawable.aura_prism_hero_1793569866167),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (auraColors.isDark) 0.40f else 0.16f)
            )

            // Smooth protective gradient scrim for crisp legibility
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (auraColors.isDark) {
                                listOf(
                                    Color(0x99081124),
                                    Color(0xCC0A152C),
                                    Color(0xEB091224)
                                )
                            } else {
                                listOf(
                                    Color(0xCCFFFFFF),
                                    Color(0xE8F2F8FF)
                                )
                            }
                        )
                    )
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraUserAvatar(
                        imagePath = profilePicturePath,
                        studentName = studentName,
                        size = 48.dp,
                        showCameraBadge = true,
                        onClick = onUploadAvatar,
                        testTag = "home_profile_avatar"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, $studentName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = auraColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$institution • $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = auraColors.secondaryAccent,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Universal Campus Search Shortcut
                    AuraGlassIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search Slides, Courses & Tasks",
                        onClick = onOpenGlobalSearch,
                        size = 44.dp,
                        iconSize = 19.dp,
                        tint = auraColors.secondaryAccent,
                        testTag = "home_global_search_button"
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // AI Assistant Liquid Glass Shortcut
                    AuraGlassIconButton(
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = "AURA AI Assistant",
                        onClick = onOpenAiAssistant,
                        size = 44.dp,
                        iconSize = 19.dp,
                        tint = auraColors.aiAccent,
                        testTag = "ai_assistant_header_button"
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Notification Bell Liquid Glass Button with Badge
                    Box {
                        AuraGlassIconButton(
                            icon = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            onClick = onOpenNotifications,
                            size = 44.dp,
                            iconSize = 19.dp,
                            tint = auraColors.textPrimary,
                            testTag = "notification_bell_button"
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(19.dp)
                                    .clip(CircleShape)
                                    .background(auraColors.danger)
                                    .border(1.2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontFamily = AuraMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compact Side-by-Side Alarm Studio Pill & Weekly Momentum Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1.15f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x320A84FF),
                                        Color(0x24818CF8)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x525CE1FF), RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenAlarmStudio)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_alarm_studio_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = auraColors.secondaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (totalLecturesCount == 0) {
                                "Alarm Studio • Tune"
                            } else {
                                "$ringingAlarmsCount/$totalLecturesCount Alarms • Tune"
                            },
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = auraColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(0.95f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (auraColors.isDark) Color(0x24FFFFFF) else Color(0x90FFFFFF))
                            .border(1.dp, Color(0x36FFFFFF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuraCircularProgressRing(
                            progress = momentumProgress,
                            centerLabel = "${(momentumProgress * 100).toInt()}%",
                            ringSize = 32.dp,
                            strokeWidth = 3.5.dp,
                            accentColor = AuraMintAccent,
                            secondaryColor = auraColors.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greetingBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = auraColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", weeklyStudyHours)}/${targetStudyHours}h Focus",
                                fontFamily = AuraMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.secondaryAccent,
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
private fun HomeCourseSlidesSpotlightCard(
    resources: List<com.example.data.local.entity.CourseResourceEntity>,
    onOpenCourseSlides: (String?) -> Unit
) {
    val auraColors = LocalAuraColors.current
    val latestResource = remember(resources) {
        resources.maxByOrNull { it.lastOpenedEpochMillis }
    }
    val completedCount = remember(resources) { resources.count { it.isCompleted } }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCourseSlides(latestResource?.courseCode) }
            .testTag("home_slides_spotlight_card"),
        cornerRadius = 22.dp,
        borderColor = auraColors.secondaryAccent.copy(alpha = 0.45f),
        accentGlow = auraColors.secondaryAccent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                auraColors.primaryAccent.copy(alpha = 0.28f),
                                auraColors.secondaryAccent.copy(alpha = 0.20f)
                            )
                        )
                    )
                    .border(1.dp, auraColors.secondaryAccent.copy(alpha = 0.50f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (latestResource?.fileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Slideshow,
                    contentDescription = null,
                    tint = auraColors.secondaryAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (latestResource == null) {
                    Text(
                        text = "Course Slides & PDF Reader",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Upload PDFs, PPTX, DOCX or handouts for offline study & AI flashcards",
                        fontSize = 11.sp,
                        color = auraColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    val progress = if (latestResource.totalPages > 0) {
                        ((latestResource.lastReadPage + 1).coerceAtMost(latestResource.totalPages).toFloat() /
                            latestResource.totalPages.toFloat()).coerceIn(0f, 1f)
                    } else 1f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AuraBadge(
                            text = "${latestResource.courseCode} • ${latestResource.fileType}",
                            backgroundColor = auraColors.primaryAccent.copy(alpha = 0.22f),
                            textColor = auraColors.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$completedCount/${resources.size} mastered",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraMintAccent,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = latestResource.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = auraColors.secondaryAccent,
                        trackColor = Color(0x24FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            AuraGlassButton(
                text = if (latestResource == null) "Upload" else "Resume",
                isPrimary = true,
                cornerRadius = 12.dp,
                onClick = { onOpenCourseSlides(latestResource?.courseCode) },
                testTag = "home_open_slides_spotlight_button"
            )
        }
    }
}

@Composable
private fun AcademicOverviewGrid(
    lecturesTodayCount: Int,
    pendingAssignmentsCount: Int,
    upcomingExamsCount: Int,
    loggedStudyHours: Float,
    targetStudyHours: Int,
    onOpenLectures: () -> Unit = {},
    onOpenAssignments: () -> Unit = {},
    onOpenExams: () -> Unit = {},
    onOpenStudy: () -> Unit = {}
) {
    val auraColors = LocalAuraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("academic_overview_grid"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Lectures Today",
                value = "$lecturesTodayCount",
                subtitle = "Active sessions",
                icon = Icons.Default.School,
                accentColor = auraColors.primaryAccent,
                accentBrush = Brush.linearGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF0A84FF))
                ),
                onClick = onOpenLectures,
                modifier = Modifier.weight(1f),
                testTag = "stat_lectures_today"
            )
            StatCard(
                title = "Assignments",
                value = "$pendingAssignmentsCount",
                subtitle = "Pending tasks",
                icon = Icons.AutoMirrored.Filled.Assignment,
                accentColor = auraColors.warning,
                accentBrush = Brush.linearGradient(
                    listOf(Color(0xFF60A5FA), Color(0xFF4F46E5))
                ),
                onClick = onOpenAssignments,
                modifier = Modifier.weight(1f),
                testTag = "stat_pending_assignments"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Examinations",
                value = "$upcomingExamsCount",
                subtitle = "This semester",
                icon = Icons.Default.Alarm,
                accentColor = auraColors.warning,
                accentBrush = Brush.linearGradient(
                    listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
                ),
                onClick = onOpenExams,
                modifier = Modifier.weight(1f),
                testTag = "stat_upcoming_exams"
            )
            StatCard(
                title = "Study Hours",
                value = "${String.format(Locale.US, "%.1f", loggedStudyHours)}h",
                subtitle = "Goal: ${targetStudyHours}h / wk",
                icon = Icons.Default.Timer,
                accentColor = AuraMintAccent,
                accentBrush = Brush.linearGradient(
                    listOf(AuraMintAccent, Color(0xFF0EA5E9))
                ),
                onClick = onOpenStudy,
                modifier = Modifier.weight(1f),
                testTag = "stat_study_hours"
            )
        }
    }
}

@Composable
private fun NextLectureCard(
    nextLecture: LectureEntity?,
    defaultAlarmToneId: String = "AURORA_CHIME",
    onToggleAlarm: (LectureEntity) -> Unit = {},
    onViewFullTimetable: () -> Unit,
    onAddLecture: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    var liveNow by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(nextLecture?.id, nextLecture?.startTime, nextLecture?.dayOfWeek) {
        if (nextLecture != null) {
            while (true) {
                delay(1000L)
                liveNow = LocalDateTime.now()
            }
        }
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("next_lecture_card"),
        cornerRadius = 24.dp,
        accentGlow = auraColors.secondaryAccent,
        onClick = if (nextLecture != null) onViewFullTimetable else onAddLecture
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .testTag("next_lecture_badge")
                        .clip(RoundedCornerShape(50))
                        .background(auraColors.primaryAccent.copy(alpha = 0.22f))
                        .border(1.dp, auraColors.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AuraMintAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEXT UPCOMING LECTURE",
                        fontFamily = OutfitFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = auraColors.secondaryAccent
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (nextLecture != null) {
                    val dayName = java.time.DayOfWeek.of(nextLecture.dayOfWeek).name.take(3)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x24FFFFFF))
                            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$dayName • ${formatTime12Hour(nextLecture.startTime)}",
                            fontFamily = AuraMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (nextLecture != null) {
                Text(
                    text = nextLecture.courseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = auraColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${nextLecture.courseCode} • ${nextLecture.lecturer} (${formatTime12Hour(nextLecture.startTime)} – ${formatTime12Hour(nextLecture.endTime)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = auraColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Live Ticking Countdown Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(auraColors.primaryAccent.copy(alpha = 0.18f))
                        .border(1.dp, auraColors.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("next_lecture_live_countdown"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Starts in ${computeLiveCountdownToLecture(nextLecture, liveNow)}",
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = auraColors.secondaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1F10B981))
                        .border(1.dp, AuraMintAccent.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = AuraMintAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Leave by ${computeLeaveByTime12Hour(nextLecture.startTime, nextLecture.walkingMinutes)} • ${nextLecture.walkingMinutes}m campus walk",
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraMintAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1EFFFFFF))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = auraColors.secondaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = nextLecture.venue,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = auraColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val toneName = AlarmToneOption.fromId(
                        nextLecture.alarmToneId.ifBlank { defaultAlarmToneId }
                    ).displayName

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (nextLecture.reminderEnabled) auraColors.warning.copy(alpha = 0.20f)
                                else Color(0x18FFFFFF)
                            )
                            .border(
                                1.dp,
                                if (nextLecture.reminderEnabled) auraColors.warning.copy(alpha = 0.60f)
                                else Color(0x28FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleAlarm(nextLecture) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (nextLecture.reminderEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                            contentDescription = "Toggle Next Lecture Alarm",
                            tint = if (nextLecture.reminderEnabled) auraColors.warning else auraColors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (!nextLecture.reminderEnabled) {
                                "Muted"
                            } else if (nextLecture.reminderMinutesBefore == 0) {
                                "Rings • ${toneName.split(" ").first()}"
                            } else {
                                "${nextLecture.reminderMinutesBefore}m • ${toneName.split(" ").first()}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (nextLecture.reminderEnabled) auraColors.warning else auraColors.textMuted
                        )
                    }
                }
            } else {
                Text(
                    text = "No Lectures Scheduled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = auraColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your timetable is clean. Tap here to add your course lectures with custom AM/PM alarms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = auraColors.secondaryAccent
                )
            }
        }
    }
}

@Composable
private fun AuraBriefingCard(
    briefing: AuraAcademicAdvisor.DailyBriefing,
    onAskAiClick: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aura_briefing_card"),
        cornerRadius = 24.dp,
        accentGlow = auraColors.aiAccent
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.study_sanctuary_nebula_1793569866560),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (auraColors.isDark) 0.22f else 0.08f)
            )

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(6.dp, CircleShape, spotColor = auraColors.aiAccent)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(auraColors.secondaryAccent, AuraVioletAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your AURA Briefing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = "Personalized daily intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = auraColors.aiAccent
                        )
                    }
                    AuraBadge(
                        text = "AI INSIGHTS",
                        backgroundColor = Color(0x285CE1FF),
                        textColor = auraColors.aiAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = briefing.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = auraColors.textPrimary,
                    lineHeight = 21.sp
                )

                if (briefing.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        briefing.recommendations.take(2).forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1EFFFFFF))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(auraColors.secondaryAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = auraColors.textSecondary,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AuraGlassButton(
                    text = "Consult AURA AI Assistant",
                    icon = Icons.Default.AutoAwesome,
                    isPrimary = true,
                    onClick = onAskAiClick,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "ask_aura_ai_button"
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onAddLecture: () -> Unit,
    onOpenTimetableScanner: () -> Unit = {},
    onOpenWhiteboardScanner: () -> Unit = {},
    onOpenCitationManager: () -> Unit = {},
    onOpenCloudRooms: () -> Unit = {},
    onOpenAlarmStudio: () -> Unit = {},
    onOpenCourseSlides: () -> Unit,
    onOpenManageCourses: () -> Unit,
    onOpenPracticeQuiz: () -> Unit,
    onOpenNotesVault: () -> Unit,
    onOpenGroupProjects: () -> Unit,
    onOpenGpaPredictor: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenExportHub: () -> Unit,
    onOpenOfficeHours: () -> Unit,
    onTestAlarm: () -> Unit,
    onAddAssessment: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    Column(modifier = Modifier.testTag("quick_actions_section")) {
        SectionHeader(
            title = "Command Center",
            subtitle = "Instant access to whiteboard camera, APA/IEEE citations, AI scanner & focus"
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier.testTag("quick_actions_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                QuickActionButton(
                    label = "Add Lecture",
                    icon = Icons.Default.Class,
                    color = auraColors.primaryAccent,
                    isPrimary = true,
                    onClick = onAddLecture,
                    testTag = "quick_action_add_lecture"
                )
            }
            item {
                QuickActionButton(
                    label = "Scan Board",
                    icon = Icons.Default.CameraAlt,
                    color = auraColors.secondaryAccent,
                    onClick = onOpenWhiteboardScanner,
                    testTag = "quick_action_whiteboard_scanner"
                )
            }
            item {
                QuickActionButton(
                    label = "Citations",
                    icon = Icons.Default.FormatQuote,
                    color = AuraVioletAccent,
                    onClick = onOpenCitationManager,
                    testTag = "quick_action_citations"
                )
            }
            item {
                QuickActionButton(
                    label = "AI Scan Schedule",
                    icon = Icons.Default.AutoAwesome,
                    color = auraColors.aiAccent,
                    onClick = onOpenTimetableScanner,
                    testTag = "quick_ai_scan_schedule"
                )
            }
            item {
                QuickActionButton(
                    label = "Cloud Rooms",
                    icon = Icons.Default.CloudSync,
                    color = AuraMintAccent,
                    onClick = onOpenCloudRooms,
                    testTag = "quick_cloud_rooms"
                )
            }
            item {
                QuickActionButton(
                    label = "Alarm Studio",
                    icon = Icons.Default.MusicNote,
                    color = auraColors.warning,
                    onClick = onOpenAlarmStudio,
                    testTag = "quick_action_alarm_studio"
                )
            }
            item {
                QuickActionButton(
                    label = "Course Slides",
                    icon = Icons.Default.Slideshow,
                    color = auraColors.secondaryAccent,
                    onClick = onOpenCourseSlides,
                    testTag = "quick_action_course_slides"
                )
            }
            item {
                QuickActionButton(
                    label = "Manage Courses",
                    icon = Icons.Default.School,
                    color = auraColors.primaryAccent,
                    onClick = onOpenManageCourses,
                    testTag = "quick_action_manage_courses"
                )
            }
            item {
                QuickActionButton(
                    label = "Practice Quiz",
                    icon = Icons.Default.Quiz,
                    color = AuraVioletAccent,
                    onClick = onOpenPracticeQuiz,
                    testTag = "quick_action_practice_quiz"
                )
            }
            item {
                QuickActionButton(
                    label = "Notes & Scan",
                    icon = Icons.Default.Functions,
                    color = auraColors.secondaryAccent,
                    onClick = onOpenNotesVault,
                    testTag = "quick_action_notes_vault"
                )
            }
            item {
                QuickActionButton(
                    label = "Group Projects",
                    icon = Icons.Default.Groups,
                    color = auraColors.primaryAccent,
                    onClick = onOpenGroupProjects,
                    testTag = "quick_action_group_projects"
                )
            }
            item {
                QuickActionButton(
                    label = "GPA & Exam Calc",
                    icon = Icons.Default.Calculate,
                    color = auraColors.warning,
                    onClick = onOpenGpaPredictor,
                    testTag = "quick_action_gpa_predictor"
                )
            }
            item {
                QuickActionButton(
                    label = "Campus Budget",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = AuraMintAccent,
                    onClick = onOpenBudget,
                    testTag = "quick_action_campus_budget"
                )
            }
            item {
                QuickActionButton(
                    label = "Export PDF/.ICS",
                    icon = Icons.Default.PictureAsPdf,
                    color = auraColors.secondaryAccent,
                    onClick = onOpenExportHub,
                    testTag = "quick_action_export_hub"
                )
            }
            item {
                QuickActionButton(
                    label = "Office Hours",
                    icon = Icons.Default.RecordVoiceOver,
                    color = AuraVioletAccent,
                    onClick = onOpenOfficeHours,
                    testTag = "quick_action_office_hours"
                )
            }
            item {
                QuickActionButton(
                    label = "Test Alarm",
                    icon = Icons.Default.NotificationsActive,
                    color = auraColors.warning,
                    onClick = onTestAlarm,
                    testTag = "quick_action_test_alarm"
                )
            }
            item {
                QuickActionButton(
                    label = "Add Assessment",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    color = auraColors.warning,
                    onClick = onAddAssessment,
                    testTag = "quick_action_add_assessment"
                )
            }
            item {
                QuickActionButton(
                    label = "Focus Session",
                    icon = Icons.Default.Timer,
                    color = AuraMintAccent,
                    onClick = onStartFocus,
                    testTag = "quick_action_start_focus"
                )
            }
            item {
                QuickActionButton(
                    label = "Calendar View",
                    icon = Icons.Default.CalendarToday,
                    color = auraColors.primaryAccent,
                    onClick = onOpenCalendar,
                    testTag = "quick_action_open_calendar"
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    AuraCard(
        modifier = Modifier
            .width(108.dp)
            .height(92.dp),
        cornerRadius = 22.dp,
        accentGlow = if (isPrimary) LocalAuraColors.current.primaryAccent else color,
        backgroundColor = if (isPrimary) Color(0xE00A84FF) else null,
        onClick = onClick,
        testTag = testTag
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPrimary) Color.White.copy(alpha = 0.24f)
                        else color.copy(alpha = 0.20f)
                    )
                    .border(
                        1.dp,
                        if (isPrimary) Color.White.copy(alpha = 0.65f) else color.copy(alpha = 0.45f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) Color.White else LocalAuraColors.current.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodayScheduleItem(
    lecture: LectureEntity,
    loggedTodayStatus: String? = null,
    onToggleAlarm: () -> Unit = {},
    onMarkPresent: () -> Unit = {},
    onMarkAbsent: () -> Unit = {}
) {
    val auraColors = LocalAuraColors.current
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_schedule_item_${lecture.id}"),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(78.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = formatTime12Hour(lecture.startTime),
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.secondaryAccent
                    )
                    Text(
                        text = formatTime12Hour(lecture.endTime),
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 10.sp,
                        color = auraColors.textMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(
                                listOf(auraColors.secondaryAccent, auraColors.primaryAccent)
                            )
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lecture.courseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${lecture.courseCode} • ${lecture.venue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary
                    )
                    Text(
                        text = "Leave by ${computeLeaveByTime12Hour(lecture.startTime, lecture.walkingMinutes)} (${lecture.walkingMinutes}m walk)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraMintAccent
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (lecture.reminderEnabled) auraColors.warning.copy(alpha = 0.20f)
                            else Color(0x16FFFFFF)
                        )
                        .border(
                            1.dp,
                            if (lecture.reminderEnabled) auraColors.warning.copy(alpha = 0.50f) else Color(0x24FFFFFF),
                            CircleShape
                        )
                        .clickable(onClick = onToggleAlarm)
                        .testTag("home_toggle_lecture_alarm_${lecture.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lecture.reminderEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                        contentDescription = if (lecture.reminderEnabled) "Mute Lecture Alarm" else "Enable Lecture Alarm",
                        tint = if (lecture.reminderEnabled) auraColors.warning else auraColors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (loggedTodayStatus != null) {
                    AuraBadge(
                        text = loggedTodayStatus,
                        textColor = if (loggedTodayStatus == "ABSENT") auraColors.danger else auraColors.success
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(auraColors.success.copy(alpha = 0.20f))
                                .border(1.dp, auraColors.success.copy(alpha = 0.60f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onMarkPresent)
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Present", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = auraColors.success)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(auraColors.danger.copy(alpha = 0.16f))
                                .border(1.dp, auraColors.danger.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onMarkAbsent)
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Absent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = auraColors.danger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayScheduleCard(onAddLecture: () -> Unit) {
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_schedule_card"),
        cornerRadius = 22.dp,
        accentGlow = AuraMintAccent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AuraMintAccent.copy(alpha = 0.20f))
                    .border(1.dp, AuraMintAccent.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AuraMintAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Lectures Scheduled for Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your schedule is clear. Use this window for deep focus sessions or tap below to add a lecture.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAuraColors.current.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            AuraGlassButton(
                text = "Schedule a Lecture",
                icon = Icons.Default.Add,
                isPrimary = true,
                onClick = onAddLecture,
                testTag = "schedule_lecture_empty_button"
            )
        }
    }
}

@Composable
fun DailyHabitsHomeCard(
    habit: com.example.data.local.entity.AcademicHabitEntity,
    onToggleLectures: () -> Unit,
    onToggleFlashcards: () -> Unit,
    onToggleFocus: () -> Unit,
    onToggleSyllabus: () -> Unit,
    onOpenHabits: () -> Unit
) {
    val completedCount = listOf(habit.attendedLectures, habit.flashcardsReviewed, habit.deepFocusCompleted, habit.syllabusProgressMade).count { it }
    val progress = completedCount / 4f

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_habits_home_card"),
        cornerRadius = 22.dp,
        accentGlow = AuraVioletAccent
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenHabits)) {
                    Text(
                        text = "Daily Academic Habits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = "Complete all 4 pillars to earn +100 XP today",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = LocalAuraColors.current.textSecondary
                    )
                }
                AuraBadge(
                    text = "${habit.xpEarned} XP • $completedCount/4",
                    backgroundColor = Color(0x26818CF8),
                    textColor = LocalAuraColors.current.secondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.03f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    LocalAuraColors.current.secondaryAccent,
                                    LocalAuraColors.current.primaryAccent,
                                    AuraVioletAccent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitChipPill(
                    label = "Lectures",
                    isDone = habit.attendedLectures,
                    onToggle = onToggleLectures,
                    modifier = Modifier.weight(1f)
                )
                HabitChipPill(
                    label = "Flashcards",
                    isDone = habit.flashcardsReviewed,
                    onToggle = onToggleFlashcards,
                    modifier = Modifier.weight(1f)
                )
                HabitChipPill(
                    label = "Deep Focus",
                    isDone = habit.deepFocusCompleted,
                    onToggle = onToggleFocus,
                    modifier = Modifier.weight(1f)
                )
                HabitChipPill(
                    label = "Syllabus",
                    isDone = habit.syllabusProgressMade,
                    onToggle = onToggleSyllabus,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HabitChipPill(
    label: String,
    isDone: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (isDone) {
        Brush.linearGradient(
            listOf(Color(0xF038BDF8), Color(0xF20A84FF), Color(0xE84F46E5))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0x24FFFFFF), Color(0x14DCEBFF))
        )
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(
                1.dp,
                if (isDone) Color(0xBBFFFFFF) else Color(0x34FFFFFF),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isDone) Icons.Default.Check else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isDone) Color.White else LocalAuraColors.current.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontSize = 11.sp,
                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Medium,
                color = if (isDone) Color.White else LocalAuraColors.current.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ExamCountdownQuickCard(
    exam: com.example.data.local.entity.AssessmentEntity,
    onOpenWarRoom: () -> Unit
) {
    val diffMillis = (exam.dueEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val days = diffMillis / (24 * 3600 * 1000L)
    val hours = (diffMillis % (24 * 3600 * 1000L)) / (3600 * 1000L)

    val warningAccent = LocalAuraColors.current.warning

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_quick_countdown_card"),
        cornerRadius = 22.dp,
        borderColor = warningAccent.copy(alpha = 0.65f),
        accentGlow = warningAccent,
        onClick = onOpenWarRoom
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                warningAccent.copy(alpha = 0.32f),
                                Color(0x332563EB)
                            )
                        )
                    )
                    .border(1.dp, warningAccent.copy(alpha = 0.70f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${days}d",
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningAccent
                    )
                    Text(
                        text = "${hours}h",
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningAccent.copy(alpha = 0.90f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraBadge(
                        text = exam.courseCode,
                        backgroundColor = warningAccent.copy(alpha = 0.22f),
                        textColor = warningAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Exam Approaching",
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningAccent
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
                Text(
                    text = "Tap to enter Exam War Room & Revision Roadmap",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
