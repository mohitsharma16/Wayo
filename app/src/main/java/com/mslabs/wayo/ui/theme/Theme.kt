package com.mslabs.wayo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = TealGlow,
    onPrimary = NavyBackground,
    secondary = TealGlowDim,
    tertiary = Coral,
    onTertiary = NavyBackground,
    background = NavyBackground,
    onBackground = Color(0xFFE7ECEF),
    surface = NavySurface,
    onSurface = Color(0xFFE7ECEF),
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = Color(0xFFAAB4BC),
    outline = NavyOutline,
    outlineVariant = Color(0xFF2A333B)
)

private val LightColors = lightColorScheme(
    primary = TealGlowDim,
    onPrimary = Color.White,
    secondary = TealGlow,
    tertiary = CoralDim,
    onTertiary = Color.White,
    background = MistBackground,
    onBackground = Color(0xFF141A1E),
    surface = MistSurface,
    onSurface = Color(0xFF141A1E),
    surfaceVariant = MistSurfaceVariant,
    onSurfaceVariant = Color(0xFF4D5A61),
    outline = MistOutline,
    outlineVariant = Color(0xFFDCE3E6)
)

/**
 * Dynamic color (Material You) is intentionally off by default. This app
 * has a deliberate, crafted palette (teal + coral against navy/mist) that a
 * wallpaper-derived dynamic scheme would override and flatten. Pass
 * dynamicColor = true if you'd rather match the system theme instead.
 */
@Composable
fun WayoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
