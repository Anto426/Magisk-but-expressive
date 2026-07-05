package com.topjohnwu.magisk.core.update

import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Info
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

    private val appCheckMutex = Mutex()
    private val moduleCheckMutex = Mutex()

    fun resetAppUpdate() {
        Info.resetUpdate()
        _appUpdateState.update { AppUpdateState() }
    }

    suspend fun checkForAppUpdate(
        svc: NetworkService,
        force: Boolean = false,
        showNotification: Boolean = false
    ): UpdateInfo? = appCheckMutex.withLock {
        if (force) {
            Info.resetUpdate()
        }
        _appUpdateState.update { it.copy(isChecking = true, hasFailed = false) }
        val info = Info.fetchUpdate(svc)
        if (info == null) {
            _appUpdateState.update { it.copy(isChecking = false, hasFailed = true) }
            return null
        }
        val isAvailable = BuildConfig.MBE_VERSION_CODE < info.versionCode
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
        if (isAvailable && showNotification && Info.env.isActive) {
            Notifications.updateAvailable()
        }
        return info
    }

    suspend fun checkForModuleUpdates(
        installedModules: List<LocalModule>,
        showNotification: Boolean = false
    ) {
        moduleCheckMutex.withLock {
            if (installedModules.isEmpty()) {
                _moduleUpdatesState.update { ModuleUpdatesState() }
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
