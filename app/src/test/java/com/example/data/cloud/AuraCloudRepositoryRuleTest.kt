package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AuraCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndGetUserBackup_authenticatedOwner_persistsFullProfileAndAcademicRecords() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(8)}@aura.test")
        val repository = AuraCloudRepository(firestore)

        val initialBackup = UserBackup(
            userId = aliceUid,
            studentName = "Ada Lovelace",
            institution = "University of Oxford",
            department = "Computer Science",
            targetGpa = 4.0,
            coursesCount = 2L,
            lecturesCount = 4L,
            assessmentsCount = 1L,
            flashcardsCount = 6L,
            totalFocusMinutes = 150L,
            profileSettings = listOf(
                "email=ada@ox.ac.uk",
                "academicLevel=400L",
                "semester=Semester 2",
                "accentPreset=AURORA_MINT",
                "widgetGlassStyle=CRYSTAL_CLEAR",
                "webPortalUrl=https://aura-study-companion.vercel.app"
            ),
            courseRecords = listOf("CSC301||Algorithms||3||Prof. Knuth||LT-1||#38BDF8"),
            lectureRecords = listOf("CSC301||Algorithms||1||09:00||11:00||LT-1||10||#38BDF8||Prof. Knuth||true||15||AURORA_CHIME"),
            assessmentRecords = listOf("CSC301||Algorithms||Midterm||EXAM||1795000000000||PENDING||Main Hall||B-14||109283||HIGH||true"),
            flashcardRecords = listOf("CSC301||Algorithms Deck||What is Big-O?||Asymptotic upper bound||2.5||3||2"),
            noteRecords = listOf("CSC301||Graph Theory||CONCEPT||false||BFS uses a queue"),
            gradeRecords = listOf("CSC301||Algorithms||3||A||4.0||Semester 1"),
            expenseRecords = listOf("CLRS Book||BOOKS||55.0||CSC301||1795000000000")
        )

        val firstWrite = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveUserBackup(initialBackup) }
        assertTrue("Expected initial backup write to succeed: ${firstWrite.exceptionOrNull()}", firstWrite.isSuccess)

        val updatedWrite = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserBackup(initialBackup.copy(totalFocusMinutes = 240L))
        }
        assertTrue("Expected backup update to succeed: ${updatedWrite.exceptionOrNull()}", updatedWrite.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserBackup(aliceUid).getOrThrow() }
        assertNotNull(fetched)
        assertEquals(240L, fetched?.totalFocusMinutes)
        assertEquals("Ada Lovelace", fetched?.studentName)
        assertTrue(fetched?.profileSettings?.contains("academicLevel=400L") == true)
        assertEquals(1, fetched?.noteRecords?.size)
        assertEquals(1, fetched?.gradeRecords?.size)
    }

    @Test
    fun getUserBackup_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID().toString().take(8)}@aura.test")
        val aliceRepo = AuraCloudRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserBackup(
                UserBackup(userId = aliceUid, studentName = "Alice Private")
            ).getOrThrow()
        }

        signInTestUser("bob_iso_${UUID.randomUUID().toString().take(8)}@aura.test")
        val bobRepo = AuraCloudRepository(firestore)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUserBackup(aliceUid) }
        assertTrue(crossRead.isFailure)
        val fsEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun crossTownStudyRoom_waitingRoomPollsWhiteboardLockAndTerminalLock_workEndToEnd() = runBlocking {
        val aliceEmail = "alice_host_${UUID.randomUUID().toString().take(8)}@aura.test"
        val aliceUid = signInTestUser(aliceEmail)
        val hostRepo = AuraCloudRepository(firestore)

        // Alice hosts an UNLISTED room with Waiting Room + Whiteboard Lock enabled
        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.createStudyRoom(
                ownerName = "Alice (Accra)",
                courseCode = "CSC 402",
                roomTitle = "Operating Systems Cross-Platform Meet",
                focusTopic = "Virtual Memory & Page Tables",
                activeMinutes = 45,
                visibility = "UNLISTED",
                initialBoardNote = "Chapter 9: Demand Paging",
                waitingRoomEnabled = true,
                whiteboardLocked = true
            ).getOrThrow()
        }

        val hostRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.getStudyRoomById(roomId).getOrThrow()
        }
        assertNotNull(hostRoom)
        assertEquals(aliceUid, hostRoom!!.ownerId)
        assertTrue("Expected 6-char join code", hostRoom.joinCode.length == 6)
        assertTrue(hostRoom.voiceRoomUrl.startsWith("https://meet.jit.si/AuraStudy-"))

        // Host launches a live Pop-Quiz Poll & starts synced countdown timer
        val pollLaunch = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.launchStagePoll(
                roomId = roomId,
                question = "Which page replacement algorithm suffers from Belady's anomaly?",
                options = listOf("LRU", "FIFO", "Optimal", "Clock"),
                correctIndex = 1
            )
        }
        assertTrue("Host poll launch should succeed: ${pollLaunch.exceptionOrNull()}", pollLaunch.isSuccess)

        val timerStart = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.updateRoomTimer(
                roomId = roomId,
                timerState = "RUNNING",
                timerEndEpochMillis = System.currentTimeMillis() + 2_700_000L,
                timerRemainingSeconds = 2700L
            )
        }
        assertTrue("Host timer start should succeed: ${timerStart.exceptionOrNull()}", timerStart.isSuccess)

        // Bob in another town joins using the 6-char Join Code -> enters WAITING room automatically!
        val bobEmail = "bob_remote_${UUID.randomUUID().toString().take(8)}@aura.test"
        val bobUid = signInTestUser(bobEmail)
        val peerRepo = AuraCloudRepository(firestore)
        val joinedRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.joinStudyRoomByCode(hostRoom.joinCode).getOrThrow()
        }
        assertNotNull(joinedRoom)
        assertTrue(joinedRoom!!.waitingRoomEnabled)

        val knockRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.upsertMyParticipantPresence(
                room = joinedRoom,
                displayName = "Bob (Kumasi)",
                cityOrCampus = "Kumasi • KNUST",
                studyStatus = "READY",
                handRaised = false,
                reactionEmoji = "👋"
            )
        }
        assertTrue("Participant knock write should succeed: ${knockRes.exceptionOrNull()}", knockRes.isSuccess)

        // While WAITING, Bob cannot post chat messages!
        val blockedEarlyMsg = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.sendRoomMessage(joinedRoom, "Bob", "Am I in yet?", "CHAT")
        }
        assertTrue("Waiting participant should be blocked from chat", blockedEarlyMsg.isFailure)

        // Host Alice admits Bob AND unlocks whiteboard!
        signInTestUser(aliceEmail)
        val admitRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.moderateParticipant(roomId, bobUid, admissionStatus = "ADMITTED")
        }
        assertTrue("Host admit should succeed: ${admitRes.exceptionOrNull()}", admitRes.isSuccess)

        val unlockRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.updateHostSecuritySettings(roomId, waitingRoomEnabled = true, whiteboardLocked = false)
        }
        assertTrue(unlockRes.isSuccess)

        // Bob is now ADMITTED: he votes in the live poll, posts Q&A chat, and co-edits unlocked whiteboard!
        signInTestUser(bobEmail)
        val voteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.submitPollVote(roomId, voteIndex = 1)
        }
        assertTrue("Poll vote should succeed: ${voteRes.exceptionOrNull()}", voteRes.isSuccess)

        val msgRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.sendRoomMessage(
                room = joinedRoom,
                authorName = "Bob (Kumasi)",
                text = "Voted B (FIFO)!",
                messageType = "QUESTION"
            )
        }
        assertTrue("Admitted room message should succeed: ${msgRes.exceptionOrNull()}", msgRes.isSuccess)

        val boardRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.updateSharedBoardNote(roomId, "EAT = (1 - p) * ma + p * page_fault_time")
        }
        assertTrue("Peer shared pinboard co-edit should succeed: ${boardRes.exceptionOrNull()}", boardRes.isSuccess)

        // Bob CANNOT hijack host timer controls
        val hijackTimer = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.updateRoomTimer(roomId, "PAUSED", 0L, 1200L)
        }
        assertTrue(hijackTimer.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            hijackTimer.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Host signs back in and completes the room -> terminal lock enforced
        signInTestUser(aliceEmail)
        val completeRes = withTimeout(DEFAULT_TIMEOUT_MS) { hostRepo.completeStudyRoom(roomId) }
        assertTrue(completeRes.isSuccess)

        val secondComplete = withTimeout(DEFAULT_TIMEOUT_MS) { hostRepo.completeStudyRoom(roomId) }
        assertTrue(secondComplete.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            secondComplete.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun observeCampusStudyRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = AuraCloudRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCampusStudyRooms().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected unauthenticated query to throw", caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
