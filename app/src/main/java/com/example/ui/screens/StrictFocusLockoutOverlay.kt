package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.theme.LocalAuraColors
import com.example.util.SoundscapeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun StrictFocusLockoutOverlay(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val remainingSeconds by viewModel.focusRemainingSeconds.collectAsState()
    val isRunning by viewModel.isFocusRunning.collectAsState()
    val plannedDuration by viewModel.focusDurationMinutes.collectAsState()
    val selectedCourse by viewModel.focusSelectedCourse.collectAsState()
    val currentSoundscape by viewModel.activeSoundscape.collectAsState()
    val sessionGoal by viewModel.sessionGoalText.collectAsState()
    val currentTrack by viewModel.musicPlayer.currentTrack.collectAsState()
    val isMusicPlaying by viewModel.musicPlayer.isPlaying.collectAsState()
    val focusTracks by viewModel.focusTracks.collectAsState()

    var showBackWarning by remember { mutableStateOf(false) }
    var isOledPureBlack by remember { mutableStateOf(false) }
    var emergencyHoldProgress by remember { mutableFloatStateOf(0f) }
    var isHoldingEmergency by remember { mutableStateOf(false) }

    // Intercept hardware/gesture Back Button completely!
    BackHandler(enabled = true) {
        showBackWarning = true
    }

    LaunchedEffect(showBackWarning) {
        if (showBackWarning) {
            delay(3500)
            showBackWarning = false
        }
    }

    // Emergency hold timer (10 seconds continuous hold)
    LaunchedEffect(isHoldingEmergency) {
        if (isHoldingEmergency) {
            val startTime = System.currentTimeMillis()
            val totalRequiredMs = 10000L
            while (isActive && isHoldingEmergency) {
                val elapsed = System.currentTimeMillis() - startTime
                emergencyHoldProgress = (elapsed.toFloat() / totalRequiredMs).coerceIn(0f, 1f)
                if (emergencyHoldProgress >= 1f) {
                    viewModel.abortStrictLockout()
                    break
                }
                delay(40)
            }
        } else {
            emergencyHoldProgress = 0f
        }
    }

    // Breathing ring animation for zen focus
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    // Motivational academic focus quotes
    val quotes = listOf(
        "Deep work is the superpower of the 21st century.",
        "Your future self will thank you for this hour.",
        "Discipline is choosing what you want most over what you want now.",
        "Lock in. All notifications and distractions are silenced.",
        "Small daily disciplines compound into academic mastery."
    )
    val quoteIndex = remember { (System.currentTimeMillis() / 60000 % quotes.size).toInt() }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", mins, secs)

    val backgroundColor = if (isOledPureBlack) Color.Black else Color(0xFF090C12)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("strict_focus_lockout_screen")
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar: Strict Lock Badge & Battery Saver Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x280A84FF))
                        .border(1.dp, Color(0x600A84FF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Strict Lock",
                        tint = Color(0xFF64D2FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PHONE STRICT LOCKOUT ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64D2FF),
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { isOledPureBlack = !isOledPureBlack },
                    modifier = Modifier.testTag("toggle_oled_mode")
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness2,
                        contentDescription = "OLED Pure Black Mode",
                        tint = if (isOledPureBlack) Color(0xFF0A84FF) else Color(0x80FFFFFF)
                    )
                }
            }

            // Warning Banner if student presses back
            AnimatedVisibility(
                visible = showBackWarning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE01E40AF))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOCKED IN: Navigation is blocked. Hold abort for 10s if critical.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Center Zen Timer with Breathing Glow Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(260.dp)
                        .scale(breatheScale)
                ) {
                    // Outer atmospheric glow
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(CircleShape)
                            .background(Color(0x0CFFFFFF))
                    )

                    // Frosted circular bezel
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(CircleShape)
                            .background(Color(0x18FFFFFF))
                            .border(1.5.dp, Color(0x35FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedTime,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFF4F6FF),
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedCourse ?: "UNIVERSITY DEEP WORK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0A84FF),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "\"${quotes[quoteIndex]}\"",
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFFA0AAC0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Bottom Panel: Native Soundscape Switcher & Emergency Abort
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (sessionGoal.isNotBlank()) {
                    Text(
                        text = "Goal: $sessionGoal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64D2FF),
                        textAlign = TextAlign.Center
                    )
                }

                // Custom Study Music quick toggle if tracks exist
                if (focusTracks.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1E0A84FF))
                            .border(1.dp, Color(0x4564D2FF), RoundedCornerShape(16.dp))
                            .pointerInput(focusTracks) {
                                detectTapGestures {
                                    viewModel.musicPlayer.togglePlayPause(focusTracks)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Study Music",
                            tint = Color(0xFF64D2FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentTrack?.title ?: focusTracks.first().title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Soundscape quick selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Soundscape",
                        tint = Color(0xFF42D6C5),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Soundscape: ${currentSoundscape.displayName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (currentSoundscape == SoundscapeType.OFF) "Tap: Lo-Fi" else "Tap: Next",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64D2FF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x200A84FF))
                            .pointerInput(currentSoundscape) {
                                detectTapGestures {
                                    val entries = SoundscapeType.entries
                                    val nextIndex = (entries.indexOf(currentSoundscape) + 1) % entries.size
                                    viewModel.setSoundscape(entries[nextIndex])
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // 10-Second Continuous Hold Emergency Abort Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(
                            1.dp,
                            if (isHoldingEmergency) Color(0xFF0A84FF) else Color(0x35FFFFFF),
                            RoundedCornerShape(28.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isHoldingEmergency = true
                                    tryAwaitRelease()
                                    isHoldingEmergency = false
                                }
                            )
                        }
                        .testTag("emergency_abort_hold_button"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Fill bar that grows as student holds
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(emergencyHoldProgress)
                            .height(56.dp)
                            .background(Color(0xCC0A84FF))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isHoldingEmergency) Color.White else Color(0xFFA0AAC0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHoldingEmergency) {
                                "HOLDING... ${(10 - (emergencyHoldProgress * 10)).toInt()}s TO ABORT"
                            } else {
                                "Hold for 10s for Emergency Unlock"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHoldingEmergency) Color.White else Color(0xFFA0AAC0)
                        )
                    }
                }

                Text(
                    text = "Screen stays active • Place face down to minimize distractions",
                    fontSize = 11.sp,
                    color = Color(0x65A0AAC0)
                )
            }
        }
    }
}
