package com.menu.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    primaryContainer = Orange700,
    secondary = Green500,
    secondaryContainer = Green700,
    error = Red500,
    background = Gray100,
    surface = Gray100,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = Gray900,
    onSurface = Gray900,
    outline = Gray300,
    onSurfaceVariant = Gray600,
)

private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    primaryContainer = Orange700,
    secondary = Green500,
    secondaryContainer = Green700,
    error = Red500,
)

@Composable
fun MenuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MenuTypography,
        content = content
    )
}
