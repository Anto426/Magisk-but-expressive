package com.topjohnwu.magisk.viewmodel.support

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.di.createApiService
import com.topjohnwu.magisk.core.repository.NetworkService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import timber.log.Timber
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

data class SupportUiState(
    val contributors: List<Contributor> = emptyList(),
    val contributorsLoading: Boolean = true
)

interface SupportGitHubService {
    @GET("repos/topjohnwu/Magisk/contributors")
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    suspend fun getContributors(@Query("per_page") perPage: Int = 30): List<Map<String, Any?>>
}

class SupportViewModel(private val svc: NetworkService) : ViewModel() {

    val mbeDonateUrl = "https://buymeacoffee.com/anto426"
    val mbeSourceUrl = "https://github.com/Anto426/Magisk-but-expressive"
    val officialDonateUrl = "https://github.com/sponsors/topjohnwu"
    val officialDocsUrl = "https://topjohnwu.github.io/Magisk/"

    private val _state = MutableStateFlow(SupportUiState())
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    private val gitHubService: SupportGitHubService by lazy {
        createApiService(ServiceLocator.retrofit, "https://api.github.com/")
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(contributorsLoading = true) }
            loadContributors()
        }
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
        }.onFailure { e ->
            Timber.e(e)
            _state.update {
                it.copy(
                    contributors = withPinnedContributors(emptyList()),
                    contributorsLoading = false
                )
            }
        }
    }

    fun openLink(url: String) {
        _effects.tryEmit(UiEffect.OpenUri(android.net.Uri.parse(url)))
    }

    companion object {
        internal const val CONTRIBUTORS_CACHE_TTL_MS = 30L * 60_000L
        internal var contributorsCache: List<Contributor> = emptyList()
        internal var contributorsCacheTimestamp: Long = 0

        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SupportViewModel(ServiceLocator.networkService) as T
            }
        }
    }
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
        ContributorLink(CoreR.string.donate, CoreR.drawable.ic_favorite, "https://buymeacoffee.com/anto426"),
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
    val cached = SupportViewModel.contributorsCache
    val cachedAt = SupportViewModel.contributorsCacheTimestamp
    if (cached.isNotEmpty() && SystemClock.elapsedRealtime() - cachedAt < SupportViewModel.CONTRIBUTORS_CACHE_TTL_MS) {
        return cached
    }

    return runCatching {
        val prefs = AppContext.getSharedPreferences("git_contributors", android.content.Context.MODE_PRIVATE)
        val data = prefs.getString("cache_data", null) ?: return null
        val timestamp = prefs.getLong("cache_timestamp", 0)

        // 7-day TTL for persistent disk cache
        if (System.currentTimeMillis() - timestamp > 7L * 24 * 60 * 60 * 1000L) {
            return null
        }

        val array = JSONArray(data)
        val list = mutableListOf<Contributor>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val login = obj.getString("login")
            val avatarUrl = obj.getString("avatarUrl")
            val htmlUrl = obj.getString("htmlUrl")
            list.add(createContributor(login, avatarUrl, htmlUrl))
        }

        SupportViewModel.contributorsCache = list
        SupportViewModel.contributorsCacheTimestamp = SystemClock.elapsedRealtime()
        list
    }.getOrNull()
}

private fun cacheContributors(list: List<Contributor>) {
    val pinned = withPinnedContributors(list)
    val now = System.currentTimeMillis()
    SupportViewModel.contributorsCache = pinned
    SupportViewModel.contributorsCacheTimestamp = SystemClock.elapsedRealtime()

    runCatching {
        val array = JSONArray()
        pinned.forEach { c ->
            val obj = JSONObject().apply {
                put("login", c.login)
                put("avatarUrl", c.avatarUrl)
                put("htmlUrl", c.htmlUrl)
            }
            array.put(obj)
        }
        val prefs = AppContext.getSharedPreferences("git_contributors", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("cache_data", array.toString())
            putLong("cache_timestamp", now)
            apply()
        }
    }
}

private fun withPinnedContributors(list: List<Contributor>): List<Contributor> =
    (listOf(forkMaintainer) + list).distinctBy { it.login.lowercase(Locale.US) }
