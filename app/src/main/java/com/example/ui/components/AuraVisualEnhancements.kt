package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LectureEntity
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun performAuraHaptic(view: View) {
    runCatching {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

/**
 * Animated Liquid Glass Circular Progress Ring for Bento overview cards and momentum tracking.
 */
@Composable
fun AuraCircularProgressRing(
    progress: Float,
    centerLabel: String,
    ringSize: Dp = 54.dp,
    strokeWidth: Dp = 5.5.dp,
    accentColor: Color = LocalAuraColors.current.primaryAccent,
    secondaryColor: Color = LocalAuraColors.current.secondaryAccent,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "auraProgressSweep"
    )
    val auraColors = LocalAuraColors.current

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = if (auraColors.isDark) Color(0x28FFFFFF) else Color(0x200A84FF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedSweep > 1f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(accentColor, secondaryColor, accentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = centerLabel,
            fontFamily = AuraMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = auraColors.textPrimary
        )
    }
}

/**
 * Animated 5-bar live soundwave equalizer rendered only when Focus Soundscape,
 * Study Music, or Audio Notes are actively playing.
 */
@Composable
fun AuraLiveAudioEqualizer(
    isPlaying: Boolean,
    accentColor: Color = LocalAuraColors.current.secondaryAccent,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) {
        Row(
            modifier = modifier.height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.35f))
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "audioEqualizer")
    val barDurations = listOf(420, 310, 480, 350, 440)
    val barFractions = barDurations.mapIndexed { idx, dur ->
        val anim by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = dur, delayMillis = idx * 45),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eqBar_$idx"
        )
        anim
    }

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barFractions.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((18f * frac).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor, LocalAuraColors.current.primaryAccent)
                        )
                    )
            )
        }
    }
}

/**
 * Computes a live ticking countdown string (e.g., "01h : 24m : 18s" or "2d 04h : 12m")
 * until the next scheduled occurrence of the given lecture.
 */
fun computeLiveCountdownToLecture(
    lecture: LectureEntity,
    now: LocalDateTime = LocalDateTime.now()
): String {
    return runCatching {
        val parts = lecture.startTime.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        val targetDow = DayOfWeek.of(lecture.dayOfWeek.coerceIn(1, 7))
        var targetDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(targetDow))
        var candidate = targetDate.atTime(LocalTime.of(h, m))
        if (!candidate.isAfter(now)) {
            targetDate = targetDate.plusWeeks(1)
            candidate = targetDate.atTime(LocalTime.of(h, m))
        }
        val secondsLeft = Duration.between(now, candidate).seconds.coerceAtLeast(0L)
        val days = secondsLeft / 86_400L
        val hours = (secondsLeft % 86_400L) / 3600L
        val mins = (secondsLeft % 3600L) / 60L
        val secs = secondsLeft % 60L
        if (days > 0) {
            String.format(Locale.US, "%dd %02dh : %02dm : %02ds", days, hours, mins, secs)
        } else {
            String.format(Locale.US, "%02dh : %02dm : %02ds", hours, mins, secs)
        }
    }.getOrDefault("Scheduled")
}
