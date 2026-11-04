package com.example.ui.cloud

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.AuraCloudRepository
import com.example.data.cloud.RoomMessage
import com.example.data.cloud.RoomParticipant
import com.example.data.cloud.SharedFlashcard
import com.example.data.cloud.StudyRoom
import com.example.data.cloud.UserBackup
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth? = runCatching {
        Firebase.auth
    }.onFailure { e ->
        Log.w("CloudSyncViewModel", "Firebase Auth unavailable in current environment", e)
    }.getOrNull()

    private val repository: AuraCloudRepository? = runCatching {
        AuraCloudRepository(application)
    }.onFailure { e ->
        Log.w("CloudSyncViewModel", "Firestore unavailable in current environment", e)
    }.getOrNull()

    val currentUser: StateFlow<FirebaseUser?> = auth?.authStateFlow()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.currentUser)
        ?: MutableStateFlow<FirebaseUser?>(null).asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId.asStateFlow()

    val campusStudyRooms: StateFlow<List<StudyRoom>> = currentUser
        .flatMapLatest { user ->
            val repo = repository
            if (user == null || repo == null) {
                flowOf(emptyList())
            } else {
                repo.observeCampusStudyRooms().catch { e ->
                    Log.e("CloudSyncViewModel", "observeCampusStudyRooms error", e)
                    emit(emptyList())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveSelectedRoom: StateFlow<StudyRoom?> = _selectedRoomId
        .flatMapLatest { roomId ->
            val repo = repository
            val user = currentUser.value
            if (roomId.isNullOrBlank() || repo == null || user == null) {
                flowOf(null)
            } else {
                repo.observeStudyRoomById(roomId).catch { e ->
                    Log.e("CloudSyncViewModel", "observeStudyRoomById error", e)
                    emit(null)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sharedCards: StateFlow<List<SharedFlashcard>> = liveSelectedRoom
        .flatMapLatest { room ->
            val repo = repository
            val user = currentUser.value
            if (room == null || repo == null || user == null) {
                flowOf(emptyList())
            } else {
                repo.observeSharedCards(room).catch { e ->
                    Log.e("CloudSyncViewModel", "observeSharedCards error", e)
                    emit(emptyList())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val participants: StateFlow<List<RoomParticipant>> = liveSelectedRoom
        .flatMapLatest { room ->
            val repo = repository
            val user = currentUser.value
            if (room == null || repo == null || user == null) {
                flowOf(emptyList())
            } else {
                repo.observeParticipants(room).catch { e ->
                    Log.e("CloudSyncViewModel", "observeParticipants error", e)
                    emit(emptyList())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomMessages: StateFlow<List<RoomMessage>> = liveSelectedRoom
        .flatMapLatest { room ->
            val repo = repository
            val user = currentUser.value
            if (room == null || repo == null || user == null) {
                flowOf(emptyList())
            } else {
                repo.observeRoomMessages(room).catch { e ->
                    Log.e("CloudSyncViewModel", "observeRoomMessages error", e)
                    emit(emptyList())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun signInWithGoogle(activityContext: Context) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _statusMessage.value = "Firebase Auth is not initialized in this environment."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            AuraGoogleAuthHelper.signInWithGoogleButton(activityContext, firebaseAuth)
                .onSuccess { user ->
                    _statusMessage.value = "Signed in as ${user.displayName ?: user.email ?: "Scholar"}"
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: "Google Sign-In was cancelled or failed."
                }
            _isBusy.value = false
        }
    }

    fun signOut() {
        val prevRoomId = _selectedRoomId.value
        val firebaseAuth = auth
        viewModelScope.launch {
            if (!prevRoomId.isNullOrBlank()) {
                runCatching { repository?.leaveStudyRoomPresence(prevRoomId) }
            }
            if (firebaseAuth != null) {
                AuraGoogleAuthHelper.signOutGoogle(getApplication(), firebaseAuth)
            }
            _selectedRoomId.value = null
            _statusMessage.value = "Signed out of AURA Cloud."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun enterRoom(
        room: StudyRoom,
        displayName: String,
        cityOrCampus: String
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        _selectedRoomId.value = room.id
        viewModelScope.launch {
            repo.upsertMyParticipantPresence(
                room = room,
                displayName = displayName.ifBlank { user.displayName ?: "Scholar" }.take(80),
                cityOrCampus = cityOrCampus.ifBlank { "Remote Campus" }.take(80),
                studyStatus = "FOCUSING",
                handRaised = false,
                reactionEmoji = "🎯"
            ).onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Unable to join room."
            }
        }
    }

    fun leaveActiveRoom() {
        val roomId = _selectedRoomId.value
        val user = currentUser.value
        val repo = repository
        _selectedRoomId.value = null
        if (!roomId.isNullOrBlank() && user != null && repo != null) {
            viewModelScope.launch {
                repo.leaveStudyRoomPresence(roomId)
            }
        }
    }

    fun joinRoomByCode(
        joinCodeInput: String,
        displayName: String,
        cityOrCampus: String
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        val cleanCode = AuraCloudRepository.normalizeJoinCode(joinCodeInput)
        if (cleanCode.length < 4) {
            _statusMessage.value = "Enter a valid 4–8 character session Join Code or invite URL."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repo.joinStudyRoomByCode(cleanCode)
                .onSuccess { found ->
                    if (found == null) {
                        _statusMessage.value = "No live session found with code '$cleanCode'. Double-check the invite code from your host."
                    } else {
                        _selectedRoomId.value = found.id
                        repo.upsertMyParticipantPresence(
                            room = found,
                            displayName = displayName.ifBlank { user.displayName ?: "Scholar" }.take(80),
                            cityOrCampus = cityOrCampus.ifBlank { "Remote Campus" }.take(80),
                            studyStatus = "FOCUSING",
                            handRaised = false,
                            reactionEmoji = "👋"
                        ).onSuccess {
                            if (found.waitingRoomEnabled && found.ownerId != user.uid && !found.coHostIds.contains(user.uid)) {
                                _statusMessage.value = "Knocked on '${found.roomTitle}'! Waiting for host ${found.ownerName} to admit you."
                            } else {
                                _statusMessage.value = "Joined '${found.roomTitle}' (${found.courseCode})!"
                            }
                        }.onFailure { err ->
                            _statusMessage.value = err.localizedMessage ?: "Could not enter room."
                        }
                    }
                }
                .onFailure { e ->
                    _statusMessage.value = "Could not join room '$cleanCode': ${e.localizedMessage ?: "error"}"
                }
            _isBusy.value = false
        }
    }

    fun saveBackup(backup: UserBackup, onComplete: () -> Unit = {}) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repo.saveUserBackup(backup.copy(userId = user.uid, id = user.uid))
                .onSuccess {
                    _statusMessage.value = "Backed up profile, settings, ${backup.coursesCount} courses, ${backup.lecturesCount} lectures & ${backup.flashcardsCount} cards! Ready on Android & Web/iPhone."
                    onComplete()
                }
                .onFailure { e ->
                    _statusMessage.value = "Cloud backup failed: ${e.localizedMessage ?: "Permission or network error"}"
                }
            _isBusy.value = false
        }
    }

    fun fetchLatestBackup(
        silentIfEmpty: Boolean = false,
        onRestored: (UserBackup) -> Unit
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repo.getUserBackup(user.uid)
                .onSuccess { backup ->
                    if (backup == null) {
                        if (!silentIfEmpty) {
                            _statusMessage.value = "No cloud snapshot found for ${user.email ?: "your account"} yet. Tap 'Backup All' to save your profile & timetable."
                        }
                    } else {
                        onRestored(backup)
                    }
                }
                .onFailure { e ->
                    if (!silentIfEmpty) {
                        _statusMessage.value = "Cloud restore failed: ${e.localizedMessage ?: "Network error"}"
                    }
                }
            _isBusy.value = false
        }
    }

    fun createRoom(
        courseCode: String,
        roomTitle: String,
        topicGoal: String,
        focusMinutes: Int,
        hostNameFallback: String,
        cityOrCampus: String = "Main Campus",
        visibility: String = "CAMPUS",
        waitingRoomEnabled: Boolean = false,
        whiteboardLocked: Boolean = false,
        initialBoardNote: String = ""
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        if (courseCode.isBlank() || roomTitle.isBlank()) {
            _statusMessage.value = "Enter both a course code and session title."
            return
        }
        val clampedMins = focusMinutes.coerceIn(5, 360)
        val hostDisplay = (user.displayName?.takeIf { it.isNotBlank() } ?: hostNameFallback.ifBlank { "Scholar" }).take(80)
        val safeVisibility = if (visibility == "UNLISTED") "UNLISTED" else "CAMPUS"
        val boardNote = initialBoardNote.trim().ifBlank {
            "Welcome to $roomTitle! Drop key formulas, questions, or sprint goals here."
        }.take(2000)

        viewModelScope.launch {
            _isBusy.value = true
            repo.createStudyRoom(
                ownerName = hostDisplay,
                courseCode = courseCode.trim().uppercase().take(24),
                roomTitle = roomTitle.trim().take(100),
                focusTopic = topicGoal.trim().ifBlank { "Collaborative Exam Prep & Deep Focus" }.take(240),
                activeMinutes = clampedMins,
                visibility = safeVisibility,
                initialBoardNote = boardNote,
                waitingRoomEnabled = waitingRoomEnabled,
                whiteboardLocked = whiteboardLocked
            ).onSuccess { roomId ->
                val joinCode = roomId.removePrefix("room_")
                val createdStub = StudyRoom(
                    id = roomId,
                    ownerId = user.uid,
                    ownerName = hostDisplay,
                    courseCode = courseCode.trim().uppercase().take(24),
                    roomTitle = roomTitle.trim().take(100),
                    focusTopic = topicGoal,
                    activeMinutes = clampedMins.toLong(),
                    visibility = safeVisibility,
                    joinCode = joinCode,
                    waitingRoomEnabled = waitingRoomEnabled,
                    whiteboardLocked = whiteboardLocked,
                    voiceRoomUrl = AuraCloudRepository.buildJitsiRoomUrl(joinCode)
                )
                repo.upsertMyParticipantPresence(
                    room = createdStub,
                    displayName = hostDisplay,
                    cityOrCampus = cityOrCampus.ifBlank { "Host Campus" }.take(80),
                    studyStatus = "FOCUSING",
                    handRaised = false,
                    reactionEmoji = "🎙️"
                )
                _selectedRoomId.value = roomId
                _statusMessage.value = "Session live! Tap QR Invite to share browser link with iPhone, Chrome, or Android friends (Code: $joinCode)."
            }.onFailure { e ->
                _statusMessage.value = "Could not host study session: ${e.localizedMessage ?: "error"}"
            }
            _isBusy.value = false
        }
    }

    fun toggleRoomTimer(room: StudyRoom) {
        val repo = repository ?: return
        if (room.status == "COMPLETED") return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val res = if (room.timerState == "RUNNING") {
                val remSec = ((room.timerEndEpochMillis - now) / 1000L).coerceIn(0L, 43_200L)
                repo.updateRoomTimer(
                    roomId = room.id,
                    timerState = "PAUSED",
                    timerEndEpochMillis = 0L,
                    timerRemainingSeconds = remSec
                )
            } else {
                val baseSec = if (room.timerRemainingSeconds > 0L) {
                    room.timerRemainingSeconds
                } else {
                    (room.activeMinutes.coerceAtLeast(1L)) * 60L
                }
                repo.updateRoomTimer(
                    roomId = room.id,
                    timerState = "RUNNING",
                    timerEndEpochMillis = now + baseSec * 1000L,
                    timerRemainingSeconds = baseSec
                )
            }
            res.onFailure { e ->
                _statusMessage.value = "Could not sync room timer: ${e.localizedMessage ?: "error"}"
            }
        }
    }

    fun resetOrSetRoomTimerMinutes(room: StudyRoom, durationMinutes: Int) {
        val repo = repository ?: return
        if (room.status == "COMPLETED") return
        val clampedMins = durationMinutes.coerceIn(1, 360).toLong()
        val secs = clampedMins * 60L
        viewModelScope.launch {
            repo.updateRoomTimer(
                roomId = room.id,
                timerState = "IDLE",
                timerEndEpochMillis = 0L,
                timerRemainingSeconds = secs,
                activeMinutes = clampedMins
            )
        }
    }

    fun saveSharedBoardNote(room: StudyRoom, updatedNote: String) {
        val repo = repository ?: return
        if (room.status == "COMPLETED") return
        viewModelScope.launch {
            repo.updateSharedBoardNote(room.id, updatedNote.take(2000))
                .onSuccess {
                    _statusMessage.value = "Shared pinboard synced across Android & Web/iPhone scholars!"
                }
                .onFailure { e ->
                    _statusMessage.value = "Could not update shared board: ${e.localizedMessage ?: "Host may have locked the chalkboard"}"
                }
        }
    }

    fun updateHostSecurity(room: StudyRoom, waitingRoomEnabled: Boolean, whiteboardLocked: Boolean) {
        val repo = repository ?: return
        if (room.status == "COMPLETED") return
        viewModelScope.launch {
            repo.updateHostSecuritySettings(room.id, waitingRoomEnabled, whiteboardLocked)
                .onSuccess {
                    val mode = if (whiteboardLocked) "Host-Only Chalkboard" else "Open Group Pinboard"
                    val wait = if (waitingRoomEnabled) "Waiting Room ON" else "Instant Entry"
                    _statusMessage.value = "Updated Host Settings: $wait • $mode"
                }
                .onFailure { e ->
                    _statusMessage.value = "Unable to update host settings: ${e.localizedMessage}"
                }
        }
    }

    fun toggleCoHost(room: StudyRoom, targetUid: String) {
        val repo = repository ?: return
        if (room.status == "COMPLETED" || targetUid == room.ownerId) return
        val updated = if (room.coHostIds.contains(targetUid)) {
            room.coHostIds - targetUid
        } else {
            (room.coHostIds + targetUid).take(10)
        }
        viewModelScope.launch {
            repo.setRoomCoHosts(room.id, updated)
                .onSuccess {
                    val promoted = updated.contains(targetUid)
                    _statusMessage.value = if (promoted) "Promoted scholar to Co-Host!" else "Removed Co-Host privilege."
                }
        }
    }

    fun admitOrRemoveParticipant(room: StudyRoom, participantUid: String, admissionStatus: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.moderateParticipant(room.id, participantUid, admissionStatus)
                .onSuccess {
                    _statusMessage.value = when (admissionStatus) {
                        "ADMITTED" -> "Admitted scholar into the live stage!"
                        "REMOVED" -> "Removed participant from session."
                        else -> "Updated participant status."
                    }
                }
                .onFailure { e ->
                    _statusMessage.value = "Moderation action failed: ${e.localizedMessage}"
                }
        }
    }

    fun lowerParticipantHand(room: StudyRoom, participantUid: String, currentAdmission: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.moderateParticipant(
                roomId = room.id,
                participantUid = participantUid,
                admissionStatus = currentAdmission.ifBlank { "ADMITTED" },
                handRaised = false
            )
        }
    }

    fun launchLivePoll(
        room: StudyRoom,
        question: String,
        options: List<String>,
        correctIndex: Int
    ) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.launchStagePoll(room.id, question, options, correctIndex)
                .onSuccess {
                    _statusMessage.value = "Live Pop-Quiz Poll pushed to all Android & Browser screens!"
                }
                .onFailure { e ->
                    _statusMessage.value = e.localizedMessage ?: "Failed to launch poll."
                }
        }
    }

    fun revealLivePoll(room: StudyRoom, revealed: Boolean) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.setStagePollRevealed(room.id, revealed)
        }
    }

    fun closeLivePoll(room: StudyRoom) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.clearStagePoll(room.id)
        }
    }

    fun voteInLivePoll(room: StudyRoom, optionIndex: Int) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.submitPollVote(room.id, optionIndex)
                .onFailure { e ->
                    _statusMessage.value = "Could not record vote: ${e.localizedMessage}"
                }
        }
    }

    fun pinSpotlightText(room: StudyRoom, text: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.updateSpotlightCardText(room.id, text)
                .onSuccess {
                    _statusMessage.value = if (text.isBlank()) "Cleared stage spotlight." else "Pinned to top stage spotlight for everyone!"
                }
        }
    }

    fun updateMyPresence(
        room: StudyRoom,
        displayName: String,
        cityOrCampus: String,
        status: String,
        handRaised: Boolean,
        reactionEmoji: String
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        viewModelScope.launch {
            repo.upsertMyParticipantPresence(
                room = room,
                displayName = displayName.ifBlank { user.displayName ?: "Scholar" }.take(80),
                cityOrCampus = cityOrCampus.ifBlank { "Remote Campus" }.take(80),
                studyStatus = status,
                handRaised = handRaised,
                reactionEmoji = reactionEmoji.take(16)
            )
        }
    }

    fun sendRoomChatMessage(
        room: StudyRoom,
        authorNameFallback: String,
        messageText: String,
        messageType: String = "CHAT"
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        if (room.status == "COMPLETED" || messageText.isBlank()) return
        val author = (user.displayName?.takeIf { it.isNotBlank() } ?: authorNameFallback.ifBlank { "Scholar" }).take(80)
        viewModelScope.launch {
            repo.sendRoomMessage(
                room = room,
                authorName = author,
                text = messageText.trim().take(600),
                messageType = messageType
            ).onFailure { e ->
                _statusMessage.value = "Could not send message: ${e.localizedMessage ?: "error"}"
            }
        }
    }

    fun completeRoom(room: StudyRoom) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.completeStudyRoom(room.id)
                .onSuccess { _statusMessage.value = "Completed & locked '${room.roomTitle}'. Tap 'Session Recap' to save or share notes!" }
                .onFailure { e -> _statusMessage.value = "Unable to complete room: ${e.localizedMessage}" }
        }
    }

    fun deleteRoom(roomId: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.deleteStudyRoom(roomId)
                .onSuccess {
                    if (_selectedRoomId.value == roomId) _selectedRoomId.value = null
                    _statusMessage.value = "Removed study session."
                }
                .onFailure { e -> _statusMessage.value = "Unable to delete room: ${e.localizedMessage}" }
        }
    }

    fun postSharedCard(
        room: StudyRoom,
        courseCode: String,
        question: String,
        answer: String,
        authorNameFallback: String
    ) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        if (question.isBlank() || answer.isBlank()) {
            _statusMessage.value = "Both question and answer are required to share a card."
            return
        }
        val author = (user.displayName?.takeIf { it.isNotBlank() } ?: authorNameFallback.ifBlank { "Scholar" }).take(80)
        viewModelScope.launch {
            repo.addSharedFlashcard(
                room = room,
                authorName = author,
                courseCode = courseCode.trim().uppercase().ifBlank { "GEN" }.take(24),
                question = question.trim().take(500),
                answer = answer.trim().take(1000)
            ).onSuccess {
                _statusMessage.value = "Shared flashcard to live study room!"
            }.onFailure { e ->
                _statusMessage.value = "Could not share card: ${e.localizedMessage}"
            }
        }
    }
}
