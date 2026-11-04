package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.theme.LocalAuraColors

@Composable
fun OnboardingScreen(
    onFinishOnboarding: (name: String, inst: String, prog: String, lvl: String, sem: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) } // 1: Welcome & Identity, 2: Academic Info, 3: Reminders & Permissions
    var name by remember { mutableStateOf("Collins") }
    var institution by remember { mutableStateOf("University of Ghana") }
    var programme by remember { mutableStateOf("BSc. Computer Science") }
    var level by remember { mutableStateOf("Level 300") }
    var semester by remember { mutableStateOf("Semester 1") }

    FrostedGlassBackground(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // AURA Logo & Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = LocalAuraColors.current.primaryAccent,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "AURA",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.primaryAccent,
                letterSpacing = 2.sp
            )
            Text(
                text = "The Intelligent Academic Companion",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAuraColors.current.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                1 -> {
                    AuraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Welcome, Student",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = "Let's set up your personalized academic profile to tailor your timetable, reminders, and study briefings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textSecondary,
                                lineHeight = 18.sp
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Your Preferred Name") },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_input_name")
                            )
                            OutlinedTextField(
                                value = institution,
                                onValueChange = { institution = it },
                                label = { Text("University / Institution") },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_input_inst")
                            )
                        }
                    }
                }
                2 -> {
                    AuraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Academic Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            OutlinedTextField(
                                value = programme,
                                onValueChange = { programme = it },
                                label = { Text("Degree / Programme of Study") },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_input_prog")
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = level,
                                    onValueChange = { level = it },
                                    label = { Text("Level / Year") },
                                    modifier = Modifier.weight(1f).testTag("onboarding_input_level")
                                )
                                OutlinedTextField(
                                    value = semester,
                                    onValueChange = { semester = it },
                                    label = { Text("Semester") },
                                    modifier = Modifier.weight(1f).testTag("onboarding_input_sem")
                                )
                            }
                        }
                    }
                }
                3 -> {
                    AuraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Timetable & Native Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LocalAuraColors.current.textPrimary
                            )
                            Text(
                                text = "AURA uses native Android AlarmManager and Notification Channels so you never miss a lecture or assignment deadline, even when the app is closed or after a device restart.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAuraColors.current.textSecondary,
                                lineHeight = 18.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, null, tint = LocalAuraColors.current.secondaryAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("High-priority lecture & exam alerts", style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, null, tint = LocalAuraColors.current.warning, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Snooze & dismiss actions on alarms", style = MaterialTheme.typography.bodySmall, color = LocalAuraColors.current.textPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Liquid Glass Controls
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuraGlassButton(
                text = if (step == 3) "Get Started with AURA" else "Continue",
                icon = if (step == 3) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                isPrimary = true,
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onFinishOnboarding(name, institution, programme, level, semester)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                testTag = "onboarding_next_button"
            )

            if (step < 3) {
                AuraGlassButton(
                    text = "Skip Setup & Use Defaults",
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    testTag = "onboarding_skip_button"
                )
            }
        }
    }
    }
}
