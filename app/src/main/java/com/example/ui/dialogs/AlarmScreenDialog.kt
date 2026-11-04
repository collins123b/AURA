package com.example.ui.dialogs

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.AlarmToneOption
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.LocalAuraColors

@Composable
fun AlarmScreenDialog(
    title: String,
    courseCode: String,
    timeText: String,
    message: String = "",
    alarmToneId: String = "",
    requireBrainCheck: Boolean = false,
    defaultSnoozeMinutes: Int = 10,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit = {},
    onSnoozeWithMinutes: ((Int) -> Unit)? = null
) {
    val pulseTransition = rememberInfiniteTransition(label = "alarmPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var selectedSnoozeMins by remember(defaultSnoozeMinutes) {
        mutableIntStateOf(defaultSnoozeMinutes.coerceIn(5, 30))
    }
    var showBrainCheckGate by remember { mutableStateOf(false) }
    val operandA = remember { 14 + (courseCode.hashCode().and(0x0F)) }
    val operandB = remember { 17 + (title.hashCode().and(0x0B)) }
    val expectedSum = operandA + operandB
    var brainAnswerInput by remember { mutableStateOf("") }
    var brainError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!requireBrainCheck) onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    LocalAuraColors.current.warning.copy(alpha = 0.38f),
                                    LocalAuraColors.current.primaryAccent.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(1.2.dp, Color(0x88FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Ringing Academic Alarm",
                        tint = LocalAuraColors.current.warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ringing Lecture Alarm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = "AURA Live Wakeup Alert",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraBadge(
                        text = courseCode,
                        backgroundColor = LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f),
                        textColor = LocalAuraColors.current.secondaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AuraBadge(
                        text = AlarmToneOption.fromId(alarmToneId).displayName,
                        backgroundColor = Color(0x1EFFFFFF),
                        textColor = LocalAuraColors.current.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scheduled Time: $timeText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalAuraColors.current.secondaryAccent
                )
                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Snooze Interval Selector (5m / 10m / 15m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Snooze:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalAuraColors.current.textMuted
                    )
                    listOf(5, 10, 15).forEach { mins ->
                        val active = selectedSnoozeMins == mins
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) LocalAuraColors.current.primaryAccent.copy(alpha = 0.28f) else Color(0x1AFFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (active) LocalAuraColors.current.secondaryAccent else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier
                                .clickable { selectedSnoozeMins = mins }
                                .testTag("alarm_snooze_chip_${mins}m")
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) LocalAuraColors.current.secondaryAccent else LocalAuraColors.current.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (showBrainCheckGate && requireBrainCheck) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AuraCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = LocalAuraColors.current.warning.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = LocalAuraColors.current.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Wake-Up Brain Check: Solve $operandA + $operandB",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAuraColors.current.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = brainAnswerInput,
                                onValueChange = {
                                    brainAnswerInput = it
                                    brainError = false
                                },
                                placeholder = { Text("Enter sum to stop alarm...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("alarm_brain_check_input")
                            )
                            if (brainError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Almost! Try $operandA + $operandB again.",
                                    fontSize = 11.sp,
                                    color = LocalAuraColors.current.danger
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AuraGlassButton(
                                text = "Verify & Stop Alarm",
                                icon = Icons.Default.Check,
                                isPrimary = true,
                                onClick = {
                                    if (brainAnswerInput.trim().toIntOrNull() == expectedSum) {
                                        onDismiss()
                                    } else {
                                        brainError = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "alarm_brain_check_verify_button"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AuraGlassButton(
                text = if (requireBrainCheck && !showBrainCheckGate) "Wake-Up Check" else "Stop Alarm",
                icon = Icons.Default.Check,
                isPrimary = true,
                onClick = {
                    if (requireBrainCheck && !showBrainCheckGate) {
                        showBrainCheckGate = true
                    } else if (requireBrainCheck) {
                        if (brainAnswerInput.trim().toIntOrNull() == expectedSum) {
                            onDismiss()
                        } else {
                            brainError = true
                        }
                    } else {
                        onDismiss()
                    }
                },
                testTag = "alarm_dialog_dismiss_button"
            )
        },
        dismissButton = {
            AuraGlassButton(
                text = "Snooze ${selectedSnoozeMins}m",
                icon = Icons.Default.Snooze,
                onClick = {
                    if (onSnoozeWithMinutes != null) {
                        onSnoozeWithMinutes(selectedSnoozeMins)
                    } else {
                        onSnooze()
                    }
                },
                testTag = "alarm_dialog_snooze_button"
            )
        },
        containerColor = Color(0xF2101B30),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.border(
            1.4.dp,
            Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x5564D2FF))),
            RoundedCornerShape(26.dp)
        )
    )
}
