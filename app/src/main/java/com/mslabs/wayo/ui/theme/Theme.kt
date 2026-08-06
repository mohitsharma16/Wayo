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
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = TealGlowDim,
    onSecondary = NavyBackground,
    secondaryContainer = TealDimContainerDark,
    onSecondaryContainer = OnTealDimContainerDark,
    tertiary = Coral,
    onTertiary = NavyBackground,
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = OnCoralContainerDark,
    background = NavyBackground,
    onBackground = Color(0xFFE7ECEF),
    surface = NavySurface,
    onSurface = Color(0xFFE7ECEF),
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = Color(0xFFAAB4BC),
    outline = NavyOutline,
    outlineVariant = Color(0xFF2A333B),
    // Explicit, not left to default to `primary` -- Surface blends this
    // color onto a container's background proportional to its elevation
    // (Card's default tonalElevation is 1.dp, not 0), so any component
    // with an unspecified surfaceTint tints toward Material3's baseline
    // purple regardless of what containerColor was otherwise set to.
    surfaceTint = TealGlow,
    inversePrimary = TealGlowDim,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    surfaceDim = NavySurfaceDim,
    surfaceBright = NavySurfaceBright,
    surfaceContainerLowest = NavySurfaceContainerLowest,
    surfaceContainerLow = NavySurfaceContainerLow,
    surfaceContainer = NavySurfaceContainer,
    surfaceContainerHigh = NavySurfaceContainerHigh,
    surfaceContainerHighest = NavySurfaceContainerHighest
)

private val LightColors = lightColorScheme(
    primary = TealGlowDim,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = OnTealContainerLight,
    secondary = TealGlow,
    onSecondary = Color.White,
    secondaryContainer = TealDimContainerLight,
    onSecondaryContainer = OnTealDimContainerLight,
    tertiary = CoralDim,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainerLight,
    onTertiaryContainer = OnCoralContainerLight,
    background = MistBackground,
    onBackground = Color(0xFF141A1E),
    surface = MistSurface,
    onSurface = Color(0xFF141A1E),
    surfaceVariant = MistSurfaceVariant,
    onSurfaceVariant = Color(0xFF4D5A61),
    outline = MistOutline,
    outlineVariant = Color(0xFFDCE3E6),
    surfaceTint = TealGlowDim,
    inversePrimary = TealGlow,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    surfaceDim = MistSurfaceDim,
    surfaceBright = MistSurfaceBright,
    surfaceContainerLowest = MistSurfaceContainerLowest,
    surfaceContainerLow = MistSurfaceContainerLow,
    surfaceContainer = MistSurfaceContainer,
    surfaceContainerHigh = MistSurfaceContainerHigh,
    surfaceContainerHighest = MistSurfaceContainerHighest
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
