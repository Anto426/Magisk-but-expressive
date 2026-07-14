package com.topjohnwu.magisk.ui.theme.themes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal val White = Color(0xFFFFFFFF)
internal val Black = Color(0xFF000000)

internal fun contentColorFor(color: Color): Color {
    // 0.179 is the luminance crossover where black and white have equal
    // WCAG contrast. Using the higher-contrast side keeps custom and bundled
    // palettes readable, especially with pink, yellow and light-blue accents.
    return if (color.luminance() > 0.179f) Black else White
}

internal fun blend(base: Color, overlay: Color, amount: Float): Color {
    val safeAmount = amount.coerceIn(0f, 1f)
    val inverse = 1f - safeAmount
    return Color(
        red = base.red * inverse + overlay.red * safeAmount,
        green = base.green * inverse + overlay.green * safeAmount,
        blue = base.blue * inverse + overlay.blue * safeAmount,
        alpha = 1f
    )
}
