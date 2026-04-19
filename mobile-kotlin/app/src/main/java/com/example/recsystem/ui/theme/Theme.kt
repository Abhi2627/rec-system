package com.example.recsystem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = CineRed,
    onPrimary        = Color.White,
    primaryContainer = CineCream,
    secondary        = CineNavy,
    onSecondary      = Color.White,
    background       = NeutralSurface,
    onBackground     = NeutralText1,
    surface          = NeutralCard,
    onSurface        = NeutralText1,
    error            = ErrorRed,
    onError          = Color.White,
)

private val DarkColors = darkColorScheme(
    primary          = CineCream,
    onPrimary        = CineNavy,
    primaryContainer = CineNavy,
    secondary        = CineCream,
    onSecondary      = CineNavy,
    background       = Color(0xFF121212),
    onBackground     = Color(0xFFE0E0E0),
    surface          = Color(0xFF1E1E1E),
    onSurface        = Color(0xFFE0E0E0),
    error            = Color(0xFFCF6679),
    onError          = Color.Black,
)

@Composable
fun RecSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content
    )
}
