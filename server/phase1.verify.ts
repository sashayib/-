import assert from 'node:assert/strict';
import type { AddressInfo } from 'node:net';
import { WebSocket } from 'ws';
import * as sharedEngine from '../shared/balootEngine.ts';
import * as webEngine from '../src/game/balootEngine';
import { parseClientMessage, PROTOCOL_VERSION } from '../shared/protocol.ts';
import type { PublicRoomState } from '../shared/balootTypes.ts';
import { buildProjectedRoomDocuments } from './roomStore.ts';
import { createMultiplayerApp } from '../server.ts';

function makeDeterministicRng(seed = 42): () => number {
  let state = seed >>> 0;
  return () => {
    state = (Math.imul(1664525, state) + 1013904223) >>> 0;
    return state / 0x100000000;
  };
}

async function testBalootEngineParity(): Promise<void> {
  assert.equal(sharedEngine.fullDeck().length, 32, 'Deck must have 32 Baloot cards');
  assert.deepEqual(sharedEngine.fullDeck(), webEngine.fullDeck(), 'Shared deck must match web deck');

  const testCards = [
    { rank: 'J' as const, suit: 'spades' as const },
    { rank: '9' as const, suit: 'spades' as const },
    { rank: 'A' as const, suit: 'hearts' as const },
    { rank: '10' as const, suit: 'diamonds' as const },
  ];

  for (const c of testCards) {
    assert.equal(
      sharedEngine.cardStrength(c, 'hokm', 'spades'),
      webEngine.cardStrength(c, 'hokm', 'spades'),
      'Hokm card strength parity',
    );
    assert.equal(
      sharedEngine.cardPoints(c, 'hokm', 'spades'),
      webEngine.cardPoints(c, 'hokm', 'spades'),
      'Hokm card points parity',
    );
    assert.equal(
      sharedEngine.cardStrength(c, 'sun', null),
      webEngine.cardStrength(c, 'sun', null),
      'Sun card strength parity',
    );
    assert.equal(
      sharedEngine.cardPoints(c, 'sun', null),
      webEngine.cardPoints(c, 'sun', null),
      'Sun card points parity',
    );
  }

  const seats = [0, 1, 2, 3].map((idx) => ({
    seatIndex: idx,
    uid: `uid_${idx}`,
    name: `لاعب ${idx + 1}`,
    rating: 100,
    isBot: false,
  }));

  const dealt = sharedEngine.dealNewRoundWithDeck(
    {
      dealerIndex: 0,
      roundNumber: 1,
      seats,
      existingUsScore: 0,
      existingThemScore: 0,
      roomCode: 'SHD123',
      isOnline: true,
      hostId: 'uid_0',
      mySeatIndex: 0,
    },
    makeDeterministicRng(12345),
  );

  // Verify 5 cards x 4 seats + 1 openCard + 11 undealt = 32 unique cards
  const allCodes = new Set<string>();
  for (let s = 0; s < 4; s++) {
    assert.equal(dealt.state.hands[s].length, 5);
    dealt.state.hands[s].forEach((c) => allCodes.add(sharedEngine.cardCode(c)));
  }
  assert.ok(dealt.state.openCard);
  allCodes.add(sharedEngine.cardCode(dealt.state.openCard));
  assert.equal(dealt.undealtDeck.length, 11);
  dealt.undealtDeck.forEach((c) => allCodes.add(sharedEngine.cardCode(c)));
  assert.equal(allCodes.size, 32, 'All 32 dealt + open + sealed undealt cards must be unique');

  // Verify completing deal after Sun bid produces 8 cards per seat with zero duplicates
  const bidRes = sharedEngine.applyBidDecision(
    dealt.state,
    dealt.state.turnIndex,
    'sun',
    null,
    dealt.undealtDeck,
    makeDeterministicRng(99),
  );
  const fullRoundCodes = new Set<string>();
  for (let s = 0; s < 4; s++) {
    assert.equal(bidRes.nextState.hands[s].length, 8);
    bidRes.nextState.hands[s].forEach((c) => fullRoundCodes.add(sharedEngine.cardCode(c)));
  }
  assert.equal(fullRoundCodes.size, 32, 'All 4 players must hold 32 unique cards after 2nd deal');
  assert.equal(bidRes.nextState.targetScore, 152);
  console.log('PASS: shared/balootEngine.ts rules parity & 32-card sealed deck integrity');
}

function testSecretHandIsolation(): void {
  const seats = [0, 1, 2, 3].map((idx) => ({
    seatIndex: idx,
    uid: `uid_${idx}`,
    name: `لاعب ${idx + 1}`,
    rating: 22,
    isBot: false,
  }));

  const dealt = sharedEngine.dealNewRoundWithDeck(
    {
      dealerIndex: 0,
      roundNumber: 1,
      seats,
      existingUsScore: 0,
      existingThemScore: 0,
      roomCode: 'AB12CD',
      isOnline: true,
      hostId: 'uid_0',
      mySeatIndex: 0,
    },
    makeDeterministicRng(777),
  );

  const basePublic: PublicRoomState = {
    roomCode: 'AB12CD',
    matchId: 'match-1',
    hostId: 'uid_0',
    roomType: 'private',
    status: 'waiting',
    targetScore: 152,
    maxPlayers: 4,
    fillWithBots: false,
    memberUids: ['uid_0', 'uid_1', 'uid_2', 'uid_3'],
    seats,
    handCounts: { '0': 0, '1': 0, '2': 0, '3': 0 },
    usScore: 0,
    themScore: 0,
    roundNumber: 0,
    turnIndex: 0,
    dealerIndex: 0,
    phase: 'lobby',
    mode: 'none',
    trumpSuit: null,
    bidderIndex: -1,
    bidLevel: 1,
    openCard: '',
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
    lastActionText: '',
    biddingPasses: 0,
    highestBidSeat: -1,
    highestBidMode: 'none',
    highestBidSuit: null,
    standbySeats: [],
    declaredProjectSeats: [],
    moveSeq: 0,
    turnDeadlineAtMs: null,
    resultsCommitted: false,
    createdAtMs: Date.now(),
    updatedAtMs: Date.now(),
  };

  const projected = buildProjectedRoomDocuments({
    existingRoom: basePublic,
    tableState: dealt.state,
    undealtDeck: dealt.undealtDeck,
    nextMoveSeq: 1,
    processedActionIds: ['act_12345678'],
    nowMs: Date.now(),
  });

  // Public room must NEVER contain `hands` or `undealtDeck`
  assert.equal('hands' in projected.publicRoom, false, 'Public room must never expose hands');
  assert.equal('undealtDeck' in projected.publicRoom, false, 'Public room must never expose deck');
  assert.deepEqual(projected.publicRoom.handCounts, { '0': 5, '1': 5, '2': 5, '3': 5 });
  assert.ok(projected.publicRoom.turnDeadlineAtMs && projected.publicRoom.turnDeadlineAtMs > Date.now());

  // Each private hand document contains ONLY that player's 5 cards
  assert.equal(Object.keys(projected.privateHands).length, 4);
  for (let s = 0; s < 4; s++) {
    const uid = `uid_${s}`;
    assert.equal(projected.privateHands[uid].uid, uid);
    assert.equal(projected.privateHands[uid].cards.length, 5);
  }
  assert.equal(projected.serverSecret.undealtDeck.length, 11);
  console.log('PASS: Secret player hand isolation & persistent turn deadline projection');
}

function testProtocolValidation(): void {
  const validPlay = parseClientMessage(
    JSON.stringify({
      type: 'game:play_card',
      actionId: 'act_0001_play',
      roomCode: 'ab12cd',
      expectedMoveSeq: 4,
      cardCode: 'as',
    }),
  );
  assert.equal(validPlay.ok, true);
  if (validPlay.ok && validPlay.msg.type === 'game:play_card') {
    assert.equal(validPlay.msg.roomCode, 'AB12CD');
    assert.equal(validPlay.msg.cardCode, 'AS');
  }

  const badCard = parseClientMessage(
    JSON.stringify({
      type: 'game:play_card',
      actionId: 'act_0001_play',
      roomCode: 'AB12CD',
      expectedMoveSeq: 4,
      cardCode: '2Z',
    }),
  );
  assert.equal(badCard.ok, false, 'Invalid Baloot card code must be rejected');

  const badRoom = parseClientMessage(
    JSON.stringify({ type: 'room:join', actionId: 'act_12345678', roomCode: 'INVALID!' }),
  );
  assert.equal(badRoom.ok, false, 'Invalid room code must be rejected');

  const oversized = parseClientMessage('x'.repeat(20_000));
  assert.equal(oversized.ok, false, 'Oversized payload must be rejected');
  console.log('PASS: Protocol runtime schema validation');
}

async function testExpressAndWebSocketAuthGuards(): Promise<void> {
  const { httpServer, wsHub } = createMultiplayerApp();
  await new Promise<void>((resolve) => httpServer.listen(0, '127.0.0.1', () => resolve()));
  const { port } = httpServer.address() as AddressInfo;
  const baseUrl = `http://127.0.0.1:${port}`;

  try {
    // 1. Health check returns 200 and protocol version
    const healthRes = await fetch(`${baseUrl}/api/health`);
    assert.equal(healthRes.status, 200);
    const healthJson = (await healthRes.json()) as Record<string, unknown>;
    assert.equal(healthJson.ok, true);
    assert.equal(healthJson.protocolVersion, PROTOCOL_VERSION);

    // 2. Unauthenticated REST endpoints strictly return 401
    const sessRes = await fetch(`${baseUrl}/api/multiplayer/session`);
    assert.equal(sessRes.status, 401);

    const createRes = await fetch(`${baseUrl}/api/multiplayer/rooms`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomType: 'private', actionId: 'act_12345678' }),
    });
    assert.equal(createRes.status, 401);

    // 3. WebSocket rejects commands prior to Firebase ID Token auth
    const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
    const firstReply = await new Promise<Record<string, unknown>>((resolve, reject) => {
      ws.on('open', () => {
        ws.send(JSON.stringify({ type: 'room:create', actionId: 'act_12345678', roomType: 'private' }));
      });
      ws.on('message', (data) => {
        resolve(JSON.parse(data.toString('utf-8')) as Record<string, unknown>);
      });
      ws.on('error', reject);
    });
    assert.equal(firstReply.type, 'auth:error');
    assert.equal(firstReply.code, 'UNAUTHENTICATED');
    ws.close();

    console.log('PASS: Express /api/health, REST 401 auth guard, and WebSocket /ws auth handshake guard');
  } finally {
    await wsHub.close();
    await new Promise<void>((resolve) => httpServer.close(() => resolve()));
  }
}

async function main() {
  await testBalootEngineParity();
  testSecretHandIsolation();
  testProtocolValidation();
  await testExpressAndWebSocketAuthGuards();
  console.log('\nALL PHASE 1 VERIFICATION CHECKS PASSED!');
}

main().catch((err) => {
  console.error('PHASE 1 VERIFICATION FAILED:', err);
  process.exit(1);
});
