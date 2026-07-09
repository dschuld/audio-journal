package com.audiojournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RecordRed = Color(0xFFD32F2F)
private val RecordRedDark = Color(0xFFEF5350)

private val LightColors = lightColorScheme(
    primary = RecordRed,
    onPrimary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = RecordRedDark,
    onPrimary = Color.Black,
)

@Composable
fun AudioJournalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
