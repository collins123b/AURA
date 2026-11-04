package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.cloud.AuraCloudRepository
import com.example.data.cloud.RoomMessage
import com.example.data.cloud.RoomParticipant
import com.example.data.cloud.SharedFlashcard
import com.example.data.cloud.StudyRoom
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassButton
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.util.AuraQrCodeCanvas

@Composable
fun UniversalRoomInviteDialog(
    room: StudyRoom,
    webPortalUrl: String,
    onSaveWebPortalUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current
    val displayCode = room.joinCode.ifBlank { room.id.removePrefix("room_").take(6).uppercase() }
    var editablePortalUrl by remember(webPortalUrl) { mutableStateOf(webPortalUrl) }
    var showEditDomain by remember { mutableStateOf(false) }

    val browserRoomUrl = remember(editablePortalUrl, displayCode) {
        AuraCloudRepository.buildBrowserJoinUrl(editablePortalUrl, displayCode)
    }
    val voiceRoomUrl = remember(room.voiceRoomUrl, displayCode) {
        room.voiceRoomUrl.ifBlank { AuraCloudRepository.buildJitsiRoomUrl(displayCode) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF60C1528),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.2.dp, AuraMintAccent.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AuraBadge(
                            text = "IPHONE • CHROME • SAFARI • ANDROID",
                            backgroundColor = AuraMintAccent.copy(alpha = 0.22f),
                            textColor = AuraMintAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invite Anyone to ${room.courseCode}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Invite Dialog", tint = Color.White)
                    }
                }

                Text(
                    text = "Classmates on iPhone Safari, Chrome, Mac, or PC can scan this QR code or tap your link to join the live stage without installing an APK!",
                    fontSize = 11.5.sp,
                    color = auraColors.textSecondary,
                    textAlign = TextAlign.Center
                )

                AuraQrCodeCanvas(
                    content = browserRoomUrl,
                    qrSize = 186.dp,
                    contentDescriptionText = "QR Code for $browserRoomUrl",
                    modifier = Modifier.testTag("room_invite_qr_canvas")
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x20FFFFFF),
                    border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "6-CHAR ROOM CODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.secondaryAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = displayCode,
                            fontFamily = AuraMonoFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraMintAccent
                        )
                        Text(
                            text = browserRoomUrl,
                            fontSize = 10.5.sp,
                            color = auraColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Vercel Domain Customizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditDomain = !showEditDomain }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = auraColors.secondaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showEditDomain) "Hide Vercel Domain Settings" else "Set Your Vercel Web App Domain",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = auraColors.secondaryAccent
                    )
                }

                if (showEditDomain) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x16FFFFFF))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editablePortalUrl,
                            onValueChange = { editablePortalUrl = it },
                            label = { Text("Your Deployed Vercel URL") },
                            placeholder = { Text("https://your-aura.vercel.app") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AuraGlassButton(
                            text = "Save Vercel URL",
                            icon = Icons.Default.Check,
                            isPrimary = true,
                            onClick = {
                                onSaveWebPortalUrl(editablePortalUrl)
                                showEditDomain = false
                                onNotify("Saved Vercel Web Portal URL! QR & links updated.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraGlassButton(
                        text = "Copy Link",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("AURA Study Room Link", browserRoomUrl))
                            onNotify("Copied Browser & iPhone Join Link!")
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "copy_browser_link_button"
                    )
                    AuraGlassButton(
                        text = "Share Invite",
                        icon = Icons.Default.Share,
                        isPrimary = true,
                        onClick = {
                            val inviteText = buildString {
                                appendLine("🎓 Join my LIVE AURA Study Session (Works on iPhone, Chrome, Safari & Android)!")
                                appendLine("📚 ${room.courseCode}: ${room.roomTitle}")
                                appendLine("🎯 Topic: ${room.focusTopic}")
                                appendLine("")
                                appendLine("🌐 Join on iPhone / Any Browser (Timetable + Live Stage):")
                                appendLine(browserRoomUrl)
                                appendLine("")
                                appendLine("🎙️ Live Voice, Camera & Screen-Share Slides (Zero Install):")
                                appendLine(voiceRoomUrl)
                                appendLine("")
                                appendLine("🔑 AURA App 6-Char Code: $displayCode")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, inviteText)
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Invite Classmates (App or Browser)")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "share_universal_invite_button"
                    )
                }

                AuraGlassButton(
                    text = "Open Live Voice, Video & Slide Share",
                    icon = Icons.Default.HeadsetMic,
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(voiceRoomUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            onNotify("Unable to open browser for live audio room.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "open_live_voice_room_button"
                )
            }
        }
    }
}

@Composable
fun WaitingRoomQueueBanner(
    waitingParticipants: List<RoomParticipant>,
    onAdmit: (String) -> Unit,
    onDecline: (String) -> Unit,
    onAdmitAll: () -> Unit
) {
    if (waitingParticipants.isEmpty()) return
    val auraColors = LocalAuraColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = auraColors.warning.copy(alpha = 0.16f),
        border = BorderStroke(1.2.dp, auraColors.warning),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("waiting_room_queue_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Doorbell,
                        contentDescription = null,
                        tint = auraColors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Waiting Room (${waitingParticipants.size} Knocking)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                if (waitingParticipants.size > 1) {
                    Text(
                        text = "Admit All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraMintAccent,
                        modifier = Modifier.clickable { onAdmitAll() }
                    )
                }
            }

            waitingParticipants.forEach { guest ->
                val platformTag = when (guest.devicePlatform) {
                    "IPHONE_WEB" -> "📱 iPhone Browser"
                    "WEB_BROWSER" -> "🌐 Web Browser"
                    else -> "🤖 Android App"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x24000000))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guest.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${guest.cityOrCampus.ifBlank { "Remote" }} • $platformTag",
                            fontSize = 10.sp,
                            color = auraColors.textSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AuraMintAccent.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, AuraMintAccent),
                            modifier = Modifier.clickable { onAdmit(guest.userId) }
                        ) {
                            Text(
                                text = "Admit",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AuraMintAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = auraColors.danger.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, auraColors.danger),
                            modifier = Modifier.clickable { onDecline(guest.userId) }
                        ) {
                            Text(
                                text = "Remove",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.danger,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStagePollCard(
    room: StudyRoom,
    participants: List<RoomParticipant>,
    myVoteIndex: Int,
    canControlStage: Boolean,
    onVote: (Int) -> Unit,
    onToggleReveal: (Boolean) -> Unit,
    onClosePoll: () -> Unit
) {
    if (room.activePollQuestion.isBlank() || room.activePollOptions.isEmpty()) return
    val auraColors = LocalAuraColors.current
    val admittedParticipants = participants.filter { it.admissionStatus != "WAITING" && it.admissionStatus != "REMOVED" }
    val totalVotes = admittedParticipants.count { it.pollVoteIndex.toInt() in room.activePollOptions.indices }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x28152648),
        border = BorderStroke(1.2.dp, auraColors.aiAccent.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_stage_poll_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraBadge(
                    text = if (room.activePollCorrectIndex >= 0) "LIVE POP-QUIZ POLL • $totalVotes VOTE(S)" else "LIVE ROOM POLL • $totalVotes VOTE(S)",
                    backgroundColor = auraColors.aiAccent.copy(alpha = 0.24f),
                    textColor = auraColors.aiAccent
                )
                if (canControlStage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (room.activePollCorrectIndex >= 0) {
                            Text(
                                text = if (room.activePollRevealed) "Hide Answer" else "Reveal Answer",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AuraMintAccent,
                                modifier = Modifier.clickable { onToggleReveal(!room.activePollRevealed) }
                            )
                        }
                        Text(
                            text = "End",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.danger,
                            modifier = Modifier.clickable { onClosePoll() }
                        )
                    }
                }
            }

            Text(
                text = room.activePollQuestion,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            val letters = listOf("A", "B", "C", "D")
            room.activePollOptions.forEachIndexed { idx, optionText ->
                val count = admittedParticipants.count { it.pollVoteIndex.toInt() == idx }
                val pct = if (totalVotes > 0) (count * 100) / totalVotes else 0
                val isSelected = myVoteIndex == idx
                val isCorrectChoice = room.activePollRevealed && room.activePollCorrectIndex.toInt() == idx

                val borderCol = when {
                    isCorrectChoice -> AuraMintAccent
                    isSelected -> auraColors.secondaryAccent
                    else -> Color(0x30FFFFFF)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x20FFFFFF))
                        .border(1.2.dp, borderCol, RoundedCornerShape(12.dp))
                        .clickable { onVote(idx) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (pct / 100f).coerceIn(0f, 1f))
                            .matchParentSize()
                            .background(
                                if (isCorrectChoice) AuraMintAccent.copy(alpha = 0.26f)
                                else auraColors.primaryAccent.copy(alpha = 0.26f)
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${letters.getOrElse(idx) { "${idx + 1}" }}. $optionText" +
                                if (isCorrectChoice) " ✓" else "",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isCorrectChoice) FontWeight.ExtraBold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$pct% ($count)",
                            fontSize = 11.sp,
                            fontFamily = AuraMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrectChoice) AuraMintAccent else auraColors.secondaryAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStagePollDialog(
    onDismiss: () -> Unit,
    onLaunch: (question: String, options: List<String>, correctIdx: Int) -> Unit
) {
    val auraColors = LocalAuraColors.current
    var question by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var hasCorrectAnswer by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF50E182E),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, auraColors.aiAccent.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Launch Live Pop-Quiz / Stage Poll",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Appears instantly on every Android phone & iPhone/Chrome browser in the room.",
                    fontSize = 11.5.sp,
                    color = auraColors.textSecondary
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question Prompt") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasCorrectAnswer, onCheckedChange = { hasCorrectAnswer = it })
                    Text("Mark a Correct Answer (Pop-Quiz Mode)", fontSize = 12.sp, color = Color.White)
                }

                if (hasCorrectAnswer) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B", "C", "D").forEachIndexed { idx, label ->
                            val selected = correctIdx == idx
                            Surface(
                                shape = CircleShape,
                                color = if (selected) AuraMintAccent else Color(0x22FFFFFF),
                                modifier = Modifier.clickable { correctIdx = idx }
                            ) {
                                Text(
                                    text = "Ans: $label",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selected) Color(0xFF071320) else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AuraGlassButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    AuraGlassButton(
                        text = "Push Live",
                        isPrimary = true,
                        onClick = {
                            val opts = listOf(optA, optB, optC, optD).map { it.trim() }.filter { it.isNotBlank() }
                            if (question.isNotBlank() && opts.size >= 2) {
                                onLaunch(question.trim(), opts, if (hasCorrectAnswer) correctIdx else -1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SmartSessionRecapDialog(
    room: StudyRoom,
    participants: List<RoomParticipant>,
    messages: List<RoomMessage>,
    sharedCards: List<SharedFlashcard>,
    onSaveToStudyNotes: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current

    val recapText = remember(room, participants, messages, sharedCards) {
        buildString {
            appendLine("📋 AURA SMART STUDY MINUTES")
            appendLine("Course: ${room.courseCode} — ${room.roomTitle}")
            appendLine("Host: ${room.ownerName} • Sprint Target: ${room.activeMinutes} mins")
            appendLine("Topic: ${room.focusTopic}")
            appendLine("")
            appendLine("👥 ATTENDEES (${participants.size.coerceAtLeast(1)}):")
            participants.filter { it.admissionStatus != "REMOVED" }.forEach { p ->
                val badge = when (p.devicePlatform) {
                    "IPHONE_WEB" -> "iPhone Web"
                    "WEB_BROWSER" -> "Browser"
                    else -> "Android"
                }
                appendLine("• ${p.displayName} (${p.cityOrCampus.ifBlank { "Remote" }} • $badge)")
            }
            if (room.spotlightCardText.isNotBlank()) {
                appendLine("")
                appendLine("📌 SPOTLIGHTED EXAM FOCUS:")
                appendLine(room.spotlightCardText)
            }
            if (room.sharedBoardNote.isNotBlank()) {
                appendLine("")
                appendLine("📝 SHARED CHALKBOARD / FORMULAS:")
                appendLine(room.sharedBoardNote)
            }
            if (sharedCards.isNotEmpty()) {
                appendLine("")
                appendLine("🃏 FLASHCARDS DROPPED (${sharedCards.size}):")
                sharedCards.take(15).forEachIndexed { i, c ->
                    appendLine("${i + 1}. Q: ${c.question}")
                    appendLine("   A: ${c.answer}")
                }
            }
            if (messages.isNotEmpty()) {
                appendLine("")
                appendLine("💬 KEY LIVE Q&A HIGHLIGHTS:")
                messages.takeLast(10).forEach { m ->
                    appendLine("• ${m.authorName}: ${m.text}")
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF60C1528),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, AuraMintAccent.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Smart Study Session Recap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x22000000),
                    border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = recapText,
                        fontSize = 11.5.sp,
                        color = auraColors.textPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AuraGlassButton(
                        text = "Save to Notes",
                        icon = Icons.Default.BookmarkAdd,
                        isPrimary = true,
                        onClick = {
                            onSaveToStudyNotes("Meeting Recap: ${room.roomTitle}", recapText)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuraGlassButton(
                        text = "Share Recap",
                        icon = Icons.Default.Share,
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, recapText)
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Share Study Minutes")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                AuraGlassButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
