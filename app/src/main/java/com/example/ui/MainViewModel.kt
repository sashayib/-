package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCoachUiState
import com.example.ai.BalootAiAdvisor
import com.example.data.FriendWithPresence
import com.example.data.GameRoom
import com.example.data.JammanaRepository
import com.example.data.MatchRecord
import com.example.data.PlayerIdGenerator
import com.example.data.PlayerProfile
import com.example.data.TableChatMessage
import com.example.data.TableInvite
import com.example.data.UiState
import com.example.game.BalootCard
import com.example.game.BalootEngine
import com.example.game.BalootSeat
import com.example.game.BalootTableState
import com.example.game.DetectedProject
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.game.PlayedCardEntry
import com.example.game.Suit
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class SeatBubble(
    val seatIndex: Int,
    val message: String,
    val timestampMs: Long,
)

data class TableVoiceCue(
    val id: Long,
    val seatIndex: Int,
    val phrase: String,
    val queueAdd: Boolean = false,
)

class MainViewModel(
    private val repository: JammanaRepository,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val initialAvatarUrl: String?,
) : ViewModel() {

    val playerProfileState: StateFlow<UiState<PlayerProfile>> =
        repository.observePlayerProfile(currentUserId)
            .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
                if (profile != null) {
                    UiState.Success(profile)
                } else {
                    UiState.Loading
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing player profile", root)
                emit(UiState.Error(root.message ?: "تعذّر تحميل بيانات اللاعب"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val leaderboardState: StateFlow<UiState<List<PlayerProfile>>> =
        repository.observeLeaderboard()
            .map<List<PlayerProfile>, UiState<List<PlayerProfile>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing leaderboard", root)
                emit(UiState.Error(root.message ?: "تعذّر تحميل قائمة التصنيف"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val matchHistoryState: StateFlow<UiState<List<MatchRecord>>> =
        repository.observeMatchHistory(currentUserId)
            .map<List<MatchRecord>, UiState<List<MatchRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing match history", root)
                emit(UiState.Error(root.message ?: "تعذّر تحميل سجل المباريات"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val publicRoomsState: StateFlow<UiState<List<GameRoom>>> =
        repository.observePublicRooms()
            .map<List<GameRoom>, UiState<List<GameRoom>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing public rooms", root)
                emit(UiState.Error(root.message ?: "تعذّر تحميل الجلسات المتاحة"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val myRoomsState: StateFlow<UiState<List<GameRoom>>> =
        repository.observeMyRooms(currentUserId)
            .map<List<GameRoom>, UiState<List<GameRoom>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing my rooms", root)
                emit(UiState.Error(root.message ?: "تعذّر تحميل جلساتك"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val friendsState: StateFlow<UiState<List<FriendWithPresence>>> =
        repository.observeFriendsWithPresence(currentUserId)
            .map<List<FriendWithPresence>, UiState<List<FriendWithPresence>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing friends list", root)
                emit(UiState.Error(root.message ?: "تعذّر تحميل قائمة الأصدقاء"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val incomingInvitesState: StateFlow<List<TableInvite>> =
        repository.observeIncomingInvites(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing incoming table invites", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )

    val outgoingInvitesState: StateFlow<List<TableInvite>> =
        repository.observeOutgoingInvites(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing outgoing table invites", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )

    private val _activeRoom = MutableStateFlow<GameRoom?>(null)
    val activeRoom: StateFlow<GameRoom?> = _activeRoom.asStateFlow()

    private val _tableState = MutableStateFlow<BalootTableState?>(null)
    val tableState: StateFlow<BalootTableState?> = _tableState.asStateFlow()

    private val _lastEloDelta = MutableStateFlow<Int?>(null)
    val lastEloDelta: StateFlow<Int?> = _lastEloDelta.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _seatBubbles = MutableStateFlow<Map<Int, SeatBubble>>(emptyMap())
    val seatBubbles: StateFlow<Map<Int, SeatBubble>> = _seatBubbles.asStateFlow()

    private val _voiceCues = MutableSharedFlow<TableVoiceCue>(extraBufferCapacity = 16)
    val voiceCues: SharedFlow<TableVoiceCue> = _voiceCues.asSharedFlow()

    private val _isVoiceMuted = MutableStateFlow(false)
    val isVoiceMuted: StateFlow<Boolean> = _isVoiceMuted.asStateFlow()

    private val _tableChatMessages = MutableStateFlow<List<TableChatMessage>>(emptyList())
    val tableChatMessages: StateFlow<List<TableChatMessage>> = _tableChatMessages.asStateFlow()

    private val _turnRemainingSeconds = MutableStateFlow<Int?>(null)
    val turnRemainingSeconds: StateFlow<Int?> = _turnRemainingSeconds.asStateFlow()

    private val _isAutoPlayStandby = MutableStateFlow(false)
    val isAutoPlayStandby: StateFlow<Boolean> = _isAutoPlayStandby.asStateFlow()

    private val _pendingProjectsToDeclare = MutableStateFlow<List<DetectedProject>>(emptyList())
    val pendingProjectsToDeclare: StateFlow<List<DetectedProject>> = _pendingProjectsToDeclare.asStateFlow()

    private val aiAdvisor = BalootAiAdvisor()
    private val _aiCoachState = MutableStateFlow<AiCoachUiState>(AiCoachUiState.Idle)
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()
    private var aiJob: Job? = null

    private var roomObserverJob: Job? = null
    private var chatObserverJob: Job? = null
    private var botTurnJob: Job? = null
    private var turnTimerJob: Job? = null
    private var standbyAutoPlayJob: Job? = null
    private var lastTurnKey: String? = null
    private var standbyRoundNumber: Int? = null
    private var resolvedProjectsRoundKey: String? = null
    private var lastProcessedTable: BalootTableState? = null
    private var recordedSoloMatchId: String? = null
    private val recordedOnlineRooms = mutableSetOf<String>()
    private var currentSoloMatchKey: String = ""

    init {
        viewModelScope.launch {
            repository.ensurePlayerProfile(
                userId = currentUserId,
                defaultName = initialDisplayName,
                avatarUrl = initialAvatarUrl,
            )
        }
    }

    fun clearAiCoach() {
        aiJob?.cancel()
        _aiCoachState.value = AiCoachUiState.Idle
    }

    fun requestHandAdvice() {
        val table = _tableState.value ?: return
        runAiStream { aiAdvisor.streamHandAdvice(table) }
    }

    fun askRulesAdvisor(question: String) {
        val clean = question.trim()
        if (clean.isBlank()) return
        runAiStream { aiAdvisor.streamRulesQuestion(clean) }
    }

    private fun runAiStream(streamProvider: () -> kotlinx.coroutines.flow.Flow<String>) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiCoachState.value = AiCoachUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    streamProvider().collect { chunk ->
                        accumulated.append(chunk)
                        if (accumulated.isNotBlank()) {
                            _aiCoachState.value = AiCoachUiState.Streaming(accumulated.toString())
                        }
                    }
                }
                if (accumulated.isNotBlank()) {
                    _aiCoachState.value = AiCoachUiState.Success(accumulated.toString())
                } else {
                    _aiCoachState.value = AiCoachUiState.Error("لم يرجع المستشار الذكي نصاً. حاول مرة أخرى.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI coach timed out", e)
                _aiCoachState.value = AiCoachUiState.Error("انتهت مهلة الاتصال بالمستشار الذكي (60 ثانية). حاول مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _aiCoachState.value = AiCoachUiState.Error(
                    e.localizedMessage ?: "تعذّر الحصول على استشارة الذكاء الاصطناعي."
                )
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun clearActionSuccess() {
        _actionSuccess.value = null
    }

    fun updatePresence(status: String) {
        viewModelScope.launch {
            repository.updatePresence(currentUserId, status)
        }
    }

    suspend fun markOfflineBeforeSignOut() {
        repository.updatePresence(currentUserId, "offline")
    }

    fun addFriendByPlayerId(rawPlayerId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addFriendByPlayerId(currentUserId, rawPlayerId)
            _isBusy.value = false
            res.onSuccess { friend ->
                _actionSuccess.value = "تمت إضافة ${friend.displayName} إلى قائمة أصدقائك"
                onSuccess()
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّرت إضافة الصديق"
            }
        }
    }

    fun addFriendProfile(profile: PlayerProfile) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addFriendProfile(currentUserId, profile)
            _isBusy.value = false
            res.onSuccess { friend ->
                _actionSuccess.value = "تمت إضافة ${friend.displayName} إلى قائمة أصدقائك"
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّرت إضافة الصديق"
            }
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.removeFriend(currentUserId, friendUid)
            _isBusy.value = false
            res.onSuccess {
                _actionSuccess.value = "تمت إزالة الصديق من القائمة"
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّرت إزالة الصديق"
            }
        }
    }

    fun inviteFriendToPrivateTable(
        friend: FriendWithPresence,
        targetScore: Int = 152,
        onNewRoomCreated: () -> Unit = {},
        onRoomReady: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = (playerProfileState.value as? UiState.Success)?.data
            val myName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val myRating = profile?.rating?.toInt() ?: 22
            val myCode = profile?.effectivePlayerCode() ?: PlayerIdGenerator.forUid(currentUserId)

            val existingWaiting = _activeRoom.value?.takeIf { it.status == "waiting" }
            if (existingWaiting != null) {
                val inviteRes = repository.sendTableInvite(
                    fromName = myName,
                    fromPlayerCode = myCode,
                    toUid = friend.friendUid,
                    toName = friend.displayName,
                    roomCode = existingWaiting.roomCode,
                    targetScore = existingWaiting.targetScore.toInt(),
                )
                _isBusy.value = false
                inviteRes.onSuccess {
                    _actionSuccess.value = "تم إرسال الدعوة إلى ${friend.displayName} للطاولة ${existingWaiting.roomCode}"
                    onRoomReady()
                }.onFailure {
                    _actionError.value = it.localizedMessage ?: "تعذّر إرسال الدعوة"
                }
            } else {
                val roomRes = repository.createRoom(myName, myRating, "private", targetScore)
                roomRes.onSuccess { code ->
                    onNewRoomCreated()
                    observeOnlineRoom(code)
                    val inviteRes = repository.sendTableInvite(
                        fromName = myName,
                        fromPlayerCode = myCode,
                        toUid = friend.friendUid,
                        toName = friend.displayName,
                        roomCode = code,
                        targetScore = targetScore,
                    )
                    _isBusy.value = false
                    inviteRes.onSuccess {
                        _actionSuccess.value = "تم إنشاء طاولة خاصة وإرسال الدعوة إلى ${friend.displayName}"
                        onRoomReady()
                    }.onFailure {
                        _actionError.value = it.localizedMessage ?: "تم إنشاء الطاولة لكن تعذّر إرسال الدعوة"
                        onRoomReady()
                    }
                }.onFailure {
                    _isBusy.value = false
                    _actionError.value = it.localizedMessage ?: "تعذّر إنشاء الطاولة الخاصة"
                }
            }
        }
    }

    fun inviteFriendToActiveRoom(friend: FriendWithPresence) {
        val room = _activeRoom.value ?: return
        if (room.status != "waiting") return
        viewModelScope.launch {
            _isBusy.value = true
            val profile = (playerProfileState.value as? UiState.Success)?.data
            val myName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val myCode = profile?.effectivePlayerCode() ?: PlayerIdGenerator.forUid(currentUserId)
            val res = repository.sendTableInvite(
                fromName = myName,
                fromPlayerCode = myCode,
                toUid = friend.friendUid,
                toName = friend.displayName,
                roomCode = room.roomCode,
                targetScore = room.targetScore.toInt(),
            )
            _isBusy.value = false
            res.onSuccess {
                _actionSuccess.value = "تمت دعوة ${friend.displayName} إلى الطاولة"
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّر إرسال الدعوة"
            }
        }
    }

    fun acceptTableInvite(invite: TableInvite, onJoined: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = (playerProfileState.value as? UiState.Success)?.data
            val myName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val myRating = profile?.rating?.toInt() ?: 22
            val joinRes = repository.joinRoom(invite.roomCode, myName, myRating)
            _isBusy.value = false
            joinRes.onSuccess { code ->
                repository.respondToInvite(invite.id, accept = true)
                observeOnlineRoom(code)
                _actionSuccess.value = "تم الانضمام لطاولة ${invite.fromName}"
                onJoined()
            }.onFailure { err ->
                val message = err.localizedMessage ?: "تعذّر الانضمام للطاولة"
                if (message.contains("لم يتم العثور") || message.contains("بدأت بالفعل")) {
                    repository.respondToInvite(invite.id, accept = false)
                }
                _actionError.value = message
            }
        }
    }

    fun declineTableInvite(invite: TableInvite) {
        viewModelScope.launch {
            val res = repository.respondToInvite(invite.id, accept = false)
            res.onSuccess {
                _actionSuccess.value = "تم رفض الدعوة"
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّر تحديث الدعوة"
            }
        }
    }

    fun toggleVoiceMuted() {
        _isVoiceMuted.value = !_isVoiceMuted.value
    }

    fun sendQuickPhrase(phrase: String) {
        val table = _tableState.value ?: return
        showSeatBubble(table.mySeatIndex, phrase)
    }

    fun sendTableChatMessage(text: String) {
        val table = _tableState.value ?: return
        val clean = text.trim().take(200)
        if (clean.isEmpty()) return
        val mySeat = table.mySeatIndex.coerceIn(0, 3)
        val myName = table.seats.getOrNull(mySeat)?.name?.ifBlank { initialDisplayName } ?: initialDisplayName
        showSeatBubble(mySeat, clean)
        if (table.isOnline && !table.roomCode.isNullOrBlank()) {
            viewModelScope.launch {
                val res = repository.sendRoomMessage(
                    roomCode = table.roomCode,
                    senderName = myName,
                    seatIndex = mySeat,
                    text = clean,
                )
                res.onFailure {
                    _actionError.value = it.localizedMessage ?: "تعذّر إرسال الرسالة"
                }
            }
        } else {
            val localMsg = TableChatMessage(
                id = "local_${System.nanoTime()}",
                senderUid = currentUserId,
                senderName = myName,
                seatIndex = mySeat,
                text = clean,
            )
            _tableChatMessages.value = (_tableChatMessages.value + localMsg).takeLast(60)
        }
    }

    private fun showSeatBubble(seatIndex: Int, phrase: String) {
        val seat = seatIndex.coerceIn(0, 3)
        val now = System.currentTimeMillis()
        _seatBubbles.value = _seatBubbles.value + (seat to SeatBubble(seat, phrase, now))
        viewModelScope.launch {
            delay(2800)
            val current = _seatBubbles.value[seat]
            if (current != null && current.timestampMs == now) {
                _seatBubbles.value = _seatBubbles.value - seat
            }
        }
    }

    fun updateDisplayName(newName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateDisplayName(currentUserId, newName)
            _isBusy.value = false
            res.onSuccess { onSuccess() }
                .onFailure { _actionError.value = it.localizedMessage ?: "تعذّر تحديث الاسم" }
        }
    }

    // --- Solo Mode vs Smart Bots ---
    fun startSoloMatch(targetScore: Int = 152) {
        stopRoomObserver()
        botTurnJob?.cancel()
        resetTurnTimerAndStandby()
        lastProcessedTable = null
        _seatBubbles.value = emptyMap()
        _lastEloDelta.value = null
        currentSoloMatchKey = "solo_${System.currentTimeMillis()}"
        val profile = (playerProfileState.value as? UiState.Success)?.data
        val myName = profile?.displayName ?: initialDisplayName.ifBlank { "أنت" }
        val myRating = profile?.rating?.toInt() ?: 22

        val seats = listOf(
            BalootSeat(0, currentUserId, myName, myRating, false),
            BalootSeat(1, "BOT_1", "أبو فهد", myRating + 15, true),
            BalootSeat(2, "BOT_2", "خويك (سعد)", myRating + 5, true),
            BalootSeat(3, "BOT_3", "أبو نايف", myRating - 10, true),
        )
        val initial = BalootEngine.dealNewRound(
            dealerIndex = 3,
            roundNumber = 1,
            seats = seats,
            existingUsScore = 0,
            existingThemScore = 0,
            targetScore = targetScore,
            roomCode = null,
            isOnline = false,
            hostId = currentUserId,
            mySeatIndex = 0,
        )
        updateObservedTableState(initial)
        scheduleBotTurnIfNeeded(initial)
    }

    // --- Online Multiplayer Room Flow ---
    fun createOnlineRoom(roomType: String, targetScore: Int, onRoomCreated: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = (playerProfileState.value as? UiState.Success)?.data
            val name = profile?.displayName ?: initialDisplayName
            val rating = profile?.rating?.toInt() ?: 22
            val res = repository.createRoom(name, rating, roomType, targetScore)
            _isBusy.value = false
            res.onSuccess { code ->
                observeOnlineRoom(code)
                onRoomCreated(code)
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّر إنشاء الجلسة"
            }
        }
    }

    fun joinOnlineRoom(roomCode: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val profile = (playerProfileState.value as? UiState.Success)?.data
            val name = profile?.displayName ?: initialDisplayName
            val rating = profile?.rating?.toInt() ?: 22
            val res = repository.joinRoom(roomCode, name, rating)
            _isBusy.value = false
            res.onSuccess { code ->
                observeOnlineRoom(code)
                onJoined(code)
            }.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّر الانضمام للجلسة"
            }
        }
    }

    fun observeOnlineRoom(roomCode: String) {
        roomObserverJob?.cancel()
        chatObserverJob?.cancel()
        botTurnJob?.cancel()
        resetTurnTimerAndStandby()
        lastProcessedTable = null
        _tableChatMessages.value = emptyList()
        _lastEloDelta.value = null
        roomObserverJob = viewModelScope.launch {
            repository.observeRoom(roomCode)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    _actionError.value = (firestoreError ?: error).localizedMessage ?: "انقطع الاتصال بالجلسة"
                }
                .collect { room ->
                    _activeRoom.value = room
                    if (room != null && room.status != "waiting") {
                        val table = room.toBalootTableState(currentUserId)
                        updateObservedTableState(table)
                        if (table.phase == GamePhase.MATCH_FINISHED) {
                            recordOnlineMatchIfNeeded(table)
                        } else {
                            scheduleBotTurnIfNeeded(table)
                        }
                    } else if (room == null) {
                        updateObservedTableState(null)
                    }
                }
        }
        chatObserverJob = viewModelScope.launch {
            var initialLoad = true
            val seenIds = mutableSetOf<String>()
            repository.observeRoomMessages(roomCode)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing table chat", firestoreError ?: error)
                }
                .collect { messages ->
                    _tableChatMessages.value = messages
                    if (initialLoad) {
                        messages.forEach { if (it.id.isNotBlank()) seenIds.add(it.id) }
                        initialLoad = false
                    } else {
                        messages.forEach { msg ->
                            if (msg.id.isNotBlank() && seenIds.add(msg.id) && msg.senderUid != currentUserId) {
                                showSeatBubble(msg.seatIndex, msg.text)
                            }
                        }
                    }
                }
        }
    }

    fun leaveWaitingRoom(onLeft: () -> Unit) {
        val room = _activeRoom.value ?: run {
            onLeft()
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.leaveWaitingRoom(room)
            _isBusy.value = false
            stopRoomObserver()
            onLeft()
        }
    }

    fun startOnlineRoomMatch() {
        val room = _activeRoom.value ?: return
        if (room.hostId != currentUserId || room.status != "waiting") return
        viewModelScope.launch {
            _isBusy.value = true
            val botNames = listOf("أبو فهد (بوت)", "سعد (بوت)", "أبو نايف (بوت)", "نواف (بوت)")
            val filledSeats = (0..3).map { idx ->
                val uid = room.seatUid(idx)
                if (uid.isNotBlank()) {
                    BalootSeat(
                        seatIndex = idx,
                        uid = uid,
                        name = room.seatName(idx),
                        rating = room.seatRating(idx),
                        isBot = uid.startsWith("BOT_"),
                    )
                } else {
                    BalootSeat(
                        seatIndex = idx,
                        uid = "BOT_$idx",
                        name = botNames[idx],
                        rating = 22,
                        isBot = true,
                    )
                }
            }
            val initial = BalootEngine.dealNewRound(
                dealerIndex = 3,
                roundNumber = 1,
                seats = filledSeats,
                existingUsScore = 0,
                existingThemScore = 0,
                targetScore = room.targetScore.toInt(),
                roomCode = room.roomCode,
                isOnline = true,
                hostId = room.hostId,
                mySeatIndex = room.mySeatIndex(currentUserId),
            )
            val updates = initial.toFirestoreUpdates(status = "playing", includeSeats = true)
            val res = repository.updateRoomState(room.roomCode, updates)
            _isBusy.value = false
            res.onFailure {
                _actionError.value = it.localizedMessage ?: "تعذّر بدء المباراة"
            }
        }
    }

    fun exitTable() {
        botTurnJob?.cancel()
        resetTurnTimerAndStandby()
        stopRoomObserver()
        lastProcessedTable = null
        _seatBubbles.value = emptyMap()
        _tableState.value = null
        _lastEloDelta.value = null
    }

    private fun stopRoomObserver() {
        roomObserverJob?.cancel()
        roomObserverJob = null
        chatObserverJob?.cancel()
        chatObserverJob = null
        _tableChatMessages.value = emptyList()
        _activeRoom.value = null
    }

    // --- Game Actions (Human & Bot) ---
    fun submitBid(
        choice: BalootEngine.BotBidChoice,
        suit: Suit? = null,
        passLabel: String? = null,
    ) {
        if (_isAutoPlayStandby.value) return
        val table = _tableState.value ?: return
        val mySeat = table.mySeatIndex
        if (table.turnIndex != mySeat) return
        if (table.phase != GamePhase.BIDDING_R1 && table.phase != GamePhase.BIDDING_R2) return
        applyBidMove(table, mySeat, choice, suit, passLabel)
    }

    fun submitDoubling(doubleIt: Boolean) {
        if (_isAutoPlayStandby.value) return
        val table = _tableState.value ?: return
        if (table.phase != GamePhase.DOUBLING) return
        val mySeat = table.mySeatIndex
        if (mySeat != table.turnIndex) return
        applyDoublingMove(table, mySeat, doubleIt)
    }

    fun playCard(card: BalootCard) {
        if (_isAutoPlayStandby.value) return
        val table = _tableState.value ?: return
        val mySeat = table.mySeatIndex
        if (table.phase != GamePhase.PLAYING || table.turnIndex != mySeat) return
        val hand = table.hands[mySeat].orEmpty()
        val legal = BalootEngine.getLegalCards(hand, table.tableCards, table.mode, table.trumpSuit, mySeat)
        if (card !in legal) return
        applyCardMove(table, mySeat, card)
    }

    fun declareSelectedProjects(selectedProjects: List<DetectedProject>) {
        val table = _tableState.value ?: return
        if (table.phase != GamePhase.DOUBLING && table.phase != GamePhase.PLAYING) return
        val mySeat = table.mySeatIndex
        val myHand = table.hands[mySeat].orEmpty()
        val roundKey = buildRoundDealKey(table)
        resolvedProjectsRoundKey = roundKey
        _pendingProjectsToDeclare.value = emptyList()

        if (myHand.size < 8 || (table.usTricksCount + table.themTricksCount) > 0) return

        val allowed = BalootEngine.detectDeclarableProjectsForHand(
            hand = myHand,
            seatIndex = mySeat,
            mode = table.mode,
            trumpSuit = table.trumpSuit,
        )
        val allowedRemaining = allowed.toMutableList()
        val validated = mutableListOf<DetectedProject>()
        for (choice in selectedProjects) {
            val matchIdx = allowedRemaining.indexOfFirst {
                it.kind == choice.kind && it.description == choice.description
            }
            if (matchIdx >= 0) {
                validated.add(allowedRemaining.removeAt(matchIdx))
            }
        }
        if (validated.isEmpty()) return

        val descriptions = validated.map { it.description }
        val myTeam = mySeat % 2
        val seatName = table.seats.getOrNull(mySeat)?.name ?: "لاعب"
        val spokenPhrase = validated.joinToString(" و ") { it.kind.arabicName }
        triggerSeatSpeech(mySeat, spokenPhrase)

        val updated = if (myTeam == 0) {
            table.copy(
                usProjects = (table.usProjects + descriptions).take(8),
                lastActionText = "$seatName أعلن مشروع: ${descriptions.joinToString("، ")}".take(140),
            )
        } else {
            table.copy(
                themProjects = (table.themProjects + descriptions).take(8),
                lastActionText = "$seatName أعلن مشروع: ${descriptions.joinToString("، ")}".take(140),
            )
        }
        commitTableState(updated)
    }

    fun skipProjectDeclaration() {
        val table = _tableState.value
        if (table != null) {
            resolvedProjectsRoundKey = buildRoundDealKey(table)
        }
        _pendingProjectsToDeclare.value = emptyList()
    }

    fun advanceToNextRound() {
        val table = _tableState.value ?: return
        if (table.phase != GamePhase.ROUND_SUMMARY) return
        if (table.isOnline && table.hostId != currentUserId) return

        val nextDealer = (table.dealerIndex + 1) % 4
        val nextState = BalootEngine.dealNewRound(
            dealerIndex = nextDealer,
            roundNumber = table.roundNumber + 1,
            seats = table.seats,
            existingUsScore = table.usScore,
            existingThemScore = table.themScore,
            targetScore = table.targetScore,
            roomCode = table.roomCode,
            isOnline = table.isOnline,
            hostId = table.hostId,
            mySeatIndex = table.mySeatIndex,
        )
        commitTableState(nextState)
    }

    private fun applyBidMove(
        table: BalootTableState,
        seat: Int,
        choice: BalootEngine.BotBidChoice,
        suit: Suit?,
        passLabel: String? = null,
    ) {
        val seatName = table.seats.getOrNull(seat)?.name ?: "لاعب"
        val firstTurn = (table.dealerIndex + 1) % 4
        val nextSeat = (seat + 1) % 4
        val isRound2 = table.phase == GamePhase.BIDDING_R2
        val effectivePassWord = passLabel?.takeIf { it == "بس" || it == "ولا" }
            ?: if (isRound2) "ولا" else "بس"

        val updated: BalootTableState = when (choice) {
            BalootEngine.BotBidChoice.SUN -> {
                BalootEngine.completeDealAfterBid(table, seat, GameMode.SUN, null)
            }
            BalootEngine.BotBidChoice.HOKM_OPEN -> {
                val trump = table.openCard?.suit ?: Suit.SPADES
                if (nextSeat == firstTurn) {
                    BalootEngine.completeDealAfterBid(table, seat, GameMode.HOKM, trump)
                } else {
                    table.copy(
                        mode = GameMode.HOKM,
                        trumpSuit = trump,
                        bidderIndex = seat,
                        turnIndex = nextSeat,
                        lastActionText = "$seatName طلب حكم ${trump.symbol}",
                    )
                }
            }
            BalootEngine.BotBidChoice.HOKM_SUIT -> {
                val chosen = suit ?: Suit.entries.first { it != table.openCard?.suit }
                BalootEngine.completeDealAfterBid(table, seat, GameMode.HOKM, chosen)
            }
            BalootEngine.BotBidChoice.PASS -> {
                if (nextSeat == firstTurn) {
                    if (!isRound2) {
                        if (table.bidderIndex >= 0 && table.mode == GameMode.HOKM) {
                            val completed = BalootEngine.completeDealAfterBid(
                                table,
                                table.bidderIndex,
                                GameMode.HOKM,
                                table.trumpSuit,
                            )
                            completed.copy(
                                lastActionText = "$seatName: $effectivePassWord — ${completed.lastActionText}",
                            )
                        } else {
                            table.copy(
                                phase = GamePhase.BIDDING_R2,
                                turnIndex = firstTurn,
                                lastActionText = "$seatName: $effectivePassWord — انتقل الشراء للدور الثاني",
                            )
                        }
                    } else {
                        // All passed Round 2 -> Re-deal
                        BalootEngine.dealNewRound(
                            dealerIndex = (table.dealerIndex + 1) % 4,
                            roundNumber = table.roundNumber,
                            seats = table.seats,
                            existingUsScore = table.usScore,
                            existingThemScore = table.themScore,
                            targetScore = table.targetScore,
                            roomCode = table.roomCode,
                            isOnline = table.isOnline,
                            hostId = table.hostId,
                            mySeatIndex = table.mySeatIndex,
                        ).copy(lastActionText = "$seatName: $effectivePassWord — إعادة التوزيع")
                    }
                } else {
                    table.copy(
                        turnIndex = nextSeat,
                        lastActionText = "$seatName: $effectivePassWord",
                    )
                }
            }
        }
        commitTableState(updated)
    }

    private fun applyDoublingMove(table: BalootTableState, seat: Int, doubleIt: Boolean) {
        val seatName = table.seats.getOrNull(seat)?.name ?: "لاعب"
        val firstLead = (table.dealerIndex + 1) % 4
        val updated = if (doubleIt) {
            val nextLevel = (table.bidLevel + 1).coerceAtMost(4)
            val label = when (nextLevel) {
                2 -> "دبل (×2)"
                3 -> "ثري (×3)"
                else -> "قهوة!"
            }
            table.copy(
                phase = GamePhase.PLAYING,
                bidLevel = nextLevel,
                turnIndex = firstLead,
                lastActionText = "$seatName أعلن $label",
            )
        } else {
            table.copy(
                phase = GamePhase.PLAYING,
                turnIndex = firstLead,
                lastActionText = "بدأ اللعب — الدور على ${table.seats.getOrNull(firstLead)?.name.orEmpty()}",
            )
        }
        commitTableState(updated)
    }

    private fun applyCardMove(table: BalootTableState, seat: Int, card: BalootCard) {
        val updated = BalootEngine.applyCardPlay(table, seat, card)
        commitTableState(updated)
    }

    private fun commitTableState(newState: BalootTableState) {
        if (!newState.isOnline) {
            updateObservedTableState(newState)
            if (newState.phase == GamePhase.MATCH_FINISHED) {
                recordSoloMatchIfNeeded(newState)
            } else {
                scheduleBotTurnIfNeeded(newState)
            }
        } else {
            val code = newState.roomCode ?: return
            val status = if (newState.phase == GamePhase.MATCH_FINISHED) "finished" else "playing"
            viewModelScope.launch {
                repository.updateRoomState(
                    code,
                    newState.toFirestoreUpdates(status = status, includeSeats = false),
                )
            }
        }
    }

    private fun updateObservedTableState(newState: BalootTableState?) {
        val previous = lastProcessedTable
        _tableState.value = newState
        lastProcessedTable = newState
        if (previous != null && newState != null && previous != newState) {
            emitVoiceAndBubbleCuesForTransition(previous, newState)
        }
        syncTurnTimerAndStandby(newState)
        syncPendingProjects(newState)
    }

    private fun resetTurnTimerAndStandby() {
        turnTimerJob?.cancel()
        turnTimerJob = null
        standbyAutoPlayJob?.cancel()
        standbyAutoPlayJob = null
        lastTurnKey = null
        standbyRoundNumber = null
        resolvedProjectsRoundKey = null
        _turnRemainingSeconds.value = null
        _isAutoPlayStandby.value = false
        _pendingProjectsToDeclare.value = emptyList()
    }

    private fun buildRoundDealKey(state: BalootTableState): String {
        return "${state.roomCode ?: currentSoloMatchKey}|r${state.roundNumber}|d${state.dealerIndex}"
    }

    private fun syncPendingProjects(state: BalootTableState?) {
        if (state == null || (state.phase != GamePhase.DOUBLING && state.phase != GamePhase.PLAYING)) {
            _pendingProjectsToDeclare.value = emptyList()
            if (state == null) {
                resolvedProjectsRoundKey = null
            }
            return
        }
        val roundKey = buildRoundDealKey(state)
        if (resolvedProjectsRoundKey == roundKey) {
            _pendingProjectsToDeclare.value = emptyList()
            return
        }
        val mySeat = state.mySeatIndex
        val myHand = state.hands[mySeat].orEmpty()
        if (myHand.size < 8 || (state.usTricksCount + state.themTricksCount) > 0) {
            resolvedProjectsRoundKey = roundKey
            _pendingProjectsToDeclare.value = emptyList()
            return
        }
        val declarable = BalootEngine.detectDeclarableProjectsForHand(
            hand = myHand,
            seatIndex = mySeat,
            mode = state.mode,
            trumpSuit = state.trumpSuit,
        )
        if (declarable.isEmpty()) {
            resolvedProjectsRoundKey = roundKey
            _pendingProjectsToDeclare.value = emptyList()
        } else if (_isAutoPlayStandby.value) {
            // In standby auto-play mode, automatically declare available projects so the player keeps their points
            declareSelectedProjects(declarable)
        } else {
            _pendingProjectsToDeclare.value = declarable
        }
    }

    private fun buildTurnKey(state: BalootTableState): String {
        val activeHandSize = state.hands[state.turnIndex]?.size ?: 0
        val tricksPlayed = state.usTricksCount + state.themTricksCount
        return "${state.roomCode ?: currentSoloMatchKey}|r${state.roundNumber}|d${state.dealerIndex}|${state.phase.name}|m${state.mode.name}|b${state.bidderIndex}|t${state.turnIndex}|tc${state.tableCards.size}|h$activeHandSize|tr$tricksPlayed|o${state.openCard?.code.orEmpty()}"
    }

    private fun syncTurnTimerAndStandby(state: BalootTableState?) {
        if (state == null || state.phase !in ACTIVE_TURN_PHASES) {
            // Round finished, match finished, or left table -> clear standby and timer
            resetTurnTimerAndStandby()
            return
        }

        // If a new round started, restore manual control for the player
        if (standbyRoundNumber != null && state.roundNumber != standbyRoundNumber) {
            standbyRoundNumber = null
            _isAutoPlayStandby.value = false
        }

        val turnKey = buildTurnKey(state)
        val isMyTurn = state.turnIndex == state.mySeatIndex

        // If the local player is in standby waiting mode for this round and it is their turn, auto-play smoothly
        if (isMyTurn && _isAutoPlayStandby.value) {
            turnTimerJob?.cancel()
            turnTimerJob = null
            _turnRemainingSeconds.value = null
            if (turnKey == lastTurnKey && standbyAutoPlayJob?.isActive == true) return
            lastTurnKey = turnKey
            standbyAutoPlayJob?.cancel()
            standbyAutoPlayJob = viewModelScope.launch {
                delay(STANDBY_AUTOPLAY_DELAY_MS)
                val latest = _tableState.value ?: return@launch
                if (!_isAutoPlayStandby.value) return@launch
                if (buildTurnKey(latest) != turnKey || latest.turnIndex != latest.mySeatIndex) return@launch
                executeAutoPlayMove(latest, latest.mySeatIndex)
            }
            return
        }

        // Avoid restarting the countdown on duplicate snapshots for the same turn
        if (turnKey == lastTurnKey && turnTimerJob?.isActive == true) return
        lastTurnKey = turnKey
        standbyAutoPlayJob?.cancel()
        standbyAutoPlayJob = null
        turnTimerJob?.cancel()

        turnTimerJob = viewModelScope.launch {
            for (remaining in TURN_TIMEOUT_SECONDS downTo 0) {
                val latest = _tableState.value ?: return@launch
                if (buildTurnKey(latest) != turnKey) return@launch
                _turnRemainingSeconds.value = remaining
                if (remaining == 0) {
                    if (latest.turnIndex == latest.mySeatIndex) {
                        // Local player timed out -> enter standby waiting mode until current round finishes and auto-play
                        standbyRoundNumber = latest.roundNumber
                        _isAutoPlayStandby.value = true
                        _turnRemainingSeconds.value = null
                        showSeatBubble(latest.mySeatIndex, "وضع الانتظار")
                        _actionSuccess.value = "انتهى الوقت — تم تفعيل اللعب التلقائي ووضعك في الانتظار حتى نهاية الجولة"
                        executeAutoPlayMove(latest, latest.mySeatIndex)
                    } else if (latest.isOnline && latest.hostId == currentUserId) {
                        // Host grace fallback if a remote human player disconnected
                        val activeSeat = latest.seats.getOrNull(latest.turnIndex)
                        if (activeSeat != null && !activeSeat.isBot) {
                            delay(ONLINE_HOST_TIMEOUT_GRACE_MS)
                            val afterGrace = _tableState.value ?: return@launch
                            if (buildTurnKey(afterGrace) == turnKey) {
                                executeAutoPlayMove(afterGrace, afterGrace.turnIndex)
                            }
                        }
                    }
                    return@launch
                }
                delay(1000L)
            }
        }
    }

    private fun executeAutoPlayMove(state: BalootTableState, seat: Int) {
        val hand = state.hands[seat].orEmpty()
        when (state.phase) {
            GamePhase.BIDDING_R1 -> {
                applyBidMove(state, seat, BalootEngine.BotBidChoice.PASS, null, "بس")
            }
            GamePhase.BIDDING_R2 -> {
                applyBidMove(state, seat, BalootEngine.BotBidChoice.PASS, null, "ولا")
            }
            GamePhase.DOUBLING -> {
                applyDoublingMove(state, seat, false)
            }
            GamePhase.PLAYING -> {
                if (hand.isNotEmpty()) {
                    val card = BalootEngine.chooseBotCard(
                        hand = hand,
                        tableCards = state.tableCards,
                        mode = state.mode,
                        trumpSuit = state.trumpSuit,
                        seatIndex = seat,
                    )
                    applyCardMove(state, seat, card)
                }
            }
            else -> Unit
        }
    }

    private fun emitVoiceAndBubbleCuesForTransition(prev: BalootTableState, next: BalootTableState) {
        if (prev.phase == GamePhase.BIDDING_R1 || prev.phase == GamePhase.BIDDING_R2) {
            when {
                next.phase == GamePhase.DOUBLING -> {
                    when {
                        next.mode == GameMode.SUN && next.bidderIndex in 0..3 -> {
                            triggerSeatSpeech(next.bidderIndex, "صن")
                        }
                        next.mode == GameMode.HOKM && prev.mode == GameMode.NONE && next.bidderIndex in 0..3 -> {
                            triggerSeatSpeech(next.bidderIndex, "حكم")
                        }
                        next.mode == GameMode.HOKM && prev.mode == GameMode.HOKM -> {
                            val passPhrase = if (next.lastActionText.contains(": ولا")) "ولا" else "بس"
                            triggerSeatSpeech(prev.turnIndex, passPhrase)
                        }
                    }
                }
                prev.phase == GamePhase.BIDDING_R1 && next.phase == GamePhase.BIDDING_R1 -> {
                    if (prev.mode == GameMode.NONE && next.mode == GameMode.HOKM && next.bidderIndex in 0..3) {
                        triggerSeatSpeech(next.bidderIndex, "حكم")
                    } else if (prev.turnIndex != next.turnIndex && prev.mode == next.mode) {
                        val passPhrase = if (next.lastActionText.endsWith(": ولا")) "ولا" else "بس"
                        triggerSeatSpeech(prev.turnIndex, passPhrase)
                    }
                }
                prev.phase == GamePhase.BIDDING_R1 && next.phase == GamePhase.BIDDING_R2 -> {
                    val passPhrase = if (next.lastActionText.contains(": ولا")) "ولا" else "بس"
                    triggerSeatSpeech(prev.turnIndex, passPhrase)
                }
                prev.phase == GamePhase.BIDDING_R2 && next.phase == GamePhase.BIDDING_R2 && prev.turnIndex != next.turnIndex -> {
                    val passPhrase = if (next.lastActionText.endsWith(": بس")) "بس" else "ولا"
                    triggerSeatSpeech(prev.turnIndex, passPhrase)
                }
                prev.phase == GamePhase.BIDDING_R2 && next.phase == GamePhase.BIDDING_R1 && next.dealerIndex != prev.dealerIndex -> {
                    val passPhrase = if (next.lastActionText.contains(": بس")) "بس" else "ولا"
                    triggerSeatSpeech(prev.turnIndex, passPhrase)
                }
            }
        }

        val isKabootFinish = prev.phase == GamePhase.PLAYING &&
            (next.phase == GamePhase.ROUND_SUMMARY || next.phase == GamePhase.MATCH_FINISHED) &&
            (next.usTricksCount == 8 || next.themTricksCount == 8)

        if (isKabootFinish) {
            val kabootWinnerSeat = next.lastTrickWinner.takeIf { it in 0..3 }
                ?: if (next.usTricksCount == 8) 0 else 1
            val winningPartnerSeat = (kabootWinnerSeat + 2) % 4
            triggerSeatSpeech(winningPartnerSeat, "وحش يا خوي", queueAdd = false)
        }
    }

    private fun triggerSeatSpeech(seatIndex: Int, phrase: String, queueAdd: Boolean = false) {
        val seat = seatIndex.coerceIn(0, 3)
        showSeatBubble(seat, phrase)
        _voiceCues.tryEmit(
            TableVoiceCue(
                id = System.nanoTime(),
                seatIndex = seat,
                phrase = phrase,
                queueAdd = queueAdd,
            ),
        )
    }

    private fun scheduleBotTurnIfNeeded(state: BalootTableState) {
        botTurnJob?.cancel()
        if (state.phase !in setOf(
                GamePhase.BIDDING_R1,
                GamePhase.BIDDING_R2,
                GamePhase.DOUBLING,
                GamePhase.PLAYING,
            )
        ) return

        val activeSeat = state.seats.getOrNull(state.turnIndex) ?: return
        if (!activeSeat.isBot) return
        // In online mode, only the host drives bot seats so bots don't double-play!
        if (state.isOnline && state.hostId != currentUserId) return

        botTurnJob = viewModelScope.launch {
            delay(580)
            val latest = _tableState.value ?: return@launch
            if (latest.turnIndex != state.turnIndex || latest.phase != state.phase) return@launch
            val seat = latest.turnIndex
            val hand = latest.hands[seat].orEmpty()
            when (latest.phase) {
                GamePhase.BIDDING_R1 -> {
                    val decision = BalootEngine.chooseBotBid(hand, latest.openCard, false, latest.mode)
                    applyBidMove(latest, seat, decision.choice, decision.suit)
                }
                GamePhase.BIDDING_R2 -> {
                    val decision = BalootEngine.chooseBotBid(hand, latest.openCard, true, latest.mode)
                    applyBidMove(latest, seat, decision.choice, decision.suit)
                }
                GamePhase.DOUBLING -> {
                    val doubleIt = BalootEngine.chooseBotDoubling(hand, latest.mode, latest.trumpSuit)
                    applyDoublingMove(latest, seat, doubleIt)
                }
                GamePhase.PLAYING -> {
                    if (hand.isNotEmpty()) {
                        val card = BalootEngine.chooseBotCard(
                            hand, latest.tableCards, latest.mode, latest.trumpSuit, seat
                        )
                        applyCardMove(latest, seat, card)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun recordSoloMatchIfNeeded(state: BalootTableState) {
        if (recordedSoloMatchId == currentSoloMatchKey) return
        recordedSoloMatchId = currentSoloMatchKey
        val profile = (playerProfileState.value as? UiState.Success)?.data ?: return
        val didWin = state.winnerTeam == 0
        val delta = BalootEngine.calculateMatchPointsDelta(
            didWin = didWin,
            currentLossStreak = profile.lossStreak.toInt(),
        )
        val oppSummary = "${state.seats[1].name} • ${state.seats[3].name}"
        viewModelScope.launch {
            repository.recordMatchResult(
                userId = currentUserId,
                currentProfile = profile,
                didWin = didWin,
                usScore = state.usScore,
                themScore = state.themScore,
                opponentSummary = oppSummary,
                mode = "solo",
                roomCode = null,
                ratingDelta = delta,
            ).onSuccess { _lastEloDelta.value = it }
        }
    }

    private fun recordOnlineMatchIfNeeded(state: BalootTableState) {
        val code = state.roomCode ?: return
        if (code in recordedOnlineRooms) return
        recordedOnlineRooms.add(code)
        val profile = (playerProfileState.value as? UiState.Success)?.data ?: return
        val myTeam = state.mySeatIndex % 2
        val didWin = state.winnerTeam == myTeam
        val myScore = if (myTeam == 0) state.usScore else state.themScore
        val oppScore = if (myTeam == 0) state.themScore else state.usScore
        val oppSeats = state.seats.filter { it.seatIndex % 2 != myTeam }
        val delta = BalootEngine.calculateMatchPointsDelta(
            didWin = didWin,
            currentLossStreak = profile.lossStreak.toInt(),
        )
        val oppSummary = oppSeats.joinToString(" • ") { it.name }
        viewModelScope.launch {
            repository.recordMatchResult(
                userId = currentUserId,
                currentProfile = profile,
                didWin = didWin,
                usScore = myScore,
                themScore = oppScore,
                opponentSummary = oppSummary,
                mode = "online",
                roomCode = code,
                ratingDelta = delta,
            ).onSuccess { _lastEloDelta.value = it }
        }
    }

    private fun GameRoom.toBalootTableState(uid: String): BalootTableState {
        val seats = (0..3).map { idx ->
            val seatUid = seatUid(idx)
            BalootSeat(
                seatIndex = idx,
                uid = seatUid,
                name = seatName(idx).ifBlank { "لاعب ${idx + 1}" },
                rating = seatRating(idx),
                isBot = seatUid.startsWith("BOT_") || seatUid.isBlank(),
            )
        }
        val parsedHands = (0..3).associateWith { idx ->
            hands[idx.toString()].orEmpty().mapNotNull { BalootCard.parse(it) }
        }
        return BalootTableState(
            roomCode = roomCode,
            isOnline = true,
            hostId = hostId,
            targetScore = targetScore.toInt(),
            mySeatIndex = mySeatIndex(uid).coerceAtLeast(0),
            seats = seats,
            usScore = usScore.toInt(),
            themScore = themScore.toInt(),
            roundNumber = roundNumber.toInt(),
            turnIndex = turnIndex.toInt(),
            dealerIndex = dealerIndex.toInt(),
            phase = GamePhase.fromCode(gamePhase),
            mode = GameMode.fromCode(gameMode),
            trumpSuit = Suit.fromCode(trumpSuit),
            bidderIndex = bidderIndex.toInt(),
            bidLevel = bidLevel.toInt(),
            openCard = BalootCard.parse(openCard),
            hands = parsedHands,
            tableCards = tableCards.mapNotNull { PlayedCardEntry.parse(it) },
            lastTrickCards = lastTrickCards.mapNotNull { PlayedCardEntry.parse(it) },
            lastTrickWinner = lastTrickWinner.toInt(),
            usTricksCount = usTricksCount.toInt(),
            themTricksCount = themTricksCount.toInt(),
            usRoundPoints = usRoundPoints.toInt(),
            themRoundPoints = themRoundPoints.toInt(),
            usProjects = usProjects,
            themProjects = themProjects,
            winnerTeam = winnerTeam.toInt(),
            lastActionText = lastActionText,
        )
    }

    private fun BalootTableState.toFirestoreUpdates(status: String, includeSeats: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "status" to status,
            "usScore" to usScore.toLong(),
            "themScore" to themScore.toLong(),
            "roundNumber" to roundNumber.toLong(),
            "turnIndex" to turnIndex.toLong(),
            "dealerIndex" to dealerIndex.toLong(),
            "gamePhase" to phase.code,
            "gameMode" to mode.code,
            "trumpSuit" to (trumpSuit?.code ?: "none"),
            "bidderIndex" to bidderIndex.toLong(),
            "bidLevel" to bidLevel.toLong(),
            "openCard" to (openCard?.code ?: ""),
            "hands" to mapOf(
                "0" to hands[0].orEmpty().map { it.code },
                "1" to hands[1].orEmpty().map { it.code },
                "2" to hands[2].orEmpty().map { it.code },
                "3" to hands[3].orEmpty().map { it.code },
            ),
            "tableCards" to tableCards.map { it.serialize() },
            "lastTrickCards" to lastTrickCards.map { it.serialize() },
            "lastTrickWinner" to lastTrickWinner.toLong(),
            "usTricksCount" to usTricksCount.toLong(),
            "themTricksCount" to themTricksCount.toLong(),
            "usRoundPoints" to usRoundPoints.toLong(),
            "themRoundPoints" to themRoundPoints.toLong(),
            "usProjects" to usProjects,
            "themProjects" to themProjects,
            "winnerTeam" to winnerTeam.toLong(),
            "lastActionText" to lastActionText.take(140),
        )
        if (includeSeats) {
            seats.forEachIndexed { idx, seat ->
                map["seat${idx}Uid"] = seat.uid
                map["seat${idx}Name"] = seat.name.take(40)
                map["seat${idx}Rating"] = seat.rating.toLong()
            }
        }
        return map
    }

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val TURN_TIMEOUT_SECONDS = 10
        const val STANDBY_AUTOPLAY_DELAY_MS = 650L
        const val ONLINE_HOST_TIMEOUT_GRACE_MS = 2000L
        val ACTIVE_TURN_PHASES = setOf(
            GamePhase.BIDDING_R1,
            GamePhase.BIDDING_R2,
            GamePhase.DOUBLING,
            GamePhase.PLAYING,
        )
    }
}
