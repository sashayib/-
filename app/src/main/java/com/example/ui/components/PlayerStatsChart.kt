package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaRedSoft
import com.example.ui.theme.ShaddahaSurfaceGray
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowRich
import com.example.ui.theme.ShaddahaYellowSoft
import com.example.ui.theme.TealAccent
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class PieSliceData(
    val id: String,
    val label: String,
    val value: Int,
    val color: Color,
    val softColor: Color,
)

@Composable
fun PlayerStatsPieChartCard(
    wins: Int,
    losses: Int,
    onlineMatches: Int,
    soloMatches: Int,
    modifier: Modifier = Modifier,
) {
    var selectedChartMode by remember { mutableIntStateOf(0) } // 0 = Win/Loss, 1 = Online/Solo
    var selectedSliceId by remember { mutableStateOf<String?>(null) }

    val slices = remember(selectedChartMode, wins, losses, onlineMatches, soloMatches) {
        if (selectedChartMode == 0) {
            listOf(
                PieSliceData(
                    id = "wins",
                    label = "الفوز",
                    value = wins.coerceAtLeast(0),
                    color = TealAccent,
                    softColor = TealAccent.copy(alpha = 0.14f),
                ),
                PieSliceData(
                    id = "losses",
                    label = "الخسارة",
                    value = losses.coerceAtLeast(0),
                    color = ShaddahaRed,
                    softColor = ShaddahaRedSoft,
                ),
            )
        } else {
            listOf(
                PieSliceData(
                    id = "online",
                    label = "أونلاين",
                    value = onlineMatches.coerceAtLeast(0),
                    color = ShaddahaBlue,
                    softColor = ShaddahaBlueSoft,
                ),
                PieSliceData(
                    id = "solo",
                    label = "ضد الذكاء",
                    value = soloMatches.coerceAtLeast(0),
                    color = ShaddahaYellowRich,
                    softColor = ShaddahaYellowSoft,
                ),
            )
        }
    }

    LaunchedEffect(selectedChartMode) {
        selectedSliceId = null
    }

    val total = slices.sumOf { it.value }
    val defaultCenterTitle = if (selectedChartMode == 0) "نسبة الفوز" else "إجمالي السجل"
    val defaultCenterPercent = if (total > 0) {
        if (selectedChartMode == 0) ((wins * 100f) / total).roundToInt() else 100
    } else 0
    val defaultCenterSub = if (total > 0) {
        if (selectedChartMode == 0) "$wins فوز من $total" else "$total مباراة مسجلة"
    } else {
        "العب أول صكة"
    }

    val activeSlice = slices.firstOrNull { it.id == selectedSliceId && it.value > 0 }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.3.dp, ShaddahaBlue.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_stats_pie_chart_card"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    Surface(
                        shape = CircleShape,
                        color = ShaddahaBlueSoft,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = "الرسم البياني للإحصائيات",
                                tint = ShaddahaBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "تحليل الأداء الشخصي",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                        )
                        Text(
                            text = "اضغط على أي قسم لعرض تفاصيله",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedChartMode == 0,
                    onClick = { selectedChartMode = 0 },
                    label = {
                        Text(
                            text = "الفوز والخسارة",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShaddahaBlue,
                        selectedLabelColor = ShaddahaWhite,
                    ),
                    modifier = Modifier.testTag("chart_mode_win_loss"),
                )
                FilterChip(
                    selected = selectedChartMode == 1,
                    onClick = { selectedChartMode = 1 },
                    label = {
                        Text(
                            text = "أونلاين / ضد الذكاء",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShaddahaBlue,
                        selectedLabelColor = ShaddahaWhite,
                    ),
                    modifier = Modifier.testTag("chart_mode_play_type"),
                )
            }

            InteractiveDonutPieChart(
                slices = slices,
                selectedSliceId = activeSlice?.id,
                onSelectSlice = { tappedId ->
                    selectedSliceId = if (selectedSliceId == tappedId) null else tappedId
                },
                centerTitle = activeSlice?.label ?: defaultCenterTitle,
                centerPercentage = if (activeSlice != null && total > 0) {
                    ((activeSlice.value * 100f) / total).roundToInt()
                } else {
                    defaultCenterPercent
                },
                centerSubtitle = if (activeSlice != null) {
                    "${activeSlice.value} من $total مباراة"
                } else {
                    defaultCenterSub
                },
                centerAccentColor = activeSlice?.color
                    ?: if (selectedChartMode == 0 && total > 0) TealAccent else ShaddahaBlue,
                chartSize = 212.dp,
            )

            // Interactive Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                slices.forEach { slice ->
                    val pct = if (total > 0) ((slice.value * 100f) / total).roundToInt() else 0
                    val isSelected = activeSlice?.id == slice.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) slice.softColor else ShaddahaWhite,
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) slice.color else OutlineSoft,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = slice.value > 0) {
                                selectedSliceId = if (selectedSliceId == slice.id) null else slice.id
                            }
                            .minimumInteractiveComponentSize()
                            .testTag("chart_legend_${slice.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(slice.color),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = InkDark,
                                )
                                Text(
                                    text = "${slice.value} مباراة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted,
                                )
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = slice.color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveDonutPieChart(
    slices: List<PieSliceData>,
    selectedSliceId: String?,
    onSelectSlice: (String) -> Unit,
    centerTitle: String,
    centerPercentage: Int,
    centerSubtitle: String,
    centerAccentColor: Color,
    chartSize: Dp = 212.dp,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }
    val nonZeroSlices = remember(slices) { slices.filter { it.value > 0 } }
    val sweepProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        )
    }

    val selectedBump = remember { Animatable(0f) }
    LaunchedEffect(selectedSliceId) {
        selectedBump.animateTo(
            targetValue = if (selectedSliceId != null) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    val a11ySummary = remember(slices, centerTitle, centerPercentage) {
        if (total == 0) {
            "رسم بياني دائري للإحصائيات، لا توجد مباريات مسجلة بعد"
        } else {
            val details = slices.joinToString("، ") { "${it.label}: ${it.value}" }
            "رسم بياني دائري، $centerTitle $centerPercentage بالمئة. $details"
        }
    }

    Box(
        modifier = modifier
            .size(chartSize)
            .semantics { contentDescription = a11ySummary }
            .testTag("donut_pie_chart_canvas"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(chartSize)
                .pointerInput(nonZeroSlices, total) {
                    if (total <= 0) return@pointerInput
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = hypot(dx, dy)
                        val outerR = size.width.coerceAtMost(size.height) / 2f
                        val innerR = outerR * 0.48f
                        if (dist in innerR..outerR) {
                            var angleDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                            // Shift so -90° (12 o'clock) maps to 0° clockwise
                            angleDeg = (angleDeg + 90f + 360f) % 360f
                            var acc = 0f
                            for (slice in nonZeroSlices) {
                                val sweep = (slice.value.toFloat() / total.toFloat()) * 360f
                                if (angleDeg in acc..(acc + sweep)) {
                                    onSelectSlice(slice.id)
                                    break
                                }
                                acc += sweep
                            }
                        }
                    }
                },
        ) {
            val minDim = size.minDimension
            val baseStroke = minDim * 0.145f
            val extraStroke = minDim * 0.032f
            val inset = baseStroke / 2f + extraStroke
            val arcSize = Size(minDim - inset * 2f, minDim - inset * 2f)
            val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

            // Soft background ring track
            drawArc(
                color = ShaddahaSurfaceGray.copy(alpha = 0.7f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = baseStroke, cap = StrokeCap.Butt),
            )

            if (total > 0 && nonZeroSlices.isNotEmpty()) {
                val gapDegrees = if (nonZeroSlices.size > 1) 4f else 0f
                val animFraction = sweepProgress.value
                var startAngle = -90f

                nonZeroSlices.forEach { slice ->
                    val rawSweep = (slice.value.toFloat() / total.toFloat()) * 360f
                    val drawSweep = ((rawSweep - gapDegrees).coerceAtLeast(1.5f)) * animFraction
                    val isSelected = slice.id == selectedSliceId
                    val strokeWidth = if (isSelected) {
                        baseStroke + extraStroke * selectedBump.value
                    } else {
                        baseStroke
                    }
                    val alpha = if (selectedSliceId != null && !isSelected) 0.42f else 1f

                    drawArc(
                        color = slice.color.copy(alpha = alpha),
                        startAngle = startAngle + gapDegrees / 2f,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = if (nonZeroSlices.size == 1) StrokeCap.Butt else StrokeCap.Round,
                        ),
                    )
                    startAngle += rawSweep
                }
            }
        }

        // Crisp inner center badge
        Surface(
            shape = CircleShape,
            color = ShaddahaWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.size(chartSize * 0.56f),
        ) {
            AnimatedContent(
                targetState = Triple(centerTitle, centerPercentage, centerSubtitle),
                label = "donut_center_readout",
            ) { (title, pct, sub) ->
                Column(
                    modifier = Modifier
                        .size(chartSize * 0.56f)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = InkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                        ),
                        color = centerAccentColor,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MiniWinRateDonutCard(
    wins: Int,
    losses: Int,
    matchesPlayed: Int,
    onOpenFullStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = (wins + losses).coerceAtLeast(matchesPlayed)
    val winRate = if (total > 0) ((wins * 100f) / total).roundToInt() else 0

    Surface(
        onClick = onOpenFullStats,
        shape = RoundedCornerShape(20.dp),
        color = ShaddahaSurfaceSoft,
        border = BorderStroke(1.2.dp, ShaddahaBlue.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_open_stats_chart_button"),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(74.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(74.dp)) {
                    val stroke = size.minDimension * 0.16f
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = ShaddahaSurfaceGray,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    if (total > 0) {
                        val winSweep = (wins.toFloat() / total.toFloat()) * 360f
                        val lossSweep = 360f - winSweep
                        if (lossSweep > 0f) {
                            drawArc(
                                color = ShaddahaRed,
                                startAngle = -90f + winSweep,
                                sweepAngle = lossSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke),
                            )
                        }
                        if (winSweep > 0f) {
                            drawArc(
                                color = TealAccent,
                                startAngle = -90f,
                                sweepAngle = winSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
                Text(
                    text = "$winRate%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (total > 0) TealAccent else InkMuted,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "إحصائياتي والرسم البياني",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkDark,
                )
                Text(
                    text = "الفوز: $wins • الخسارة: $losses • الإجمالي: $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ShaddahaYellow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            tint = InkDark,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "عرض التحليل الدائري الكامل",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = InkDark,
                        )
                    }
                }
            }
        }
    }
}
