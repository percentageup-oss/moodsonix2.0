package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightAccent,
    onPrimary = Color.Black,
    primaryContainer = MidnightAccent.copy(alpha = 0.2f),
    onPrimaryContainer = MidnightAccent,
    background = MidnightBackground,
    onBackground = MidnightTextPrimary,
    surface = MidnightSurface,
    onSurface = MidnightTextPrimary,
    surfaceVariant = MidnightSurfaceSecondary,
    onSurfaceVariant = MidnightTextSecondary,
    outline = MidnightBorder
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkAccent,
    onPrimary = Color.Black,
    secondary = CyberpunkSecondary,
    onSecondary = Color.White,
    primaryContainer = CyberpunkSecondary.copy(alpha = 0.25f),
    onPrimaryContainer = CyberpunkAccent,
    background = CyberpunkBackground,
    onBackground = CyberpunkTextPrimary,
    surface = CyberpunkSurface,
    onSurface = CyberpunkTextPrimary,
    surfaceVariant = CyberpunkSurfaceSecondary,
    onSurfaceVariant = CyberpunkTextSecondary,
    outline = CyberpunkSecondary.copy(alpha = 0.3f)
)

private val SlateColorScheme = darkColorScheme(
    primary = SlateAccent,
    onPrimary = Color.Black,
    primaryContainer = SlateAccent.copy(alpha = 0.2f),
    onPrimaryContainer = SlateAccent,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurfaceSecondary,
    onSurfaceVariant = SlateTextSecondary,
    outline = Color(0x22FFFFFF)
)

@Composable
fun MoodsonixTheme(
    appTheme: AppTheme = AppTheme.MIDNIGHT_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.MIDNIGHT_DARK -> MidnightColorScheme
        AppTheme.NEON_CYBERPUNK -> CyberpunkColorScheme
        AppTheme.MINIMALIST_SLATE -> SlateColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
