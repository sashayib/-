package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.R
import com.example.game.BalootCard
import com.example.game.Rank
import com.example.game.Suit
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaSurfaceGray
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowWave
import com.example.ui.theme.SuitBlack
import com.example.ui.theme.SuitRed
import com.example.ui.theme.TierBronze
import com.example.ui.theme.TierGold
import com.example.ui.theme.TierLegend
import com.example.ui.theme.TierPlatinum
import com.example.ui.theme.TierSilver

enum class CardDeckStyle(
    val title: String,
    val priceLabel: String,
    val priceSar: Int,
    val faceBackground: Color,
    val faceBorder: Color,
    val faceInnerAccent: Color,
    val backPrimary: Color,
    val backSecondary: Color,
    val backTrim: Color,
    val badgeColor: Color,
) {
    CLASSIC(
        title = "الورق الكلاسيكي",
        priceLabel = "مجاني",
        priceSar = 0,
        faceBackground = Color(0xFFFFFFFF),
        faceBorder = Color(0xFFD5DBE8),
        faceInnerAccent = Color(0xFF002BFF),
        backPrimary = Color(0xFF002BFF),
        backSecondary = Color(0xFF001EC2),
        backTrim = Color(0xFFFFD629),
        badgeColor = Color(0xFF002BFF),
    ),
    DIAMOND(
        title = "الورق الألماسي",
        priceLabel = "20 ريال",
        priceSar = 20,
        faceBackground = Color(0xFFEAF8FF),
        faceBorder = Color(0xFF0EA5E9),
        faceInnerAccent = Color(0xFF0284C7),
        backPrimary = Color(0xFF0284C7),
        backSecondary = Color(0xFF0369A1),
        backTrim = Color(0xFFBAE6FD),
        badgeColor = Color(0xFF0284C7),
    ),
    GOLD(
        title = "الورق الذهبي",
        priceLabel = "10 ريال",
        priceSar = 10,
        faceBackground = Color(0xFFFFF8DC),
        faceBorder = Color(0xFFD97706),
        faceInnerAccent = Color(0xFFB45309),
        backPrimary = Color(0xFFD97706),
        backSecondary = Color(0xFF92400E),
        backTrim = Color(0xFFFDE047),
        badgeColor = Color(0xFFD97706),
    ),
    SILVER(
        title = "الورق الفضي",
        priceLabel = "7 ريال",
        priceSar = 7,
        faceBackground = Color(0xFFF1F5F9),
        faceBorder = Color(0xFF94A3B8),
        faceInnerAccent = Color(0xFF475569),
        backPrimary = Color(0xFF64748B),
        backSecondary = Color(0xFF334155),
        backTrim = Color(0xFFE2E8F0),
        badgeColor = Color(0xFF64748B),
    ),
    BRONZE(
        title = "الورق البرونزي",
        priceLabel = "5 ريال",
        priceSar = 5,
        faceBackground = Color(0xFFFDF1E4),
        faceBorder = Color(0xFFCD7F32),
        faceInnerAccent = Color(0xFF8C4A14),
        backPrimary = Color(0xFFA85C1B),
        backSecondary = Color(0xFF6F380B),
        backTrim = Color(0xFFFDBA74),
        badgeColor = Color(0xFFCD7F32),
    ),
}

enum class ShaddahaRankTier(
    val title: String,
    val subtitle: String,
    val minRating: Int,
    val accentColor: Color,
    val icon: ImageVector?,
) {
    MUBTADI(
        title = "مبتدئ",
        subtitle = "بذرة البداية في مجالس شدها",
        minRating = 0,
        accentColor = Color(0xFF10B981),
        icon = Icons.Rounded.Eco,
    ),
    NASHI(
        title = "ناشئ",
        subtitle = "خطوات صاعدة نحو الإتقان",
        minRating = 2000,
        accentColor = Color(0xFF0EA5E9),
        icon = Icons.Rounded.TrendingUp,
    ),
    HAWI(
        title = "هاوي",
        subtitle = "شغف مستمر بأوراق البلوت",
        minRating = 4000,
        accentColor = Color(0xFF2563EB),
        icon = Icons.Rounded.Style,
    ),
    LAEB(
        title = "لاعب",
        subtitle = "حضور ثابت ومنافسة قوية",
        minRating = 6000,
        accentColor = Color(0xFF4F46E5),
        icon = Icons.Rounded.SportsEsports,
    ),
    MUTAMARRIS(
        title = "متمرس",
        subtitle = "قراءة ذكية للعب والمشاريع",
        minRating = 8000,
        accentColor = Color(0xFF0D9488),
        icon = null, // Custom Brain ("عقل") vector drawn on Canvas
    ),
    SINDEED(
        title = "صنديد",
        subtitle = "ثبات وجرأة في الدبل والرد",
        minRating = 10000,
        accentColor = Color(0xFFCD7F32),
        icon = null, // Custom Flexed Muscle ("عضلات") vector drawn on Canvas
    ),
    DHEEBAN(
        title = "ذيبان",
        subtitle = "فطنة وسرعة بديهة في الطاولة",
        minRating = 12000,
        accentColor = Color(0xFFD97706),
        icon = null, // Custom Wolf ("ذيب") vector drawn on Canvas
    ),
    NAUKHADHA(
        title = "نوخذة",
        subtitle = "ربّان مخضرم يقود الفوز بثقة",
        minRating = 14000,
        accentColor = Color(0xFF0369A1),
        icon = Icons.Rounded.Sailing,
    ),
    ZAEEM(
        title = "زعيم",
        subtitle = "هيبة الصدارة ووسام القيادة",
        minRating = 16000,
        accentColor = Color(0xFF9333EA),
        icon = Icons.Rounded.MilitaryTech,
    ),
    MALIK(
        title = "ملك البلوت",
        subtitle = "تاج القمة وأعلى عرش في شدها",
        minRating = 18000,
        accentColor = Color(0xFFEAB308),
        icon = null, // Custom Crown vector drawn on Canvas
    );

    companion object {
        fun forRating(rating: Int): ShaddahaRankTier =
            entries.lastOrNull { rating >= it.minRating } ?: MUBTADI

        fun nextTierForRating(rating: Int): ShaddahaRankTier? =
            entries.firstOrNull { it.minRating > rating }
    }
}

fun tierColor(rating: Int): Color = ShaddahaRankTier.forRating(rating).accentColor

@Composable
fun RankTierIcon(
    tier: ShaddahaRankTier,
    size: Dp = 20.dp,
    tintOverride: Color? = null,
) {
    val color = tintOverride ?: tier.accentColor
    val vector = tier.icon
    if (vector != null) {
        Icon(
            imageVector = vector,
            contentDescription = tier.title,
            tint = color,
            modifier = Modifier.size(size),
        )
    } else {
        Canvas(
            modifier = Modifier
                .size(size)
                .semantics { contentDescription = tier.title },
        ) {
            val w = this.size.width
            val h = this.size.height
            when (tier) {
                ShaddahaRankTier.MUTAMARRIS -> {
                    // Brain ("عقل"): left & right hemispheres with cortical folds
                    val leftLobe = Path().apply {
                        moveTo(w * 0.47f, h * 0.22f)
                        cubicTo(w * 0.44f, h * 0.13f, w * 0.24f, h * 0.13f, w * 0.22f, h * 0.28f)
                        cubicTo(w * 0.11f, h * 0.30f, w * 0.10f, h * 0.50f, w * 0.17f, h * 0.57f)
                        cubicTo(w * 0.14f, h * 0.72f, w * 0.28f, h * 0.80f, w * 0.40f, h * 0.76f)
                        cubicTo(w * 0.45f, h * 0.78f, w * 0.47f, h * 0.72f, w * 0.47f, h * 0.64f)
                        close()
                    }
                    val rightLobe = Path().apply {
                        moveTo(w * 0.53f, h * 0.22f)
                        cubicTo(w * 0.56f, h * 0.13f, w * 0.76f, h * 0.13f, w * 0.78f, h * 0.28f)
                        cubicTo(w * 0.89f, h * 0.30f, w * 0.90f, h * 0.50f, w * 0.83f, h * 0.57f)
                        cubicTo(w * 0.86f, h * 0.72f, w * 0.72f, h * 0.80f, w * 0.60f, h * 0.76f)
                        cubicTo(w * 0.55f, h * 0.78f, w * 0.53f, h * 0.72f, w * 0.53f, h * 0.64f)
                        close()
                    }
                    drawPath(path = leftLobe, color = color, style = Fill)
                    drawPath(path = rightLobe, color = color, style = Fill)
                    // Brainstem base
                    drawLine(
                        color = color,
                        start = Offset(w * 0.50f, h * 0.73f),
                        end = Offset(w * 0.50f, h * 0.88f),
                        strokeWidth = w * 0.12f,
                        cap = StrokeCap.Round,
                    )
                    // Cortical fold accents
                    val foldStroke = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
                    val leftFold = Path().apply {
                        moveTo(w * 0.26f, h * 0.36f)
                        quadraticTo(w * 0.36f, h * 0.32f, w * 0.38f, h * 0.44f)
                        moveTo(w * 0.24f, h * 0.58f)
                        quadraticTo(w * 0.34f, h * 0.53f, w * 0.40f, h * 0.62f)
                    }
                    val rightFold = Path().apply {
                        moveTo(w * 0.74f, h * 0.36f)
                        quadraticTo(w * 0.64f, h * 0.32f, w * 0.62f, h * 0.44f)
                        moveTo(w * 0.76f, h * 0.58f)
                        quadraticTo(w * 0.66f, h * 0.53f, w * 0.60f, h * 0.62f)
                    }
                    drawPath(path = leftFold, color = Color.White.copy(alpha = 0.82f), style = foldStroke)
                    drawPath(path = rightFold, color = Color.White.copy(alpha = 0.82f), style = foldStroke)
                }
                ShaddahaRankTier.SINDEED -> {
                    // Flexed Muscle Arm ("عضلات")
                    val armPath = Path().apply {
                        // Start at curled fist top-left
                        moveTo(w * 0.24f, h * 0.22f)
                        cubicTo(w * 0.16f, h * 0.22f, w * 0.14f, h * 0.35f, w * 0.22f, h * 0.40f)
                        // Forearm down to elbow at bottom-left
                        lineTo(w * 0.15f, h * 0.66f)
                        cubicTo(w * 0.12f, h * 0.78f, w * 0.22f, h * 0.85f, w * 0.36f, h * 0.85f)
                        // Lower arm across to shoulder base on right
                        lineTo(w * 0.78f, h * 0.85f)
                        cubicTo(w * 0.88f, h * 0.85f, w * 0.90f, h * 0.62f, w * 0.78f, h * 0.54f)
                        // Bicep bulge dome
                        cubicTo(w * 0.74f, h * 0.37f, w * 0.52f, h * 0.34f, w * 0.42f, h * 0.50f)
                        // Inner elbow fold back up to fist
                        lineTo(w * 0.35f, h * 0.38f)
                        cubicTo(w * 0.42f, h * 0.31f, w * 0.38f, h * 0.20f, w * 0.24f, h * 0.22f)
                        close()
                    }
                    drawPath(path = armPath, color = color, style = Fill)
                    // Muscle definition crease line
                    val crease = Path().apply {
                        moveTo(w * 0.44f, h * 0.56f)
                        quadraticTo(w * 0.56f, h * 0.46f, w * 0.68f, h * 0.54f)
                    }
                    drawPath(
                        path = crease,
                        color = Color.White.copy(alpha = 0.82f),
                        style = Stroke(width = w * 0.06f, cap = StrokeCap.Round),
                    )
                }
                ShaddahaRankTier.DHEEBAN -> {
                    // Wolf Head ("ذيب")
                    val wolfPath = Path().apply {
                        moveTo(w * 0.50f, h * 0.25f) // Forehead center
                        lineTo(w * 0.34f, h * 0.24f)
                        lineTo(w * 0.20f, h * 0.10f) // Left ear tip
                        lineTo(w * 0.18f, h * 0.38f)
                        lineTo(w * 0.10f, h * 0.52f) // Left cheek fur tuft
                        lineTo(w * 0.22f, h * 0.62f)
                        lineTo(w * 0.18f, h * 0.70f)
                        lineTo(w * 0.36f, h * 0.76f)
                        lineTo(w * 0.50f, h * 0.90f) // Snout tip / chin
                        lineTo(w * 0.64f, h * 0.76f)
                        lineTo(w * 0.82f, h * 0.70f)
                        lineTo(w * 0.78f, h * 0.62f)
                        lineTo(w * 0.90f, h * 0.52f) // Right cheek fur tuft
                        lineTo(w * 0.82f, h * 0.38f)
                        lineTo(w * 0.80f, h * 0.10f) // Right ear tip
                        lineTo(w * 0.66f, h * 0.24f)
                        close()
                    }
                    drawPath(path = wolfPath, color = color, style = Fill)
                    // Fierce angled wolf eyes
                    val leftEye = Path().apply {
                        moveTo(w * 0.28f, h * 0.46f)
                        lineTo(w * 0.42f, h * 0.51f)
                        lineTo(w * 0.33f, h * 0.55f)
                        close()
                    }
                    val rightEye = Path().apply {
                        moveTo(w * 0.72f, h * 0.46f)
                        lineTo(w * 0.58f, h * 0.51f)
                        lineTo(w * 0.67f, h * 0.55f)
                        close()
                    }
                    drawPath(path = leftEye, color = Color.White, style = Fill)
                    drawPath(path = rightEye, color = Color.White, style = Fill)
                    // Wolf snout & nose accent
                    val nose = Path().apply {
                        moveTo(w * 0.43f, h * 0.70f)
                        lineTo(w * 0.57f, h * 0.70f)
                        lineTo(w * 0.50f, h * 0.79f)
                        close()
                    }
                    drawPath(
                        path = nose,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = w * 0.045f, join = StrokeJoin.Round),
                    )
                }
                else -> {
                    // Golden Crown for "ملك البلوت"
                    val path = Path().apply {
                        moveTo(w * 0.12f, h * 0.78f)
                        lineTo(w * 0.08f, h * 0.30f)
                        lineTo(w * 0.32f, h * 0.52f)
                        lineTo(w * 0.50f, h * 0.16f)
                        lineTo(w * 0.68f, h * 0.52f)
                        lineTo(w * 0.92f, h * 0.30f)
                        lineTo(w * 0.88f, h * 0.78f)
                        close()
                    }
                    drawPath(path = path, color = color, style = Fill)
                    drawLine(
                        color = color,
                        start = Offset(w * 0.14f, h * 0.86f),
                        end = Offset(w * 0.86f, h * 0.86f),
                        strokeWidth = h * 0.10f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = color, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.16f))
                    drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.08f, h * 0.28f))
                    drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.92f, h * 0.28f))
                }
            }
        }
    }
}

@Composable
fun RankTierBadge(
    tierName: String,
    rating: Int,
    modifier: Modifier = Modifier,
) {
    val tier = ShaddahaRankTier.forRating(rating)
    val accent = tier.accentColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RankTierIcon(tier = tier, size = 16.dp)
            Text(
                text = "${tierName.ifBlank { tier.title }} • $rating",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun PlayerAvatar(
    name: String,
    avatarUrl: String?,
    size: Dp = 44.dp,
    isBot: Boolean = false,
    isHighlighted: Boolean = false,
    preferCartoonAvatar: Boolean = false,
    borderColorOverride: Color? = null,
) {
    val borderColor by animateColorAsState(
        targetValue = borderColorOverride ?: if (isHighlighted) ShaddahaBlue else OutlineSoft,
        label = "avatarBorder",
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ShaddahaSurfaceGray)
            .border(if (isHighlighted) 2.5.dp else 1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val presetRes = com.example.ui.AvatarStorage.presetDrawableRes(avatarUrl)
        when {
            isBot -> Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = name,
                tint = ShaddahaBlue,
                modifier = Modifier.size(size * 0.54f),
            )
            presetRes != null -> Image(
                painter = painterResource(id = presetRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ShaddahaWhite),
            )
            avatarUrl.isNullOrBlank() || (preferCartoonAvatar && avatarUrl == "cartoon") -> Image(
                painter = painterResource(id = R.drawable.shaddaha_avatar),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                placeholder = painterResource(id = R.drawable.shaddaha_avatar),
                error = painterResource(id = R.drawable.shaddaha_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun BalootCardView(
    card: BalootCard,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 94.dp,
    isLegal: Boolean = true,
    isPlayableTurn: Boolean = false,
    isSelected: Boolean = false,
    isThrowing: Boolean = false,
    deckStyle: CardDeckStyle = CardDeckStyle.CLASSIC,
    onClick: (() -> Unit)? = null,
) {
    val suitColor = if (card.suit.isRed) SuitRed else SuitBlack
    val elevation by animateDpAsState(
        targetValue = when {
            isThrowing -> 18.dp
            isSelected -> 14.dp
            isPlayableTurn && isLegal -> 6.dp
            else -> 3.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardElevation",
    )
    val liftY by animateDpAsState(
        targetValue = when {
            isThrowing -> (-135).dp
            isSelected -> (-20).dp
            else -> 0.dp
        },
        animationSpec = if (isThrowing) {
            tween(durationMillis = 190, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.62f, stiffness = 420f)
        },
        label = "cardLift",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isThrowing -> 0.72f
            isSelected -> 1.07f
            else -> 1f
        },
        animationSpec = if (isThrowing) {
            tween(durationMillis = 190, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.65f, stiffness = 420f)
        },
        label = "cardScale",
    )
    val rotation by animateFloatAsState(
        targetValue = when {
            isThrowing -> 15f
            isSelected -> -2.5f
            else -> 0f
        },
        animationSpec = if (isThrowing) {
            tween(durationMillis = 190, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.65f, stiffness = 380f)
        },
        label = "cardRotation",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = when {
            isThrowing -> 0.08f
            isPlayableTurn && !isLegal -> 0.42f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 175),
        label = "cardAlpha",
    )
    val borderStroke = when {
        isSelected -> BorderStroke(2.8.dp, ShaddahaYellow)
        isPlayableTurn && isLegal -> BorderStroke(2.dp, if (deckStyle == CardDeckStyle.CLASSIC) ShaddahaBlue else deckStyle.faceBorder)
        deckStyle != CardDeckStyle.CLASSIC -> BorderStroke(1.8.dp, deckStyle.faceBorder)
        else -> BorderStroke(1.2.dp, deckStyle.faceBorder)
    }

    Surface(
        modifier = modifier
            .zIndex(if (isThrowing) 10f else if (isSelected) 4f else 0f)
            .offset(y = liftY)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                alpha = cardAlpha
            }
            .width(width)
            .height(height)
            .shadow(elevation, RoundedCornerShape(12.dp))
            .then(
                if (onClick != null && !isThrowing && (!isPlayableTurn || isLegal)) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag("card_${card.code}"),
        shape = RoundedCornerShape(12.dp),
        color = deckStyle.faceBackground,
        border = borderStroke,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .then(
                    if (deckStyle != CardDeckStyle.CLASSIC) {
                        Modifier.border(
                            width = 1.dp,
                            color = deckStyle.faceInnerAccent.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(9.dp),
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 3.dp, vertical = 2.dp),
        ) {
            val cornerRankSize = (width.value * 0.21f).coerceIn(13f, 17f).sp
            val cornerSuitSize = (width.value * 0.19f).coerceIn(12f, 15f).sp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    text = card.rank.label,
                    fontSize = cornerRankSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = suitColor,
                    lineHeight = cornerRankSize,
                )
                Text(
                    text = card.suit.symbol,
                    fontSize = cornerSuitSize,
                    color = suitColor,
                    lineHeight = cornerSuitSize,
                )
            }

            Text(
                text = card.suit.symbol,
                fontSize = (width.value * 0.42f).sp,
                color = suitColor,
                modifier = Modifier.align(Alignment.Center),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Text(
                    text = card.rank.label,
                    fontSize = (width.value * 0.18f).coerceIn(12f, 15f).sp,
                    fontWeight = FontWeight.Bold,
                    color = suitColor,
                    lineHeight = (width.value * 0.18f).coerceIn(12f, 15f).sp,
                )
            }
        }
    }
}

@Composable
fun BalootCardBack(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 46.dp,
    deckStyle: CardDeckStyle = CardDeckStyle.CLASSIC,
) {
    Surface(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(7.dp),
        color = deckStyle.backPrimary,
        border = BorderStroke(1.5.dp, ShaddahaWhite),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(deckStyle.backPrimary, deckStyle.backSecondary)
                    )
                )
                .padding(3.dp)
                .border(1.2.dp, deckStyle.backTrim.copy(alpha = 0.9f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.shaddaha_logo),
                contentDescription = "ظهر الورقة",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = ShaddahaYellowRichIcon,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = InkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
    }
}

private val ShaddahaYellowRichIcon = Color(0xFFE6B400)

@Composable
fun YellowWavePatternBackground(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.background(ShaddahaYellow)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)

        val p1 = Path().apply {
            moveTo(-w * 0.1f, h * 0.25f)
            cubicTo(w * 0.25f, h * 0.05f, w * 0.45f, h * 0.55f, w * 0.85f, h * 0.22f)
            cubicTo(w * 1.0f, h * 0.10f, w * 1.1f, h * 0.35f, w * 1.15f, h * 0.45f)
        }
        drawPath(path = p1, color = ShaddahaYellowWave.copy(alpha = 0.55f), style = stroke)

        val p2 = Path().apply {
            moveTo(-w * 0.05f, h * 0.72f)
            cubicTo(w * 0.3f, h * 0.48f, w * 0.55f, h * 0.92f, w * 1.05f, h * 0.65f)
        }
        drawPath(path = p2, color = ShaddahaYellowWave.copy(alpha = 0.5f), style = stroke)

        drawCircle(
            color = ShaddahaYellowWave.copy(alpha = 0.42f),
            radius = w * 0.16f,
            center = Offset(w * 0.14f, h * 0.46f),
            style = stroke,
        )
    }
}

@Composable
fun WinkingSmileyIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.12f).coerceAtLeast(2.dp.toPx())

        drawCircle(
            color = tint,
            radius = w * 0.09f,
            center = Offset(w * 0.32f, h * 0.34f),
        )
        val wink = Path().apply {
            moveTo(w * 0.56f, h * 0.28f)
            lineTo(w * 0.72f, h * 0.36f)
            lineTo(w * 0.56f, h * 0.44f)
        }
        drawPath(wink, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

        val smile = Path().apply {
            moveTo(w * 0.25f, h * 0.60f)
            quadraticTo(w * 0.52f, h * 0.84f, w * 0.76f, h * 0.58f)
        }
        drawPath(smile, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}

@Composable
fun BalootRulesDialog(
    onDismiss: () -> Unit,
) {
    val sunRanks = Rank.entries.sortedByDescending { it.sunOrder }
    val hokmRanks = Rank.entries.sortedByDescending { it.hokmOrder }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = ShaddahaWhite,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "قوانين البلوت الأساسية",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = ShaddahaBlue,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("rules_dialog_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "إغلاق",
                            tint = InkMuted,
                        )
                    }
                }

                // 1. Sun Card Order with Real Playing Cards
                RulesCardOrderSection(
                    title = "الصن (26 نقطة)",
                    subtitle = "ترتيب القوة من الأقوى إلى الأضعف",
                    ranks = sunRanks,
                    suit = Suit.SPADES,
                    pointsProvider = { it.sunPoints },
                    badgeColor = ShaddahaBlueSoft,
                    badgeTextColor = ShaddahaBlue,
                )

                // 2. Hokm Card Order with Real Playing Cards
                RulesCardOrderSection(
                    title = "الحكم (16 نقطة) — ورق الحكم",
                    subtitle = "في غير نوع الحكم يتبع ترتيب الصن",
                    ranks = hokmRanks,
                    suit = Suit.HEARTS,
                    pointsProvider = { it.hokmPoints },
                    badgeColor = ShaddahaYellow.copy(alpha = 0.45f),
                    badgeTextColor = InkDark,
                )

                // 3. Projects & Kaboot summary
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ShaddahaSurfaceSoft,
                    border = BorderStroke(1.dp, OutlineSoft),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "• المشاريع: سرا (3 متتالية)، خمسين (4 متتالية)، مية (5 متتالية أو 4 متشابهة)، أربعمية (4 إكات في الصن)، بلوت (الشايب + البنت من الحكم).",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkDark,
                        )
                        Text(
                            text = "• الكبوت: أكل جميع اللفات الثمان يمنح 44 نقطة في الصن أو 25 نقطة في الحكم.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkDark,
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rules_dialog_confirm"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShaddahaBlue),
                ) {
                    Text(
                        text = "فهمت",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ShaddahaWhite,
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesCardOrderSection(
    title: String,
    subtitle: String,
    ranks: List<Rank>,
    suit: Suit,
    pointsProvider: (Rank) -> Int,
    badgeColor: Color,
    badgeTextColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.dp, OutlineSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkDark,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor,
                ) {
                    Text(
                        text = "الأقوى ← الأضعف",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gap = 4.dp
                val cardWidth = ((maxWidth - (gap * 7)) / 8f).coerceIn(28.dp, 42.dp)
                val cardHeight = cardWidth * 1.44f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                ) {
                    ranks.forEach { rank ->
                        val pts = pointsProvider(rank)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            MiniRulePlayingCard(
                                rank = rank,
                                suit = suit,
                                width = cardWidth,
                                height = cardHeight,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (pts > 0) badgeColor else ShaddahaSurfaceGray,
                            ) {
                                Text(
                                    text = if (pts > 0) "$pts" else "٠",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                    ),
                                    color = if (pts > 0) badgeTextColor else InkMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun MiniRulePlayingCard(
    rank: Rank,
    suit: Suit,
    width: Dp,
    height: Dp,
) {
    val suitColor = if (suit.isRed) SuitRed else SuitBlack
    val rankFont = (width.value * 0.30f).coerceIn(9f, 13f).sp
    val cornerSuitFont = (width.value * 0.25f).coerceIn(8f, 11f).sp
    val centerSuitFont = (width.value * 0.44f).coerceIn(13f, 20f).sp

    Surface(
        modifier = Modifier
            .width(width)
            .height(height)
            .shadow(2.dp, RoundedCornerShape(7.dp)),
        shape = RoundedCornerShape(7.dp),
        color = ShaddahaWhite,
        border = BorderStroke(1.dp, Color(0xFFD0D7E5)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.5.dp, vertical = 2.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    text = rank.label,
                    fontSize = rankFont,
                    fontWeight = FontWeight.ExtraBold,
                    color = suitColor,
                    lineHeight = rankFont,
                )
                Text(
                    text = suit.symbol,
                    fontSize = cornerSuitFont,
                    color = suitColor,
                    lineHeight = cornerSuitFont,
                )
            }
            Text(
                text = suit.symbol,
                fontSize = centerSuitFont,
                color = suitColor,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
