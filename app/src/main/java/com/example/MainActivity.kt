package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.configureAppCheck
import com.example.auth.authStateFlow
import com.example.auth.consumePendingSignUpDisplayName
import com.example.auth.signOut
import com.example.billing.BillingLaunchResult
import com.example.billing.DailySessionQuotaStorage
import com.example.billing.PlayBillingManager
import com.example.billing.SubscriptionPlan
import com.example.data.JammanaRepository
import com.example.ui.AuthScreen
import com.example.ui.AvatarStorage
import com.example.ui.BalootCalculatorScreen
import com.example.ui.FriendsScreen
import com.example.ui.GameTableScreen
import com.example.ui.HistoryScreen
import com.example.ui.LeaderboardScreen
import com.example.ui.LobbyScreen
import com.example.ui.MainViewModel
import com.example.ui.ProfileScreen
import com.example.ui.SplashScreen
import com.example.ui.SubscriptionSheet
import com.example.ui.WaitingRoomScreen
import com.example.ui.components.CardDeckStyle
import com.example.ui.components.TableBackgroundStorage
import com.example.ui.components.TableBackgroundStyle
import com.example.ui.theme.JammanaTheme
import com.example.ui.theme.ShaddahaWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class HomeScreenDest {
    LOBBY,
    FRIENDS,
    LEADERBOARD,
    HISTORY,
    PROFILE,
    CALCULATOR,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            JammanaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
    ) {
        Crossfade(
            targetState = showSplash,
            animationSpec = tween(durationMillis = 420),
            label = "splashCrossfade",
        ) { splashVisible ->
            if (splashVisible) {
                SplashScreen(onFinished = { showSplash = false })
            } else {
            val user = currentUser
            if (user == null) {
                AuthScreen(onAuthSuccess = {})
            } else {
                val uid = user.uid
                val displayName = remember(uid) {
                    user.displayName?.takeIf { it.isNotBlank() }
                        ?: consumePendingSignUpDisplayName()
                        ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                        ?: "لاعب شدها"
                }
                val photoUrl = user.photoUrl?.toString()
                key(uid) {
                    AuthenticatedShell(
                        currentUserId = uid,
                        initialDisplayName = displayName,
                        initialAvatarUrl = photoUrl,
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    initialDisplayName: String,
    initialAvatarUrl: String?,
) {
    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = JammanaRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName,
                    initialAvatarUrl = initialAvatarUrl,
                )
            }
        },
    )

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val billingManager = remember(context) { PlayBillingManager(context) }
    val billingSnapshot by billingManager.snapshot.collectAsStateWithLifecycle()

    DisposableEffect(billingManager) {
        billingManager.connectAndSync()
        onDispose { billingManager.release() }
    }

    val profileState by viewModel.playerProfileState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val historyState by viewModel.matchHistoryState.collectAsStateWithLifecycle()
    val publicRoomsState by viewModel.publicRoomsState.collectAsStateWithLifecycle()
    val myRoomsState by viewModel.myRoomsState.collectAsStateWithLifecycle()
    val friendsState by viewModel.friendsState.collectAsStateWithLifecycle()
    val incomingInvites by viewModel.incomingInvitesState.collectAsStateWithLifecycle()
    val outgoingInvites by viewModel.outgoingInvitesState.collectAsStateWithLifecycle()
    val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()
    val tableState by viewModel.tableState.collectAsStateWithLifecycle()
    val seatBubbles by viewModel.seatBubbles.collectAsStateWithLifecycle()
    val tableChatMessages by viewModel.tableChatMessages.collectAsStateWithLifecycle()
    val turnRemainingSeconds by viewModel.turnRemainingSeconds.collectAsStateWithLifecycle()
    val isAutoPlayStandby by viewModel.isAutoPlayStandby.collectAsStateWithLifecycle()
    val pendingProjectsToDeclare by viewModel.pendingProjectsToDeclare.collectAsStateWithLifecycle()
    val isVoiceMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
    val lastEloDelta by viewModel.lastEloDelta.collectAsStateWithLifecycle()
    val aiCoachState by viewModel.aiCoachState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val actionSuccess by viewModel.actionSuccess.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionError) {
        val msg = actionError
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionError()
        }
    }
    LaunchedEffect(actionSuccess) {
        val msg = actionSuccess
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionSuccess()
        }
    }
    LaunchedEffect(billingManager) {
        billingManager.purchaseEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var currentDest by rememberSaveable { mutableStateOf(HomeScreenDest.LOBBY) }
    var selectedTableBackground by remember(currentUserId) {
        mutableStateOf(TableBackgroundStorage.loadSelected(context, currentUserId))
    }
    var previewOwnedBackgrounds by remember(currentUserId) {
        mutableStateOf(TableBackgroundStorage.loadOwnedPreview(context, currentUserId))
    }
    var selectedCardDeckName by rememberSaveable { mutableStateOf(CardDeckStyle.CLASSIC.name) }
    var previewOwnedDeckNames by rememberSaveable { mutableStateOf(listOf(CardDeckStyle.CLASSIC.name)) }
    var previewSubscribedPlanName by rememberSaveable { mutableStateOf<String?>(null) }
    var showSubscriptionSheet by remember { mutableStateOf(false) }
    var subscriptionLimitMessage by remember { mutableStateOf<String?>(null) }
    var localSessionsCreatedToday by remember(currentUserId) {
        mutableStateOf(DailySessionQuotaStorage.getLocalCreatedToday(context, currentUserId))
    }

    val profile = (profileState as? com.example.data.UiState.Success)?.data
    val myRoomsList = (myRoomsState as? com.example.data.UiState.Success)?.data.orEmpty()
    val effectiveSessionsCreatedToday = remember(localSessionsCreatedToday, myRoomsList, currentUserId) {
        val remoteToday = DailySessionQuotaStorage.countFirestoreRoomsHostedToday(myRoomsList, currentUserId)
        maxOf(localSessionsCreatedToday, remoteToday)
    }
    val remainingFreeSessionsToday = (DailySessionQuotaStorage.FREE_DAILY_SESSION_LIMIT - effectiveSessionsCreatedToday)
        .coerceAtLeast(0)

    var effectiveAvatarUrl by remember(currentUserId) {
        mutableStateOf(
            AvatarStorage.resolveAvatarUrl(
                context = context,
                userId = currentUserId,
                googlePhotoUrl = initialAvatarUrl,
                remoteAvatarUrl = profile?.avatarUrl,
            ),
        )
    }

    val selectedCardDeck = remember(selectedCardDeckName) {
        runCatching { CardDeckStyle.valueOf(selectedCardDeckName) }.getOrDefault(CardDeckStyle.CLASSIC)
    }
    val ownedDecks = remember(previewOwnedDeckNames, billingSnapshot.ownedDecksFromPlay) {
        val local = previewOwnedDeckNames.mapNotNull { name ->
            runCatching { CardDeckStyle.valueOf(name) }.getOrNull()
        }.toSet()
        local + billingSnapshot.ownedDecksFromPlay
    }
    val ownedBackgrounds = remember(previewOwnedBackgrounds, billingSnapshot.ownedBackgroundsFromPlay) {
        previewOwnedBackgrounds + billingSnapshot.ownedBackgroundsFromPlay + TableBackgroundStyle.freeBackgrounds
    }
    val previewSubscribedPlan = remember(previewSubscribedPlanName) {
        previewSubscribedPlanName?.let { name -> runCatching { SubscriptionPlan.valueOf(name) }.getOrNull() }
    }
    val activeSubscriptionPlan = billingSnapshot.activeSubscriptionPlanFromPlay ?: previewSubscribedPlan
    val isSubscribed = billingSnapshot.isSubscribedFromPlay || previewSubscribedPlan != null

    val openSubscriptionSheet: (String?) -> Unit = { gateMsg ->
        subscriptionLimitMessage = gateMsg
        showSubscriptionSheet = true
    }

    val handleSelectSubscriptionPlan: (SubscriptionPlan) -> Unit = { plan ->
        if (billingSnapshot.isSubscribedFromPlay && billingSnapshot.activeSubscriptionPlanFromPlay == plan) {
            scope.launch {
                snackbarHostState.showSnackbar("باقة ${plan.title} نشطة عبر Google Play — يمكنك إدارتها من متجر Google Play")
            }
        } else {
            when (val launchResult = billingManager.launchSubscriptionPurchase(activity, plan, currentUserId)) {
                BillingLaunchResult.LaunchedPlaySheet -> {
                    showSubscriptionSheet = false
                }
                is BillingLaunchResult.PreviewFallback -> {
                    previewSubscribedPlanName = plan.name
                    previewOwnedDeckNames = CardDeckStyle.entries.map { it.name }
                    subscriptionLimitMessage = null
                    showSubscriptionSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar(launchResult.reason)
                    }
                }
            }
        }
    }

    val dailyLimitGatePrompt = "لديك جلستان مجانيتان فقط في اليوم للإنشاء. لإنشاء جلسات إضافية بلا حدود، اشترك في شدها بلس (أسبوعي 9.99 ريال، شهري 29 ريال، أو سنوي 149 ريال)."

    val handleSelectOrPurchaseBackground: (TableBackgroundStyle) -> Unit = { bg ->
        val alreadyOwned = bg.priceSar == 0 || bg in ownedBackgrounds
        if (alreadyOwned) {
            selectedTableBackground = bg
            TableBackgroundStorage.saveSelected(context, currentUserId, bg)
            scope.launch {
                snackbarHostState.showSnackbar("تم تفعيل ${bg.title} كخلفية لطاولة اللعب")
            }
        } else {
            when (val launchResult = billingManager.launchBackgroundPurchase(activity, bg, currentUserId)) {
                BillingLaunchResult.LaunchedPlaySheet -> {
                    selectedTableBackground = bg
                    TableBackgroundStorage.saveSelected(context, currentUserId, bg)
                }
                is BillingLaunchResult.PreviewFallback -> {
                    val updatedOwned = previewOwnedBackgrounds + bg
                    previewOwnedBackgrounds = updatedOwned
                    TableBackgroundStorage.saveOwnedPreview(context, currentUserId, updatedOwned)
                    selectedTableBackground = bg
                    TableBackgroundStorage.saveSelected(context, currentUserId, bg)
                    scope.launch {
                        snackbarHostState.showSnackbar(launchResult.reason)
                    }
                }
            }
        }
    }

    val currentTable = tableState
    val currentWaitingRoom = activeRoom
    val friendsList = (friendsState as? com.example.data.UiState.Success)?.data.orEmpty()
    val onlineFriendsCount = remember(friendsList) {
        friendsList.count { it.isOnlineNow() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val desiredPresence = if (currentTable != null || currentWaitingRoom != null) "in_game" else "online"
    val latestDesiredPresence by rememberUpdatedState(desiredPresence)

    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.updatePresence(latestDesiredPresence)
                Lifecycle.Event.ON_STOP -> viewModel.updatePresence("offline")
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.updatePresence("offline")
        }
    }

    LaunchedEffect(currentUserId, desiredPresence) {
        while (true) {
            viewModel.updatePresence(desiredPresence)
            delay(45_000L)
        }
    }

    val performSignOut: () -> Unit = {
        viewModel.clearAiCoach()
        viewModel.exitTable()
        scope.launch {
            withContext(NonCancellable) {
                viewModel.markOfflineBeforeSignOut()
            }
            signOut(credentialManager, scope)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
    ) {
        when {
            currentTable != null -> {
                GameTableScreen(
                    table = currentTable,
                    currentUserId = currentUserId,
                    seatBubbles = seatBubbles,
                    chatMessages = tableChatMessages,
                    lastEloDelta = lastEloDelta,
                    turnRemainingSeconds = turnRemainingSeconds,
                    isAutoPlayStandby = isAutoPlayStandby,
                    pendingProjectsToDeclare = pendingProjectsToDeclare,
                    voiceCues = viewModel.voiceCues,
                    isVoiceMuted = isVoiceMuted,
                    tableBackground = selectedTableBackground,
                    cardDeckStyle = selectedCardDeck,
                    myAvatarUrl = effectiveAvatarUrl,
                    onBid = { choice, suit, passLabel -> viewModel.submitBid(choice, suit, passLabel) },
                    onDoubleDecision = { dbl -> viewModel.submitDoubling(dbl) },
                    onPlayCard = { card -> viewModel.playCard(card) },
                    onDeclareProjects = { projects -> viewModel.declareSelectedProjects(projects) },
                    onSkipProjects = { viewModel.skipProjectDeclaration() },
                    onNextRound = { viewModel.advanceToNextRound() },
                    onSendPhrase = { phrase -> viewModel.sendQuickPhrase(phrase) },
                    onSendChatMessage = { msg -> viewModel.sendTableChatMessage(msg) },
                    onToggleVoiceMuted = { viewModel.toggleVoiceMuted() },
                    onExitTable = {
                        viewModel.exitTable()
                    },
                )
            }
            currentWaitingRoom != null && currentWaitingRoom.status == "waiting" -> {
                WaitingRoomScreen(
                    room = currentWaitingRoom,
                    currentUserId = currentUserId,
                    friends = friendsList,
                    outgoingInvites = outgoingInvites,
                    isBusy = isBusy,
                    onInviteFriend = { friend -> viewModel.inviteFriendToActiveRoom(friend) },
                    onStartMatch = { viewModel.startOnlineRoomMatch() },
                    onLeaveRoom = { viewModel.leaveWaitingRoom {} },
                )
            }
            else -> {
                when (currentDest) {
                    HomeScreenDest.LOBBY -> LobbyScreen(
                        profileState = profileState,
                        publicRoomsState = publicRoomsState,
                        myRoomsState = myRoomsState,
                        incomingInvites = incomingInvites,
                        onlineFriendsCount = onlineFriendsCount,
                        aiCoachState = aiCoachState,
                        isBusy = isBusy,
                        selectedTableBackground = selectedTableBackground,
                        ownedBackgrounds = ownedBackgrounds,
                        selectedCardDeck = selectedCardDeck,
                        ownedDecks = ownedDecks,
                        isSubscribed = isSubscribed,
                        remainingFreeSessionsToday = remainingFreeSessionsToday,
                        billingSnapshot = billingSnapshot,
                        avatarUrl = effectiveAvatarUrl,
                        googlePhotoUrl = initialAvatarUrl,
                        onPickGalleryPhoto = { uri ->
                            scope.launch {
                                val saved = AvatarStorage.savePickedImage(context, currentUserId, uri)
                                if (saved != null) {
                                    effectiveAvatarUrl = saved
                                    snackbarHostState.showSnackbar("تم تحديث صورتك الشخصية بنجاح")
                                } else {
                                    snackbarHostState.showSnackbar("تعذّر قراءة الصورة المختارة")
                                }
                            }
                        },
                        onSelectPresetAvatar = { presetKey ->
                            AvatarStorage.setPresetAvatar(context, currentUserId, presetKey)
                            effectiveAvatarUrl = presetKey
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تحديث صورتك الشخصية بنجاح")
                            }
                        },
                        onUseCartoonAvatar = {
                            AvatarStorage.setCartoonAvatar(context, currentUserId)
                            effectiveAvatarUrl = null
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تفعيل شخصية شدها الكلاسيكية")
                            }
                        },
                        onUseGoogleAvatar = {
                            AvatarStorage.setGoogleAvatar(context, currentUserId)
                            effectiveAvatarUrl = initialAvatarUrl
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تفعيل صورة حساب Google")
                            }
                        },
                        onSelectOrPurchaseBackground = handleSelectOrPurchaseBackground,
                        onSelectOrPurchaseDeck = { deck ->
                            val alreadyOwned = isSubscribed || deck in ownedDecks || deck.priceSar == 0
                            if (alreadyOwned) {
                                selectedCardDeckName = deck.name
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم تفعيل ${deck.title} على طاولة اللعب")
                                }
                            } else {
                                when (val launchResult = billingManager.launchDeckPurchase(activity, deck, currentUserId)) {
                                    BillingLaunchResult.LaunchedPlaySheet -> {
                                        selectedCardDeckName = deck.name
                                    }
                                    is BillingLaunchResult.PreviewFallback -> {
                                        previewOwnedDeckNames = (previewOwnedDeckNames + deck.name).distinct()
                                        selectedCardDeckName = deck.name
                                        scope.launch {
                                            snackbarHostState.showSnackbar(launchResult.reason)
                                        }
                                    }
                                }
                            }
                        },
                        onOpenSubscription = openSubscriptionSheet,
                        onStartSoloMatch = { target -> viewModel.startSoloMatch(target) },
                        onCreateOnlineRoom = { type, target ->
                            if (!isSubscribed && remainingFreeSessionsToday <= 0) {
                                openSubscriptionSheet(dailyLimitGatePrompt)
                            } else {
                                viewModel.createOnlineRoom(type, target) {
                                    localSessionsCreatedToday = DailySessionQuotaStorage.recordSessionCreated(context, currentUserId)
                                }
                            }
                        },
                        onJoinRoomCode = { code -> viewModel.joinOnlineRoom(code) {} },
                        onResumeRoom = { code -> viewModel.observeOnlineRoom(code) },
                        onOpenFriends = { currentDest = HomeScreenDest.FRIENDS },
                        onAcceptInvite = { invite -> viewModel.acceptTableInvite(invite) },
                        onDeclineInvite = { invite -> viewModel.declineTableInvite(invite) },
                        onOpenLeaderboard = { currentDest = HomeScreenDest.LEADERBOARD },
                        onOpenHistory = { currentDest = HomeScreenDest.HISTORY },
                        onOpenProfile = { currentDest = HomeScreenDest.PROFILE },
                        onOpenCalculator = { currentDest = HomeScreenDest.CALCULATOR },
                        onAskRulesQuestion = { q -> viewModel.askRulesAdvisor(q) },
                        onClearAiCoach = { viewModel.clearAiCoach() },
                        onSignOut = performSignOut,
                    )
                    HomeScreenDest.FRIENDS -> FriendsScreen(
                        profileState = profileState,
                        friendsState = friendsState,
                        leaderboardState = leaderboardState,
                        incomingInvites = incomingInvites,
                        outgoingInvites = outgoingInvites,
                        activeWaitingRoomCode = currentWaitingRoom?.takeIf { it.status == "waiting" }?.roomCode,
                        currentUserId = currentUserId,
                        isBusy = isBusy,
                        onBack = { currentDest = HomeScreenDest.LOBBY },
                        onAddFriendByPlayerId = { code, onDone ->
                            viewModel.addFriendByPlayerId(code, onDone)
                        },
                        onAddFriendProfile = { candidate -> viewModel.addFriendProfile(candidate) },
                        onRemoveFriend = { uid -> viewModel.removeFriend(uid) },
                        onInviteFriendToPrivateTable = { friend, target ->
                            val hasActiveWaiting = currentWaitingRoom?.status == "waiting"
                            if (!hasActiveWaiting && !isSubscribed && remainingFreeSessionsToday <= 0) {
                                openSubscriptionSheet(dailyLimitGatePrompt)
                            } else {
                                viewModel.inviteFriendToPrivateTable(
                                    friend = friend,
                                    targetScore = target,
                                    onNewRoomCreated = {
                                        localSessionsCreatedToday = DailySessionQuotaStorage.recordSessionCreated(context, currentUserId)
                                    },
                                    onRoomReady = {
                                        currentDest = HomeScreenDest.LOBBY
                                    },
                                )
                            }
                        },
                        onAcceptInvite = { invite ->
                            viewModel.acceptTableInvite(invite) {
                                currentDest = HomeScreenDest.LOBBY
                            }
                        },
                        onDeclineInvite = { invite -> viewModel.declineTableInvite(invite) },
                        onJoinRoomCode = { code ->
                            viewModel.joinOnlineRoom(code) {
                                currentDest = HomeScreenDest.LOBBY
                            }
                        },
                        onCreatePrivateRoom = { target ->
                            if (!isSubscribed && remainingFreeSessionsToday <= 0) {
                                openSubscriptionSheet(dailyLimitGatePrompt)
                            } else {
                                viewModel.createOnlineRoom("private", target) {
                                    localSessionsCreatedToday = DailySessionQuotaStorage.recordSessionCreated(context, currentUserId)
                                    currentDest = HomeScreenDest.LOBBY
                                }
                            }
                        },
                    )
                    HomeScreenDest.LEADERBOARD -> LeaderboardScreen(
                        leaderboardState = leaderboardState,
                        currentUserId = currentUserId,
                        onBack = { currentDest = HomeScreenDest.LOBBY },
                    )
                    HomeScreenDest.HISTORY -> HistoryScreen(
                        profileState = profileState,
                        historyState = historyState,
                        onBack = { currentDest = HomeScreenDest.LOBBY },
                    )
                    HomeScreenDest.PROFILE -> ProfileScreen(
                        profileState = profileState,
                        aiCoachState = aiCoachState,
                        isBusy = isBusy,
                        avatarUrl = effectiveAvatarUrl,
                        googlePhotoUrl = initialAvatarUrl,
                        selectedTableBackground = selectedTableBackground,
                        ownedBackgrounds = ownedBackgrounds,
                        billingSnapshot = billingSnapshot,
                        onSelectOrPurchaseBackground = handleSelectOrPurchaseBackground,
                        onPickGalleryPhoto = { uri ->
                            scope.launch {
                                val saved = AvatarStorage.savePickedImage(context, currentUserId, uri)
                                if (saved != null) {
                                    effectiveAvatarUrl = saved
                                    snackbarHostState.showSnackbar("تم تحديث صورتك الشخصية بنجاح")
                                } else {
                                    snackbarHostState.showSnackbar("تعذّر قراءة الصورة المختارة")
                                }
                            }
                        },
                        onSelectPresetAvatar = { presetKey ->
                            AvatarStorage.setPresetAvatar(context, currentUserId, presetKey)
                            effectiveAvatarUrl = presetKey
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تحديث صورتك الشخصية بنجاح")
                            }
                        },
                        onUseCartoonAvatar = {
                            AvatarStorage.setCartoonAvatar(context, currentUserId)
                            effectiveAvatarUrl = null
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تفعيل شخصية شدها الكلاسيكية")
                            }
                        },
                        onUseGoogleAvatar = {
                            AvatarStorage.setGoogleAvatar(context, currentUserId)
                            effectiveAvatarUrl = initialAvatarUrl
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تفعيل صورة حساب Google")
                            }
                        },
                        onBack = { currentDest = HomeScreenDest.LOBBY },
                        onOpenHistory = { currentDest = HomeScreenDest.HISTORY },
                        onAskRulesQuestion = { q -> viewModel.askRulesAdvisor(q) },
                        onClearAiCoach = { viewModel.clearAiCoach() },
                        onUpdateDisplayName = { name, onDone ->
                            viewModel.updateDisplayName(name, onDone)
                        },
                        onSignOut = performSignOut,
                    )
                    HomeScreenDest.CALCULATOR -> BalootCalculatorScreen(
                        onBack = { currentDest = HomeScreenDest.LOBBY },
                    )
                }
            }
        }

        if (showSubscriptionSheet) {
            SubscriptionSheet(
                isSubscribed = isSubscribed,
                activePlan = activeSubscriptionPlan,
                billingSnapshot = billingSnapshot,
                limitGateMessage = subscriptionLimitMessage,
                canCancelPreview = !billingSnapshot.isSubscribedFromPlay && previewSubscribedPlan != null,
                onSelectPlan = handleSelectSubscriptionPlan,
                onCancelPreview = {
                    previewSubscribedPlanName = null
                    showSubscriptionSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar("تم إنهاء معاينة اشتراك شدها بلس")
                    }
                },
                onDismiss = {
                    showSubscriptionSheet = false
                    subscriptionLimitMessage = null
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
