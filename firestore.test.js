const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validBackupPayload(uid, serverTimestamp) {
  return {
    userId: uid,
    studentName: "Ada Lovelace",
    institution: "MIT",
    department: "Computer Science",
    targetGpa: 4.0,
    coursesCount: 2,
    lecturesCount: 3,
    assessmentsCount: 1,
    flashcardsCount: 4,
    totalFocusMinutes: 120,
    profileSettings: [
      "email=ada@mit.edu",
      "academicLevel=300L",
      "semester=Semester 2",
      "accentPreset=AURORA_MINT",
      "isDarkTheme=true",
    ],
    courseRecords: ["CSC301||Algorithms||3||Prof. Knuth||Hall A||#38BDF8"],
    lectureRecords: ["CSC301||Algorithms||1||09:00||11:00||Hall A||10||#38BDF8||Prof. Knuth||true||15||AURORA_CHIME"],
    assessmentRecords: ["CSC301||Algorithms||Midterm||EXAM||1795000000000||PENDING||Hall A||B-12||109283||HIGH||true"],
    flashcardRecords: ["CSC301||Deck 1||What is DP?||Optimal substructure + memoization||2.5||3||2"],
    noteRecords: ["CSC301||DP Lecture Notes||CONCEPT||false||Memoization + Tabulation"],
    gradeRecords: ["CSC301||Algorithms||3||A||4.0||Semester 1"],
    expenseRecords: ["Algorithm Design Manual||BOOKS||45.5||CSC301||1795000000000"],
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

function validRoomPayload(uid, visibility, serverTimestamp, extra = {}) {
  return {
    ownerId: uid,
    ownerName: "Ada Lovelace",
    courseCode: "CSC 301",
    roomTitle: "Algorithms Midterm Sprint",
    focusTopic: "Dynamic Programming & Graph Shortest Paths",
    activeMinutes: 50,
    visibility,
    status: "ACTIVE",
    joinCode: "AUR7K2",
    timerState: "IDLE",
    timerEndEpochMillis: 0,
    timerRemainingSeconds: 3000,
    sharedBoardNote: "Agenda: 1) Bellman-Ford 2) Floyd-Warshall",
    waitingRoomEnabled: false,
    whiteboardLocked: false,
    coHostIds: [],
    voiceRoomUrl: "https://meet.jit.si/AuraStudy-AUR7K2",
    activePollQuestion: "",
    activePollOptions: [],
    activePollCorrectIndex: -1,
    activePollRevealed: false,
    spotlightCardText: "",
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
    ...extra,
  };
}

// --- DIRTY DOZEN SECURITY BOUNDS & CROSS-PLATFORM MEETING SUITE TESTS ---

test("1. Unauthenticated user cannot read user_backups or study_rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("user_backups").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("study_rooms").where("visibility", "==", "CAMPUS").get()
  );
});

test("2. Cross-user isolation: Bob cannot read or overwrite Alice's full profile backup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("user_backups").doc(ALICE_UID).set(validBackupPayload(ALICE_UID, ts))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("user_backups").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("user_backups").doc(ALICE_UID).set(validBackupPayload(BOB_UID, ts))
  );
});

test("3. Identity spoofing: Alice cannot create a StudyRoom claiming Bob as ownerId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("study_rooms").doc("room_spoof").set(validRoomPayload(BOB_UID, "CAMPUS", ts))
  );
});

test("4. Ghost field shadow update: rejecting undocumented fields on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("user_backups").doc(ALICE_UID).set({
      ...validBackupPayload(ALICE_UID, ts),
      isAdmin: true,
    })
  );

  await assertSucceeds(
    aliceDb.collection("user_backups").doc(ALICE_UID).set(validBackupPayload(ALICE_UID, ts))
  );
  await assertFails(
    aliceDb.collection("user_backups").doc(ALICE_UID).update({
      studentName: "Ada Updated",
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("5. Query alignment & UNLISTED code-only protection: unfiltered or UNLISTED list fails, CAMPUS list succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("campus_room_1").set(validRoomPayload(ALICE_UID, "CAMPUS", ts))
  );
  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("room_AUR7K2").set(validRoomPayload(ALICE_UID, "UNLISTED", ts))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("study_rooms").get());
  await assertFails(
    bobDb.collection("study_rooms").where("visibility", "==", "UNLISTED").get()
  );
  await assertSucceeds(
    bobDb.collection("study_rooms").where("visibility", "==", "CAMPUS").get()
  );
  await assertSucceeds(bobDb.collection("study_rooms").doc("room_AUR7K2").get());
});

test("6. Private solo room isolation: Bob cannot get Alice's PRIVATE room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("priv_room_1").set(validRoomPayload(ALICE_UID, "PRIVATE", ts))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("study_rooms").doc("priv_room_1").get());
});

test("7. Host Whiteboard Lock ('Lecture Mode') & Co-Host Privileges work securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("room_lecture").set(
      validRoomPayload(ALICE_UID, "UNLISTED", ts, {
        whiteboardLocked: true,
        coHostIds: [CHARLIE_UID],
      })
    )
  );

  // Regular peer Bob CANNOT edit locked whiteboard
  await assertFails(
    bobDb.collection("study_rooms").doc("room_lecture").update({
      sharedBoardNote: "Bob graffiti",
      updatedAt: new Date(Date.now() - 2500),
    })
  );

  // Promoted Co-Host Charlie CAN edit locked whiteboard & push a Live Pop-Quiz Poll!
  await assertSucceeds(
    charlieDb.collection("study_rooms").doc("room_lecture").update({
      sharedBoardNote: "Co-Host Formula: O(V + E) for BFS",
      activePollQuestion: "What is the time complexity of Dijkstra with a binary heap?",
      activePollOptions: ["O(V^2)", "O((V + E) log V)", "O(VE)", "O(log V)"],
      activePollCorrectIndex: 1,
      activePollRevealed: false,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Co-Host Charlie CANNOT escalate privileges by editing coHostIds or waitingRoomEnabled
  await assertFails(
    charlieDb.collection("study_rooms").doc("room_lecture").update({
      coHostIds: [CHARLIE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Host unlocks whiteboard -> now Bob CAN collaborate!
  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("room_lecture").update({
      whiteboardLocked: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("study_rooms").doc("room_lecture").update({
      sharedBoardNote: "Bob collaborative note added!",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("8. Cryptographic Waiting Room, iPhone Browser Badge, Live Poll Vote & Ban Tombstone", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("room_wait").set(
      validRoomPayload(ALICE_UID, "UNLISTED", ts, {
        waitingRoomEnabled: true,
      })
    )
  );

  // iPhone Safari guest Bob tries to bypass Waiting Room by claiming ADMITTED -> MUST FAIL!
  await assertFails(
    bobDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).set({
      roomId: "room_wait",
      roomOwnerId: ALICE_UID,
      roomVisibility: "UNLISTED",
      userId: BOB_UID,
      displayName: "Bob (iPhone Safari)",
      cityOrCampus: "Kumasi",
      studyStatus: "READY",
      handRaised: false,
      reactionEmoji: "👋",
      focusMinutesLogged: 0,
      admissionStatus: "ADMITTED",
      pollVoteIndex: -1,
      devicePlatform: "IPHONE_WEB",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob knocks properly with admissionStatus: "WAITING" -> SUCCEEDS!
  await assertSucceeds(
    bobDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).set({
      roomId: "room_wait",
      roomOwnerId: ALICE_UID,
      roomVisibility: "UNLISTED",
      userId: BOB_UID,
      displayName: "Bob (iPhone Safari)",
      cityOrCampus: "Kumasi",
      studyStatus: "READY",
      handRaised: false,
      reactionEmoji: "👋",
      focusMinutesLogged: 0,
      admissionStatus: "WAITING",
      pollVoteIndex: -1,
      devicePlatform: "IPHONE_WEB",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // While WAITING, Bob CANNOT post chat messages or self-admit!
  await assertFails(
    bobDb.collection("study_rooms").doc("room_wait").collection("messages").doc("msg_early").set({
      roomId: "room_wait",
      roomOwnerId: ALICE_UID,
      roomVisibility: "UNLISTED",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Let me bypass waiting room!",
      messageType: "CHAT",
      createdAt: new Date(Date.now() - 3500),
      updatedAt: new Date(Date.now() - 3500),
    })
  );
  await assertFails(
    bobDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).update({
      admissionStatus: "ADMITTED",
      updatedAt: new Date(Date.now() - 3200),
    })
  );

  // Host Alice admits Bob -> SUCCEEDS!
  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).update({
      admissionStatus: "ADMITTED",
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Now admitted, Bob votes in the live poll AND posts a Q&A message!
  await assertSucceeds(
    bobDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).update({
      pollVoteIndex: 1,
      updatedAt: new Date(Date.now() - 2500),
    })
  );
  await assertSucceeds(
    bobDb.collection("study_rooms").doc("room_wait").collection("messages").doc("msg_ok").set({
      roomId: "room_wait",
      roomOwnerId: ALICE_UID,
      roomVisibility: "UNLISTED",
      authorId: BOB_UID,
      authorName: "Bob (iPhone Safari)",
      text: "Joined from my iPhone on Chrome/Safari!",
      messageType: "CHAT",
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Host kicks Bob (admissionStatus = REMOVED) -> Bob cannot delete ban tombstone or post messages!
  await assertSucceeds(
    aliceDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).update({
      admissionStatus: "REMOVED",
      updatedAt: new Date(Date.now() - 1500),
    })
  );
  await assertFails(
    bobDb.collection("study_rooms").doc("room_wait").collection("participants").doc(BOB_UID).delete()
  );
  await assertFails(
    bobDb.collection("study_rooms").doc("room_wait").collection("messages").doc("msg_after_kick").set({
      roomId: "room_wait",
      roomOwnerId: ALICE_UID,
      roomVisibility: "UNLISTED",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Still talking",
      messageType: "CHAT",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
