package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AuraThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryDark: Color,
    val secondaryDark: Color,
    val aiDark: Color,
    val primaryLight: Color,
    val secondaryLight: Color
) {
    GLACIAL_CYAN(
        id = "GLACIAL_CYAN",
        title = "Glacial Cyan",
        subtitle = "Signature sapphire & ice crystal",
        primaryDark = Color(0xFF0A84FF),
        secondaryDark = Color(0xFF5CE1FF),
        aiDark = Color(0xFF38BDF8),
        primaryLight = Color(0xFF0A84FF),
        secondaryLight = Color(0xFF0284C7)
    ),
    EMERALD_MINT(
        id = "EMERALD_MINT",
        title = "Emerald Mint",
        subtitle = "Lush botanical jade & seafoam",
        primaryDark = Color(0xFF10B981),
        secondaryDark = Color(0xFF34D399),
        aiDark = Color(0xFF2DD4BF),
        primaryLight = Color(0xFF059669),
        secondaryLight = Color(0xFF0D9488)
    ),
    ROYAL_VIOLET(
        id = "ROYAL_VIOLET",
        title = "Royal Violet",
        subtitle = "Nebula amethyst & electric iris",
        primaryDark = Color(0xFF8B5CF6),
        secondaryDark = Color(0xFFC084FC),
        aiDark = Color(0xFFA78BFA),
        primaryLight = Color(0xFF7C3AED),
        secondaryLight = Color(0xFF6366F1)
    ),
    SUNSET_GOLD(
        id = "SUNSET_GOLD",
        title = "Amber Horizon",
        subtitle = "Warm solar gold & coral prism",
        primaryDark = Color(0xFFF59E0B),
        secondaryDark = Color(0xFFFBBF24),
        aiDark = Color(0xFFFB923C),
        primaryLight = Color(0xFFD97706),
        secondaryLight = Color(0xFFEA580C)
    );

    companion object {
        fun fromId(id: String?): AuraThemePreset =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: GLACIAL_CYAN
    }
}

@Immutable
data class AuraCustomColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val cardBg: Color,
    val surfaceElevated: Color,
    val border: Color,
    val glassCard: Color,
    val glassElevated: Color,
    val glassBorder: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val aiAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean = true,
    val presetId: String = "GLACIAL_CYAN"
)

val LocalAuraColors = staticCompositionLocalOf {
    AuraCustomColors(
        bgPrimary = AuraGlassDarkBg,
        bgSecondary = AuraGlassDarkBgSecondary,
        cardBg = AuraGlassCardBg,
        surfaceElevated = AuraGlassCardElevated,
        border = AuraGlassBorderSubtle,
        glassCard = AuraGlassCardBg,
        glassElevated = AuraGlassCardElevated,
        glassBorder = AuraGlassBorderLight,
        primaryAccent = AuraPrimaryAccent,
        secondaryAccent = AuraSecondaryAccent,
        aiAccent = AuraAiAccent,
        success = AuraSuccess,
        warning = AuraWarning,
        danger = AuraDanger,
        textPrimary = AuraTextPrimaryDark,
        textSecondary = AuraTextSecondaryDark,
        textMuted = AuraTextMutedDark,
        isDark = true,
        presetId = "GLACIAL_CYAN"
    )
}

@Composable
fun AuraTheme(
    darkTheme: Boolean = true,
    accentPreset: String = "GLACIAL_CYAN",
    content: @Composable () -> Unit
) {
    val preset = AuraThemePreset.fromId(accentPreset)

    val customColors = if (darkTheme) {
        AuraCustomColors(
            bgPrimary = AuraGlassDarkBg,
            bgSecondary = AuraGlassDarkBgSecondary,
            cardBg = AuraGlassCardBg,
            surfaceElevated = AuraGlassCardElevated,
            border = AuraGlassBorderSubtle,
            glassCard = AuraGlassCardBg,
            glassElevated = AuraGlassCardElevated,
            glassBorder = AuraGlassBorderLight,
            primaryAccent = preset.primaryDark,
            secondaryAccent = preset.secondaryDark,
            aiAccent = preset.aiDark,
            success = AuraSuccess,
            warning = AuraWarning,
            danger = AuraDanger,
            textPrimary = AuraTextPrimaryDark,
            textSecondary = AuraTextSecondaryDark,
            textMuted = AuraTextMutedDark,
            isDark = true,
            presetId = preset.id
        )
    } else {
        AuraCustomColors(
            bgPrimary = AuraGlassLightBg,
            bgSecondary = AuraGlassLightBgSecondary,
            cardBg = AuraGlassLightCardBg,
            surfaceElevated = AuraGlassLightCardElevated,
            border = preset.primaryLight.copy(alpha = 0.22f),
            glassCard = AuraGlassLightCardBg,
            glassElevated = AuraGlassLightCardElevated,
            glassBorder = AuraGlassLightBorder,
            primaryAccent = preset.primaryLight,
            secondaryAccent = preset.secondaryLight,
            aiAccent = preset.primaryLight,
            success = Color(0xFF059669),
            warning = Color(0xFF2563EB),
            danger = Color(0xFFE11D48),
            textPrimary = AuraTextPrimaryLight,
            textSecondary = AuraTextSecondaryLight,
            textMuted = AuraTextMutedLight,
            isDark = false,
            presetId = preset.id
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = preset.primaryDark,
            onPrimary = Color.White,
            primaryContainer = AuraGlassCardElevated,
            onPrimaryContainer = Color.White,
            secondary = preset.secondaryDark,
            onSecondary = Color.Black,
            secondaryContainer = AuraGlassCardElevated,
            onSecondaryContainer = preset.secondaryDark,
            tertiary = preset.aiDark,
            onTertiary = Color.Black,
            background = AuraGlassDarkBg,
            onBackground = AuraTextPrimaryDark,
            surface = AuraGlassCardBg,
            onSurface = AuraTextPrimaryDark,
            surfaceVariant = AuraGlassCardElevated,
            onSurfaceVariant = AuraTextSecondaryDark,
            outline = AuraGlassBorderSubtle,
            error = AuraDanger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = preset.primaryLight,
            onPrimary = Color.White,
            primaryContainer = AuraGlassLightCardElevated,
            onPrimaryContainer = AuraTextPrimaryLight,
            secondary = preset.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = AuraGlassLightCardElevated,
            onSecondaryContainer = preset.secondaryLight,
            tertiary = preset.primaryLight,
            onTertiary = Color.White,
            background = AuraGlassLightBg,
            onBackground = AuraTextPrimaryLight,
            surface = AuraGlassLightCardBg,
            onSurface = AuraTextPrimaryLight,
            surfaceVariant = AuraGlassLightCardElevated,
            onSurfaceVariant = AuraTextSecondaryLight,
            outline = Color(0x330B1528),
            error = AuraDanger,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalAuraColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
