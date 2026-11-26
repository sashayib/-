package com.example.ui

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ai.AiCoachUiState
import com.example.billing.BillingUiSnapshot
import com.example.billing.DailySessionQuotaStorage
import com.example.billing.ShaddahaProducts
import com.example.billing.SubscriptionPlan
import com.example.data.GameRoom
import com.example.data.PlayerProfile
import com.example.data.TableInvite
import com.example.data.UiState
import com.example.game.BalootCard
import com.example.game.Rank
import com.example.game.Suit
import com.example.ui.components.BalootCardBack
import com.example.ui.components.BalootCardView
import com.example.ui.components.BalootRulesDialog
import com.example.ui.components.CardDeckStyle
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.RankTierIcon
import com.example.ui.components.ShaddahaRankTier
import com.example.ui.components.TableBackgroundStyle
import com.example.ui.components.TableBackgroundThumbnail
import com.example.ui.components.YellowWavePatternBackground
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaCardGray
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaSurfaceGray
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowRich
import com.example.ui.theme.ShaddahaYellowSoft
import com.example.ui.theme.TealAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    profileState: UiState<PlayerProfile>,
    publicRoomsState: UiState<List<GameRoom>>,
    myRoomsState: UiState<List<GameRoom>>,
    incomingInvites: List<TableInvite>,
    onlineFriendsCount: Int,
    aiCoachState: AiCoachUiState,
    isBusy: Boolean,
    selectedTableBackground: TableBackgroundStyle,
    ownedBackgrounds: Set<TableBackgroundStyle>,
    selectedCardDeck: CardDeckStyle,
    ownedDecks: Set<CardDeckStyle>,
    isSubscribed: Boolean,
    remainingFreeSessionsToday: Int,
    billingSnapshot: BillingUiSnapshot,
    avatarUrl: String?,
    googlePhotoUrl: String?,
    onPickGalleryPhoto: (Uri) -> Unit,
    onSelectPresetAvatar: (String) -> Unit,
    onUseCartoonAvatar: () -> Unit,
    onUseGoogleAvatar: () -> Unit,
    onSelectOrPurchaseBackground: (TableBackgroundStyle) -> Unit,
    onSelectOrPurchaseDeck: (CardDeckStyle) -> Unit,
    onOpenSubscription: (limitGateMessage: String?) -> Unit,
    onStartSoloMatch: (Int) -> Unit,
    onCreateOnlineRoom: (roomType: String, targetScore: Int) -> Unit,
    onJoinRoomCode: (String) -> Unit,
    onResumeRoom: (String) -> Unit,
    onOpenFriends: () -> Unit,
    onAcceptInvite: (TableInvite) -> Unit,
    onDeclineInvite: (TableInvite) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenCalculator: () -> Unit,
    onAskRulesQuestion: (String) -> Unit,
    onClearAiCoach: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showPlayHubSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSoloDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }

    val profile = (profileState as? UiState.Success)?.data
    val myRooms = (myRoomsState as? UiState.Success)?.data.orEmpty()
    val publicRooms = (publicRoomsState as? UiState.Success)?.data.orEmpty()
    val currentRating = profile?.rating?.toInt() ?: 22
    val currentRankTier = ShaddahaRankTier.forRating(currentRating)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // TOP WHITE AREA (Matches UX #2 proportions)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 1. Centered Logo + 4 Quick Tiles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedShaddahaLogo(
                        modifier = Modifier
                            .height(92.dp)
                            .width(168.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Top 4 Quick Action Gray Tiles: الإشعارات | الاصدقاء | المتصدرين | المزيد
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuickTopTile(
                            label = "الإشعارات",
                            icon = Icons.Rounded.Notifications,
                            badgeCount = incomingInvites.size + myRooms.size + publicRooms.size,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tile_notifications"),
                            onClick = { showNotificationsSheet = true },
                        )
                        QuickTopTile(
                            label = if (onlineFriendsCount > 0) "الاصدقاء ($onlineFriendsCount)" else "الاصدقاء",
                            icon = Icons.Rounded.Groups,
                            badgeCount = incomingInvites.size,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tile_friends"),
                            onClick = onOpenFriends,
                        )
                        QuickTopTile(
                            label = "المتصدرين",
                            icon = Icons.Rounded.Star,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_leaderboard"),
                            onClick = onOpenLeaderboard,
                        )
                        QuickTopTile(
                            label = "المزيد",
                            icon = Icons.Rounded.MoreHoriz,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tile_more"),
                            onClick = { showMoreSheet = true },
                        )
                    }

                    if (incomingInvites.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val latestInvite = incomingInvites.first()
                        IncomingTableInviteCard(
                            invite = latestInvite,
                            isBusy = isBusy,
                            onAccept = { onAcceptInvite(latestInvite) },
                            onDecline = { onDeclineInvite(latestInvite) },
                            modifier = Modifier.testTag("lobby_incoming_invite_banner"),
                        )
                    }
                }

                // 2. Center Avatar + Overlapping Yellow Name Pill + Blue Rank Tier
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { onOpenProfile() }
                        .testTag("nav_profile"),
                ) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        PlayerAvatar(
                            name = profile?.displayName ?: "لاعب شدها",
                            avatarUrl = avatarUrl,
                            size = 136.dp,
                            preferCartoonAvatar = avatarUrl.isNullOrBlank(),
                            borderColorOverride = ShaddahaSurfaceGray,
                        )
                        // Yellow overlapping name pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ShaddahaYellowRich,
                            shadowElevation = 2.dp,
                            modifier = Modifier.offset(y = 14.dp),
                        ) {
                            Text(
                                text = profile?.displayName ?: "اسم الحساب",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                ),
                                color = InkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RankTierIcon(tier = currentRankTier, size = 20.dp)
                        Text(
                            text = currentRankTier.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                            ),
                            color = ShaddahaBlue,
                        )
                    }
                    val lobbyTotalMatches = (profile?.matchesPlayed ?: 0L).toInt()
                    val lobbyWins = (profile?.wins ?: 0L).toInt()
                    val lobbyWinRate = if (lobbyTotalMatches > 0) (lobbyWins * 100) / lobbyTotalMatches else 0
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        onClick = onOpenHistory,
                        shape = RoundedCornerShape(50),
                        color = ShaddahaBlueSoft,
                        border = BorderStroke(1.dp, ShaddahaBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("lobby_open_stats_pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = "إحصائياتي",
                                tint = ShaddahaBlue,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "إحصائياتي • $lobbyWins فوز ($lobbyWinRate%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ShaddahaBlue,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // BOTTOM CURVED YELLOW WAVE + OVERLAPPING "العب الآن" BUTTON (Matches UX #2)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Yellow wave body
                Surface(
                    shape = RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp),
                    color = ShaddahaYellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        YellowWavePatternBackground(modifier = Modifier.matchParentSize())

                        Column(
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 66.dp, bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            // Red "اشترك الآن" pill button (clean, without stars)
                            Surface(
                                onClick = { onOpenSubscription(null) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSubscribed) TealAccent else ShaddahaRed,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .testTag("subscribe_ranks_button"),
                            ) {
                                Text(
                                    text = if (isSubscribed) "مشترك شدها بلس ✓" else "اشترك الآن",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                    ),
                                    color = ShaddahaWhite,
                                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 10.dp),
                                )
                            }

                            // Bottom 4 White Square Tiles: مستشارك | المتجر | الحاسبة | ادع اصدقائك
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                QuickBottomWhiteTile(
                                    label = "مستشارك",
                                    icon = Icons.Rounded.ChatBubble,
                                    animDelayMillis = 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tile_chat_ai"),
                                    onClick = { showAiAdvisorDialog = true },
                                )
                                QuickBottomWhiteTile(
                                    label = "المتجر",
                                    icon = Icons.Rounded.Storefront,
                                    animDelayMillis = 240,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tile_store"),
                                    onClick = { showStoreDialog = true },
                                )
                                QuickBottomWhiteTile(
                                    label = "الحاسبة",
                                    icon = Icons.Rounded.Calculate,
                                    animDelayMillis = 480,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tile_calculator"),
                                    onClick = onOpenCalculator,
                                )
                                QuickBottomWhiteTile(
                                    label = "ادع اصدقائك",
                                    icon = Icons.Rounded.Campaign,
                                    animDelayMillis = 720,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tile_share"),
                                    onClick = { showShareDialog = true },
                                )
                            }
                        }
                    }
                }

                // Overlapping Hero Cobalt Blue "العب الآن" Button with subtle wavy motion
                WavyPlayNowButton(
                    enabled = !isBusy,
                    onClick = { showPlayHubSheet = true },
                    modifier = Modifier
                        .zIndex(2f)
                        .widthIn(min = 240.dp, max = 310.dp)
                        .fillMaxWidth(0.68f)
                        .height(62.dp)
                        .testTag("play_now_button"),
                )
            }
        }
    }

    // Play Hub Sheet ("العب الآن")
    if (showPlayHubSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayHubSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ShaddahaWhite,
        ) {
            PlayHubSheetContent(
                myRooms = myRooms,
                publicRoomsState = publicRoomsState,
                isSubscribed = isSubscribed,
                remainingFreeSessionsToday = remainingFreeSessionsToday,
                isBusy = isBusy,
                onCreateOnline = {
                    showPlayHubSheet = false
                    if (!isSubscribed && remainingFreeSessionsToday <= 0) {
                        onOpenSubscription(
                            "لديك جلستان مجانيتان فقط في اليوم للإنشاء. لإنشاء جلسات أكثر، اختر باقة اشتراك شدها بلس المناسبة لك.",
                        )
                    } else {
                        showCreateDialog = true
                    }
                },
                onOpenSubscription = {
                    showPlayHubSheet = false
                    onOpenSubscription(null)
                },
                onPlaySolo = {
                    showPlayHubSheet = false
                    showSoloDialog = true
                },
                onJoinCode = { code ->
                    showPlayHubSheet = false
                    onJoinRoomCode(code)
                },
                onResumeRoom = { code ->
                    showPlayHubSheet = false
                    onResumeRoom(code)
                },
            )
        }
    }

    // Notifications Sheet ("الإشعارات")
    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ShaddahaWhite,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "الإشعارات ودعوات الطاولات",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = InkDark,
                )
                if (incomingInvites.isEmpty() && myRooms.isEmpty() && publicRooms.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ShaddahaSurfaceSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "لا توجد إشعارات أو جلسات نشطة حالياً. اضغط \"العب الآن\" لبدء صكة جديدة!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                } else {
                    incomingInvites.forEach { invite ->
                        IncomingTableInviteCard(
                            invite = invite,
                            isBusy = isBusy,
                            onAccept = {
                                showNotificationsSheet = false
                                onAcceptInvite(invite)
                            },
                            onDecline = { onDeclineInvite(invite) },
                        )
                    }
                    myRooms.forEach { room ->
                        RoomListCard(
                            room = room,
                            actionLabel = if (room.status == "playing") "استئناف" else "دخول",
                            isBusy = isBusy,
                            onClick = {
                                showNotificationsSheet = false
                                onResumeRoom(room.roomCode)
                            },
                        )
                    }
                    publicRooms.take(4).forEach { room ->
                        RoomListCard(
                            room = room,
                            actionLabel = "انضمام",
                            isBusy = isBusy,
                            onClick = {
                                showNotificationsSheet = false
                                onJoinRoomCode(room.roomCode)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // More Menu Sheet ("المزيد")
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = ShaddahaWhite,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "القائمة والمزيد",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = InkDark,
                )
                MoreMenuRow(
                    icon = Icons.Rounded.PieChart,
                    title = "إحصائياتي وسجل المباريات",
                    subtitle = "الرسم البياني الدائري لنسبة الفوز، الانتصارات، وسجل الصكات",
                    testTag = "nav_history",
                    onClick = {
                        showMoreSheet = false
                        onOpenHistory()
                    },
                )
                MoreMenuRow(
                    icon = Icons.Rounded.Person,
                    title = "ملفي الشخصي والتصنيفات",
                    subtitle = "تعديل الاسم، استعراض الرتب الـ 10، وتقدم التصنيف",
                    testTag = "more_profile_button",
                    onClick = {
                        showMoreSheet = false
                        onOpenProfile()
                    },
                )
                MoreMenuRow(
                    icon = Icons.Rounded.PhotoCamera,
                    title = "تغيير الصورة الشخصية",
                    subtitle = "اختر صورة من الجهاز أو شخصية شدها",
                    testTag = "more_change_avatar_button",
                    onClick = {
                        showMoreSheet = false
                        showAvatarPickerDialog = true
                    },
                )
                MoreMenuRow(
                    icon = Icons.Rounded.MenuBook,
                    title = "دليل قوانين البلوت",
                    subtitle = "ترتيب الصن والحكم والمشاريع والكبوت",
                    testTag = "more_rules_button",
                    onClick = {
                        showMoreSheet = false
                        showRulesDialog = true
                    },
                )
                MoreMenuRow(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "تسجيل الخروج",
                    subtitle = "تبديل الحساب الحالي",
                    accent = ShaddahaRed,
                    testTag = "more_signout_button",
                    onClick = {
                        showMoreSheet = false
                        onSignOut()
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreateRoomDialog(
            isSubscribed = isSubscribed,
            remainingFreeSessionsToday = remainingFreeSessionsToday,
            onDismiss = { showCreateDialog = false },
            onConfirm = { roomType, targetScore ->
                showCreateDialog = false
                onCreateOnlineRoom(roomType, targetScore)
            },
        )
    }

    if (showSoloDialog) {
        SoloMatchDialog(
            onDismiss = { showSoloDialog = false },
            onStart = { targetScore ->
                showSoloDialog = false
                onStartSoloMatch(targetScore)
            },
        )
    }

    if (showAiAdvisorDialog) {
        AiRulesAdvisorDialog(
            aiCoachState = aiCoachState,
            onAskRulesQuestion = onAskRulesQuestion,
            onDismiss = {
                onClearAiCoach()
                showAiAdvisorDialog = false
            },
        )
    }

    if (showStoreDialog) {
        StoreSheet(
            selectedTableBackground = selectedTableBackground,
            ownedBackgrounds = ownedBackgrounds,
            selectedCardDeck = selectedCardDeck,
            ownedDecks = ownedDecks,
            isSubscribed = isSubscribed,
            billingSnapshot = billingSnapshot,
            onSelectOrPurchaseBackground = onSelectOrPurchaseBackground,
            onSelectOrPurchaseDeck = onSelectOrPurchaseDeck,
            onOpenSubscription = {
                showStoreDialog = false
                onOpenSubscription(null)
            },
            onDismiss = { showStoreDialog = false },
        )
    }

    if (showShareDialog) {
        ShareInviteDialog(
            playerName = profile?.displayName ?: "لاعب شدها",
            onDismiss = { showShareDialog = false },
        )
    }

    if (showAvatarPickerDialog) {
        AvatarPickerDialog(
            currentAvatarUrl = avatarUrl,
            googlePhotoUrl = googlePhotoUrl,
            onPickGalleryPhoto = { uri ->
                showAvatarPickerDialog = false
                onPickGalleryPhoto(uri)
            },
            onSelectPresetAvatar = { presetKey ->
                showAvatarPickerDialog = false
                onSelectPresetAvatar(presetKey)
            },
            onUseCartoonAvatar = {
                showAvatarPickerDialog = false
                onUseCartoonAvatar()
            },
            onUseGoogleAvatar = {
                showAvatarPickerDialog = false
                onUseGoogleAvatar()
            },
            onDismiss = { showAvatarPickerDialog = false },
        )
    }

    if (showRulesDialog) {
        BalootRulesDialog(onDismiss = { showRulesDialog = false })
    }
}

@Composable
private fun QuickTopTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ShaddahaCardGray)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ShaddahaYellowRich,
                modifier = Modifier.size(30.dp),
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ShaddahaRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badgeCount.coerceAtMost(9).toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShaddahaWhite,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AnimatedShaddahaLogo(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_vibrate")
    val vibrate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2100
                0f at 0
                -2.3f at 70
                2.3f at 140
                -1.9f at 210
                1.9f at 280
                -1.1f at 350
                0.9f at 420
                0f at 500
                0f at 2100
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "logo_vibrate_value",
    )

    Image(
        painter = painterResource(id = R.drawable.shaddaha_logo),
        contentDescription = "شعار شدها",
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            rotationZ = vibrate
            translationX = (vibrate * 0.7f).dp.toPx()
        },
    )
}

@Composable
private fun QuickBottomWhiteTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    animDelayMillis: Int = 0,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bottom_icon_$label")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(animDelayMillis),
        ),
        label = "icon_float_y",
    )
    val tilt by infiniteTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(animDelayMillis),
        ),
        label = "icon_tilt",
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(animDelayMillis),
        ),
        label = "icon_scale",
    )

    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaWhite,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ShaddahaYellowRich,
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        translationY = floatY.dp.toPx()
                        rotationZ = tilt
                        scaleX = scale
                        scaleY = scale
                    },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WavyPlayNowButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "play_now_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_phase",
    )
    val gentlePulse by infiniteTransition.animateFloat(
        initialValue = 0.986f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gentle_pulse",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        color = ShaddahaBlue,
        shadowElevation = 8.dp,
        modifier = modifier.graphicsLayer {
            scaleX = gentlePulse
            scaleY = gentlePulse
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val amp1 = h * 0.085f
                val base1 = h * 0.62f
                val wave1 = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 12f) {
                        val y = base1 + amp1 * sin((x / w) * 2f * PI.toFloat() + wavePhase)
                        lineTo(x, y)
                        x += 12f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = wave1,
                    color = Color(0xFF4B74FF).copy(alpha = 0.42f),
                    style = Fill,
                )

                val amp2 = h * 0.065f
                val base2 = h * 0.74f
                val wave2 = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 12f) {
                        val y = base2 + amp2 * sin((x / w) * 2.5f * PI.toFloat() - wavePhase * 1.2f)
                        lineTo(x, y)
                        x += 12f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = wave2,
                    color = ShaddahaWhite.copy(alpha = 0.14f),
                    style = Fill,
                )
            }
            Text(
                text = "العب الآن",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                ),
                color = ShaddahaWhite,
            )
        }
    }
}

@Composable
private fun PlayHubSheetContent(
    myRooms: List<GameRoom>,
    publicRoomsState: UiState<List<GameRoom>>,
    isSubscribed: Boolean,
    remainingFreeSessionsToday: Int,
    isBusy: Boolean,
    onCreateOnline: () -> Unit,
    onOpenSubscription: () -> Unit,
    onPlaySolo: () -> Unit,
    onJoinCode: (String) -> Unit,
    onResumeRoom: (String) -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "اختر طريقة اللعب",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = InkDark,
            )
        }
        item {
            val limitReached = !isSubscribed && remainingFreeSessionsToday <= 0
            Surface(
                onClick = if (limitReached) onOpenSubscription else ({}),
                enabled = limitReached,
                shape = RoundedCornerShape(14.dp),
                color = when {
                    isSubscribed -> TealAccent.copy(alpha = 0.12f)
                    limitReached -> ShaddahaRed.copy(alpha = 0.1f)
                    else -> ShaddahaYellowSoft
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isSubscribed -> TealAccent.copy(alpha = 0.4f)
                        limitReached -> ShaddahaRed.copy(alpha = 0.45f)
                        else -> ShaddahaYellowRich
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_session_quota_banner"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = when {
                            isSubscribed -> "اشتراك شدها بلس نشط — إنشاء جلسات غير محدود يومياً ✓"
                            limitReached -> "استهلكت جلستيك اليوم (0/${DailySessionQuotaStorage.FREE_DAILY_SESSION_LIMIT}) — اشترك للإنشاء بلا حدود"
                            else -> "المتبقي لك اليوم لإنشاء الجلسات: $remainingFreeSessionsToday من ${DailySessionQuotaStorage.FREE_DAILY_SESSION_LIMIT}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (limitReached) ShaddahaRed else InkDark,
                        modifier = Modifier.weight(1f),
                    )
                    if (limitReached) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ShaddahaRed,
                        ) {
                            Text(
                                text = "اشترك",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ShaddahaWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onCreateOnline,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    colors = if (!isSubscribed && remainingFreeSessionsToday <= 0) {
                        ButtonDefaults.buttonColors(containerColor = ShaddahaRed, contentColor = ShaddahaWhite)
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("create_online_room_button"),
                ) {
                    Icon(imageVector = Icons.Rounded.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isSubscribed && remainingFreeSessionsToday <= 0) "اشترك لإنشاء جلسة" else "إنشاء جلسة",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = onPlaySolo,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShaddahaYellow, contentColor = InkDark),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("play_solo_button"),
                ) {
                    Icon(imageVector = Icons.Rounded.SmartToy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("لعب سريع", fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { input ->
                        codeInput = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
                    },
                    label = { Text("رمز الجلسة (6 خانات)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onJoinCode(codeInput) },
                    enabled = codeInput.length == 6 && !isBusy,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("دخول")
                }
            }
        }
        if (myRooms.isNotEmpty()) {
            item {
                Text(
                    text = "جلساتك النشطة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ShaddahaBlue,
                )
            }
            items(myRooms, key = { "hub_my_${it.roomCode}" }) { room ->
                RoomListCard(
                    room = room,
                    actionLabel = if (room.status == "playing") "استئناف" else "دخول",
                    isBusy = isBusy,
                    onClick = { onResumeRoom(room.roomCode) },
                )
            }
        }
        item {
            Text(
                text = "الطاولات العامة المفتوحة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = InkDark,
            )
        }
        when (publicRoomsState) {
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ShaddahaBlue)
                }
            }
            is UiState.Error -> item {
                Text(publicRoomsState.message, color = ShaddahaRed)
            }
            is UiState.Success -> {
                if (publicRoomsState.data.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ShaddahaSurfaceSoft,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(imageVector = Icons.Rounded.Public, contentDescription = null, tint = ShaddahaBlue)
                                Text(
                                    text = "لا توجد طاولات عامة الآن — أنشئ طاولة جديدة أو العب سريعاً!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                )
                            }
                        }
                    }
                } else {
                    items(publicRoomsState.data, key = { "hub_pub_${it.roomCode}" }) { room ->
                        RoomListCard(
                            room = room,
                            actionLabel = "انضمام",
                            isBusy = isBusy,
                            onClick = { onJoinCode(room.roomCode) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomListCard(
    room: GameRoom,
    actionLabel: String,
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    val seatCount = room.humanSeatCount()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.dp, OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_card_${room.roomCode}"),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ShaddahaBlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = ShaddahaBlue,
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "جلسة ${room.seat0Name.ifBlank { "المضيف" }}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = InkDark,
                        )
                        Surface(
                            color = ShaddahaYellowSoft,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = InkDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "اللاعبون: $seatCount/4 • الهدف: ${room.targetScore}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
            }
            Button(
                onClick = onClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp),
            ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun MoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color = ShaddahaBlue,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.dp, OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    isSubscribed: Boolean,
    remainingFreeSessionsToday: Int,
    onDismiss: () -> Unit,
    onConfirm: (roomType: String, targetScore: Int) -> Unit,
) {
    var selectedType by remember { mutableStateOf("public") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء جلسة بلوت أونلاين") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("خصوصية الجلسة", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == "public",
                        onClick = { selectedType = "public" },
                        label = { Text("عامة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShaddahaBlue,
                            selectedLabelColor = ShaddahaWhite,
                        ),
                    )
                    FilterChip(
                        selected = selectedType == "private",
                        onClick = { selectedType = "private" },
                        label = { Text("خاصة (بالرمز)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShaddahaBlue,
                            selectedLabelColor = ShaddahaWhite,
                        ),
                    )
                }
                Text("نقاط الفوز (الصكة)", style = MaterialTheme.typography.titleSmall)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ShaddahaYellowSoft,
                    border = BorderStroke(1.dp, ShaddahaYellowRich),
                ) {
                    Text(
                        text = "صكة رسمية: 152 نقطة",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = InkDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Text(
                    text = if (isSubscribed) {
                        "إنشاء جلسات غير محدود باشتراك شدها بلس ✓"
                    } else {
                        "المتبقي لك اليوم مجاناً: $remainingFreeSessionsToday من ${DailySessionQuotaStorage.FREE_DAILY_SESSION_LIMIT} جلسات"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSubscribed) TealAccent else ShaddahaBlue,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedType, 152) },
                modifier = Modifier.testTag("confirm_create_room_button"),
            ) { Text("إنشاء الطاولة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
private fun SoloMatchDialog(
    onDismiss: () -> Unit,
    onStart: (targetScore: Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحدي سريع ضد الذكاء البرمجي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "العب فوراً مع خويّك الذكي ضد خصمين في صكة رسمية (152 نقطة) واحصل على نقاط تصنيف إضافية عند الفوز.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(152) },
                modifier = Modifier.testTag("confirm_start_solo_button"),
            ) { Text("ابدأ اللعب") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreSheet(
    selectedTableBackground: TableBackgroundStyle,
    ownedBackgrounds: Set<TableBackgroundStyle>,
    selectedCardDeck: CardDeckStyle,
    ownedDecks: Set<CardDeckStyle>,
    isSubscribed: Boolean,
    billingSnapshot: BillingUiSnapshot,
    onSelectOrPurchaseBackground: (TableBackgroundStyle) -> Unit,
    onSelectOrPurchaseDeck: (CardDeckStyle) -> Unit,
    onOpenSubscription: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sampleCard = remember { BalootCard(suit = Suit.HEARTS, rank = Rank.ACE) }
    val storeBackgrounds = TableBackgroundStyle.storeBackgrounds
    val freeBackgrounds = TableBackgroundStyle.freeBackgrounds
    val deckOrder = listOf(
        CardDeckStyle.DIAMOND,
        CardDeckStyle.GOLD,
        CardDeckStyle.SILVER,
        CardDeckStyle.BRONZE,
        CardDeckStyle.CLASSIC,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ShaddahaWhite,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(imageVector = Icons.Rounded.Storefront, contentDescription = null, tint = ShaddahaBlue)
                    Text(
                        text = "متجر شدها",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkDark,
                    )
                }
                Text(
                    text = "خصص خلفية طاولة اللعب واختر لون الورق المفضل لديك",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "خلفيات الطاولات السعودية المميزة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkDark,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ShaddahaYellow,
                    ) {
                        Text(
                            text = "20 ريال للخلفية",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            items(storeBackgrounds, key = { "store_bg_${it.name}" }) { bg ->
                val isSelected = selectedTableBackground == bg
                val isUnlocked = bg in ownedBackgrounds
                val displayPrice = billingSnapshot.backgroundPrices[bg] ?: bg.priceLabel
                Surface(
                    onClick = { onSelectOrPurchaseBackground(bg) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) ShaddahaBlueSoft else ShaddahaSurfaceSoft,
                    border = BorderStroke(
                        width = if (isSelected) 2.2.dp else 1.2.dp,
                        color = if (isSelected) ShaddahaBlue else bg.accentColor.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_bg_${bg.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TableBackgroundThumbnail(
                            style = bg,
                            isSelected = isSelected,
                            isLocked = !isUnlocked,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bg.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                            )
                            Text(
                                text = bg.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = bg.accentColor.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = if (isUnlocked) "مملوكة ✓" else displayPrice,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = bg.accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Button(
                            onClick = { onSelectOrPurchaseBackground(bg) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSelected -> TealAccent
                                    isUnlocked -> ShaddahaBlue
                                    else -> bg.accentColor
                                },
                                contentColor = ShaddahaWhite,
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text(
                                text = when {
                                    isSelected -> "مُفعّلة ✓"
                                    isUnlocked -> "اختيار"
                                    else -> "شراء $displayPrice"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "خلفيات الطاولة المجانية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = InkDark,
                )
            }

            items(freeBackgrounds, key = { "free_bg_${it.name}" }) { bg ->
                val isSelected = selectedTableBackground == bg
                Surface(
                    onClick = { onSelectOrPurchaseBackground(bg) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ShaddahaBlueSoft else ShaddahaSurfaceSoft,
                    border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) ShaddahaBlue else OutlineSoft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_bg_${bg.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TableBackgroundThumbnail(
                            style = bg,
                            isSelected = isSelected,
                            isLocked = false,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bg.title, fontWeight = FontWeight.Bold, color = InkDark)
                            Text(bg.subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                        Button(
                            onClick = { onSelectOrPurchaseBackground(bg) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) TealAccent else ShaddahaBlue,
                                contentColor = ShaddahaWhite,
                            ),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(
                                text = if (isSelected) "مُفعّلة ✓" else "اختيار",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "ألوان الورق المتاحة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = InkDark,
                    )
                    if (!isSubscribed) {
                        TextButton(onClick = onOpenSubscription) {
                            Text("أو افتح الكل بالاشتراك", fontWeight = FontWeight.Bold, color = ShaddahaRed)
                        }
                    }
                }
            }

            items(deckOrder, key = { it.name }) { deck ->
                val isSelected = selectedCardDeck == deck
                val isUnlocked = isSubscribed || deck in ownedDecks || deck.priceSar == 0
                val displayPrice = billingSnapshot.deckPrices[deck] ?: deck.priceLabel
                Surface(
                    onClick = { onSelectOrPurchaseDeck(deck) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) deck.faceBackground else ShaddahaSurfaceSoft,
                    border = BorderStroke(
                        width = if (isSelected) 2.2.dp else 1.dp,
                        color = if (isSelected) deck.faceBorder else OutlineSoft,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_tier_${deck.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(modifier = Modifier.width(66.dp), contentAlignment = Alignment.CenterStart) {
                                BalootCardBack(
                                    width = 34.dp,
                                    height = 48.dp,
                                    deckStyle = deck,
                                    modifier = Modifier.offset(x = 24.dp),
                                )
                                BalootCardView(
                                    card = sampleCard,
                                    width = 36.dp,
                                    height = 52.dp,
                                    deckStyle = deck,
                                )
                            }
                            Column {
                                Text(
                                    text = deck.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = InkDark,
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = deck.badgeColor.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = displayPrice,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = deck.badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onSelectOrPurchaseDeck(deck) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSelected -> TealAccent
                                    isUnlocked -> ShaddahaBlue
                                    else -> deck.badgeColor
                                },
                                contentColor = ShaddahaWhite,
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text(
                                text = when {
                                    isSelected -> "مُفعّل ✓"
                                    isUnlocked -> "اختيار"
                                    else -> "شراء $displayPrice"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }

            item {
                PlayBillingStatusNote(billingSnapshot = billingSnapshot)
            }
        }
    }
}

@Composable
private fun ShareInviteDialog(
    playerName: String,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val inviteText = "تحدّاني الآن في لعبة البلوت \"شدها\"! اسم حسابي: $playerName"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.Campaign, contentDescription = null, tint = ShaddahaBlue)
                Text("انشر التحدي وادعُ ربعك")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "شارك دعوة شدها مع أصدقائك لتلعبوا معاً في طاولات خاصة وتتنافسوا على الصدارة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ShaddahaSurfaceSoft,
                    border = BorderStroke(1.dp, OutlineSoft),
                ) {
                    Text(
                        text = inviteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkDark,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                if (copied) {
                    Text(
                        text = "تم نسخ نص الدعوة بنجاح!",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealAccent,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                clipboard.setText(AnnotatedString(inviteText))
                copied = true
            }) {
                Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نسخ الدعوة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionSheet(
    isSubscribed: Boolean,
    activePlan: SubscriptionPlan?,
    billingSnapshot: BillingUiSnapshot,
    limitGateMessage: String? = null,
    canCancelPreview: Boolean = false,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onCancelPreview: () -> Unit = {},
    onOpenStore: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ShaddahaWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "اشتراك شدها بلس",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkDark,
                )
                Text(
                    text = "أنشئ جلسات بلا حدود يومياً (بدلاً من جلستين مجانياً) وافتح جميع ألوان الورق ومزايا المشتركين.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }

            if (!limitGateMessage.isNullOrBlank() && !isSubscribed) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ShaddahaRed.copy(alpha = 0.1f),
                    border = BorderStroke(1.5.dp, ShaddahaRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subscription_limit_gate_banner"),
                ) {
                    Text(
                        text = limitGateMessage,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = ShaddahaRed,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            SubscriptionPlan.entries.forEach { plan ->
                val isCurrentPlan = isSubscribed && activePlan == plan
                val priceLabel = billingSnapshot.priceFor(plan)
                val accentColor = when (plan) {
                    SubscriptionPlan.WEEKLY -> ShaddahaBlue
                    SubscriptionPlan.MONTHLY -> ShaddahaRed
                    SubscriptionPlan.YEARLY -> Color(0xFFD97706)
                }
                val cardBg = when {
                    isCurrentPlan -> TealAccent.copy(alpha = 0.1f)
                    plan == SubscriptionPlan.YEARLY -> ShaddahaYellowSoft
                    plan == SubscriptionPlan.MONTHLY -> ShaddahaRed.copy(alpha = 0.06f)
                    else -> ShaddahaBlueSoft.copy(alpha = 0.65f)
                }
                val cardBorder = when {
                    isCurrentPlan -> TealAccent
                    plan == SubscriptionPlan.YEARLY -> ShaddahaYellowRich
                    else -> accentColor.copy(alpha = 0.65f)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(if (plan == SubscriptionPlan.MONTHLY || isCurrentPlan) 2.dp else 1.4.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (isCurrentPlan) TealAccent else accentColor,
                                )
                                Column {
                                    Text(
                                        text = plan.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = InkDark,
                                    )
                                    Text(
                                        text = plan.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrentPlan) TealAccent else accentColor,
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isCurrentPlan) TealAccent else accentColor,
                            ) {
                                Text(
                                    text = if (isCurrentPlan) "مفعّل ✓" else priceLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ShaddahaWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }

                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )

                        Button(
                            onClick = { onSelectPlan(plan) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentPlan) TealAccent else accentColor,
                                contentColor = ShaddahaWhite,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("subscribe_${plan.name.lowercase()}_button"),
                        ) {
                            Text(
                                text = if (isCurrentPlan) {
                                    "باقتك الحالية مفعّلة ✓"
                                } else {
                                    "اشترك الآن — $priceLabel"
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (canCancelPreview && isSubscribed) {
                TextButton(
                    onClick = onCancelPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_preview_subscription_button"),
                ) {
                    Text("إنهاء معاينة الاشتراك التجريبي", color = ShaddahaRed, fontWeight = FontWeight.Bold)
                }
            }

            if (onOpenStore != null) {
                OutlinedButton(
                    onClick = onOpenStore,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(imageVector = Icons.Rounded.Storefront, contentDescription = null, tint = ShaddahaBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصفح ألوان الورق والخلفيات في المتجر", fontWeight = FontWeight.Bold, color = ShaddahaBlue)
                }
            }

            PlayBillingStatusNote(billingSnapshot = billingSnapshot)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun PlayBillingStatusNote(billingSnapshot: BillingUiSnapshot) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ShaddahaBlueSoft,
        border = BorderStroke(1.dp, ShaddahaBlue.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = billingSnapshot.statusMessage,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ShaddahaBlue,
            )
            Text(
                text = "تُعالج جميع المدفوعات عبر Google Play Billing وتُودع عوائدها في حساب التاجر المرتبط بحساب المطور على Google Play Console (الاشتراكات: ${ShaddahaProducts.SUBSCRIPTION_WEEKLY_ID}، ${ShaddahaProducts.SUBSCRIPTION_PLUS_ID}، ${ShaddahaProducts.SUBSCRIPTION_YEARLY_ID} • العناصر: bg_saudi_gold_20، bg_saudi_royal_20، bg_saudi_green_20، deck_diamond_20، deck_gold_10، deck_silver_7، deck_bronze_5).",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
            )
        }
    }
}

@Composable
fun AiRulesAdvisorDialog(
    aiCoachState: AiCoachUiState,
    onAskRulesQuestion: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var questionInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null, tint = ShaddahaBlue)
                Text("مستشار شدها الذكي")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it.take(200) },
                    label = { Text("اسأل عن قانون أو حسبة بلوت...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_rules_question_input"),
                )
                when (aiCoachState) {
                    is AiCoachUiState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ShaddahaBlue)
                        Text("جاري التحليل...")
                    }
                    is AiCoachUiState.Streaming -> Text(
                        text = aiCoachState.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkDark,
                    )
                    is AiCoachUiState.Success -> Text(
                        text = aiCoachState.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkDark,
                    )
                    is AiCoachUiState.Error -> Text(
                        text = aiCoachState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ShaddahaRed,
                    )
                    AiCoachUiState.Idle -> Text(
                        text = "مثال: متى تنحسب الخسارة في الحكم؟ أو كيف أحسب السرا؟",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAskRulesQuestion(questionInput) },
                enabled = questionInput.trim().isNotEmpty() && aiCoachState !is AiCoachUiState.Loading,
                modifier = Modifier.testTag("submit_ai_rules_question_button"),
            ) { Text("اسأل") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
    )
}
