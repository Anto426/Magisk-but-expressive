package com.topjohnwu.magisk.view

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toIcon
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.ktx.getBitmap
import com.topjohnwu.magisk.core.ktx.selfLaunchIntent
import java.util.concurrent.atomic.AtomicInteger

@Suppress("DEPRECATION")
object Notifications {

    val mgr by lazy { AppContext.getSystemService<NotificationManager>()!! }

    private const val APP_UPDATED_ID = 4
    private const val APP_UPDATE_AVAILABLE_ID = 5
    private const val MODULE_UPDATE_AVAILABLE_ID = 6

    private const val UPDATE_CHANNEL = "update"
    private const val PROGRESS_CHANNEL = "progress"
    private const val UPDATED_CHANNEL = "updated"
    private const val SU_CHANNEL = "su_notification"

    private val nextId = AtomicInteger(APP_UPDATE_AVAILABLE_ID)

    fun setup() {
        AppContext.apply {
            if (SDK_INT >= Build.VERSION_CODES.O) {
                mgr.createNotificationChannels(
                    listOf(
                        notificationChannel(
                            id = UPDATE_CHANNEL,
                            name = getString(R.string.update_channel),
                            importance = NotificationManager.IMPORTANCE_DEFAULT,
                            showBadge = true
                        ),
                        notificationChannel(
                            id = PROGRESS_CHANNEL,
                            name = getString(R.string.progress_channel),
                            importance = NotificationManager.IMPORTANCE_LOW,
                            showBadge = false
                        ),
                        notificationChannel(
                            id = UPDATED_CHANNEL,
                            name = getString(R.string.updated_channel),
                            importance = NotificationManager.IMPORTANCE_HIGH,
                            showBadge = true
                        ),
                        notificationChannel(
                            id = SU_CHANNEL,
                            name = getString(R.string.su_notification_channel),
                            importance = NotificationManager.IMPORTANCE_HIGH,
                            showBadge = false
                        )
                    )
                )
            }
        }
    }

    private fun notificationChannel(
        id: String,
        name: CharSequence,
        importance: Int,
        showBadge: Boolean
    ): NotificationChannel {
        return NotificationChannel(id, name, importance).apply {
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setShowBadge(showBadge)
        }
    }

    @SuppressLint("InlinedApi")
    fun updateDone() {
        AppContext.apply {
            val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pending = PendingIntent.getActivity(this, 0, selfLaunchIntent(), flag)
            val builder = if (SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, UPDATED_CHANNEL)
                    .setSmallIcon(getBitmap(R.drawable.ic_magisk_outline).toIcon())
            } else {
                Notification.Builder(this).setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_magisk_outline)
            }
                .setContentIntent(pending)
                .setContentTitle(getText(R.string.updated_title))
                .setContentText(getText(R.string.updated_text))
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
            mgr.notify(APP_UPDATED_ID, builder.build())
        }
    }

    fun updateAvailable() {
        AppContext.apply {
            val intent = DownloadEngine.getPendingIntent(this, Subject.App())
            val bitmap = getBitmap(R.drawable.ic_magisk_outline)
            val builder = if (SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, UPDATE_CHANNEL)
                    .setSmallIcon(bitmap.toIcon())
            } else {
                Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_magisk_outline)
            }
                .setLargeIcon(bitmap)
                .setContentTitle(getString(R.string.magisk_update_title))
                .setContentText(getString(R.string.manager_download_install))
                .setAutoCancel(true)
                .setContentIntent(intent)
                .setVisibility(Notification.VISIBILITY_PRIVATE)

            mgr.notify(APP_UPDATE_AVAILABLE_ID, builder.build())
        }
    }

    fun moduleUpdateAvailable(count: Int) {
        AppContext.apply {
            val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pending = PendingIntent.getActivity(this, 0, selfLaunchIntent(), flag)
            val bitmap = getBitmap(R.drawable.ic_magisk_outline)
            val builder = if (SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, UPDATE_CHANNEL)
                    .setSmallIcon(bitmap.toIcon())
            } else {
                Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_magisk_outline)
            }
                .setLargeIcon(bitmap)
                .setContentIntent(pending)
                .setContentTitle(getString(R.string.module_updates_title))
                .setContentText(getString(R.string.module_updates_available_count, count))
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)

            mgr.notify(MODULE_UPDATE_AVAILABLE_ID, builder.build())
        }
    }

    fun startProgress(title: CharSequence): Notification.Builder {
        val builder = if (SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(AppContext, PROGRESS_CHANNEL)
        } else {
            Notification.Builder(AppContext).setPriority(Notification.PRIORITY_LOW)
        }
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
        if (SDK_INT >= Build.VERSION_CODES.S)
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        builder.applyAndroid16ProgressStyle(progress = null)
        return builder
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
                val setStyleMethod = javaClass.getMethod("setStyle", styleClass)
                setStyleMethod.invoke(this, null)
            }
        }
        return this
    }

    private const val SU_NOTIFICATION_TIMEOUT_MS = 3_000L

    @SuppressLint("InlinedApi")
    fun suNotification(granted: Boolean, appName: String) {
        AppContext.apply {
            val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pending = PendingIntent.getActivity(this, 0, selfLaunchIntent(), flag)
            val title = getString(
                if (granted) R.string.su_notification_granted_title
                else R.string.su_notification_denied_title
            )
            val text = getString(
                if (granted) R.string.su_allow_toast
                else R.string.su_deny_toast,
                appName
            )
            val builder = if (SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, SU_CHANNEL)
                    .setSmallIcon(getBitmap(R.drawable.ic_magisk_outline).toIcon())
            } else {
                Notification.Builder(this).setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_magisk_outline)
            }
                .setContentIntent(pending)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setTimeoutAfter(SU_NOTIFICATION_TIMEOUT_MS)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
            mgr.notify(nextId(), builder.build())
        }
    }

    fun nextId() = nextId.incrementAndGet()
}
