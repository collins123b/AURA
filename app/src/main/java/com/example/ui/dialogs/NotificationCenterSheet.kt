package com.example.ui.dialogs

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NotificationHistoryEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.LocalAuraColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    viewModel: AuraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notifications by viewModel.notifications.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2101B30),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color.Transparent)),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .padding(20.dp)
                .testTag("notification_center_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.22f))
                        .border(1.dp, Color(0x55FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = LocalAuraColors.current.secondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Centre",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = "${notifications.count { !it.isRead }} unread alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }

                AuraGlassIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Clear notifications",
                    onClick = { viewModel.clearNotifications() },
                    size = 42.dp,
                    iconSize = 18.dp,
                    tint = LocalAuraColors.current.danger,
                    testTag = "clear_notifications_button"
                )

                Spacer(modifier = Modifier.width(8.dp))

                AuraGlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    size = 42.dp,
                    iconSize = 18.dp,
                    tint = LocalAuraColors.current.textPrimary,
                    testTag = "close_notifications_button"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuraGlassButton(
                    text = "Test Ringing Alarm",
                    icon = Icons.Default.NotificationsActive,
                    isPrimary = true,
                    onClick = {
                        viewModel.triggerTestAlarm()
                        Toast.makeText(
                            context,
                            "Test lecture alarm ringing in 1 second!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.3f),
                    testTag = "test_lecture_alarm_button"
                )

                AuraGlassButton(
                    text = "Mark All Read",
                    onClick = { viewModel.markAllNotificationsRead() },
                    modifier = Modifier.weight(1f),
                    testTag = "mark_all_read_button"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (notifications.isEmpty()) {
                AuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No notification history yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalAuraColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scheduled lecture alarms, office hour alerts, and deadline reminders will ring audibly and appear here. Tap 'Test Ringing Alarm' above to verify right now!",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalAuraColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationHistoryRow(
                            notification = item,
                            onMarkRead = { viewModel.markNotificationRead(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationHistoryRow(
    notification: NotificationHistoryEntity,
    onMarkRead: () -> Unit
) {
    val date = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(notification.triggerEpochMillis),
        ZoneId.systemDefault()
    )
    val timeFormatted = date.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US))

    val (icon, color) = when (notification.category) {
        "EXAMINATION" -> Icons.Default.Alarm to LocalAuraColors.current.danger
        "ASSIGNMENT" -> Icons.Default.Assignment to LocalAuraColors.current.warning
        "STUDY", "FOCUS" -> Icons.Default.Timer to LocalAuraColors.current.secondaryAccent
        else -> Icons.Default.School to LocalAuraColors.current.primaryAccent
    }

    AuraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = if (notification.isRead) null else LocalAuraColors.current.secondaryAccent.copy(alpha = 0.55f),
        onClick = onMarkRead
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
        }
    }
}
