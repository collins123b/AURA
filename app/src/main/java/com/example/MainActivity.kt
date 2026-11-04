package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.notifications.ActiveAlarmPayload
import com.example.notifications.NotificationHelper
import com.example.ui.AuraViewModel
import com.example.ui.TimetableViewMode
import com.example.ui.dialogs.AddAssessmentDialog
import com.example.ui.dialogs.AddLectureDialog
import com.example.ui.dialogs.AlarmScreenDialog
import com.example.ui.dialogs.CampusBudgetDialog
import com.example.ui.dialogs.CourseManagerDialog
import com.example.ui.dialogs.GlobalSearchDialog
import com.example.ui.dialogs.GpaCalculatorDialog
import com.example.ui.dialogs.JsonImportExportDialog
import com.example.ui.dialogs.LectureAlarmManagerDialog
import com.example.ui.dialogs.NotificationCenterSheet
import com.example.ui.dialogs.TimetableScannerDialog
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StrictFocusLockoutOverlay
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.StudyTabs
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TimetableScreen
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.performAuraHaptic
import com.example.ui.dialogs.CitationManagerDialog
import com.example.ui.dialogs.SlideScannerDialog
import com.example.ui.theme.AuraTheme
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import com.example.widget.AuraUpNextWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow

enum class AppDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    TIMETABLE("Timetable", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
    STUDY("Study", Icons.Filled.Timer, Icons.Outlined.Timer),
    TASKS("Tasks", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

enum class NavigationScreen {
    MAIN,
    AI_ASSISTANT
}

class MainActivity : ComponentActivity() {

    private val intentAlarmFlow = MutableStateFlow<ActiveAlarmPayload?>(null)
    private val widgetDestinationFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        extractAlarmFromIntent(intent)

        setContent {
            val viewModel: AuraViewModel = viewModel()
            val userProfile by viewModel.userProfile.collectAsState()
            val activeRingingAlarm by viewModel.activeRingingAlarm.collectAsState()
            val intentAlarm by intentAlarmFlow.collectAsState()
            val widgetDest by widgetDestinationFlow.collectAsState()
            val isStrictLockout by viewModel.isStrictLockoutActive.collectAsState()

            AuraTheme(
                darkTheme = userProfile.isDarkTheme,
                accentPreset = userProfile.accentPreset
            ) {
                // Request Notification Permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var currentScreen by remember { mutableStateOf(NavigationScreen.MAIN) }
                var currentTab by remember { mutableStateOf(AppDestination.HOME) }
                var requestedStudyTab by remember { mutableIntStateOf(0) }
                var requestedTasksSection by remember { mutableIntStateOf(0) }
                var showNotificationCenter by remember { mutableStateOf(false) }
                var showAddLectureDialog by remember { mutableStateOf(false) }
                var showAddAssessmentDialog by remember { mutableStateOf(false) }
                var showCourseManagerDialog by remember { mutableStateOf(false) }
                var showGpaPredictorDialog by remember { mutableStateOf(false) }
                var showBudgetDialog by remember { mutableStateOf(false) }
                var showExportHubDialog by remember { mutableStateOf(false) }
                var showAlarmStudioDialog by remember { mutableStateOf(false) }
                var showTimetableScannerDialog by remember { mutableStateOf(false) }
                var showCitationDialog by remember { mutableStateOf(false) }
                var showWhiteboardScannerDialog by remember { mutableStateOf(false) }
                var showGlobalSearchDialog by remember { mutableStateOf(false) }

                LaunchedEffect(widgetDest) {
                    val dest = widgetDest ?: return@LaunchedEffect
                    when (dest) {
                        AuraUpNextWidgetProvider.DEST_TIMETABLE -> {
                            currentScreen = NavigationScreen.MAIN
                            viewModel.setTimetableViewMode(TimetableViewMode.DAILY)
                            currentTab = AppDestination.TIMETABLE
                        }
                        AuraUpNextWidgetProvider.DEST_TIMETABLE_WEEK -> {
                            currentScreen = NavigationScreen.MAIN
                            viewModel.setTimetableViewMode(TimetableViewMode.WEEKLY)
                            currentTab = AppDestination.TIMETABLE
                        }
                        AuraUpNextWidgetProvider.DEST_ADD_LECTURE -> {
                            currentScreen = NavigationScreen.MAIN
                            currentTab = AppDestination.TIMETABLE
                            showAddLectureDialog = true
                        }
                        AuraUpNextWidgetProvider.DEST_FOCUS -> {
                            currentScreen = NavigationScreen.MAIN
                            requestedStudyTab = StudyTabs.FOCUS
                            currentTab = AppDestination.STUDY
                        }
                        AuraUpNextWidgetProvider.DEST_SLIDES -> {
                            currentScreen = NavigationScreen.MAIN
                            requestedStudyTab = StudyTabs.COURSE_SLIDES
                            currentTab = AppDestination.STUDY
                        }
                        AuraUpNextWidgetProvider.DEST_FLASHCARDS -> {
                            currentScreen = NavigationScreen.MAIN
                            requestedStudyTab = StudyTabs.FLASHCARDS
                            currentTab = AppDestination.STUDY
                        }
                        AuraUpNextWidgetProvider.DEST_PRACTICE_QUIZ -> {
                            currentScreen = NavigationScreen.MAIN
                            requestedStudyTab = StudyTabs.PRACTICE_QUIZ
                            currentTab = AppDestination.STUDY
                        }
                        AuraUpNextWidgetProvider.DEST_WAR_ROOM -> {
                            currentScreen = NavigationScreen.MAIN
                            requestedStudyTab = StudyTabs.WAR_ROOM
                            currentTab = AppDestination.STUDY
                        }
                        AuraUpNextWidgetProvider.DEST_TASKS -> {
                            currentScreen = NavigationScreen.MAIN
                            currentTab = AppDestination.TASKS
                        }
                        AuraUpNextWidgetProvider.DEST_AI_ADVISOR -> {
                            currentScreen = NavigationScreen.AI_ASSISTANT
                        }
                        AuraUpNextWidgetProvider.DEST_HOME -> {
                            currentScreen = NavigationScreen.MAIN
                            currentTab = AppDestination.HOME
                        }
                    }
                    widgetDestinationFlow.value = null
                }

                // Top-Level Screen Back Handling
                if (!isStrictLockout) {
                    if (currentScreen == NavigationScreen.AI_ASSISTANT) {
                        BackHandler {
                            currentScreen = NavigationScreen.MAIN
                        }
                    } else if (currentTab != AppDestination.HOME) {
                        BackHandler {
                            currentTab = AppDestination.HOME
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = LocalAuraColors.current.bgPrimary,
                        bottomBar = {
                            if (currentScreen == NavigationScreen.MAIN && !isStrictLockout) {
                                AuraBottomNavigationBar(
                                    currentTab = currentTab,
                                    onSelectTab = { currentTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    NavigationScreen.AI_ASSISTANT -> {
                                        AiAssistantScreen(
                                            viewModel = viewModel,
                                            onBack = { currentScreen = NavigationScreen.MAIN }
                                        )
                                    }
                                    NavigationScreen.MAIN -> {
                                        AnimatedContent(
                                            targetState = currentTab,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "tab_transition"
                                        ) { tab ->
                                            when (tab) {
                                                AppDestination.HOME -> HomeScreen(
                                                    viewModel = viewModel,
                                                    onNavigateToTimetable = {
                                                        viewModel.setTimetableViewMode(TimetableViewMode.DAILY)
                                                        currentTab = AppDestination.TIMETABLE
                                                    },
                                                    onNavigateToOfficeHours = {
                                                        viewModel.setTimetableViewMode(TimetableViewMode.OFFICE_HOURS)
                                                        currentTab = AppDestination.TIMETABLE
                                                    },
                                                    onNavigateToStudy = { targetTab ->
                                                        requestedStudyTab = targetTab
                                                        currentTab = AppDestination.STUDY
                                                    },
                                                    onNavigateToTasks = {
                                                        requestedTasksSection = 0
                                                        currentTab = AppDestination.TASKS
                                                    },
                                                    onOpenGroupProjects = {
                                                        requestedTasksSection = 1
                                                        currentTab = AppDestination.TASKS
                                                    },
                                                    onOpenManageCourses = { showCourseManagerDialog = true },
                                                    onOpenGpaPredictor = { showGpaPredictorDialog = true },
                                                    onOpenBudget = { showBudgetDialog = true },
                                                    onOpenExportHub = { showExportHubDialog = true },
                                                    onOpenAlarmStudio = { showAlarmStudioDialog = true },
                                                    onOpenTimetableScanner = { showTimetableScannerDialog = true },
                                                    onOpenCitationManager = { showCitationDialog = true },
                                                    onOpenWhiteboardScanner = { showWhiteboardScannerDialog = true },
                                                    onOpenGlobalSearch = { showGlobalSearchDialog = true },
                                                    onOpenAiAssistant = { currentScreen = NavigationScreen.AI_ASSISTANT },
                                                    onOpenNotifications = { showNotificationCenter = true },
                                                    onOpenAddLecture = { showAddLectureDialog = true },
                                                    onOpenAddAssessment = { showAddAssessmentDialog = true }
                                                )
                                                AppDestination.TIMETABLE -> TimetableScreen(
                                                    viewModel = viewModel,
                                                    onOpenCourseSlides = { courseCode ->
                                                        viewModel.setResourceCourseFilter(courseCode)
                                                        requestedStudyTab = StudyTabs.COURSE_SLIDES
                                                        currentTab = AppDestination.STUDY
                                                    },
                                                    onStartFocusForCourse = { courseCode ->
                                                        viewModel.setSelectedFocusCourse(courseCode)
                                                        requestedStudyTab = StudyTabs.FOCUS
                                                        currentTab = AppDestination.STUDY
                                                    }
                                                )
                                                AppDestination.STUDY -> StudyScreen(
                                                    viewModel = viewModel,
                                                    initialTab = requestedStudyTab
                                                )
                                                AppDestination.TASKS -> TasksScreen(
                                                    viewModel = viewModel,
                                                    initialSection = requestedTasksSection,
                                                    onOpenCitationManager = { showCitationDialog = true },
                                                    onOpenGpaPredictor = { showGpaPredictorDialog = true }
                                                )
                                                AppDestination.PROFILE -> ProfileScreen(viewModel = viewModel)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Strict Focus Lockout Full-Screen Overlay
                    if (isStrictLockout) {
                        StrictFocusLockoutOverlay(viewModel = viewModel)
                    }
                }

                // Notification Centre Modal Sheet
                if (showNotificationCenter) {
                    NotificationCenterSheet(
                        viewModel = viewModel,
                        onDismiss = { showNotificationCenter = false }
                    )
                }

                // Add Lecture Dialog from Quick Action
                if (showAddLectureDialog) {
                    val courses by viewModel.courses.collectAsState()
                    val previewingToneId by viewModel.previewingAlarmToneId.collectAsState()
                    AddLectureDialog(
                        courses = courses,
                        onManageCourses = { showCourseManagerDialog = true },
                        previewingToneId = previewingToneId,
                        onPreviewTone = { toneId -> viewModel.previewAlarmTone(toneId) },
                        onDismiss = {
                            viewModel.stopAlarmPreview()
                            showAddLectureDialog = false
                        },
                        onSave = { lecture ->
                            viewModel.stopAlarmPreview()
                            viewModel.saveLecture(lecture) {
                                showAddLectureDialog = false
                            }
                        }
                    )
                }

                // Lecture Alarm & Sound Studio Dialog from Home Quick Action
                if (showAlarmStudioDialog) {
                    LectureAlarmManagerDialog(
                        viewModel = viewModel,
                        onDismiss = { showAlarmStudioDialog = false }
                    )
                }

                // Enrolled Course Manager Dialog from Quick Action or Lecture Dialog
                if (showCourseManagerDialog) {
                    CourseManagerDialog(
                        viewModel = viewModel,
                        onDismiss = { showCourseManagerDialog = false },
                        onOpenCourseSlides = { courseCode ->
                            viewModel.setResourceCourseFilter(courseCode)
                            requestedStudyTab = StudyTabs.COURSE_SLIDES
                            currentTab = AppDestination.STUDY
                        }
                    )
                }

                // Add Assessment Dialog from Quick Action
                if (showAddAssessmentDialog) {
                    val courses by viewModel.courses.collectAsState()
                    AddAssessmentDialog(
                        courses = courses,
                        onDismiss = { showAddAssessmentDialog = false },
                        onSave = { assessment ->
                            viewModel.saveAssessment(assessment)
                            showAddAssessmentDialog = false
                        }
                    )
                }

                // GPA & Final Exam Score Predictor Dialog from Quick Action
                if (showGpaPredictorDialog) {
                    GpaCalculatorDialog(
                        viewModel = viewModel,
                        initialTab = 1,
                        onDismiss = { showGpaPredictorDialog = false }
                    )
                }

                // Campus Budget & Academic Expenses Dialog from Quick Action
                if (showBudgetDialog) {
                    CampusBudgetDialog(
                        viewModel = viewModel,
                        onDismiss = { showBudgetDialog = false }
                    )
                }

                // Academic PDF & .ICS Export Hub Dialog from Quick Action
                if (showExportHubDialog) {
                    JsonImportExportDialog(
                        viewModel = viewModel,
                        onDismiss = { showExportHubDialog = false }
                    )
                }

                // AI Timetable & Syllabus Scanner Dialog from Home Quick Action
                if (showTimetableScannerDialog) {
                    TimetableScannerDialog(
                        viewModel = viewModel,
                        onDismiss = { showTimetableScannerDialog = false }
                    )
                }

                // Academic Citation & Bibliography Formatter Dialog
                if (showCitationDialog) {
                    val courses by viewModel.courses.collectAsState()
                    CitationManagerDialog(
                        viewModel = viewModel,
                        defaultCourseCode = courses.firstOrNull()?.code ?: "General",
                        onDismiss = { showCitationDialog = false }
                    )
                }

                // Whiteboard & Handout Live Camera Scanner Dialog
                if (showWhiteboardScannerDialog) {
                    val courses by viewModel.courses.collectAsState()
                    SlideScannerDialog(
                        viewModel = viewModel,
                        defaultCourseCode = courses.firstOrNull()?.code ?: "General",
                        onDismiss = { showWhiteboardScannerDialog = false }
                    )
                }

                // Universal Campus Spotlight Search & Command Center
                if (showGlobalSearchDialog) {
                    GlobalSearchDialog(
                        viewModel = viewModel,
                        onDismiss = { showGlobalSearchDialog = false },
                        onOpenTimetableDay = { targetDow ->
                            val now = java.time.LocalDate.now()
                            val delta = (targetDow - now.dayOfWeek.value).toLong()
                            viewModel.setSelectedDate(now.plusDays(delta))
                            viewModel.setTimetableViewMode(TimetableViewMode.DAILY)
                            currentTab = AppDestination.TIMETABLE
                        },
                        onOpenStudyTab = { targetStudyTab, courseCode ->
                            if (!courseCode.isNullOrBlank() && targetStudyTab == StudyTabs.COURSE_SLIDES) {
                                viewModel.setResourceCourseFilter(courseCode)
                            }
                            requestedStudyTab = targetStudyTab
                            currentTab = AppDestination.STUDY
                        },
                        onOpenTasks = {
                            requestedTasksSection = 0
                            currentTab = AppDestination.TASKS
                        },
                        onOpenManageCourses = { showCourseManagerDialog = true },
                        onOpenAiAssistant = { currentScreen = NavigationScreen.AI_ASSISTANT }
                    )
                }

                // Live Ringing Lecture / Academic Alarm Dialog (triggered by AlarmReceiver or Notification tap)
                val displayedAlarm = activeRingingAlarm ?: intentAlarm
                if (displayedAlarm != null) {
                    AlarmScreenDialog(
                        title = displayedAlarm.title,
                        courseCode = displayedAlarm.courseCode,
                        timeText = displayedAlarm.timeText,
                        message = displayedAlarm.message,
                        alarmToneId = displayedAlarm.alarmToneId,
                        requireBrainCheck = userProfile.alarmWakeUpBrainCheck,
                        defaultSnoozeMinutes = userProfile.defaultSnoozeMinutes,
                        onDismiss = {
                            viewModel.dismissActiveAlarm(displayedAlarm.notificationId)
                            intentAlarmFlow.value = null
                        },
                        onSnooze = {
                            viewModel.snoozeActiveAlarm(
                                notificationId = displayedAlarm.notificationId,
                                title = displayedAlarm.title,
                                message = displayedAlarm.message.ifBlank { "Snoozed Academic Alarm" },
                                category = displayedAlarm.category,
                                eventId = displayedAlarm.eventId,
                                courseCode = displayedAlarm.courseCode,
                                snoozeMinutes = userProfile.defaultSnoozeMinutes
                            )
                            intentAlarmFlow.value = null
                        },
                        onSnoozeWithMinutes = { mins ->
                            viewModel.snoozeActiveAlarm(
                                notificationId = displayedAlarm.notificationId,
                                title = displayedAlarm.title,
                                message = displayedAlarm.message.ifBlank { "Snoozed Academic Alarm" },
                                category = displayedAlarm.category,
                                eventId = displayedAlarm.eventId,
                                courseCode = displayedAlarm.courseCode,
                                snoozeMinutes = mins
                            )
                            intentAlarmFlow.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractAlarmFromIntent(intent)
    }

    private fun extractAlarmFromIntent(intent: Intent?) {
        val widgetDest = intent?.getStringExtra(AuraUpNextWidgetProvider.EXTRA_WIDGET_DESTINATION)
        if (!widgetDest.isNullOrBlank()) {
            widgetDestinationFlow.value = widgetDest
        }
        val category = intent?.getStringExtra(NotificationHelper.EXTRA_CATEGORY)
        val title = intent?.getStringExtra(NotificationHelper.EXTRA_TITLE)
        if (!category.isNullOrBlank() && !title.isNullOrBlank()) {
            val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: ""
            val courseCode = intent.getStringExtra(NotificationHelper.EXTRA_COURSE_CODE) ?: category
            val timeText = intent.getStringExtra(NotificationHelper.EXTRA_TIME_TEXT) ?: "Scheduled Alert"
            val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 1001)
            val eventId = intent.getLongExtra(NotificationHelper.EXTRA_EVENT_ID, 0L)
            val alarmToneId = intent.getStringExtra(NotificationHelper.EXTRA_ALARM_TONE_ID) ?: ""

            intentAlarmFlow.value = ActiveAlarmPayload(
                notificationId = notifId,
                title = title,
                message = message,
                courseCode = courseCode,
                timeText = timeText,
                category = category,
                eventId = eventId,
                alarmToneId = alarmToneId
            )
        }
    }
}

@Composable
fun AuraBottomNavigationBar(
    currentTab: AppDestination,
    onSelectTab: (AppDestination) -> Unit
) {
    val auraColors = LocalAuraColors.current
    val view = LocalView.current
    val navBrush = if (auraColors.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xEE152442),
                Color(0xF60A1224)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFAFFFFFF),
                Color(0xF0EDF6FF)
            )
        )
    }

    val rimBrush = Brush.linearGradient(
        listOf(
            Color(0x99FFFFFF),
            auraColors.secondaryAccent.copy(alpha = 0.48f),
            auraColors.aiAccent.copy(alpha = 0.35f),
            auraColors.primaryAccent.copy(alpha = 0.55f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = auraColors.primaryAccent.copy(alpha = 0.45f),
                ambientColor = auraColors.secondaryAccent.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(navBrush)
            .border(1.2.dp, rimBrush, RoundedCornerShape(32.dp))
            .drawWithContent {
                drawContent()
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xCCFFFFFF),
                            auraColors.secondaryAccent.copy(alpha = 0.78f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(x = 28.dp.toPx(), y = 1.2.dp.toPx()),
                    end = Offset(x = size.width - 28.dp.toPx(), y = 1.2.dp.toPx()),
                    strokeWidth = 1.6.dp.toPx()
                )
            }
            .testTag("aura_bottom_navigation")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestination.entries.forEach { destination ->
                val isSelected = currentTab == destination
                val interactionSource = remember { MutableInteractionSource() }
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.14f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navBounce_${destination.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 50.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                auraColors.secondaryAccent.copy(alpha = 0.24f),
                                                auraColors.primaryAccent.copy(alpha = 0.24f),
                                                auraColors.aiAccent.copy(alpha = 0.16f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x99FFFFFF),
                                                auraColors.secondaryAccent.copy(alpha = 0.45f)
                                            )
                                        ),
                                        RoundedCornerShape(24.dp)
                                    )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = auraColors.secondaryAccent),
                            onClick = {
                                if (!isSelected) {
                                    performAuraHaptic(view)
                                }
                                onSelectTab(destination)
                            }
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("nav_item_${destination.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label,
                            tint = if (isSelected) auraColors.secondaryAccent else auraColors.textMuted,
                            modifier = Modifier
                                .size(21.dp)
                                .scale(iconScale)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = destination.label,
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) auraColors.textPrimary else auraColors.textMuted,
                            maxLines = 1
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 2.6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(auraColors.secondaryAccent, auraColors.primaryAccent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

