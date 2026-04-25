package com.topjohnwu.magisk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val MagiskExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val BaseTypography = Typography()

private val MagiskExpressiveTypography = BaseTypography.copy(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Black),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Black),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Black),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Black),
    labelMedium = BaseTypography.labelMedium.copy(fontWeight = FontWeight.Bold)
)

@Composable
fun MagiskExpressiveTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MagiskExpressiveShapes,
        typography = MagiskExpressiveTypography,
        content = content
    )
}
