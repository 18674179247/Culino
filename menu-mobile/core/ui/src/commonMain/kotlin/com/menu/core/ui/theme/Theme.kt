package com.menu.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BiliPink,
    onPrimary = White,
    primaryContainer = BiliPinkLight,
    onPrimaryContainer = BiliPinkDark,
    secondary = BiliBlue,
    onSecondary = White,
    secondaryContainer = BiliBlueLight,
    onSecondaryContainer = BiliBlueDark,
    tertiary = BiliSuccess,
    error = BiliError,
    errorContainer = BiliErrorLight,
    onError = White,
    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    outline = Gray200,
    outlineVariant = Gray100,
    inverseSurface = Gray900,
    inverseOnSurface = White,
)

private val DarkColorScheme = darkColorScheme(
    primary = BiliPink,
    onPrimary = White,
    primaryContainer = BiliPinkDark,
    onPrimaryContainer = BiliPinkLight,
    secondary = BiliBlue,
    onSecondary = White,
    secondaryContainer = BiliBlueDark,
    onSecondaryContainer = BiliBlueLight,
    tertiary = BiliSuccess,
    error = BiliError,
    errorContainer = Color(0xFF4E0002),
    onError = White,
    background = DarkBg,
    onBackground = Gray200,
    surface = DarkSurface,
    onSurface = Gray200,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Gray400,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
    inverseSurface = Gray200,
    inverseOnSurface = Gray900,
)

private val MenuShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
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
        shapes = MenuShapes,
        content = content
    )
}
