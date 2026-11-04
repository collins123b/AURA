package com.example.data.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserBackup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val institution: String = "",
    val department: String = "",
    val targetGpa: Double = 4.0,
    val coursesCount: Long = 0L,
    val lecturesCount: Long = 0L,
    val assessmentsCount: Long = 0L,
    val flashcardsCount: Long = 0L,
    val totalFocusMinutes: Long = 0L,
    val profileSettings: List<String> = emptyList(),
    val courseRecords: List<String> = emptyList(),
    val lectureRecords: List<String> = emptyList(),
    val assessmentRecords: List<String> = emptyList(),
    val flashcardRecords: List<String> = emptyList(),
    val noteRecords: List<String> = emptyList(),
    val gradeRecords: List<String> = emptyList(),
    val expenseRecords: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyRoom(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val courseCode: String = "",
    val roomTitle: String = "",
    val focusTopic: String = "",
    val activeMinutes: Long = 25L,
    val visibility: String = "CAMPUS",
    val status: String = "ACTIVE",
    val joinCode: String = "",
    val timerState: String = "IDLE",
    val timerEndEpochMillis: Long = 0L,
    val timerRemainingSeconds: Long = 1500L,
    val sharedBoardNote: String = "",
    val waitingRoomEnabled: Boolean = false,
    val whiteboardLocked: Boolean = false,
    val coHostIds: List<String> = emptyList(),
    val voiceRoomUrl: String = "",
    val activePollQuestion: String = "",
    val activePollOptions: List<String> = emptyList(),
    val activePollCorrectIndex: Long = -1L,
    val activePollRevealed: Boolean = false,
    val spotlightCardText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SharedFlashcard(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomOwnerId: String = "",
    val roomVisibility: String = "CAMPUS",
    val authorId: String = "",
    val authorName: String = "",
    val courseCode: String = "",
    val question: String = "",
    val answer: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomParticipant(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomOwnerId: String = "",
    val roomVisibility: String = "CAMPUS",
    val userId: String = "",
    val displayName: String = "",
    val cityOrCampus: String = "",
    val studyStatus: String = "FOCUSING",
    val handRaised: Boolean = false,
    val reactionEmoji: String = "",
    val focusMinutesLogged: Long = 0L,
    val admissionStatus: String = "ADMITTED",
    val pollVoteIndex: Long = -1L,
    val devicePlatform: String = "ANDROID",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomOwnerId: String = "",
    val roomVisibility: String = "CAMPUS",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val messageType: String = "CHAT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
