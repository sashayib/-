package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.AiCoachUiState
import com.example.billing.BillingUiSnapshot
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.example.ui.components.BalootRulesDialog
import com.example.ui.components.MiniWinRateDonutCard
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.RankTierBadge
import com.example.ui.components.RankTierIcon
import com.example.ui.components.ShaddahaRankTier
import com.example.ui.components.TableBackgroundStyle
import com.example.ui.components.TableBackgroundThumbnail
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.TealAccent

@Composable
fun ProfileScreen(
    profileState: UiState<PlayerProfile>,
    aiCoachState: AiCoachUiState,
    isBusy: Boolean,
    avatarUrl: String?,
    googlePhotoUrl: String?,
    selectedTableBackground: TableBackgroundStyle,
    ownedBackgrounds: Set<TableBackgroundStyle>,
    billingSnapshot: BillingUiSnapshot,
    onSelectOrPurchaseBackground: (TableBackgroundStyle) -> Unit,
    onPickGalleryPhoto: (Uri) -> Unit,
    onSelectPresetAvatar: (String) -> Unit,
    onUseCartoonAvatar: () -> Unit,
    onUseGoogleAvatar: () -> Unit,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onAskRulesQuestion: (String) -> Unit,
    onClearAiCoach: () -> Unit,
    onUpdateDisplayName: (String, () -> Unit) -> Unit,
    onSignOut: () -> Unit,
) {
    BackHandler { onBack() }

    val clipboard = LocalClipboardManager.current
    val profile = (profileState as? UiState.Success)?.data
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var copiedPlayerId by remember { mutableStateOf(false) }

    val rating = profile?.rating?.toInt() ?: 22
    val currentTier = ShaddahaRankTier.forRating(rating)
    val nextTier = ShaddahaRankTier.nextTierForRating(rating)
    val progress = if (nextTier == null) {
        1f
    } else {
        val baseFloor = currentTier.minRating
        val span = (nextTier.minRating - baseFloor).coerceAtLeast(1)
        ((rating - baseFloor).toFloat() / span.toFloat()).coerceIn(0f, 1f)
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
                    modifier = Modifier.testTag("profile_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "العودة للرئيسية",
                        tint = ShaddahaWhite,
                    )
                }
                Text(
                    text = "ملفي الشخصي",
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
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ShaddahaSurfaceSoft,
                        border = BorderStroke(1.5.dp, ShaddahaBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .clickable { showAvatarPickerDialog = true }
                                    .testTag("profile_avatar_tap"),
                            ) {
                                PlayerAvatar(
                                    name = profile?.displayName ?: "لاعب",
                                    avatarUrl = avatarUrl,
                                    size = 94.dp,
                                    isHighlighted = true,
                                    preferCartoonAvatar = avatarUrl.isNullOrBlank(),
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = ShaddahaBlue,
                                    border = BorderStroke(2.dp, ShaddahaWhite),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.PhotoCamera,
                                            contentDescription = "تغيير الصورة الشخصية",
                                            tint = ShaddahaWhite,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = { showAvatarPickerDialog = true },
                                modifier = Modifier.testTag("change_avatar_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = null,
                                    tint = ShaddahaBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تغيير الصورة الشخصية",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ShaddahaBlue,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Surface(shape = RoundedCornerShape(10.dp), color = ShaddahaYellow) {
                                    Text(
                                        text = profile?.displayName ?: "لاعب شدها",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = InkDark,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { showEditNameDialog = true },
                                    modifier = Modifier.testTag("edit_display_name_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "تعديل الاسم",
                                        tint = ShaddahaBlue,
                                    )
                                }
                            }
                            RankTierBadge(
                                tierName = currentTier.title,
                                rating = rating,
                            )
                            val myPlayerCode = profile?.effectivePlayerCode()
                            if (!myPlayerCode.isNullOrBlank()) {
                                Surface(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(myPlayerCode))
                                        copiedPlayerId = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ShaddahaBlueSoft,
                                    border = BorderStroke(1.dp, ShaddahaBlue.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("profile_copy_player_id_button"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "معرّف اللاعب: $myPlayerCode",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                            color = ShaddahaBlue,
                                        )
                                        Icon(
                                            imageVector = if (copiedPlayerId) Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
                                            contentDescription = "نسخ معرّف اللاعب",
                                            tint = ShaddahaBlue,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = if (nextTier != null) "التقدّم نحو رتبة (${nextTier.title})" else "وصلت لأعلى رتبة: ملك البلوت",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkMuted,
                                    )
                                    Text(
                                        text = if (nextTier != null) "$rating / ${nextTier.minRating}" else "$rating",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ShaddahaBlue,
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = ShaddahaBlue,
                                    trackColor = ShaddahaBlueSoft,
                                )
                            }
                        }
                    }
                }

                // Personal Win-Rate Circular Chart Summary
                item {
                    MiniWinRateDonutCard(
                        wins = (profile?.wins ?: 0L).toInt(),
                        losses = (profile?.losses ?: 0L).toInt(),
                        matchesPlayed = (profile?.matchesPlayed ?: 0L).toInt(),
                        onOpenFullStats = onOpenHistory,
                    )
                }

                // Table / Session Background Settings
                item {
                    ProfileTableBackgroundSettingsCard(
                        selectedBackground = selectedTableBackground,
                        ownedBackgrounds = ownedBackgrounds,
                        billingSnapshot = billingSnapshot,
                        onSelectOrPurchase = onSelectOrPurchaseBackground,
                    )
                }

                // 10 Rank Tiers Ladder
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ShaddahaSurfaceSoft,
                        border = BorderStroke(1.dp, OutlineSoft),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_rank_tiers_card"),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "التصنيفات والرتب",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = InkDark,
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ShaddahaBlueSoft,
                                ) {
                                    Text(
                                        text = "${ShaddahaRankTier.entries.size} رتب",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ShaddahaBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            ShaddahaRankTier.entries.forEach { tier ->
                                val isCurrent = tier == currentTier
                                val isUnlocked = rating >= tier.minRating
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isCurrent -> tier.accentColor.copy(alpha = 0.12f)
                                        isUnlocked -> ShaddahaWhite
                                        else -> ShaddahaWhite.copy(alpha = 0.65f)
                                    },
                                    border = BorderStroke(
                                        width = if (isCurrent) 1.8.dp else 1.dp,
                                        color = if (isCurrent) tier.accentColor else OutlineSoft,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rank_tier_${tier.name.lowercase()}"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = tier.accentColor.copy(alpha = if (isUnlocked) 0.16f else 0.08f),
                                            modifier = Modifier.size(42.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                RankTierIcon(
                                                    tier = tier,
                                                    size = 24.dp,
                                                    tintOverride = if (isUnlocked) tier.accentColor else tier.accentColor.copy(alpha = 0.55f),
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tier.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (isUnlocked) InkDark else InkMuted,
                                            )
                                            Text(
                                                text = tier.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InkMuted,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = when {
                                                    isCurrent -> tier.accentColor
                                                    isUnlocked -> TealAccent.copy(alpha = 0.14f)
                                                    else -> OutlineSoft.copy(alpha = 0.5f)
                                                },
                                            ) {
                                                Text(
                                                    text = when {
                                                        isCurrent -> "رتبتك الآن"
                                                        isUnlocked -> "مفتوح ✓"
                                                        else -> "${tier.minRating}+ نقطة"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = when {
                                                        isCurrent -> ShaddahaWhite
                                                        isUnlocked -> TealAccent
                                                        else -> InkMuted
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = onOpenHistory,
                        shape = RoundedCornerShape(18.dp),
                        color = ShaddahaSurfaceSoft,
                        border = BorderStroke(1.dp, OutlineSoft),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_history_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(imageVector = Icons.Rounded.History, contentDescription = null, tint = ShaddahaBlue)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "سجل المباريات والإحصائيات",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = InkDark,
                                )
                                Text(
                                    text = "استعرض انتصاراتك وتفاصيل النقاط",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { showRulesDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        color = ShaddahaSurfaceSoft,
                        border = BorderStroke(1.dp, OutlineSoft),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baloot_rules_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(imageVector = Icons.Rounded.MenuBook, contentDescription = null, tint = ShaddahaBlue)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "دليل قوانين البلوت في شدها",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = InkDark,
                                )
                                Text(
                                    text = "ترتيب الأوراق في الصن والحكم، المشاريع، الكبوت والخسارة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { showAiAdvisorDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        color = ShaddahaBlueSoft,
                        border = BorderStroke(1.2.dp, ShaddahaBlue.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_ai_rules_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null, tint = ShaddahaBlue)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "اسأل مستشار البلوت الذكي",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ShaddahaBlue,
                                )
                                Text(
                                    text = "استفسر عن أي قانون أو حسبة أو استراتيجية لعب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, ShaddahaRed.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShaddahaRed),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(profile?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("تعديل اسم اللاعب") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(40) },
                    label = { Text("الاسم المستعار") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDisplayName(nameInput) { showEditNameDialog = false }
                    },
                    enabled = nameInput.trim().isNotEmpty() && !isBusy,
                    modifier = Modifier.testTag("confirm_edit_name_button"),
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("إلغاء") }
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
private fun ProfileTableBackgroundSettingsCard(
    selectedBackground: TableBackgroundStyle,
    ownedBackgrounds: Set<TableBackgroundStyle>,
    billingSnapshot: BillingUiSnapshot,
    onSelectOrPurchase: (TableBackgroundStyle) -> Unit,
) {
    val allBackgrounds = TableBackgroundStyle.freeBackgrounds + TableBackgroundStyle.storeBackgrounds

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.2.dp, ShaddahaBlue.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_background_settings_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        imageVector = Icons.Rounded.Wallpaper,
                        contentDescription = null,
                        tint = ShaddahaBlue,
                    )
                    Column {
                        Text(
                            text = "إعدادات خلفية طاولة اللعب",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                        )
                        Text(
                            text = "اختر خلفية الجلسة المفضلة لديك بحرية",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                        )
                    }
                }
            }

            allBackgrounds.forEach { bg ->
                val isSelected = selectedBackground == bg
                val isUnlocked = bg.priceSar == 0 || bg in ownedBackgrounds
                val priceLabel = billingSnapshot.backgroundPrices[bg] ?: bg.priceLabel
                Surface(
                    onClick = { onSelectOrPurchase(bg) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ShaddahaBlueSoft else ShaddahaWhite,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ShaddahaBlue else OutlineSoft,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_bg_option_${bg.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
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
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isUnlocked) TealAccent.copy(alpha = 0.14f) else ShaddahaYellow.copy(alpha = 0.45f),
                            ) {
                                Text(
                                    text = if (isUnlocked) {
                                        if (bg.priceSar == 0) "متاحة مجاناً" else "مملوكة ✓"
                                    } else {
                                        "من المتجر • $priceLabel"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isUnlocked) TealAccent else InkDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Button(
                            onClick = { onSelectOrPurchase(bg) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSelected -> TealAccent
                                    isUnlocked -> ShaddahaBlue
                                    else -> ShaddahaYellow
                                },
                                contentColor = if (!isUnlocked && !isSelected) InkDark else ShaddahaWhite,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = when {
                                    isSelected -> "مُفعّلة ✓"
                                    isUnlocked -> "اختيار"
                                    else -> "شراء"
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

@Composable
fun AvatarPickerDialog(
    currentAvatarUrl: String?,
    googlePhotoUrl: String?,
    onPickGalleryPhoto: (Uri) -> Unit,
    onSelectPresetAvatar: (String) -> Unit,
    onUseCartoonAvatar: () -> Unit,
    onUseGoogleAvatar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onPickGalleryPhoto(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.PhotoCamera, contentDescription = null, tint = ShaddahaBlue)
                Text("تغيير الصورة الشخصية")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "اختر شخصية جاهزة:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = InkDark,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AvatarStorage.presetOptions.chunked(3).forEachIndexed { rowIdx, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEachIndexed { colIdx, (presetKey, resId) ->
                                val itemNumber = rowIdx * 3 + colIdx + 1
                                val isSelected = currentAvatarUrl == presetKey
                                Surface(
                                    onClick = { onSelectPresetAvatar(presetKey) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) ShaddahaBlueSoft else ShaddahaWhite,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) ShaddahaBlue else OutlineSoft,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("preset_avatar_$itemNumber"),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = "شخصية $itemNumber",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(58.dp)
                                                .clip(CircleShape)
                                                .background(ShaddahaWhite)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) ShaddahaBlue else OutlineSoft,
                                                    shape = CircleShape,
                                                ),
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "محددة",
                                                tint = ShaddahaBlue,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(18.dp)
                                                    .background(ShaddahaWhite, CircleShape),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = ShaddahaBlueSoft,
                    border = BorderStroke(1.2.dp, ShaddahaBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_gallery_avatar_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(imageVector = Icons.Rounded.PhotoLibrary, contentDescription = null, tint = ShaddahaBlue)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("اختيار صورة من الجهاز", fontWeight = FontWeight.Bold, color = InkDark)
                            Text("اختر صورتك الشخصية من ألبوم الصور", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                    }
                }

                Surface(
                    onClick = onUseCartoonAvatar,
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentAvatarUrl.isNullOrBlank()) ShaddahaBlueSoft else ShaddahaSurfaceSoft,
                    border = BorderStroke(1.dp, if (currentAvatarUrl.isNullOrBlank()) ShaddahaBlue else OutlineSoft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("use_cartoon_avatar_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shaddaha_avatar),
                            contentDescription = "شخصية شدها",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("شخصية شدها الكلاسيكية", fontWeight = FontWeight.Bold, color = InkDark)
                            Text("الأفاتار الرسمي للعبة شدها", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                        Icon(imageVector = Icons.Rounded.Face, contentDescription = null, tint = ShaddahaBlue)
                    }
                }

                if (!googlePhotoUrl.isNullOrBlank()) {
                    Surface(
                        onClick = onUseGoogleAvatar,
                        shape = RoundedCornerShape(14.dp),
                        color = if (currentAvatarUrl == googlePhotoUrl) ShaddahaBlueSoft else ShaddahaSurfaceSoft,
                        border = BorderStroke(1.dp, if (currentAvatarUrl == googlePhotoUrl) ShaddahaBlue else OutlineSoft),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_google_avatar_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AsyncImage(
                                model = googlePhotoUrl,
                                contentDescription = "صورة Google",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("صورة حساب Google", fontWeight = FontWeight.Bold, color = InkDark)
                                Text("استخدام صورة ملفك الشخصي في Google", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                            }
                            Icon(imageVector = Icons.Rounded.AccountCircle, contentDescription = null, tint = ShaddahaBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
    )
}
