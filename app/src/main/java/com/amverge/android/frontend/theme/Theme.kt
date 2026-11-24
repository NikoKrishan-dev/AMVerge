package com.amverge.android.frontend.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleDarkColorScheme = darkColorScheme(
    primary = AppleSkyBlue,
    onPrimary = AppleCanvasDark,
    primaryContainer = AppleActionBlue,
    onPrimaryContainer = AppleTextPrimaryDark,
    secondary = AppleSkyBlue,
    onSecondary = AppleCanvasDark,
    secondaryContainer = AppleTile2,
    onSecondaryContainer = AppleTextPrimaryDark,
    tertiary = AppleGold,
    onTertiary = AppleCanvasDark,
    background = AppleCanvasDark,
    onBackground = AppleTextPrimaryDark,
    surface = AppleSurfaceDark,
    onSurface = AppleTextPrimaryDark,
    surfaceVariant = AppleTile1,
    onSurfaceVariant = AppleTextSecondaryDark,
    outline = AppleHairlineDark,
    outlineVariant = AppleDividerDark,
    error = AppleRed,
    onError = AppleCanvasDark,
    errorContainer = Color(0x33FF453A),
    onErrorContainer = AppleRed
)

private val AppleLightColorScheme = lightColorScheme(
    primary = AppleActionBlue,
    onPrimary = AppleCanvasLight,
    primaryContainer = AppleActionBlueFocused,
    onPrimaryContainer = AppleCanvasLight,
    secondary = AppleActionBlue,
    onSecondary = AppleCanvasLight,
    secondaryContainer = ApplePearl,
    onSecondaryContainer = AppleInkLight,
    tertiary = AppleGold,
    onTertiary = AppleCanvasLight,
    background = AppleParchment,
    onBackground = AppleInkLight,
    surface = AppleCanvasLight,
    onSurface = AppleInkLight,
    surfaceVariant = ApplePearl,
    onSurfaceVariant = AppleInkSecondaryLight,
    outline = AppleHairlineLight,
    outlineVariant = AppleDividerLight,
    error = AppleRed,
    onError = AppleCanvasLight
)

@Composable
fun AMVergeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppleDarkColorScheme else AppleLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
