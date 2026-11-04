package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class AuraCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. User Academic & Full Profile Backup / Cross-Device Sync ---

    fun observeUserBackup(userId: String): Flow<UserBackup?> {
        val docRef = db.collection("user_backups").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserBackup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun getUserBackup(userId: String): Result<UserBackup?> {
        val docRef = db.collection("user_backups").document(userId)
        return try {
            val snap = docRef.get().await()
            val model = if (snap.exists()) {
                snap.toObject(UserBackup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(model)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveUserBackup(backup: UserBackup): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("user_backups").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val sanitizedBackup = backup.copy(
                id = uid,
                userId = uid,
                studentName = backup.studentName.trim().ifBlank { "Scholar" }.take(100),
                institution = backup.institution.trim().take(150),
                department = backup.department.trim().take(150),
                targetGpa = backup.targetGpa.coerceIn(0.0, 5.0),
                coursesCount = backup.coursesCount.coerceIn(0L, 500L),
                lecturesCount = backup.lecturesCount.coerceIn(0L, 1000L),
                assessmentsCount = backup.assessmentsCount.coerceIn(0L, 1000L),
                flashcardsCount = backup.flashcardsCount.coerceIn(0L, 5000L),
                totalFocusMinutes = backup.totalFocusMinutes.coerceIn(0L, 1_000_000L),
                profileSettings = backup.profileSettings.take(40).map { it.take(500) },
                courseRecords = backup.courseRecords.take(50).map { it.take(500) },
                lectureRecords = backup.lectureRecords.take(80).map { it.take(500) },
                assessmentRecords = backup.assessmentRecords.take(80).map { it.take(500) },
                flashcardRecords = backup.flashcardRecords.take(100).map { it.take(500) },
                noteRecords = backup.noteRecords.take(80).map { it.take(1500) },
                gradeRecords = backup.gradeRecords.take(60).map { it.take(500) },
                expenseRecords = backup.expenseRecords.take(100).map { it.take(500) }
            )
            if (existingSnap.exists()) {
                val existingCreated = existingSnap.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                docRef.set(sanitizedBackup.copy(createdAt = existingCreated, updatedAt = null)).await()
            } else {
                docRef.set(sanitizedBackup.copy(createdAt = null, updatedAt = null)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 2. Live Collaborative & Cross-Platform Study Rooms ---

    fun observeCampusStudyRooms(): Flow<List<StudyRoom>> {
        val path = "study_rooms"
        return db.collection(path)
            .whereEqualTo("visibility", "CAMPUS")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyStudyRooms(userId: String): Flow<List<StudyRoom>> {
        val path = "study_rooms"
        return db.collection(path)
            .whereEqualTo("ownerId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeStudyRoomById(roomId: String): Flow<StudyRoom?> {
        val docRef = db.collection("study_rooms").document(roomId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(StudyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun getStudyRoomById(roomId: String): Result<StudyRoom?> {
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = if (snap.exists()) {
                snap.toObject(StudyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinStudyRoomByCode(rawCode: String): Result<StudyRoom?> {
        requireUserId()
        val normalized = normalizeJoinCode(rawCode)
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid room invite code."))
        }
        val candidateDocId = if (rawCode.trim().startsWith("room_")) {
            rawCode.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(128)
        } else {
            "room_$normalized"
        }
        return getStudyRoomById(candidateDocId)
    }

    suspend fun createStudyRoom(
        ownerName: String,
        courseCode: String,
        roomTitle: String,
        focusTopic: String,
        activeMinutes: Int,
        visibility: String = "CAMPUS",
        initialBoardNote: String = "",
        waitingRoomEnabled: Boolean = false,
        whiteboardLocked: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val joinCode = generateJoinCode()
        val roomId = "room_$joinCode"
        val docRef = db.collection("study_rooms").document(roomId)
        val clampedMins = activeMinutes.coerceIn(1, 720).toLong()
        val normalizedVisibility = when (visibility) {
            "PRIVATE" -> "PRIVATE"
            "UNLISTED" -> "UNLISTED"
            else -> "CAMPUS"
        }
        val voiceUrl = buildJitsiRoomUrl(joinCode)
        val model = StudyRoom(
            id = roomId,
            ownerId = uid,
            ownerName = ownerName.trim().ifBlank { "Scholar" }.take(80),
            courseCode = courseCode.trim().ifBlank { "GEN 101" }.take(24),
            roomTitle = roomTitle.trim().let { if (it.length < 2) "Deep Focus Room" else it }.take(120),
            focusTopic = focusTopic.trim().take(240),
            activeMinutes = clampedMins,
            visibility = normalizedVisibility,
            status = "ACTIVE",
            joinCode = joinCode,
            timerState = "IDLE",
            timerEndEpochMillis = 0L,
            timerRemainingSeconds = clampedMins * 60L,
            sharedBoardNote = initialBoardNote.trim().take(2000),
            waitingRoomEnabled = waitingRoomEnabled,
            whiteboardLocked = whiteboardLocked,
            coHostIds = emptyList(),
            voiceRoomUrl = voiceUrl,
            activePollQuestion = "",
            activePollOptions = emptyList(),
            activePollCorrectIndex = -1L,
            activePollRevealed = false,
            spotlightCardText = "",
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(model).await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomTimer(
        roomId: String,
        timerState: String,
        timerEndEpochMillis: Long,
        timerRemainingSeconds: Long,
        activeMinutes: Long? = null
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        val safeState = when (timerState) {
            "RUNNING", "PAUSED" -> timerState
            else -> "IDLE"
        }
        val updates = mutableMapOf<String, Any>(
            "timerState" to safeState,
            "timerEndEpochMillis" to timerEndEpochMillis.coerceAtLeast(0L),
            "timerRemainingSeconds" to timerRemainingSeconds.coerceIn(0L, 43_200L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (activeMinutes != null) {
            updates["activeMinutes"] = activeMinutes.coerceIn(1L, 720L)
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSharedBoardNote(roomId: String, sharedBoardNote: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "sharedBoardNote" to sharedBoardNote.trim().take(2000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateHostSecuritySettings(
        roomId: String,
        waitingRoomEnabled: Boolean,
        whiteboardLocked: Boolean
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "waitingRoomEnabled" to waitingRoomEnabled,
                    "whiteboardLocked" to whiteboardLocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setRoomCoHosts(roomId: String, coHostIds: List<String>): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        val sanitized = coHostIds.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(10)
        return try {
            docRef.update(
                mapOf(
                    "coHostIds" to sanitized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun launchStagePoll(
        roomId: String,
        question: String,
        options: List<String>,
        correctIndex: Int = -1
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        val cleanOpts = options.map { it.trim().take(160) }.filter { it.isNotBlank() }.take(4)
        if (question.isBlank() || cleanOpts.size < 2) {
            return Result.failure(IllegalArgumentException("Enter a poll question and at least 2 options."))
        }
        val clampedCorrect = if (correctIndex in cleanOpts.indices) correctIndex.toLong() else -1L
        return try {
            docRef.update(
                mapOf(
                    "activePollQuestion" to question.trim().take(300),
                    "activePollOptions" to cleanOpts,
                    "activePollCorrectIndex" to clampedCorrect,
                    "activePollRevealed" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setStagePollRevealed(roomId: String, revealed: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "activePollRevealed" to revealed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearStagePoll(roomId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "activePollQuestion" to "",
                    "activePollOptions" to emptyList<String>(),
                    "activePollCorrectIndex" to -1L,
                    "activePollRevealed" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSpotlightCardText(roomId: String, spotlightText: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "spotlightCardText" to spotlightText.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completeStudyRoom(roomId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "COMPLETED",
                    "timerState" to "IDLE",
                    "timerRemainingSeconds" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyRoom(roomId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3. Meet-Style Live Participant Roster (Waiting Room + Poll Votes + Moderation) ---

    fun observeParticipants(room: StudyRoom): Flow<List<RoomParticipant>> {
        val collectionPath = "study_rooms/${room.id}/participants"
        val baseCollection = db.collection("study_rooms").document(room.id).collection("participants")
        val alignedQuery = if (room.visibility == "CAMPUS" || room.visibility == "UNLISTED") {
            baseCollection.whereEqualTo("roomVisibility", room.visibility)
        } else {
            baseCollection.whereEqualTo("roomOwnerId", room.ownerId)
        }
        return alignedQuery
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RoomParticipant::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<RoomParticipant> { it.userId == room.ownerId }
                        .thenByDescending { room.coHostIds.contains(it.userId) }
                        .thenByDescending { it.handRaised }
                        .thenBy { it.displayName }
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, collectionPath)
                throw error
            }
    }

    suspend fun upsertMyParticipantPresence(
        room: StudyRoom,
        displayName: String,
        cityOrCampus: String,
        studyStatus: String = "FOCUSING",
        handRaised: Boolean = false,
        reactionEmoji: String = "",
        focusMinutesLogged: Long = 0L,
        pollVoteIndex: Int? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("study_rooms").document(room.id).collection("participants").document(uid)
        val safeStatus = when (studyStatus) {
            "BREAK", "QUIZZING", "READY" -> studyStatus
            else -> "FOCUSING"
        }
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingAdmission = existingSnap.getString("admissionStatus") ?: "ADMITTED"
                if (existingAdmission == "REMOVED") {
                    return Result.failure(IllegalStateException("You were removed from this session by the host."))
                }
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim().ifBlank { "Scholar" }.take(80),
                    "cityOrCampus" to cityOrCampus.trim().take(80),
                    "studyStatus" to safeStatus,
                    "handRaised" to handRaised,
                    "reactionEmoji" to reactionEmoji.trim().take(16),
                    "focusMinutesLogged" to focusMinutesLogged.coerceIn(0L, 10_000L),
                    "devicePlatform" to "ANDROID",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (pollVoteIndex != null) {
                    updates["pollVoteIndex"] = pollVoteIndex.coerceIn(-1, 3).toLong()
                }
                docRef.update(updates).await()
            } else {
                val isHostOrCoHost = room.ownerId == uid || room.coHostIds.contains(uid)
                val initialAdmission = if (!isHostOrCoHost && room.waitingRoomEnabled) "WAITING" else "ADMITTED"
                val model = RoomParticipant(
                    id = uid,
                    roomId = room.id,
                    roomOwnerId = room.ownerId,
                    roomVisibility = room.visibility,
                    userId = uid,
                    displayName = displayName.trim().ifBlank { "Scholar" }.take(80),
                    cityOrCampus = cityOrCampus.trim().take(80),
                    studyStatus = safeStatus,
                    handRaised = handRaised,
                    reactionEmoji = reactionEmoji.trim().take(16),
                    focusMinutesLogged = focusMinutesLogged.coerceIn(0L, 10_000L),
                    admissionStatus = initialAdmission,
                    pollVoteIndex = (pollVoteIndex ?: -1).coerceIn(-1, 3).toLong(),
                    devicePlatform = "ANDROID",
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(model).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitPollVote(roomId: String, voteIndex: Int): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("study_rooms").document(roomId).collection("participants").document(uid)
        return try {
            docRef.update(
                mapOf(
                    "pollVoteIndex" to voteIndex.coerceIn(-1, 3).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun moderateParticipant(
        roomId: String,
        participantUid: String,
        admissionStatus: String,
        handRaised: Boolean? = null
    ): Result<Unit> {
        requireUserId()
        val safeAdmission = when (admissionStatus) {
            "WAITING", "REMOVED" -> admissionStatus
            else -> "ADMITTED"
        }
        val docRef = db.collection("study_rooms").document(roomId).collection("participants").document(participantUid)
        val updates = mutableMapOf<String, Any>(
            "admissionStatus" to safeAdmission,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (handRaised != null) {
            updates["handRaised"] = handRaised
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveStudyRoomPresence(roomId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("study_rooms").document(roomId).collection("participants").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists() && snap.getString("admissionStatus") != "REMOVED") {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 4. Live Study Room Chat & Q&A Stream ---

    fun observeRoomMessages(room: StudyRoom): Flow<List<RoomMessage>> {
        val collectionPath = "study_rooms/${room.id}/messages"
        val baseCollection = db.collection("study_rooms").document(room.id).collection("messages")
        val alignedQuery = if (room.visibility == "CAMPUS" || room.visibility == "UNLISTED") {
            baseCollection.whereEqualTo("roomVisibility", room.visibility)
        } else {
            baseCollection.whereEqualTo("roomOwnerId", room.ownerId)
        }
        return alignedQuery
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RoomMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, collectionPath)
                throw error
            }
    }

    suspend fun sendRoomMessage(
        room: StudyRoom,
        authorName: String,
        text: String,
        messageType: String = "CHAT"
    ): Result<String> {
        val uid = requireUserId()
        val trimmedText = text.trim().take(600)
        if (trimmedText.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("study_rooms").document(room.id).collection("messages").document(msgId)
        val safeType = when (messageType) {
            "QUESTION", "INSIGHT" -> messageType
            else -> "CHAT"
        }
        val model = RoomMessage(
            id = msgId,
            roomId = room.id,
            roomOwnerId = room.ownerId,
            roomVisibility = room.visibility,
            authorId = uid,
            authorName = authorName.trim().ifBlank { "Scholar" }.take(80),
            text = trimmedText,
            messageType = safeType,
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(model).await()
            Result.success(msgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 5. Shared Flashcard Deck inside Study Rooms ---

    fun observeSharedCards(room: StudyRoom): Flow<List<SharedFlashcard>> {
        val collectionPath = "study_rooms/${room.id}/shared_cards"
        val baseCollection = db.collection("study_rooms").document(room.id).collection("shared_cards")
        val alignedQuery = if (room.visibility == "CAMPUS" || room.visibility == "UNLISTED") {
            baseCollection.whereEqualTo("roomVisibility", room.visibility)
        } else {
            baseCollection.whereEqualTo("roomOwnerId", room.ownerId)
        }
        return alignedQuery
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SharedFlashcard::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, collectionPath)
                throw error
            }
    }

    suspend fun addSharedFlashcard(
        room: StudyRoom,
        authorName: String,
        courseCode: String,
        question: String,
        answer: String
    ): Result<String> {
        val uid = requireUserId()
        val cardId = "card_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("study_rooms").document(room.id).collection("shared_cards").document(cardId)
        val model = SharedFlashcard(
            id = cardId,
            roomId = room.id,
            roomOwnerId = room.ownerId,
            roomVisibility = room.visibility,
            authorId = uid,
            authorName = authorName.trim().ifBlank { "Scholar" }.take(80),
            courseCode = courseCode.trim().ifBlank { room.courseCode.ifBlank { "GEN" } }.take(24),
            question = question.trim().let { if (it.length < 2) "Concept?" else it }.take(500),
            answer = answer.trim().ifBlank { "Answer" }.take(1000),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(model).await()
            Result.success(cardId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSharedFlashcard(roomId: String, cardId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("study_rooms").document(roomId).collection("shared_cards").document(cardId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val DEFAULT_WEB_PORTAL_URL = "https://aura-study-companion.vercel.app"

        fun generateJoinCode(length: Int = 6): String {
            val raw = UUID.randomUUID().toString().replace("-", "")
            return buildString(length) {
                for (i in 0 until length) {
                    val idx = raw[i * 2].code xor raw[i * 2 + 1].code
                    append(CODE_ALPHABET[kotlin.math.abs(idx) % CODE_ALPHABET.length])
                }
            }
        }

        fun normalizeJoinCode(input: String): String {
            val trimmed = input.trim()
            val extracted = if (trimmed.contains("room=", ignoreCase = true)) {
                trimmed.substringAfter("room=").substringBefore("&")
            } else {
                trimmed
            }
            return extracted
                .uppercase(Locale.US)
                .removePrefix("ROOM_")
                .replace(Regex("[^A-Z0-9]"), "")
                .take(12)
        }

        fun buildJitsiRoomUrl(joinCode: String): String {
            val clean = normalizeJoinCode(joinCode).ifBlank { "LIVE" }
            return "https://meet.jit.si/AuraStudy-$clean"
        }

        fun buildBrowserJoinUrl(basePortalUrl: String, joinCode: String): String {
            val cleanBase = basePortalUrl.trim()
                .ifBlank { DEFAULT_WEB_PORTAL_URL }
                .trimEnd('/')
            val cleanCode = normalizeJoinCode(joinCode)
            return "$cleanBase/?room=$cleanCode"
        }
    }
}
