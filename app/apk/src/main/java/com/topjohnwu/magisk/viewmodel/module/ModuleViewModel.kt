package com.topjohnwu.magisk.viewmodel.module

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

data class ModuleUiItem(
    val id: String,
    val name: String,
    val versionAuthor: String,
    val description: String,
    val enabled: Boolean,
    val removed: Boolean,
    val updated: Boolean,
    val showAction: Boolean,
    val noticeText: UiText?,
    val showUpdate: Boolean,
    val updateReady: Boolean,
    val update: OnlineModule?,
    val badges: List<String>,
    val searchKey: String,
    val expanded: Boolean = false
)

data class ModuleUiState(
    val loading: Boolean = true,
    val checkingUpdates: Boolean = false,
    val modules: List<ModuleUiItem> = emptyList(),
    val filteredModules: List<ModuleUiItem> = emptyList(),
    val searchQuery: String = "",
    val searchVisible: Boolean = false
)

class ModuleViewModel(
    private val moduleProvider: suspend () -> List<LocalModule>
) : ViewModel() {
    private val _state = MutableStateFlow(ModuleUiState())
    val state: StateFlow<ModuleUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private var refreshJob: Job? = null
    private var metadataJob: Job? = null
    private val moduleCache = linkedMapOf<String, LocalModule>()
    private val cacheLock = Any()
    private var lastRefreshAt = 0L
    private var lastMetadataRefreshAt = 0L

    fun refresh(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && _state.value.modules.isNotEmpty() && now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS) {
            return
        }
        lastRefreshAt = now
        refreshJob?.cancel()
        metadataJob?.cancel()
        val hadModules = _state.value.modules.isNotEmpty()
        refreshJob = viewModelScope.launch {
            if (!hadModules) {
                _state.update { it.copy(loading = true) }
            }
            val runtime = MagiskRuntimeEngine.snapshot()
            val list = if (runtime.isInstalled && isModuleRepoLoaded()) {
                readInstalledModules()
            } else {
                emptyList()
            }
            synchronized(cacheLock) {
                moduleCache.clear()
                list.forEach { moduleCache[it.id] = it }
            }
            val expanded = _state.value.modules.filter { it.expanded }.map { it.id }.toSet()
            _state.update {
                it.withModules(
                    modules = list.map { module -> module.toUiItem(expanded.contains(module.id)) },
                    loading = false,
                    checkingUpdates = false
                )
            }
            if (list.isNotEmpty() && (force || now - lastMetadataRefreshAt >= MIN_METADATA_REFRESH_INTERVAL_MS)) {
                lastMetadataRefreshAt = now
                _state.update { it.copy(checkingUpdates = true) }
                metadataJob = launch(Dispatchers.IO) {
                    list.forEach { runCatching { it.fetch() } }
                    val expandedAfterMetadata =
                        _state.value.modules.filter { it.expanded }.map { it.id }.toSet()
                    val updatedUi = list.map { it.toUiItem(expandedAfterMetadata.contains(it.id)) }
                    withContext(Dispatchers.Main) {
                        _state.update { st ->
                            st.withModules(updatedUi, checkingUpdates = false)
                        }
                    }
                }
            }
        }
    }

    fun toggleExpanded(id: String) {
        _state.update { state ->
            state.withModules(state.modules.map {
                if (it.id == id) it.copy(expanded = !it.expanded) else it
            })
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.withSearchQuery(query) }
    }

    fun clearSearch() {
        setSearchQuery("")
    }

    fun toggleSearch() {
        _state.update {
            if (it.searchVisible) {
                it.withSearchQuery("").copy(searchVisible = false)
            } else {
                it.copy(searchVisible = true)
            }
        }
    }

    fun toggleEnabled(id: String) = updateModule(id) { it.enable = !it.enable }

    fun toggleRemove(id: String) = updateModule(id) { it.remove = !it.remove }

    fun postMessageRes(@StringRes res: Int) {
        _messages.tryEmit(uiText(res))
    }

    private fun updateModule(id: String, block: (LocalModule) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val module = synchronized(cacheLock) { moduleCache[id] } ?: return@launch
            val ok = runCatching { block(module) }.isSuccess
            if (!ok) {
                _messages.emit(uiText(CoreR.string.failure))
                return@launch
            }
            val expanded = _state.value.modules.find { it.id == id }?.expanded ?: false
            val updatedUi = module.toUiItem(expanded)
            withContext(Dispatchers.Main) {
                _state.update { state ->
                    val index = state.modules.indexOfFirst { it.id == id }
                    if (index < 0) {
                        state
                    } else {
                        val copy = state.modules.toMutableList()
                        copy[index] = updatedUi
                        state.withModules(copy)
                    }
                }
            }
        }
    }

    private suspend fun isModuleRepoLoaded(): Boolean =
        withTimeoutOrNull(3000) { withContext(Dispatchers.IO) { LocalModule.loaded() } } ?: false

    private suspend fun readInstalledModules(): List<LocalModule> =
        withTimeoutOrNull(5000) { withContext(Dispatchers.IO) { moduleProvider() } } ?: emptyList()

    companion object {
        private const val MIN_REFRESH_INTERVAL_MS = 1200L
        private const val MIN_METADATA_REFRESH_INTERVAL_MS = 30_000L

        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return ModuleViewModel { LocalModule.installed() } as T
            }
        }
    }
}

private fun ModuleUiState.withModules(
    modules: List<ModuleUiItem>,
    loading: Boolean = this.loading,
    checkingUpdates: Boolean = this.checkingUpdates
): ModuleUiState {
    return copy(
        loading = loading,
        checkingUpdates = checkingUpdates,
        modules = modules,
        filteredModules = modules.filteredBy(searchQuery)
    )
}

private fun ModuleUiState.withSearchQuery(query: String): ModuleUiState {
    return copy(
        searchQuery = query, filteredModules = modules.filteredBy(query)
    )
}

private fun List<ModuleUiItem>.filteredBy(query: String): List<ModuleUiItem> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    return if (normalized.isEmpty()) this else filter { it.searchKey.contains(normalized) }
}

private fun LocalModule.toUiItem(expanded: Boolean = false): ModuleUiItem {
    val zygiskLabel = AppContext.getString(CoreR.string.zygisk)
    val safeName = name.ifBlank { id }
    val safeDescription = description
    val runtime = MagiskRuntimeEngine.snapshot()
    val noticeText: UiText? = when {
        zygiskUnloaded -> uiText(CoreR.string.zygisk_module_unloaded)
        runtime.isZygiskEnabled && isRiru -> uiText(CoreR.string.suspend_text_riru, zygiskLabel)
        !runtime.isZygiskEnabled && isZygisk -> uiText(CoreR.string.suspend_text_zygisk, zygiskLabel)
        else -> null
    }
    return ModuleUiItem(
        id = id,
        name = safeName,
        versionAuthor = AppContext.getString(CoreR.string.module_version_author, version, author),
        description = safeDescription,
        enabled = enable,
        removed = remove,
        updated = updated,
        showAction = hasAction && noticeText == null,
        noticeText = noticeText,
        showUpdate = updateInfo != null,
        updateReady = outdated && !remove && enable,
        update = updateInfo,
        badges = buildList {
            if (outdated) add(AppContext.getString(CoreR.string.module_badge_update))
            if (updated) add(AppContext.getString(CoreR.string.module_badge_updated))
            if (remove) add(AppContext.getString(CoreR.string.module_badge_removing))
            if (!enable) add(AppContext.getString(CoreR.string.module_badge_disabled))
        },
        searchKey = buildString {
            append(safeName.lowercase(Locale.ROOT))
            append('\n')
            append(id.lowercase(Locale.ROOT))
            append('\n')
            append(safeDescription.lowercase(Locale.ROOT))
        },
        expanded = expanded
    )
}
