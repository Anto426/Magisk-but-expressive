package com.topjohnwu.magisk.core.update

import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.view.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object UpdateManager {

    // 1. App Update State
    data class AppUpdateState(
        val isChecking: Boolean = false,
        val isUpdateAvailable: Boolean = false,
        val latestVersionName: String = "",
        val latestVersionCode: Int = 0,
        val releaseNotes: String = "",
        val updateInfo: UpdateInfo? = null,
        val hasFailed: Boolean = false
    )

    private val _appUpdateState = MutableStateFlow(AppUpdateState())
    val appUpdateState: StateFlow<AppUpdateState> = _appUpdateState.asStateFlow()

    // 2. Module Updates State
    data class ModuleUpdatesState(
        val isChecking: Boolean = false,
        val outdatedCount: Int = 0,
        val updates: Map<String, OnlineModule> = emptyMap()
    )

    private val _moduleUpdatesState = MutableStateFlow(ModuleUpdatesState())
    val moduleUpdatesState: StateFlow<ModuleUpdatesState> = _moduleUpdatesState.asStateFlow()

    private val emptyAppUpdate = UpdateInfo()
    private val appCacheMutex = Mutex()
    private var appUpdateCacheKey = ""
    var cachedAppUpdate: UpdateInfo = emptyAppUpdate
        private set

    private val moduleUpdateCache = ConcurrentHashMap<String, OnlineModule>()

    private val appCheckMutex = Mutex()
    private val moduleCheckMutex = Mutex()

    fun resetAppUpdate() {
        cachedAppUpdate = emptyAppUpdate
        appUpdateCacheKey = ""
        _appUpdateState.update { AppUpdateState() }
    }

    fun resetModuleUpdates() {
        moduleUpdateCache.clear()
        _moduleUpdatesState.update { ModuleUpdatesState() }
    }

    fun resetAllUpdates() {
        resetAppUpdate()
        resetModuleUpdates()
    }

    fun retainModuleUpdates(moduleIds: Set<String>) {
        moduleUpdateCache.keys.retainAll { it in moduleIds }
    }

    fun getCachedModuleUpdate(module: LocalModule): OnlineModule? {
        val cached = moduleUpdateCache[module.id] ?: return null
        return if (cached.versionCode > module.versionCode) {
            cached
        } else {
            moduleUpdateCache.remove(module.id)
            null
        }
    }

    fun cacheModuleUpdate(module: LocalModule, update: OnlineModule?) {
        if (update != null && update.versionCode > module.versionCode) {
            moduleUpdateCache[module.id] = update
        } else {
            moduleUpdateCache.remove(module.id)
        }
    }

    suspend fun checkForAppUpdate(
        svc: NetworkService,
        force: Boolean = false,
        showNotification: Boolean = false
    ): UpdateInfo? = appCheckMutex.withLock {
        if (force) {
            cachedAppUpdate = emptyAppUpdate
            appUpdateCacheKey = ""
        }
        _appUpdateState.update { it.copy(isChecking = true, hasFailed = false) }
        val info = fetchAppUpdate(svc)
        if (info == null) {
            _appUpdateState.update { it.copy(isChecking = false, hasFailed = true) }
            return null
        }
        val isAvailable = AppVersion.isUpdateAvailable(info)
        _appUpdateState.update {
            AppUpdateState(
                isChecking = false,
                isUpdateAvailable = isAvailable,
                latestVersionName = info.version,
                latestVersionCode = info.versionCode,
                releaseNotes = info.note,
                updateInfo = info
            )
        }
        if (isAvailable && showNotification) {
            Notifications.updateAvailable()
        }
        return info
    }

    private suspend fun fetchAppUpdate(svc: NetworkService): UpdateInfo? {
        val key = currentAppUpdateCacheKey()
        val cached = cachedAppUpdate
        if (cached !== emptyAppUpdate && appUpdateCacheKey == key) {
            return cached
        }

        return appCacheMutex.withLock {
            val lockedCached = cachedAppUpdate
            if (lockedCached !== emptyAppUpdate && appUpdateCacheKey == key) {
                lockedCached
            } else {
                svc.fetchUpdate()?.also {
                    cachedAppUpdate = it
                    appUpdateCacheKey = key
                }
            }
        }
    }

    suspend fun checkForModuleUpdates(
        installedModules: List<LocalModule>,
        showNotification: Boolean = false
    ) {
        moduleCheckMutex.withLock {
            if (installedModules.isEmpty()) {
                resetModuleUpdates()
                return@withLock
            }
            _moduleUpdatesState.update { it.copy(isChecking = true) }
            coroutineScope {
                withContext(Dispatchers.IO) {
                    installedModules.map { module ->
                        async { runCatching { module.fetch() } }
                    }.awaitAll()
                }
            }
            val outdatedModules = installedModules.filter { it.outdated }
            val updatesMap = outdatedModules.associate { it.id to it.updateInfo!! }
            val count = outdatedModules.size

            _moduleUpdatesState.update {
                ModuleUpdatesState(
                    isChecking = false,
                    outdatedCount = count,
                    updates = updatesMap
                )
            }

            if (count > 0 && showNotification) {
                Notifications.moduleUpdateAvailable(count)
            }
        }
    }
}

private fun currentAppUpdateCacheKey(): String {
    val channel = Config.updateChannel.coerceIn(
        Config.Value.MBE_CHANNEL,
        Config.Value.CUSTOM_CHANNEL
    )
    val customUrl = Config.customChannelUrl.takeIf {
        channel == Config.Value.CUSTOM_CHANNEL && it.isNotBlank()
    }.orEmpty()
    return "$channel:$customUrl"
}
