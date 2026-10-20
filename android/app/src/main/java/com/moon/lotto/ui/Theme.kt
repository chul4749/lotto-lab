package com.moon.lotto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFFD6452F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE6E1),
    onPrimaryContainer = Color(0xFF7A2214),
    secondaryContainer = Color(0xFFFBE6E1),
    onSecondaryContainer = Color(0xFF7A2214),
    background = Color(0xFFF6F4EF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1EDE5),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF6B6B6B),
    outlineVariant = Color(0xFFE4E0D8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF6B55),
    onPrimary = Color(0xFF16171A),
    primaryContainer = Color(0xFF452019),
    onPrimaryContainer = Color(0xFFFFD9D1),
    secondaryContainer = Color(0xFF452019),
    onSecondaryContainer = Color(0xFFFFD9D1),
    background = Color(0xFF16171A),
    surface = Color(0xFF202226),
    surfaceVariant = Color(0xFF2A2C31),
    onSurface = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outlineVariant = Color(0xFF33363B),
)

@Composable
fun LottoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
