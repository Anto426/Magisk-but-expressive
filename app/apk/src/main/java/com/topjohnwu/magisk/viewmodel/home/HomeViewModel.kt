package com.topjohnwu.magisk.viewmodel.home

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.uiText
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.core.tasks.MagiskInstaller
import com.topjohnwu.magisk.runtime.MagiskInstallState
import com.topjohnwu.magisk.runtime.MagiskRuntimeEngine
import com.topjohnwu.magisk.runtime.MagiskRuntimeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

data class ContributorLink(
    @param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int, val url: String
)

data class Contributor(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val links: List<ContributorLink> = emptyList()
)

data class HomeUiState(
    val runtime: MagiskRuntimeState = MagiskRuntimeEngine.snapshot(),
    val magiskState: HomeViewModel.State = HomeViewModel.State.INVALID,
    val magiskInstalledVersion: String = "",
    val appState: HomeViewModel.State = HomeViewModel.State.LOADING,
    val managerRemoteVersion: String = "",
    val managerReleaseNotes: String = "",
    val managerInstalledVersion: String = "",
    val updateChannel: Int = Config.updateChannel,
    val packageName: String = "",
    val envActive: Boolean = runtime.isInstalled,
    val showHideRestore: Boolean = false,
    val showManagerInstall: Boolean = false,
    val envFixCode: Int = 0,
    val contributors: List<Contributor> = emptyList(),
    val contributorsLoading: Boolean = true,
    val noticeVisible: Boolean = Config.safetyNotice
)

interface GitHubService {
    @GET("repos/topjohnwu/Magisk/contributors")
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    suspend fun getContributors(@Query("per_page") perPage: Int = 30): List<Map<String, Any?>>
}

class HomeViewModel(private val svc: NetworkService) : ViewModel() {
    enum class State {
        LOADING, INVALID, OUTDATED, UP_TO_DATE
    }

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L

    private val gitHubService: GitHubService by lazy {
        Retrofit.Builder().baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create()).build()
            .create(GitHubService::class.java)
    }

    fun refresh(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && _state.value.appState != HomeViewModel.State.LOADING && now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS) {
            return
        }
        lastRefreshAt = now
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { if (it.contributors.isEmpty()) it.copy(contributorsLoading = true) else it }
            loadContributors()
            val remote = Info.fetchUpdate(svc)
            val appState = when {
                remote == null -> HomeViewModel.State.INVALID
                BuildConfig.APP_VERSION_CODE < remote.versionCode -> HomeViewModel.State.OUTDATED
                else -> HomeViewModel.State.UP_TO_DATE
            }
            val runtime = MagiskRuntimeEngine.snapshot()
            val magiskState = runtime.toHomeState()
            _state.update {
                it.copy(
                    runtime = runtime,
                    magiskState = magiskState,
                    magiskInstalledVersion = runtime.installedMagiskVersion,
                    appState = appState,
                    managerInstalledVersion = runtime.installedManagerVersion,
                    managerRemoteVersion = remote?.run {
                        val debug = Config.updateChannel == Config.Value.DEBUG_CHANNEL
                        "$version ($versionCode)" + if (debug) " (D)" else ""
                    }.orEmpty(),
                    managerReleaseNotes = remote?.note.orEmpty(),
                    updateChannel = Config.updateChannel,
                    packageName = AppContext.packageName,
                    envActive = runtime.isInstalled,
                    noticeVisible = Config.safetyNotice
                )
            }
            ensureEnv(runtime)
        }
    }

    fun hideNotice() {
        Config.safetyNotice = false
        _state.update { it.copy(noticeVisible = false) }
    }

    fun checkForMagiskUpdates() {
        refresh(force = true)
    }

    fun onHideRestorePressed() {
        _state.update { it.copy(showHideRestore = true) }
    }

    fun onHideRestoreConsumed() {
        _state.update { it.copy(showHideRestore = false) }
    }

    fun onEnvFixConsumed() {
        _state.update { it.copy(envFixCode = 0) }
    }

    fun onManagerPressed() {
        when (_state.value.appState) {
            HomeViewModel.State.LOADING -> _messages.tryEmit(uiText(CoreR.string.loading))
            HomeViewModel.State.INVALID -> _messages.tryEmit(uiText(CoreR.string.no_connection))
            else -> _state.update { it.copy(showManagerInstall = true) }
        }
    }

    fun onManagerInstallConsumed() {
        _state.update { it.copy(showManagerInstall = false) }
    }

    fun restoreImages() {
        viewModelScope.launch {
            _messages.tryEmit(uiText(CoreR.string.restore_img_msg))
            val success = MagiskInstaller.Restore().exec { }
            _messages.emit(uiText(if (success) CoreR.string.restore_done else CoreR.string.restore_fail))
        }
    }

    fun requestReboot(reason: String = "") {
        viewModelScope.launch {
            val runtime = MagiskRuntimeEngine.snapshot()
            if (!runtime.isRooted || !MagiskRuntimeEngine.hasRootShell()) {
                _messages.emit(uiText(CoreR.string.root_required_operation))
                return@launch
            }
            _effects.emit(UiEffect.Reboot(reason))
        }
    }

    fun openLink(link: String) {
        _effects.tryEmit(UiEffect.OpenUri(link.toUri()))
    }

    private suspend fun loadContributors() {
        val cached = cachedContributors()
        if (cached != null) {
            _state.update { it.copy(contributors = cached, contributorsLoading = false) }
            return
        }
        runCatching { gitHubService.getContributors(perPage = 30) }.onSuccess { raw ->
                val fetched = raw.mapNotNull { item ->
                    val login = item["login"] as? String ?: return@mapNotNull null
                    createContributor(
                        login = login,
                        avatarUrl = item["avatar_url"] as? String ?: "",
                        htmlUrl = item["html_url"] as? String ?: ""
                    )
                }
                val priorityOrder =
                    listOf("topjohnwu", "vvb2060", "yujincheng08", "rikkaw", "canyie")
                val fetchedMap = fetched.associateBy { it.login.lowercase(Locale.US) }
                val ordered = priorityOrder.mapNotNull { fetchedMap[it] }
                val finalList = withPinnedContributors(ordered.ifEmpty { fetched })
                cacheContributors(finalList)
                _state.update { it.copy(contributors = finalList, contributorsLoading = false) }
            }.onFailure {
                _state.update {
                    it.copy(
                        contributors = withPinnedContributors(emptyList()),
                        contributorsLoading = false
                    )
                }
            }
    }

    private suspend fun ensureEnv(runtime: MagiskRuntimeState) {
        if (!runtime.isInstalled || runtime.isUnsupported || checkedEnv) return
        val code = MagiskRuntimeEngine.checkEnvironment(runtime)
        if (code != 0) {
            _state.update { it.copy(envFixCode = code) }
        }
        checkedEnv = true
    }

    companion object {
        internal const val MIN_REFRESH_INTERVAL_MS = 1200L
        internal const val CONTRIBUTORS_CACHE_TTL_MS = 30L * 60_000L
        internal var contributorsCache: List<Contributor> = emptyList()
        internal var contributorsCacheTimestamp: Long = 0
        private var checkedEnv = false

        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return HomeViewModel(ServiceLocator.networkService) as T
            }
        }
    }
}

private fun MagiskRuntimeState.toHomeState(): HomeViewModel.State = when (installState) {
    MagiskInstallState.NotInstalled -> HomeViewModel.State.INVALID
    MagiskInstallState.Installed -> HomeViewModel.State.UP_TO_DATE
    MagiskInstallState.Outdated,
    MagiskInstallState.Unsupported -> HomeViewModel.State.OUTDATED
}

private val maintainerLinks: Map<String, List<ContributorLink>> = mapOf(
    "topjohnwu" to listOf(
        ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/topjohnwu"),
        ContributorLink(
            CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/topjohnwu/Magisk"
        )
    ), "vvb2060" to listOf(
        ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/vvb2060"),
        ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/vvb2060")
    ), "yujincheng08" to listOf(
        ContributorLink(
            CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/yujincheng08"
        ), ContributorLink(
            CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/yujincheng08"
        ), ContributorLink(
            CoreR.string.github,
            CoreR.drawable.ic_favorite,
            "https://github.com/sponsors/yujincheng08"
        )
    ), "rikkaw" to listOf(
        ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/rikkaw_"),
        ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/RikkaW")
    ), "canyie" to listOf(
        ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/canyieq"),
        ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/canyie")
    ), "anto426" to listOf(
        ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/Anto426")
    )
)

private val forkMaintainer = createContributor(
    login = "Anto426",
    avatarUrl = "https://github.com/Anto426.png",
    htmlUrl = "https://github.com/Anto426"
)

private fun createContributor(login: String, avatarUrl: String, htmlUrl: String): Contributor {
    val normalized = login.lowercase(Locale.US)
    return Contributor(login, avatarUrl, htmlUrl, maintainerLinks[normalized].orEmpty())
}

private fun cachedContributors(): List<Contributor>? {
    val cached = HomeViewModel.contributorsCache
    val cachedAt = HomeViewModel.contributorsCacheTimestamp
    return cached.takeIf {
        it.isNotEmpty() && System.currentTimeMillis() - cachedAt < HomeViewModel.CONTRIBUTORS_CACHE_TTL_MS
    }
}

private fun cacheContributors(list: List<Contributor>) {
    HomeViewModel.contributorsCache = withPinnedContributors(list)
    HomeViewModel.contributorsCacheTimestamp = System.currentTimeMillis()
}

private fun withPinnedContributors(list: List<Contributor>): List<Contributor> =
    (listOf(forkMaintainer) + list).distinctBy { it.login.lowercase(Locale.US) }
