package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FriendWithPresence
import com.example.data.GameRoom
import com.example.data.TableInvite
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaSurfaceGray
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowRich
import com.example.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingRoomScreen(
    room: GameRoom,
    currentUserId: String,
    friends: List<FriendWithPresence>,
    outgoingInvites: List<TableInvite>,
    isBusy: Boolean,
    onInviteFriend: (FriendWithPresence) -> Unit,
    onStartMatch: () -> Unit,
    onLeaveRoom: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val isHost = room.hostId == currentUserId
    var showInviteFriendsSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = !isBusy) {
        onLeaveRoom()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Curved Blue Header matching UX #3
        Surface(
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            color = ShaddahaBlue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onLeaveRoom,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("waiting_room_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "مغادرة الجلسة",
                        tint = ShaddahaWhite,
                    )
                }
                Text(
                    text = "طاولة الانتظار",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = ShaddahaWhite,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ShaddahaYellow,
                ) {
                    Text(
                        text = "${room.targetScore} نقطة",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = InkDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ShaddahaSurfaceSoft,
                    border = BorderStroke(1.5.dp, ShaddahaBlue.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column {
                            Text(
                                text = "رمز الدعوة للجلسة",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted,
                            )
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = ShaddahaBlue,
                            )
                        }
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(room.roomCode)) },
                            modifier = Modifier.testTag("copy_room_code_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "نسخ رمز الجلسة",
                                tint = ShaddahaBlue,
                            )
                        }
                    }
                }

                // 2x2 Team Seating Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SeatCard(
                        seatLabel = "فريقنا • مقعد 1",
                        name = room.seat0Name,
                        rating = room.seat0Rating.toInt(),
                        isOccupied = room.seat0Uid.isNotBlank(),
                        isTeamUs = true,
                        modifier = Modifier.weight(1f),
                    )
                    SeatCard(
                        seatLabel = "فريقنا • مقعد 3 (خويك)",
                        name = room.seat2Name,
                        rating = room.seat2Rating.toInt(),
                        isOccupied = room.seat2Uid.isNotBlank(),
                        isTeamUs = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SeatCard(
                        seatLabel = "الخصوم • مقعد 2",
                        name = room.seat1Name,
                        rating = room.seat1Rating.toInt(),
                        isOccupied = room.seat1Uid.isNotBlank(),
                        isTeamUs = false,
                        modifier = Modifier.weight(1f),
                    )
                    SeatCard(
                        seatLabel = "الخصوم • مقعد 4",
                        name = room.seat3Name,
                        rating = room.seat3Rating.toInt(),
                        isOccupied = room.seat3Uid.isNotBlank(),
                        isTeamUs = false,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = room.lastActionText.ifBlank { "بانتظار انضمام اللاعبين..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isHost) {
                    val humanCount = room.humanSeatCount()
                    Button(
                        onClick = onStartMatch,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_online_match_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShaddahaBlue,
                            contentColor = ShaddahaWhite,
                        ),
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (humanCount == 4) {
                                "بدء المباراة الآن (4/4)"
                            } else {
                                "بدء الآن وإكمال المقاعد بروبوتات ($humanCount/4)"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ShaddahaBlueSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(imageVector = Icons.Rounded.HourglassTop, contentDescription = null, tint = ShaddahaBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بانتظار قيام المضيف ببدء الصكة...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShaddahaBlue,
                            )
                        }
                    }
                }

                Button(
                    onClick = { showInviteFriendsSheet = true },
                    enabled = !isBusy && room.humanSeatCount() < 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("waiting_room_invite_friends_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShaddahaYellowRich,
                        contentColor = InkDark,
                    ),
                ) {
                    Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دعوة صديق للطاولة (${friends.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    )
                }

                OutlinedButton(
                    onClick = onLeaveRoom,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("leave_waiting_room_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (isHost) "إلغاء الجلسة" else "مغادرة الطاولة")
                }
            }
        }
    }

    if (showInviteFriendsSheet) {
        val nowMs = System.currentTimeMillis()
        ModalBottomSheet(
            onDismissRequest = { showInviteFriendsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ShaddahaWhite,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "دعوة الأصدقاء للطاولة (${room.roomCode})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkDark,
                )
                if (friends.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ShaddahaSurfaceSoft,
                        border = BorderStroke(1.dp, OutlineSoft),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "لا يوجد أصدقاء في قائمتك بعد. يمكنك نسخ رمز الطاولة (${room.roomCode}) أو إضافة أصدقاء من قائمة الأصدقاء بالرئيسية.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        items(friends, key = { it.friendUid }) { friend ->
                            val inRoom = friend.friendUid in room.memberUids
                            val invited = outgoingInvites.any {
                                it.toUid == friend.friendUid &&
                                    it.roomCode == room.roomCode &&
                                    it.status == "pending"
                            }
                            val presence = friend.effectivePresence(nowMs)
                            val (statusText, statusColor) = when (presence) {
                                "online" -> "متصل الآن" to OnlineDotGreen
                                "in_game" -> "داخل طاولة" to ShaddahaBlue
                                else -> "خارج الخط" to OfflineDotGray
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ShaddahaSurfaceSoft,
                                border = BorderStroke(1.dp, OutlineSoft),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box {
                                        PlayerAvatar(
                                            name = friend.displayName,
                                            avatarUrl = friend.avatarUrl,
                                            size = 44.dp,
                                            preferCartoonAvatar = friend.avatarUrl.isNullOrBlank(),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                                .border(2.dp, ShaddahaWhite, CircleShape),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friend.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = InkDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "$statusText • ${friend.playerCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                        )
                                    }
                                    Button(
                                        onClick = { onInviteFriend(friend) },
                                        enabled = !isBusy && !inRoom && !invited,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (inRoom || invited) ShaddahaSurfaceGray else ShaddahaBlue,
                                            contentColor = if (inRoom || invited) InkMuted else ShaddahaWhite,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("room_invite_friend_${friend.friendUid}"),
                                    ) {
                                        if (invited) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = when {
                                                inRoom -> "بالطاولة"
                                                invited -> "تمت الدعوة"
                                                else -> "دعوة"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatCard(
    seatLabel: String,
    name: String,
    rating: Int,
    isOccupied: Boolean,
    isTeamUs: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (isTeamUs) ShaddahaBlue else TealAccent
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(
            1.2.dp,
            if (isOccupied) accent.copy(alpha = 0.55f) else OutlineSoft,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = seatLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
            if (isOccupied) {
                PlayerAvatar(
                    name = name,
                    avatarUrl = null,
                    size = 48.dp,
                    isHighlighted = true,
                    preferCartoonAvatar = true,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = InkDark,
                    maxLines = 1,
                )
                Surface(shape = RoundedCornerShape(8.dp), color = ShaddahaYellow) {
                    Text(
                        text = "تصنيف $rating",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = InkDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ShaddahaSurfaceGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "?", style = MaterialTheme.typography.titleLarge, color = InkMuted)
                }
                Text(text = "مقعد شاغر", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Text(
                    text = "متاح للاعب أو بوت",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted.copy(alpha = 0.7f),
                )
            }
        }
    }
}
