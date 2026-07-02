package com.topjohnwu.magisk.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.theme.ThemeCustomColorSlot
import com.topjohnwu.magisk.ui.theme.ThemeCustomColors
import com.topjohnwu.magisk.ui.theme.ThemeOption
import com.topjohnwu.magisk.ui.theme.shouldUseDarkTheme
import com.topjohnwu.magisk.ui.theme.themes.ThemeCatalog
import android.graphics.Color as AndroidColor
import com.topjohnwu.magisk.core.R as CoreR

data class ThemeModeOption(
    val mode: Int,
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int
)

data class BottomBarStyleOption(
    val style: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int
)

fun bottomBarStyleOptions(): List<BottomBarStyleOption> = listOf(
    BottomBarStyleOption(
        style = Config.Value.BOTTOM_BAR_AUTO,
        titleRes = CoreR.string.bottom_bar_style_auto,
        subtitleRes = CoreR.string.bottom_bar_style_auto_subtitle
    ),
    BottomBarStyleOption(
        style = Config.Value.BOTTOM_BAR_FLOATING,
        titleRes = CoreR.string.bottom_bar_style_floating,
        subtitleRes = CoreR.string.bottom_bar_style_floating_subtitle
    ),
    BottomBarStyleOption(
        style = Config.Value.BOTTOM_BAR_FIXED,
        titleRes = CoreR.string.bottom_bar_style_fixed,
        subtitleRes = CoreR.string.bottom_bar_style_fixed_subtitle
    )
)

fun themeModeOptions(): List<ThemeModeOption> = listOf(
    ThemeModeOption(
        mode = -1,
        icon = Icons.Rounded.BrightnessAuto,
        titleRes = CoreR.string.settings_dark_mode_system,
        subtitleRes = CoreR.string.theme_dark_mode_subtitle_system
    ),
    ThemeModeOption(
        mode = 0,
        icon = Icons.Rounded.LightMode,
        titleRes = CoreR.string.settings_dark_mode_light,
        subtitleRes = CoreR.string.theme_dark_mode_subtitle_light
    ),
    ThemeModeOption(
        mode = 1,
        icon = Icons.Rounded.DarkMode,
        titleRes = CoreR.string.settings_dark_mode_dark,
        subtitleRes = CoreR.string.theme_dark_mode_subtitle_dark
    ),
    ThemeModeOption(
        mode = Config.Value.DARK_THEME_AMOLED,
        icon = Icons.Rounded.AutoAwesome,
        titleRes = CoreR.string.settings_dark_mode_amoled,
        subtitleRes = CoreR.string.theme_dark_mode_subtitle_amoled
    )
)

fun Int.toColorHex(): String = String.format("#%08X", this)

@Composable
fun BottomBarStyleItem(
    option: BottomBarStyleOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    MagiskSettingsListItem(
        title = stringResource(option.titleRes),
        subtitle = stringResource(option.subtitleRes),
        leadingIcon = Icons.Rounded.Tune,
        selected = selected,
        trailingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        onClick = onClick
    )
}

@Composable
fun ThemeCardGrid(
    selectedIndex: Int,
    darkMode: Int,
    onThemeClick: (Int, ThemeOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeOption.displayOrder.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowOptions.forEach { option ->
                    val index = ThemeOption.displayOrder.indexOf(option)
                    ThemeOptionCard(
                        option = option,
                        selected = selectedIndex == index,
                        darkMode = darkMode,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeClick(index, option) }
                    )
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    option: ThemeOption,
    selected: Boolean,
    darkMode: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val previewColors = themePreviewColors(option = option, darkMode = darkMode)
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        MagiskComponentDefaults.CardBorder
    }

    MagiskCard(
        modifier = modifier.height(190.dp),
        containerColor = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = border,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Brush.linearGradient(previewColors)),
                contentAlignment = Alignment.Center
            ) {
                ThemeCharacter(
                    option = option,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )

                if (selected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(option.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ThemePreviewSwatches(option = option, darkMode = darkMode)
            }
        }
    }
}

@Composable
fun ThemeCharacter(
    option: ThemeOption,
    modifier: Modifier = Modifier
) {
    val characterRes = option.characterRes()
    if (characterRes != null) {
        Image(
            painter = painterResource(characterRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = if (option == ThemeOption.Custom) Icons.Rounded.Brush else Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(54.dp),
            tint = Color.White.copy(alpha = 0.78f)
        )
    }
}

@DrawableRes
fun ThemeOption.characterRes(): Int? = when (this) {
    ThemeOption.Ruby -> CoreR.drawable.theme_ruby
    ThemeOption.MemCho -> CoreR.drawable.theme_memcho
    ThemeOption.Aqua -> CoreR.drawable.theme_aqua
    ThemeOption.SungJinWoo -> CoreR.drawable.theme_sung_jinwoo
    ThemeOption.Default,
    ThemeOption.Custom -> null
}

@Composable
fun ThemeModeItem(
    option: ThemeModeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    MagiskSettingsListItem(
        title = stringResource(option.titleRes),
        subtitle = stringResource(option.subtitleRes),
        leadingIcon = option.icon,
        selected = selected,
        trailingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        onClick = onClick
    )
}

@Composable
fun ThemePreviewSwatches(
    option: ThemeOption,
    darkMode: Int
) {
    val colors = themePreviewColors(option = option, darkMode = darkMode).take(4)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color ->
            ThemeColorDot(color = color)
        }
    }
}

@Composable
fun ThemeColorDot(color: Color) {
    Surface(
        modifier = Modifier.size(18.dp),
        shape = CircleShape,
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {}
}

@Composable
fun themePreviewColors(
    option: ThemeOption,
    darkMode: Int
): List<Color> {
    if (option == ThemeOption.Default && ThemeOption.supportsDynamicColor) {
        return listOf(
            Color(0xFFE85CF0),
            Color(0xFF8A7BFF),
            Color(0xFF58B8FF),
            Color(0xFF57E2E7),
            Color(0xFF62EB9A),
            Color(0xFFB6EF64)
        )
    }

    val seed = ThemeCatalog.seedFor(option)
    val dark = shouldUseDarkTheme(darkMode)
    return if (dark) {
        listOf(seed.darkPrimary, seed.darkSecondary, seed.darkTertiary, seed.darkSurface)
    } else {
        listOf(seed.lightPrimary, seed.lightSecondary, seed.lightTertiary, seed.lightSurface)
    }
}

@Composable
fun CustomThemeBottomSheet(
    colors: ThemeCustomColors,
    onColorChanged: (ThemeCustomColorSlot, Int) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    MagiskBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(CoreR.string.theme_custom_palette),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(CoreR.string.theme_custom_palette_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ThemeCustomColorSlot.entries.forEach { slot ->
                key(slot) {
                    CustomColorRow(
                        label = stringResource(slot.labelRes),
                        colorInt = colors.value(slot),
                        onColorChange = { onColorChanged(slot, it) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(MagiskComponentDefaults.ActionHeight),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(1f)
                        .height(MagiskComponentDefaults.ActionHeight),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Text(stringResource(CoreR.string.apply))
                }
            }
        }
    }
}

@Composable
fun CustomColorRow(
    label: String,
    colorInt: Int,
    onColorChange: (Int) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }

    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { showEditor = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(colorInt),
                modifier = Modifier.size(34.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = colorInt.toColorHex(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showEditor) {
        ColorChannelDialog(
            title = label,
            initialColor = colorInt,
            onDismiss = { showEditor = false },
            onConfirm = { color ->
                onColorChange(color)
                showEditor = false
            }
        )
    }
}

@Composable
fun ColorChannelDialog(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var alpha by remember(initialColor) { mutableIntStateOf(AndroidColor.alpha(initialColor)) }
    var red by remember(initialColor) { mutableIntStateOf(AndroidColor.red(initialColor)) }
    var green by remember(initialColor) { mutableIntStateOf(AndroidColor.green(initialColor)) }
    var blue by remember(initialColor) { mutableIntStateOf(AndroidColor.blue(initialColor)) }
    val currentColorInt = remember(alpha, red, green, blue) {
        AndroidColor.argb(alpha, red, green, blue)
    }

    MagiskDialog(
        title = title,
        onDismissRequest = onDismiss,
        icon = Icons.Rounded.Brush,
        textContent = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(currentColorInt),
                        modifier = Modifier.size(42.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {}
                    Text(
                        text = currentColorInt.toColorHex(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                ColorChannelSlider(
                    label = stringResource(CoreR.string.color_channel_alpha),
                    value = alpha,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onValueChange = { alpha = it }
                )
                ColorChannelSlider(
                    label = stringResource(CoreR.string.color_channel_red),
                    value = red,
                    activeColor = Color(0xFFEF5350),
                    onValueChange = { red = it }
                )
                ColorChannelSlider(
                    label = stringResource(CoreR.string.color_channel_green),
                    value = green,
                    activeColor = Color(0xFF66BB6A),
                    onValueChange = { green = it }
                )
                ColorChannelSlider(
                    label = stringResource(CoreR.string.color_channel_blue),
                    value = blue,
                    activeColor = Color(0xFF42A5F5),
                    onValueChange = { blue = it }
                )
            }
        },
        confirmAction = MagiskDialogAction(
            text = stringResource(CoreR.string.apply),
            onClick = { onConfirm(currentColorInt) }
        ),
        dismissAction = MagiskDialogAction(
            text = stringResource(android.R.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
fun ColorChannelSlider(
    label: String,
    value: Int,
    activeColor: Color,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            steps = 254,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = activeColor.copy(alpha = 0.18f)
            )
        )
    }
}
