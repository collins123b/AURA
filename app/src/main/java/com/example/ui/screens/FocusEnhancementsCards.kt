package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.LocalAuraColors
import com.example.util.MusicLoopMode
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun FocusSessionGoalBannerCard(viewModel: AuraViewModel) {
    val sessionGoal by viewModel.sessionGoalText.collectAsState()
    val auraColors = LocalAuraColors.current

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_session_goal_card"),
        cornerRadius = 22.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_aura_hero_glass_1793566791688),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (auraColors.isDark) 0.24f else 0.14f)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deep Focus Target",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AuraBadge(
                        text = if (sessionGoal.isBlank()) "Optional Intent" else "Goal Locked",
                        textColor = auraColors.secondaryAccent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sessionGoal,
                    onValueChange = { viewModel.setSessionGoalText(it) },
                    placeholder = {
                        Text(
                            text = "e.g., Master Chapter 4 Deadlocks & solve 5 past questions",
                            fontSize = 12.sp,
                            color = auraColors.textMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = auraColors.secondaryAccent,
                        unfocusedBorderColor = Color(0x45FFFFFF),
                        focusedContainerColor = Color(0x1CFFFFFF),
                        unfocusedContainerColor = Color(0x12FFFFFF),
                        focusedTextColor = auraColors.textPrimary,
                        unfocusedTextColor = auraColors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_session_goal_input")
                )
            }
        }
    }
}

@Composable
fun FocusCustomMusicPlayerCard(viewModel: AuraViewModel) {
    val tracks by viewModel.focusTracks.collectAsState()
    val currentTrack by viewModel.musicPlayer.currentTrack.collectAsState()
    val isPlaying by viewModel.musicPlayer.isPlaying.collectAsState()
    val positionSec by viewModel.musicPlayer.currentPositionSec.collectAsState()
    val durationSec by viewModel.musicPlayer.durationSec.collectAsState()
    val volume by viewModel.musicPlayer.volume.collectAsState()
    val loopMode by viewModel.musicPlayer.loopMode.collectAsState()
    val isShuffle by viewModel.musicPlayer.isShuffleEnabled.collectAsState()
    val auraColors = LocalAuraColors.current

    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tracks) {
        viewModel.musicPlayer.updatePlaylist(tracks)
    }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFocusMusicFromUri(uri) { _, msg ->
                feedbackMessage = msg
            }
        }
    }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_custom_music_card"),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    auraColors.primaryAccent.copy(alpha = 0.35f),
                                    auraColors.secondaryAccent.copy(alpha = 0.25f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Personal Study Music Deck",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary
                    )
                    Text(
                        text = "Play your MP3s/Lo-Fi & layer with soundscapes",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = auraColors.textSecondary
                    )
                }
                AuraGlassButton(
                    text = "Add Music",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    cornerRadius = 14.dp,
                    onClick = { openAudioLauncher.launch(arrayOf("audio/*")) },
                    testTag = "add_focus_music_button"
                )
            }

            AnimatedVisibility(visible = feedbackMessage != null) {
                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = auraColors.secondaryAccent,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Now Playing Glass Console
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (auraColors.isDark) Color(0x24FFFFFF) else Color(0x55FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x42FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val activeTitle = currentTrack?.title
                        ?: tracks.firstOrNull()?.title
                        ?: "No custom study track loaded yet"
                    val activeSubtitle = currentTrack?.artistOrCategory
                        ?: if (tracks.isNotEmpty()) "Tap Play or select a track below" else "Tap 'Add Music' to import MP3, M4A, WAV, or OGG"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isPlaying) auraColors.secondaryAccent else auraColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = auraColors.textPrimary
                            )
                            Text(
                                text = activeSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = auraColors.textSecondary
                            )
                        }
                        if (isPlaying) {
                            AuraBadge(
                                text = "PLAYING",
                                textColor = auraColors.secondaryAccent
                            )
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        val safeDuration = durationSec.coerceAtLeast(1)
                        val sliderPos = positionSec.coerceIn(0, safeDuration).toFloat()

                        Slider(
                            value = sliderPos,
                            onValueChange = { viewModel.musicPlayer.seekToSeconds(it.toInt()) },
                            valueRange = 0f..safeDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = auraColors.secondaryAccent,
                                activeTrackColor = auraColors.primaryAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("focus_music_seek_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatAudioSeconds(positionSec),
                                fontSize = 10.sp,
                                color = auraColors.textMuted
                            )
                            Text(
                                text = formatAudioSeconds(durationSec),
                                fontSize = 10.sp,
                                color = auraColors.textMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Transport Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraGlassIconButton(
                                icon = Icons.Default.Shuffle,
                                contentDescription = "Toggle Shuffle",
                                onClick = { viewModel.musicPlayer.toggleShuffle() },
                                size = 48.dp,
                                tint = if (isShuffle) auraColors.secondaryAccent else auraColors.textMuted,
                                testTag = "focus_music_shuffle_button"
                            )
                            AuraGlassIconButton(
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                onClick = { viewModel.musicPlayer.skipPrevious(tracks) },
                                size = 48.dp,
                                tint = auraColors.textPrimary,
                                testTag = "focus_music_prev_button"
                            )
                            AuraGlassIconButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Study Music" else "Play Study Music",
                                onClick = { viewModel.musicPlayer.togglePlayPause(tracks) },
                                size = 54.dp,
                                tint = auraColors.secondaryAccent,
                                testTag = "focus_music_play_pause_button"
                            )
                            AuraGlassIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                onClick = { viewModel.musicPlayer.skipNext(tracks) },
                                size = 48.dp,
                                tint = auraColors.textPrimary,
                                testTag = "focus_music_next_button"
                            )
                            AuraGlassIconButton(
                                icon = if (loopMode == MusicLoopMode.LOOP_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Loop Mode: ${loopMode.label}",
                                onClick = { viewModel.musicPlayer.cycleLoopMode() },
                                size = 48.dp,
                                tint = if (loopMode != MusicLoopMode.OFF) auraColors.secondaryAccent else auraColors.textMuted,
                                testTag = "focus_music_loop_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Music Volume Mixer Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = auraColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Music Vol ${(volume * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = auraColors.textSecondary,
                                modifier = Modifier.width(82.dp)
                            )
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.musicPlayer.setVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = auraColors.secondaryAccent,
                                    activeTrackColor = auraColors.primaryAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_music_volume_slider")
                            )
                        }
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tracks.forEach { track ->
                        val isTrackSelected = currentTrack?.id == track.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isTrackSelected) auraColors.primaryAccent.copy(alpha = 0.22f)
                                    else Color(0x14FFFFFF)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isTrackSelected) auraColors.secondaryAccent.copy(alpha = 0.6f)
                                    else Color(0x28FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.musicPlayer.playTrack(track, tracks) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("focus_track_row_${track.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTrackSelected && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isTrackSelected) auraColors.secondaryAccent else auraColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = auraColors.textPrimary
                                )
                                Text(
                                    text = "${track.artistOrCategory} • ${formatAudioSeconds(track.durationSeconds)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = auraColors.textMuted
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleFavoriteFocusTrack(track) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("favorite_focus_track_${track.id}")
                            ) {
                                Icon(
                                    imageVector = if (track.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite ${track.title}",
                                    tint = if (track.isFavorite) Color(0xFFFBBF24) else auraColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteFocusTrack(track) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_focus_track_${track.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove ${track.title}",
                                    tint = auraColors.danger,
                                    modifier = Modifier.size(18.dp)
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
fun GuidedBoxBreathingCard() {
    val auraColors = LocalAuraColors.current
    var isBreathingActive by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) } // 0: Inhale, 1: Hold, 2: Exhale, 3: Hold
    var secondsLeftInPhase by remember { mutableIntStateOf(4) }
    var completedCycles by remember { mutableIntStateOf(0) }

    val phases = listOf("Inhale Slowly", "Hold Gently", "Exhale Smoothly", "Hold Calmly")

    LaunchedEffect(isBreathingActive) {
        if (isBreathingActive) {
            phaseIndex = 0
            secondsLeftInPhase = 4
            while (isBreathingActive) {
                delay(1000L)
                if (secondsLeftInPhase > 1) {
                    secondsLeftInPhase -= 1
                } else {
                    secondsLeftInPhase = 4
                    phaseIndex = (phaseIndex + 1) % 4
                    if (phaseIndex == 0) {
                        completedCycles += 1
                    }
                }
            }
        }
    }

    val targetOrbScale = when {
        !isBreathingActive -> 0.88f
        phaseIndex == 0 || phaseIndex == 1 -> 1.12f
        else -> 0.78f
    }
    val orbScale by animateFloatAsState(
        targetValue = targetOrbScale,
        animationSpec = tween(durationMillis = 900),
        label = "breathingOrbScale"
    )

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_breathing_card"),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = auraColors.secondaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4-4-4-4 Box Breathing Warm-Up",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary
                    )
                    Text(
                        text = "Lower exam anxiety & prime concentration in 60s",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = auraColors.textSecondary
                    )
                }
                if (completedCycles > 0) {
                    AuraBadge(
                        text = "$completedCycles cycles",
                        textColor = auraColors.secondaryAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(orbScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        auraColors.secondaryAccent.copy(alpha = 0.55f),
                                        auraColors.primaryAccent.copy(alpha = 0.22f)
                                    )
                                )
                            )
                            .border(1.2.dp, Color(0x75FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBreathingActive) "${secondsLeftInPhase}s" else "4s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBreathingActive) phases[phaseIndex] else "Ready to Center Mind",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = "4s Inhale • 4s Hold • 4s Exhale • 4s Hold",
                            style = MaterialTheme.typography.labelSmall,
                            color = auraColors.textMuted
                        )
                    }
                }

                AuraGlassButton(
                    text = if (isBreathingActive) "Stop" else "Breathe",
                    icon = if (isBreathingActive) Icons.Default.Stop else Icons.Default.Air,
                    isPrimary = !isBreathingActive,
                    cornerRadius = 16.dp,
                    onClick = { isBreathingActive = !isBreathingActive },
                    testTag = "toggle_box_breathing_button"
                )
            }
        }
    }
}

@Composable
fun DistractionParkingLotCard(viewModel: AuraViewModel) {
    val distractions by viewModel.focusDistractions.collectAsState()
    val auraColors = LocalAuraColors.current
    var thoughtDraft by remember { mutableStateOf("") }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_distraction_pad_card"),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = auraColors.aiAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Distraction Parking Lot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.textPrimary
                    )
                    Text(
                        text = "Park random mid-study thoughts here & stay locked in",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = auraColors.textSecondary
                    )
                }
                if (distractions.any { it.isResolved }) {
                    AuraGlassButton(
                        text = "Clear Done",
                        isPrimary = false,
                        cornerRadius = 12.dp,
                        onClick = { viewModel.clearResolvedFocusDistractions() },
                        testTag = "clear_resolved_distractions_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = thoughtDraft,
                    onValueChange = { thoughtDraft = it },
                    placeholder = {
                        Text(
                            text = "Park a distracting thought or reminder...",
                            fontSize = 12.sp,
                            color = auraColors.textMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = auraColors.secondaryAccent,
                        unfocusedBorderColor = Color(0x42FFFFFF),
                        focusedContainerColor = Color(0x1AFFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF),
                        focusedTextColor = auraColors.textPrimary,
                        unfocusedTextColor = auraColors.textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("distraction_thought_input")
                )

                AuraGlassButton(
                    text = "Park",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    cornerRadius = 14.dp,
                    onClick = {
                        if (thoughtDraft.isNotBlank()) {
                            viewModel.addFocusDistraction(thoughtDraft)
                            thoughtDraft = ""
                        }
                    },
                    testTag = "add_distraction_thought_button"
                )
            }

            if (distractions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    distractions.take(8).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x16FFFFFF))
                                .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("distraction_row_${item.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleFocusDistractionResolved(item) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("resolve_distraction_${item.id}")
                            ) {
                                Icon(
                                    imageVector = if (item.isResolved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Mark distraction resolved",
                                    tint = if (item.isResolved) auraColors.secondaryAccent else auraColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.thoughtText,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (item.isResolved) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (item.isResolved) auraColors.textMuted else auraColors.textPrimary
                                )
                                Text(
                                    text = item.courseCode,
                                    fontSize = 10.sp,
                                    color = auraColors.secondaryAccent
                                )
                            }
                            if (!item.isResolved) {
                                IconButton(
                                    onClick = { viewModel.convertDistractionToStudyNote(item) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("convert_distraction_note_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Save thought to Notes Vault",
                                        tint = auraColors.aiAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteFocusDistraction(item) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_distraction_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete parked thought",
                                    tint = auraColors.danger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAudioSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
