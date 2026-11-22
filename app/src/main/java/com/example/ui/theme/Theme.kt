package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VallartaDarkColorScheme = darkColorScheme(
    primary = SunsetCoral,
    onPrimary = PacificMidnight,
    primaryContainer = CardSlateSurface,
    onPrimaryContainer = SandWhite,
    secondary = BanderasTurquoise,
    onSecondary = PacificMidnight,
    secondaryContainer = DeepBaySurface,
    onSecondaryContainer = BanderasTurquoise,
    tertiary = TropicalGold,
    onTertiary = PacificMidnight,
    background = PacificMidnight,
    onBackground = SandWhite,
    surface = DeepBaySurface,
    onSurface = SandWhite,
    surfaceVariant = CardSlateSurface,
    onSurfaceVariant = MutedMist,
    error = PoliceRed,
    onError = SandWhite,
    outline = SubtleBorder
)

@Composable
fun VallartaViceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VallartaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
