package com.evernoteclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF2FBE4F)
val GreenBg = Color(0xFFE8F5E9)
val Amber = Color(0xFFF57C00)
val AmberBg = Color(0xFFFFF8E1)
val Red = Color(0xFFE53935)
val Blue = Color(0xFF2196F3)
val Teal = Color(0xFF00897B)
val Purple = Color(0xFF8E24AA)
val Orange = Color(0xFFFB8C00)
val Ink = Color(0xFF263238)
val Sub = Color(0xFF9AA0A6)
val FieldBg = Color(0xFFF1F3F4)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenBg,
    onPrimaryContainer = Color(0xFF0B3D17),
    secondary = Blue,
    onSecondary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = FieldBg,
    onSurfaceVariant = Sub,
    outline = Color(0xFFE0E0E0),
    error = Red,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4CD964),
    onPrimary = Color(0xFF0B3D17),
    primaryContainer = Color(0xFF1B3A24),
    onPrimaryContainer = Color(0xFF8FE8A0),
    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF0D2C45),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFF333333),
    error = Color(0xFFEF5350),
)

@Composable
fun EvernoteCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
