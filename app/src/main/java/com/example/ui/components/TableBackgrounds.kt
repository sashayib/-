package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.TealAccent

enum class TableBackgroundStyle(
    val title: String,
    val subtitle: String,
    val priceSar: Int,
    val isLightBackground: Boolean,
    val isStoreBackground: Boolean,
    val accentColor: Color,
) {
    CLASSIC_WHITE(
        title = "خلفية شدها البيضاء",
        subtitle = "خلفية بيضاء ناصعة مع شعار شدها الكلاسيكي",
        priceSar = 0,
        isLightBackground = true,
        isStoreBackground = false,
        accentColor = Color(0xFF002BFF),
    ),
    CLASSIC_GREEN(
        title = "خلفية المجلس الخضراء",
        subtitle = "بساط أخضر شبكي تقليدي مع شعار شدها بالوسط",
        priceSar = 0,
        isLightBackground = false,
        isStoreBackground = false,
        accentColor = Color(0xFF0D7A32),
    ),
    SAUDI_GOLD(
        title = "الخلفية الذهبية السعودية",
        subtitle = "لون ذهبي ملكي مع شعار السيفين والنخلة الأبيض",
        priceSar = 20,
        isLightBackground = false,
        isStoreBackground = true,
        accentColor = Color(0xFFD2A700),
    ),
    SAUDI_ROYAL_DARK_GOLD(
        title = "خلفية المملكة الذهبية",
        subtitle = "أخضر ملكي بخطوط بيضاء وشعار السيفين والنخلة الذهبي",
        priceSar = 20,
        isLightBackground = false,
        isStoreBackground = true,
        accentColor = Color(0xFF08421E),
    ),
    SAUDI_GREEN(
        title = "الخلفية الخضراء السعودية",
        subtitle = "أخضر سعودي أصيل مع شعار السيفين والنخلة الأبيض",
        priceSar = 20,
        isLightBackground = false,
        isStoreBackground = true,
        accentColor = Color(0xFF00642F),
    ),
    ;

    val priceLabel: String
        get() = if (priceSar <= 0) "مجانية" else "$priceSar ريال"

    companion object {
        val freeBackgrounds: List<TableBackgroundStyle> = listOf(CLASSIC_WHITE, CLASSIC_GREEN)
        val storeBackgrounds: List<TableBackgroundStyle> = listOf(
            SAUDI_GOLD,
            SAUDI_ROYAL_DARK_GOLD,
            SAUDI_GREEN,
        )
    }
}

object TableBackgroundStorage {
    private const val PREFS_NAME = "shaddaha_table_bg_prefs"

    fun loadSelected(context: Context, userId: String): TableBackgroundStyle {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("selected_$userId", null)
        return raw?.let { name -> runCatching { TableBackgroundStyle.valueOf(name) }.getOrNull() }
            ?: TableBackgroundStyle.CLASSIC_WHITE
    }

    fun saveSelected(context: Context, userId: String, style: TableBackgroundStyle) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("selected_$userId", style.name)
            .apply()
    }

    fun loadOwnedPreview(context: Context, userId: String): Set<TableBackgroundStyle> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet("owned_$userId", emptySet()).orEmpty()
        val parsed = raw.mapNotNull { name ->
            runCatching { TableBackgroundStyle.valueOf(name) }.getOrNull()
        }.toSet()
        return parsed + TableBackgroundStyle.freeBackgrounds
    }

    fun saveOwnedPreview(context: Context, userId: String, owned: Set<TableBackgroundStyle>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet("owned_$userId", owned.map { it.name }.toSet())
            .apply()
    }
}

private fun resolveCustomBackgroundDrawable(
    context: Context,
    candidateNames: List<String>,
): Int {
    for (name in candidateNames) {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id != 0) return id
    }
    return 0
}

@Composable
fun TableArenaBackground(
    style: TableBackgroundStyle,
    modifier: Modifier = Modifier,
    isMiniPreview: Boolean = false,
) {
    val context = LocalContext.current
    val customDrawableId = remember(style) {
        when (style) {
            TableBackgroundStyle.SAUDI_GOLD -> resolveCustomBackgroundDrawable(
                context,
                listOf("bg_saudi_gold", "goold"),
            )
            TableBackgroundStyle.SAUDI_ROYAL_DARK_GOLD -> resolveCustomBackgroundDrawable(
                context,
                listOf("bg_saudi_royal_gold", "bg_saudi_royal_dark_gold", "saudi_gold"),
            )
            TableBackgroundStyle.SAUDI_GREEN -> resolveCustomBackgroundDrawable(
                context,
                listOf("bg_saudi_green", "saudi_green"),
            )
            else -> 0
        }
    }

    if (customDrawableId != 0) {
        Image(
            painter = painterResource(id = customDrawableId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }

    val emblemSize: Dp = if (isMiniPreview) 30.dp else 132.dp
    val logoWidth: Dp = if (isMiniPreview) 36.dp else 184.dp
    val logoHeight: Dp = if (isMiniPreview) 22.dp else 108.dp
    val subLogoWidth: Dp = if (isMiniPreview) 18.dp else 64.dp
    val subLogoHeight: Dp = if (isMiniPreview) 11.dp else 38.dp

    when (style) {
        TableBackgroundStyle.CLASSIC_WHITE -> {
            Box(
                modifier = modifier.background(ShaddahaWhite),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.shaddaha_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = logoWidth, height = logoHeight)
                        .alpha(0.28f),
                )
            }
        }

        TableBackgroundStyle.CLASSIC_GREEN -> {
            Box(
                modifier = modifier.background(Color(0xFF04220E)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0F8234),
                                Color(0xFF095823),
                                Color(0xFF042810),
                                Color(0xFF021408),
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.68f,
                        ),
                    )
                    // Subtle felt square grid
                    val cell = if (isMiniPreview) size.width / 4f else size.width / 5f
                    val gridColor = Color.White.copy(alpha = 0.055f)
                    val strokeW = if (isMiniPreview) 0.8f else 1.5f
                    var x = cell
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokeW)
                        x += cell
                    }
                    var y = cell
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokeW)
                        y += cell
                    }
                    // Center rounded playing mat frame
                    val frameW = size.width * 0.86f
                    val frameH = (size.height * 0.42f).coerceAtLeast(frameW * 0.85f).coerceAtMost(size.height * 0.78f)
                    val left = (size.width - frameW) / 2f
                    val top = (size.height - frameH) / 2f
                    val radius = if (isMiniPreview) 10.dp.toPx() else 34.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF1AA345).copy(alpha = 0.14f),
                        topLeft = Offset(left, top),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(radius, radius),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.14f),
                        topLeft = Offset(left, top),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = if (isMiniPreview) 1f else 2.2f),
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.shaddaha_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(0xFF56C93F)),
                    modifier = Modifier
                        .size(width = logoWidth, height = logoHeight)
                        .alpha(0.68f),
                )
            }
        }

        TableBackgroundStyle.SAUDI_GOLD -> {
            // Exact match to goold.png: flat rich Saudi gold, crisp white emblem, subtle watermark logo underneath
            Box(
                modifier = modifier.background(Color(0xFFCDA200)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isMiniPreview) 1.dp else 2.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_saudi_emblem),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ShaddahaWhite),
                        modifier = Modifier.size(emblemSize),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.shaddaha_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0xFF9C7B00)),
                        modifier = Modifier
                            .size(width = subLogoWidth, height = subLogoHeight)
                            .alpha(0.38f),
                    )
                }
            }
        }

        TableBackgroundStyle.SAUDI_ROYAL_DARK_GOLD -> {
            // Exact match to saudi----gold.png: deep royal green, thin crisp white geometric contours, gold emblem
            Box(
                modifier = modifier.background(Color(0xFF093F1E)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val lineStroke = Stroke(
                        width = if (isMiniPreview) 1.0f else 2.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                    val whiteLine = Color.White.copy(alpha = 0.92f)

                    // Top-left diagonal & upper jagged chevron
                    val topPath = Path().apply {
                        moveTo(w * 0.145f, 0f)
                        lineTo(0f, h * 0.165f)
                        moveTo(w * 0.58f, 0f)
                        lineTo(w * 0.495f, h * 0.115f)
                        lineTo(w * 0.045f, h * 0.24f)
                        lineTo(w, h * 0.125f)
                    }
                    drawPath(topPath, color = whiteLine, style = lineStroke)

                    // Middle chevron contour line & right small notch
                    val midPath = Path().apply {
                        moveTo(0f, h * 0.515f)
                        lineTo(w * 0.18f, h * 0.398f)
                        lineTo(w * 0.73f, h * 0.345f)
                        lineTo(w, h * 0.285f)
                        moveTo(w, h * 0.545f)
                        lineTo(w * 0.925f, h * 0.592f)
                        lineTo(w, h * 0.582f)
                    }
                    drawPath(midPath, color = whiteLine, style = lineStroke)

                    // Lower gentle curve & bottom angular peak
                    val lowerPath = Path().apply {
                        moveTo(0f, h * 0.77f)
                        quadraticTo(w * 0.46f, h * 0.68f, w, h * 0.648f)
                        moveTo(0f, h * 0.908f)
                        lineTo(w * 0.595f, h * 0.838f)
                        lineTo(w * 0.455f, h)
                        moveTo(w, h * 0.885f)
                        lineTo(w * 0.895f, h)
                    }
                    drawPath(lowerPath, color = whiteLine, style = lineStroke)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isMiniPreview) 1.dp else 2.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_saudi_emblem),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFFDFB400)),
                        modifier = Modifier.size(emblemSize),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.shaddaha_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0xFF3E694C)),
                        modifier = Modifier
                            .size(width = subLogoWidth, height = subLogoHeight)
                            .alpha(0.45f),
                    )
                }
            }
        }

        TableBackgroundStyle.SAUDI_GREEN -> {
            // Exact match to saudi-green.png: flat solid classic Saudi green, crisp white emblem, subtle watermark logo
            Box(
                modifier = modifier.background(Color(0xFF046631)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isMiniPreview) 1.dp else 2.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_saudi_emblem),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ShaddahaWhite),
                        modifier = Modifier.size(emblemSize),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.shaddaha_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0xFF338756)),
                        modifier = Modifier
                            .size(width = subLogoWidth, height = subLogoHeight)
                            .alpha(0.38f),
                    )
                }
            }
        }
    }
}

@Composable
fun TableBackgroundThumbnail(
    style: TableBackgroundStyle,
    isSelected: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 2.4.dp else 1.2.dp,
            color = if (isSelected) ShaddahaBlue else Color(0xFFD5DBE8),
        ),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = modifier.size(width = 56.dp, height = 76.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp)),
        ) {
            TableArenaBackground(
                style = style,
                isMiniPreview = true,
                modifier = Modifier.fillMaxSize(),
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "محددة",
                    tint = TealAccent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .background(ShaddahaWhite, CircleShape),
                )
            } else if (isLocked) {
                Surface(
                    shape = CircleShape,
                    color = ShaddahaYellow,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(18.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "مدفوعة",
                            tint = Color(0xFF191C24),
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }
    }
}
