package com.example.data

import android.content.Context
import com.example.R
import com.example.game.BalootEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class JammanaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً.")
    }

    suspend fun ensurePlayerProfile(
        userId: String,
        defaultName: String,
        avatarUrl: String?,
    ): Result<PlayerProfile> {
        val docPath = "players/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("players").document(uid)
            val existingSnap = docRef.get().await()
            val expectedCode = PlayerIdGenerator.forUid(uid)
            if (existingSnap.exists()) {
                var existing = existingSnap.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(userId = uid, displayName = defaultName, playerCode = expectedCode)
                if (existing.matchesPlayed == 0L && existing.rating == 1200L) {
                    val resetTier = BalootEngine.rankTierForRating(22)
                    runCatching {
                        docRef.update(
                            mapOf(
                                "rating" to 22L,
                                "rankTier" to resetTier,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        ).await()
                    }
                    existing = existing.copy(rating = 22L, rankTier = resetTier)
                }
                val presenceUpdates = mutableMapOf<String, Any>(
                    "presenceStatus" to "online",
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (existing.playerCode.isBlank()) {
                    presenceUpdates["playerCode"] = expectedCode
                    existing = existing.copy(playerCode = expectedCode)
                }
                runCatching { docRef.update(presenceUpdates).await() }
                return Result.success(existing.copy(presenceStatus = "online"))
            }
            val safeName = defaultName.trim().ifBlank { "لاعب بلوت" }.take(40)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "avatarUrl" to avatarUrl?.take(500),
                "playerCode" to expectedCode,
                "presenceStatus" to "online",
                "lastSeenAt" to FieldValue.serverTimestamp(),
                "visibility" to "public",
                "rating" to 22L,
                "rankTier" to BalootEngine.rankTierForRating(22),
                "wins" to 0L,
                "losses" to 0L,
                "matchesPlayed" to 0L,
                "streak" to 0L,
                "lossStreak" to 0L,
                "totalPoints" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            val created = PlayerProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                avatarUrl = avatarUrl,
                playerCode = expectedCode,
                presenceStatus = "online",
                visibility = "public",
                rating = 22L,
                rankTier = BalootEngine.rankTierForRating(22),
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observePlayerProfile(userId: String): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("players").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<PlayerProfile>> = flow {
        val uid = requireUserId()
        val path = "players"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PlayerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.rating }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateDisplayName(userId: String, newName: String): Result<Unit> {
        val docPath = "players/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val clean = newName.trim()
            require(clean.length in 1..40) { "الاسم يجب أن يكون بين 1 و 40 حرفاً" }
            db.collection("players").document(uid).update(
                mapOf(
                    "displayName" to clean,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        userId: String,
        currentProfile: PlayerProfile,
        didWin: Boolean,
        usScore: Int,
        themScore: Int,
        opponentSummary: String,
        mode: String,
        roomCode: String?,
        ratingDelta: Int,
    ): Result<Int> {
        val playerPath = "players/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val boundedDelta = ratingDelta.coerceIn(-500, 80)
            val newRating = (currentProfile.rating.toInt() + boundedDelta).coerceIn(0, 100000)
            val newTier = BalootEngine.rankTierForRating(newRating)
            val newWins = currentProfile.wins + if (didWin) 1L else 0L
            val newLosses = currentProfile.losses + if (!didWin) 1L else 0L
            val newMatches = currentProfile.matchesPlayed + 1L
            val newStreak = if (didWin) currentProfile.streak + 1L else 0L
            val newLossStreak = if (didWin) 0L else currentProfile.lossStreak + 1L
            val newTotalPoints = currentProfile.totalPoints + usScore.coerceAtLeast(0).toLong()

            db.collection("players").document(uid).update(
                mapOf(
                    "rating" to newRating.toLong(),
                    "rankTier" to newTier,
                    "wins" to newWins,
                    "losses" to newLosses,
                    "matchesPlayed" to newMatches,
                    "streak" to newStreak,
                    "lossStreak" to newLossStreak,
                    "totalPoints" to newTotalPoints,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val matchRef = db.collection("users").document(uid).collection("matches").document()
            val matchPayload = mapOf(
                "userId" to uid,
                "roomCode" to roomCode?.take(16),
                "mode" to if (mode == "online") "online" else "solo",
                "usScore" to usScore.coerceIn(0, 500).toLong(),
                "themScore" to themScore.coerceIn(0, 500).toLong(),
                "didWin" to didWin,
                "ratingDelta" to boundedDelta.toLong(),
                "opponentSummary" to opponentSummary.take(120),
                "createdAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null } as Map<String, Any>
            matchRef.set(matchPayload).await()

            Result.success(boundedDelta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, playerPath)
            Result.failure(e)
        }
    }

    fun observeMatchHistory(userId: String): Flow<List<MatchRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/matches"
        emitAll(
            db.collection("users").document(uid).collection("matches")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicRooms(): Flow<List<GameRoom>> = flow {
        val uid = requireUserId()
        val path = "rooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("roomType", "public")
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        GameRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.status == "waiting" }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyRooms(userId: String): Flow<List<GameRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "rooms"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .limit(20)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        GameRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.status != "finished" }
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRoom(roomCode: String): Flow<GameRoom?> = flow {
        val uid = requireUserId()
        val cleanCode = roomCode.trim().uppercase()
        val docRef = db.collection("rooms").document(cleanCode)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            GameRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createRoom(
        hostName: String,
        hostRating: Int,
        roomType: String,
        targetScore: Int,
    ): Result<String> {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val docPath = "rooms/$code"
        return try {
            val uid = requireUserId()
            val safeName = hostName.trim().ifBlank { "المضيف" }.take(40)
            val validTarget = 152L
            val validType = if (roomType == "private") "private" else "public"
            val payload = mapOf(
                "roomCode" to code,
                "hostId" to uid,
                "roomType" to validType,
                "status" to "waiting",
                "targetScore" to validTarget,
                "memberUids" to listOf(uid),
                "seat0Uid" to uid,
                "seat1Uid" to "",
                "seat2Uid" to "",
                "seat3Uid" to "",
                "seat0Name" to safeName,
                "seat1Name" to "",
                "seat2Name" to "",
                "seat3Name" to "",
                "seat0Rating" to hostRating.coerceIn(0, 100000).toLong(),
                "seat1Rating" to 0L,
                "seat2Rating" to 0L,
                "seat3Rating" to 0L,
                "usScore" to 0L,
                "themScore" to 0L,
                "roundNumber" to 0L,
                "turnIndex" to 0L,
                "dealerIndex" to 0L,
                "gamePhase" to "lobby",
                "gameMode" to "none",
                "trumpSuit" to "none",
                "bidderIndex" to -1L,
                "bidLevel" to 1L,
                "openCard" to "",
                "hands" to mapOf(
                    "0" to emptyList<String>(),
                    "1" to emptyList<String>(),
                    "2" to emptyList<String>(),
                    "3" to emptyList<String>(),
                ),
                "tableCards" to emptyList<String>(),
                "lastTrickCards" to emptyList<String>(),
                "lastTrickWinner" to -1L,
                "usTricksCount" to 0L,
                "themTricksCount" to 0L,
                "usRoundPoints" to 0L,
                "themRoundPoints" to 0L,
                "usProjects" to emptyList<String>(),
                "themProjects" to emptyList<String>(),
                "winnerTeam" to -1L,
                "lastActionText" to "أنشأ $safeName جلسة جديدة",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("rooms").document(code).set(payload).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(
        roomCode: String,
        playerName: String,
        playerRating: Int,
    ): Result<String> {
        val code = roomCode.trim().uppercase()
        val docPath = "rooms/$code"
        return try {
            val uid = requireUserId()
            require(code.matches(Regex("^[A-Z0-9]{6}$"))) { "رمز الجلسة يجب أن يكون 6 خانات" }
            val docRef = db.collection("rooms").document(code)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("لم يتم العثور على جلسة بهذا الرمز"))
            }
            val room = snap.toObject(
                GameRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("تعذّر قراءة بيانات الجلسة"))

            if (uid in room.memberUids) {
                return Result.success(code)
            }
            if (room.status != "waiting") {
                return Result.failure(IllegalStateException("الجلسة بدأت بالفعل"))
            }
            val emptySeat = (0..3).firstOrNull { room.seatUid(it).isBlank() }
                ?: return Result.failure(IllegalStateException("الجلسة مكتملة (4 لاعبين)"))

            val safeName = playerName.trim().ifBlank { "لاعب" }.take(40)
            val updatedMembers = room.memberUids + uid
            val updates = mutableMapOf<String, Any>(
                "memberUids" to updatedMembers,
                "seat${emptySeat}Uid" to uid,
                "seat${emptySeat}Name" to safeName,
                "seat${emptySeat}Rating" to playerRating.coerceIn(0, 100000).toLong(),
                "lastActionText" to "انضم $safeName إلى الطاولة",
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(updates).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveWaitingRoom(room: GameRoom): Result<Unit> {
        val docPath = "rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("rooms").document(room.roomCode)
            if (room.hostId == uid && room.status == "waiting") {
                docRef.delete().await()
                return Result.success(Unit)
            }
            val seatIdx = room.mySeatIndex(uid)
            val updatedMembers = room.memberUids.filterNot { it == uid }
            if (updatedMembers.isEmpty()) {
                if (room.hostId == uid) docRef.delete().await()
                return Result.success(Unit)
            }
            val updates = mutableMapOf<String, Any>(
                "memberUids" to updatedMembers,
                "lastActionText" to "غادر أحد اللاعبين الطاولة",
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (seatIdx in 0..3) {
                updates["seat${seatIdx}Uid"] = ""
                updates["seat${seatIdx}Name"] = ""
                updates["seat${seatIdx}Rating"] = 0L
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRoomState(roomCode: String, updates: Map<String, Any>): Result<Unit> {
        val docPath = "rooms/$roomCode"
        return try {
            requireUserId()
            val payload = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("rooms").document(roomCode).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeRoomMessages(roomCode: String): Flow<List<TableChatMessage>> = flow {
        val uid = requireUserId()
        val cleanCode = roomCode.trim().uppercase()
        val colRef = db.collection("rooms").document(cleanCode).collection("messages")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        TableChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendRoomMessage(
        roomCode: String,
        senderName: String,
        seatIndex: Int,
        text: String,
    ): Result<Unit> {
        val cleanCode = roomCode.trim().uppercase()
        val docPath = "rooms/$cleanCode/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(200)
            require(cleanText.isNotEmpty()) { "الرسالة فارغة" }
            val safeName = senderName.trim().ifBlank { "لاعب شدها" }.take(40)
            val payload = mapOf(
                "senderUid" to uid,
                "senderName" to safeName,
                "seatIndex" to seatIndex.coerceIn(0, 3),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            db.collection("rooms").document(cleanCode).collection("messages")
                .add(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- Presence, Friends List & Direct Table Invites ---

    suspend fun updatePresence(userId: String, status: String): Result<Unit> {
        val docPath = "players/$userId"
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            if (uid != userId) return Result.success(Unit)
            val validStatus = when (status) {
                "online", "in_game", "offline" -> status
                else -> "online"
            }
            db.collection("players").document(uid).update(
                mapOf(
                    "presenceStatus" to validStatus,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFriendsWithPresence(userId: String): Flow<List<FriendWithPresence>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val friendsPath = "users/$uid/friends"
        val friendsFlow: Flow<List<FriendEntry>> = db.collection("users")
            .document(uid)
            .collection("friends")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    FriendEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.LIST, friendsPath)
                }
                throw error
            }

        emitAll(
            friendsFlow.flatMapLatest { entries ->
                if (entries.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val perFriendFlows: List<Flow<FriendWithPresence>> = entries.map { entry ->
                        val fallback = FriendWithPresence(
                            friendUid = entry.friendUid,
                            playerCode = entry.friendPlayerCode.ifBlank { PlayerIdGenerator.forUid(entry.friendUid) },
                            displayName = entry.friendName.ifBlank { "لاعب شدها" },
                            avatarUrl = null,
                            rating = entry.friendRating.toInt(),
                            rankTier = BalootEngine.rankTierForRating(entry.friendRating.toInt()),
                            presenceStatus = "offline",
                            lastSeenAt = null,
                        )
                        db.collection("players")
                            .document(entry.friendUid)
                            .snapshots()
                            .map { snap ->
                                val live = if (snap.exists()) {
                                    snap.toObject(
                                        PlayerProfile::class.java,
                                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                                    )
                                } else {
                                    null
                                }
                                if (live != null) {
                                    FriendWithPresence(
                                        friendUid = entry.friendUid,
                                        playerCode = live.effectivePlayerCode(),
                                        displayName = live.displayName.ifBlank { fallback.displayName },
                                        avatarUrl = live.avatarUrl,
                                        rating = live.rating.toInt(),
                                        rankTier = live.rankTier.ifBlank { BalootEngine.rankTierForRating(live.rating.toInt()) },
                                        presenceStatus = live.presenceStatus,
                                        lastSeenAt = live.lastSeenAt,
                                    )
                                } else {
                                    fallback
                                }
                            }
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                emit(fallback)
                            }
                    }
                    combine(perFriendFlows) { array ->
                        val now = System.currentTimeMillis()
                        array.toList().sortedWith(
                            compareByDescending<FriendWithPresence> { it.isOnlineNow(now) }
                                .thenByDescending { it.effectivePresence(now) == "online" }
                                .thenByDescending { it.rating }
                        )
                    }
                }
            }
        )
    }

    suspend fun addFriendByPlayerId(userId: String, rawPlayerId: String): Result<PlayerProfile> {
        val docPath = "users/$userId/friends"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val trimmed = rawPlayerId.trim()
            val normalizedCode = PlayerIdGenerator.normalizeInput(trimmed)
            if (normalizedCode.length < 6) {
                return Result.failure(IllegalArgumentException("أدخل معرّف لاعب صحيح (مثل SH4K9P2M)"))
            }

            // 1. Search by playerCode among public profiles
            val byCodeSnap = db.collection("players")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("playerCode", normalizedCode)
                .limit(1)
                .get()
                .await()

            var target: PlayerProfile? = byCodeSnap.documents.firstOrNull()?.toObject(
                PlayerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )

            // 2. Fallback: exact UID document lookup if user pasted Firebase UID
            if (target == null && trimmed.matches(Regex("^[a-zA-Z0-9_\\-]{6,128}$"))) {
                val directSnap = db.collection("players").document(trimmed).get().await()
                if (directSnap.exists()) {
                    target = directSnap.toObject(
                        PlayerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
            }

            // 3. Fallback: match deterministic Player ID for legacy accounts not yet backfilled
            if (target == null) {
                val publicSnap = db.collection("players")
                    .whereEqualTo("visibility", "public")
                    .limit(100)
                    .get()
                    .await()
                target = publicSnap.toObjects(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).firstOrNull { it.effectivePlayerCode().equals(normalizedCode, ignoreCase = true) }
            }

            val found = target
                ?: return Result.failure(IllegalStateException("لم يتم العثور على لاعب بهذا المعرّف ($normalizedCode)"))

            addFriendProfile(uid, found)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addFriendProfile(userId: String, target: PlayerProfile): Result<PlayerProfile> {
        val targetUid = target.userId.ifBlank { target.id }
        val docPath = "users/$userId/friends/$targetUid"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            if (targetUid.isBlank()) {
                return Result.failure(IllegalStateException("بيانات اللاعب غير مكتملة"))
            }
            if (targetUid == uid) {
                return Result.failure(IllegalArgumentException("لا يمكنك إضافة نفسك إلى قائمة الأصدقاء"))
            }
            val friendDocRef = db.collection("users")
                .document(uid)
                .collection("friends")
                .document(targetUid)
            val existing = friendDocRef.get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("${target.displayName} موجود بالفعل في قائمة أصدقائك"))
            }
            val safeName = target.displayName.trim().ifBlank { "لاعب شدها" }.take(40)
            val safeCode = target.effectivePlayerCode().take(16)
            val payload = mapOf(
                "userId" to uid,
                "friendUid" to targetUid,
                "friendPlayerCode" to safeCode,
                "friendName" to safeName,
                "friendRating" to target.rating.coerceIn(0L, 100000L),
                "createdAt" to FieldValue.serverTimestamp(),
            )
            friendDocRef.set(payload).await()
            Result.success(target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFriend(userId: String, friendUid: String): Result<Unit> {
        val docPath = "users/$userId/friends/$friendUid"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("users")
                .document(uid)
                .collection("friends")
                .document(friendUid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeIncomingInvites(userId: String): Flow<List<TableInvite>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "invites"
        emitAll(
            db.collection(path)
                .whereEqualTo("toUid", uid)
                .limit(25)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        TableInvite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).filter { it.status == "pending" }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOutgoingInvites(userId: String): Flow<List<TableInvite>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "invites"
        emitAll(
            db.collection(path)
                .whereEqualTo("fromUid", uid)
                .limit(25)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        TableInvite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).filter { it.status == "pending" }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendTableInvite(
        fromName: String,
        fromPlayerCode: String,
        toUid: String,
        toName: String,
        roomCode: String,
        targetScore: Int,
    ): Result<Unit> {
        val path = "invites"
        return try {
            val uid = requireUserId()
            require(toUid.isNotBlank() && toUid != uid) { "لا يمكنك دعوة نفسك" }
            val cleanCode = roomCode.trim().uppercase()
            require(cleanCode.matches(Regex("^[A-Z0-9]{6}$"))) { "رمز الجلسة غير صالح" }
            val validTarget = 152L
            val payload = mapOf(
                "fromUid" to uid,
                "fromName" to fromName.trim().ifBlank { "لاعب شدها" }.take(40),
                "fromPlayerCode" to fromPlayerCode.trim().ifBlank { PlayerIdGenerator.forUid(uid) }.take(16),
                "toUid" to toUid,
                "toName" to toName.trim().ifBlank { "صديق" }.take(40),
                "roomCode" to cleanCode,
                "targetScore" to validTarget,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection(path).add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToInvite(inviteId: String, accept: Boolean): Result<Unit> {
        val docPath = "invites/$inviteId"
        return try {
            requireUserId()
            val nextStatus = if (accept) "accepted" else "declined"
            db.collection("invites").document(inviteId).update(
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
