package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaSurfaceGray
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellowRich
import com.example.ui.theme.ShaddahaYellowSoft

private data class CalcRoundEntry(
    val roundNumber: Int,
    val usDelta: Int,
    val themDelta: Int,
    val usTotalAfter: Int,
    val themTotalAfter: Int,
    val modeLabel: String,
    val buyerIsUs: Boolean,
    val multiplier: Int,
    val isKhassara: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BalootCalculatorScreen(
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    var targetScore by remember { mutableIntStateOf(152) }
    var sakkahNumber by remember { mutableIntStateOf(1) }
    val usTeamName = "لنا"
    val themTeamName = "لهم"
    val rounds = remember { mutableStateListOf<CalcRoundEntry>() }

    var showAddRoundSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingScoreTarget by remember { mutableStateOf<String?>(null) }

    val usTotal = rounds.firstOrNull()?.usTotalAfter ?: 0
    val themTotal = rounds.firstOrNull()?.themTotalAfter ?: 0
    val isSakkahFinished = usTotal >= targetScore || themTotal >= targetScore
    val startNewSakkah = {
        rounds.clear()
        sakkahNumber += 1
    }

    Scaffold(
        containerColor = ShaddahaSurfaceSoft,
        topBar = {
            Surface(
                color = ShaddahaWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("calculator_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "العودة",
                            tint = InkDark,
                        )
                    }

                    Text(
                        text = "حاسبة بلوت",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                        ),
                        color = InkDark,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = startNewSakkah,
                            modifier = Modifier.testTag("calculator_reset_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "صكة جديدة",
                                tint = InkDark,
                            )
                        }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("calculator_settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "إعدادات الحاسبة",
                                tint = InkDark,
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = ShaddahaSurfaceSoft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { showAddRoundSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShaddahaBlue,
                            contentColor = ShaddahaWhite,
                        ),
                        modifier = Modifier
                            .weight(1.45f)
                            .height(54.dp)
                            .testTag("calc_open_add_round_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أضف جولة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        )
                    }

                    Button(
                        onClick = {
                            if (rounds.isNotEmpty()) rounds.removeAt(0)
                        },
                        enabled = rounds.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShaddahaBlueSoft,
                            contentColor = ShaddahaBlue,
                            disabledContainerColor = ShaddahaSurfaceGray,
                            disabledContentColor = InkMuted,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("calc_undo_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تراجع",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Top Two Big Score Cards: لنا (Blue) | لهم (Red)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TeamScoreCard(
                        teamName = usTeamName,
                        score = usTotal,
                        targetScore = targetScore,
                        cardColor = ShaddahaBlue,
                        scoreTag = "calc_edit_us_score",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_us_score_card"),
                        onEditScore = { editingScoreTarget = "us" },
                    )
                    TeamScoreCard(
                        teamName = themTeamName,
                        score = themTotal,
                        targetScore = targetScore,
                        cardColor = ShaddahaRed,
                        scoreTag = "calc_edit_them_score",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_them_score_card"),
                        onEditScore = { editingScoreTarget = "them" },
                    )
                }

                if (isSakkahFinished) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val usWonSakkah = usTotal >= themTotal
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ShaddahaYellowSoft,
                        border = BorderStroke(1.5.dp, ShaddahaYellowRich),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "انتهت الصكة #$sakkahNumber!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = InkDark,
                                )
                                Text(
                                    text = "الفائز: ${if (usWonSakkah) usTeamName else themTeamName} ($usTotal - $themTotal)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkMuted,
                                )
                            }
                            Button(
                                onClick = startNewSakkah,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShaddahaBlue,
                                    contentColor = ShaddahaWhite,
                                ),
                                modifier = Modifier.testTag("calc_victory_new_sakkah_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("صكة جديدة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "الجولات",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = InkMuted,
                        )
                        Text(
                            text = "الصكة #$sakkahNumber • الهدف: $targetScore نقطة",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                        )
                    }
                    Surface(
                        onClick = startNewSakkah,
                        shape = RoundedCornerShape(50),
                        color = ShaddahaBlueSoft,
                        border = BorderStroke(1.dp, ShaddahaBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("calc_new_sakkah_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = ShaddahaBlue,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "صكة جديدة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = ShaddahaBlue,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (rounds.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ShaddahaWhite,
                        border = BorderStroke(1.dp, OutlineSoft),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "لا توجد جولات مسجلة بعد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = InkDark,
                            )
                            Text(
                                text = "اضغط على (+ أضف جولة) بالأسفل لإدخال نقاط كل جولة يدوياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(rounds, key = { _, item -> item.roundNumber }) { _, entry ->
                            RoundHistoryCard(
                                entry = entry,
                                usTeamName = usTeamName,
                                themTeamName = themTeamName,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddRoundSheet) {
        AddManualRoundSheet(
            usTeamName = usTeamName,
            themTeamName = themTeamName,
            onDismiss = { showAddRoundSheet = false },
            onAddRound = { usPts, themPts, modeLabel, buyerIsUs, multiplier, isKhassara ->
                val nextRoundNumber = (rounds.firstOrNull()?.roundNumber ?: 0) + 1
                val newUsTotal = usTotal + usPts
                val newThemTotal = themTotal + themPts
                rounds.add(
                    0,
                    CalcRoundEntry(
                        roundNumber = nextRoundNumber,
                        usDelta = usPts,
                        themDelta = themPts,
                        usTotalAfter = newUsTotal,
                        themTotalAfter = newThemTotal,
                        modeLabel = modeLabel,
                        buyerIsUs = buyerIsUs,
                        multiplier = multiplier,
                        isKhassara = isKhassara,
                    ),
                )
                showAddRoundSheet = false
            },
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("إعدادات الحاسبة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نقاط الصكة الرسمية:", style = MaterialTheme.typography.titleSmall)
                    FilterChip(
                        selected = true,
                        onClick = { targetScore = 152 },
                        label = { Text("152 نقطة (صكة رسمية)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShaddahaBlue,
                            selectedLabelColor = ShaddahaWhite,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) { Text("حفظ") }
            },
        )
    }

    val scoreTarget = editingScoreTarget
    if (scoreTarget != null) {
        val isUs = scoreTarget == "us"
        val currentTeamTotal = if (isUs) usTotal else themTotal
        var scoreInput by remember(scoreTarget) {
            mutableStateOf(currentTeamTotal.toString())
        }
        AlertDialog(
            onDismissRequest = { editingScoreTarget = null },
            title = { Text("تعديل رقم النشرة (${if (isUs) usTeamName else themTeamName})") },
            text = {
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = { raw -> scoreInput = raw.filter { it.isDigit() }.take(3) },
                    label = { Text("الرقم الجديد للنشرة") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_edit_score_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = scoreInput.toIntOrNull()
                        if (parsed != null && parsed != currentTeamTotal) {
                            val nextRoundNumber = (rounds.firstOrNull()?.roundNumber ?: 0) + 1
                            val usDelta = if (isUs) parsed - usTotal else 0
                            val themDelta = if (!isUs) parsed - themTotal else 0
                            rounds.add(
                                0,
                                CalcRoundEntry(
                                    roundNumber = nextRoundNumber,
                                    usDelta = usDelta,
                                    themDelta = themDelta,
                                    usTotalAfter = if (isUs) parsed else usTotal,
                                    themTotalAfter = if (!isUs) parsed else themTotal,
                                    modeLabel = "تعديل النشرة",
                                    buyerIsUs = isUs,
                                    multiplier = 1,
                                    isKhassara = false,
                                ),
                            )
                        }
                        editingScoreTarget = null
                    },
                    modifier = Modifier.testTag("calc_save_edited_score_button"),
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { editingScoreTarget = null }) { Text("إلغاء") }
            },
        )
    }
}

@Composable
private fun TeamScoreCard(
    teamName: String,
    score: Int,
    targetScore: Int,
    cardColor: Color,
    scoreTag: String,
    modifier: Modifier = Modifier,
    onEditScore: () -> Unit,
) {
    val progress = (score.toFloat() / targetScore.toFloat()).coerceIn(0f, 1f)
    val remaining = (targetScore - score).coerceAtLeast(0)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ShaddahaWhite,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onEditScore)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .testTag(scoreTag),
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = ShaddahaWhite,
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "تعديل رقم نشرة $teamName",
                    tint = ShaddahaWhite.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = ShaddahaYellowRich,
                trackColor = ShaddahaWhite.copy(alpha = 0.25f),
            )

            Text(
                text = if (remaining == 0) "فاز بالصكة!" else "باقي $remaining",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (remaining == 0) ShaddahaYellowRich else ShaddahaWhite.copy(alpha = 0.92f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundHistoryCard(
    entry: CalcRoundEntry,
    usTeamName: String,
    themTeamName: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ShaddahaWhite,
        border = BorderStroke(1.dp, OutlineSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = ShaddahaSurfaceGray,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#${entry.roundNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkDark,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = usTeamName,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                    )
                    Text(
                        text = if (entry.usDelta >= 0) "+${entry.usDelta}" else "${entry.usDelta}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (entry.usDelta > 0) ShaddahaBlue else InkDark,
                    )
                    Text(
                        text = "المجموع ${entry.usTotalAfter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = themTeamName,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                    )
                    Text(
                        text = if (entry.themDelta >= 0) "+${entry.themDelta}" else "${entry.themDelta}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (entry.themDelta > 0) ShaddahaRed else InkDark,
                    )
                    Text(
                        text = "المجموع ${entry.themTotalAfter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                    )
                }
            }

            HorizontalDivider(color = OutlineSoft.copy(alpha = 0.7f))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RoundTagPill(
                    text = entry.modeLabel,
                    containerColor = ShaddahaSurfaceGray,
                    textColor = InkDark,
                )
                if (entry.multiplier > 1) {
                    RoundTagPill(
                        text = "دبل ×${entry.multiplier}",
                        containerColor = ShaddahaYellowSoft,
                        textColor = InkDark,
                    )
                }
                if (entry.modeLabel != "تعديل النشرة") {
                    RoundTagPill(
                        text = "مشتري: ${if (entry.buyerIsUs) usTeamName else themTeamName}",
                        containerColor = ShaddahaSurfaceGray,
                        textColor = InkDark,
                        dotColor = if (entry.buyerIsUs) ShaddahaBlue else ShaddahaRed,
                    )
                }
                if (entry.isKhassara) {
                    RoundTagPill(
                        text = "خسارة",
                        containerColor = ShaddahaRed.copy(alpha = 0.14f),
                        textColor = ShaddahaRed,
                        dotColor = ShaddahaRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundTagPill(
    text: String,
    containerColor: Color,
    textColor: Color,
    dotColor: Color? = null,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddManualRoundSheet(
    usTeamName: String,
    themTeamName: String,
    onDismiss: () -> Unit,
    onAddRound: (
        usPts: Int,
        themPts: Int,
        modeLabel: String,
        buyerIsUs: Boolean,
        multiplier: Int,
        isKhassara: Boolean,
    ) -> Unit,
) {
    var usInput by remember { mutableStateOf("") }
    var themInput by remember { mutableStateOf("") }
    var modeLabel by remember { mutableStateOf("صن") }
    var buyerIsUs by remember { mutableStateOf(true) }
    var multiplier by remember { mutableIntStateOf(1) }
    var isKhassara by remember { mutableStateOf(false) }

    val usVal = usInput.toIntOrNull() ?: 0
    val themVal = themInput.toIntOrNull() ?: 0
    val canSave = usVal > 0 || themVal > 0

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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "إضافة جولة (إدخال يدوي)",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = InkDark,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = usInput,
                    onValueChange = { raw -> usInput = raw.filter { it.isDigit() }.take(3) },
                    label = { Text("نقاط $usTeamName") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_us_input"),
                )
                OutlinedTextField(
                    value = themInput,
                    onValueChange = { raw -> themInput = raw.filter { it.isDigit() }.take(3) },
                    label = { Text("نقاط $themTeamName") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_them_input"),
                )
            }

            Text("نوع اللعب والمشتري:", style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = modeLabel == "صن",
                    onClick = { modeLabel = "صن" },
                    label = { Text("صن") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = modeLabel == "حكم ♠",
                    onClick = { modeLabel = "حكم ♠" },
                    label = { Text("حكم") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = buyerIsUs,
                    onClick = { buyerIsUs = true },
                    label = { Text("مشتري: $usTeamName") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = !buyerIsUs,
                    onClick = { buyerIsUs = false },
                    label = { Text("مشتري: $themTeamName") },
                    modifier = Modifier.weight(1f),
                )
            }

            Text("الدبل والحالة:", style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1 to "عادي", 2 to "دبل ×2", 3 to "×3", 4 to "×4").forEach { (m, label) ->
                    FilterChip(
                        selected = multiplier == m,
                        onClick = { multiplier = m },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                FilterChip(
                    selected = isKhassara,
                    onClick = { isKhassara = !isKhassara },
                    label = { Text("خسارة") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShaddahaRed.copy(alpha = 0.16f),
                        selectedLabelColor = ShaddahaRed,
                    ),
                )
            }

            Button(
                onClick = { onAddRound(usVal, themVal, modeLabel, buyerIsUs, multiplier, isKhassara) },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShaddahaBlue,
                    contentColor = ShaddahaWhite,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("calc_add_round_button"),
            ) {
                Text("حفظ وإضافة الجولة", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
