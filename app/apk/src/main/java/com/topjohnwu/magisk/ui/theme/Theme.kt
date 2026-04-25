package com.topjohnwu.magisk.ui.theme

import android.os.Build
import com.topjohnwu.magisk.core.Config

enum class Theme(private val baseThemeName: String) {
    Ruby("Ruby Hoshino"),
    MemCho("Mem-Cho"),
    Aqua("Aqua"),
    SungJinWoo("Sung Jin-Woo"),
    Default("Default (Dynamic)"),
    Custom("Custom");

    val themeName: String
        get() = when {
            this == Default && !supportsMonet -> "Default (Automatic)"
            else -> baseThemeName
        }

    val isSelected get() = selected == this

    fun windowBackgroundColor(darkTheme: Boolean): Int {
        return when {
            this == Custom && darkTheme -> Config.themeCustomDarkSurface
            this == Custom -> Config.themeCustomLightSurface
            darkTheme && Config.darkTheme == Config.Value.DARK_THEME_AMOLED -> 0xFF000000.toInt()
            this == Ruby && darkTheme -> 0xFF211017.toInt()
            this == Ruby -> 0xFFFFF5F8.toInt()
            this == MemCho && darkTheme -> 0xFF211E10.toInt()
            this == MemCho -> 0xFFFFFBEA.toInt()
            this == Aqua && darkTheme -> 0xFF0D1820.toInt()
            this == Aqua -> 0xFFF0FBFF.toInt()
            this == SungJinWoo && darkTheme -> 0xFF12101F.toInt()
            this == SungJinWoo -> 0xFFF6F1FF.toInt()
            darkTheme -> 0xFF0D0D0D.toInt()
            else -> 0xFFFFF5F8.toInt()
        }
    }

    companion object {
        val supportsMonet: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val selected
            get() = values().getOrNull(Config.themeOrdinal) ?: Default

        val shouldUseDynamicColor: Boolean
            get() = supportsMonet && selected == Default

        val displayOrder: List<Theme>
            get() = buildList {
                add(Default)
                add(Custom)
                addAll(values().filterNot { it == Default || it == Custom })
            }
    }

}
