package com.topjohnwu.magisk.core.update

import android.os.SystemClock
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.view.NotificationCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

/**
 * The single source of truth for update checks. UI code reads this state and
 * never mutates LocalModule with remote metadata, so cached results cannot
 * leak into an unrelated module screen.
 */
object UpdateManager {

    private const val APP_CACHE_TTL_MS = 5 * 60 * 1000L
    private const val MODULE_CACHE_TTL_MS = 5 * 60 * 1000L
    private const val MAX_CONCURRENT_MODULE_CHECKS = 6

    enum class CheckStatus { IDLE, CHECKING, READY, FAILED }

    data class AppUpdateState(
        val status: CheckStatus = CheckStatus.IDLE,
        val updateInfo: UpdateInfo? = null,
        /** Cached release notes for the exact updateInfo currently exposed. */
        val changelog: String = ""
    ) {
        val isChecking: Boolean get() = status == CheckStatus.CHECKING
        val hasFailed: Boolean get() = status == CheckStatus.FAILED
        val isUpdateAvailable: Boolean get() = updateInfo?.let(AppVersion::isUpdateAvailable) == true
    }

    data class ModuleUpdatesState(
        val status: CheckStatus = CheckStatus.IDLE,
        val updates: Map<String, OnlineModule> = emptyMap(),
        /** Changelog cache keyed by module id and version code. */
        val changelogs: Map<String, String> = emptyMap(),
        val failedModuleIds: Set<String> = emptySet(),
        val installableModuleIds: Set<String> = emptySet()
    ) {
        val isChecking: Boolean get() = status == CheckStatus.CHECKING
        val hasFailed: Boolean get() = status == CheckStatus.FAILED
        val outdatedCount: Int get() = updates.size

        fun changelog(moduleId: String, versionCode: Int): String {
            return changelogs[changelogKey(moduleId, versionCode)].orEmpty()
        }

        /** Return the update with its changelog resolved from this cache. */
        fun updateFor(moduleId: String): OnlineModule? {
            return updates[moduleId]?.let { update ->
                update.copy(changelog = changelog(moduleId, update.versionCode))
            }
        }
    }

    data class State(
        val app: AppUpdateState = AppUpdateState(),
        val modules: ModuleUpdatesState = ModuleUpdatesState()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val appCheckMutex = Mutex()
    private val moduleCheckMutex = Mutex()
    private var appCacheKey = ""
    private var lastAppCheckAt = 0L
    private var moduleCacheKey = ""
    private var lastModuleCheckAt = 0L

    suspend fun refreshApp(
        service: NetworkService,
        force: Boolean = false,
        publishNotification: Boolean = false
    ): UpdateInfo? {
        val result = appCheckMutex.withLock {
            val now = SystemClock.elapsedRealtime()
            val cacheKey = currentAppUpdateCacheKey()
            val current = _state.value.app
            if (!force && appCacheKey == cacheKey && now - lastAppCheckAt < APP_CACHE_TTL_MS) {
                return@withLock current.updateInfo
            }

            _state.update { it.copy(app = current.copy(status = CheckStatus.CHECKING)) }
            val fetched = try {
                service.fetchUpdate()
            } catch (e: CancellationException) {
                _state.update { state ->
                    if (state.app.status == CheckStatus.CHECKING) {
                        state.copy(app = current)
                    } else {
                        state
                    }
                }
                throw e
            } catch (_: Exception) {
                null
            }
            lastAppCheckAt = now
            appCacheKey = cacheKey

            if (fetched == null) {
                _state.update { it.copy(app = current.copy(status = CheckStatus.FAILED)) }
                return@withLock null
            }

            val info = fetched.takeIf(UpdatePolicy::isValidAppMetadata)
            if (info == null) {
                _state.update { it.copy(app = current.copy(status = CheckStatus.FAILED)) }
                return@withLock null
            }
            val changelog = info.note.ifBlank {
                current.updateInfo
                    ?.takeIf { UpdatePolicy.appFingerprint(it) == UpdatePolicy.appFingerprint(info) }
                    ?.let { current.changelog }
                    .orEmpty()
            }
            val metadata = info.copy(note = "")
            _state.update {
                it.copy(
                    app = AppUpdateState(
                        status = CheckStatus.READY,
                        updateInfo = metadata,
                        changelog = changelog
                    )
                )
            }
            metadata
        }
        if (publishNotification && result != null) {
            publishCachedNotifications()
        }
        return result
    }

    suspend fun refreshModules(
        installed: List<LocalModule>,
        force: Boolean = false,
        publishNotification: Boolean = false
    ): Boolean {
        val successful = moduleCheckMutex.withLock {
            val modulesById = installed.associateBy { it.id }
            if (modulesById.isEmpty()) {
                moduleCacheKey = ""
                lastModuleCheckAt = 0L
                _state.update {
                    it.copy(modules = ModuleUpdatesState(status = CheckStatus.READY))
                }
                return@withLock true
            }

            val now = SystemClock.elapsedRealtime()
            val cacheKey = installed.moduleCacheKey()
            if (!force && moduleCacheKey == cacheKey && now - lastModuleCheckAt < MODULE_CACHE_TTL_MS) {
                reconcileInstalledModules(installed)
                return@withLock _state.value.modules.status != CheckStatus.FAILED
            }

            val previous = _state.value.modules
            _state.update { current ->
                current.copy(modules = current.modules.copy(status = CheckStatus.CHECKING, failedModuleIds = emptySet()))
            }

            val results = try {
                coroutineScope {
                    val semaphore = Semaphore(MAX_CONCURRENT_MODULE_CHECKS)
                    installed.map { module ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                if (!module.hasUpdateSource) {
                                    ModuleResult(module.id, null, "", false)
                                } else {
                                    try {
                                        val update = UpdatePolicy.resolveModuleUpdate(
                                            module,
                                            module.fetchUpdateMetadata()
                                        )
                                        val changelog: String? = when (val source = update?.changelog.orEmpty()) {
                                            "" -> ""
                                            else -> if (UpdatePolicy.isHttpsUrl(source)) {
                                                try {
                                                    module.fetchUpdateChangelog(source)
                                                } catch (e: CancellationException) {
                                                    throw e
                                                } catch (e: Exception) {
                                                    Timber.w(e, "Unable to fetch changelog for module ${module.id}")
                                                    null
                                                }
                                            } else {
                                                ""
                                            }
                                        }
                                        ModuleResult(
                                            id = module.id,
                                            update = update?.copy(changelog = ""),
                                            changelog = changelog,
                                            failed = false
                                        )
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Timber.w(e, "Unable to fetch update metadata for module ${module.id}")
                                        ModuleResult(module.id, null, "", true)
                                    }
                                }
                            }
                        }
                    }.awaitAll()
                }
            } catch (e: CancellationException) {
                _state.update { current ->
                    if (current.modules.status == CheckStatus.CHECKING) {
                        current.copy(
                            modules = current.modules.copy(
                                status = previous.status,
                                failedModuleIds = previous.failedModuleIds
                            )
                        )
                    } else {
                        current
                    }
                }
                throw e
            }

            val freshUpdates = results.mapNotNull { result ->
                result.update?.let { result.id to it }
            }.toMap()
            val failures = results.filter { it.failed }.mapTo(mutableSetOf()) { it.id }
            val retainedUpdates = previous.updates.filter { (id, update) ->
                id in failures && modulesById[id]?.versionCode?.let { update.versionCode > it } == true
            }
            val updates = retainedUpdates + freshUpdates
            val freshChangelogs = results.mapNotNull { result ->
                result.update?.let { update ->
                    val cached = previous.updates[result.id]
                        ?.takeIf {
                            it.versionCode == update.versionCode && it.zipUrl == update.zipUrl
                        }
                        ?.let { previous.changelog(result.id, it.versionCode) }
                        .orEmpty()
                    changelogKey(result.id, update.versionCode) to (result.changelog ?: cached)
                }
            }.toMap()
            val retainedChangelogs = retainedUpdates.map { (id, update) ->
                changelogKey(id, update.versionCode) to previous.changelog(id, update.versionCode)
            }.toMap()
            val changelogs = retainedChangelogs + freshChangelogs
            val installableIds = updates.keys.filterTo(mutableSetOf()) { id ->
                modulesById[id]?.isInstallableUpdate() == true
            }
            moduleCacheKey = cacheKey
            lastModuleCheckAt = now

            _state.update {
                it.copy(
                    modules = ModuleUpdatesState(
                        status = if (failures.isEmpty()) CheckStatus.READY else CheckStatus.FAILED,
                        updates = updates,
                        changelogs = changelogs,
                        failedModuleIds = failures,
                        installableModuleIds = installableIds
                    )
                )
            }
            failures.isEmpty()
        }
        if (publishNotification) {
            publishCachedNotifications()
        }
        return successful
    }

    suspend fun refreshAll(
        service: NetworkService,
        installedModules: List<LocalModule>,
        force: Boolean = false,
        publishNotifications: Boolean = false
    ) = coroutineScope {
        val app = async { refreshApp(service, force) }
        val modules = async { refreshModules(installedModules, force) }
        val appSuccessful = app.await() != null
        val modulesSuccessful = modules.await()
        if (publishNotifications) publishCachedNotifications()
        appSuccessful && modulesSuccessful
    }

    fun invalidateApp() {
        appCacheKey = ""
        lastAppCheckAt = 0L
        _state.update { it.copy(app = AppUpdateState()) }
    }

    fun expireAppCache() {
        appCacheKey = ""
        lastAppCheckAt = 0L
    }

    fun clearModules() {
        moduleCacheKey = ""
        lastModuleCheckAt = 0L
        _state.update { it.copy(modules = ModuleUpdatesState()) }
    }

    fun clearAll() {
        invalidateApp()
        clearModules()
    }

    fun reconcileInstalledModules(installed: List<LocalModule>) {
        val installedById = installed.associateBy { it.id }
        _state.update { current ->
            val retainedUpdates = current.modules.updates.filter { (id, update) ->
                installedById[id]?.versionCode?.let { update.versionCode > it } == true
            }
            val retainedChangelogs = retainedUpdates.map { (id, update) ->
                changelogKey(id, update.versionCode) to current.modules.changelog(id, update.versionCode)
            }.toMap()
            val installableIds = retainedUpdates.keys.filterTo(mutableSetOf()) { id ->
                installedById[id]?.isInstallableUpdate() == true
            }
            current.copy(
                modules = current.modules.copy(
                    updates = retainedUpdates,
                    changelogs = retainedChangelogs,
                    failedModuleIds = current.modules.failedModuleIds.intersect(installedById.keys),
                    installableModuleIds = installableIds
                )
            )
        }
    }

    /**
     * Publishes the currently cached results without performing network I/O.
     * A notification fingerprint is persisted only after Android accepted the
     * notification, so a permission grant can safely retry a previously missed
     * update without creating duplicates.
     */
    @Synchronized
    fun publishCachedNotifications() {
        val snapshot = _state.value
        if (snapshot.app.status == CheckStatus.READY) {
            val appUpdate = snapshot.app.updateInfo?.takeIf { snapshot.app.isUpdateAvailable }
            appUpdate?.let { info ->
                val fingerprint = UpdatePolicy.appFingerprint(info)
                if (Config.lastAppUpdateNotification != fingerprint) {
                    if (NotificationCenter.showAppUpdate(info)) {
                        Config.lastAppUpdateNotification = fingerprint
                    }
                }
            } ?: run {
                NotificationCenter.cancelAppUpdate()
                Config.lastAppUpdateNotification = ""
            }
        }
        val installableUpdates = snapshot.modules.updates.filterKeys {
            it in snapshot.modules.installableModuleIds
        }
        if (installableUpdates.isNotEmpty()) {
            val fingerprint = UpdatePolicy.modulesFingerprint(installableUpdates)
            if (Config.lastModuleUpdateNotification != fingerprint) {
                if (NotificationCenter.showModuleUpdates(installableUpdates.size)) {
                    Config.lastModuleUpdateNotification = fingerprint
                }
            }
        } else if (snapshot.modules.status == CheckStatus.READY) {
            NotificationCenter.cancelModuleUpdates()
            Config.lastModuleUpdateNotification = ""
        }
    }

    private data class ModuleResult(
        val id: String,
        val update: OnlineModule?,
        /** null means a transient changelog fetch failure; preserve a matching cached value. */
        val changelog: String?,
        val failed: Boolean
    )

    private fun changelogKey(moduleId: String, versionCode: Int) = "$moduleId:$versionCode"

}

private fun List<LocalModule>.moduleCacheKey(): String {
    return sortedBy { it.id }.joinToString("\u0000") { "${it.id}:${it.versionCode}" }
}

private fun LocalModule.isInstallableUpdate(): Boolean {
    return runCatching { enable && !remove && !updated }.getOrDefault(false)
}

private fun currentAppUpdateCacheKey(): String {
    val channel = Config.updateChannel.coerceIn(Config.Value.MBE_CHANNEL, Config.Value.CUSTOM_CHANNEL)
    val customUrl = Config.customChannelUrl.takeIf {
        channel == Config.Value.CUSTOM_CHANNEL && it.isNotBlank()
    }.orEmpty()
    return "$channel:$customUrl"
}
