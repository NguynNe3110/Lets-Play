package com.uzuu.letplay.resources.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = SoftGreen,
    onPrimaryContainer = PrimaryGreen,
    secondary = SecondaryAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = Color(0xFFD8EED5),
    onSecondaryContainer = Color(0xFF0A3912),
    tertiary = AccentRust,
    onTertiary = SurfaceWhite,
    tertiaryContainer = Peach,
    onTertiaryContainer = Color(0xFF5E1B0F),
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = Color(0xFFC7CAC2),
    error = Color(0xFFBA1A1A),
    onError = SurfaceWhite
)

val DarkColorScheme = darkColorScheme(
    primary = SecondaryAccent,
    onPrimary = DarkSurface,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = Color(0xFFD8EED5),
    secondary = PhotoAccent,
    onSecondary = DarkSurface,
    secondaryContainer = Color(0xFF2E4E2C),
    onSecondaryContainer = PhotoAccent,
    tertiary = PhotoAccent,
    onTertiary = DarkSurface,
    background = DarkBackground,
    onBackground = TextLight,
    surface = DarkSurface,
    onSurface = TextLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMutedDark,
    outline = Color(0xFF38433C),
    outlineVariant = Color(0xFF4C5850),
    error = PhotoError,
    onError = DarkBackground
)

@Composable
fun LetsPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * BaseTheme provides backward compatibility for existing screens and callers.
 */
@Composable
fun BaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = LetsPlayTheme(darkTheme = darkTheme, content = content)
