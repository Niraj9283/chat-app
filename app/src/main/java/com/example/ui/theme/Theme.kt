package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = Color(0xFF020617),
    secondary = CyberSecondary,
    onSecondary = Color(0xFF020617),
    tertiary = CyberAccent,
    background = CyberDarkBg,
    surface = CyberSurface,
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFE2E8F0)
)

private val GlacierLightColorScheme = lightColorScheme(
    primary = GlacierPrimary,
    onPrimary = Color.White,
    secondary = GlacierSecondary,
    onSecondary = Color.White,
    tertiary = GlacierAccent,
    background = GlacierLightBg,
    surface = GlacierSurface,
    onBackground = GlacierTextPrimary,
    onSurface = GlacierTextPrimary,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155)
)

private val CrimsonDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color(0xFF1C050E),
    secondary = CrimsonSecondary,
    onSecondary = Color(0xFF1C050E),
    tertiary = CrimsonAccent,
    background = CrimsonDarkBg,
    surface = CrimsonSurface,
    onBackground = CrimsonTextPrimary,
    onSurface = CrimsonTextPrimary,
    surfaceVariant = Color(0xFF4C0519),
    onSurfaceVariant = Color(0xFFFFE4E6)
)

@Composable
fun SecureMessengerTheme(
    themeName: String = "Cyber Obsidian",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Nordic Glacier" -> GlacierLightColorScheme
        "Royal Crimson" -> CrimsonDarkColorScheme
        else -> CyberDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
