package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SoundSpireColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    secondary = SubheadingPeach,
    onSecondary = Color.Black,
    tertiary = HeadingPeach,
    background = BackgroundDarkPurple,
    onBackground = TextWhite,
    surface = BackgroundMidPurple,
    onSurface = TextWhite,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = Color.White,
    outline = CardBorder,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SoundSpireColorScheme,
        typography = Typography,
        content = content,
    )
}
