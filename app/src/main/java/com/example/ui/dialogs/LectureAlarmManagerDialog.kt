package com.example.ui.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.LectureEntity
import com.example.notifications.AlarmToneOption
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.components.formatTime12Hour
import com.example.ui.theme.LocalAuraColors
import java.time.DayOfWeek

@Composable
fun LectureAlarmManagerDialog(
    viewModel: AuraViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val lectures by viewModel.lectures.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val previewingToneId by viewModel.previewingAlarmToneId.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAlarmPreview()
        }
    }

    val customAlarmAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomAlarmSoundFromUri(uri) { _, message ->
                feedbackBanner = message
            }
        }
    }

    val sortedLectures = remember(lectures) {
        lectures.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }
    val ringingCount = remember(lectures) { lectures.count { it.reminderEnabled } }

    Dialog(
        onDismissRequest = {
            viewModel.stopAlarmPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF5111D35),
                            Color(0xF80B1324)
                        )
                    )
                )
                .border(
                    1.4.dp,
                    Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x4564D2FF))),
                    RoundedCornerShape(28.dp)
                )
                .testTag("lecture_alarm_manager_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.24f))
                            .border(1.dp, Color(0x66FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lecture Alarm Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = "$ringingCount of ${lectures.size} lectures set to ring • Single-Stream Audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.secondaryAccent
                        )
                    }
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Alarm Studio",
                        onClick = {
                            viewModel.stopAlarmPreview()
                            onDismiss()
                        },
                        size = 44.dp,
                        iconSize = 18.dp,
                        tint = LocalAuraColors.current.textPrimary,
                        testTag = "close_alarm_studio_button"
                    )
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x18FFFFFF),
                    contentColor = LocalAuraColors.current.primaryAccent,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = LocalAuraColors.current.secondaryAccent
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Select Lectures ($ringingCount/${lectures.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("alarm_studio_tab_lectures")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Alarm Sound & Tone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("alarm_studio_tab_sounds")
                    )
                }

                if (feedbackBanner != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LocalAuraColors.current.success.copy(alpha = 0.18f))
                            .border(1.dp, LocalAuraColors.current.success.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = feedbackBanner ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalAuraColors.current.success
                        )
                    }
                }

                if (selectedTab == 0) {
                    SelectLecturesAlarmTab(
                        lectures = sortedLectures,
                        defaultToneId = userProfile.defaultAlarmToneId,
                        onEnableAll = { viewModel.setAllLectureAlarmsEnabled(true) },
                        onMuteAll = { viewModel.setAllLectureAlarmsEnabled(false) },
                        onToggleLecture = { viewModel.toggleLectureAlarm(it) },
                        onUpdateConfig = { lec, enabled, mins, toneId ->
                            viewModel.updateLectureAlarmConfig(lec, enabled, mins, toneId)
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    AlarmSoundStudioTab(
                        selectedDefaultToneId = userProfile.defaultAlarmToneId,
                        customFileName = userProfile.customAlarmFileName,
                        customFilePath = userProfile.customAlarmFilePath,
                        previewingToneId = previewingToneId,
                        onSelectDefaultTone = { toneId ->
                            viewModel.setDefaultAlarmTone(toneId)
                        },
                        onPreviewTone = { toneId ->
                            viewModel.previewAlarmTone(toneId, userProfile.customAlarmFilePath)
                        },
                        onUploadCustomAudio = {
                            customAlarmAudioLauncher.launch(arrayOf("audio/*"))
                        },
                        onClearCustomAudio = {
                            viewModel.clearCustomAlarmSound()
                            feedbackBanner = "Removed custom alarm audio; reverted to Aurora Crystal Chime."
                        },
                        onTestLiveAlarm = {
                            viewModel.stopAlarmPreview()
                            viewModel.triggerTestAlarm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectLecturesAlarmTab(
    lectures: List<LectureEntity>,
    defaultToneId: String,
    onEnableAll: () -> Unit,
    onMuteAll: () -> Unit,
    onToggleLecture: (LectureEntity) -> Unit,
    onUpdateConfig: (LectureEntity, Boolean, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val leadOptions = listOf(0, 5, 10, 15, 30)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuraGlassButton(
                    text = "Ring All Lectures",
                    icon = Icons.Default.AlarmOn,
                    isPrimary = true,
                    onClick = onEnableAll,
                    modifier = Modifier.weight(1f),
                    testTag = "enable_all_lecture_alarms_button"
                )
                AuraGlassButton(
                    text = "Mute All",
                    icon = Icons.Default.AlarmOff,
                    onClick = onMuteAll,
                    modifier = Modifier.weight(1f),
                    testTag = "mute_all_lecture_alarms_button"
                )
            }
        }

        if (lectures.isEmpty()) {
            item {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Scheduled Lectures Yet",
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add lectures to your Timetable first, then choose exactly which ones should ring.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(lectures, key = { it.id }) { lecture ->
                val dayName = DayOfWeek.of(lecture.dayOfWeek.coerceIn(1, 7))
                    .name.lowercase().replaceFirstChar { it.uppercase() }
                val effectiveTone = AlarmToneOption.fromId(
                    lecture.alarmToneId.ifBlank { defaultToneId }
                )

                AuraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_manager_item_${lecture.id}"),
                    cornerRadius = 18.dp,
                    borderColor = if (lecture.reminderEnabled) {
                        LocalAuraColors.current.secondaryAccent.copy(alpha = 0.55f)
                    } else {
                        Color(0x28FFFFFF)
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraBadge(
                                        text = lecture.courseCode,
                                        backgroundColor = if (lecture.reminderEnabled) {
                                            LocalAuraColors.current.primaryAccent.copy(alpha = 0.25f)
                                        } else {
                                            Color(0x18FFFFFF)
                                        },
                                        textColor = if (lecture.reminderEnabled) {
                                            LocalAuraColors.current.secondaryAccent
                                        } else {
                                            LocalAuraColors.current.textMuted
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$dayName • ${formatTime12Hour(lecture.startTime)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAuraColors.current.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lecture.courseName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (lecture.reminderEnabled) {
                                        LocalAuraColors.current.textPrimary
                                    } else {
                                        LocalAuraColors.current.textMuted
                                    }
                                )
                                Text(
                                    text = if (lecture.reminderEnabled) {
                                        "${lecture.venue} • Tone: ${effectiveTone.displayName}"
                                    } else {
                                        "${lecture.venue} • Alarm Muted"
                                    },
                                    fontSize = 11.sp,
                                    color = if (lecture.reminderEnabled) {
                                        LocalAuraColors.current.secondaryAccent
                                    } else {
                                        LocalAuraColors.current.textMuted
                                    }
                                )
                            }

                            Switch(
                                checked = lecture.reminderEnabled,
                                onCheckedChange = { onToggleLecture(lecture) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LocalAuraColors.current.primaryAccent
                                ),
                                modifier = Modifier.testTag("alarm_manager_toggle_${lecture.id}")
                            )
                        }

                        if (lecture.reminderEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(leadOptions) { mins ->
                                    val isSelected = lecture.reminderMinutesBefore == mins
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) {
                                            LocalAuraColors.current.primaryAccent.copy(alpha = 0.35f)
                                        } else {
                                            Color(0x16FFFFFF)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) LocalAuraColors.current.secondaryAccent else Color(0x30FFFFFF)
                                        ),
                                        modifier = Modifier.clickable {
                                            onUpdateConfig(lecture, true, mins, lecture.alarmToneId)
                                        }
                                    ) {
                                        Text(
                                            text = if (mins == 0) "At start" else "${mins}m early",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else LocalAuraColors.current.textSecondary,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmSoundStudioTab(
    selectedDefaultToneId: String,
    customFileName: String,
    customFilePath: String,
    previewingToneId: String?,
    onSelectDefaultTone: (String) -> Unit,
    onPreviewTone: (String) -> Unit,
    onUploadCustomAudio: () -> Unit,
    onClearCustomAudio: () -> Unit,
    onTestLiveAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val builtInTones = remember {
        listOf(
            AlarmToneOption.AURORA_CHIME,
            AlarmToneOption.SUNRISE_HARP,
            AlarmToneOption.ACADEMIC_BELL,
            AlarmToneOption.QUANTUM_PULSE,
            AlarmToneOption.ZEN_BOWL,
            AlarmToneOption.SYSTEM_ALARM
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("alarm_sounds_lazy_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Custom Audio File Upload Card
        item {
            AuraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                borderColor = if (selectedDefaultToneId == AlarmToneOption.CUSTOM_AUDIO.id) {
                    LocalAuraColors.current.secondaryAccent
                } else {
                    Color(0x38FFFFFF)
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = LocalAuraColors.current.secondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Alarm Audio (MP3 / WAV / M4A)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = if (customFilePath.isNotBlank()) {
                                    "Loaded: $customFileName"
                                } else {
                                    "Pick any song or ringtone from your phone"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuraGlassButton(
                            text = if (customFilePath.isNotBlank()) "Change Audio" else "Upload Audio File",
                            icon = Icons.Default.UploadFile,
                            isPrimary = customFilePath.isBlank(),
                            onClick = onUploadCustomAudio,
                            modifier = Modifier.weight(1f),
                            testTag = "upload_custom_alarm_sound_button"
                        )

                        if (customFilePath.isNotBlank()) {
                            val isCustomSelected = selectedDefaultToneId == AlarmToneOption.CUSTOM_AUDIO.id
                            AuraGlassButton(
                                text = if (isCustomSelected) "Selected" else "Use This",
                                isPrimary = isCustomSelected,
                                onClick = { onSelectDefaultTone(AlarmToneOption.CUSTOM_AUDIO.id) },
                                testTag = "select_alarm_tone_CUSTOM_AUDIO"
                            )
                            AuraGlassIconButton(
                                icon = if (previewingToneId == AlarmToneOption.CUSTOM_AUDIO.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Preview Custom Alarm",
                                onClick = { onPreviewTone(AlarmToneOption.CUSTOM_AUDIO.id) },
                                size = 48.dp,
                                tint = LocalAuraColors.current.secondaryAccent,
                                testTag = "preview_alarm_tone_CUSTOM_AUDIO"
                            )
                            IconButton(
                                onClick = onClearCustomAudio,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("remove_custom_alarm_sound_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Custom Alarm",
                                    tint = LocalAuraColors.current.danger
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "AURA Harmonic & System Alarm Tones",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.secondaryAccent
            )
        }

        items(builtInTones, key = { it.id }) { option ->
            val isSelected = selectedDefaultToneId.equals(option.id, ignoreCase = true)
            val isPreviewing = previewingToneId == option.id

            AuraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_alarm_tone_${option.id}"),
                cornerRadius = 18.dp,
                borderColor = if (isSelected) LocalAuraColors.current.secondaryAccent else Color(0x28FFFFFF),
                onClick = { onSelectDefaultTone(option.id) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) LocalAuraColors.current.primaryAccent.copy(alpha = 0.30f)
                                else Color(0x18FFFFFF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isSelected) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    AuraGlassIconButton(
                        icon = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewing) "Stop Preview" else "Preview ${option.displayName}",
                        onClick = { onPreviewTone(option.id) },
                        size = 48.dp,
                        iconSize = 20.dp,
                        isPrimary = isPreviewing,
                        tint = LocalAuraColors.current.secondaryAccent,
                        testTag = "preview_alarm_tone_${option.id}"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            AuraGlassButton(
                text = "Test Live Single-Stream Alarm Now",
                icon = Icons.Default.NotificationsActive,
                isPrimary = true,
                onClick = onTestLiveAlarm,
                modifier = Modifier.fillMaxWidth(),
                testTag = "alarm_studio_test_live_button"
            )
        }
    }
}
