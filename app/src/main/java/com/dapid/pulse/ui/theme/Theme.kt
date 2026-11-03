package com.dapid.pulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Web tokens mapped onto Material 3 roles. No dynamic (wallpaper) colour: Pulse stays green.
private val LightColors = lightColorScheme(
    primary = Light.accent,
    onPrimary = Light.onAccent,
    primaryContainer = Light.accentSoft,
    onPrimaryContainer = Light.accent,
    secondaryContainer = Light.accentSoft,
    onSecondaryContainer = Light.accent,
    background = Light.background,
    onBackground = Light.foreground,
    surface = Light.surface,
    onSurface = Light.foreground,
    surfaceVariant = Light.track,
    onSurfaceVariant = Light.muted,
    outline = Light.muted,
    outlineVariant = Light.line,
    error = Light.danger,
    errorContainer = Light.dangerSoft,
)

private val DarkColors = darkColorScheme(
    primary = Dark.accent,
    onPrimary = Dark.onAccent,
    primaryContainer = Dark.accentSoft,
    onPrimaryContainer = Dark.accent,
    secondaryContainer = Dark.accentSoft,
    onSecondaryContainer = Dark.accent,
    background = Dark.background,
    onBackground = Dark.foreground,
    surface = Dark.surface,
    onSurface = Dark.foreground,
    surfaceVariant = Dark.track,
    onSurfaceVariant = Dark.muted,
    outline = Dark.muted,
    outlineVariant = Dark.line,
    error = Dark.danger,
    errorContainer = Dark.dangerSoft,
)

// Follows the phone's light/dark setting (a Settings override comes later, like the web's theme switch).
@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
