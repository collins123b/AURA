package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.LocalAuraColors

@Composable
fun AboutDeveloperCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var emailCopied by remember { mutableStateOf(false) }

    val appName = stringResource(id = R.string.app_name)
    val aboutTitle = stringResource(id = R.string.about_developer_title)
    val developerName = stringResource(id = R.string.developer_name)
    val developerEmail = stringResource(id = R.string.developer_email)
    val versionDisplay = stringResource(id = R.string.app_version_display)

    val launchEmailIntent = {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$developerEmail")
                putExtra(Intent.EXTRA_SUBJECT, "$appName ($versionDisplay) — Feedback & Support")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    val copyEmailToClipboard = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Developer Email", developerEmail))
        emailCopied = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AuraCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_developer_card"),
            cornerRadius = 20.dp,
            onClick = { showAboutDialog = true }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        auraColors.primaryAccent.copy(alpha = 0.24f),
                                        auraColors.secondaryAccent.copy(alpha = 0.20f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = aboutTitle,
                            tint = auraColors.primaryAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = aboutTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = "$appName Academic Companion",
                            style = MaterialTheme.typography.bodySmall,
                            color = auraColors.textSecondary
                        )
                    }

                    AuraBadge(
                        text = versionDisplay,
                        backgroundColor = auraColors.primaryAccent.copy(alpha = 0.16f),
                        textColor = auraColors.primaryAccent
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open About Details",
                        tint = auraColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color(0x22FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AuraMintAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.developer_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = developerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = auraColors.textPrimary,
                        modifier = Modifier.testTag("developer_name_text")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.developer_email_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = developerEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = auraColors.secondaryAccent,
                        modifier = Modifier.testTag("developer_email_text")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraGlassButton(
                        text = stringResource(id = R.string.contact_developer_action),
                        icon = Icons.Default.Email,
                        isPrimary = true,
                        onClick = { launchEmailIntent() },
                        modifier = Modifier.weight(1f),
                        testTag = "contact_developer_email_button"
                    )
                    AuraGlassButton(
                        text = if (emailCopied) "Copied" else stringResource(id = R.string.copy_developer_email_action),
                        icon = if (emailCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        onClick = { copyEmailToClipboard() },
                        modifier = Modifier.weight(1f),
                        testTag = "copy_developer_email_button"
                    )
                }
            }
        }

        // Standard subtle app version footer below Settings cards
        Text(
            text = "$appName • $versionDisplay • Developed by $developerName",
            style = MaterialTheme.typography.labelSmall,
            color = auraColors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp)
                .testTag("about_footer_version_label")
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = auraColors.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "About $appName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = auraColors.textPrimary
                        )
                        Text(
                            text = "Version $versionDisplay",
                            style = MaterialTheme.typography.labelMedium,
                            color = auraColors.secondaryAccent
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Designed to empower university students with smart timetables, single-stream lecture alarms, study slide decks, spaced-repetition flashcards, focus timers, and cloud academic backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = Color(0x28FFFFFF))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "DEVELOPER INFORMATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.textMuted,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.developer_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = auraColors.textSecondary
                            )
                            Text(
                                text = developerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.textPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.developer_email_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = auraColors.textSecondary
                            )
                            Text(
                                text = developerEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = auraColors.secondaryAccent
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.app_version_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = auraColors.textSecondary
                            )
                            AuraBadge(
                                text = versionDisplay,
                                backgroundColor = auraColors.primaryAccent.copy(alpha = 0.18f),
                                textColor = auraColors.primaryAccent
                            )
                        }
                    }
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = stringResource(id = R.string.contact_developer_action),
                    icon = Icons.Default.Email,
                    isPrimary = true,
                    onClick = {
                        launchEmailIntent()
                        showAboutDialog = false
                    },
                    testTag = "dialog_email_developer_button"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Close",
                    onClick = { showAboutDialog = false },
                    testTag = "dialog_close_about_button"
                )
            },
            containerColor = if (auraColors.isDark) Color(0xF2101B30) else Color(0xF7F2F7FF),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x4564D2FF))),
                RoundedCornerShape(24.dp)
            )
        )
    }
}
