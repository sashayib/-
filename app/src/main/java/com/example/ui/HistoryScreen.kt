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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MatchRecord
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.example.ui.components.PlayerStatsPieChartCard
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowRich
import com.example.ui.theme.TealAccent
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    profileState: UiState<PlayerProfile>,
    historyState: UiState<List<MatchRecord>>,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val profile = (profileState as? UiState.Success)?.data
    val matches = (historyState as? UiState.Success)?.data.orEmpty()

    val wins = (profile?.wins ?: 0L).toInt()
    val losses = (profile?.losses ?: 0L).toInt()
    val totalMatches = (profile?.matchesPlayed ?: 0L).toInt().coerceAtLeast(wins + losses)
    val winRate = if (totalMatches > 0) ((wins * 100f) / totalMatches).roundToInt() else 0
    val streak = (profile?.streak ?: 0L).toInt()
    val rating = (profile?.rating ?: 22L).toInt()

    val onlineMatchesCount = remember(matches) { matches.count { it.mode == "online" } }
    val soloMatchesCount = remember(matches) { matches.count { it.mode != "online" } }

    var matchFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Wins, 2 = Losses
    val filteredMatches = remember(matches, matchFilter) {
        when (matchFilter) {
            1 -> matches.filter { it.didWin }
            2 -> matches.filter { !it.didWin }
            else -> matches
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                    modifier = Modifier.testTag("history_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "العودة للرئيسية",
                        tint = ShaddahaWhite,
                    )
                }
                Text(
                    text = "إحصائياتي وسجل اللعب",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = ShaddahaWhite,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ShaddahaYellow,
                ) {
                    Text(
                        text = "$winRate% فوز",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. Interactive Circular Donut/Pie Chart
                item(key = "stats_pie_chart") {
                    PlayerStatsPieChartCard(
                        wins = wins,
                        losses = losses,
                        onlineMatches = onlineMatchesCount,
                        soloMatches = soloMatchesCount,
                    )
                }

                // 2. Personal Stats Grid (Row 1: Wins, Losses, Win Rate)
                item(key = "stats_row_primary") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatTile(
                            label = "مباريات الفوز",
                            value = "$wins",
                            icon = Icons.Rounded.EmojiEvents,
                            accent = TealAccent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_wins_tile"),
                        )
                        StatTile(
                            label = "مباريات الخسارة",
                            value = "$losses",
                            icon = Icons.Rounded.Cancel,
                            accent = ShaddahaRed,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_losses_tile"),
                        )
                        StatTile(
                            label = "نسبة الفوز",
                            value = "$winRate%",
                            icon = Icons.Rounded.Percent,
                            accent = ShaddahaBlue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_win_rate_tile"),
                        )
                    }
                }

                // 3. Secondary Stats Grid (Row 2: Total Matches, Win Streak, Rating)
                item(key = "stats_row_secondary") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatTile(
                            label = "إجمالي المباريات",
                            value = "$totalMatches",
                            icon = Icons.Rounded.SportsEsports,
                            accent = ShaddahaBlue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_total_matches_tile"),
                        )
                        StatTile(
                            label = "سلسلة الفوز",
                            value = "$streak",
                            icon = Icons.Rounded.LocalFireDepartment,
                            accent = ShaddahaYellowRich,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_streak_tile"),
                        )
                        StatTile(
                            label = "نقاط التصنيف",
                            value = "$rating",
                            icon = Icons.Rounded.Star,
                            accent = TealAccent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_rating_tile"),
                        )
                    }
                }

                // 4. Match History Header & Filter Chips
                item(key = "history_section_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "سجل المباريات الأخيرة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = InkDark,
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ShaddahaBlueSoft,
                            ) {
                                Text(
                                    text = "${filteredMatches.size} مباراة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ShaddahaBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }
                        if (matches.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    0 to "الكل (${matches.size})",
                                    1 to "الانتصارات (${matches.count { it.didWin }})",
                                    2 to "الخسائر (${matches.count { !it.didWin }})",
                                ).forEach { (idx, label) ->
                                    FilterChip(
                                        selected = matchFilter == idx,
                                        onClick = { matchFilter = idx },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ShaddahaYellow,
                                            selectedLabelColor = InkDark,
                                        ),
                                        modifier = Modifier.testTag("history_filter_$idx"),
                                    )
                                }
                            }
                        }
                    }
                }

                when (historyState) {
                    is UiState.Loading -> item(key = "history_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = ShaddahaBlue)
                        }
                    }
                    is UiState.Error -> item(key = "history_error") {
                        Text(text = historyState.message, color = ShaddahaRed)
                    }
                    is UiState.Success -> {
                        if (filteredMatches.isEmpty()) {
                            item(key = "history_empty") {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = ShaddahaSurfaceSoft,
                                    border = BorderStroke(1.dp, OutlineSoft),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = ShaddahaBlue,
                                            modifier = Modifier.size(38.dp),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (matches.isEmpty()) {
                                                "لم تُسجّل مباريات مكتملة بعد"
                                            } else {
                                                "لا توجد مباريات ضمن هذا التصنيف"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = InkDark,
                                        )
                                        Text(
                                            text = if (matches.isEmpty()) {
                                                "أكمل صكة أونلاين أو ضد الذكاء ليظهر سجلك هنا ويتحدّث الرسم البياني"
                                            } else {
                                                "اختر \"الكل\" لاستعراض جميع مبارياتك"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredMatches, key = { it.id }) { match ->
                                MatchHistoryCard(match = match)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.dp, OutlineSoft),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MatchHistoryCard(match: MatchRecord) {
    val resultColor = if (match.didWin) TealAccent else ShaddahaRed
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.2.dp, resultColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (match.didWin) "انتصار" else "خسارة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = resultColor,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ShaddahaYellow.copy(alpha = 0.45f),
                    ) {
                        Text(
                            text = if (match.mode == "online") "أونلاين ${match.roomCode ?: ""}" else "ضد الذكاء",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "النتيجة: لنا ${match.usScore} — لهم ${match.themScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkDark,
                )
                if (match.opponentSummary.isNotBlank()) {
                    Text(
                        text = "ضد: ${match.opponentSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = resultColor.copy(alpha = 0.14f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (match.didWin) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = resultColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = if (match.ratingDelta >= 0) "+${match.ratingDelta}" else "${match.ratingDelta}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = resultColor,
                    )
                }
            }
        }
    }
}
