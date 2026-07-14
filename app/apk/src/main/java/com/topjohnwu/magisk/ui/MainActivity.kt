package com.topjohnwu.magisk.ui

import android.Manifest.permission.REQUEST_INSTALL_PACKAGES
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.magisk.arch.POST_NOTIFICATIONS_PERMISSION
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.base.ActivityExtension
import com.topjohnwu.magisk.core.base.SplashController
import com.topjohnwu.magisk.core.base.SplashScreenHost
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.ktx.reflectField
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.update.UpdateManager
import com.topjohnwu.magisk.core.wrap
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import com.topjohnwu.magisk.view.Shortcuts
import com.topjohnwu.magisk.ui.theme.MagiskThemeController
import com.topjohnwu.magisk.ui.theme.shouldUseDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

class MainActivity : UIActivity<Unit>(), SplashScreenHost {

    override val extension = ActivityExtension(this)
    override val splashController = SplashController(this)

    internal val showInvalidState = MutableStateFlow(false)
    internal val showUnsupported = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    internal val showShortcutPrompt = MutableStateFlow(false)

    override fun attachBaseContext(base: Context) {
        val nightMode = if (Config.darkTheme == Config.Value.DARK_THEME_AMOLED) {
            Configuration.UI_MODE_NIGHT_YES
        } else {
            when (Config.darkTheme) {
                -1 -> base.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                0 -> Configuration.UI_MODE_NIGHT_NO
                else -> Configuration.UI_MODE_NIGHT_YES
            }
        }
        val config = Configuration(base.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        super.attachBaseContext(base.createConfigurationContext(config).wrap())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        extension.onCreate(savedInstanceState)
        splashController.preOnCreate()
        super.onCreate(savedInstanceState)
        splashController.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        splashController.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        extension.onSaveInstanceState(outState)
    }

    @SuppressLint("InlinedApi")
    override fun onCreateUi(savedInstanceState: Bundle?) {
        showUnsupportedMessage()
        askForHomeShortcut()

        if (Config.checkUpdate) {
            // A denied notification permission must not disable update checks.
            // NotificationCenter will publish after permission is granted and
            // UpdateManager deliberately does not persist a missed fingerprint.
            extension.withPermission(POST_NOTIFICATIONS_PERMISSION) { granted ->
                if (granted) UpdateManager.publishCachedNotifications()
            }
        }

        enableEdgeToEdge()
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val openRoute = if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            Const.Nav.SETTINGS
        } else {
            intent.getStringExtra(Const.Key.OPEN_SECTION)
        }

        setContent {
            val themeState by MagiskThemeController.state.collectAsStateWithLifecycle()
            SystemBarAppearance(darkTheme = shouldUseDarkTheme(themeState.darkThemeMode))
            MagiskAppContainer(openSection = openRoute) {
                MainActivityDialogs(activity = this@MainActivity)
            }
        }
    }

    @SuppressLint("InlinedApi")
    override fun showInvalidStateMessage() {
        showInvalidState.value = true
    }

    internal fun handleInvalidStateInstall() {
        if (!MagiskRuntimeEngine.snapshot().canMigrateApp) {
            toast(CoreR.string.root_required_operation, Toast.LENGTH_LONG)
            showInvalidState.value = true
            return
        }
        extension.withPermission(REQUEST_INSTALL_PACKAGES) {
            if (!it) {
                toast(CoreR.string.install_unknown_denied, Toast.LENGTH_SHORT)
                showInvalidState.value = true
            } else {
                lifecycleScope.launch {
                    if (!AppMigration.restoreApp(this@MainActivity)) {
                        toast(CoreR.string.failure, Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    private fun showUnsupportedMessage() {
        val messages = mutableListOf<Pair<Int, Int>>()
        val runtime = MagiskRuntimeEngine.snapshot()

        if (runtime.isUnsupported) {
            messages.add(CoreR.string.unsupport_magisk_title to CoreR.string.unsupport_magisk_msg)
        }
        if (!runtime.isEmulator && runtime.isInstalled && System.getenv("PATH")?.split(':')
                ?.filterNot { java.io.File("$it/magisk").exists() }
                ?.any { java.io.File("$it/su").exists() } == true
        ) {
            messages.add(CoreR.string.unsupport_general_title to CoreR.string.unsupport_other_su_msg)
        }
        if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            messages.add(CoreR.string.unsupport_general_title to CoreR.string.unsupport_system_app_msg)
        }
        if (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) {
            messages.add(CoreR.string.unsupport_general_title to CoreR.string.unsupport_external_storage_msg)
        }

        if (messages.isNotEmpty()) {
            showUnsupported.value = messages
        }
    }

    private fun askForHomeShortcut() {
        if (isRunningAsStub && !Config.askedHome && androidx.core.content.pm.ShortcutManagerCompat.isRequestPinShortcutSupported(
                this
            )
        ) {
            Config.askedHome = true
            showShortcutPrompt.value = true
        }
    }
}

@Composable
private fun MainActivity.SystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
private fun MainActivityDialogs(activity: MainActivity) {
    val showInvalid by activity.showInvalidState.collectAsStateWithLifecycle()
    val unsupportedMessages by activity.showUnsupported.collectAsStateWithLifecycle()
    val showShortcut by activity.showShortcutPrompt.collectAsStateWithLifecycle()

    if (showInvalid) {
        com.topjohnwu.magisk.ui.component.MagiskDialog(
            title = activity.getString(CoreR.string.unsupport_nonroot_stub_title),
            text = activity.getString(CoreR.string.unsupport_nonroot_stub_msg),
            onDismissRequest = {},
            confirmAction = com.topjohnwu.magisk.ui.component.MagiskDialogAction(
                text = activity.getString(CoreR.string.install), onClick = {
                    activity.showInvalidState.value = false
                    activity.handleInvalidStateInstall()
                }))
    }

    unsupportedMessages.forEachIndexed { index, pair ->
        val show = rememberSaveable(index) { androidx.compose.runtime.mutableStateOf(true) }
        if (show.value) {
            val (titleRes, msgRes) = pair
            com.topjohnwu.magisk.ui.component.MagiskDialog(
                title = activity.getString(titleRes),
                text = activity.getString(msgRes),
                onDismissRequest = { show.value = false },
                confirmAction = com.topjohnwu.magisk.ui.component.MagiskDialogAction(
                    text = activity.getString(android.R.string.ok),
                    onClick = { show.value = false }))
        }
    }

    if (showShortcut) {
        com.topjohnwu.magisk.ui.component.MagiskDialog(
            title = activity.getString(CoreR.string.add_shortcut_title),
            text = activity.getString(CoreR.string.add_shortcut_msg),
            onDismissRequest = { activity.showShortcutPrompt.value = false },
            confirmAction = com.topjohnwu.magisk.ui.component.MagiskDialogAction(
                text = activity.getString(android.R.string.ok), onClick = {
                    activity.showShortcutPrompt.value = false
                    Shortcuts.addHomeIcon(activity)
                }),
            dismissAction = com.topjohnwu.magisk.ui.component.MagiskDialogAction(
                text = activity.getString(android.R.string.cancel),
                onClick = { activity.showShortcutPrompt.value = false }))
    }
}
