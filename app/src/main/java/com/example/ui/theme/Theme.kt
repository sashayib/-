package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val ShaddahaColorScheme = lightColorScheme(
    primary = ShaddahaBlue,
    onPrimary = ShaddahaWhite,
    primaryContainer = ShaddahaBlueSoft,
    onPrimaryContainer = ShaddahaBlueDark,
    secondary = ShaddahaYellow,
    onSecondary = InkDark,
    secondaryContainer = ShaddahaYellowSoft,
    onSecondaryContainer = InkDark,
    tertiary = ShaddahaRed,
    onTertiary = ShaddahaWhite,
    background = ShaddahaWhite,
    onBackground = InkDark,
    surface = ShaddahaWhite,
    onSurface = InkDark,
    surfaceVariant = ShaddahaSurfaceGray,
    onSurfaceVariant = InkMuted,
    outline = OutlineSoft,
    outlineVariant = ShaddahaSurfaceGray,
    error = CrimsonError,
    onError = ShaddahaWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonError,
)

val JammanaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun JammanaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShaddahaColorScheme,
        typography = JammanaTypography,
        shapes = JammanaShapes,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides JammanaTypography.bodyMedium,
            content = content,
        )
    }
}
