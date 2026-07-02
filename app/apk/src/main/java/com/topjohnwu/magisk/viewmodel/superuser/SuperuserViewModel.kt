package com.topjohnwu.magisk.viewmodel.superuser

import android.content.pm.PackageManager
import android.os.Process
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.MATCH_UNINSTALLED_PACKAGES_COMPAT
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.data.magiskdb.PolicyDao
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.model.su.SuLog
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.core.repository.LogRepository
import com.topjohnwu.magisk.core.su.SuEvents
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

data class PolicyUiItem(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val iconPackageName: String?,
    val isSharedUid: Boolean,
    val policy: Int,
    val remain: Long,
    val notification: Boolean,
    val logging: Boolean,
    val expanded: Boolean = false
) {
    val title: String
        get() = if (isSharedUid) AppContext.getString(
            CoreR.string.shared_uid_label, appName
        ) else appName
    val showSlider: Boolean get() = Config.suRestrict || policy == SuPolicy.RESTRICT
}

data class SuperuserUiState(
    val loading: Boolean = true,
    val items: List<PolicyUiItem> = emptyList(),
    val filteredItems: List<PolicyUiItem> = emptyList(),
    val searchQuery: String = "",
    val searchVisible: Boolean = false
)

class SuperuserViewModel(
    private val policyDao: PolicyDao,
    private val logRepo: LogRepository,
    private val pm: PackageManager
) : ViewModel() {
    private val _state = MutableStateFlow(SuperuserUiState())
    val state: StateFlow<SuperuserUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private var reloadJob: Job? = null
    private var lastReloadAt = 0L

    init {
        reload(force = true)
        @OptIn(kotlinx.coroutines.FlowPreview::class) viewModelScope.launch {
            SuEvents.policyChanged.debounce(400).collect { reload(force = true) }
        }
    }

    fun reload(force: Boolean = false) {
        val previousItems = _state.value.items
        val now = SystemClock.elapsedRealtime()
        if (!force && reloadJob?.isActive == true) return
        if (!force && now - lastReloadAt < MIN_RELOAD_INTERVAL_MS) return
        lastReloadAt = now
        reloadJob?.cancel()
        if (previousItems.isEmpty()) {
            _state.update { it.copy(loading = true) }
        }
        val previousByKey = previousItems.associateBy { policyKey(it.uid, it.packageName) }
        reloadJob = viewModelScope.launch {
            val policies = loadPolicies(previousByKey)
            _state.update { it.withItems(policies, loading = false) }
        }
    }

    private suspend fun loadPolicies(previousByKey: Map<String, PolicyUiItem>): List<PolicyUiItem> =
        withContext(Dispatchers.IO) {
            if (!MagiskRuntimeEngine.snapshot().canShowSuperuser) return@withContext emptyList()
            policyDao.deleteOutdated()
            policyDao.delete(AppContext.applicationInfo.uid)
            val previousByUid = previousByKey.values.groupBy { it.uid }
            val latestLogsByUid = logRepo.fetchSuLogs().asSequence().distinctBy { it.fromUid }
                .associateBy { it.fromUid }
            val policies = mutableListOf<PolicyUiItem>()
            for (policy in policyDao.fetchAll()) {
                val sizeBeforePolicy = policies.size
                val pkgs = if (policy.uid == Process.SYSTEM_UID) {
                    arrayOf("android")
                } else {
                    pm.getPackagesForUid(policy.uid)
                }
                if (pkgs.isNullOrEmpty()) {
                    val item = previousByUid[policy.uid]?.firstOrNull()?.withPolicy(policy)
                        ?: latestLogsByUid[policy.uid]?.toPolicyItem(policy)
                    item?.let(policies::add)
                    continue
                }
                pkgs.forEach { pkg ->
                    val item = runCatching {
                        val info = pm.getPackageInfo(pkg, MATCH_UNINSTALLED_PACKAGES_COMPAT)
                        val appInfo = info.applicationInfo
                        val key = policyKey(policy.uid, info.packageName)
                        val previous = previousByKey[key]
                        PolicyUiItem(
                            uid = policy.uid,
                            packageName = info.packageName,
                            appName = appInfo?.loadLabel(pm)?.toString() ?: info.packageName,
                            iconPackageName = info.packageName,
                            isSharedUid = info.sharedUserId != null,
                            policy = policy.policy,
                            remain = policy.remain,
                            notification = policy.notification,
                            logging = policy.logging,
                            expanded = previous?.expanded ?: false
                        )
                    }.getOrElse {
                        previousByKey[policyKey(policy.uid, pkg)]?.withPolicy(policy)
                            ?: latestLogsByUid[policy.uid]?.takeIf {
                                it.packageName == pkg || it.packageName.substringBefore(":") == pkg
                            }?.toPolicyItem(policy)
                    }
                    item?.let(policies::add)
                }
                if (policies.size == sizeBeforePolicy) {
                    val item = previousByUid[policy.uid]?.firstOrNull()?.withPolicy(policy)
                        ?: latestLogsByUid[policy.uid]?.toPolicyItem(policy)
                    item?.let(policies::add)
                }
            }
            policies.distinctBy { policyKey(it.uid, it.packageName) }
                .sortedWith(compareBy({ it.appName.lowercase(Locale.ROOT) }, { it.packageName }))
        }

    fun toggleExpanded(uid: Int, packageName: String) {
        _state.update { state ->
            state.withItems(state.items.map {
                if (it.uid == uid && it.packageName == packageName) it.copy(expanded = !it.expanded) else it
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

    fun setPolicy(uid: Int, policy: Int) {
        viewModelScope.launch {
            val item = state.value.items.firstOrNull { it.uid == uid } ?: return@launch
            withContext(Dispatchers.IO) {
                policyDao.update(item.toPolicy(policy = policy))
            }
            _state.update { state ->
                state.withItems(state.items.map { if (it.uid == uid) it.copy(policy = policy) else it })
            }
            _messages.emit(
                uiText(
                    if (policy >= SuPolicy.ALLOW) CoreR.string.su_snack_grant else CoreR.string.su_snack_deny,
                    item.appName
                )
            )
        }
    }

    fun toggleNotify(item: PolicyUiItem) {
        viewModelScope.launch {
            val value = !item.notification
            withContext(Dispatchers.IO) {
                policyDao.update(item.toPolicy(notification = value))
            }
            _state.update { state ->
                state.withItems(state.items.map {
                    if (it.uid == item.uid) it.copy(notification = value) else it
                })
            }
            _messages.emit(
                uiText(
                    if (value) CoreR.string.su_snack_notif_on else CoreR.string.su_snack_notif_off,
                    item.appName
                )
            )
        }
    }

    fun toggleLog(item: PolicyUiItem) {
        viewModelScope.launch {
            val value = !item.logging
            withContext(Dispatchers.IO) {
                policyDao.update(item.toPolicy(logging = value))
            }
            _state.update { state ->
                state.withItems(state.items.map {
                    if (it.uid == item.uid) it.copy(logging = value) else it
                })
            }
            _messages.emit(
                uiText(
                    if (value) CoreR.string.su_snack_log_on else CoreR.string.su_snack_log_off,
                    item.appName
                )
            )
        }
    }

    fun revoke(item: PolicyUiItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { policyDao.delete(item.uid) }
            _state.update { state -> state.withItems(state.items.filterNot { it.uid == item.uid }) }
            _messages.emit(uiText(CoreR.string.su_snack_deny, item.appName))
        }
    }

    private fun policyKey(uid: Int, packageName: String): String = "$uid:$packageName"

    private fun PolicyUiItem.withPolicy(policy: SuPolicy) = copy(
        policy = policy.policy,
        remain = policy.remain,
        notification = policy.notification,
        logging = policy.logging
    )

    private fun PolicyUiItem.toPolicy(
        policy: Int = this.policy,
        notification: Boolean = this.notification,
        logging: Boolean = this.logging
    ) = SuPolicy(
        uid = uid, policy = policy, remain = remain, notification = notification, logging = logging
    )

    private fun SuLog.toPolicyItem(policy: SuPolicy): PolicyUiItem {
        val fallbackName = AppContext.getString(CoreR.string.uid_label, policy.uid)
        val resolvedPackage = packageName.takeUnless { it.isBlank() } ?: fallbackName
        val resolvedAppName = appName.takeUnless { it.isBlank() } ?: resolvedPackage
        val iconPackage = resolvedPackage.takeUnless { it == fallbackName }?.substringBefore(":")
        return PolicyUiItem(
            uid = policy.uid,
            packageName = resolvedPackage,
            appName = resolvedAppName,
            iconPackageName = iconPackage,
            isSharedUid = resolvedPackage.contains(":") || resolvedPackage == fallbackName,
            policy = policy.policy,
            remain = policy.remain,
            notification = policy.notification,
            logging = policy.logging
        )
    }

    companion object {
        private const val MIN_RELOAD_INTERVAL_MS = 1200L

        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SuperuserViewModel(
                    ServiceLocator.policyDB, ServiceLocator.logRepo, AppContext.packageManager
                ) as T
            }
        }
    }
}

fun policyToSliderValue(policy: Int): Float = when (policy) {
    SuPolicy.DENY -> 1f
    SuPolicy.RESTRICT -> 2f
    SuPolicy.ALLOW -> 3f
    else -> 1f
}

fun sliderValueToPolicy(value: Float): Int = when (value.toInt()) {
    1 -> SuPolicy.DENY
    2 -> SuPolicy.RESTRICT
    3 -> SuPolicy.ALLOW
    else -> SuPolicy.DENY
}

private fun SuperuserUiState.withItems(items: List<PolicyUiItem>, loading: Boolean = this.loading): SuperuserUiState {
    return copy(
        items = items,
        filteredItems = items.filteredBy(searchQuery),
        loading = loading
    )
}

private fun SuperuserUiState.withSearchQuery(query: String): SuperuserUiState {
    return copy(
        searchQuery = query,
        filteredItems = items.filteredBy(query)
    )
}

private fun List<PolicyUiItem>.filteredBy(query: String): List<PolicyUiItem> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    return if (normalized.isEmpty()) this else filter {
        it.appName.lowercase(Locale.ROOT).contains(normalized) ||
                it.packageName.lowercase(Locale.ROOT).contains(normalized)
    }
}
