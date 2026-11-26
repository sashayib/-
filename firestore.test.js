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

function validPlayerPayload(uid, name, playerCode = "SH4K9P2M", now = new Date(Date.now() - 1000)) {
  return {
    userId: uid,
    displayName: name,
    playerCode,
    presenceStatus: "online",
    lastSeenAt: now,
    visibility: "public",
    rating: 22,
    rankTier: "مبتدئ",
    wins: 0,
    losses: 0,
    matchesPlayed: 0,
    streak: 0,
    lossStreak: 0,
    totalPoints: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validRoomPayload(roomCode, hostUid, now = new Date(Date.now() - 1000)) {
  return {
    roomCode,
    hostId: hostUid,
    roomType: "public",
    status: "waiting",
    targetScore: 152,
    memberUids: [hostUid],
    seat0Uid: hostUid,
    seat1Uid: "",
    seat2Uid: "",
    seat3Uid: "",
    seat0Name: "أحمد",
    seat1Name: "",
    seat2Name: "",
    seat3Name: "",
    seat0Rating: 22,
    seat1Rating: 0,
    seat2Rating: 0,
    seat3Rating: 0,
    usScore: 0,
    themScore: 0,
    roundNumber: 0,
    turnIndex: 0,
    dealerIndex: 0,
    gamePhase: "lobby",
    gameMode: "none",
    trumpSuit: "none",
    bidderIndex: -1,
    bidLevel: 1,
    openCard: "",
    hands: { "0": [], "1": [], "2": [], "3": [] },
    tableCards: [],
    lastTrickCards: [],
    lastTrickWinner: -1,
    usTricksCount: 0,
    themTricksCount: 0,
    usRoundPoints: 0,
    themRoundPoints: 0,
    usProjects: [],
    themProjects: [],
    winnerTeam: -1,
    lastActionText: "تم إنشاء الجلسة",
    createdAt: now,
    updatedAt: now,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated access is denied across players, rooms, friends, invites, and matches", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("players").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("rooms").where("roomType", "==", "public").get());
  await assertFails(unauthDb.collection("invites").where("toUid", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(unauthDb.collection("players").doc(ALICE_UID).set(validPlayerPayload(ALICE_UID, "أحمد")));
});

test("Player profile create, Player ID lookup, presence heartbeat, bounded score update, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("players").doc(ALICE_UID).set(validPlayerPayload(ALICE_UID, "أحمد", "SH4K9P2M")));

  // Filtered leaderboard and Player ID lookup queries succeed
  await assertSucceeds(aliceDb.collection("players").where("visibility", "==", "public").get());
  await assertSucceeds(
    bobDb.collection("players").where("visibility", "==", "public").where("playerCode", "==", "SH4K9P2M").get()
  );
  // Unfiltered query must fail
  await assertFails(aliceDb.collection("players").get());

  // Presence heartbeat update succeeds
  await assertSucceeds(
    aliceDb.collection("players").doc(ALICE_UID).update({
      presenceStatus: "in_game",
      lastSeenAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Attempting to mutate existing playerCode fails
  await assertFails(
    aliceDb.collection("players").doc(ALICE_UID).update({
      playerCode: "SH999999",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid bounded match update
  await assertSucceeds(
    aliceDb.collection("players").doc(ALICE_UID).update({
      rating: 44,
      rankTier: "مبتدئ",
      wins: 1,
      losses: 0,
      matchesPlayed: 1,
      streak: 1,
      lossStreak: 0,
      totalPoints: 156,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject unbounded rating jump (+500)
  await assertFails(
    aliceDb.collection("players").doc(ALICE_UID).update({
      rating: 544,
      rankTier: "مبتدئ",
      wins: 2,
      losses: 0,
      matchesPlayed: 2,
      streak: 2,
      lossStreak: 0,
      totalPoints: 300,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Cross-user profile tamper denied
  await assertFails(bobDb.collection("players").doc(ALICE_UID).update({ displayName: "Hacked" }));
});

test("User-scoped friends list enforces ownership, self-friend denial, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice adds Bob as a friend
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).set({
      userId: ALICE_UID,
      friendUid: BOB_UID,
      friendPlayerCode: "SH7M3N8Q",
      friendName: "سارة",
      friendRating: 120,
      createdAt: now,
    })
  );

  // Alice cannot add herself as a friend
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(ALICE_UID).set({
      userId: ALICE_UID,
      friendUid: ALICE_UID,
      friendPlayerCode: "SH4K9P2M",
      friendName: "أحمد",
      friendRating: 22,
      createdAt: now,
    })
  );

  // Owner can list/get; another user cannot read or delete
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).delete());

  // Owner can remove friend
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).delete());
});

test("Table invites allow sender create, recipient accept/decline, and block unauthorized users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice invites Bob to a private room
  await assertSucceeds(
    aliceDb.collection("invites").doc("inv_1").set({
      fromUid: ALICE_UID,
      fromName: "أحمد",
      fromPlayerCode: "SH4K9P2M",
      toUid: BOB_UID,
      toName: "سارة",
      roomCode: "PRV482",
      targetScore: 152,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered queries for recipient and sender succeed; unfiltered fails
  await assertSucceeds(bobDb.collection("invites").where("toUid", "==", BOB_UID).get());
  await assertSucceeds(aliceDb.collection("invites").where("fromUid", "==", ALICE_UID).get());
  await assertFails(charlieDb.collection("invites").get());
  await assertFails(charlieDb.collection("invites").doc("inv_1").get());

  // Sender cannot accept their own invite; third party cannot update
  await assertFails(
    aliceDb.collection("invites").doc("inv_1").update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    charlieDb.collection("invites").doc("inv_1").update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Recipient can accept pending invite
  await assertSucceeds(
    bobDb.collection("invites").doc("inv_1").update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Cannot re-mutate once accepted
  await assertFails(
    bobDb.collection("invites").doc("inv_1").update({
      status: "declined",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Multiplayer room creation, join, play update, terminal state locking, and unfiltered list rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("rooms").doc("JMN482").set(validRoomPayload("JMN482", ALICE_UID)));

  // Public lobby query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("rooms").where("roomType", "==", "public").get());
  await assertFails(aliceDb.collection("rooms").get());

  // Bob joins waiting room
  await assertSucceeds(
    bobDb.collection("rooms").doc("JMN482").update({
      memberUids: [ALICE_UID, BOB_UID],
      seat1Uid: BOB_UID,
      seat1Name: "سارة",
      seat1Rating: 44,
      lastActionText: "انضم سارة",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice finishes match -> terminal state locked
  await assertSucceeds(
    aliceDb.collection("rooms").doc("JMN482").update({
      status: "finished",
      gamePhase: "match_finished",
      usScore: 154,
      winnerTeam: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("rooms").doc("JMN482").update({
      usScore: 200,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("User-scoped match history enforces ownership and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("m_1").set({
      userId: ALICE_UID,
      roomCode: "JMN482",
      mode: "online",
      usScore: 154,
      themScore: 92,
      didWin: true,
      ratingDelta: 22,
      opponentSummary: "سارة • خالد",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("matches").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("matches").get());
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("m_1").delete());
});
