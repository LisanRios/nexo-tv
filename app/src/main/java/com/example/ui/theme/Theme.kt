package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexoDarkColorScheme = darkColorScheme(
    primary = NexoBluePrimary,
    onPrimary = NexoTextPrimary,
    primaryContainer = NexoSurfaceElevated,
    onPrimaryContainer = NexoBlueLight,
    secondary = NexoCyan,
    onSecondary = NexoNavyDark,
    tertiary = NexoEmerald,
    onTertiary = NexoNavyDark,
    background = NexoNavyDark,
    onBackground = NexoTextPrimary,
    surface = NexoSurface,
    onSurface = NexoTextPrimary,
    surfaceVariant = NexoSurfaceCard,
    onSurfaceVariant = NexoTextSecondary,
    outline = NexoSurfaceBorder,
    error = NexoRed,
    onError = NexoTextPrimary
)

@Composable
fun NexoTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
