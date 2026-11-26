package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.TableChatMessage
import com.example.game.BalootCard
import com.example.game.BalootEngine
import com.example.game.BalootSeat
import com.example.game.BalootTableState
import com.example.game.DetectedProject
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.game.PlayedCardEntry
import com.example.game.Suit
import com.example.ui.components.BalootCardBack
import com.example.ui.components.BalootCardView
import com.example.ui.components.CardDeckStyle
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.TableArenaBackground
import com.example.ui.components.TableBackgroundStyle
import com.example.ui.components.WinkingSmileyIcon
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.EmeraldFeltLight
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaSurfaceGray
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowRich
import com.example.ui.theme.SuitRed
import com.example.ui.theme.TealAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun GameTableScreen(
    table: BalootTableState,
    currentUserId: String,
    seatBubbles: Map<Int, SeatBubble>,
    chatMessages: List<TableChatMessage>,
    lastEloDelta: Int?,
    turnRemainingSeconds: Int? = null,
    isAutoPlayStandby: Boolean = false,
    pendingProjectsToDeclare: List<DetectedProject> = emptyList(),
    voiceCues: Flow<TableVoiceCue> = emptyFlow(),
    isVoiceMuted: Boolean = false,
    tableBackground: TableBackgroundStyle = TableBackgroundStyle.CLASSIC_WHITE,
    cardDeckStyle: CardDeckStyle = CardDeckStyle.CLASSIC,
    myAvatarUrl: String? = null,
    onBid: (BalootEngine.BotBidChoice, Suit?, String?) -> Unit,
    onDoubleDecision: (Boolean) -> Unit,
    onPlayCard: (BalootCard) -> Unit,
    onDeclareProjects: (List<DetectedProject>) -> Unit = {},
    onSkipProjects: () -> Unit = {},
    onNextRound: () -> Unit,
    onSendPhrase: (String) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onToggleVoiceMuted: () -> Unit = {},
    onExitTable: () -> Unit,
) {
    val context = LocalContext.current
    val voiceSpeaker = remember(context) { SaudiVoiceSpeaker(context) }

    DisposableEffect(voiceSpeaker) {
        onDispose { voiceSpeaker.release() }
    }

    LaunchedEffect(voiceSpeaker, isVoiceMuted) {
        voiceSpeaker.isMuted = isVoiceMuted
    }

    LaunchedEffect(voiceSpeaker, voiceCues) {
        voiceCues.collect { cue ->
            if (!isVoiceMuted) {
                voiceSpeaker.speak(
                    phrase = cue.phrase,
                    seatIndex = cue.seatIndex,
                    queueAdd = cue.queueAdd,
                )
            }
        }
    }

    var showExitConfirm by remember { mutableStateOf(false) }
    var showQuickChat by remember { mutableStateOf(false) }
    var showTableChatSheet by remember { mutableStateOf(false) }
    var seenMessageCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(showTableChatSheet, chatMessages.size) {
        if (showTableChatSheet) {
            seenMessageCount = chatMessages.size
        }
    }
    val unreadChatCount = if (showTableChatSheet) 0 else (chatMessages.size - seenMessageCount).coerceAtLeast(0)

    BackHandler {
        if (table.phase == GamePhase.MATCH_FINISHED) {
            onExitTable()
        } else {
            showExitConfirm = true
        }
    }

    val mySeat = table.mySeatIndex
    val rightSeat = (mySeat + 1) % 4
    val topSeat = (mySeat + 2) % 4
    val leftSeat = (mySeat + 3) % 4
    val myTeam = mySeat % 2
    val ourScore = if (myTeam == 0) table.usScore else table.themScore
    val oppScore = if (myTeam == 0) table.themScore else table.usScore
    val ourProjects = if (myTeam == 0) table.usProjects else table.themProjects
    val oppProjects = if (myTeam == 0) table.themProjects else table.usProjects

    val isLightTable = tableBackground.isLightBackground

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        TableArenaBackground(
            style = tableBackground,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 1. TOP CURVED BLUE HEADER + HANGING SCOREBOARD PILL ("لنا" / "لهم") (Matches UX #3)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Cobalt Blue Curved Top Bar
                Surface(
                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
                    color = ShaddahaBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = {
                                if (table.phase == GamePhase.MATCH_FINISHED) onExitTable()
                                else showExitConfirm = true
                            },
                            modifier = Modifier.testTag("exit_table_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                contentDescription = "خروج من الطاولة",
                                tint = ShaddahaWhite,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ShaddahaWhite.copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = "${contractBadgeText(table)} • الهدف ${table.targetScore}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShaddahaWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleVoiceMuted,
                                modifier = Modifier.testTag("table_voice_toggle_button"),
                            ) {
                                Icon(
                                    imageVector = if (isVoiceMuted) {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    },
                                    contentDescription = if (isVoiceMuted) "تشغيل الأصوات" else "كتم الأصوات",
                                    tint = if (isVoiceMuted) ShaddahaWhite.copy(alpha = 0.6f) else ShaddahaWhite,
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showTableChatSheet = true },
                                    modifier = Modifier.testTag("table_chat_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubble,
                                        contentDescription = "دردشة الطاولة",
                                        tint = ShaddahaWhite,
                                    )
                                }
                                if (unreadChatCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 8.dp, end = 8.dp)
                                            .size(10.dp)
                                            .background(ShaddahaYellow, CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }

                // Hanging Scoreboard Card ("لنا" vs "لهم") with red score pills
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ShaddahaSurfaceGray,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(2f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "لنا",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkMuted,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ShaddahaRed,
                            ) {
                                Text(
                                    text = "$ourScore",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                    ),
                                    color = ShaddahaWhite,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "لهم",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkMuted,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ShaddahaRed,
                            ) {
                                Text(
                                    text = "$oppScore",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                    ),
                                    color = ShaddahaWhite,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 2. CENTER PLAYING TABLE AREA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 10-Second Turn Countdown Pill (visible for every active player's turn)
                val activeSeatName = table.seats.getOrNull(table.turnIndex)?.name.orEmpty()
                if (turnRemainingSeconds != null) {
                    TurnCountdownPill(
                        remainingSeconds = turnRemainingSeconds,
                        isMyTurn = table.turnIndex == mySeat,
                        activePlayerName = activeSeatName,
                        isBiddingPhase = table.phase == GamePhase.BIDDING_R1 || table.phase == GamePhase.BIDDING_R2,
                    )
                }

                // Top Seat (Partner)
                SeatHeaderBadge(
                    seat = table.seats[topSeat],
                    isTurn = table.turnIndex == topSeat,
                    isDealer = table.dealerIndex == topSeat,
                    cardCount = table.hands[topSeat]?.size ?: 0,
                    bubble = seatBubbles[topSeat],
                    showCardBackFan = true,
                    lightSurface = isLightTable,
                    cardDeckStyle = cardDeckStyle,
                )

                // Middle Row: Left Opponent + Center Open Card/Trick + Right Opponent (Spatial LTR)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            modifier = Modifier.width(82.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SeatHeaderBadge(
                                seat = table.seats[leftSeat],
                                isTurn = table.turnIndex == leftSeat,
                                isDealer = table.dealerIndex == leftSeat,
                                cardCount = table.hands[leftSeat]?.size ?: 0,
                                bubble = seatBubbles[leftSeat],
                                showCardBackFan = false,
                                lightSurface = isLightTable,
                                cardDeckStyle = cardDeckStyle,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CenterTableArea(
                                table = table,
                                mySeat = mySeat,
                                leftSeat = leftSeat,
                                topSeat = topSeat,
                                rightSeat = rightSeat,
                                lightSurface = isLightTable,
                                cardDeckStyle = cardDeckStyle,
                            )
                        }

                        Box(
                            modifier = Modifier.width(82.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SeatHeaderBadge(
                                seat = table.seats[rightSeat],
                                isTurn = table.turnIndex == rightSeat,
                                isDealer = table.dealerIndex == rightSeat,
                                cardCount = table.hands[rightSeat]?.size ?: 0,
                                bubble = seatBubbles[rightSeat],
                                showCardBackFan = false,
                                lightSurface = isLightTable,
                                cardDeckStyle = cardDeckStyle,
                            )
                        }
                    }
                }

                // Projects + Status Text + Quick Chat Strip
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (ourProjects.isNotEmpty() || oppProjects.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 3.dp),
                        ) {
                            if (ourProjects.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isLightTable) TealAccent.copy(alpha = 0.16f) else ShaddahaWhite.copy(alpha = 0.94f),
                                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.45f)),
                                ) {
                                    Text(
                                        text = "مشاريعنا: ${ourProjects.joinToString("، ")}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TealAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            if (oppProjects.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isLightTable) ShaddahaYellow.copy(alpha = 0.35f) else ShaddahaYellow.copy(alpha = 0.94f),
                                    border = BorderStroke(1.dp, ShaddahaYellowRich.copy(alpha = 0.6f)),
                                ) {
                                    Text(
                                        text = "مشاريعهم: ${oppProjects.joinToString("، ")}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = InkDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLightTable) ShaddahaSurfaceSoft else ShaddahaWhite.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, OutlineSoft),
                    ) {
                        Text(
                            text = table.lastActionText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = InkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }

                    if (isAutoPlayStandby) {
                        Spacer(modifier = Modifier.height(4.dp))
                        StandbyWaitingBanner()
                    } else if (pendingProjectsToDeclare.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        ProjectDeclarationCard(
                            projects = pendingProjectsToDeclare,
                            mode = table.mode,
                            onDeclare = onDeclareProjects,
                            onSkip = onSkipProjects,
                        )
                    }

                    AnimatedVisibility(
                        visible = showQuickChat,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BalootEngine.quickReactionEmojis.forEach { emoji ->
                                Surface(
                                    onClick = {
                                        onSendPhrase(emoji)
                                        showQuickChat = false
                                    },
                                    shape = CircleShape,
                                    color = ShaddahaWhite,
                                    border = BorderStroke(1.dp, OutlineSoft),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = emoji,
                                            fontSize = 24.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Player Hand Cards right above Bottom Blue Dock
                BottomPlayerHand(
                    table = table,
                    mySeat = mySeat,
                    isAutoPlayStandby = isAutoPlayStandby,
                    bubble = seatBubbles[mySeat],
                    cardDeckStyle = cardDeckStyle,
                    onPlayCard = onPlayCard,
                )
            }

            // 3. BOTTOM CURVED BLUE DOCK + AVATAR/NAME PILL + تعابير + دردشة + 4 WHITE ACTION BUTTONS (Matches UX #3)
            BottomBlueDock(
                table = table,
                mySeat = mySeat,
                isAutoPlayStandby = isAutoPlayStandby,
                myAvatarUrl = myAvatarUrl,
                unreadChatCount = unreadChatCount,
                onToggleReactions = { showQuickChat = !showQuickChat },
                onOpenChat = { showTableChatSheet = true },
                onBid = onBid,
                onDoubleDecision = onDoubleDecision,
            )
        }

        // Round Summary Overlay
        if (table.phase == GamePhase.ROUND_SUMMARY) {
            RoundSummaryOverlay(
                table = table,
                ourScore = ourScore,
                oppScore = oppScore,
                isHostOrSolo = !table.isOnline || table.hostId == currentUserId,
                onNextRound = onNextRound,
            )
        }

        // Match Finished Overlay
        if (table.phase == GamePhase.MATCH_FINISHED) {
            MatchFinishedOverlay(
                didWin = table.winnerTeam == myTeam,
                ourScore = ourScore,
                oppScore = oppScore,
                eloDelta = lastEloDelta,
                onExit = onExitTable,
            )
        }
    }

    if (showTableChatSheet) {
        TableChatBottomSheet(
            isOnline = table.isOnline,
            currentUserId = currentUserId,
            messages = chatMessages,
            onSendMessage = onSendChatMessage,
            onDismiss = { showTableChatSheet = false },
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("مغادرة الطاولة") },
            text = { Text("هل تريد الخروج من المباراة الحالية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onExitTable()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShaddahaRed),
                ) {
                    Text("خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("البقاء")
                }
            },
        )
    }
}

@Composable
private fun BottomBlueDock(
    table: BalootTableState,
    mySeat: Int,
    isAutoPlayStandby: Boolean,
    myAvatarUrl: String?,
    unreadChatCount: Int,
    onToggleReactions: () -> Unit,
    onOpenChat: () -> Unit,
    onBid: (BalootEngine.BotBidChoice, Suit?, String?) -> Unit,
    onDoubleDecision: (Boolean) -> Unit,
) {
    val mySeatObj = table.seats[mySeat]
    var showR2Suits by remember(table.phase, table.turnIndex) { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Blue Curved Bottom Dock Body
        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = ShaddahaBlue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 34.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 46.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // R2 Suit picker bar if user clicked "حكم" in Round 2
                AnimatedVisibility(
                    visible = !isAutoPlayStandby &&
                        showR2Suits &&
                        table.phase == GamePhase.BIDDING_R2 &&
                        table.turnIndex == mySeat,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Suit.entries
                            .filter { it != table.openCard?.suit }
                            .forEach { suit ->
                                Surface(
                                    onClick = {
                                        showR2Suits = false
                                        onBid(BalootEngine.BotBidChoice.HOKM_SUIT, suit, null)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ShaddahaWhite,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "حكم ${suit.symbol}",
                                            color = if (suit.isRed) SuitRed else InkDark,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                            }
                    }
                }

                // Bottom White Action Buttons Row: Standby indicator OR R1/R2 bidding OR Doubling OR Prompt
                if (isAutoPlayStandby) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ShaddahaYellow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dock_standby_status"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = null,
                                tint = InkDark,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضع الانتظار — يلعب تلقائياً حتى نهاية الجولة",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else if (table.turnIndex == mySeat && (table.phase == GamePhase.BIDDING_R1 || table.phase == GamePhase.BIDDING_R2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DockWhiteActionButton(
                            label = "صن",
                            modifier = Modifier
                                .weight(1f)
                                .testTag(if (table.phase == GamePhase.BIDDING_R1) "bid_sun_button" else "bid_r2_sun_button"),
                            onClick = { onBid(BalootEngine.BotBidChoice.SUN, null, null) },
                        )
                        DockWhiteActionButton(
                            label = if (table.phase == GamePhase.BIDDING_R1) {
                                "حكم ${table.openCard?.suit?.symbol.orEmpty()}"
                            } else {
                                "حكم"
                            },
                            enabled = table.phase == GamePhase.BIDDING_R2 || table.mode == GameMode.NONE,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bid_hokm_button"),
                            onClick = {
                                if (table.phase == GamePhase.BIDDING_R1) {
                                    onBid(BalootEngine.BotBidChoice.HOKM_OPEN, table.openCard?.suit, null)
                                } else {
                                    showR2Suits = !showR2Suits
                                }
                            },
                        )
                        if (table.phase == GamePhase.BIDDING_R1) {
                            DockWhiteActionButton(
                                label = "بس",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bid_pass_button"),
                                onClick = { onBid(BalootEngine.BotBidChoice.PASS, null, "بس") },
                            )
                        } else {
                            DockWhiteActionButton(
                                label = "ولا",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bid_r2_pass_button"),
                                onClick = { onBid(BalootEngine.BotBidChoice.PASS, null, "ولا") },
                            )
                        }
                    }
                } else if (table.turnIndex == mySeat && table.phase == GamePhase.DOUBLING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DockWhiteActionButton(
                            label = "دبل (×2)",
                            textColor = ShaddahaRed,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("double_yes_button"),
                            onClick = { onDoubleDecision(true) },
                        )
                        DockWhiteActionButton(
                            label = "لعب",
                            modifier = Modifier
                                .weight(1f)
                                .testTag("double_no_button"),
                            onClick = { onDoubleDecision(false) },
                        )
                    }
                } else {
                    // During card play or waiting for another player's turn
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ShaddahaWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val turnName = table.seats.getOrNull(table.turnIndex)?.name ?: ""
                            val promptText = when {
                                table.phase == GamePhase.PLAYING && table.turnIndex == mySeat ->
                                    "دورك الآن — اختر ورقة من يدك للعب"
                                table.phase == GamePhase.PLAYING ->
                                    "بانتظار لعب $turnName..."
                                else ->
                                    "دور $turnName في ${contractBadgeText(table)}..."
                            }
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ShaddahaBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // Overlapping Row: Player Name Pill + Avatar | تعابير (Yellow) | دردشة (Gray)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Player Avatar + Yellow Name Pill
            Box(contentAlignment = Alignment.CenterStart) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ShaddahaYellowRich,
                    shadowElevation = 3.dp,
                    modifier = Modifier.padding(start = 24.dp),
                ) {
                    Text(
                        text = mySeatObj.name.take(14),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 34.dp, end = 16.dp, top = 7.dp, bottom = 7.dp),
                    )
                }
                PlayerAvatar(
                    name = mySeatObj.name,
                    avatarUrl = myAvatarUrl,
                    size = 54.dp,
                    preferCartoonAvatar = myAvatarUrl.isNullOrBlank(),
                    borderColorOverride = ShaddahaWhite,
                )
            }

            // تعابير (White) & دردشة (Gray) Square Tiles
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DockSmileySquareButton(
                    label = "تعابير",
                    backgroundColor = ShaddahaWhite,
                    iconTint = ShaddahaBlue,
                    labelColor = InkDark,
                    modifier = Modifier.testTag("quick_chat_toggle_button"),
                    onClick = onToggleReactions,
                )
                Box {
                    DockSmileySquareButton(
                        label = "دردشة",
                        backgroundColor = ShaddahaSurfaceGray,
                        iconTint = InkDark,
                        labelColor = InkMuted,
                        modifier = Modifier.testTag("dock_table_chat_button"),
                        onClick = onOpenChat,
                    )
                    if (unreadChatCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(10.dp)
                                .background(ShaddahaRed, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockWhiteActionButton(
    label: String,
    modifier: Modifier = Modifier,
    textColor: Color = ShaddahaBlue,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) ShaddahaWhite else ShaddahaWhite.copy(alpha = 0.45f),
        shadowElevation = 2.dp,
        modifier = modifier.height(50.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                ),
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DockSmileySquareButton(
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = if (backgroundColor == ShaddahaWhite) BorderStroke(1.dp, OutlineSoft) else null,
        shadowElevation = 3.dp,
        modifier = modifier.size(width = 64.dp, height = 64.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            WinkingSmileyIcon(
                tint = iconTint,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = labelColor,
                maxLines = 1,
            )
        }
    }
}

private fun contractBadgeText(table: BalootTableState): String {
    if (table.mode == GameMode.NONE) return "المشترى"
    val modeStr = if (table.mode == GameMode.SUN) "صن" else "حكم ${table.trumpSuit?.symbol.orEmpty()}"
    val mult = when (table.bidLevel) {
        2 -> " • دبل"
        3 -> " • ثري"
        4 -> " • قهوة"
        else -> ""
    }
    return "$modeStr$mult"
}

@Composable
private fun SeatHeaderBadge(
    seat: BalootSeat,
    isTurn: Boolean,
    isDealer: Boolean,
    cardCount: Int,
    bubble: SeatBubble?,
    showCardBackFan: Boolean,
    lightSurface: Boolean,
    cardDeckStyle: CardDeckStyle,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (bubble != null) {
            val isEmojiBubble = bubble.message in BalootEngine.quickReactionEmojis
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isEmojiBubble) ShaddahaWhite else ShaddahaYellow,
                border = BorderStroke(
                    1.dp,
                    if (isEmojiBubble) OutlineSoft else ShaddahaBlue.copy(alpha = 0.35f),
                ),
                shadowElevation = if (isEmojiBubble) 2.dp else 0.dp,
            ) {
                Text(
                    text = bubble.message,
                    style = if (isEmojiBubble) {
                        MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
                    } else {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    },
                    color = InkDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlayerAvatar(
                name = seat.name,
                avatarUrl = null,
                size = 38.dp,
                isBot = seat.isBot,
                isHighlighted = isTurn,
                preferCartoonAvatar = !seat.isBot,
            )
            if (showCardBackFan && cardCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
                    repeat(cardCount.coerceAtMost(5)) {
                        BalootCardBack(width = 24.dp, height = 34.dp, deckStyle = cardDeckStyle)
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = when {
                isTurn -> ShaddahaYellow
                lightSurface -> ShaddahaSurfaceGray
                else -> ShaddahaWhite.copy(alpha = 0.9f)
            },
            border = if (isTurn) BorderStroke(1.5.dp, ShaddahaBlue) else null,
        ) {
            Text(
                text = buildString {
                    append(seat.name.take(10))
                    if (isDealer) append(" ★")
                    if (!showCardBackFan && cardCount > 0) append(" ($cardCount)")
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isTurn) ShaddahaBlue else InkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CenterTableArea(
    table: BalootTableState,
    mySeat: Int,
    leftSeat: Int,
    topSeat: Int,
    rightSeat: Int,
    lightSurface: Boolean,
    cardDeckStyle: CardDeckStyle,
) {
    if (table.phase == GamePhase.BIDDING_R1 || table.phase == GamePhase.BIDDING_R2) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            table.openCard?.let { open ->
                BalootCardView(card = open, width = 76.dp, height = 110.dp, deckStyle = cardDeckStyle)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (lightSurface) ShaddahaBlueSoft else ShaddahaWhite.copy(alpha = 0.9f),
            ) {
                Text(
                    text = if (table.phase == GamePhase.BIDDING_R1) "الدور الأول للمشترى" else "الدور الثاني للمشترى",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ShaddahaBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        return
    }

    val cardsToShow = if (table.tableCards.isNotEmpty()) table.tableCards else table.lastTrickCards
    val isPrevious = table.tableCards.isEmpty() && table.lastTrickCards.isNotEmpty()
    val cardBySeat: Map<Int, PlayedCardEntry> = cardsToShow.associateBy { it.seatIndex }

    Box(
        modifier = Modifier.size(196.dp),
        contentAlignment = Alignment.Center,
    ) {
        val trickAlpha = if (isPrevious) 0.55f else 1f
        Box(modifier = Modifier.fillMaxSize().alpha(trickAlpha)) {
            cardBySeat[topSeat]?.let { entry ->
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) {
                    key("top_${entry.card.code}_$isPrevious") {
                        AnimatedThrownTableCard(
                            entry = entry,
                            origin = TableThrowOrigin.TOP,
                            skipAnimation = isPrevious,
                            cardDeckStyle = cardDeckStyle,
                        )
                    }
                }
            }
            cardBySeat[leftSeat]?.let { entry ->
                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) {
                    key("left_${entry.card.code}_$isPrevious") {
                        AnimatedThrownTableCard(
                            entry = entry,
                            origin = TableThrowOrigin.LEFT,
                            skipAnimation = isPrevious,
                            cardDeckStyle = cardDeckStyle,
                        )
                    }
                }
            }
            cardBySeat[rightSeat]?.let { entry ->
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) {
                    key("right_${entry.card.code}_$isPrevious") {
                        AnimatedThrownTableCard(
                            entry = entry,
                            origin = TableThrowOrigin.RIGHT,
                            skipAnimation = isPrevious,
                            cardDeckStyle = cardDeckStyle,
                        )
                    }
                }
            }
            cardBySeat[mySeat]?.let { entry ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
                    key("bottom_${entry.card.code}_$isPrevious") {
                        AnimatedThrownTableCard(
                            entry = entry,
                            origin = TableThrowOrigin.BOTTOM,
                            skipAnimation = isPrevious,
                            cardDeckStyle = cardDeckStyle,
                        )
                    }
                }
            }
        }
        if (isPrevious) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ShaddahaSurfaceGray,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = "اللفة السابقة",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

private enum class TableThrowOrigin { TOP, BOTTOM, LEFT, RIGHT }

@Composable
private fun AnimatedThrownTableCard(
    entry: PlayedCardEntry,
    origin: TableThrowOrigin,
    skipAnimation: Boolean,
    cardDeckStyle: CardDeckStyle,
) {
    val density = LocalDensity.current
    val restingAngle = remember(entry.card.code, entry.seatIndex) {
        val seed = (entry.card.code.hashCode() + entry.seatIndex * 31) % 13
        (seed - 6).toFloat() * 0.9f
    }
    val startOffsetDpX = when (origin) {
        TableThrowOrigin.LEFT -> -125f
        TableThrowOrigin.RIGHT -> 125f
        TableThrowOrigin.BOTTOM -> 14f
        TableThrowOrigin.TOP -> -14f
    }
    val startOffsetDpY = when (origin) {
        TableThrowOrigin.BOTTOM -> 135f
        TableThrowOrigin.TOP -> -125f
        TableThrowOrigin.LEFT -> 16f
        TableThrowOrigin.RIGHT -> -16f
    }
    val startRotation = when (origin) {
        TableThrowOrigin.BOTTOM -> -22f
        TableThrowOrigin.TOP -> 20f
        TableThrowOrigin.LEFT -> -26f
        TableThrowOrigin.RIGHT -> 26f
    }

    val progress = remember { Animatable(if (skipAnimation) 1f else 0f) }

    LaunchedEffect(entry.card.code, entry.seatIndex, skipAnimation) {
        if (!skipAnimation) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.74f,
                    stiffness = 290f,
                ),
            )
        }
    }

    val t = progress.value
    val eased = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))
    val offsetX = with(density) { (startOffsetDpX * (1f - eased)).dp.toPx() }
    val offsetY = with(density) { (startOffsetDpY * (1f - eased)).dp.toPx() }
    val currentScale = 1.24f - (0.24f * eased)
    val currentRotation = startRotation + (restingAngle - startRotation) * eased

    BalootCardView(
        card = entry.card,
        width = 58.dp,
        height = 82.dp,
        deckStyle = cardDeckStyle,
        modifier = Modifier.graphicsLayer {
            translationX = offsetX
            translationY = offsetY
            scaleX = currentScale
            scaleY = currentScale
            rotationZ = currentRotation
            alpha = (0.35f + 0.65f * t.coerceIn(0f, 1f))
        },
    )
}

@Composable
private fun TurnCountdownPill(
    remainingSeconds: Int,
    isMyTurn: Boolean,
    activePlayerName: String,
    isBiddingPhase: Boolean,
) {
    val clamped = remainingSeconds.coerceIn(0, 10)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 10f,
        animationSpec = tween(durationMillis = 350),
        label = "turnTimerProgress",
    )
    val isUrgent = clamped <= 3
    val isWarning = clamped in 4..5
    val containerColor by animateColorAsState(
        targetValue = when {
            isUrgent -> ShaddahaRed
            isWarning -> ShaddahaYellow
            else -> ShaddahaWhite.copy(alpha = 0.96f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "turnTimerBg",
    )
    val contentColor = when {
        isUrgent -> ShaddahaWhite
        isWarning -> InkDark
        else -> ShaddahaBlue
    }
    val borderColor = when {
        isUrgent -> ShaddahaWhite.copy(alpha = 0.4f)
        isWarning -> ShaddahaYellowRich
        else -> ShaddahaBlue.copy(alpha = 0.35f)
    }

    val label = when {
        isMyTurn && isBiddingPhase -> "دورك في المشترى"
        isMyTurn -> "دورك الآن"
        isBiddingPhase -> "مشترى ${activePlayerName.take(10)}"
        else -> "دور ${activePlayerName.take(10)}"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.5.dp, borderColor),
        shadowElevation = 3.dp,
        modifier = Modifier
            .padding(top = 2.dp, bottom = 2.dp)
            .testTag("turn_countdown_timer"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.22f),
                    strokeWidth = 3.dp,
                )
                Text(
                    text = "$clamped",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                    ),
                    color = contentColor,
                )
            }
            Text(
                text = "$label • $clamped ث",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StandbyWaitingBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ShaddahaYellow,
        border = BorderStroke(1.5.dp, ShaddahaBlue.copy(alpha = 0.35f)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .testTag("standby_waiting_banner"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = "وضع الانتظار",
                tint = ShaddahaBlue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "أنت في وضع الانتظار — يلعب المساعد الآلي عنك حتى نهاية الجولة",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = InkDark,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProjectDeclarationCard(
    projects: List<DetectedProject>,
    mode: GameMode,
    onDeclare: (List<DetectedProject>) -> Unit,
    onSkip: () -> Unit,
) {
    var selectedIndices by remember(projects) {
        mutableStateOf(projects.indices.toSet())
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaWhite.copy(alpha = 0.97f),
        border = BorderStroke(2.dp, ShaddahaYellowRich),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("project_declaration_card"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "المشاريع المتاحة",
                    tint = ShaddahaBlue,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "لديك مشاريع! اختر ما تريد إعلانه قبل اللعب:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                projects.forEachIndexed { index, project ->
                    val isSelected = index in selectedIndices
                    val abnat = BalootEngine.projectAbnat(project.description, mode)
                    Surface(
                        onClick = {
                            selectedIndices = if (isSelected) {
                                selectedIndices - index
                            } else {
                                selectedIndices + index
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ShaddahaBlue else ShaddahaSurfaceSoft,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) ShaddahaBlue else OutlineSoft,
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("project_option_${project.kind.name.lowercase()}_$index"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "محدد",
                                    tint = ShaddahaYellow,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = "${project.description} (+$abnat)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isSelected) ShaddahaWhite else InkDark,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val chosenProjects = projects.filterIndexed { idx, _ -> idx in selectedIndices }
                Button(
                    onClick = { onDeclare(chosenProjects) },
                    enabled = chosenProjects.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShaddahaBlue,
                        contentColor = ShaddahaWhite,
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("declare_projects_button"),
                ) {
                    Text(
                        text = if (chosenProjects.size == 1) {
                            "إعلان ${chosenProjects.first().kind.arabicName}"
                        } else {
                            "إعلان المختار (${chosenProjects.size})"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    )
                }
                Surface(
                    onClick = onSkip,
                    shape = RoundedCornerShape(12.dp),
                    color = ShaddahaSurfaceSoft,
                    border = BorderStroke(1.dp, OutlineSoft),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                        .testTag("skip_projects_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "بدون",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomPlayerHand(
    table: BalootTableState,
    mySeat: Int,
    isAutoPlayStandby: Boolean,
    bubble: SeatBubble?,
    cardDeckStyle: CardDeckStyle,
    onPlayCard: (BalootCard) -> Unit,
) {
    val myHand = table.hands[mySeat].orEmpty()
    val isMyPlayTurn = !isAutoPlayStandby && table.phase == GamePhase.PLAYING && table.turnIndex == mySeat
    val legalCards = if (isMyPlayTurn) {
        BalootEngine.getLegalCards(myHand, table.tableCards, table.mode, table.trumpSuit, mySeat)
    } else {
        myHand
    }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var selectedCard by remember(table.roundNumber, table.phase, table.turnIndex, isAutoPlayStandby) {
        mutableStateOf<BalootCard?>(null)
    }
    var throwingCard by remember(table.roundNumber, table.phase, table.turnIndex, isAutoPlayStandby) {
        mutableStateOf<BalootCard?>(null)
    }

    LaunchedEffect(myHand, isMyPlayTurn) {
        if (!isMyPlayTurn || (selectedCard != null && selectedCard !in legalCards)) {
            selectedCard = null
        }
    }

    val triggerThrow: (BalootCard) -> Unit = { card ->
        if (isMyPlayTurn && card in legalCards && throwingCard == null) {
            selectedCard = null
            throwingCard = card
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                delay(185L)
                onPlayCard(card)
                throwingCard = null
            }
        }
    }

    val handleCardTap: (BalootCard) -> Unit = { card ->
        if (isMyPlayTurn && card in legalCards && throwingCard == null) {
            if (selectedCard == card) {
                triggerThrow(card)
            } else {
                selectedCard = card
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bubble != null) {
            val isEmojiBubble = bubble.message in BalootEngine.quickReactionEmojis
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isEmojiBubble) ShaddahaWhite else ShaddahaYellow,
                border = BorderStroke(
                    1.dp,
                    if (isEmojiBubble) OutlineSoft else ShaddahaBlue.copy(alpha = 0.35f),
                ),
                shadowElevation = if (isEmojiBubble) 2.dp else 0.dp,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Text(
                    text = bubble.message,
                    style = if (isEmojiBubble) {
                        MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp)
                    } else {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    },
                    color = InkDark,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = isMyPlayTurn && selectedCard != null && throwingCard == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val activeSelected = selectedCard
            if (activeSelected != null) {
                Surface(
                    onClick = { triggerThrow(activeSelected) },
                    shape = RoundedCornerShape(50),
                    color = ShaddahaYellow,
                    border = BorderStroke(1.5.dp, ShaddahaBlue),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .testTag("confirm_throw_card_pill"),
                ) {
                    Text(
                        text = "اضغط الورقة مرة أخرى أو اسحبها لأعلى للرمي (${activeSelected.rank.label}${activeSelected.suit.symbol})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 2.dp)
                .alpha(if (isAutoPlayStandby) 0.65f else 1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val horizontalGap = 8.dp
            val cardWidth = ((maxWidth - 40.dp) / 4f).coerceIn(68.dp, 86.dp)
            val cardHeight = cardWidth * 1.24f
            val topRow = myHand.take(4)
            val bottomRow = myHand.drop(4)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val renderRow: @Composable (List<BalootCard>) -> Unit = { rowCards ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            rowCards.forEach { card ->
                                val isCardLegal = card in legalCards
                                val swipeModifier = if (isMyPlayTurn && isCardLegal) {
                                    Modifier.pointerInput(card.code, isMyPlayTurn, isCardLegal) {
                                        var cumulativeDragY = 0f
                                        detectVerticalDragGestures(
                                            onDragStart = { cumulativeDragY = 0f },
                                            onVerticalDrag = { _, dragAmount ->
                                                cumulativeDragY += dragAmount
                                                if (cumulativeDragY < -42f) {
                                                    cumulativeDragY = 0f
                                                    triggerThrow(card)
                                                }
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                                BalootCardView(
                                    card = card,
                                    modifier = swipeModifier,
                                    width = cardWidth,
                                    height = cardHeight,
                                    isLegal = isCardLegal,
                                    isPlayableTurn = isMyPlayTurn,
                                    isSelected = selectedCard == card,
                                    isThrowing = throwingCard == card,
                                    deckStyle = cardDeckStyle,
                                    onClick = if (isMyPlayTurn && isCardLegal) ({ handleCardTap(card) }) else null,
                                )
                            }
                        }
                    }

                    if (topRow.isNotEmpty()) {
                        renderRow(topRow)
                    }
                    if (bottomRow.isNotEmpty()) {
                        renderRow(bottomRow)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundSummaryOverlay(
    table: BalootTableState,
    ourScore: Int,
    oppScore: Int,
    isHostOrSolo: Boolean,
    onNextRound: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkDark.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = ShaddahaWhite,
            border = BorderStroke(2.dp, ShaddahaBlue),
            shadowElevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "انتهت الصكة رقم ${table.roundNumber}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = ShaddahaBlue,
                )
                Text(
                    text = table.lastActionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkDark,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("لنا", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                        Surface(shape = RoundedCornerShape(50), color = ShaddahaRed) {
                            Text(
                                text = "$ourScore",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShaddahaWhite,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("لهم", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                        Surface(shape = RoundedCornerShape(50), color = ShaddahaRed) {
                            Text(
                                text = "$oppScore",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShaddahaWhite,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (isHostOrSolo) {
                    Button(
                        onClick = onNextRound,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("next_round_button"),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الصكة التالية", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "بانتظار قيام المضيف بتوزيع الصكة التالية...",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchFinishedOverlay(
    didWin: Boolean,
    ourScore: Int,
    oppScore: Int,
    eloDelta: Int?,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkDark.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ShaddahaWhite,
            border = BorderStroke(2.5.dp, if (didWin) ShaddahaYellow else ShaddahaBlue),
            shadowElevation = 14.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(
                            if (didWin) ShaddahaYellow else ShaddahaBlueSoft,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (didWin) ShaddahaBlue else InkMuted,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Text(
                    text = if (didWin) "مبروك الفوز!" else "حظ أوفر في الجلسة القادمة",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (didWin) ShaddahaBlue else InkDark,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "النتيجة النهائية: لنا $ourScore — لهم $oppScore",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                )
                if (eloDelta != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (eloDelta >= 0) TealAccent.copy(alpha = 0.16f) else ShaddahaRed.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = if (eloDelta >= 0) "+$eloDelta نقطة تصنيف" else "$eloDelta نقطة تصنيف",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (eloDelta >= 0) TealAccent else ShaddahaRed,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
                Button(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("finish_match_exit_button"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("العودة للرئيسية", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableChatBottomSheet(
    isOnline: Boolean,
    currentUserId: String,
    messages: List<TableChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ShaddahaWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ShaddahaBlueSoft,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubble,
                                contentDescription = null,
                                tint = ShaddahaBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "دردشة الطاولة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                        )
                        Text(
                            text = if (isOnline) "محادثة فورية مع اللاعبين أثناء اللعب" else "رسائل الطاولة المباشرة",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_table_chat_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إغلاق الدردشة",
                        tint = InkMuted,
                    )
                }
            }

            // Messages List
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ShaddahaSurfaceSoft,
                border = BorderStroke(1.dp, OutlineSoft),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp, max = 290.dp),
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 190.dp)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubble,
                                contentDescription = null,
                                tint = InkMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(30.dp),
                            )
                            Text(
                                text = "لا توجد رسائل بعد، شارك اللاعبين بالسلام أو التعليق!",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = messages,
                            key = { msg ->
                                msg.id.ifBlank { "${msg.senderUid}_${msg.seatIndex}_${msg.text.hashCode()}" }
                            },
                        ) { msg ->
                            val isMine = msg.senderUid == currentUserId
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                            ) {
                                if (!isMine) {
                                    Text(
                                        text = msg.senderName.ifBlank { "لاعب" },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ShaddahaBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isMine) 14.dp else 4.dp,
                                        bottomEnd = if (isMine) 4.dp else 14.dp,
                                    ),
                                    color = if (isMine) ShaddahaBlue else ShaddahaWhite,
                                    border = if (isMine) null else BorderStroke(1.dp, OutlineSoft),
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMine) ShaddahaWhite else InkDark,
                                        modifier = Modifier
                                            .widthIn(max = 260.dp)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick 1-tap phrases strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BalootEngine.quickChatPhrases.forEach { phrase ->
                    Surface(
                        onClick = { onSendMessage(phrase) },
                        shape = RoundedCornerShape(50),
                        color = ShaddahaYellow.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, ShaddahaBlue.copy(alpha = 0.2f)),
                    ) {
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // Input Row
            val sendCurrent = {
                val clean = inputText.trim()
                if (clean.isNotEmpty()) {
                    onSendMessage(clean)
                    inputText = ""
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { if (it.length <= 200) inputText = it },
                    placeholder = { Text("اكتب رسالتك للاعبين...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendCurrent() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ShaddahaBlue,
                        unfocusedBorderColor = OutlineSoft,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("table_chat_input"),
                )
                Surface(
                    onClick = { sendCurrent() },
                    enabled = inputText.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (inputText.isNotBlank()) ShaddahaBlue else ShaddahaSurfaceGray,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("table_chat_send_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "إرسال الرسالة",
                            tint = if (inputText.isNotBlank()) ShaddahaWhite else InkMuted,
                        )
                    }
                }
            }
        }
    }
}

