package com.topjohnwu.magisk.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.topjohnwu.magisk.core.Config

@Composable
fun magiskComposeColorScheme(
    useDynamicColor: Boolean,
    darkTheme: Boolean,
    selectedTheme: Theme = Theme.selected
): ColorScheme {
    val dynamicSupported = Theme.supportsMonet
    val shouldApplyDynamic = selectedTheme == Theme.Default && useDynamicColor && dynamicSupported
    val scheme = if (selectedTheme == Theme.Default) {
        when {
            shouldApplyDynamic -> {
                if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
                else dynamicLightColorScheme(LocalContext.current)
            }

            else -> defaultFallbackScheme(darkTheme)
        }
    } else {
        composeStaticThemeScheme(selectedTheme, darkTheme)
    }

    val forceAmoledBase = darkTheme && Config.darkTheme == Config.Value.DARK_THEME_AMOLED
    return if (forceAmoledBase) scheme.toAmoledBaseScheme() else scheme
}

@Composable
private fun composeStaticThemeScheme(theme: Theme, darkTheme: Boolean): ColorScheme {
    val seed = when (theme) {
        Theme.Ruby -> RUBY
        Theme.MemCho -> MEM_CHO
        Theme.Aqua -> AQUA
        Theme.SungJinWoo -> SUNG_JIN_WOO
        Theme.Custom -> customSeed()
        Theme.Default -> null
    } ?: return defaultFallbackScheme(darkTheme)
    return seed.toColorScheme(darkTheme)
}

private fun customSeed(): ComposeThemeSeed = ComposeThemeSeed(
    lightPrimary = Color(Config.themeCustomLightPrimary),
    darkPrimary = Color(Config.themeCustomDarkPrimary),
    lightSecondary = Color(Config.themeCustomLightSecondary),
    darkSecondary = Color(Config.themeCustomDarkSecondary),
    lightSurface = Color(Config.themeCustomLightSurface),
    darkSurface = Color(Config.themeCustomDarkSurface),
    lightOnSurface = Color(Config.themeCustomLightOnSurface),
    darkOnSurface = Color(Config.themeCustomDarkOnSurface),
    lightError = Color(Config.themeCustomLightError),
    darkError = Color(Config.themeCustomDarkError)
)

private data class ComposeThemeSeed(
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightSecondary: Color,
    val darkSecondary: Color,
    val lightSurface: Color,
    val darkSurface: Color,
    val lightOnSurface: Color,
    val darkOnSurface: Color,
    val lightError: Color,
    val darkError: Color
)

private fun ComposeThemeSeed.toColorScheme(darkTheme: Boolean): ColorScheme {
    val primary = if (darkTheme) darkPrimary else lightPrimary
    val secondary = if (darkTheme) darkSecondary else lightSecondary
    val surface = if (darkTheme) darkSurface else lightSurface
    val onSurface = if (darkTheme) darkOnSurface else lightOnSurface
    val error = if (darkTheme) darkError else lightError

    val blendTarget = if (darkTheme) BLACK else WHITE
    val variantTarget = if (darkTheme) WHITE else BLACK
    val tertiary = blend(primary, secondary, 0.42f)

    val primaryContainer = blend(primary, blendTarget, if (darkTheme) 0.42f else 0.78f)
    val secondaryContainer = blend(secondary, blendTarget, if (darkTheme) 0.40f else 0.76f)
    val tertiaryContainer = blend(tertiary, blendTarget, if (darkTheme) 0.40f else 0.76f)
    val errorContainer = blend(error, blendTarget, if (darkTheme) 0.40f else 0.78f)
    val surfaceVariant = blend(surface, variantTarget, if (darkTheme) 0.08f else 0.05f)
    val surfaceContainerLowest = if (darkTheme) {
        blend(surface, BLACK, 0.24f)
    } else {
        blend(surface, WHITE, 0.72f)
    }
    val surfaceContainerLow = if (darkTheme) {
        blend(surface, WHITE, 0.03f)
    } else {
        blend(surface, variantTarget, 0.025f)
    }
    val surfaceContainer = if (darkTheme) {
        blend(surface, WHITE, 0.055f)
    } else {
        blend(surface, variantTarget, 0.04f)
    }
    val surfaceContainerHigh = if (darkTheme) {
        blend(surface, WHITE, 0.085f)
    } else {
        blend(surface, variantTarget, 0.065f)
    }
    val surfaceContainerHighest = if (darkTheme) {
        blend(surface, WHITE, 0.12f)
    } else {
        blend(surface, variantTarget, 0.09f)
    }
    val surfaceDim = if (darkTheme) {
        blend(surface, BLACK, 0.18f)
    } else {
        blend(surface, variantTarget, 0.08f)
    }
    val surfaceBright = if (darkTheme) {
        blend(surface, WHITE, 0.16f)
    } else {
        blend(surface, WHITE, 0.48f)
    }

    val outline = blend(onSurface, surface, 0.58f)
    val outlineVariant = blend(onSurface, surface, 0.74f)
    val inverseSurface =
        blend(surface, if (darkTheme) WHITE else BLACK, if (darkTheme) 0.86f else 0.80f)
    val inversePrimary =
        blend(primary, if (darkTheme) WHITE else BLACK, if (darkTheme) 0.34f else 0.22f)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = contentColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contentColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = contentColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contentColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contentColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contentColorFor(tertiaryContainer),
            error = error,
            onError = contentColorFor(error),
            errorContainer = errorContainer,
            onErrorContainer = contentColorFor(errorContainer),
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceDim = surfaceDim,
            surfaceBright = surfaceBright,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = blend(onSurface, BLACK, 0.16f),
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = contentColorFor(inverseSurface),
            inversePrimary = inversePrimary,
            surfaceTint = primary,
            scrim = BLACK
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = contentColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contentColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = contentColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contentColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contentColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contentColorFor(tertiaryContainer),
            error = error,
            onError = contentColorFor(error),
            errorContainer = errorContainer,
            onErrorContainer = contentColorFor(errorContainer),
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceDim = surfaceDim,
            surfaceBright = surfaceBright,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = blend(onSurface, WHITE, 0.22f),
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = contentColorFor(inverseSurface),
            inversePrimary = inversePrimary,
            surfaceTint = primary,
            scrim = BLACK
        )
    }
}

private fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.42f) BLACK else WHITE
}

private fun blend(base: Color, overlay: Color, amount: Float): Color {
    val safeAmount = amount.coerceIn(0f, 1f)
    val inv = 1f - safeAmount
    return Color(
        red = base.red * inv + overlay.red * safeAmount,
        green = base.green * inv + overlay.green * safeAmount,
        blue = base.blue * inv + overlay.blue * safeAmount,
        alpha = 1f
    )
}

private fun defaultFallbackScheme(darkTheme: Boolean): ColorScheme = RUBY.toColorScheme(darkTheme)

private fun ColorScheme.toAmoledBaseScheme(): ColorScheme = copy(
    background = BLACK,
    surface = BLACK,
    surfaceVariant = Color(0xFF0D0D0D),
    surfaceDim = BLACK,
    surfaceBright = Color(0xFF171717),
    surfaceContainerLowest = BLACK,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF090909),
    surfaceContainerHigh = Color(0xFF0E0E0E),
    surfaceContainerHighest = Color(0xFF141414),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF141414),
    scrim = BLACK
)

private val WHITE = Color(0xFFFFFFFF)
private val BLACK = Color(0xFF000000)

private val RUBY = ComposeThemeSeed(
    lightPrimary = Color(0xFFF06292),
    darkPrimary = Color(0xFFF48FB1),
    lightSecondary = Color(0xFFD81B60),
    darkSecondary = Color(0xFFF06292),
    lightSurface = Color(0xFFFFF5F8),
    darkSurface = Color(0xFF211017),
    lightOnSurface = Color(0xFF3C1020),
    darkOnSurface = Color(0xFFFCE4EC),
    lightError = Color(0xFFB00020),
    darkError = Color(0xFFCF6679)
)

private val MEM_CHO = ComposeThemeSeed(
    lightPrimary = Color(0xFFFFD54F),
    darkPrimary = Color(0xFFFFE082),
    lightSecondary = Color(0xFFFBC02D),
    darkSecondary = Color(0xFFFFD54F),
    lightSurface = Color(0xFFFFFBEA),
    darkSurface = Color(0xFF211E10),
    lightOnSurface = Color(0xFF3E2723),
    darkOnSurface = Color(0xFFFFF9C4),
    lightError = Color(0xFFB00020),
    darkError = Color(0xFFCF6679)
)

private val AQUA = ComposeThemeSeed(
    lightPrimary = Color(0xFF4FC3F7),
    darkPrimary = Color(0xFF81D4FA),
    lightSecondary = Color(0xFF0288D1),
    darkSecondary = Color(0xFF4FC3F7),
    lightSurface = Color(0xFFF0FBFF),
    darkSurface = Color(0xFF0D1820),
    lightOnSurface = Color(0xFF01579B),
    darkOnSurface = Color(0xFFE1F5FE),
    lightError = Color(0xFFB00020),
    darkError = Color(0xFFCF6679)
)

private val SUNG_JIN_WOO = ComposeThemeSeed(
    lightPrimary = Color(0xFF9575CD),
    darkPrimary = Color(0xFFB39DDB),
    lightSecondary = Color(0xFF5E35B1),
    darkSecondary = Color(0xFF9575CD),
    lightSurface = Color(0xFFF6F1FF),
    darkSurface = Color(0xFF12101F),
    lightOnSurface = Color(0xFF311B92),
    darkOnSurface = Color(0xFFEDE7F6),
    lightError = Color(0xFFB00020),
    darkError = Color(0xFFCF6679)
)
