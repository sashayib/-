package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.RankTierBadge
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
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TierBronze
import com.example.ui.theme.TierGold
import com.example.ui.theme.TierSilver

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<PlayerProfile>>,
    currentUserId: String,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Blue Header
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
                    modifier = Modifier.testTag("leaderboard_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "العودة للرئيسية",
                        tint = ShaddahaWhite,
                    )
                }
                Text(
                    text = "المتصدرين وتصنيف شدها",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = ShaddahaWhite,
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (leaderboardState) {
                is UiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ShaddahaBlue)
                }
                is UiState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = leaderboardState.message,
                        color = ShaddahaRed,
                    )
                }
                is UiState.Success -> {
                    val players = leaderboardState.data
                    val myIndex = players.indexOfFirst { it.userId == currentUserId }
                    val myProfile = players.getOrNull(myIndex)

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (myProfile != null) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = ShaddahaBlueSoft,
                                    border = BorderStroke(1.5.dp, ShaddahaBlue.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = ShaddahaBlue,
                                            ) {
                                                Text(
                                                    text = "#${myIndex + 1}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = ShaddahaWhite,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "ترتيبك الحالي (${myProfile.displayName})",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = InkDark,
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                RankTierBadge(
                                                    tierName = myProfile.rankTier,
                                                    rating = myProfile.rating.toInt(),
                                                )
                                            }
                                        }
                                        Surface(shape = RoundedCornerShape(50), color = ShaddahaYellow) {
                                            Text(
                                                text = "${myProfile.wins} فوز",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = InkDark,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (players.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = ShaddahaSurfaceSoft,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = ShaddahaBlue,
                                            modifier = Modifier.size(38.dp),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "لا يوجد متصدرون بعد",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = InkDark,
                                        )
                                    }
                                }
                            }
                        } else {
                            if (players.size >= 3) {
                                item {
                                    PodiumRow(
                                        first = players[0],
                                        second = players[1],
                                        third = players[2],
                                    )
                                }
                            }

                            itemsIndexed(players, key = { _, p -> p.userId }) { index, player ->
                                LeaderboardPlayerRow(
                                    rank = index + 1,
                                    player = player,
                                    isMe = player.userId == currentUserId,
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
private fun PodiumRow(
    first: PlayerProfile,
    second: PlayerProfile,
    third: PlayerProfile,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumCard(rank = 2, player = second, accent = TierSilver, modifier = Modifier.weight(1f))
        PodiumCard(rank = 1, player = first, accent = TierGold, isChampion = true, modifier = Modifier.weight(1.1f))
        PodiumCard(rank = 3, player = third, accent = TierBronze, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumCard(
    rank: Int,
    player: PlayerProfile,
    accent: Color,
    isChampion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isChampion) ShaddahaYellow.copy(alpha = 0.28f) else ShaddahaSurfaceSoft,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.75f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = if (isChampion) 16.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = "المركز $rank",
                tint = accent,
                modifier = Modifier.size(if (isChampion) 28.dp else 22.dp),
            )
            PlayerAvatar(
                name = player.displayName,
                avatarUrl = player.avatarUrl,
                size = if (isChampion) 50.dp else 40.dp,
                isHighlighted = isChampion,
                preferCartoonAvatar = true,
            )
            Text(
                text = player.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = InkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${player.rating}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = ShaddahaBlue,
            )
        }
    }
}

@Composable
private fun LeaderboardPlayerRow(
    rank: Int,
    player: PlayerProfile,
    isMe: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isMe) ShaddahaBlueSoft else ShaddahaSurfaceSoft,
        border = BorderStroke(
            1.dp,
            if (isMe) ShaddahaBlue else OutlineSoft,
        ),
        modifier = Modifier.fillMaxWidth(),
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (rank <= 3) ShaddahaYellow else ShaddahaSurfaceGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = InkDark,
                    )
                }
                PlayerAvatar(
                    name = player.displayName,
                    avatarUrl = player.avatarUrl,
                    size = 40.dp,
                    preferCartoonAvatar = true,
                )
                Column {
                    Text(
                        text = player.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${player.rankTier} • ${player.wins} فوز / ${player.losses} خسارة",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
            }
            Surface(shape = RoundedCornerShape(50), color = ShaddahaWhite) {
                Text(
                    text = "${player.rating}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = ShaddahaBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
