package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.notifications.AlarmToneOption
import com.example.ui.AuraViewModel
import com.example.ui.components.AboutDeveloperCard
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.AuraUserAvatar
import com.example.ui.components.ExamReadinessHeatmapCard
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.performAuraHaptic
import com.example.ui.dialogs.CampusBudgetDialog
import com.example.ui.dialogs.CitationManagerDialog
import com.example.ui.dialogs.GpaCalculatorDialog
import com.example.ui.dialogs.JsonImportExportDialog
import com.example.ui.dialogs.LectureAlarmManagerDialog
import com.example.ui.dialogs.WidgetStudioDialog
import androidx.compose.material.icons.filled.EmojiEvents
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.AuraThemePreset
import com.example.ui.theme.LocalAuraColors
import com.example.util.AttendanceCalculator
import com.example.util.AuraSemesterWrappedDialog
import com.example.widget.AuraWidgetSuiteManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProfileScreen(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val focusRecords by viewModel.focusRecords.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val courseGrades by viewModel.courseGrades.collectAsState()
    val academicExpenses by viewModel.academicExpenses.collectAsState()

    val lectures by viewModel.lectures.collectAsState()
    val recentHabits by viewModel.recentAcademicHabits.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val citations by viewModel.citations.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showGpaDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAlarmStudioDialog by remember { mutableStateOf(false) }
    var showCloudRoomsDialog by remember { mutableStateOf(false) }
    var showCitationDialog by remember { mutableStateOf(false) }
    var showWidgetStudioDialog by remember { mutableStateOf(false) }
    var showAuraWrappedDialog by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importProfilePictureFromUri(uri)
        }
    }

    val totalBudgetSpent = remember(academicExpenses) { academicExpenses.sumOf { it.amount } }

    // Real calculations
    val overallAttendancePercent = remember(attendanceRecords, userProfile.attendanceTargetPercent) {
        val summary = AttendanceCalculator.computeCourseAttendance(
            records = attendanceRecords,
            targetPercent = userProfile.attendanceTargetPercent
        )
        if (summary.totalCountable == 0) 0 else summary.percentage
    }

    val totalCredits = remember(courseGrades) { courseGrades.sumOf { it.creditHours } }
    val currentCgpa = remember(courseGrades) {
        val qp = courseGrades.sumOf { it.creditHours * it.gradePoint }
        if (totalCredits > 0) qp / totalCredits else 0.0
    }

    val weekAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
    val totalFocusMins = remember(focusRecords) {
        focusRecords.filter { it.timestampEpochMillis >= weekAgo }.sumOf { it.durationMinutes }
    }
    val weeklyHours = totalFocusMins / 60f

    FrostedGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("profile_lazy_list"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Student Identity Card with Profile Picture Upload
            item {
                StudentIdentityCard(
                    userProfile = userProfile,
                    onUploadPhotoClick = {
                        profilePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemovePhotoClick = { viewModel.removeProfilePicture() },
                    onEditClick = { showEditProfileDialog = true }
                )
            }

            // 1b. Aurora Accent Theme Studio (4 Iridescent Presets)
            item {
                AuroraThemeStudioCard(
                    selectedPresetId = userProfile.accentPreset,
                    onSelectPreset = { presetId -> viewModel.setAccentPreset(presetId) }
                )
            }

            // 1c. Aurora Home Screen Widget Studio (5 Specialized Liquid Glass Widgets)
            item {
                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_widget_studio_card"),
                    cornerRadius = 18.dp,
                    onClick = { showWidgetStudioDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "Home Screen Widgets Studio",
                                tint = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Home Screen Widgets Studio",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Up-Next • Schedule • Exam Radar • Focus 2×2 • Study Deck",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                        AuraBadge(
                            text = "5 WIDGETS",
                            backgroundColor = LocalAuraColors.current.secondaryAccent.copy(alpha = 0.16f),
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                    }
                }
            }

            // 2. Extra Feature Shortcut Row (GPA Simulator & JSON Timetable Backup)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_gpa_calculator_card"),
                        cornerRadius = 18.dp,
                        onClick = { showGpaDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Calculate, null, tint = LocalAuraColors.current.primaryAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("GPA Calculator", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary, fontSize = 13.sp)
                                Text(
                                    text = if (totalCredits > 0) "CGPA: ${String.format("%.2f", currentCgpa)}" else "Tap to compute",
                                    fontSize = 11.sp,
                                    color = LocalAuraColors.current.secondaryAccent
                                )
                            }
                        }
                    }

                    AuraCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_json_backup_card"),
                        cornerRadius = 18.dp,
                        onClick = { showJsonDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LocalAuraColors.current.secondaryAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Share, null, tint = LocalAuraColors.current.secondaryAccent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Export & Backup", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary, fontSize = 13.sp)
                                Text("PDF, PNG, .ICS", fontSize = 11.sp, color = LocalAuraColors.current.textMuted)
                            }
                        }
                    }
                }
            }

            // 2.1 Academic Citation & Reference Formatter Vault Card
            item {
                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_citation_manager_card"),
                    cornerRadius = 18.dp,
                    accentGlow = LocalAuraColors.current.aiAccent,
                    onClick = { showCitationDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.aiAccent.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FormatQuote, null, tint = LocalAuraColors.current.aiAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Academic Citation & Bibliography Vault",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "APA 7th, IEEE, MLA 9th & Harvard formatter with AI auto-fill",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                        AuraBadge(
                            text = "${citations.size} Saved",
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                    }
                }
            }

            // 2a. Cloud Backup, Cross-Device Sync & Live Campus Study Rooms Card
            item {
                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_cloud_sync_card"),
                    cornerRadius = 20.dp,
                    accentGlow = AuraMintAccent,
                    onClick = { showCloudRoomsDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AuraMintAccent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = AuraMintAccent,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Backup & Live Campus Study Rooms",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Google Sign-In sync, 1-tap restore & real-time peer study rooms",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                        AuraBadge(
                            text = "CLOUD",
                            backgroundColor = AuraMintAccent.copy(alpha = 0.18f),
                            textColor = AuraMintAccent
                        )
                    }
                }
            }

            // 2a-2. AURA Semester Wrapped & Shareable Scholar Card
            item {
                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_aura_wrapped_card"),
                    cornerRadius = 18.dp,
                    accentGlow = LocalAuraColors.current.aiAccent,
                    onClick = { showAuraWrappedDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.aiAccent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "AURA Semester Wrapped",
                                tint = LocalAuraColors.current.secondaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AURA Semester Wrapped & Scholar Summary",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Your scholar archetype, deep-work hours, streak & 1-tap share card",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                        AuraBadge(
                            text = "WRAPPED",
                            backgroundColor = LocalAuraColors.current.aiAccent.copy(alpha = 0.18f),
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                    }
                }
            }

            // 2b. Semester Academic Budget & Expense Tracker Card
            item {
                val budgetRemaining = userProfile.semesterBudgetLimit - totalBudgetSpent
                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_campus_budget_card"),
                    cornerRadius = 18.dp,
                    onClick = { showBudgetDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.warning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = LocalAuraColors.current.warning, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Semester Budget & Campus Expenses",
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Spent ${userProfile.currencySymbol}${String.format("%.2f", totalBudgetSpent)} of ${userProfile.currencySymbol}${String.format("%.2f", userProfile.semesterBudgetLimit)}",
                                fontSize = 11.sp,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                        AuraBadge(
                            text = "${userProfile.currencySymbol}${String.format("%.0f", budgetRemaining)} left",
                            textColor = if (budgetRemaining >= 0) LocalAuraColors.current.success else LocalAuraColors.current.danger
                        )
                    }
                }
            }

            // 3. Attendance Tracker Section
            item {
                AttendanceTrackerSection(
                    courses = courses,
                    attendanceRecords = attendanceRecords,
                    targetPercent = userProfile.attendanceTargetPercent,
                    onRecordAttendance = { cId, cCode, status ->
                        viewModel.recordAttendance(cId, cCode, status)
                    },
                    onUndoLatest = { cId ->
                        viewModel.deleteLatestCourseAttendance(cId)
                    }
                )
            }

            // 4. Academic Analytics
            item {
                AcademicAnalyticsSection(
                    weeklyHours = weeklyHours,
                    goalHours = userProfile.studyGoalHoursPerWeek,
                    focusSessionsCount = focusRecords.size,
                    focusRecords = focusRecords,
                    attendancePercent = overallAttendancePercent,
                    completedAssessments = assessments.count { it.status == "COMPLETED" },
                    totalAssessments = assessments.size
                )
            }

            // 4b. 28-Day Exam Readiness & Memory Heatmap
            item {
                ExamReadinessHeatmapCard(
                    habits = recentHabits,
                    focusRecords = focusRecords,
                    flashcards = flashcards,
                    testTag = "profile_readiness_heatmap"
                )
            }

            // 5. AI Settings
            item {
                AiSettingsCard(
                    userProfile = userProfile,
                    onToggleAi = { viewModel.setAiEnabled(it) }
                )
            }

            // 6. System & Reminder Settings + Alarm Sound Studio & Live Verification
            item {
                PreferencesSettingsCard(
                    userProfile = userProfile,
                    ringingLecturesCount = lectures.count { it.reminderEnabled },
                    totalLecturesCount = lectures.size,
                    onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                    onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                    onToggleAlarms = { viewModel.setAlarmsEnabled(it) },
                    onToggleGentleFadeIn = { viewModel.setAlarmGentleFadeIn(it) },
                    onToggleWakeUpBrainCheck = { viewModel.setAlarmWakeUpBrainCheck(it) },
                    onSelectSnoozeMinutes = { viewModel.setDefaultSnoozeMinutes(it) },
                    onOpenAlarmStudio = { showAlarmStudioDialog = true },
                    onTestAlarm = { viewModel.triggerTestAlarm() }
                )
            }

            // 7. About App & Developer Info (Standard Settings Footer Placement)
            item {
                AboutDeveloperCard()
            }
        }
    }

    if (showAlarmStudioDialog) {
        LectureAlarmManagerDialog(
            viewModel = viewModel,
            onDismiss = { showAlarmStudioDialog = false }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, inst, prog, lvl, sem, tz, target, goal ->
                viewModel.updateProfile(name, inst, prog, lvl, sem, tz, target, goal)
                showEditProfileDialog = false
            }
        )
    }

    if (showGpaDialog) {
        GpaCalculatorDialog(
            viewModel = viewModel,
            onDismiss = { showGpaDialog = false }
        )
    }

    if (showJsonDialog) {
        JsonImportExportDialog(
            viewModel = viewModel,
            onDismiss = { showJsonDialog = false }
        )
    }

    if (showBudgetDialog) {
        CampusBudgetDialog(
            viewModel = viewModel,
            onDismiss = { showBudgetDialog = false }
        )
    }

    if (showCitationDialog) {
        CitationManagerDialog(
            viewModel = viewModel,
            defaultCourseCode = courses.firstOrNull()?.code ?: "General",
            onDismiss = { showCitationDialog = false }
        )
    }

    if (showWidgetStudioDialog) {
        WidgetStudioDialog(
            viewModel = viewModel,
            onDismiss = { showWidgetStudioDialog = false }
        )
    }

    if (showAuraWrappedDialog) {
        val streakDays = remember(focusRecords) {
            AuraWidgetSuiteManager.computeStudyStreakDays(focusRecords)
        }
        val masteredCards = remember(flashcards) {
            flashcards.count { it.repetitions >= 2 }
        }
        AuraSemesterWrappedDialog(
            studentName = userProfile.name,
            institution = userProfile.institution,
            programme = userProfile.programme,
            semester = userProfile.semester,
            cgpa = currentCgpa,
            totalFocusMinutes = focusRecords.sumOf { it.durationMinutes },
            streakDays = streakDays,
            attendancePercent = overallAttendancePercent,
            masteredCards = masteredCards,
            coursesCount = courses.size,
            onDismiss = { showAuraWrappedDialog = false }
        )
    }

    if (showCloudRoomsDialog) {
        Dialog(
            onDismissRequest = { showCloudRoomsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FrostedGlassBackground(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cloud Sync & Campus Study Rooms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = LocalAuraColors.current.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AuraGlassIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Close Cloud Sync",
                            onClick = { showCloudRoomsDialog = false },
                            size = 42.dp,
                            tint = LocalAuraColors.current.textPrimary,
                            testTag = "close_cloud_sync_dialog_button"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CloudStudyRoomsView(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentIdentityCard(
    userProfile: com.example.data.preferences.UserProfile,
    onUploadPhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_identity_card"),
        cornerRadius = 28.dp,
        accentGlow = auraColors.primaryAccent
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.aura_prism_hero_1793569866167),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (auraColors.isDark) 0.38f else 0.15f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (auraColors.isDark) {
                                listOf(
                                    Color(0x88081124),
                                    Color(0xD40A152C)
                                )
                            } else {
                                listOf(Color(0xCCFFFFFF), Color(0xEBEEF5FF))
                            }
                        )
                    )
            )

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraUserAvatar(
                        imagePath = userProfile.profilePicturePath,
                        studentName = userProfile.name,
                        size = 76.dp,
                        showCameraBadge = true,
                        onClick = onUploadPhotoClick,
                        testTag = "profile_avatar_view"
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        AuraBadge(
                            text = userProfile.academicLevel.uppercase(),
                            backgroundColor = Color(0x2E38BDF8),
                            textColor = auraColors.secondaryAccent
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = userProfile.programme,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = auraColors.textSecondary
                        )
                        Text(
                            text = "${userProfile.institution} • ${userProfile.timeZone}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = auraColors.textMuted
                        )
                    }

                    AuraGlassIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        onClick = onEditClick,
                        size = 48.dp,
                        iconSize = 18.dp,
                        tint = auraColors.secondaryAccent,
                        testTag = "edit_profile_button"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraGlassButton(
                        text = if (userProfile.profilePicturePath.isBlank()) "Upload Profile Photo" else "Change Photo",
                        icon = Icons.Default.CameraAlt,
                        isPrimary = userProfile.profilePicturePath.isBlank(),
                        onClick = onUploadPhotoClick,
                        modifier = Modifier.weight(1f),
                        testTag = "upload_profile_photo_button"
                    )
                    if (userProfile.profilePicturePath.isNotBlank()) {
                        AuraGlassButton(
                            text = "Remove",
                            icon = Icons.Default.Delete,
                            containerColor = auraColors.danger.copy(alpha = 0.28f),
                            textColor = auraColors.danger,
                            onClick = onRemovePhotoClick,
                            testTag = "remove_profile_photo_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceTrackerSection(
    courses: List<com.example.data.local.entity.CourseEntity>,
    attendanceRecords: List<com.example.data.local.entity.AttendanceRecordEntity>,
    targetPercent: Int,
    onRecordAttendance: (Long, String, String) -> Unit,
    onUndoLatest: (Long) -> Unit
) {
    Column {
        SectionHeader(
            title = "Skip-Safe Attendance Calculator",
            subtitle = "Target: $targetPercent% exam eligibility • Live skip allowance & recovery math"
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (courses.isEmpty()) {
            AuraCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Add your university courses in Timetable to log real lecture attendance and Skip-Safe buffers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                courses.forEach { course ->
                    val courseRecords = attendanceRecords.filter { it.courseId == course.id }
                    val summary = AttendanceCalculator.computeCourseAttendance(courseRecords, targetPercent)

                    AuraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendance_course_${course.code}"),
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AuraBadge(text = course.code)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LocalAuraColors.current.textPrimary
                                    )
                                    Text(
                                        text = "${summary.attendedCount} attended • ${summary.absentCount} missed • ${summary.excusedCount} excused",
                                        fontSize = 10.sp,
                                        color = LocalAuraColors.current.textMuted
                                    )
                                }
                                if (courseRecords.isNotEmpty()) {
                                    AuraGlassIconButton(
                                        icon = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo latest attendance log",
                                        onClick = { onUndoLatest(course.id) },
                                        size = 32.dp,
                                        iconSize = 15.dp,
                                        tint = LocalAuraColors.current.textSecondary,
                                        testTag = "undo_attendance_${course.code}"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "${summary.percentage}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.isBelowTarget) LocalAuraColors.current.danger else LocalAuraColors.current.success
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (summary.isBelowTarget) LocalAuraColors.current.danger.copy(alpha = 0.14f)
                                        else if (summary.safeMissAllowance > 0) LocalAuraColors.current.success.copy(alpha = 0.14f)
                                        else Color(0x18FFFFFF)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (summary.isBelowTarget) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (summary.isBelowTarget) LocalAuraColors.current.danger
                                        else if (summary.safeMissAllowance > 0) LocalAuraColors.current.success
                                        else LocalAuraColors.current.secondaryAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = summary.guidanceText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (summary.isBelowTarget) LocalAuraColors.current.danger
                                        else if (summary.safeMissAllowance > 0) LocalAuraColors.current.success
                                        else LocalAuraColors.current.textSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AttendanceActionButton(
                                    label = "Present",
                                    color = LocalAuraColors.current.success,
                                    onClick = { onRecordAttendance(course.id, course.code, "PRESENT") },
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceActionButton(
                                    label = "Late",
                                    color = LocalAuraColors.current.warning,
                                    onClick = { onRecordAttendance(course.id, course.code, "LATE") },
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceActionButton(
                                    label = "Absent",
                                    color = LocalAuraColors.current.danger,
                                    onClick = { onRecordAttendance(course.id, course.code, "ABSENT") },
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceActionButton(
                                    label = "Excused",
                                    color = LocalAuraColors.current.secondaryAccent,
                                    onClick = { onRecordAttendance(course.id, course.code, "EXCUSED") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun AcademicAnalyticsSection(
    weeklyHours: Float,
    goalHours: Int,
    focusSessionsCount: Int,
    focusRecords: List<com.example.data.local.entity.FocusRecordEntity>,
    attendancePercent: Int,
    completedAssessments: Int,
    totalAssessments: Int
) {
    val sevenDayBars = remember(focusRecords) {
        val today = LocalDate.now()
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val mins = focusRecords
                .filter {
                    Instant.ofEpochMilli(it.timestampEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == date
                }
                .sumOf { it.durationMinutes }
            date.dayOfWeek.name.take(1) to mins
        }
    }
    val maxDayMins = remember(sevenDayBars) { (sevenDayBars.maxOfOrNull { it.second } ?: 60).coerceAtLeast(30) }

    Column {
        SectionHeader(
            title = "Academic Productivity Analytics",
            subtitle = "Live metrics based on your verified records"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Study Progress",
                value = "${String.format("%.1f", weeklyHours)} / ${goalHours}h",
                subtitle = "Weekly study goal",
                icon = Icons.Default.Timer,
                accentColor = LocalAuraColors.current.primaryAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Focus Blocks",
                value = "$focusSessionsCount",
                subtitle = "Completed sessions",
                icon = Icons.Default.CheckCircle,
                accentColor = LocalAuraColors.current.secondaryAccent,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Avg Attendance",
                value = "$attendancePercent%",
                subtitle = "Across all courses",
                icon = Icons.Default.School,
                accentColor = LocalAuraColors.current.success,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Deliverables",
                value = "$completedAssessments / $totalAssessments",
                subtitle = "Completed tasks",
                icon = Icons.Default.BarChart,
                accentColor = LocalAuraColors.current.warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        AuraCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("seven_day_focus_sparkline_card"),
            cornerRadius = 18.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-Day Deep Focus Trend",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = "${sevenDayBars.sumOf { it.second }}m this week",
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraMintAccent
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    sevenDayBars.forEachIndexed { idx, (dayInitial, mins) ->
                        val ratio = (mins.toFloat() / maxDayMins.toFloat()).coerceIn(0.08f, 1f)
                        val isTodayBar = idx == 6
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height((42f * ratio).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isTodayBar) AuraMintAccent
                                        else LocalAuraColors.current.primaryAccent.copy(alpha = 0.72f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayInitial,
                                fontSize = 10.sp,
                                fontWeight = if (isTodayBar) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isTodayBar) AuraMintAccent else LocalAuraColors.current.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSettingsCard(
    userProfile: com.example.data.preferences.UserProfile,
    onToggleAi: (Boolean) -> Unit
) {
    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_settings_card"),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = LocalAuraColors.current.aiAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AURA AI Integration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = userProfile.aiEnabled,
                    onCheckedChange = onToggleAi,
                    colors = SwitchDefaults.colors(checkedThumbColor = LocalAuraColors.current.aiAccent)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Model: Gemini via Firebase AI Logic & App Check. Built-in deterministic academic advisor runs offline without credentials.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAuraColors.current.textSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            AuraBadge(
                text = if (userProfile.aiEnabled) "AI STATUS: ACTIVE" else "AI STATUS: DISABLED",
                backgroundColor = if (userProfile.aiEnabled) LocalAuraColors.current.success.copy(alpha = 0.15f) else Color(0x18FFFFFF),
                textColor = if (userProfile.aiEnabled) LocalAuraColors.current.success else LocalAuraColors.current.textMuted
            )
        }
    }
}

@Composable
private fun AuroraThemeStudioCard(
    selectedPresetId: String,
    onSelectPreset: (String) -> Unit
) {
    val view = LocalView.current
    val auraColors = LocalAuraColors.current

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_theme_preset_card"),
        cornerRadius = 22.dp,
        accentGlow = auraColors.secondaryAccent
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = auraColors.secondaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aurora Accent Theme Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary
                    )
                    Text(
                        text = "Dynamic time-of-day atmosphere + 4 iridescent glass presets",
                        fontSize = 11.sp,
                        color = auraColors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AuraThemePreset.entries) { preset ->
                    val isSelected = preset.id.equals(selectedPresetId, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) preset.primaryDark.copy(alpha = 0.25f) else Color(0x18FFFFFF),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) preset.secondaryDark else Color(0x30FFFFFF)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                performAuraHaptic(view)
                                onSelectPreset(preset.id)
                            }
                            .testTag("theme_preset_${preset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(preset.secondaryDark, preset.primaryDark)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) preset.secondaryDark else auraColors.textPrimary
                                )
                                Text(
                                    text = preset.subtitle,
                                    fontSize = 9.sp,
                                    color = auraColors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferencesSettingsCard(
    userProfile: com.example.data.preferences.UserProfile,
    ringingLecturesCount: Int,
    totalLecturesCount: Int,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleAlarms: (Boolean) -> Unit,
    onToggleGentleFadeIn: (Boolean) -> Unit = {},
    onToggleWakeUpBrainCheck: (Boolean) -> Unit = {},
    onSelectSnoozeMinutes: (Int) -> Unit = {},
    onOpenAlarmStudio: () -> Unit,
    onTestAlarm: () -> Unit
) {
    val activeSoundLabel = remember(userProfile.defaultAlarmToneId, userProfile.customAlarmFileName) {
        if (userProfile.defaultAlarmToneId == AlarmToneOption.CUSTOM_AUDIO.id && userProfile.customAlarmFileName.isNotBlank()) {
            "Custom: ${userProfile.customAlarmFileName}"
        } else {
            AlarmToneOption.fromId(userProfile.defaultAlarmToneId).displayName
        }
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preferences_settings_card"),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "System & Audible Lecture Alarms",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (userProfile.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null, tint = LocalAuraColors.current.primaryAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Frosted Liquid Glass Dark Mode", style = MaterialTheme.typography.titleSmall, color = LocalAuraColors.current.textPrimary)
                    Text("Midnight ice & frosted polar mist switch", style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textSecondary)
                }
                Switch(checked = userProfile.isDarkTheme, onCheckedChange = onToggleDarkTheme)
            }

            HorizontalDivider(color = Color(0x22FFFFFF))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, null, tint = LocalAuraColors.current.secondaryAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Native Android Notifications", style = MaterialTheme.typography.titleSmall, color = LocalAuraColors.current.textPrimary)
                    Text("Silent visual banners paired with single-stream alarm", style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textSecondary)
                }
                Switch(checked = userProfile.notificationsEnabled, onCheckedChange = onToggleNotifications)
            }

            HorizontalDivider(color = Color(0x22FFFFFF))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Alarm, null, tint = LocalAuraColors.current.warning)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Audible Ringing Lecture Alarms", style = MaterialTheme.typography.titleSmall, color = LocalAuraColors.current.textPrimary)
                    Text(
                        text = "$ringingLecturesCount/$totalLecturesCount lectures active • Sound: $activeSoundLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
                Switch(checked = userProfile.alarmsEnabled, onCheckedChange = onToggleAlarms)
            }

            AuraGlassButton(
                text = "Change Alarm Sound & Select Lectures",
                icon = Icons.Default.MusicNote,
                onClick = onOpenAlarmStudio,
                modifier = Modifier.fillMaxWidth(),
                testTag = "profile_open_alarm_studio_button"
            )

            AuraGlassButton(
                text = "Test Single-Stream Alarm Now",
                icon = Icons.Default.NotificationsActive,
                isPrimary = true,
                onClick = onTestAlarm,
                modifier = Modifier.fillMaxWidth(),
                testTag = "profile_test_alarm_button"
            )

            HorizontalDivider(color = Color(0x22FFFFFF))

            // Gradual Volume Fade-In Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, null, tint = AuraMintAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gentle Volume Fade-In (8s)", style = MaterialTheme.typography.titleSmall, color = LocalAuraColors.current.textPrimary)
                    Text("Smoothly ramps alarm volume from 18% to 100%", style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textSecondary)
                }
                Switch(
                    checked = userProfile.alarmGentleFadeIn,
                    onCheckedChange = onToggleGentleFadeIn,
                    modifier = Modifier.testTag("alarm_gentle_fade_switch")
                )
            }

            // Wake-Up Brain Check Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Psychology, null, tint = LocalAuraColors.current.aiAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wake-Up Brain Check", style = MaterialTheme.typography.titleSmall, color = LocalAuraColors.current.textPrimary)
                    Text("Require a quick mental math check to stop morning alarms", style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textSecondary)
                }
                Switch(
                    checked = userProfile.alarmWakeUpBrainCheck,
                    onCheckedChange = onToggleWakeUpBrainCheck,
                    modifier = Modifier.testTag("alarm_brain_check_switch")
                )
            }

            // Custom Snooze Interval Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Snooze:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalAuraColors.current.textSecondary
                )
                listOf(5, 10, 15).forEach { mins ->
                    val selected = userProfile.defaultSnoozeMinutes == mins
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) LocalAuraColors.current.primaryAccent.copy(alpha = 0.28f) else Color(0x18FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (selected) LocalAuraColors.current.secondaryAccent else Color(0x2EFFFFFF)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSnoozeMinutes(mins) }
                            .testTag("profile_snooze_$mins")
                    ) {
                        Text(
                            text = "${mins}m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    userProfile: com.example.data.preferences.UserProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, inst: String, prog: String, lvl: String, sem: String, tz: String, target: Int, goal: Int) -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var institution by remember { mutableStateOf(userProfile.institution) }
    var programme by remember { mutableStateOf(userProfile.programme) }
    var level by remember { mutableStateOf(userProfile.academicLevel) }
    var semester by remember { mutableStateOf(userProfile.semester) }
    var timeZone by remember { mutableStateOf(userProfile.timeZone) }
    var targetAttendance by remember { mutableStateOf(userProfile.attendanceTargetPercent.toString()) }
    var studyGoal by remember { mutableStateOf(userProfile.studyGoalHoursPerWeek.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Student Profile", fontWeight = FontWeight.Bold, color = LocalAuraColors.current.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = institution, onValueChange = { institution = it }, label = { Text("University / Institution") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = programme, onValueChange = { programme = it }, label = { Text("Programme") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Level") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = timeZone, onValueChange = { timeZone = it }, label = { Text("Time Zone") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = "Save Profile",
                isPrimary = true,
                onClick = {
                    onSave(
                        name,
                        institution,
                        programme,
                        level,
                        semester,
                        timeZone,
                        targetAttendance.toIntOrNull() ?: 75,
                        studyGoal.toIntOrNull() ?: 18
                    )
                },
                testTag = "save_profile_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Cancel",
                onClick = onDismiss,
                testTag = "cancel_profile_button"
            )
        },
        containerColor = Color(0xF0101B30),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(
            1.2.dp,
            Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x4564D2FF))),
            RoundedCornerShape(24.dp)
        )
    )
}
