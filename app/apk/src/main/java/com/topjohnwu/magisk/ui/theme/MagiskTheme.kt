package com.topjohnwu.magisk.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.ui.motion.ProvideMotionCenter
import com.topjohnwu.magisk.ui.theme.themes.Black
import com.topjohnwu.magisk.ui.theme.themes.ThemeCatalog
import com.topjohnwu.magisk.ui.theme.themes.ThemeSeed
import com.topjohnwu.magisk.ui.theme.themes.White
import com.topjohnwu.magisk.ui.theme.themes.blend
import com.topjohnwu.magisk.ui.theme.themes.contentColorFor

@Composable
fun MagiskTheme(
    themeOption: ThemeOption = ThemeOption.selected,
    darkTheme: Boolean = shouldUseDarkTheme(),
    darkThemeMode: Int = Config.darkTheme,
    useDynamicColor: Boolean = true,
    themeVersion: Int = 0,
    setSolidBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        themeOption == ThemeOption.Default &&
                useDynamicColor &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val fallbackSeed = ThemeCatalog.seedFor(themeOption)
            val dynamicSeed = dynamicThemeSeed(context, fallbackSeed)
            dynamicSeed.toColorScheme(darkTheme)
        }

        else -> ThemeCatalog.seedFor(themeOption).toColorScheme(darkTheme)
    }.let {
        if (darkTheme && Config.darkTheme == Config.Value.DARK_THEME_AMOLED) {
            it.toAmoledScheme()
        } else {
            it
        }
    }

    key(themeOption, darkTheme, darkThemeMode, themeVersion) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MagiskTypography,
            shapes = MagiskShapes
        ) {
            if (setSolidBackground) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    ProvideMotionCenter(content)
                }
            } else {
                ProvideMotionCenter(content)
            }
        }
    }
}

@Composable
fun shouldUseDarkTheme(): Boolean {
    return shouldUseDarkTheme(Config.darkTheme)
}

@Composable
fun shouldUseDarkTheme(mode: Int): Boolean {
    return when (mode) {
        Config.Value.DARK_THEME_AMOLED -> true
        -1 -> isSystemInDarkTheme()
        0 -> false
        else -> true
    }
}

private val MagiskShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val BaseTypography = Typography()

private val MagiskTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
)

private fun ThemeSeed.toColorScheme(darkTheme: Boolean): ColorScheme {
    // Material color roles are composited by their consumers. Keeping seed
    // alpha here would make custom palettes doubly translucent and can expose
    // unrelated content below surfaces, so normalize every base role first.
    val primary = (if (darkTheme) darkPrimary else lightPrimary).copy(alpha = 1f)
    val secondary = (if (darkTheme) darkSecondary else lightSecondary).copy(alpha = 1f)
    val tertiary = (if (darkTheme) darkTertiary else lightTertiary).copy(alpha = 1f)
    val surface = (if (darkTheme) darkSurface else lightSurface).copy(alpha = 1f)
    val onSurface = (if (darkTheme) darkOnSurface else lightOnSurface).copy(alpha = 1f)
    val error = (if (darkTheme) darkError else lightError).copy(alpha = 1f)
    val target = if (darkTheme) Black else White
    val opposite = if (darkTheme) White else Black

    val primaryContainer = blend(primary, target, if (darkTheme) 0.42f else 0.78f)
    val secondaryContainer = blend(secondary, target, if (darkTheme) 0.40f else 0.76f)
    val tertiaryContainer = blend(tertiary, target, if (darkTheme) 0.40f else 0.76f)
    val errorContainer = blend(error, target, if (darkTheme) 0.40f else 0.78f)
    val surfaceVariant = blend(surface, opposite, if (darkTheme) 0.08f else 0.05f)
    val surfaceContainerLowest = if (darkTheme) {
        blend(surface, Black, 0.24f)
    } else {
        blend(surface, White, 0.72f)
    }
    val surfaceContainerLow = if (darkTheme) {
        blend(surface, White, 0.03f)
    } else {
        blend(surface, opposite, 0.025f)
    }
    val surfaceContainer = if (darkTheme) {
        blend(surface, White, 0.055f)
    } else {
        blend(surface, opposite, 0.04f)
    }
    val surfaceContainerHigh = if (darkTheme) {
        blend(surface, White, 0.085f)
    } else {
        blend(surface, opposite, 0.065f)
    }
    val surfaceContainerHighest = if (darkTheme) {
        blend(surface, White, 0.12f)
    } else {
        blend(surface, opposite, 0.09f)
    }
    val surfaceDim = if (darkTheme) {
        blend(surface, Black, 0.18f)
    } else {
        blend(surface, opposite, 0.08f)
    }
    val surfaceBright = if (darkTheme) {
        blend(surface, White, 0.16f)
    } else {
        blend(surface, White, 0.48f)
    }
    val inverseSurface = blend(surface, opposite, if (darkTheme) 0.86f else 0.80f)
    val inversePrimary = blend(primary, opposite, if (darkTheme) 0.34f else 0.22f)

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
            onSurfaceVariant = blend(onSurface, Black, 0.16f),
            outline = blend(onSurface, surface, 0.58f),
            outlineVariant = blend(onSurface, surface, 0.74f),
            inverseSurface = inverseSurface,
            inverseOnSurface = contentColorFor(inverseSurface),
            inversePrimary = inversePrimary,
            surfaceTint = primary,
            scrim = Black
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
            onSurfaceVariant = blend(onSurface, White, 0.22f),
            outline = blend(onSurface, surface, 0.58f),
            outlineVariant = blend(onSurface, surface, 0.74f),
            inverseSurface = inverseSurface,
            inverseOnSurface = contentColorFor(inverseSurface),
            inversePrimary = inversePrimary,
            surfaceTint = primary,
            scrim = Black
        )
    }
}

private fun ColorScheme.toAmoledScheme(): ColorScheme = copy(
    background = Black,
    surface = Black,
    surfaceVariant = Color(0xFF0D0D0D),
    surfaceDim = Black,
    surfaceBright = Color(0xFF171717),
    surfaceContainerLowest = Black,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF090909),
    surfaceContainerHigh = Color(0xFF0E0E0E),
    surfaceContainerHighest = Color(0xFF141414),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF141414),
    scrim = Black
)

internal fun androidSystemColor(context: Context, name: String, fallback: Color): Color {
    val id = context.resources.getIdentifier(name, "color", "android")
    if (id == 0) return fallback
    val colorInt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getColor(id)
    } else {
        @Suppress("DEPRECATION")
        context.resources.getColor(id)
    }
    return Color(colorInt)
}

internal fun dynamicThemeSeed(context: Context, fallback: ThemeSeed): ThemeSeed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return fallback
    }
    val dynamicLightSurface = androidSystemColor(
        context,
        "system_neutral1_50",
        blend(fallback.lightSurface, Black, 0.06f)
    )
    val dynamicDarkSurface = blend(
        androidSystemColor(context, "system_neutral1_900", fallback.darkSurface),
        Black,
        0.12f
    )
    return ThemeSeed(
        lightPrimary = androidSystemColor(context, "system_accent1_900", fallback.lightPrimary),
        darkPrimary = androidSystemColor(context, "system_accent1_400", fallback.darkPrimary),
        lightSecondary = androidSystemColor(context, "system_accent2_900", fallback.lightSecondary),
        darkSecondary = androidSystemColor(context, "system_accent2_400", fallback.darkSecondary),
        lightTertiary = androidSystemColor(context, "system_accent3_900", fallback.lightTertiary),
        darkTertiary = androidSystemColor(context, "system_accent3_400", fallback.darkTertiary),
        lightSurface = dynamicLightSurface,
        darkSurface = dynamicDarkSurface,
        lightOnSurface = androidSystemColor(context, "system_neutral1_900", fallback.lightOnSurface),
        darkOnSurface = androidSystemColor(context, "system_neutral1_100", fallback.darkOnSurface),
        lightError = fallback.lightError,
        darkError = fallback.darkError
    )
}
