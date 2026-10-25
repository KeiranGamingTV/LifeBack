
package com.keiranhaas.getyourlifeback.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF587D62)
private val PaleGreen = Color(0xFFD6E8D5)
private val DarkBackground = Color(0xFF101510)
private val DarkSurface = Color(0xFF1A211B)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Color(0xFF52665A),
    background = Color(0xFFF6F7F1),
    surface = Color(0xFFF6F7F1),
    surfaceVariant = Color(0xFFE5E9DF)
)

private val DarkColors = darkColorScheme(
    primary = PaleGreen,
    onPrimary = Color(0xFF183321),
    secondary = Color(0xFFB9CCBA),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF303A31)
)

@Composable
fun LifeBackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
