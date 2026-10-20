package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RawiCoralPrimary,
    onPrimary = RawiNightOnPrimary,
    primaryContainer = RawiNightPrimaryContainer,
    onPrimaryContainer = RawiNightOnPrimaryContainer,
    secondary = RawiNightGoldSecondary,
    secondaryContainer = RawiNightSecondaryContainer,
    onSecondaryContainer = RawiNightOnSecondaryContainer,
    tertiary = RawiGoldAccent,
    background = RawiNightBackground,
    onBackground = RawiNightText,
    surface = RawiNightSurface,
    onSurface = RawiNightText,
    surfaceVariant = RawiNightSurfaceVariant,
    onSurfaceVariant = RawiNightMutedText
)

private val LightColorScheme = lightColorScheme(
    primary = RawiBurgundyPrimary,
    onPrimary = RawiOnPrimary,
    primaryContainer = RawiPrimaryContainer,
    onPrimaryContainer = RawiOnPrimaryContainer,
    secondary = RawiGoldSecondary,
    secondaryContainer = RawiSecondaryContainer,
    onSecondaryContainer = RawiOnSecondaryContainer,
    tertiary = RawiTealTertiary,
    background = RawiCreamBackground,
    onBackground = RawiInkText,
    surface = RawiPaperSurface,
    onSurface = RawiInkText,
    surfaceVariant = RawiSurfaceVariant,
    onSurfaceVariant = RawiMutedInk
)

@Composable
fun RawiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
