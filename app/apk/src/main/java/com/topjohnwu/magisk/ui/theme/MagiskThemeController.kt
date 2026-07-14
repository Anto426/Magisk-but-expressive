package com.topjohnwu.magisk.ui.theme

import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.ui.component.MagiskLoadingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MagiskThemeState(
    val themeOption: ThemeOption = ThemeOption.selected,
    val darkThemeMode: Int = Config.darkTheme,
    val bottomBarStyle: Int = Config.bottomBarStyle,
    val bottomBarOpacity: Int = Config.bottomBarOpacity,
    val customColorVersion: Int = customColorVersion()
)

object MagiskThemeController {
    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<MagiskThemeState> = _state.asStateFlow()

    fun refresh() {
        _state.value = snapshot()
    }

    fun setDarkMode(mode: Int) {
        if (Config.darkTheme == mode) return
        applyThemeChange { Config.darkTheme = mode }
    }

    fun setTheme(option: ThemeOption) {
        if (ThemeOption.selected == option) return
        applyThemeChange { Config.themeOrdinal = option.configOrdinal }
    }

    fun setThemeIndex(index: Int) {
        setTheme(ThemeOption.displayOrder.getOrNull(index) ?: ThemeOption.Default)
    }

    fun setCustomColors(colors: ThemeCustomColors) {
        if (ThemeOption.selected == ThemeOption.Custom && ThemeCustomColors.fromConfig() == colors) {
            return
        }
        applyThemeChange {
            colors.persistToConfig()
            Config.themeOrdinal = ThemeOption.Custom.configOrdinal
        }
    }

    fun setBottomBarStyle(style: Int) {
        Config.bottomBarStyle = style.coerceIn(
            Config.Value.BOTTOM_BAR_AUTO,
            Config.Value.BOTTOM_BAR_FIXED
        )
        refresh()
    }

    fun setBottomBarOpacity(opacity: Int) {
        Config.bottomBarOpacity = opacity.coerceIn(0, 100)
        refresh()
    }

    private inline fun applyThemeChange(change: () -> Unit) {
        val generation = MagiskLoadingController.begin()
        try {
            change()
            refresh()
        } catch (error: Throwable) {
            MagiskLoadingController.complete(generation)
            throw error
        }
    }

    private fun snapshot() = MagiskThemeState()
}

val ThemeOption.configOrdinal: Int
    get() = if (this == ThemeOption.Default) -1 else ordinal

private fun customColorVersion(): Int {
    var result = Config.themeCustomLightPrimary
    result = 31 * result + Config.themeCustomDarkPrimary
    result = 31 * result + Config.themeCustomLightSecondary
    result = 31 * result + Config.themeCustomDarkSecondary
    result = 31 * result + Config.themeCustomLightSurface
    result = 31 * result + Config.themeCustomDarkSurface
    result = 31 * result + Config.themeCustomLightOnSurface
    result = 31 * result + Config.themeCustomDarkOnSurface
    result = 31 * result + Config.themeCustomLightError
    result = 31 * result + Config.themeCustomDarkError
    return result
}
