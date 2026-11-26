package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaRed
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.ShaddahaYellowSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoTilt = remember { Animatable(-8f) }
    var showTagline by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splashLoop")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatOffsetY",
    )
    val sparkRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkRotation",
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(260))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            logoTilt.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        delay(340)
        showTagline = true
        delay(1850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(ShaddahaWhite, ShaddahaWhite, ShaddahaBlueSoft.copy(alpha = 0.25f))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle animated background suit sparks
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 36.dp, y = 120.dp)
                .rotate(sparkRotation)
                .size(44.dp)
                .clip(CircleShape)
                .background(ShaddahaYellowSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("♥", fontSize = 22.sp, color = ShaddahaRed)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-42).dp, y = 160.dp)
                .rotate(-sparkRotation)
                .size(40.dp)
                .clip(CircleShape)
                .background(ShaddahaBlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("♠", fontSize = 22.sp, color = ShaddahaBlue)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 48.dp, y = (-140).dp)
                .rotate(-sparkRotation)
                .size(38.dp)
                .clip(CircleShape)
                .background(ShaddahaBlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("♣", fontSize = 20.sp, color = ShaddahaBlue)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-120).dp)
                .rotate(sparkRotation)
                .size(42.dp)
                .clip(CircleShape)
                .background(ShaddahaYellowSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("♦", fontSize = 20.sp, color = ShaddahaRed)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.shaddaha_logo),
                contentDescription = "شعار شدها",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(280.dp)
                    .offset(y = floatOffsetY.dp)
                    .scale(logoScale.value * breatheScale)
                    .rotate(logoTilt.value)
                    .alpha(logoAlpha.value),
            )

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 2 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "لعبة بلوت .. تفهــــــمك",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                        ),
                        color = ShaddahaBlue,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(120.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ShaddahaBlue,
                        trackColor = ShaddahaYellow.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
