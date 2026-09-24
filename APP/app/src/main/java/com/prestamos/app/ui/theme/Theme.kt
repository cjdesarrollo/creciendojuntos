package com.prestamos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryGreen,
    tertiary = AccentYellowDark,
    background = LightSurface,
    surface = CardSurface,
    onPrimary = CardSurface,
    onSecondary = CardSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PrestamoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
