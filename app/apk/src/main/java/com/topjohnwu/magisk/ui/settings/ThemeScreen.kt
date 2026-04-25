package com.topjohnwu.magisk.ui.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.component.MagiskBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogConfirmButton
import com.topjohnwu.magisk.ui.component.MagiskDialogDismissButton
import com.topjohnwu.magisk.ui.component.MagiskDialogOption
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.ui.theme.Theme
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onThemeChanged: () -> Unit = {}
) {
    var currentTheme by remember { mutableStateOf(Theme.selected) }
    var currentDarkMode by remember { mutableIntStateOf(Config.darkTheme) }
    var customColors by remember { mutableStateOf(CustomThemeColors.fromConfig()) }
    var customDraftColors by remember { mutableStateOf(customColors) }
    var showCustomEditorSheet by remember { mutableStateOf(false) }
    val themes = remember { Theme.displayOrder }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = MagiskUiDefaults.SectionSpacing,
                end = MagiskUiDefaults.SectionSpacing,
                top = MagiskUiDefaults.ScreenTopPadding,
                bottom = MagiskUiDefaults.ScreenBottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(2) }) {
                ThemeExpressiveHeader()
            }

            item(span = { GridItemSpan(2) }) {
                DarkModeExpressiveSection(
                    currentDarkMode = currentDarkMode,
                    onDarkModeSelected = { mode ->
                        if (currentDarkMode == mode) return@DarkModeExpressiveSection
                        val previousMode = currentDarkMode
                        currentDarkMode = mode
                        Config.darkTheme = mode
                        AppCompatDelegate.setDefaultNightMode(
                            if (mode == Config.Value.DARK_THEME_AMOLED) {
                                AppCompatDelegate.MODE_NIGHT_YES
                            } else {
                                mode
                            }
                        )
                        if (previousMode == Config.Value.DARK_THEME_AMOLED ||
                            mode == Config.Value.DARK_THEME_AMOLED
                        ) {
                            onThemeChanged()
                        }
                    }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(id = CoreR.string.theme_custom_themes).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }

            items(themes, key = { it.ordinal }) { theme ->
                ThemeOrganicCard(
                    theme = theme,
                    isSelected = theme == currentTheme,
                    customColors = if (theme == Theme.Custom) customColors else null,
                    onClick = {
                        if (theme == currentTheme) {
                            if (theme == Theme.Custom) {
                                customDraftColors = customColors
                                showCustomEditorSheet = true
                            }
                            return@ThemeOrganicCard
                        }
                        if (theme == Theme.Custom) {
                            customDraftColors = customColors
                            showCustomEditorSheet = true
                            return@ThemeOrganicCard
                        }
                        currentTheme = theme
                        Config.themeOrdinal = if (theme == Theme.Default) -1 else theme.ordinal
                        onThemeChanged()
                    }
                )
            }
        }

        if (showCustomEditorSheet) {
            MagiskBottomSheet(
                onDismissRequest = {
                    customDraftColors = customColors
                    showCustomEditorSheet = false
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    CustomThemeEditor(
                        colors = customDraftColors,
                        onColorChanged = { slot, colorInt ->
                            customDraftColors = customDraftColors.update(slot, colorInt)
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                customDraftColors = customColors
                                showCustomEditorSheet = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(MagiskUiDefaults.ActionHeight),
                            shape = MagiskUiDefaults.PillShape
                        ) {
                            Text(
                                stringResource(id = android.R.string.cancel),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                customDraftColors.persistToConfig()
                                customColors = customDraftColors
                                currentTheme = Theme.Custom
                                Config.themeOrdinal = Theme.Custom.ordinal
                                onThemeChanged()
                                showCustomEditorSheet = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(MagiskUiDefaults.ActionHeight),
                            shape = MagiskUiDefaults.PillShape
                        ) {
                            Text(
                                stringResource(id = CoreR.string.apply),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeExpressiveHeader() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MagiskUiDefaults.HeroShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = CoreR.string.theme_personalize_experience),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkModeExpressiveSection(
    currentDarkMode: Int,
    onDarkModeSelected: (Int) -> Unit
) {
    var showModeMenu by remember { mutableStateOf(false) }
    val options = listOf(
        DarkModeMenuOption(
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            icon = Icons.Rounded.BrightnessAuto,
            label = stringResource(id = CoreR.string.settings_dark_mode_system),
            subtitle = stringResource(id = CoreR.string.theme_dark_mode_subtitle_system)
        ),
        DarkModeMenuOption(
            mode = AppCompatDelegate.MODE_NIGHT_NO,
            icon = Icons.Rounded.LightMode,
            label = stringResource(id = CoreR.string.settings_dark_mode_light),
            subtitle = stringResource(id = CoreR.string.theme_dark_mode_subtitle_light)
        ),
        DarkModeMenuOption(
            mode = AppCompatDelegate.MODE_NIGHT_YES,
            icon = Icons.Rounded.DarkMode,
            label = stringResource(id = CoreR.string.settings_dark_mode_dark),
            subtitle = stringResource(id = CoreR.string.theme_dark_mode_subtitle_dark)
        ),
        DarkModeMenuOption(
            mode = Config.Value.DARK_THEME_AMOLED,
            icon = Icons.Rounded.DarkMode,
            label = stringResource(id = CoreR.string.settings_dark_mode_amoled),
            subtitle = stringResource(id = CoreR.string.theme_dark_mode_subtitle_amoled)
        )
    )
    val selected = options.firstOrNull { it.mode == currentDarkMode } ?: options.first()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = CoreR.string.theme_mode).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Surface(
            onClick = { showModeMenu = true },
            shape = MagiskUiDefaults.ExtraLargeShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(MagiskUiDefaults.IconContainerSize)
                ) {
                    Icon(
                        imageVector = selected.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = selected.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (showModeMenu) {
            MagiskDialog(
                onDismissRequest = { showModeMenu = false },
                title = stringResource(id = CoreR.string.settings_dark_mode_title),
                icon = Icons.Rounded.DarkMode,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            val isSelected = option.mode == currentDarkMode
                            MagiskDialogOption(
                                title = option.label,
                                subtitle = option.subtitle,
                                selected = isSelected,
                                showRadio = true,
                                onClick = {
                                    showModeMenu = false
                                    onDarkModeSelected(option.mode)
                                }
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

private data class DarkModeMenuOption(
    val mode: Int,
    val icon: ImageVector,
    val label: String,
    val subtitle: String
)

@Composable
private fun ThemeOrganicCard(
    theme: Theme,
    isSelected: Boolean,
    customColors: CustomThemeColors? = null,
    onClick: () -> Unit
) {
    val defaultUsesStaticFallback = theme == Theme.Default && !Theme.supportsMonet
    val previewGradient = when (theme) {
        Theme.Ruby -> listOf(Color(0xFFE91E63), Color(0xFFFF80AB))
        Theme.MemCho -> listOf(Color(0xFFFFC107), Color(0xFFFFD54F))
        Theme.Aqua -> listOf(Color(0xFF03A9F4), Color(0xFF81D4FA))
        Theme.SungJinWoo -> listOf(Color(0xFF5E35B1), Color(0xFFB39DDB))
        Theme.Default -> if (defaultUsesStaticFallback) {
            listOf(Color(0xFFE91E63), Color(0xFFFF4081))
        } else {
            listOf(
                Color(0xFFE85CF0),
                Color(0xFF8A7BFF),
                Color(0xFF58B8FF),
                Color(0xFF57E2E7),
                Color(0xFF62EB9A),
                Color(0xFFB6EF64)
            )
        }
        Theme.Custom -> listOf(
            Color(customColors?.lightPrimary ?: Config.themeCustomLightPrimary),
            Color(customColors?.darkPrimary ?: Config.themeCustomDarkPrimary)
        )
    }
    val accentColor = previewGradient.first()
    val watermarkAlpha = when (theme) {
        Theme.Default,
        Theme.Custom -> 0.34f
        else -> 0.92f
    }
    val cardScale by MagiskMotion.animateSelectionScale(
        selected = isSelected,
        selectedScale = 1.035f,
        label = "themeCardScale"
    )
    val selectionBorderWidth by MagiskMotion.animateSelectionBorderWidth(
        selected = isSelected,
        selectedWidth = 2.dp,
        label = "themeCardBorder"
    )
    val containerColor by MagiskMotion.animateColor(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = MagiskMotion.quickColorSpec(),
        label = "themeCardContainer"
    )

    val shape = remember(theme) {
        val index = theme.ordinal
        when (index % 3) {
            0 -> RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 12.dp, bottomStart = 12.dp)
            1 -> RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 32.dp, bottomStart = 32.dp)
            else -> RoundedCornerShape(24.dp)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(cardScale)
            .then(
                if (selectionBorderWidth > 0.dp) {
                    Modifier.border(
                        width = selectionBorderWidth,
                        color = accentColor.copy(alpha = 0.78f),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) {
                MagiskUiDefaults.ExpandedCardElevation
            } else {
                MagiskUiDefaults.CardElevation
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Brush.verticalGradient(previewGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimeThemeWatermark(
                        theme = theme,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .alpha(watermarkAlpha)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = MagiskMotion.selectionIndicatorEnter(),
                            exit = MagiskMotion.selectionIndicatorExit()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (defaultUsesStaticFallback) "Default (Magisk)" else theme.themeName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (defaultUsesStaticFallback) {
                        Text(
                            text = "STATIC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomThemeEditor(
    colors: CustomThemeColors,
    onColorChanged: (CustomColorSlot, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = CoreR.string.theme_custom_palette).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = CoreR.string.theme_custom_palette_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        CustomColorSlot.entries.forEach { slot ->
            key(slot) {
                CustomColorRow(
                    label = stringResource(id = slot.labelRes),
                    colorInt = colors.value(slot),
                    onColorChange = { onColorChanged(slot, it) }
                )
            }
        }
    }
}

@Composable
private fun CustomColorRow(
    label: String,
    colorInt: Int,
    onColorChange: (Int) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    Surface(
        onClick = { showEditor = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(colorInt),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.size(32.dp)
            ) {}
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    colorInt.toColorHex(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                Icons.Rounded.Brush,
                null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    if (showEditor) {
        ColorHexDialog(
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
private fun ColorHexDialog(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var alpha by remember(initialColor) { mutableIntStateOf(android.graphics.Color.alpha(initialColor)) }
    var red by remember(initialColor) { mutableIntStateOf(android.graphics.Color.red(initialColor)) }
    var green by remember(initialColor) { mutableIntStateOf(android.graphics.Color.green(initialColor)) }
    var blue by remember(initialColor) { mutableIntStateOf(android.graphics.Color.blue(initialColor)) }
    val currentColorInt = remember(alpha, red, green, blue) {
        android.graphics.Color.argb(alpha, red, green, blue)
    }

    MagiskDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = Icons.Rounded.Brush,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(currentColorInt),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.size(40.dp)
                    ) {}
                    Text(
                        text = currentColorInt.toColorHex(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                ColorChannelSlider(
                    label = stringResource(id = CoreR.string.color_channel_alpha),
                    value = alpha,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onValueChange = { alpha = it }
                )
                ColorChannelSlider(
                    label = stringResource(id = CoreR.string.color_channel_red),
                    value = red,
                    activeColor = Color(0xFFEF5350),
                    onValueChange = { red = it })
                ColorChannelSlider(
                    label = stringResource(id = CoreR.string.color_channel_green),
                    value = green,
                    activeColor = Color(0xFF66BB6A),
                    onValueChange = { green = it })
                ColorChannelSlider(
                    label = stringResource(id = CoreR.string.color_channel_blue),
                    value = blue,
                    activeColor = Color(0xFF42A5F5),
                    onValueChange = { blue = it })
            }
        },
        confirmButton = {
            MagiskDialogConfirmButton(
                onClick = { onConfirm(currentColorInt) },
                text = stringResource(id = CoreR.string.apply)
            )
        },
        dismissButton = {
            MagiskDialogDismissButton(onClick = onDismiss)
        }
    )
}

@Composable
private fun ColorChannelSlider(
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
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
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
                inactiveTrackColor = activeColor.copy(alpha = 0.2f)
            )
        )
    }
}

private enum class CustomColorSlot(@param:StringRes val labelRes: Int) {
    LightPrimary(CoreR.string.theme_color_light_primary),
    DarkPrimary(CoreR.string.theme_color_dark_primary),
    LightSecondary(CoreR.string.theme_color_light_secondary),
    DarkSecondary(CoreR.string.theme_color_dark_secondary),
    LightSurface(CoreR.string.theme_color_light_surface),
    DarkSurface(CoreR.string.theme_color_dark_surface),
    LightOnSurface(CoreR.string.theme_color_light_on_surface),
    DarkOnSurface(CoreR.string.theme_color_dark_on_surface),
    LightError(CoreR.string.theme_color_light_error),
    DarkError(CoreR.string.theme_color_dark_error)
}

private data class CustomThemeColors(
    val lightPrimary: Int,
    val darkPrimary: Int,
    val lightSecondary: Int,
    val darkSecondary: Int,
    val lightSurface: Int,
    val darkSurface: Int,
    val lightOnSurface: Int,
    val darkOnSurface: Int,
    val lightError: Int,
    val darkError: Int
) {
    fun value(slot: CustomColorSlot): Int = when (slot) {
        CustomColorSlot.LightPrimary -> lightPrimary
        CustomColorSlot.DarkPrimary -> darkPrimary
        CustomColorSlot.LightSecondary -> lightSecondary
        CustomColorSlot.DarkSecondary -> darkSecondary
        CustomColorSlot.LightSurface -> lightSurface
        CustomColorSlot.DarkSurface -> darkSurface
        CustomColorSlot.LightOnSurface -> lightOnSurface
        CustomColorSlot.DarkOnSurface -> darkOnSurface
        CustomColorSlot.LightError -> lightError
        CustomColorSlot.DarkError -> darkError
    }

    fun update(slot: CustomColorSlot, colorInt: Int): CustomThemeColors = when (slot) {
        CustomColorSlot.LightPrimary -> copy(lightPrimary = colorInt)
        CustomColorSlot.DarkPrimary -> copy(darkPrimary = colorInt)
        CustomColorSlot.LightSecondary -> copy(lightSecondary = colorInt)
        CustomColorSlot.DarkSecondary -> copy(darkSecondary = colorInt)
        CustomColorSlot.LightSurface -> copy(lightSurface = colorInt)
        CustomColorSlot.DarkSurface -> copy(darkSurface = colorInt)
        CustomColorSlot.LightOnSurface -> copy(lightOnSurface = colorInt)
        CustomColorSlot.DarkOnSurface -> copy(darkOnSurface = colorInt)
        CustomColorSlot.LightError -> copy(lightError = colorInt)
        CustomColorSlot.DarkError -> copy(darkError = colorInt)
    }

    fun persistToConfig() {
        Config.themeCustomLightPrimary = lightPrimary
        Config.themeCustomDarkPrimary = darkPrimary
        Config.themeCustomLightSecondary = lightSecondary
        Config.themeCustomDarkSecondary = darkSecondary
        Config.themeCustomLightSurface = lightSurface
        Config.themeCustomDarkSurface = darkSurface
        Config.themeCustomLightOnSurface = lightOnSurface
        Config.themeCustomDarkOnSurface = darkOnSurface
        Config.themeCustomLightError = lightError
        Config.themeCustomDarkError = darkError
    }

    companion object {
        fun fromConfig(): CustomThemeColors = CustomThemeColors(
            lightPrimary = Config.themeCustomLightPrimary,
            darkPrimary = Config.themeCustomDarkPrimary,
            lightSecondary = Config.themeCustomLightSecondary,
            darkSecondary = Config.themeCustomDarkSecondary,
            lightSurface = Config.themeCustomLightSurface,
            darkSurface = Config.themeCustomDarkSurface,
            lightOnSurface = Config.themeCustomLightOnSurface,
            darkOnSurface = Config.themeCustomDarkOnSurface,
            lightError = Config.themeCustomLightError,
            darkError = Config.themeCustomDarkError
        )
    }
}

private fun Int.toColorHex(): String = String.format("#%08X", this)

@Composable
private fun AnimeThemeWatermark(theme: Theme, modifier: Modifier = Modifier) {
    val characterRes = when (theme) {
        Theme.Ruby -> CoreR.drawable.theme_ruby
        Theme.MemCho -> CoreR.drawable.theme_memcho
        Theme.Aqua -> CoreR.drawable.theme_aqua
        Theme.SungJinWoo -> CoreR.drawable.theme_sung_jinwoo
        Theme.Default,
        Theme.Custom -> null
    }

    if (characterRes != null) {
        Image(
            painter = painterResource(id = characterRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.padding(4.dp)
        )
        return
    }

    val icon = when (theme) {
        Theme.Default -> Icons.Rounded.AutoAwesome
        Theme.Custom -> Icons.Rounded.Brush
        else -> Icons.Rounded.AutoAwesome
    }
    val accent = Color.White.copy(alpha = 0.76f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            tint = accent
        )
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(42.dp),
            tint = Color.White.copy(alpha = 0.58f)
        )
    }
}
