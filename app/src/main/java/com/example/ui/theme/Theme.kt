package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SoloWorkPalette(
    val isDark: Boolean,
    val presetName: String,
    val accentName: String,
    val fontName: String,
    val customizeLayoutStyle: Boolean,
    val sidebarCompact: Boolean,
    val background: Color,
    val sidebarBg: Color,
    val cardBg: Color,
    val cardElevatedBg: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSecondary: Color
)

val LocalSoloWorkPalette = staticCompositionLocalOf {
    SoloWorkPalette(
        isDark = true,
        presetName = "Classic Dark",
        accentName = "Indigo",
        fontName = "Inter",
        customizeLayoutStyle = true,
        sidebarCompact = false,
        background = ClassicDarkBg,
        sidebarBg = ClassicDarkSidebar,
        cardBg = ClassicDarkCard,
        cardElevatedBg = ClassicDarkCardElevated,
        border = ClassicDarkBorder,
        textPrimary = ClassicDarkTextPrimary,
        textSecondary = ClassicDarkTextSecondary,
        accent = AccentIndigo,
        accentSecondary = AccentViolet
    )
}

fun resolveAccentPair(accentName: String): Pair<Color, Color> = when (accentName) {
    "Emerald" -> AccentEmerald to AccentEmeraldLight
    "Violet" -> AccentViolet to AccentVioletLight
    "Amber" -> AccentAmber to AccentAmberLight
    "Crimson" -> AccentCrimson to AccentCrimsonLight
    else -> AccentIndigo to AccentViolet
}

fun resolveSoloWorkPalette(
    themeMode: String,
    themePreset: String,
    accentColor: String,
    fontFamily: String,
    customizeLayoutStyle: Boolean,
    sidebarCompact: Boolean
): SoloWorkPalette {
    val (accent, accentSec) = resolveAccentPair(accentColor)
    val isDark = themeMode != "Light" && themePreset != "Daylight"

    return when {
        themeMode == "Light" || themePreset == "Daylight" -> SoloWorkPalette(
            isDark = false,
            presetName = themePreset,
            accentName = accentColor,
            fontName = fontFamily,
            customizeLayoutStyle = customizeLayoutStyle,
            sidebarCompact = sidebarCompact,
            background = DaylightBg,
            sidebarBg = DaylightSidebar,
            cardBg = DaylightCard,
            cardElevatedBg = DaylightCardElevated,
            border = DaylightBorder,
            textPrimary = DaylightTextPrimary,
            textSecondary = DaylightTextSecondary,
            accent = accent,
            accentSecondary = accentSec
        )
        themePreset == "Solarized" -> SoloWorkPalette(
            isDark = true,
            presetName = themePreset,
            accentName = accentColor,
            fontName = fontFamily,
            customizeLayoutStyle = customizeLayoutStyle,
            sidebarCompact = sidebarCompact,
            background = SolarizedDarkBg,
            sidebarBg = SolarizedDarkSidebar,
            cardBg = SolarizedDarkCard,
            cardElevatedBg = SolarizedDarkCardElevated,
            border = SolarizedDarkBorder,
            textPrimary = SolarizedDarkTextPrimary,
            textSecondary = SolarizedDarkTextSecondary,
            accent = accent,
            accentSecondary = accentSec
        )
        themePreset == "Cyberpunk" -> SoloWorkPalette(
            isDark = true,
            presetName = themePreset,
            accentName = accentColor,
            fontName = fontFamily,
            customizeLayoutStyle = customizeLayoutStyle,
            sidebarCompact = sidebarCompact,
            background = CyberpunkBg,
            sidebarBg = CyberpunkSidebar,
            cardBg = CyberpunkCard,
            cardElevatedBg = CyberpunkCardElevated,
            border = CyberpunkBorder,
            textPrimary = CyberpunkTextPrimary,
            textSecondary = CyberpunkTextSecondary,
            accent = accent,
            accentSecondary = accentSec
        )
        else -> SoloWorkPalette(
            isDark = true,
            presetName = "Classic Dark",
            accentName = accentColor,
            fontName = fontFamily,
            customizeLayoutStyle = customizeLayoutStyle,
            sidebarCompact = sidebarCompact,
            background = ClassicDarkBg,
            sidebarBg = ClassicDarkSidebar,
            cardBg = ClassicDarkCard,
            cardElevatedBg = ClassicDarkCardElevated,
            border = ClassicDarkBorder,
            textPrimary = ClassicDarkTextPrimary,
            textSecondary = ClassicDarkTextSecondary,
            accent = accent,
            accentSecondary = accentSec
        )
    }
}

@Composable
fun SoloWorkTheme(
    themeMode: String = "Dark",
    themePreset: String = "Classic Dark",
    accentColor: String = "Indigo",
    fontFamily: String = "Inter",
    customizeLayoutStyle: Boolean = true,
    sidebarCompact: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = resolveSoloWorkPalette(
        themeMode = themeMode,
        themePreset = themePreset,
        accentColor = accentColor,
        fontFamily = fontFamily,
        customizeLayoutStyle = customizeLayoutStyle,
        sidebarCompact = sidebarCompact
    )

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.accentSecondary,
            background = palette.background,
            surface = palette.cardBg,
            surfaceVariant = palette.cardElevatedBg,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.accentSecondary,
            background = palette.background,
            surface = palette.cardBg,
            surfaceVariant = palette.cardElevatedBg,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    }

    val typography = buildSoloWorkTypography(fontFamily)

    CompositionLocalProvider(LocalSoloWorkPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
