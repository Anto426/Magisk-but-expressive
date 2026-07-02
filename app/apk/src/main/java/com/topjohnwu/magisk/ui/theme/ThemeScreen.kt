package com.topjohnwu.magisk.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.ui.component.BottomBarStyleItem
import com.topjohnwu.magisk.ui.component.CustomThemeBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskSection
import com.topjohnwu.magisk.ui.component.MagiskSettingsGroup
import com.topjohnwu.magisk.ui.component.ThemeCardGrid
import com.topjohnwu.magisk.ui.component.ThemeModeItem
import com.topjohnwu.magisk.ui.component.bottomBarStyleOptions
import com.topjohnwu.magisk.ui.component.themeModeOptions
import com.topjohnwu.magisk.viewmodel.settings.SettingsViewModel
import com.topjohnwu.magisk.core.R as CoreR


@Composable
fun ThemeScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    var showCustomEditor by remember { mutableStateOf(false) }
    var customDraftColors by remember { mutableStateOf(ThemeCustomColors.fromConfig()) }

    LaunchedEffect(viewModel) {
        viewModel.refreshState()
    }

    Column(modifier = modifier.fillMaxSize()) {
        MagiskLazyContent(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                MagiskSettingsGroup(
                    title = stringResource(CoreR.string.theme_mode),
                    icon = Icons.Rounded.BrightnessAuto,
                    items = themeModeOptions().map { option ->
                        {
                            ThemeModeItem(
                                option = option,
                                selected = state.darkThemeMode == option.mode,
                                onClick = { viewModel.setDarkMode(option.mode) })
                        }
                    }
                )
            }

            item {
                MagiskSection(
                    title = stringResource(CoreR.string.theme_appearance),
                    icon = Icons.Rounded.Palette
                ) {
                    ThemeCardGrid(
                        selectedIndex = state.selectedThemeIndex,
                        darkMode = state.darkThemeMode,
                        onThemeClick = { index, option ->
                            if (option == ThemeOption.Custom) {
                                customDraftColors = ThemeCustomColors.fromConfig()
                                showCustomEditor = true
                            } else {
                                viewModel.setThemeIndex(index)
                            }
                        })
                }
            }

            item {
                MagiskSettingsGroup(
                    title = stringResource(CoreR.string.bottom_bar_style_title),
                    icon = Icons.Rounded.Tune,
                    items = bottomBarStyleOptions().map { option ->
                        {
                            BottomBarStyleItem(
                                option = option,
                                selected = state.bottomBarStyle == option.style,
                                onClick = { viewModel.setBottomBarStyle(option.style) })
                        }
                    }
                )
            }
        }
    }

    if (showCustomEditor) {
        CustomThemeBottomSheet(colors = customDraftColors, onColorChanged = { slot, colorInt ->
            customDraftColors = customDraftColors.update(slot, colorInt)
        }, onDismiss = {
            customDraftColors = ThemeCustomColors.fromConfig()
            showCustomEditor = false
        }, onApply = {
            viewModel.setCustomTheme(customDraftColors)
            showCustomEditor = false
        })
    }
}
