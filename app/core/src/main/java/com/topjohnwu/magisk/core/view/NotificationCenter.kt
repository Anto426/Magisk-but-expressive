package com.topjohnwu.magisk.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.app.NotificationManagerCompat
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.ktx.getBitmap
import com.topjohnwu.magisk.core.ktx.selfLaunchIntent
import com.topjohnwu.magisk.core.model.UpdateInfo
import java.util.concurrent.atomic.AtomicInteger

/**
 * Single Android notification boundary for the application layer.
 *
 * Features request semantic operations here. The raw builder is exposed only
 * for the foreground Service/JobService download session contract imposed by
 * Android; channels, IDs, posting and cancellation remain centralized.
 */
@Suppress("DEPRECATION")
object NotificationCenter {

    class ProgressHandle internal constructor(
        internal val id: Int,
        internal val builder: Notification.Builder
    )

    private const val APP_UPDATED_ID = 4
    private const val APP_UPDATE_AVAILABLE_ID = 5
    private const val MODULE_UPDATE_AVAILABLE_ID = 6
    private const val DYNAMIC_ID_START = 1_000

    // A new channel ID is intentional: Android keeps the importance selected for an
    // existing channel, so the old DEFAULT channel could never be promoted to a
    // visible heads-up alert on devices that had already created it.
    private const val UPDATE_CHANNEL = "update_alerts"
    private const val PROGRESS_CHANNEL = "progress"
    private const val UPDATED_CHANNEL = "updated"
    private const val SU_CHANNEL = "su_access_alerts"
    private const val LEGACY_SU_CHANNEL = "su_notification"
    private val LEGACY_ALERT_CHANNELS = arrayOf("update")
    private const val SU_NOTIFICATION_TIMEOUT_MS = 8_000L

    private val manager by lazy { AppContext.getSystemService<NotificationManager>()!! }
    private val managerCompat by lazy { NotificationManagerCompat.from(AppContext) }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var configuredLocaleTag: String? = null
    private var configuredSuChannel: String? = null
    @Volatile
    private var effectiveSuChannel = SU_CHANNEL
    private val dynamicIds = AtomicInteger(
        DYNAMIC_ID_START + (System.currentTimeMillis() % 100_000L).toInt()
    )

    @Synchronized
    fun setup() {
        if (SDK_INT < Build.VERSION_CODES.O) return
        effectiveSuChannel = resolveSuChannel()
        val localeTag = AppContext.resources.configuration.locales[0].toLanguageTag()
        if (
            configuredLocaleTag == localeTag &&
            configuredSuChannel == effectiveSuChannel &&
            manager.getNotificationChannel(effectiveSuChannel) != null
        ) {
            return
        }
        val channels = mutableListOf(
            channel(
                UPDATE_CHANNEL,
                AppContext.getString(R.string.update_channel),
                NotificationManager.IMPORTANCE_HIGH,
                showBadge = true
            ),
            channel(
                PROGRESS_CHANNEL,
                AppContext.getString(R.string.progress_channel),
                NotificationManager.IMPORTANCE_LOW,
                showBadge = false
            ),
            channel(
                UPDATED_CHANNEL,
                AppContext.getString(R.string.updated_channel),
                NotificationManager.IMPORTANCE_HIGH,
                showBadge = true
            )
        )
        if (effectiveSuChannel == SU_CHANNEL) {
            channels += channel(
                SU_CHANNEL,
                AppContext.getString(R.string.su_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
                showBadge = false
            )
        }
        manager.createNotificationChannels(channels)
        // This old default-priority ID is no longer posted to. Removing it keeps
        // Android's notification settings free of a duplicate update category.
        LEGACY_ALERT_CHANNELS.forEach(manager::deleteNotificationChannel)
        configuredLocaleTag = localeTag
        configuredSuChannel = effectiveSuChannel
    }

    fun canPostNotifications(channelId: String? = null): Boolean {
        setup()
        val permissionGranted = SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                AppContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted || !managerCompat.areNotificationsEnabled()) return false
        if (SDK_INT < Build.VERSION_CODES.O || channelId == null) return true
        return manager.getNotificationChannel(channelId)
            ?.let { it.importance != NotificationManager.IMPORTANCE_NONE }
            ?: false
    }

    fun canPostRootNotifications(): Boolean = canPostNotifications(suChannelId())

    fun rootNotificationSettingsIntent(): Intent {
        val channelId = suChannelId()
        return if (SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, AppContext.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        } else {
            applicationDetailsSettingsIntent()
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun appNotificationSettingsIntent(): Intent {
        return if (SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, AppContext.packageName)
        } else {
            applicationDetailsSettingsIntent()
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @SuppressLint("InlinedApi")
    fun showAppUpdated(): Boolean {
        cancelAppUpdate()
        val builder = builder(UPDATED_CHANNEL, NotificationManager.IMPORTANCE_HIGH)
            .setSmallIcon(appSmallIcon())
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(navigationPendingIntent(Const.Nav.HOME, APP_UPDATED_ID))
            .setContentTitle(AppContext.getText(R.string.updated_title))
            .setContentText(AppContext.getText(R.string.updated_text))
            .setAutoCancel(true)
        return post(APP_UPDATED_ID, builder, UPDATED_CHANNEL)
    }

    fun showAppUpdate(info: UpdateInfo): Boolean {
        val bitmap = AppContext.getBitmap(R.drawable.ic_magisk_outline)
        val builder = updateAlertBuilder()
            .setSmallIcon(appSmallIcon())
            .setLargeIcon(bitmap)
            .setContentTitle(AppContext.getString(R.string.magisk_update_title))
            .setContentText(AppContext.getString(R.string.manager_download_install))
            .setSubText(info.version)
            .setAutoCancel(true)
            .setContentIntent(
                navigationPendingIntent(Const.Nav.APP_UPDATE, APP_UPDATE_AVAILABLE_ID)
            )
        return post(APP_UPDATE_AVAILABLE_ID, builder, UPDATE_CHANNEL)
    }

    fun showModuleUpdates(count: Int): Boolean {
        val bitmap = AppContext.getBitmap(R.drawable.ic_magisk_outline)
        val builder = updateAlertBuilder()
            .setSmallIcon(appSmallIcon())
            .setLargeIcon(bitmap)
            .setContentTitle(AppContext.getString(R.string.module_updates_title))
            .setContentText(AppContext.getString(R.string.module_updates_available_count, count))
            .setAutoCancel(true)
            .setContentIntent(
                navigationPendingIntent(Const.Nav.MODULE_UPDATES, MODULE_UPDATE_AVAILABLE_ID)
            )
        return post(MODULE_UPDATE_AVAILABLE_ID, builder, UPDATE_CHANNEL)
    }

    fun cancelAppUpdate() = cancel(APP_UPDATE_AVAILABLE_ID)

    fun cancelModuleUpdates() = cancel(MODULE_UPDATE_AVAILABLE_ID)

    @SuppressLint("InlinedApi")
    fun showRootPermission(granted: Boolean, appName: String): Boolean {
        val channelId = suChannelId()
        val title = AppContext.getString(
            if (granted) R.string.su_notification_granted_title
            else R.string.su_notification_denied_title
        )
        val text = AppContext.getString(
            if (granted) R.string.su_allow_toast else R.string.su_deny_toast,
            appName
        )
        val id = nextDynamicId()
        val builder = builder(channelId, NotificationManager.IMPORTANCE_HIGH)
            .setSmallIcon(appSmallIcon())
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(navigationPendingIntent(Const.Nav.SUPERUSER, id))
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
        if (SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(SU_NOTIFICATION_TIMEOUT_MS)
        } else {
            builder.setDefaults(Notification.DEFAULT_ALL)
            mainHandler.postDelayed({ cancel(id) }, SU_NOTIFICATION_TIMEOUT_MS)
        }
        return post(id, builder, channelId)
    }

    /** Builder used only by the OS-bound download session implementation. */
    fun progressBuilder(title: CharSequence): Notification.Builder {
        val result = builder(PROGRESS_CHANNEL, NotificationManager.IMPORTANCE_LOW)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
        if (SDK_INT >= Build.VERSION_CODES.S) {
            result.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return result.applyAndroid16ProgressStyle(progress = null)
    }

    fun startProgress(title: CharSequence): ProgressHandle? {
        if (!canPostNotifications(PROGRESS_CHANNEL)) return null
        val handle = ProgressHandle(nextDynamicId(), progressBuilder(title))
        post(handle.id, handle.builder, PROGRESS_CHANNEL)
        return handle
    }

    fun updateProgress(handle: ProgressHandle?, progress: Int?, text: CharSequence) {
        handle ?: return
        handle.builder
            .setProgress(if (progress == null) 0 else 100, progress ?: 0, progress == null)
            .setContentText(text)
            .applyAndroid16ProgressStyle(progress)
        post(handle.id, handle.builder, PROGRESS_CHANNEL)
    }

    fun finishProgress(handle: ProgressHandle?, text: CharSequence, success: Boolean) {
        handle ?: return
        handle.builder
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentText(text)
            .setProgress(if (success) 100 else 0, if (success) 100 else 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
        if (success) {
            handle.builder.applyAndroid16ProgressStyle(100)
        } else {
            handle.builder.clearAndroid16ProgressStyle()
        }
        post(handle.id, handle.builder, PROGRESS_CHANNEL)
    }

    fun post(
        id: Int,
        builder: Notification.Builder,
        channelId: String? = null
    ): Boolean {
        setup()
        if (!canPostNotifications(channelId)) return false
        return runCatching {
            manager.notify(id, builder.build())
            true
        }.getOrDefault(false)
    }

    fun cancel(id: Int) {
        manager.cancel(id)
    }

    fun nextDynamicId(): Int {
        val next = dynamicIds.incrementAndGet()
        if (next > Int.MAX_VALUE - 1_000) {
            dynamicIds.set(DYNAMIC_ID_START)
            return dynamicIds.incrementAndGet()
        }
        return next
    }

    fun Notification.Builder.applyAndroid16ProgressStyle(progress: Int?): Notification.Builder {
        if (SDK_INT >= 36) {
            runCatching {
                val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
                val progressStyleInstance = progressStyleClass.getConstructor().newInstance()
                if (progress == null) {
                    progressStyleClass
                        .getMethod("setProgressIndeterminate", Boolean::class.javaPrimitiveType)
                        .invoke(progressStyleInstance, true)
                } else {
                    progressStyleClass
                        .getMethod("setProgress", Int::class.javaPrimitiveType)
                        .invoke(progressStyleInstance, progress.coerceIn(0, 100))
                }
                val setStyleMethod = javaClass.getMethod(
                    "setStyle", Class.forName("android.app.Notification\$Style")
                )
                setStyleMethod.invoke(this, progressStyleInstance)
            }
        }
        return this
    }

    fun Notification.Builder.clearAndroid16ProgressStyle(): Notification.Builder {
        if (SDK_INT >= 36) {
            runCatching {
                val styleClass = Class.forName("android.app.Notification\$Style")
                javaClass.getMethod("setStyle", styleClass).invoke(this, null)
            }
        }
        return this
    }

    private fun builder(channel: String, legacyImportance: Int): Notification.Builder {
        setup()
        return if (SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(AppContext, channel)
        } else {
            Notification.Builder(AppContext).setPriority(
                when (legacyImportance) {
                    NotificationManager.IMPORTANCE_HIGH -> Notification.PRIORITY_HIGH
                    NotificationManager.IMPORTANCE_LOW -> Notification.PRIORITY_LOW
                    else -> Notification.PRIORITY_DEFAULT
                }
            )
        }.setVisibility(Notification.VISIBILITY_PRIVATE)
    }

    private fun updateAlertBuilder(): Notification.Builder {
        return builder(UPDATE_CHANNEL, NotificationManager.IMPORTANCE_HIGH)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .apply {
                if (SDK_INT < Build.VERSION_CODES.O) {
                    setDefaults(Notification.DEFAULT_ALL)
                }
            }
    }

    private fun channel(
        id: String,
        name: CharSequence,
        importance: Int,
        showBadge: Boolean
    ): NotificationChannel {
        return NotificationChannel(id, name, importance).apply {
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setShowBadge(showBadge)
            if (importance >= NotificationManager.IMPORTANCE_HIGH) {
                enableLights(true)
                enableVibration(true)
            }
        }
    }

    /**
     * Migrate untouched legacy HIGH channels to the stronger vibrating channel.
     * A legacy channel below HIGH was changed by the user, so it remains both
     * the effective channel and the settings destination.
     */
    private fun resolveSuChannel(): String {
        val legacy = manager.getNotificationChannel(LEGACY_SU_CHANNEL)
        return if (legacy != null && legacy.importance < NotificationManager.IMPORTANCE_HIGH) {
            manager.deleteNotificationChannel(SU_CHANNEL)
            legacy.name = AppContext.getString(R.string.su_notification_channel)
            manager.createNotificationChannel(legacy)
            LEGACY_SU_CHANNEL
        } else {
            if (legacy != null) {
                manager.deleteNotificationChannel(LEGACY_SU_CHANNEL)
            }
            SU_CHANNEL
        }
    }

    private fun suChannelId(): String {
        setup()
        return effectiveSuChannel
    }

    private fun applicationDetailsSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", AppContext.packageName, null)
        )
    }

    private fun navigationPendingIntent(section: String, requestCode: Int): PendingIntent {
        val intent = AppContext.selfLaunchIntent().putExtra(Const.Key.OPEN_SECTION, section)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(AppContext, requestCode, intent, flags)
    }

    private fun appSmallIcon(): Int = R.drawable.ic_magisk_outline
}
