package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BridgeCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF70F7FF),
    secondary = BridgeTeal,
    onSecondary = Color(0xFF00382B),
    background = BridgeNavyDark,
    onBackground = Color(0xFFE2E8F0),
    surface = BridgeSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = BridgeCardDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = BridgeRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BridgePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = BridgeTeal,
    onSecondary = Color.White,
    background = BridgeSurfaceLight,
    onBackground = BridgeTextPrimaryLight,
    surface = BridgeCardLight,
    onSurface = BridgeTextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = BridgeTextSecondaryLight,
    error = BridgeRed,
    onError = Color.White
)

@Composable
fun SafeBridgeTheme(
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
