package com.topjohnwu.magisk.ui.theme.themes

import androidx.compose.ui.graphics.Color
import com.topjohnwu.magisk.ui.theme.ThemeOption
import com.topjohnwu.magisk.core.R as CoreR

internal val DefaultThemeDefinition = ThemeDefinition(
    option = ThemeOption.Default,
    labelRes = CoreR.string.theme_option_default_dynamic,
    seed = {
        ThemeSeed(
            lightPrimary = Color(0xFF8F174F),
            darkPrimary = Color(0xFFE889B3),
            lightSecondary = Color(0xFF00545A),
            darkSecondary = Color(0xFF52B8C2),
            lightTertiary = Color(0xFF6F4100),
            darkTertiary = Color(0xFFE8AC59),
            lightSurface = Color(0xFFF0E4EA),
            darkSurface = Color(0xFF100B10),
            lightOnSurface = Color(0xFF1F171C),
            darkOnSurface = Color(0xFFEBD7E0),
            lightError = Color(0xFFBA1A1A),
            darkError = Color(0xFFFFB4AB)
        )
    }
)
