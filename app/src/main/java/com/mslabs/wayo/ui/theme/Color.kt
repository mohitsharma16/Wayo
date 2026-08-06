package com.mslabs.wayo.ui.theme

import androidx.compose.ui.graphics.Color

// Primary: an electric teal — reads as "navigation/signal", not generic app-blue.
val TealGlow = Color(0xFF00E6C3)
val TealGlowDim = Color(0xFF00A98D)

// Tertiary: warm coral, used for gradient contrast on the primary action
// and for the "found it" success moment.
val Coral = Color(0xFFFF6B5B)
val CoralDim = Color(0xFFE5503F)

// Dark surfaces — deliberately near-black navy rather than pure black,
// since this app is very plausibly opened in a dim parking garage.
val NavyBackground = Color(0xFF0A0E13)
val NavySurface = Color(0xFF13191F)
val NavySurfaceVariant = Color(0xFF1C242C)
val NavyOutline = Color(0xFF39434D)

// Light surfaces
val MistBackground = Color(0xFFF6F8F9)
val MistSurface = Color(0xFFFFFFFF)
val MistSurfaceVariant = Color(0xFFE9EEF0)
val MistOutline = Color(0xFFC7D0D4)

// "Container" roles (primaryContainer, secondaryContainer, tertiaryContainer,
// inversePrimary, inverse surfaces). Material3's darkColorScheme()/
// lightColorScheme() fall back to its own baseline purple for any of these
// left unspecified -- components like FilledTonalButton draw from
// secondaryContainer, so without these the permission screen's buttons
// rendered stock Material purple instead of the app's teal/coral palette.
val TealContainerDark = Color(0xFF00332B)
val OnTealContainerDark = Color(0xFF6FFFE9)
val TealDimContainerDark = Color(0xFF15302C)
val OnTealDimContainerDark = Color(0xFFB0E8DD)
val CoralContainerDark = Color(0xFF4A241E)
val OnCoralContainerDark = Color(0xFFFFDAD2)
val InverseSurfaceDark = Color(0xFFE7ECEF)
val InverseOnSurfaceDark = Color(0xFF141A1E)

val TealContainerLight = Color(0xFFB6F5EA)
val OnTealContainerLight = Color(0xFF003731)
val TealDimContainerLight = Color(0xFFCDEFE8)
val OnTealDimContainerLight = Color(0xFF0B3D36)
val CoralContainerLight = Color(0xFFFFDAD2)
val OnCoralContainerLight = Color(0xFF5C160C)
val InverseSurfaceLight = Color(0xFF29323A)
val InverseOnSurfaceLight = Color(0xFFEFF3F4)

// Newer Material3 "surface container" tonal roles (surfaceDim/surfaceBright,
// surfaceContainerLowest...surfaceContainerHighest). Card, Menu, BottomSheet
// etc. draw their default backgrounds from THESE roles now, not plain
// `surface` -- left unspecified they fall back to Material3's own baseline
// tonal palette (purple-derived), which is why cards kept rendering purple
// even after surface/surfaceVariant were already on-brand. All derived as
// a navy/mist ramp so every default container stays on-palette.
val NavySurfaceDim = Color(0xFF0A0E13)
val NavySurfaceBright = Color(0xFF2B333B)
val NavySurfaceContainerLowest = Color(0xFF060A0E)
val NavySurfaceContainerLow = Color(0xFF12181E)
val NavySurfaceContainer = Color(0xFF171E25)
val NavySurfaceContainerHigh = Color(0xFF212930)
val NavySurfaceContainerHighest = Color(0xFF2C353D)

val MistSurfaceDim = Color(0xFFD8DEE0)
val MistSurfaceBright = Color(0xFFFFFFFF)
val MistSurfaceContainerLowest = Color(0xFFFFFFFF)
val MistSurfaceContainerLow = Color(0xFFF1F4F5)
val MistSurfaceContainer = Color(0xFFEBEFF1)
val MistSurfaceContainerHigh = Color(0xFFE5EAEC)
val MistSurfaceContainerHighest = Color(0xFFDFE5E7)
