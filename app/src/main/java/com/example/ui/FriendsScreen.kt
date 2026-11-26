package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FriendWithPresence
import com.example.data.PlayerIdGenerator
import com.example.data.PlayerProfile
import com.example.data.TableInvite
import com.example.data.UiState
import com.example.ui.components.PlayerAvatar
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
import com.example.ui.theme.ShaddahaYellowSoft
import kotlinx.coroutines.delay

internal val OnlineDotGreen = Color(0xFF16A34A)
internal val OfflineDotGray = Color(0xFF94A3B8)

@Composable
fun FriendsScreen(
    profileState: UiState<PlayerProfile>,
    friendsState: UiState<List<FriendWithPresence>>,
    leaderboardState: UiState<List<PlayerProfile>>,
    incomingInvites: List<TableInvite>,
    outgoingInvites: List<TableInvite>,
    activeWaitingRoomCode: String?,
    currentUserId: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onAddFriendByPlayerId: (String, () -> Unit) -> Unit,
    onAddFriendProfile: (PlayerProfile) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onInviteFriendToPrivateTable: (FriendWithPresence, Int) -> Unit,
    onAcceptInvite: (TableInvite) -> Unit,
    onDeclineInvite: (TableInvite) -> Unit,
    onJoinRoomCode: (String) -> Unit,
    onCreatePrivateRoom: (Int) -> Unit,
) {
    BackHandler { onBack() }

    val clipboard = LocalClipboardManager.current
    val profile = (profileState as? UiState.Success)?.data
    val myPlayerCode = profile?.effectivePlayerCode() ?: PlayerIdGenerator.forUid(currentUserId)
    val friends = (friendsState as? UiState.Success)?.data.orEmpty()
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val onlineCount = remember(friends, nowMs) {
        friends.count { it.isOnlineNow(nowMs) }
    }
    val existingFriendUids = remember(friends) { friends.map { it.friendUid }.toSet() }
    val suggestedPlayers = remember(leaderboard, existingFriendUids, currentUserId) {
        leaderboard.filter { p ->
            val uid = p.userId.ifBlank { p.id }
            uid.isNotBlank() && uid != currentUserId && uid !in existingFriendUids
        }.take(5)
    }

    var playerIdInput by remember { mutableStateOf("") }
    var copiedMyId by remember { mutableStateOf(false) }
    var inviteTargetScore by remember { mutableIntStateOf(152) }
    var joinRoomCodeInput by remember { mutableStateOf("") }

    LaunchedEffect(copiedMyId) {
        if (copiedMyId) {
            delay(1800)
            copiedMyId = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Curved Cobalt Blue Header
        Surface(
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            color = ShaddahaBlue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("friends_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "العودة للرئيسية",
                        tint = ShaddahaWhite,
                    )
                }
                Text(
                    text = "قائمة الأصدقاء",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = ShaddahaWhite,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ShaddahaYellow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (onlineCount > 0) OnlineDotGreen else OfflineDotGray),
                        )
                        Text(
                            text = "$onlineCount متصل",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkDark,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. My Player ID Card
                item(key = "my_player_id") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ShaddahaBlueSoft,
                        border = BorderStroke(1.3.dp, ShaddahaBlue.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ShaddahaBlue,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Badge,
                                            contentDescription = null,
                                            tint = ShaddahaWhite,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "معرّف اللاعب الخاص بك (Player ID)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkMuted,
                                    )
                                    Text(
                                        text = myPlayerCode,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp,
                                        ),
                                        color = ShaddahaBlue,
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(myPlayerCode))
                                    copiedMyId = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (copiedMyId) OnlineDotGreen else ShaddahaBlue,
                                    contentColor = ShaddahaWhite,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("copy_my_player_id_button"),
                            ) {
                                Icon(
                                    imageVector = if (copiedMyId) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = "نسخ معرّف اللاعب",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copiedMyId) "تم النسخ" else "نسخ",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                // 2. Incoming Private Table Invites
                if (incomingInvites.isNotEmpty()) {
                    item(key = "invites_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mail,
                                contentDescription = null,
                                tint = ShaddahaRed,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "دعوات الطاولات الواردة (${incomingInvites.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                            )
                        }
                    }
                    items(incomingInvites, key = { "invite_${it.id}" }) { invite ->
                        IncomingTableInviteCard(
                            invite = invite,
                            isBusy = isBusy,
                            onAccept = { onAcceptInvite(invite) },
                            onDecline = { onDeclineInvite(invite) },
                        )
                    }
                }

                // 3. Add Friend by Player ID Card
                item(key = "add_friend_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ShaddahaSurfaceSoft,
                        border = BorderStroke(1.dp, OutlineSoft),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = null,
                                    tint = ShaddahaBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "إضافة صديق عبر معرّف اللاعب",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = InkDark,
                                )
                            }
                            Text(
                                text = "أدخل معرّف صديقك (مثل SH4K9P2M) لمتابعة حالة اتصاله ودعوته مباشرة لطاولاتك الخاصة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = playerIdInput,
                                    onValueChange = { raw ->
                                        playerIdInput = raw.uppercase()
                                            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                                            .take(36)
                                    },
                                    label = { Text("معرّف اللاعب (Player ID)") },
                                    placeholder = { Text("SH4K9P2M") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_friend_player_id_input"),
                                )
                                Button(
                                    onClick = {
                                        onAddFriendByPlayerId(playerIdInput) {
                                            playerIdInput = ""
                                        }
                                    },
                                    enabled = playerIdInput.trim().length >= 6 && !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ShaddahaBlue,
                                        contentColor = ShaddahaWhite,
                                    ),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("add_friend_submit_button"),
                                ) {
                                    Text("إضافة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 4. Friends List Header + Target Score Selector for Direct Invites
                item(key = "friends_list_header") {
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
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = ShaddahaBlue,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "أصدقائي (${friends.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ShaddahaYellow,
                        ) {
                            Text(
                                text = "صكة 152",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                when (friendsState) {
                    is UiState.Loading -> {
                        item(key = "friends_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = ShaddahaBlue)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "friends_error") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ShaddahaRed.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, ShaddahaRed.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = friendsState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ShaddahaRed,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (friends.isEmpty()) {
                            item(key = "friends_empty") {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = ShaddahaSurfaceSoft,
                                    border = BorderStroke(1.dp, OutlineSoft),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = ShaddahaBlue.copy(alpha = 0.65f),
                                            modifier = Modifier.size(40.dp),
                                        )
                                        Text(
                                            text = "قائمة أصدقائك فارغة حالياً",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = InkDark,
                                        )
                                        Text(
                                            text = "شارك معرّفك ($myPlayerCode) مع ربعك أو أضفهم بالأعلى لتتمكن من متابعة اتصالهم ودعوتهم مباشرة.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(friends, key = { "friend_${it.friendUid}" }) { friend ->
                                val alreadyInvited = activeWaitingRoomCode != null &&
                                    outgoingInvites.any {
                                        it.toUid == friend.friendUid &&
                                            it.roomCode == activeWaitingRoomCode &&
                                            it.status == "pending"
                                    }
                                FriendRowCard(
                                    friend = friend,
                                    nowMs = nowMs,
                                    isInvited = alreadyInvited,
                                    isBusy = isBusy,
                                    onInvite = { onInviteFriendToPrivateTable(friend, inviteTargetScore) },
                                    onRemove = { onRemoveFriend(friend.friendUid) },
                                )
                            }
                        }
                    }
                }

                // 5. Suggested Players from Leaderboard
                if (suggestedPlayers.isNotEmpty()) {
                    item(key = "suggested_header") {
                        Text(
                            text = "لاعبون مقترحون لإضافتهم",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                        )
                    }
                    items(suggestedPlayers, key = { "suggested_${it.userId.ifBlank { it.id }}" }) { candidate ->
                        SuggestedPlayerCard(
                            profile = candidate,
                            nowMs = nowMs,
                            isBusy = isBusy,
                            onAdd = { onAddFriendProfile(candidate) },
                        )
                    }
                }

                // 6. Quick Private Table Actions (Join by Code or Create Private Room)
                item(key = "quick_private_room_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ShaddahaYellowSoft,
                        border = BorderStroke(1.2.dp, ShaddahaYellowRich.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "انضمام أو إنشاء طاولة خاصة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = joinRoomCodeInput,
                                    onValueChange = { input ->
                                        joinRoomCodeInput = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                    },
                                    label = { Text("رمز الطاولة (6 خانات)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Key,
                                            contentDescription = null,
                                            tint = ShaddahaBlue,
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("join_room_code_input"),
                                )
                                Button(
                                    onClick = { onJoinRoomCode(joinRoomCodeInput) },
                                    enabled = joinRoomCodeInput.length == 6 && !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("join_room_code_button"),
                                ) {
                                    Text("انضمام", fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { onCreatePrivateRoom(inviteTargetScore) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("create_private_table_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShaddahaYellowRich,
                                    contentColor = InkDark,
                                ),
                            ) {
                                Icon(imageVector = Icons.Rounded.AddCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إنشاء طاولة خاصة جديدة ($inviteTargetScore)",
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IncomingTableInviteCard(
    invite: TableInvite,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ShaddahaYellowSoft,
        border = BorderStroke(1.5.dp, ShaddahaBlue),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = ShaddahaBlue,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = ShaddahaWhite,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${invite.fromName} يدعوك لطاولة خاصة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "رمز الطاولة: ${invite.roomCode} • صكة ${invite.targetScore} نقطة",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShaddahaBlue,
                        contentColor = ShaddahaWhite,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("accept_invite_${invite.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("قبول والانضمام", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ShaddahaRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShaddahaRed),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("decline_invite_${invite.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "رفض الدعوة",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رفض", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FriendRowCard(
    friend: FriendWithPresence,
    nowMs: Long,
    isInvited: Boolean,
    isBusy: Boolean,
    onInvite: () -> Unit,
    onRemove: () -> Unit,
) {
    val presence = friend.effectivePresence(nowMs)
    val (statusLabel, statusColor) = when (presence) {
        "online" -> "متصل الآن" to OnlineDotGreen
        "in_game" -> "داخل طاولة" to ShaddahaBlue
        else -> "خارج الخط" to OfflineDotGray
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(
            width = if (presence == "online") 1.3.dp else 1.dp,
            color = if (presence == "online") OnlineDotGreen.copy(alpha = 0.45f) else OutlineSoft,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_card_${friend.friendUid}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box {
                    PlayerAvatar(
                        name = friend.displayName,
                        avatarUrl = friend.avatarUrl,
                        size = 52.dp,
                        preferCartoonAvatar = friend.avatarUrl.isNullOrBlank(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .border(2.dp, ShaddahaWhite, CircleShape),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "${friend.rankTier} • تصنيف ${friend.rating} • معرّف: ${friend.playerCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("remove_friend_${friend.friendUid}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "إزالة الصديق",
                        tint = InkMuted,
                    )
                }
            }

            Button(
                onClick = onInvite,
                enabled = !isBusy && !isInvited,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInvited) ShaddahaSurfaceGray else ShaddahaBlue,
                    contentColor = if (isInvited) InkMuted else ShaddahaWhite,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("invite_friend_${friend.friendUid}"),
            ) {
                Icon(
                    imageVector = if (isInvited) Icons.Rounded.Check else Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isInvited) "تم إرسال الدعوة للطاولة ✓" else "دعوة إلى طاولة خاصة",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun SuggestedPlayerCard(
    profile: PlayerProfile,
    nowMs: Long,
    isBusy: Boolean,
    onAdd: () -> Unit,
) {
    val uid = profile.userId.ifBlank { profile.id }
    val presence = profile.effectivePresence(nowMs)
    val dotColor = when (presence) {
        "online" -> OnlineDotGreen
        "in_game" -> ShaddahaBlue
        else -> OfflineDotGray
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaWhite,
        border = BorderStroke(1.dp, OutlineSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box {
                PlayerAvatar(
                    name = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    size = 42.dp,
                    preferCartoonAvatar = profile.avatarUrl.isNullOrBlank(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.5.dp, ShaddahaWhite, CircleShape),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName.ifBlank { "لاعب شدها" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "معرّف: ${profile.effectivePlayerCode()} • تصنيف ${profile.rating}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onAdd,
                enabled = !isBusy,
                modifier = Modifier.testTag("add_suggested_friend_$uid"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    tint = ShaddahaBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة", fontWeight = FontWeight.Bold, color = ShaddahaBlue)
            }
        }
    }
}
