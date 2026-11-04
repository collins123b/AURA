package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import com.example.util.SemesterWorkloadAdvisorCard
import com.example.util.SmartSleepAndWakePlannerCard
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ai.AuraAcademicAdvisor
import com.example.ui.AuraViewModel
import com.example.ui.FocusMode
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.AuraLiveAudioEqualizer
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.formatTime12Hour
import com.example.ui.components.performAuraHaptic
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import com.example.util.SoundscapeType
import java.time.DayOfWeek
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object StudyTabs {
    const val FOCUS = 0
    const val COURSE_SLIDES = 1
    const val SMART_PLANNER = 2
    const val NOTES_VAULT = 3
    const val OFFICE_HOURS = 4
    const val FLASHCARDS = 5
    const val AUDIO_NOTES = 6
    const val SYLLABUS = 7
    const val HABITS = 8
    const val WAR_ROOM = 9
    const val PRACTICE_QUIZ = 10
    const val CLOUD_ROOMS = 11
    const val MAX_INDEX = 11
}

@Composable
fun StudyScreen(
    viewModel: AuraViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, StudyTabs.MAX_INDEX)) }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, StudyTabs.MAX_INDEX)
    }

    val view = LocalView.current
    val auraColors = LocalAuraColors.current

    val activeCategoryIndex = when (selectedTab) {
        StudyTabs.FOCUS, StudyTabs.SMART_PLANNER, StudyTabs.HABITS -> 0
        StudyTabs.COURSE_SLIDES, StudyTabs.NOTES_VAULT, StudyTabs.AUDIO_NOTES, StudyTabs.SYLLABUS -> 1
        StudyTabs.FLASHCARDS, StudyTabs.WAR_ROOM, StudyTabs.PRACTICE_QUIZ -> 2
        else -> 3
    }

    FrostedGlassBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 4-Category Bento Study Hub Switcher (Compact Horizontal Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("study_bento_category_switcher"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(
                    Triple("Focus", StudyTabs.FOCUS, "study_category_focus"),
                    Triple("Slides & Vault", StudyTabs.COURSE_SLIDES, "study_category_slides"),
                    Triple("Exam Prep", StudyTabs.FLASHCARDS, "study_category_exam"),
                    Triple("Cloud Hub", StudyTabs.CLOUD_ROOMS, "study_category_cloud")
                )
                categories.forEachIndexed { idx, (label, targetTab, tag) ->
                    val isCatSelected = activeCategoryIndex == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatSelected) {
                            auraColors.primaryAccent.copy(alpha = 0.28f)
                        } else if (auraColors.isDark) {
                            Color(0x1EFFFFFF)
                        } else {
                            Color(0x99FFFFFF)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isCatSelected) auraColors.secondaryAccent else Color(0x32FFFFFF)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                performAuraHaptic(view)
                                selectedTab = targetTab
                            }
                            .testTag(tag)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isCatSelected) auraColors.secondaryAccent else auraColors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Frosted Iridescent Scrollable Tab Strip (Inline Horizontal Icon + Label)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (LocalAuraColors.current.isDark) Color(0xCC0D172C) else Color(0xDDFFFFFF),
                contentColor = LocalAuraColors.current.secondaryAccent,
                edgePadding = 12.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .clip(CircleShape),
                            height = 3.dp,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, Color(0x2EFFFFFF))
                    .testTag("study_screen_tabs")
            ) {
                val tabItems = listOf(
                    Triple(StudyTabs.FOCUS, "Focus Timer", Icons.Default.Timer) to "study_tab_focus",
                    Triple(StudyTabs.COURSE_SLIDES, "Course Slides", Icons.Default.Slideshow) to "study_tab_slides",
                    Triple(StudyTabs.SMART_PLANNER, "Smart Planner", Icons.Default.AutoAwesome) to "study_tab_planner",
                    Triple(StudyTabs.NOTES_VAULT, "Notes Vault", Icons.Default.Functions) to "study_tab_notes_vault",
                    Triple(StudyTabs.OFFICE_HOURS, "Office Hours", Icons.Default.RecordVoiceOver) to "study_tab_office_hours",
                    Triple(StudyTabs.FLASHCARDS, "Flashcards", Icons.Default.Flip) to "study_tab_flashcards",
                    Triple(StudyTabs.AUDIO_NOTES, "Audio Notes", Icons.Default.Mic) to "study_tab_audio",
                    Triple(StudyTabs.SYLLABUS, "Syllabus", Icons.Default.Checklist) to "study_tab_syllabus",
                    Triple(StudyTabs.HABITS, "Habits & Streaks", Icons.Default.Refresh) to "study_tab_habits",
                    Triple(StudyTabs.WAR_ROOM, "Exam War Room", Icons.Default.HourglassBottom) to "study_tab_war_room",
                    Triple(StudyTabs.PRACTICE_QUIZ, "Practice Quiz", Icons.Default.Quiz) to "study_tab_quiz",
                    Triple(StudyTabs.CLOUD_ROOMS, "Cloud & Rooms", Icons.Default.CloudSync) to "study_tab_cloud_rooms"
                )
                tabItems.forEach { (triple, tag) ->
                    val (tabIdx, label, icon) = triple
                    val isSelected = selectedTab == tabIdx
                    Tab(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tabIdx
                            if (tabIdx == StudyTabs.SMART_PLANNER) {
                                viewModel.generateAiStudyPlan()
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    StudyTabs.FOCUS -> FocusTimerTab(viewModel = viewModel)
                    StudyTabs.COURSE_SLIDES -> CourseResourcesView(viewModel = viewModel)
                    StudyTabs.SMART_PLANNER -> AiSmartPlannerTab(viewModel = viewModel)
                    StudyTabs.NOTES_VAULT -> StudyNotesView(viewModel = viewModel)
                    StudyTabs.OFFICE_HOURS -> OfficeHoursView(viewModel = viewModel)
                    StudyTabs.FLASHCARDS -> FlashcardsView(viewModel = viewModel)
                    StudyTabs.AUDIO_NOTES -> AudioNotesView(viewModel = viewModel)
                    StudyTabs.SYLLABUS -> SyllabusTrackerView(viewModel = viewModel)
                    StudyTabs.HABITS -> AcademicHabitsView(viewModel = viewModel)
                    StudyTabs.WAR_ROOM -> ExamWarRoomView(viewModel = viewModel)
                    StudyTabs.PRACTICE_QUIZ -> PracticeQuizView(viewModel = viewModel)
                    StudyTabs.CLOUD_ROOMS -> CloudStudyRoomsView(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun FocusTimerTab(viewModel: AuraViewModel) {
    val focusMode by viewModel.focusMode.collectAsState()
    val totalSeconds by viewModel.focusTotalSeconds.collectAsState()
    val remainingSeconds by viewModel.focusRemainingSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val selectedCourseCode by viewModel.selectedFocusCourseCode.collectAsState()
    val activeSoundscape by viewModel.activeSoundscape.collectAsState()
    val ambientVolume by viewModel.ambientVolume.collectAsState()
    val sessionGoal by viewModel.sessionGoalText.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val focusRecords by viewModel.focusRecords.collectAsState()

    var customMinutesSlider by remember(totalSeconds) {
        mutableFloatStateOf((totalSeconds / 60f).coerceIn(1f, 240f))
    }
    var showAnyTimePickerDialog by remember { mutableStateOf(false) }

    val progress = remember(totalSeconds, remainingSeconds) {
        if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat() else 0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressArc")

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val completedToday = remember(focusRecords) {
        val startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        focusRecords.filter { it.timestampEpochMillis >= startOfToday && it.completed }.size
    }

    val totalMinsToday = remember(focusRecords) {
        val startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        focusRecords.filter { it.timestampEpochMillis >= startOfToday && it.completed }.sumOf { it.durationMinutes }
    }

    if (showAnyTimePickerDialog) {
        CustomFocusAnyTimeDialog(
            currentTotalSeconds = totalSeconds,
            onDismiss = { showAnyTimePickerDialog = false },
            onConfirmSeconds = { chosenSeconds ->
                viewModel.setCustomFocusDurationSeconds(chosenSeconds)
                showAnyTimePickerDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("focus_timer_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Liquid Glass Mode Selector Chips + Set Any Exact Time Pill
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AuraGlassButton(
                        text = "Set Any Time",
                        icon = Icons.Default.Schedule,
                        isPrimary = focusMode == FocusMode.CUSTOM,
                        onClick = { showAnyTimePickerDialog = true },
                        cornerRadius = 16.dp,
                        testTag = "open_any_time_picker_button"
                    )
                }
                items(FocusMode.values()) { mode ->
                    val isSelected = focusMode == mode
                    AuraGlassButton(
                        text = mode.label,
                        isPrimary = isSelected,
                        onClick = {
                            if (mode == FocusMode.CUSTOM) {
                                viewModel.setFocusMode(mode, customMinutesSlider.roundToInt().coerceAtLeast(1))
                            } else {
                                viewModel.setFocusMode(mode)
                            }
                        },
                        cornerRadius = 16.dp,
                        testTag = "mode_${mode.name}"
                    )
                }
            }
        }

        // Custom Duration Slider (1 min to 240 mins + Exact H:M:S Editor)
        if (focusMode == FocusMode.CUSTOM) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Deep Work Duration",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                                modifier = Modifier.clickable { showAnyTimePickerDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Exact Hours Minutes Seconds",
                                        tint = LocalAuraColors.current.secondaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (totalSeconds >= 3600) {
                                            "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
                                        } else if (totalSeconds % 60 != 0) {
                                            "${totalSeconds / 60}m ${totalSeconds % 60}s"
                                        } else {
                                            "${(totalSeconds / 60).coerceAtLeast(1)} mins"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.secondaryAccent
                                    )
                                }
                            }
                        }
                        Slider(
                            value = customMinutesSlider,
                            onValueChange = {
                                customMinutesSlider = it
                                viewModel.setFocusMode(FocusMode.CUSTOM, it.roundToInt().coerceAtLeast(1))
                            },
                            valueRange = 1f..240f,
                            colors = SliderDefaults.colors(
                                thumbColor = LocalAuraColors.current.secondaryAccent,
                                activeTrackColor = LocalAuraColors.current.primaryAccent
                            ),
                            modifier = Modifier.testTag("custom_focus_slider")
                        )
                    }
                }
            }
        }

        // Course Target Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Studying for:",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCourseCode == "General Study") LocalAuraColors.current.primaryAccent.copy(alpha = 0.3f) else Color(0x22FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x45FFFFFF)),
                            modifier = Modifier.clickable { viewModel.setSelectedFocusCourse("General Study") }
                        ) {
                            Text(
                                text = "General",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCourseCode == "General Study") LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(courses) { c ->
                        val isSel = selectedCourseCode == c.code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) LocalAuraColors.current.primaryAccent.copy(alpha = 0.3f) else Color(0x22FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x45FFFFFF)),
                            modifier = Modifier.clickable { viewModel.setSelectedFocusCourse(c.code) }
                        ) {
                            Text(
                                text = c.code,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Zen Sanctuary Precision Horology Timer Card
        item {
            val auraColors = LocalAuraColors.current
            val primaryColor = auraColors.primaryAccent
            val secondaryColor = auraColors.secondaryAccent

            AuraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                accentGlow = secondaryColor
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.study_sanctuary_nebula_1793569866560),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(if (auraColors.isDark) 0.34f else 0.12f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (auraColors.isDark) {
                                        listOf(
                                            Color(0x88081124),
                                            Color(0xCC0A152C),
                                            Color(0xE6081122)
                                        )
                                    } else {
                                        listOf(Color(0xCCFFFFFF), Color(0xEBEEF5FF))
                                    }
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraBadge(
                                text = if (isRunning) "LIVE SANCTUARY" else focusMode.label.uppercase(),
                                backgroundColor = if (isRunning) AuraMintAccent.copy(alpha = 0.22f) else Color(0x2638BDF8),
                                textColor = if (isRunning) AuraMintAccent else secondaryColor
                            )
                            AuraBadge(
                                text = "$completedToday sessions • ${totalMinsToday}m today",
                                backgroundColor = Color(0x22FFFFFF),
                                textColor = auraColors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(258.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0x3238BDF8),
                                            Color(0x140A84FF),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .clickable { showAnyTimePickerDialog = true }
                                .testTag("circular_timer_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                                val outerRadius = size.minDimension / 2f
                                val tickCount = 60

                                // 60 Precision Horology Tick Marks
                                for (i in 0 until tickCount) {
                                    val angleDeg = -90f + (i * 6f)
                                    val angleRad = Math.toRadians(angleDeg.toDouble())
                                    val isMajor = i % 5 == 0
                                    val tickFraction = i.toFloat() / tickCount.toFloat()
                                    val isLit = tickFraction <= animatedProgress && animatedProgress > 0.005f

                                    val innerTickR = outerRadius - if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                                    val outerTickR = outerRadius - 2.dp.toPx()

                                    drawLine(
                                        color = when {
                                            isLit -> secondaryColor
                                            isMajor -> Color(0x55FFFFFF)
                                            else -> Color(0x24FFFFFF)
                                        },
                                        start = Offset(
                                            x = centerOffset.x + (innerTickR * cos(angleRad)).toFloat(),
                                            y = centerOffset.y + (innerTickR * sin(angleRad)).toFloat()
                                        ),
                                        end = Offset(
                                            x = centerOffset.x + (outerTickR * cos(angleRad)).toFloat(),
                                            y = centerOffset.y + (outerTickR * sin(angleRad)).toFloat()
                                        ),
                                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                val arcInset = 18.dp.toPx()
                                val strokeWidth = 12.dp.toPx()
                                val arcDiameter = size.minDimension - (arcInset * 2f)

                                drawCircle(
                                    color = Color(0x22FFFFFF),
                                    radius = arcDiameter / 2f,
                                    center = centerOffset,
                                    style = Stroke(width = strokeWidth)
                                )

                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(secondaryColor, primaryColor, AuraMintAccent, secondaryColor)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    topLeft = Offset(
                                        centerOffset.x - arcDiameter / 2f,
                                        centerOffset.y - arcDiameter / 2f
                                    ),
                                    size = androidx.compose.ui.geometry.Size(arcDiameter, arcDiameter),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = timeFormatted,
                                    fontFamily = AuraMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = auraColors.textPrimary,
                                    fontSize = if (hours > 0) 36.sp else 46.sp,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedCourseCode,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryColor
                                )
                                Text(
                                    text = if (sessionGoal.isNotBlank()) {
                                        sessionGoal.take(26)
                                    } else if (isRunning) {
                                        "Deep Focus Active"
                                    } else {
                                        "Tap clock for any time"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = auraColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick On-The-Fly Time Stepper Pills (-5m, -1m, +1m, +5m, +15m, +30m)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickSteps = listOf(-5, -1, 1, 5, 15, 30)
                items(quickSteps) { step ->
                    val label = if (step > 0) "+${step}m" else "${step}m"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1EFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x36FFFFFF)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.adjustFocusDurationMinutes(step) }
                            .testTag("focus_step_${if (step > 0) "plus_$step" else "minus_${-step}"}")
                    ) {
                        Text(
                            text = label,
                            fontFamily = AuraMonoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Sculpted Iridescent Glass Timer Controls Dock
        item {
            val auraColors = LocalAuraColors.current
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraGlassIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    onClick = { viewModel.resetTimer() },
                    size = 54.dp,
                    tint = auraColors.textPrimary,
                    testTag = "timer_reset_button"
                )

                AuraGlassButton(
                    text = if (isRunning) "Pause" else "Start Focus",
                    icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    isPrimary = !isRunning,
                    containerColor = if (isRunning) auraColors.danger else null,
                    textColor = Color.White,
                    cornerRadius = 28.dp,
                    onClick = {
                        if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .width(184.dp),
                    testTag = "timer_toggle_button"
                )
            }
        }

        // Deep Focus Session Goal Banner
        item {
            FocusSessionGoalBannerCard(viewModel = viewModel)
        }

        // Personal Study Music Deck (Import & Play MP3/M4A/WAV/OGG while studying)
        item {
            FocusCustomMusicPlayerCard(viewModel = viewModel)
        }

        // Ambient Soundscape Synthesizer Card + Layering Volume Mixer
        item {
            AuraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ambient_soundscape_card"),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Frosted Study Soundscapes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        AuraLiveAudioEqualizer(
                            isPlaying = activeSoundscape != SoundscapeType.OFF,
                            accentColor = LocalAuraColors.current.secondaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AuraBadge(
                            text = activeSoundscape.displayName,
                            textColor = LocalAuraColors.current.secondaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SoundscapeType.entries) { sound ->
                            val isActive = activeSoundscape == sound
                            AuraGlassButton(
                                text = sound.displayName,
                                isPrimary = isActive,
                                onClick = { viewModel.setSoundscape(sound) },
                                cornerRadius = 14.dp,
                                testTag = "soundscape_${sound.name}"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = LocalAuraColors.current.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ambient Mix ${(ambientVolume * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = LocalAuraColors.current.textSecondary,
                            modifier = Modifier.width(96.dp)
                        )
                        Slider(
                            value = ambientVolume,
                            onValueChange = { viewModel.setAmbientVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = LocalAuraColors.current.secondaryAccent,
                                activeTrackColor = LocalAuraColors.current.primaryAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ambient_volume_slider")
                        )
                    }
                }
            }
        }

        // Guided 4-4-4-4 Box Breathing Pre-Study Warm-Up
        item {
            GuidedBoxBreathingCard()
        }

        // Distraction Parking Lot ("Brain Dump" Scratchpad)
        item {
            DistractionParkingLotCard(viewModel = viewModel)
        }

        // Strict Focus Lockout Card
        item {
            AuraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("strict_lockout_card"),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LocalAuraColors.current.aiAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strict Exam Lockout Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Blocks accidental back-navigation with OLED pure-black support & +100 XP bonus on completion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuraGlassButton(
                        text = "Enter Strict Lockout Mode",
                        icon = Icons.Default.Lock,
                        isPrimary = true,
                        onClick = { viewModel.startStrictLockout() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "start_strict_lockout_button"
                    )
                }
            }
        }

        // Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Sessions Today",
                    value = "$completedToday",
                    subtitle = "Target: 4 sessions",
                    icon = Icons.Default.CheckCircle,
                    accentColor = LocalAuraColors.current.secondaryAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Focus Time",
                    value = "${totalMinsToday}m",
                    subtitle = "Logged today",
                    icon = Icons.Default.Timer,
                    accentColor = LocalAuraColors.current.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CustomFocusAnyTimeDialog(
    currentTotalSeconds: Int,
    onDismiss: () -> Unit,
    onConfirmSeconds: (Int) -> Unit
) {
    val auraColors = LocalAuraColors.current
    var hoursInput by remember { mutableStateOf((currentTotalSeconds / 3600).toString()) }
    var minutesInput by remember { mutableStateOf(((currentTotalSeconds % 3600) / 60).toString()) }
    var secondsInput by remember { mutableStateOf((currentTotalSeconds % 60).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF5101B30),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, auraColors.secondaryAccent.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_any_time_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set Any Focus Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Choose any exact Hours, Minutes, and Seconds (from 10 seconds up to 12 hours).",
                    fontSize = 11.5.sp,
                    color = auraColors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Hours") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("any_time_hours_input")
                    )
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { minutesInput = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Mins") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("any_time_minutes_input")
                    )
                    OutlinedTextField(
                        value = secondsInput,
                        onValueChange = { secondsInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Secs") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("any_time_seconds_input")
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val quickMins = listOf(15, 35, 45, 60, 90, 120, 180, 240)
                    items(quickMins) { m ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x22FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x36FFFFFF)),
                            modifier = Modifier.clickable {
                                hoursInput = (m / 60).toString()
                                minutesInput = (m % 60).toString()
                                secondsInput = "0"
                            }
                        ) {
                            Text(
                                text = if (m >= 60) "${m / 60}h${if (m % 60 > 0) " ${m % 60}m" else ""}" else "${m}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.secondaryAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraGlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    AuraGlassButton(
                        text = "Apply Time",
                        isPrimary = true,
                        onClick = {
                            val h = hoursInput.toIntOrNull()?.coerceIn(0, 12) ?: 0
                            val m = minutesInput.toIntOrNull()?.coerceIn(0, 720) ?: 0
                            val s = secondsInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            val computed = (h * 3600 + m * 60 + s).coerceIn(10, 12 * 3600)
                            onConfirmSeconds(computed)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "apply_any_time_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSmartPlannerTab(viewModel: AuraViewModel) {
    val recommendations by viewModel.aiStudyPlan.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val assessments by viewModel.assessments.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("ai_smart_planner_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SmartSleepAndWakePlannerCard(lectures = lectures)
        }

        item {
            SemesterWorkloadAdvisorCard(
                courses = courses,
                lectures = lectures,
                assessments = assessments
            )
        }

        item {
            AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(LocalAuraColors.current.aiAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LocalAuraColors.current.aiAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Smart Academic Scheduler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.aiAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "AURA synthesizes your enrolled courses, upcoming deadlines, credit hour loads and lecture schedule to build realistic, non-conflicting study blocks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AuraGlassButton(
                        text = "Regenerate Study Schedule",
                        icon = Icons.Default.Refresh,
                        isPrimary = true,
                        onClick = { viewModel.generateAiStudyPlan() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "regenerate_plan_button"
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Recommended Study Sessions",
                subtitle = "Review and accept sessions into your timetable"
            )
        }

        if (recommendations.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Add courses and lectures to generate study schedules, or tap 'Regenerate' to refresh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recommendations) { rec ->
                StudyRecommendationCard(
                    recommendation = rec,
                    onAccept = { viewModel.acceptStudyRecommendation(rec) }
                )
            }
        }
    }
}

@Composable
private fun StudyRecommendationCard(
    recommendation: AuraAcademicAdvisor.StudyPlanRecommendation,
    onAccept: () -> Unit
) {
    val dayName = DayOfWeek.of(recommendation.dayOfWeek).name.lowercase().replaceFirstChar { it.uppercase() }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_rec_card_${recommendation.courseCode}"),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraBadge(
                    text = recommendation.courseCode,
                    backgroundColor = Color(0x280A84FF),
                    textColor = LocalAuraColors.current.secondaryAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$dayName • ${formatTime12Hour(recommendation.startTime)} - ${formatTime12Hour(recommendation.endTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                AuraBadge(
                    text = "${recommendation.durationMinutes}m",
                    backgroundColor = Color(0x2264D2FF),
                    textColor = LocalAuraColors.current.secondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = recommendation.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = LocalAuraColors.current.textSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuraGlassButton(
                text = "Accept & Add to Timetable",
                icon = Icons.Default.Check,
                isPrimary = true,
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                testTag = "accept_study_rec_button"
            )
        }
    }
}
