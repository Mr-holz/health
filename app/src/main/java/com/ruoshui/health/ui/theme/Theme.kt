package com.ruoshui.health.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Spring,
    secondary = Sky,
    tertiary = Sun,
    background = DeepInk,
    surface = SurfaceGreen,
    surfaceVariant = SurfaceLift,
    onPrimary = DeepInk,
    onSecondary = DeepInk,
    onTertiary = DeepInk,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    error = Rose
)

@Composable
fun RuoshuiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
