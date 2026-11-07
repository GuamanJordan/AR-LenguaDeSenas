package com.espe.arlenguadesenas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BorderAccent,
    secondary = AccentTeal,
    background = HighContrastBackground,
    surface = HighContrastSurface,
    onPrimary = HighContrastBackground,
    onBackground = TextHighContrast,
    onSurface = TextHighContrast
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentTeal,
    background = TextHighContrast,
    surface = TextMuted,
    onPrimary = TextHighContrast,
    onBackground = HighContrastBackground,
    onSurface = HighContrastBackground
)

@Composable
fun ARLenguaDeSenasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = AccessibleTypography,
        content = content
    )
}
