package com.topjohnwu.magisk.core.ktx

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.core.utils.RootUtils
import com.topjohnwu.magisk.utils.APKInstall
import com.topjohnwu.superuser.internal.UiThreadHandler
import java.io.File

fun Context.getBitmap(id: Int): Bitmap {
    var drawable = getDrawable(id)!!
    if (drawable is BitmapDrawable)
        return drawable.bitmap
    if (SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
        drawable = LayerDrawable(arrayOf(drawable.background, drawable.foreground))
    }
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

val Context.deviceProtectedContext: Context get() =
    if (SDK_INT >= Build.VERSION_CODES.N) {
        createDeviceProtectedStorageContext()
    } else { this }

fun Context.cachedFile(name: String) = File(cacheDir, name)

fun ApplicationInfo.getLabel(pm: PackageManager): String {
    runCatching {
        if (labelRes > 0) {
            val res = pm.getResourcesForApplication(this)
            LocaleSetting.instance.updateResource(res)
            return res.getString(labelRes)
        }
    }

    return loadLabel(pm).toString()
}

fun Context.unwrap(): Context {
    var context = this
    while (context is ContextWrapper)
        context = context.baseContext
    return context
}

fun Activity.hideKeyboard() {
    val view = currentFocus ?: return
    getSystemService<InputMethodManager>()
        ?.hideSoftInputFromWindow(view.windowToken, 0)
    view.clearFocus()
}

val View.activity: Activity get() {
    var context = context
    while(true) {
        if (context !is ContextWrapper)
            error("View is not attached to activity")
        if (context is Activity)
            return context
        context = context.baseContext
    }
}

@SuppressLint("PrivateApi")
fun getProperty(key: String, def: String): String {
    runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java, String::class.java)
        return get.invoke(clazz, key, def) as String
    }
    return def
}

@SuppressLint("InlinedApi")
@Throws(PackageManager.NameNotFoundException::class)
fun PackageManager.getPackageInfo(uid: Int, pid: Int): PackageInfo? {
    val flag = PackageManager.MATCH_UNINSTALLED_PACKAGES
    val pkgs = getPackagesForUid(uid) ?: throw PackageManager.NameNotFoundException()
    if (pkgs.size > 1) {
        if (pid <= 0) {
            return null
        }
        // Try to find package name from PID
        val proc = RootUtils.getAppProcess(pid)
        if (proc == null) {
            if (uid == Process.SHELL_UID) {
                // It is possible that some apps installed are sharing UID with shell.
                // We will not be able to find a package from the active process list,
                // because the client is forked from ADB shell, not any app process.
                return getPackageInfo("com.android.shell", flag)
            }
        } else if (uid == proc.uid) {
            return getPackageInfo(proc.pkgList[0], flag)
        }

        return null
    }
    if (pkgs.size == 1) {
        return getPackageInfo(pkgs[0], flag)
    }
    throw PackageManager.NameNotFoundException()
}

fun Context.registerRuntimeReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
    APKInstall.registerReceiver(this, receiver, filter)
}

fun Context.selfLaunchIntent(): Intent {
    val pm = packageManager
    val intent = pm.getLaunchIntentForPackage(packageName)!!
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return intent
}

@Suppress("DEPRECATION")
fun Context.toast(msg: CharSequence, duration: Int) {
    UiThreadHandler.run {
        Toast(applicationContext).apply {
            this.duration = duration
            view = makeMagiskToastView(msg)
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, magiskToastYOffset())
            show()
        }
    }
}

fun Context.toast(resId: Int, duration: Int) {
    toast(getText(resId), duration)
}

private fun Context.makeMagiskToastView(msg: CharSequence): View {
    val colors = magiskToastColors()

    return LinearLayout(applicationContext).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(12.dp, 10.dp, 18.dp, 10.dp)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 28.dp.toFloat()
            setColor(colors.surface)
            setStroke(1.dp, colors.outline)
        }
        elevation = 8.dp.toFloat()

        val icon = ImageView(applicationContext).apply {
            setImageResource(R.drawable.ic_magisk_outline)
            imageTintList = ColorStateList.valueOf(colors.onPrimaryContainer)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(7.dp, 7.dp, 7.dp, 7.dp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14.dp.toFloat()
                setColor(colors.primaryContainer)
            }
        }
        addView(icon, LinearLayout.LayoutParams(36.dp, 36.dp))

        val text = TextView(applicationContext).apply {
            this.text = msg
            setTextColor(colors.onSurface)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            maxLines = 3
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(text, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 12.dp
        })
    }
}

private fun Context.magiskToastYOffset(): Int {
    val activity = unwrap() as? Activity
    val navigationInset = if (SDK_INT >= Build.VERSION_CODES.M) {
        activity?.window?.decorView?.rootWindowInsets?.systemWindowInsetBottom ?: 0
    } else {
        0
    }
    return 132.dp + navigationInset
}

private fun Context.magiskToastColors(): MagiskToastColors {
    val dark = isMagiskDarkTheme()
    val seed = when (Config.themeOrdinal) {
        THEME_RUBY -> RUBY_TOAST_SEED
        THEME_MEM_CHO -> MEM_CHO_TOAST_SEED
        THEME_AQUA -> AQUA_TOAST_SEED
        THEME_SUNG_JIN_WOO -> SUNG_JIN_WOO_TOAST_SEED
        THEME_CUSTOM -> customToastSeed()
        else -> defaultToastSeed(dark)
    }
    return seed.toToastColors(dark)
}

private fun Context.isMagiskDarkTheme(): Boolean {
    val systemDark =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    return when (Config.darkTheme) {
        MODE_NIGHT_YES,
        Config.Value.DARK_THEME_AMOLED -> true
        MODE_NIGHT_NO -> false
        else -> systemDark
    }
}

private fun Context.defaultToastSeed(dark: Boolean): MagiskToastSeed {
    val fallback = RUBY_TOAST_SEED
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return fallback
    }
    return MagiskToastSeed(
        lightPrimary = androidSystemColor("system_accent1_600", fallback.lightPrimary),
        darkPrimary = androidSystemColor("system_accent1_200", fallback.darkPrimary),
        lightSecondary = androidSystemColor("system_accent2_600", fallback.lightSecondary),
        darkSecondary = androidSystemColor("system_accent2_200", fallback.darkSecondary),
        lightSurface = androidSystemColor("system_neutral1_10", fallback.lightSurface),
        darkSurface = androidSystemColor("system_neutral1_900", fallback.darkSurface),
        lightOnSurface = androidSystemColor("system_neutral1_900", fallback.lightOnSurface),
        darkOnSurface = androidSystemColor("system_neutral1_100", fallback.darkOnSurface)
    ).let {
        if (dark && Config.darkTheme == Config.Value.DARK_THEME_AMOLED) {
            it.copy(darkSurface = Color.BLACK)
        } else {
            it
        }
    }
}

@Suppress("DEPRECATION")
private fun Context.androidSystemColor(name: String, fallback: Int): Int {
    val id = resources.getIdentifier(name, "color", "android")
    if (id == 0) return fallback
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getColor(id) else resources.getColor(id)
}

private fun customToastSeed(): MagiskToastSeed = MagiskToastSeed(
    lightPrimary = Config.themeCustomLightPrimary,
    darkPrimary = Config.themeCustomDarkPrimary,
    lightSecondary = Config.themeCustomLightSecondary,
    darkSecondary = Config.themeCustomDarkSecondary,
    lightSurface = Config.themeCustomLightSurface,
    darkSurface = Config.themeCustomDarkSurface,
    lightOnSurface = Config.themeCustomLightOnSurface,
    darkOnSurface = Config.themeCustomDarkOnSurface
)

private data class MagiskToastSeed(
    val lightPrimary: Int,
    val darkPrimary: Int,
    val lightSecondary: Int,
    val darkSecondary: Int,
    val lightSurface: Int,
    val darkSurface: Int,
    val lightOnSurface: Int,
    val darkOnSurface: Int
) {
    fun toToastColors(dark: Boolean): MagiskToastColors {
        val primary = if (dark) darkPrimary else lightPrimary
        val surface = if (dark) darkSurface else lightSurface
        val onSurface = if (dark) darkOnSurface else lightOnSurface
        val blendTarget = if (dark) Color.BLACK else Color.WHITE
        val variantTarget = if (dark) Color.WHITE else Color.BLACK
        val primaryContainer = blend(primary, blendTarget, if (dark) 0.42f else 0.78f)
        val surfaceContainerHigh = if (dark) {
            if (Config.darkTheme == Config.Value.DARK_THEME_AMOLED) {
                0xFF0E0E0E.toInt()
            } else {
                blend(surface, Color.WHITE, 0.085f)
            }
        } else {
            blend(surface, variantTarget, 0.065f)
        }
        return MagiskToastColors(
            surface = surfaceContainerHigh,
            onSurface = onSurface,
            primaryContainer = primaryContainer,
            onPrimaryContainer = contentColorFor(primaryContainer),
            outline = withAlpha(primary, 0.22f)
        )
    }
}

private data class MagiskToastColors(
    val surface: Int,
    val onSurface: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val outline: Int
)

private fun blend(base: Int, overlay: Int, amount: Float): Int {
    val safeAmount = amount.coerceIn(0f, 1f)
    val inv = 1f - safeAmount
    return Color.argb(
        0xFF,
        (Color.red(base) * inv + Color.red(overlay) * safeAmount).toInt(),
        (Color.green(base) * inv + Color.green(overlay) * safeAmount).toInt(),
        (Color.blue(base) * inv + Color.blue(overlay) * safeAmount).toInt()
    )
}

private fun contentColorFor(color: Int): Int {
    return if (luminance(color) > 0.42f) Color.BLACK else Color.WHITE
}

private fun luminance(color: Int): Float {
    fun channel(value: Int): Float {
        val normalized = value / 255f
        return if (normalized <= 0.03928f) {
            normalized / 12.92f
        } else {
            ((normalized + 0.055f) / 1.055f).toDouble()
                .let { Math.pow(it, 2.4) }
                .toFloat()
        }
    }
    return 0.2126f * channel(Color.red(color)) +
        0.7152f * channel(Color.green(color)) +
        0.0722f * channel(Color.blue(color))
}

private fun withAlpha(color: Int, alpha: Float): Int {
    return (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24)
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

private const val MODE_NIGHT_NO = 1
private const val MODE_NIGHT_YES = 2

private const val THEME_RUBY = 0
private const val THEME_MEM_CHO = 1
private const val THEME_AQUA = 2
private const val THEME_SUNG_JIN_WOO = 3
private const val THEME_CUSTOM = 5

private val RUBY_TOAST_SEED = MagiskToastSeed(
    lightPrimary = 0xFFF06292.toInt(),
    darkPrimary = 0xFFF48FB1.toInt(),
    lightSecondary = 0xFFD81B60.toInt(),
    darkSecondary = 0xFFF06292.toInt(),
    lightSurface = 0xFFFFF5F8.toInt(),
    darkSurface = 0xFF211017.toInt(),
    lightOnSurface = 0xFF3C1020.toInt(),
    darkOnSurface = 0xFFFCE4EC.toInt()
)

private val MEM_CHO_TOAST_SEED = MagiskToastSeed(
    lightPrimary = 0xFFFFD54F.toInt(),
    darkPrimary = 0xFFFFE082.toInt(),
    lightSecondary = 0xFFFBC02D.toInt(),
    darkSecondary = 0xFFFFD54F.toInt(),
    lightSurface = 0xFFFFFBEA.toInt(),
    darkSurface = 0xFF211E10.toInt(),
    lightOnSurface = 0xFF3E2723.toInt(),
    darkOnSurface = 0xFFFFF9C4.toInt()
)

private val AQUA_TOAST_SEED = MagiskToastSeed(
    lightPrimary = 0xFF4FC3F7.toInt(),
    darkPrimary = 0xFF81D4FA.toInt(),
    lightSecondary = 0xFF0288D1.toInt(),
    darkSecondary = 0xFF4FC3F7.toInt(),
    lightSurface = 0xFFF0FBFF.toInt(),
    darkSurface = 0xFF0D1820.toInt(),
    lightOnSurface = 0xFF01579B.toInt(),
    darkOnSurface = 0xFFE1F5FE.toInt()
)

private val SUNG_JIN_WOO_TOAST_SEED = MagiskToastSeed(
    lightPrimary = 0xFF9575CD.toInt(),
    darkPrimary = 0xFFB39DDB.toInt(),
    lightSecondary = 0xFF5E35B1.toInt(),
    darkSecondary = 0xFF9575CD.toInt(),
    lightSurface = 0xFFF6F1FF.toInt(),
    darkSurface = 0xFF12101F.toInt(),
    lightOnSurface = 0xFF311B92.toInt(),
    darkOnSurface = 0xFFEDE7F6.toInt()
)
