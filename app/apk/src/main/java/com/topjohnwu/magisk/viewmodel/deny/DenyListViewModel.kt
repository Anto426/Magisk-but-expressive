package com.topjohnwu.magisk.viewmodel.deny

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.MATCH_UNINSTALLED_PACKAGES_COMPAT
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

enum class DenyListSortMethod(@param:StringRes val labelRes: Int) {
    ActiveFirst(CoreR.string.denylist_sort_active_first), NameAsc(CoreR.string.denylist_sort_name_asc), NameDesc(
        CoreR.string.denylist_sort_name_desc
    )
}

data class DenyListUiState(
    val loading: Boolean = true,
    val query: String = "",
    val searchVisible: Boolean = false,
    val showSystem: Boolean = false,
    val showOs: Boolean = false,
    val sortMethod: DenyListSortMethod = DenyListSortMethod.ActiveFirst,
    val items: List<DenyListAppUi> = emptyList()
)

class DenyListViewModel : ViewModel() {
    private val _state = MutableStateFlow(DenyListUiState())
    val state: StateFlow<DenyListUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private var allApps: List<DenyListAppUi> = emptyList()
    private var refreshJob: Job? = null
    private var queryApplyJob: Job? = null

    fun refresh(force: Boolean = false) {
        if (!force && refreshJob?.isActive == true) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { loadApps() }.onSuccess { loaded ->
                    allApps = loaded
                    _state.update { it.copy(loading = false) }
                    applyFilters()
                }.onFailure {
                    _state.update { st -> st.copy(loading = false) }
                    _messages.tryEmit(uiText(it.message ?: ""))
                }
        }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        queryApplyJob?.cancel()
        queryApplyJob = viewModelScope.launch {
            delay(120)
            applyFilters()
        }
    }

    fun toggleSearch() {
        val current = _state.value
        if (current.searchVisible) {
            _state.update { it.copy(query = "", searchVisible = false) }
            applyFilters()
        } else {
            _state.update { it.copy(searchVisible = true) }
        }
    }

    fun setShowSystem(value: Boolean) {
        _state.update {
            it.copy(
                showSystem = value, showOs = if (value) it.showOs else false
            )
        }
        applyFilters()
    }

    fun setShowOs(value: Boolean) {
        _state.update { it.copy(showOs = value) }
        applyFilters()
    }

    fun setSortMethod(value: DenyListSortMethod) {
        _state.update { it.copy(sortMethod = value) }
        applyFilters()
    }

    fun toggleExpanded(packageName: String) {
        allApps = allApps.map { app ->
            if (app.packageName == packageName) {
                rebuildAppState(app, expanded = !app.expanded)
            } else {
                app
            }
        }
        applyFilters(shouldSort = false)
    }

    fun toggleProcess(packageName: String, processName: String, processPackage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = allApps.firstOrNull { it.packageName == packageName } ?: return@launch
            val process = app.processes.firstOrNull {
                it.name == processName && it.packageName == processPackage
            } ?: return@launch
            val enabled = !process.enabled
            val cmd = if (enabled) "add" else "rm"
            val result = Shell.cmd(
                "magisk --denylist $cmd $processPackage ${shellQuote(processName)}"
            ).exec()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    allApps = allApps.map { candidate ->
                        if (candidate.packageName != packageName) {
                            candidate
                        } else {
                            val processes = candidate.processes.map { item ->
                                if (item.name == processName && item.packageName == processPackage) {
                                    item.copy(enabled = enabled)
                                } else {
                                    item
                                }
                            }
                            rebuildAppState(candidate, processes = processes)
                        }
                    }
                    applyFilters(shouldSort = false)
                } else {
                    postFailure()
                }
            }
        }
    }

    fun setAppChecked(packageName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = allApps.firstOrNull { it.packageName == packageName } ?: return@launch
            var success = true
            val affected = if (enabled) {
                if (app.expanded) app.processes else app.processes.filter { it.defaultSelection }
            } else {
                app.processes
            }

            if (enabled) {
                affected.filterNot { it.enabled }.forEach { process ->
                    success = Shell.cmd(
                        "magisk --denylist add ${process.packageName} ${shellQuote(process.name)}"
                    ).exec().isSuccess && success
                }
            } else {
                success = Shell.cmd("magisk --denylist rm $packageName").exec().isSuccess
                if (success) {
                    affected.filter { it.enabled && it.isIsolated }.forEach { process ->
                        success = Shell.cmd(
                            "magisk --denylist rm ${process.packageName} ${shellQuote(process.name)}"
                        ).exec().isSuccess && success
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    allApps = allApps.map { candidate ->
                        if (candidate.packageName != packageName) {
                            candidate
                        } else {
                            val processes = candidate.processes.map { process ->
                                if (affected.any { it.name == process.name && it.packageName == process.packageName }) {
                                    process.copy(enabled = enabled)
                                } else {
                                    process
                                }
                            }
                            rebuildAppState(candidate, processes = processes)
                        }
                    }
                    applyFilters(shouldSort = false)
                } else {
                    postFailure()
                }
            }
        }
    }

    private fun postFailure() {
        _messages.tryEmit(uiText(CoreR.string.failure))
    }

    private fun rebuildAppState(
        app: DenyListAppUi,
        processes: List<DenyListProcessUi> = app.processes,
        expanded: Boolean = app.expanded
    ): DenyListAppUi {
        return app.copy(
            expanded = expanded,
            processes = processes,
            checkedCount = processes.count { it.enabled },
            selectionState = DenyListAppUi.deriveSelectionState(processes)
        )
    }

    private fun applyFilters(shouldSort: Boolean = true) {
        val current = _state.value
        val query = current.query.trim().lowercase()

        if (!shouldSort) {
            val updatedItems = current.items.map { item ->
                allApps.firstOrNull { it.packageName == item.packageName } ?: item
            }
            _state.update { it.copy(items = updatedItems) }
            return
        }

        val filtered = allApps.asSequence().filter { app ->
                val visible =
                    app.expanded || app.checkedCount > 0 || (current.showSystem || !app.isSystem) && (app.isAppUid || current.showSystem && current.showOs)
                visible && (query.isBlank() || app.searchKey.contains(query))
            }.toList()

        val sorted = when (current.sortMethod) {
            DenyListSortMethod.ActiveFirst -> filtered.sortedWith(
                compareBy({ it.checkedCount == 0 }, { it.sortKey }, { it.packageName })
            )

            DenyListSortMethod.NameAsc -> filtered.sortedWith(
                compareBy({ it.sortKey }, { it.packageName })
            )

            DenyListSortMethod.NameDesc -> filtered.sortedWith(compareByDescending<DenyListAppUi> { it.sortKey }.thenByDescending { it.packageName })
        }

        _state.update { it.copy(items = sorted) }
    }

    @SuppressLint("InlinedApi")
    private suspend fun loadApps(): List<DenyListAppUi> = withContext(Dispatchers.Default) {
        val pm = AppContext.packageManager
        val denyList = Shell.cmd("magisk --denylist ls").exec().out.map { CmdlineListItem(it) }
        pm.getInstalledApplications(MATCH_UNINSTALLED_PACKAGES_COMPAT).asSequence()
            .filter { it.packageName != AppContext.packageName }.mapNotNull { app ->
                runCatching {
                    val info = AppProcessInfo(app, pm, denyList)
                    if (info.processes.isEmpty()) {
                        null
                    } else {
                        DenyListAppUi.create(
                            packageName = app.packageName,
                            label = info.label,
                            isSystem = info.isSystemApp(),
                            isAppUid = info.isApp(),
                            expanded = false,
                            processes = info.processes.map {
                                DenyListProcessUi(
                                    name = it.name,
                                    packageName = it.packageName,
                                    isIsolated = it.isIsolated,
                                    isAppZygote = it.isAppZygote,
                                    defaultSelection = it.isIsolated || it.isAppZygote || it.name == it.packageName,
                                    enabled = it.isEnabled
                                )
                            })
                    }
                }.getOrNull()
            }.toList()
    }

    override fun onCleared() {
        queryApplyJob?.cancel()
        refreshJob?.cancel()
        super.onCleared()
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return DenyListViewModel() as T
            }
        }
    }
}

internal fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
