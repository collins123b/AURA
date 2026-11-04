package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.cloud.AuraCloudRepository
import com.example.data.cloud.RoomMessage
import com.example.data.cloud.RoomParticipant
import com.example.data.cloud.SharedFlashcard
import com.example.data.cloud.StudyRoom
import com.example.ui.AuraViewModel
import com.example.ui.cloud.CloudSyncViewModel
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraCard
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.AuraMintAccent
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LiveRoomStageTab(val label: String) {
    WHITEBOARD("Pinboard"),
    LIVE_CHAT("Live Q&A"),
    SHARED_CARDS("Flashcards")
}

@Composable
fun CloudStudyRoomsView(
    viewModel: AuraViewModel,
    cloudVm: CloudSyncViewModel = viewModel()
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current
    val scope = rememberCoroutineScope()

    val currentUser by cloudVm.currentUser.collectAsStateWithLifecycle()
    val campusRooms by cloudVm.campusStudyRooms.collectAsStateWithLifecycle()
    val liveSelectedRoom by cloudVm.liveSelectedRoom.collectAsStateWithLifecycle()
    val selectedRoomId by cloudVm.selectedRoomId.collectAsStateWithLifecycle()
    val sharedCards by cloudVm.sharedCards.collectAsStateWithLifecycle()
    val participants by cloudVm.participants.collectAsStateWithLifecycle()
    val roomMessages by cloudVm.roomMessages.collectAsStateWithLifecycle()
    val statusMessage by cloudVm.statusMessage.collectAsStateWithLifecycle()
    val isBusy by cloudVm.isBusy.collectAsStateWithLifecycle()

    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showShareCardDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCreatePollDialog by remember { mutableStateOf(false) }
    var showSpotlightDialog by remember { mutableStateOf(false) }
    var showRecapDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var cityOrCampusInput by remember(userProfile.institution) {
        mutableStateOf(userProfile.institution.ifBlank { "Remote Campus" })
    }

    // Automatic Cloud Profile, Timetable & Settings Restore when user logs in with their Google/email account
    LaunchedEffect(currentUser?.uid) {
        val signedIn = currentUser ?: return@LaunchedEffect
        if (viewModel.shouldAutoRestoreForUid(signedIn.uid)) {
            cloudVm.fetchLatestBackup(silentIfEmpty = true) { backup ->
                viewModel.restoreFromCloudBackup(
                    backup = backup,
                    markUidRestored = signedIn.uid
                ) { summary ->
                    cloudVm.postStatusMessage("Welcome back ${backup.studentName}! Auto-imported your profile, settings & timetable. ($summary)")
                }
            }
        }
    }

    // Gate ALL Firestore reads & writes behind Google Sign-In via CredentialManager
    if (currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("cloud_sign_in_gate_view"),
            contentAlignment = Alignment.TopCenter
        ) {
            AuraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                cornerRadius = 24.dp,
                accentGlow = auraColors.secondaryAccent
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(auraColors.primaryAccent, auraColors.aiAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "AURA Cloud Sync & Cross-Platform Study Stage",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "AURA Cloud Sync & Universal Browser + App Study Stage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = auraColors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Sign in with Google to sync your Profile, Timetable, Exams, Notes, GPA & Flashcards across Android & Web—plus host live study sessions where classmates on iPhone Safari, Chrome, Mac, or Android join together.",
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x16FFFFFF))
                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CloudPerkRow("Auto-syncs Profile, Timetable & Flashcards with Android + Web PWA")
                        CloudPerkRow("Invite iPhone, Chrome & Safari classmates via QR Code or Link")
                        CloudPerkRow("Waiting Room, Live Voice/Slide Share, Pop-Quiz Polls & Chalkboard")
                    }

                    if (!statusMessage.isNullOrBlank()) {
                        Surface(
                            color = auraColors.warning.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, auraColors.warning.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = auraColors.warning,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    AuraGlassButton(
                        text = if (isBusy) "Connecting to Google..." else "Sign in with Google",
                        icon = Icons.Default.AccountCircle,
                        isPrimary = true,
                        enabled = !isBusy,
                        onClick = { cloudVm.signInWithGoogle(context) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "google_sign_in_button"
                    )
                }
            }
        }
        return
    }

    val signedInUser = currentUser!!
    val activeRoom = liveSelectedRoom ?: campusRooms.firstOrNull { it.id == selectedRoomId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cloud_authenticated_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Account & Full Cloud Profile + Timetable Backup Card
        item {
            AuraCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                accentGlow = AuraMintAccent
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AuraBadge(
                                    text = "CLOUD + WEB SYNCED",
                                    backgroundColor = AuraMintAccent.copy(alpha = 0.22f),
                                    textColor = AuraMintAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = signedInUser.displayName ?: userProfile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = auraColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${signedInUser.email ?: userProfile.email} • ${userProfile.institution}",
                                style = MaterialTheme.typography.labelSmall,
                                color = auraColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        AuraGlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            onClick = { cloudVm.signOut() },
                            tint = auraColors.danger,
                            testTag = "cloud_sign_out_button"
                        )
                    }

                    Text(
                        text = "Backs up your Profile, Theme, Alarms, Budget, Timetable, Exams, Notes, GPA & SM-2 Flashcards—works seamlessly between Android and the iPhone/Browser Web App.",
                        fontSize = 11.sp,
                        color = auraColors.textSecondary
                    )

                    if (!statusMessage.isNullOrBlank()) {
                        Surface(
                            color = auraColors.secondaryAccent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, auraColors.secondaryAccent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cloudVm.clearStatusMessage() }
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = auraColors.textPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AuraGlassButton(
                            text = "Backup All",
                            icon = Icons.Default.CloudUpload,
                            isPrimary = true,
                            enabled = !isBusy,
                            onClick = {
                                scope.launch {
                                    val payload = viewModel.buildCloudBackupPayload(
                                        userId = signedInUser.uid,
                                        accountEmail = signedInUser.email.orEmpty()
                                    )
                                    cloudVm.saveBackup(payload) {
                                        viewModel.markUidAutoRestored(signedInUser.uid)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "cloud_backup_now_button"
                        )

                        AuraGlassButton(
                            text = "Import Cloud",
                            icon = Icons.Default.CloudDownload,
                            enabled = !isBusy,
                            onClick = {
                                cloudVm.fetchLatestBackup(silentIfEmpty = false) { backup ->
                                    viewModel.restoreFromCloudBackup(
                                        backup = backup,
                                        markUidRestored = signedInUser.uid
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "cloud_restore_now_button"
                        )
                    }
                }
            }
        }

        // 2. If inside a Live Study Session -> Render Google-Meet-Plus Cross-Platform Study Stage!
        if (activeRoom != null) {
            item {
                LiveCrossTownStudyStageCard(
                    room = activeRoom,
                    currentUid = signedInUser.uid,
                    myDisplayName = signedInUser.displayName ?: userProfile.name,
                    myCityOrCampus = cityOrCampusInput,
                    participants = participants,
                    messages = roomMessages,
                    sharedCards = sharedCards,
                    onLeaveStage = { cloudVm.leaveActiveRoom() },
                    onOpenQrInvite = { showInviteDialog = true },
                    onOpenCreatePoll = { showCreatePollDialog = true },
                    onOpenSpotlight = { showSpotlightDialog = true },
                    onOpenRecap = { showRecapDialog = true },
                    onToggleTimer = { cloudVm.toggleRoomTimer(activeRoom) },
                    onSetTimerMinutes = { mins -> cloudVm.resetOrSetRoomTimerMinutes(activeRoom, mins) },
                    onSaveBoardNote = { note -> cloudVm.saveSharedBoardNote(activeRoom, note) },
                    onUpdateHostSecurity = { waitOn, lockBoard ->
                        cloudVm.updateHostSecurity(activeRoom, waitOn, lockBoard)
                    },
                    onAdmitParticipant = { uid ->
                        cloudVm.admitOrRemoveParticipant(activeRoom, uid, "ADMITTED")
                    },
                    onRemoveParticipant = { uid ->
                        cloudVm.admitOrRemoveParticipant(activeRoom, uid, "REMOVED")
                    },
                    onLowerHand = { p ->
                        cloudVm.lowerParticipantHand(activeRoom, p.userId, p.admissionStatus)
                    },
                    onToggleCoHost = { uid ->
                        cloudVm.toggleCoHost(activeRoom, uid)
                    },
                    onVotePoll = { idx -> cloudVm.voteInLivePoll(activeRoom, idx) },
                    onRevealPoll = { rev -> cloudVm.revealLivePoll(activeRoom, rev) },
                    onClosePoll = { cloudVm.closeLivePoll(activeRoom) },
                    onClearSpotlight = { cloudVm.pinSpotlightText(activeRoom, "") },
                    onUpdatePresence = { status, handRaised, emoji ->
                        cloudVm.updateMyPresence(
                            room = activeRoom,
                            displayName = signedInUser.displayName ?: userProfile.name,
                            cityOrCampus = cityOrCampusInput,
                            status = status,
                            handRaised = handRaised,
                            reactionEmoji = emoji
                        )
                    },
                    onSendChat = { msg, type ->
                        cloudVm.sendRoomChatMessage(
                            room = activeRoom,
                            authorNameFallback = userProfile.name,
                            messageText = msg,
                            messageType = type
                        )
                    },
                    onOpenShareCardDialog = { showShareCardDialog = true },
                    onImportCardToDeck = { card ->
                        viewModel.addFlashcard(
                            courseCode = card.courseCode,
                            deckTitle = "Live Room • ${activeRoom.roomTitle}",
                            question = card.question,
                            answer = card.answer
                        )
                        cloudVm.postStatusMessage("Saved '${card.courseCode}' card to your SM-2 Flashcard Deck!")
                    },
                    onCompleteRoom = { cloudVm.completeRoom(activeRoom) },
                    onDeleteRoom = { cloudVm.deleteRoom(activeRoom.id) },
                    onNotify = { cloudVm.postStatusMessage(it) }
                )
            }
        } else {
            // 3. Cross-Town & Cross-Platform Join by Invite Code / Link + Host New Live Study Stage Card
            item {
                AuraCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    accentGlow = auraColors.aiAccent
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AuraBadge(
                                text = "ANDROID • IPHONE SAFARI • CHROME • MAC / PC",
                                backgroundColor = auraColors.aiAccent.copy(alpha = 0.20f),
                                textColor = auraColors.aiAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Host or Join Universal Live Study Stage",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = auraColors.textPrimary
                            )
                            Text(
                                text = "Host from your phone and invite anyone—even iPhone or laptop friends without the APK—using a QR Code or Web Link, with Live Voice/Screen-Share, Waiting Room, Polls & Chalkboard.",
                                fontSize = 11.sp,
                                color = auraColors.textSecondary
                            )
                        }

                        OutlinedTextField(
                            value = cityOrCampusInput,
                            onValueChange = { cityOrCampusInput = it },
                            label = { Text("Your Town / Campus Badge (shown on your tile)") },
                            placeholder = { Text("e.g. Accra • Legon / Kumasi • KNUST") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_room_city_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.take(120) },
                                label = { Text("6-Char Code or Invite Link") },
                                placeholder = { Text("e.g. AUR7K2") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_room_code_input")
                            )

                            AuraGlassButton(
                                text = "Join",
                                icon = Icons.Default.Login,
                                isPrimary = true,
                                enabled = !isBusy && AuraCloudRepository.normalizeJoinCode(joinCodeInput).length >= 4,
                                onClick = {
                                    cloudVm.joinRoomByCode(
                                        joinCodeInput = joinCodeInput,
                                        displayName = signedInUser.displayName ?: userProfile.name,
                                        cityOrCampus = cityOrCampusInput
                                    )
                                },
                                testTag = "join_room_by_code_button"
                            )
                        }

                        AuraGlassButton(
                            text = "Host New Live Study Session",
                            icon = Icons.Default.VideoCall,
                            isPrimary = true,
                            onClick = { showCreateRoomDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "host_study_room_button"
                        )
                    }
                }
            }

            // 4. Open Campus Study Lobby Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Live Open Study Lobby",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = auraColors.textPrimary
                    )
                    Text(
                        text = "Tap any open room to join immediately, or host a private code/QR session for classmates in any town",
                        style = MaterialTheme.typography.labelSmall,
                        color = auraColors.textSecondary
                    )
                }
            }

            if (campusRooms.isEmpty()) {
                item {
                    AuraCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = auraColors.secondaryAccent,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Open Study Sessions Right Now",
                                fontWeight = FontWeight.Bold,
                                color = auraColors.textPrimary
                            )
                            Text(
                                text = "Tap 'Host New Live Study Session' above to generate a QR Code & Browser Link that Android, iPhone, and Laptop friends can join.",
                                style = MaterialTheme.typography.bodySmall,
                                color = auraColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(campusRooms, key = { it.id }) { room ->
                    val isHost = room.ownerId == signedInUser.uid
                    AuraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                cloudVm.enterRoom(
                                    room = room,
                                    displayName = signedInUser.displayName ?: userProfile.name,
                                    cityOrCampus = cityOrCampusInput
                                )
                            }
                            .testTag("study_room_card_${room.id}"),
                        cornerRadius = 20.dp,
                        accentGlow = if (room.timerState == "RUNNING") AuraMintAccent else null
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AuraBadge(
                                        text = room.courseCode,
                                        backgroundColor = auraColors.primaryAccent.copy(alpha = 0.22f),
                                        textColor = auraColors.secondaryAccent
                                    )
                                    if (room.joinCode.isNotBlank()) {
                                        AuraBadge(
                                            text = "CODE: ${room.joinCode}",
                                            backgroundColor = auraColors.aiAccent.copy(alpha = 0.22f),
                                            textColor = auraColors.aiAccent
                                        )
                                    }
                                    if (room.waitingRoomEnabled) {
                                        AuraBadge(
                                            text = "KNOCK",
                                            backgroundColor = auraColors.warning.copy(alpha = 0.20f),
                                            textColor = auraColors.warning
                                        )
                                    }
                                    AuraBadge(
                                        text = when {
                                            room.status == "COMPLETED" -> "LOCKED"
                                            room.timerState == "RUNNING" -> "TIMER LIVE"
                                            else -> "${room.activeMinutes}M"
                                        },
                                        backgroundColor = if (room.status == "COMPLETED") {
                                            auraColors.textMuted.copy(alpha = 0.2f)
                                        } else {
                                            AuraMintAccent.copy(alpha = 0.2f)
                                        },
                                        textColor = if (room.status == "COMPLETED") auraColors.textMuted else AuraMintAccent
                                    )
                                }

                                if (isHost) {
                                    AuraBadge(
                                        text = "HOST",
                                        backgroundColor = auraColors.warning.copy(alpha = 0.20f),
                                        textColor = auraColors.warning
                                    )
                                }
                            }

                            Text(
                                text = room.roomTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = auraColors.textPrimary
                            )

                            Text(
                                text = "${room.focusTopic} • Hosted by ${room.ownerName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = auraColors.textSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                AuraGlassButton(
                                    text = if (room.waitingRoomEnabled && !isHost) "Knock to Join" else "Enter Study Stage",
                                    icon = Icons.Default.MeetingRoom,
                                    isPrimary = true,
                                    onClick = {
                                        cloudVm.enterRoom(
                                            room = room,
                                            displayName = signedInUser.displayName ?: userProfile.name,
                                            cityOrCampus = cityOrCampusInput
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateRoomDialog) {
        HostCrossTownRoomDialog(
            defaultCourseCode = courses.firstOrNull()?.code ?: "CS301",
            defaultCityOrCampus = cityOrCampusInput,
            onDismiss = { showCreateRoomDialog = false },
            onCreate = { code, title, goal, mins, visibility, cityTag, waitingRoomOn, whiteboardLocked ->
                cityOrCampusInput = cityTag
                cloudVm.createRoom(
                    courseCode = code,
                    roomTitle = title,
                    topicGoal = goal,
                    focusMinutes = mins,
                    hostNameFallback = userProfile.name,
                    cityOrCampus = cityTag,
                    visibility = visibility,
                    waitingRoomEnabled = waitingRoomOn,
                    whiteboardLocked = whiteboardLocked
                )
                showCreateRoomDialog = false
            }
        )
    }

    if (showShareCardDialog && activeRoom != null) {
        ShareFlashcardToRoomDialog(
            roomTitle = activeRoom.roomTitle,
            defaultCourseCode = activeRoom.courseCode,
            onDismiss = { showShareCardDialog = false },
            onShare = { code, q, a ->
                cloudVm.postSharedCard(
                    room = activeRoom,
                    courseCode = code,
                    question = q,
                    answer = a,
                    authorNameFallback = userProfile.name
                )
                showShareCardDialog = false
            }
        )
    }

    if (showInviteDialog && activeRoom != null) {
        UniversalRoomInviteDialog(
            room = activeRoom,
            webPortalUrl = userProfile.webPortalUrl,
            onSaveWebPortalUrl = { url -> viewModel.setWebPortalUrl(url) },
            onDismiss = { showInviteDialog = false },
            onNotify = { msg -> cloudVm.postStatusMessage(msg) }
        )
    }

    if (showCreatePollDialog && activeRoom != null) {
        CreateStagePollDialog(
            onDismiss = { showCreatePollDialog = false },
            onLaunch = { q, opts, correctIdx ->
                cloudVm.launchLivePoll(activeRoom, q, opts, correctIdx)
                showCreatePollDialog = false
            }
        )
    }

    if (showSpotlightDialog && activeRoom != null) {
        PinSpotlightDialog(
            initialText = activeRoom.spotlightCardText,
            onDismiss = { showSpotlightDialog = false },
            onPin = { pinned ->
                cloudVm.pinSpotlightText(activeRoom, pinned)
                showSpotlightDialog = false
            }
        )
    }

    if (showRecapDialog && activeRoom != null) {
        SmartSessionRecapDialog(
            room = activeRoom,
            participants = participants,
            messages = roomMessages,
            sharedCards = sharedCards,
            onSaveToStudyNotes = { title, content ->
                viewModel.saveStudyNote(
                    courseCode = activeRoom.courseCode,
                    title = title,
                    content = content,
                    category = "Summary",
                    isPinned = true
                )
                cloudVm.postStatusMessage("Saved Smart Study Minutes to your Study Notes Vault!")
            },
            onDismiss = { showRecapDialog = false }
        )
    }
}

@Composable
private fun CloudPerkRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AuraMintAccent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = LocalAuraColors.current.textPrimary
        )
    }
}

@Composable
private fun LiveCrossTownStudyStageCard(
    room: StudyRoom,
    currentUid: String,
    myDisplayName: String,
    myCityOrCampus: String,
    participants: List<RoomParticipant>,
    messages: List<RoomMessage>,
    sharedCards: List<SharedFlashcard>,
    onLeaveStage: () -> Unit,
    onOpenQrInvite: () -> Unit,
    onOpenCreatePoll: () -> Unit,
    onOpenSpotlight: () -> Unit,
    onOpenRecap: () -> Unit,
    onToggleTimer: () -> Unit,
    onSetTimerMinutes: (Int) -> Unit,
    onSaveBoardNote: (String) -> Unit,
    onUpdateHostSecurity: (waitingRoomEnabled: Boolean, whiteboardLocked: Boolean) -> Unit,
    onAdmitParticipant: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onLowerHand: (RoomParticipant) -> Unit,
    onToggleCoHost: (String) -> Unit,
    onVotePoll: (Int) -> Unit,
    onRevealPoll: (Boolean) -> Unit,
    onClosePoll: () -> Unit,
    onClearSpotlight: () -> Unit,
    onUpdatePresence: (status: String, handRaised: Boolean, emoji: String) -> Unit,
    onSendChat: (String, String) -> Unit,
    onOpenShareCardDialog: () -> Unit,
    onImportCardToDeck: (SharedFlashcard) -> Unit,
    onCompleteRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val auraColors = LocalAuraColors.current
    val isHost = room.ownerId == currentUid
    val isCoHost = room.coHostIds.contains(currentUid)
    val canControlStage = isHost || isCoHost
    val isLocked = room.status == "COMPLETED"

    var selectedStageTab by remember { mutableStateOf(LiveRoomStageTab.WHITEBOARD) }
    var boardDraft by remember(room.id, room.sharedBoardNote) { mutableStateOf(room.sharedBoardNote) }
    var chatInput by remember { mutableStateOf("") }
    var showRoomMinutesPicker by remember { mutableStateOf(false) }
    var selectedParticipantForModeration by remember { mutableStateOf<RoomParticipant?>(null) }

    val myParticipant = participants.firstOrNull { it.userId == currentUid }
    val myAdmissionStatus = when {
        canControlStage -> "ADMITTED"
        myParticipant != null -> myParticipant.admissionStatus.ifBlank { "ADMITTED" }
        room.waitingRoomEnabled -> "WAITING"
        else -> "ADMITTED"
    }

    // Waiting Room or Removed Gate for Non-Host Guests
    if (myAdmissionStatus == "REMOVED") {
        AuraCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("removed_from_room_card"),
            cornerRadius = 24.dp,
            accentGlow = auraColors.danger
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Removed from session",
                    tint = auraColors.danger,
                    modifier = Modifier.size(38.dp)
                )
                Text(
                    text = "Session Access Ended",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = auraColors.textPrimary
                )
                Text(
                    text = "The host has removed your tile from '${room.roomTitle}'.",
                    fontSize = 12.sp,
                    color = auraColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                AuraGlassButton(
                    text = "Return to Study Lobby",
                    icon = Icons.Default.ArrowBack,
                    isPrimary = true,
                    onClick = onLeaveStage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    if (myAdmissionStatus == "WAITING") {
        AuraCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("guest_waiting_room_card"),
            cornerRadius = 24.dp,
            accentGlow = auraColors.warning
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Doorbell,
                    contentDescription = "Waiting Room",
                    tint = auraColors.warning,
                    modifier = Modifier.size(40.dp)
                )
                AuraBadge(
                    text = "WAITING ROOM • KNOCK SENT",
                    backgroundColor = auraColors.warning.copy(alpha = 0.22f),
                    textColor = auraColors.warning
                )
                Text(
                    text = room.roomTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = auraColors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Host ${room.ownerName} has enabled the Waiting Room. You'll enter the live stage automatically as soon as the host or a co-host admits you.",
                    fontSize = 12.sp,
                    color = auraColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                AuraGlassButton(
                    text = "Leave Waiting Room",
                    icon = Icons.Default.Close,
                    onClick = onLeaveStage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    val myHandRaised = myParticipant?.handRaised ?: false
    val myStatus = myParticipant?.studyStatus ?: "FOCUSING"
    val myEmoji = myParticipant?.reactionEmoji ?: "🎯"
    val myVoteIndex = myParticipant?.pollVoteIndex?.toInt() ?: -1

    val waitingGuests = participants.filter { it.admissionStatus == "WAITING" }
    val admittedParticipants = participants.filter { it.admissionStatus != "WAITING" && it.admissionStatus != "REMOVED" }

    // Real-time synchronized countdown clock across all connected towns/devices
    var liveDisplaySeconds by remember(room.timerState, room.timerEndEpochMillis, room.timerRemainingSeconds) {
        val initial = if (room.timerState == "RUNNING" && room.timerEndEpochMillis > 0L) {
            ((room.timerEndEpochMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        } else if (room.timerRemainingSeconds > 0L) {
            room.timerRemainingSeconds
        } else {
            room.activeMinutes * 60L
        }
        mutableLongStateOf(initial)
    }

    LaunchedEffect(room.timerState, room.timerEndEpochMillis, room.timerRemainingSeconds) {
        while (room.timerState == "RUNNING" && room.timerEndEpochMillis > 0L) {
            val rem = ((room.timerEndEpochMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            liveDisplaySeconds = rem
            if (rem <= 0L) break
            delay(1000L)
        }
    }

    val hrs = liveDisplaySeconds / 3600L
    val mins = (liveDisplaySeconds % 3600L) / 60L
    val secs = liveDisplaySeconds % 60L
    val formattedClock = if (hrs > 0L) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }

    val displayCode = room.joinCode.ifBlank { room.id.removePrefix("room_").take(6).uppercase() }
    val voiceUrl = room.voiceRoomUrl.ifBlank { AuraCloudRepository.buildJitsiRoomUrl(displayCode) }

    AuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_study_stage_card"),
        cornerRadius = 24.dp,
        accentGlow = AuraMintAccent
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Stage Header: Course + Join Code + QR/Browser Invite + Leave
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AuraBadge(
                        text = room.courseCode,
                        backgroundColor = auraColors.primaryAccent.copy(alpha = 0.25f),
                        textColor = auraColors.secondaryAccent
                    )
                    AuraBadge(
                        text = "CODE: $displayCode",
                        backgroundColor = AuraMintAccent.copy(alpha = 0.22f),
                        textColor = AuraMintAccent
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AuraGlassIconButton(
                        icon = Icons.Default.QrCode2,
                        contentDescription = "QR & Browser Invite for iPhone / Chrome / Android",
                        onClick = onOpenQrInvite,
                        size = 34.dp,
                        tint = AuraMintAccent,
                        testTag = "share_room_invite_button"
                    )
                    AuraGlassIconButton(
                        icon = Icons.Default.Summarize,
                        contentDescription = "Smart Study Session Recap",
                        onClick = onOpenRecap,
                        size = 34.dp,
                        tint = auraColors.secondaryAccent,
                        testTag = "open_session_recap_button"
                    )
                    AuraGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Leave Study Stage",
                        onClick = onLeaveStage,
                        size = 34.dp,
                        tint = auraColors.danger,
                        testTag = "leave_study_stage_button"
                    )
                }
            }

            Text(
                text = room.roomTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = auraColors.textPrimary
            )
            Text(
                text = "${room.focusTopic} • Host: ${room.ownerName} • ${admittedParticipants.size.coerceAtLeast(1)} Scholar(s) on Stage",
                fontSize = 11.5.sp,
                color = auraColors.textSecondary
            )

            // Quick Universal Invite + Live Voice/Video/Screen-Share Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuraGlassButton(
                    text = "QR / iPhone Link",
                    icon = Icons.Default.QrCode2,
                    isPrimary = true,
                    onClick = onOpenQrInvite,
                    modifier = Modifier.weight(1f),
                    testTag = "stage_qr_invite_button"
                )
                AuraGlassButton(
                    text = "Voice & Slide Share",
                    icon = Icons.Default.HeadsetMic,
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(voiceUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            onNotify("Unable to open browser for live audio/screen-share room.")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "stage_voice_share_button"
                )
            }

            // Host / Co-Host Waiting Room Queue Banner
            if (canControlStage && waitingGuests.isNotEmpty()) {
                WaitingRoomQueueBanner(
                    waitingParticipants = waitingGuests,
                    onAdmit = onAdmitParticipant,
                    onDecline = onRemoveParticipant,
                    onAdmitAll = {
                        waitingGuests.forEach { g -> onAdmitParticipant(g.userId) }
                    }
                )
            }

            // Host & Co-Host Stage Controls Strip
            if (canControlStage && !isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHost) {
                        HostControlChip(
                            label = if (room.waitingRoomEnabled) "🚪 Knock: ON" else "🚪 Knock: OFF",
                            active = room.waitingRoomEnabled,
                            onClick = {
                                onUpdateHostSecurity(!room.waitingRoomEnabled, room.whiteboardLocked)
                            }
                        )
                        HostControlChip(
                            label = if (room.whiteboardLocked) "🔒 Lecture Board" else "🔓 Open Board",
                            active = room.whiteboardLocked,
                            onClick = {
                                onUpdateHostSecurity(room.waitingRoomEnabled, !room.whiteboardLocked)
                            }
                        )
                    }
                    HostControlChip(
                        label = "📊 Pop-Quiz Poll",
                        active = room.activePollQuestion.isNotBlank(),
                        onClick = onOpenCreatePoll
                    )
                    HostControlChip(
                        label = "📌 Pin Spotlight",
                        active = room.spotlightCardText.isNotBlank(),
                        onClick = onOpenSpotlight
                    )
                }
            }

            // Pinned Exam Spotlight Banner (visible to all Android + iPhone/Browser scholars)
            if (room.spotlightCardText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = auraColors.warning.copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, auraColors.warning.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stage_spotlight_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "📌 STAGE SPOTLIGHT QUESTION / FOCUS",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = auraColors.warning,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = room.spotlightCardText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (canControlStage && !isLocked) {
                            IconButton(
                                onClick = onClearSpotlight,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Spotlight",
                                    tint = auraColors.warning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Live Stage Pop-Quiz Poll Card
            if (room.activePollQuestion.isNotBlank()) {
                LiveStagePollCard(
                    room = room,
                    participants = admittedParticipants,
                    myVoteIndex = myVoteIndex,
                    canControlStage = canControlStage && !isLocked,
                    onVote = { idx -> if (!isLocked) onVotePoll(idx) },
                    onToggleReveal = onRevealPoll,
                    onClosePoll = onClosePoll
                )
            }

            // Synchronized Cloud Countdown Clock Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0x22000000),
                border = BorderStroke(
                    1.2.dp,
                    if (room.timerState == "RUNNING") AuraMintAccent else auraColors.secondaryAccent.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            isLocked -> "SESSION COMPLETED & ARCHIVED"
                            room.timerState == "RUNNING" -> "LIVE SYNCHRONIZED FOCUS CLOCK"
                            room.timerState == "PAUSED" -> "SHARED TIMER PAUSED"
                            else -> "READY TO START SHARED SPRINT"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (room.timerState == "RUNNING") AuraMintAccent else auraColors.secondaryAccent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formattedClock,
                        fontFamily = AuraMonoFontFamily,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("synced_room_clock_text")
                    )

                    if (canControlStage && !isLocked) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraGlassButton(
                                text = if (room.timerState == "RUNNING") "Pause All" else "Start Live Timer",
                                icon = if (room.timerState == "RUNNING") Icons.Default.Pause else Icons.Default.PlayArrow,
                                isPrimary = true,
                                onClick = onToggleTimer,
                                testTag = "host_toggle_synced_timer_button"
                            )
                            AuraGlassButton(
                                text = "Set Mins",
                                icon = Icons.Default.Schedule,
                                onClick = { showRoomMinutesPicker = true },
                                testTag = "host_set_room_mins_button"
                            )
                        }
                    } else if (!canControlStage) {
                        Text(
                            text = "Synced live across Android & Browser with host ${room.ownerName}",
                            fontSize = 10.5.sp,
                            color = auraColors.textMuted
                        )
                    }
                }
            }

            // Meet-Style Live Scholar Presence Grid (with Device Platform & Co-Host Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connected Scholars (${admittedParticipants.size.coerceAtLeast(1)})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = auraColors.secondaryAccent
                )
                if (canControlStage && admittedParticipants.size > 1) {
                    Text(
                        text = "Tap tile to moderate",
                        fontSize = 10.sp,
                        color = auraColors.textMuted
                    )
                }
            }

            val effectiveTiles = if (admittedParticipants.isEmpty()) {
                listOf(
                    RoomParticipant(
                        userId = currentUid,
                        displayName = myDisplayName,
                        cityOrCampus = myCityOrCampus,
                        studyStatus = myStatus,
                        handRaised = myHandRaised,
                        reactionEmoji = myEmoji,
                        devicePlatform = "ANDROID"
                    )
                )
            } else admittedParticipants

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                effectiveTiles.forEach { p ->
                    val tileIsHost = p.userId == room.ownerId
                    val tileIsCoHost = room.coHostIds.contains(p.userId)
                    ScholarPresenceTile(
                        participant = p,
                        isHostTile = tileIsHost,
                        isCoHostTile = tileIsCoHost,
                        isMe = p.userId == currentUid,
                        onClick = {
                            if (canControlStage && p.userId != currentUid && !tileIsHost) {
                                selectedParticipantForModeration = p
                            }
                        }
                    )
                }
            }

            // Interactive Meet-Better Controls: Raise Hand, Status Mode & Live Emoji Burst
            if (!isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (myHandRaised) auraColors.warning.copy(alpha = 0.28f) else Color(0x1EFFFFFF),
                        border = BorderStroke(1.dp, if (myHandRaised) auraColors.warning else Color(0x36FFFFFF)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                onUpdatePresence(myStatus, !myHandRaised, if (!myHandRaised) "✋" else "🎯")
                            }
                            .testTag("stage_raise_hand_button")
                    ) {
                        Text(
                            text = if (myHandRaised) "✋ Hand Raised" else "✋ Raise Hand",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (myHandRaised) auraColors.warning else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    val nextStatus = when (myStatus) {
                        "FOCUSING" -> "QUIZZING"
                        "QUIZZING" -> "BREAK"
                        else -> "FOCUSING"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = auraColors.primaryAccent.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, auraColors.secondaryAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onUpdatePresence(nextStatus, myHandRaised, myEmoji) }
                            .testTag("stage_cycle_status_button")
                    ) {
                        Text(
                            text = "Status: $myStatus",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = auraColors.secondaryAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    listOf("🔥", "💡", "👏", "✅", "🎯").forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = if (myEmoji == emoji) auraColors.secondaryAccent.copy(alpha = 0.30f) else Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onUpdatePresence(myStatus, myHandRaised, emoji) }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Collaborative Stage Sub-Tabs: Shared Pinboard | Live Q&A Chat | Shared Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiveRoomStageTab.entries.forEach { tab ->
                    val selected = selectedStageTab == tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) auraColors.primaryAccent else Color(0x18FFFFFF),
                        border = BorderStroke(1.dp, if (selected) auraColors.secondaryAccent else Color(0x2EFFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedStageTab = tab }
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else auraColors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            when (selectedStageTab) {
                LiveRoomStageTab.WHITEBOARD -> {
                    val canEditBoard = !isLocked && (!room.whiteboardLocked || canControlStage)
                    if (room.whiteboardLocked && !canControlStage) {
                        Text(
                            text = "🔒 Lecture Chalkboard Mode: Only the Host & Co-Hosts can edit right now.",
                            fontSize = 11.sp,
                            color = auraColors.warning
                        )
                    }
                    OutlinedTextField(
                        value = boardDraft,
                        onValueChange = { boardDraft = it },
                        enabled = canEditBoard,
                        label = {
                            Text(
                                if (room.whiteboardLocked) "Host Chalkboard / Formulas (Lecture Mode)"
                                else "Co-Edited Shared Study Pinboard / Formulas"
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shared_whiteboard_input")
                    )
                    if (canEditBoard) {
                        AuraGlassButton(
                            text = "Sync Pinboard to All Phones & Browsers",
                            icon = Icons.Default.Sync,
                            isPrimary = true,
                            onClick = { onSaveBoardNote(boardDraft) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "sync_whiteboard_button"
                        )
                    }
                }

                LiveRoomStageTab.LIVE_CHAT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x18000000))
                            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (messages.isEmpty()) {
                            Text(
                                text = "Ask a question, drop a derivation hint, or chat with Android & iPhone classmates!",
                                fontSize = 11.sp,
                                color = auraColors.textMuted
                            )
                        } else {
                            messages.takeLast(10).forEach { msg ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (msg.authorId == currentUid) {
                                        auraColors.primaryAccent.copy(alpha = 0.24f)
                                    } else {
                                        Color(0x1EFFFFFF)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = msg.authorName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = auraColors.secondaryAccent
                                            )
                                            if (canControlStage && !isLocked) {
                                                Text(
                                                    text = "Pin",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AuraMintAccent,
                                                    modifier = Modifier.clickable {
                                                        onSaveBoardNote(
                                                            (boardDraft + "\n• Q&A (${msg.authorName}): ${msg.text}").trim()
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = auraColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        if (!isLocked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { chatInput = it },
                                    placeholder = { Text("Ask room or share insight...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("room_chat_input")
                                )
                                AuraGlassIconButton(
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Room Message",
                                    onClick = {
                                        if (chatInput.isNotBlank()) {
                                            onSendChat(chatInput, "CHAT")
                                            chatInput = ""
                                        }
                                    },
                                    tint = AuraMintAccent,
                                    testTag = "room_chat_send_button"
                                )
                            }
                        }
                    }
                }

                LiveRoomStageTab.SHARED_CARDS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isLocked) {
                            AuraGlassButton(
                                text = "Drop Flashcard to Room",
                                icon = Icons.Default.Add,
                                isPrimary = true,
                                onClick = onOpenShareCardDialog,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "share_flashcard_to_room_button"
                            )
                        }
                        if (sharedCards.isEmpty()) {
                            Text(
                                text = "No flashcards dropped in this room yet.",
                                fontSize = 11.5.sp,
                                color = auraColors.textMuted
                            )
                        } else {
                            sharedCards.forEach { card ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x1EFFFFFF),
                                    border = BorderStroke(1.dp, Color(0x32FFFFFF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${card.courseCode} • by ${card.authorName}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = auraColors.secondaryAccent
                                            )
                                            Text(
                                                text = "+ Save to SM-2 Deck",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AuraMintAccent,
                                                modifier = Modifier.clickable { onImportCardToDeck(card) }
                                            )
                                        }
                                        Text(
                                            text = "Q: ${card.question}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = auraColors.textPrimary
                                        )
                                        Text(
                                            text = "A: ${card.answer}",
                                            fontSize = 11.5.sp,
                                            color = auraColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraGlassButton(
                    text = "Session Recap",
                    icon = Icons.Default.Summarize,
                    onClick = onOpenRecap
                )

                if (isHost) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isLocked) {
                            AuraGlassButton(
                                text = "End Session",
                                icon = Icons.Default.CheckCircle,
                                onClick = {
                                    onCompleteRoom()
                                    onOpenRecap()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        AuraGlassIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete Session",
                            onClick = onDeleteRoom,
                            tint = auraColors.danger
                        )
                    }
                }
            }
        }
    }

    if (showRoomMinutesPicker) {
        var minsText by remember { mutableStateOf(room.activeMinutes.toString()) }
        Dialog(onDismissRequest = { showRoomMinutesPicker = false }) {
            Surface(
                color = Color(0xF5101B30),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, auraColors.secondaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Set Shared Timer Duration (Minutes)", fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Any duration (1 – 360 mins)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AuraGlassButton(text = "Cancel", onClick = { showRoomMinutesPicker = false }, modifier = Modifier.weight(1f))
                        AuraGlassButton(
                            text = "Apply",
                            isPrimary = true,
                            onClick = {
                                val parsed = minsText.toIntOrNull()?.coerceIn(1, 360) ?: 45
                                onSetTimerMinutes(parsed)
                                showRoomMinutesPicker = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    selectedParticipantForModeration?.let { target ->
        val targetIsCoHost = room.coHostIds.contains(target.userId)
        Dialog(onDismissRequest = { selectedParticipantForModeration = null }) {
            Surface(
                color = Color(0xF50E182E),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.2.dp, auraColors.secondaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Moderate ${target.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${target.cityOrCampus.ifBlank { "Remote" }} • ${target.devicePlatform}",
                        fontSize = 11.sp,
                        color = auraColors.textSecondary
                    )
                    if (target.handRaised) {
                        AuraGlassButton(
                            text = "Lower Raised Hand",
                            icon = Icons.Default.PanTool,
                            onClick = {
                                onLowerHand(target)
                                selectedParticipantForModeration = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isHost) {
                        AuraGlassButton(
                            text = if (targetIsCoHost) "Remove Co-Host Privilege" else "Promote to Co-Host",
                            icon = Icons.Default.AdminPanelSettings,
                            isPrimary = !targetIsCoHost,
                            onClick = {
                                onToggleCoHost(target.userId)
                                selectedParticipantForModeration = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    AuraGlassButton(
                        text = "Remove from Session",
                        icon = Icons.Default.PersonRemove,
                        onClick = {
                            onRemoveParticipant(target.userId)
                            selectedParticipantForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AuraGlassButton(
                        text = "Close",
                        onClick = { selectedParticipantForModeration = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HostControlChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) AuraMintAccent.copy(alpha = 0.24f) else Color(0x1EFFFFFF),
        border = BorderStroke(1.dp, if (active) AuraMintAccent else auraColors.secondaryAccent.copy(alpha = 0.45f)),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) AuraMintAccent else Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun PinSpotlightDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onPin: (String) -> Unit
) {
    val auraColors = LocalAuraColors.current
    var text by remember { mutableStateOf(initialText) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF5101B30),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, auraColors.warning.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pin Top Stage Spotlight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Pins a past-paper question, theorem, or sprint goal to the top banner of every Android & iPhone/Browser screen.",
                    fontSize = 11.5.sp,
                    color = auraColors.textSecondary
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(580) },
                    label = { Text("Spotlight Question or Theorem") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (initialText.isNotBlank()) {
                        AuraGlassButton(
                            text = "Unpin",
                            onClick = { onPin("") },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        AuraGlassButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AuraGlassButton(
                        text = "Pin to Stage",
                        isPrimary = true,
                        onClick = { onPin(text.trim()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScholarPresenceTile(
    participant: RoomParticipant,
    isHostTile: Boolean,
    isCoHostTile: Boolean,
    isMe: Boolean,
    onClick: () -> Unit
) {
    val auraColors = LocalAuraColors.current
    val initials = participant.displayName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "SC" }

    val platformLabel = when (participant.devicePlatform) {
        "IPHONE_WEB" -> "📱 iPhone"
        "WEB_BROWSER" -> "🌐 Browser"
        else -> "🤖 Android"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (participant.handRaised) auraColors.warning.copy(alpha = 0.18f) else Color(0x22FFFFFF),
        border = BorderStroke(
            width = 1.2.dp,
            color = when {
                participant.handRaised -> auraColors.warning
                isMe -> AuraMintAccent
                isCoHostTile -> auraColors.aiAccent
                else -> Color(0x38FFFFFF)
            }
        ),
        modifier = Modifier
            .width(142.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(auraColors.primaryAccent, auraColors.aiAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                if (participant.reactionEmoji.isNotBlank()) {
                    Text(
                        text = participant.reactionEmoji,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                if (participant.handRaised) {
                    Text(
                        text = "✋",
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }

            Text(
                text = if (isMe) "${participant.displayName} (You)" else participant.displayName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = auraColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${participant.cityOrCampus.ifBlank { "Remote" }} • $platformLabel",
                fontSize = 9.5.sp,
                color = auraColors.secondaryAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AuraBadge(
                text = when {
                    isHostTile -> "HOST • ${participant.studyStatus}"
                    isCoHostTile -> "CO-HOST • ${participant.studyStatus}"
                    else -> participant.studyStatus
                },
                backgroundColor = AuraMintAccent.copy(alpha = 0.18f),
                textColor = AuraMintAccent
            )
        }
    }
}

@Composable
private fun HostCrossTownRoomDialog(
    defaultCourseCode: String,
    defaultCityOrCampus: String,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String, String, Boolean, Boolean) -> Unit
) {
    val auraColors = LocalAuraColors.current
    var courseCode by remember { mutableStateOf(defaultCourseCode) }
    var roomTitle by remember { mutableStateOf("") }
    var topicGoal by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("50") }
    var cityTag by remember { mutableStateOf(defaultCityOrCampus) }
    var visibility by remember { mutableStateOf("CAMPUS") }
    var waitingRoomEnabled by remember { mutableStateOf(false) }
    var whiteboardLocked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF5101B30),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, auraColors.secondaryAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Host Cross-Platform Live Study Stage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Generates a 6-character Code, scannable QR Code, Browser Join Link (for iPhone/Chrome/Safari), and Live Voice/Slide Share room.",
                    fontSize = 11.5.sp,
                    color = auraColors.textSecondary
                )

                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roomTitle,
                    onValueChange = { roomTitle = it },
                    label = { Text("Session Title (e.g. Past Paper Sprint)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topicGoal,
                    onValueChange = { topicGoal = it },
                    label = { Text("Sprint Goal / Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cityTag,
                    onValueChange = { cityTag = it },
                    label = { Text("Your Town / Campus Badge") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Focus Duration in Minutes (5 – 360)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "CAMPUS" to "Open Lobby + Link",
                        "UNLISTED" to "Private Link/QR Only"
                    ).forEach { (mode, label) ->
                        val active = visibility == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) auraColors.primaryAccent else Color(0x1EFFFFFF),
                            border = BorderStroke(1.dp, if (active) auraColors.secondaryAccent else Color(0x30FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { visibility = mode }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }

                // Host Security Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { waitingRoomEnabled = !waitingRoomEnabled }
                    ) {
                        Checkbox(
                            checked = waitingRoomEnabled,
                            onCheckedChange = { waitingRoomEnabled = it }
                        )
                        Text(
                            text = "Waiting Room ('Knock to Join' approval)",
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { whiteboardLocked = !whiteboardLocked }
                    ) {
                        Checkbox(
                            checked = whiteboardLocked,
                            onCheckedChange = { whiteboardLocked = it }
                        )
                        Text(
                            text = "Lecture Mode (Host-only Chalkboard)",
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
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
                        text = "Go Live",
                        isPrimary = true,
                        onClick = {
                            val mins = durationText.toIntOrNull()?.coerceIn(5, 360) ?: 50
                            onCreate(
                                courseCode,
                                roomTitle,
                                topicGoal,
                                mins,
                                visibility,
                                cityTag,
                                waitingRoomEnabled,
                                whiteboardLocked
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareFlashcardToRoomDialog(
    roomTitle: String,
    defaultCourseCode: String,
    onDismiss: () -> Unit,
    onShare: (String, String, String) -> Unit
) {
    val auraColors = LocalAuraColors.current
    var courseCode by remember { mutableStateOf(defaultCourseCode) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF5101B30),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, auraColors.secondaryAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Flashcard to $roomTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question / Prompt") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Verified Answer") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        text = "Post Card",
                        isPrimary = true,
                        onClick = { onShare(courseCode, question, answer) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
