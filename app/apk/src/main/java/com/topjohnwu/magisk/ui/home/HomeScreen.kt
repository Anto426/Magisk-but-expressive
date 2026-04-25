package com.topjohnwu.magisk.ui.home

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.ktx.await
import com.topjohnwu.magisk.core.ktx.reboot
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.tasks.MagiskInstaller
import com.topjohnwu.magisk.ui.MainActivity
import com.topjohnwu.magisk.ui.RefreshOnResume
import com.topjohnwu.magisk.ui.component.MagiskBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogConfirmButton
import com.topjohnwu.magisk.ui.component.MagiskDialogDismissButton
import com.topjohnwu.magisk.ui.component.MagiskDialogOption
import com.topjohnwu.magisk.ui.component.MagiskSnackbarHost
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.ui.component.SectionHeader
import com.topjohnwu.magisk.ui.component.rememberLoadingDialog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun HomeScreen(
    rebootRequestToken: Int = 0,
    onRebootTokenConsumed: () -> Unit = {},
    onOpenInstall: () -> Unit = {},
    onOpenUninstall: () -> Unit = {},
    viewModel: HomeComposeViewModel = viewModel(factory = HomeComposeViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val isHiddenApp = remember(context.packageName) { context.packageName != BuildConfig.APP_PACKAGE_NAME }
    val snackbarHostState = remember { SnackbarHostState() }
    var showUninstallDialog by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var showManagerInstallSheet by remember { mutableStateOf(false) }
    var showEnvFixDialog by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var envFixCode by remember { mutableStateOf(0) }

    RefreshOnResume { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(rebootRequestToken) {
        if (rebootRequestToken > 0) {
            onRebootTokenConsumed()
            showRebootDialog = true
        }
    }
    LaunchedEffect(state.envFixCode) {
        if (state.envFixCode != 0) {
            envFixCode = state.envFixCode
            showEnvFixDialog = true
            viewModel.onEnvFixConsumed()
        }
    }
    LaunchedEffect(state.showHideRestore) {
        if (state.showHideRestore) {
            if (isHiddenApp) showRestoreDialog = true else showHideDialog = true
            viewModel.onHideRestoreConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = MagiskUiDefaults.verticalContentPadding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.ListItemSpacing)
        ) {
            item {
                HomeStatusHeroCard(envActive = state.envActive)
            }

            if (state.noticeVisible) {
                item {
                    Box(modifier = Modifier.padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding)) {
                        HomeNotice(onHide = viewModel::hideNotice)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing)
                ) {
                    SectionHeader(
                        title = stringResource(id = CoreR.string.home_section_magisk_core),
                        icon = Icons.Rounded.VerifiedUser,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                    HomeMagiskCoreCard(
                        magiskState = state.magiskState,
                        magiskInstalledVersion = state.magiskInstalledVersion,
                        onAction = onOpenInstall
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing)
                ) {
                    SectionHeader(
                        title = stringResource(id = CoreR.string.home_section_application),
                        icon = Icons.Rounded.AppShortcut,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                    HomeAppCard(
                        appState = state.appState,
                        managerInstalledVersion = state.managerInstalledVersion,
                        managerRemoteVersion = state.managerRemoteVersion,
                        updateChannelName = state.updateChannelName,
                        packageName = state.packageName,
                        isHidden = isHiddenApp,
                        onAction = {
                            viewModel.onManagerPressed {
                                showManagerInstallSheet = true
                            }
                        },
                        onHideRestore = viewModel::onHideRestorePressed
                    )
                }
            }

            if (state.envActive) {
                item {
                    Box(modifier = Modifier.padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding)) {
                        HomeUninstallCard(onClick = { showUninstallDialog = true })
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing)
                ) {
                    SectionHeader(
                        title = stringResource(id = CoreR.string.home_section_contributors),
                        icon = Icons.Rounded.Groups,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                    HomeContributorsList(
                        state.contributors,
                        state.contributorsLoading,
                        onOpen = { viewModel.openLink(context, it) }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing)
                ) {
                    SectionHeader(
                        title = stringResource(id = CoreR.string.home_support_title),
                        icon = Icons.Rounded.Favorite,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                    HomeSupportCard(
                        onPatreon = { viewModel.openLink(context, Const.Url.PATREON_URL) },
                        onPaypal = { viewModel.openLink(context, Const.Url.PAYPAL_URL) }
                    )
                }
            }
        }

        MagiskSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MagiskUiDefaults.SnackbarBottomPaddingWithBar)
        )
    }

    if (showRebootDialog) {
        RebootExpressiveDialog(
            onDismiss = { showRebootDialog = false },
            onReboot = { type ->
                showRebootDialog = false
                reboot(type)
            }
        )
    }

    if (showUninstallDialog) {
        UninstallExpressiveDialog(
            onDismiss = { showUninstallDialog = false },
            onRestoreImages = {
                showUninstallDialog = false
                viewModel.restoreImages()
            },
            onCompleteUninstall = {
                showUninstallDialog = false
                onOpenUninstall()
            }
        )
    }

    if (showEnvFixDialog) {
        EnvFixExpressiveDialog(
            code = envFixCode,
            onDismiss = { showEnvFixDialog = false },
            onFix = {
                showEnvFixDialog = false
                val needsFullFix = envFixCode == 2 ||
                        Info.env.versionCode != BuildConfig.APP_VERSION_CODE ||
                        Info.env.versionString != BuildConfig.APP_VERSION_NAME
                if (needsFullFix) {
                    onOpenInstall()
                } else {
                    scope.launch {
                        val success = loadingDialog.withLoading {
                            MagiskInstaller.FixEnv().exec()
                        }
                        activity.toast(
                            if (success) CoreR.string.reboot_delay_toast else CoreR.string.setup_fail,
                            Toast.LENGTH_LONG
                        )
                        if (success) {
                            Handler(Looper.getMainLooper()).postDelayed({ reboot() }, 5000)
                        }
                    }
                }
            }
        )
    }

    if (showHideDialog) {
        HideAppExpressiveDialog(
            onDismiss = { showHideDialog = false },
            onConfirm = { appName ->
                showHideDialog = false
                scope.launch {
                    loadingDialog.withLoading {
                        AppMigration.patchAndHide(context, appName)
                    }
                }
            }
        )
    }

    if (showRestoreDialog) {
        RestoreAppExpressiveDialog(
            onDismiss = { showRestoreDialog = false },
            onConfirm = {
                showRestoreDialog = false
                scope.launch {
                    loadingDialog.withLoading {
                        AppMigration.restoreApp(context)
                    }
                }
            }
        )
    }

    if (showManagerInstallSheet) {
        ManagerInstallSheet(
            notes = state.managerReleaseNotes,
            onDismiss = { showManagerInstallSheet = false },
            onInstall = {
                showManagerInstallSheet = false
                DownloadEngine.startWithActivity(activity, activity.extension, Subject.App())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerInstallSheet(
    notes: String,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    MagiskBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.SectionSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SystemUpdateAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = CoreR.string.install),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(id = CoreR.string.release_notes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 400.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    HomeMarkdownText(
                        markdown = notes.ifBlank { AppContext.getString(CoreR.string.not_available) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(MagiskUiDefaults.ActionHeight),
                    shape = MagiskUiDefaults.PillShape
                ) {
                    Text(stringResource(id = android.R.string.cancel), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onInstall,
                    modifier = Modifier
                        .weight(1f)
                        .height(MagiskUiDefaults.ActionHeight),
                    shape = MagiskUiDefaults.PillShape
                ) {
                    Icon(Icons.Rounded.DownloadDone, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = CoreR.string.install), fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor)
                textSize = 14f
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            if (textView.tag != markdown) {
                ServiceLocator.markwon.setMarkdown(textView, markdown)
                textView.tag = markdown
            }
        }
    )
}

@Composable
private fun RebootExpressiveDialog(onDismiss: () -> Unit, onReboot: (String) -> Unit) {
    MagiskDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = CoreR.string.reboot),
        icon = Icons.Rounded.RestartAlt,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MagiskDialogOption(
                    icon = Icons.Rounded.PowerSettingsNew,
                    title = stringResource(id = CoreR.string.reboot),
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = { onReboot("") }
                )
                MagiskDialogOption(
                    icon = Icons.Rounded.History,
                    title = stringResource(id = CoreR.string.reboot_recovery),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = { onReboot("recovery") }
                )
                MagiskDialogOption(
                    icon = Icons.Rounded.Terminal,
                    title = stringResource(id = CoreR.string.reboot_bootloader),
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { onReboot("bootloader") }
                )
                MagiskDialogOption(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(id = CoreR.string.reboot_userspace),
                    accentColor = MaterialTheme.colorScheme.outline,
                    onClick = { onReboot("userspace") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            MagiskDialogDismissButton(
                onClick = onDismiss,
                text = stringResource(id = CoreR.string.close)
            )
        }
    )
}

@Composable
private fun UninstallExpressiveDialog(onDismiss: () -> Unit, onRestoreImages: () -> Unit, onCompleteUninstall: () -> Unit) {
    MagiskDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = CoreR.string.uninstall_magisk_title),
        icon = Icons.Rounded.DeleteSweep,
        iconTint = MaterialTheme.colorScheme.error,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(id = CoreR.string.uninstall_magisk_msg))
                MagiskDialogOption(
                    icon = Icons.Rounded.SettingsBackupRestore,
                    title = stringResource(id = CoreR.string.restore_img),
                    subtitle = stringResource(id = CoreR.string.uninstall_restore_images_subtitle),
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onRestoreImages
                )
                MagiskDialogOption(
                    icon = Icons.Rounded.DeleteForever,
                    title = stringResource(id = CoreR.string.complete_uninstall),
                    subtitle = stringResource(id = CoreR.string.uninstall_complete_subtitle),
                    accentColor = MaterialTheme.colorScheme.error,
                    onClick = onCompleteUninstall
                )
            }
        },
        dismissButton = {
            MagiskDialogDismissButton(
                onClick = onDismiss,
                text = stringResource(id = CoreR.string.close)
            )
        }
    )
}

@Composable
private fun EnvFixExpressiveDialog(code: Int, onDismiss: () -> Unit, onFix: () -> Unit) {
    val needsFullFix = code == 2 || Info.env.versionCode != BuildConfig.APP_VERSION_CODE || Info.env.versionString != BuildConfig.APP_VERSION_NAME
    MagiskDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = CoreR.string.env_fix_title),
        icon = Icons.Rounded.BuildCircle,
        text = { Text(stringResource(id = if (needsFullFix) CoreR.string.env_full_fix_msg else CoreR.string.env_fix_msg)) },
        confirmButton = {
            MagiskDialogConfirmButton(onClick = onFix)
        },
        dismissButton = {
            MagiskDialogDismissButton(onClick = onDismiss)
        }
    )
}

@Composable
private fun HideAppExpressiveDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val defaultAppName = stringResource(id = CoreR.string.settings)
    var appName by remember(defaultAppName) { mutableStateOf(defaultAppName) }
    val isError = appName.length > AppMigration.MAX_LABEL_LENGTH || appName.isBlank()
    MagiskDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = CoreR.string.settings_hide_app_title),
        icon = Icons.Rounded.Masks,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(id = CoreR.string.settings_hide_app_summary))
                OutlinedTextField(
                    value = appName, onValueChange = { appName = it },
                    label = { Text(stringResource(id = CoreR.string.settings_app_name_hint)) },
                    singleLine = true, isError = isError, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            MagiskDialogConfirmButton(
                onClick = { onConfirm(appName) },
                enabled = !isError
            )
        },
        dismissButton = {
            MagiskDialogDismissButton(onClick = onDismiss)
        }
    )
}

@Composable
private fun RestoreAppExpressiveDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    MagiskDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = CoreR.string.settings_restore_app_title),
        icon = Icons.Rounded.RestorePage,
        text = { Text(stringResource(id = CoreR.string.restore_app_confirmation)) },
        confirmButton = {
            MagiskDialogConfirmButton(onClick = onConfirm)
        },
        dismissButton = {
            MagiskDialogDismissButton(onClick = onDismiss)
        }
    )
}

// Logic components - Mantengo questi per compatibilità
data class ContributorLink(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val url: String
)
data class Contributor(val login: String, val avatarUrl: String, val htmlUrl: String, val links: List<ContributorLink> = emptyList())
private val MAINTAINER_LINKS: Map<String, List<ContributorLink>> = mapOf(
    "topjohnwu" to listOf(ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/topjohnwu"), ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/topjohnwu/Magisk")),
    "vvb2060" to listOf(ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/vvb2060"), ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/vvb2060")),
    "yujincheng08" to listOf(ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/yujincheng08"), ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/yujincheng08"), ContributorLink(CoreR.string.github, CoreR.drawable.ic_favorite, "https://github.com/sponsors/yujincheng08")),
    "rikkaw" to listOf(ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/rikkaw_"), ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/RikkaW")),
    "canyie" to listOf(ContributorLink(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/canyieq"), ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/canyie")),
    "anto426" to listOf(ContributorLink(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/Anto426"))
)
private fun createContributor(login: String, avatarUrl: String, htmlUrl: String): Contributor {
    val normalized = login.lowercase(Locale.US)
    return Contributor(login = login, avatarUrl = avatarUrl, htmlUrl = htmlUrl, links = MAINTAINER_LINKS[normalized].orEmpty())
}
private val FORK_MAINTAINER = createContributor(login = "Anto426", avatarUrl = "https://github.com/Anto426.png", htmlUrl = "https://github.com/Anto426")

interface GitHubService {
    @GET("repos/topjohnwu/Magisk/contributors")
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    suspend fun getContributors(@Query("per_page") perPage: Int = 30): List<Map<String, Any?>>
}

data class HomeUiState(
    val magiskState: HomeViewModel.State = HomeViewModel.State.INVALID,
    val magiskInstalledVersion: String = AppContext.getString(CoreR.string.not_available),
    val appState: HomeViewModel.State = HomeViewModel.State.LOADING,
    val managerRemoteVersion: String = AppContext.getString(CoreR.string.not_available),
    val managerReleaseNotes: String = "",
    val managerInstalledVersion: String = "",
    val updateChannelName: String = AppContext.getString(CoreR.string.settings_update_stable),
    val packageName: String = "",
    val envActive: Boolean = Info.env.isActive,
    val showHideRestore: Boolean = false,
    val envFixCode: Int = 0,
    val contributors: List<Contributor> = emptyList(),
    val contributorsLoading: Boolean = true,
    val noticeVisible: Boolean = Config.safetyNotice
)

class HomeComposeViewModel(private val svc: NetworkService) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L
    private val gitHubService: GitHubService by lazy {
        Retrofit.Builder().baseUrl("https://api.github.com/").addConverterFactory(MoshiConverterFactory.create()).build().create(GitHubService::class.java)
    }

    private fun cachedContributors(): List<Contributor>? {
        val cached = contributorsCache
        val cachedAt = contributorsCacheTimestamp
        return cached.takeIf { cached.isNotEmpty() && System.currentTimeMillis() - cachedAt < CONTRIBUTORS_CACHE_TTL_MS }
    }

    private fun cacheContributors(list: List<Contributor>) {
        contributorsCache = withPinnedContributors(list)
        contributorsCacheTimestamp = System.currentTimeMillis()
    }

    private fun withPinnedContributors(list: List<Contributor>): List<Contributor> {
        return (listOf(FORK_MAINTAINER) + list).distinctBy { it.login.lowercase(Locale.US) }
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
            val cached = cachedContributors()
            if (cached != null) {
                _state.update { it.copy(contributors = cached, contributorsLoading = false) }
            } else {
                launch {
                    runCatching { gitHubService.getContributors(perPage = 30) }
                        .onSuccess { raw ->
                            val fetched = raw.mapNotNull { item ->
                                val login = item["login"] as? String ?: return@mapNotNull null
                                createContributor(login = login, avatarUrl = item["avatar_url"] as? String ?: "", htmlUrl = item["html_url"] as? String ?: "")
                            }
                            val priorityOrder = listOf("topjohnwu", "vvb2060", "yujincheng08", "rikkaw", "canyie")
                            val fetchedMap = fetched.associateBy { it.login.lowercase(Locale.US) }
                            val ordered = priorityOrder.mapNotNull { handle -> fetchedMap[handle] }
                            val finalList = withPinnedContributors(ordered.ifEmpty { fetched })
                            cacheContributors(finalList)
                            _state.update { it.copy(contributors = finalList, contributorsLoading = false) }
                        }
                        .onFailure { _state.update { it.copy(contributors = withPinnedContributors(emptyList()), contributorsLoading = false) } }
                }
            }
            val remote = Info.fetchUpdate(svc)
            val appState = when {
                remote == null -> HomeViewModel.State.INVALID
                BuildConfig.APP_VERSION_CODE < remote.versionCode -> HomeViewModel.State.OUTDATED
                else -> HomeViewModel.State.UP_TO_DATE
            }
            val magiskState = when {
                Info.isRooted && Info.env.isUnsupported -> HomeViewModel.State.OUTDATED
                !Info.env.isActive -> HomeViewModel.State.INVALID
                Info.env.versionCode < BuildConfig.APP_VERSION_CODE -> HomeViewModel.State.OUTDATED
                else -> HomeViewModel.State.UP_TO_DATE
            }
            val managerInstalled = "${BuildConfig.APP_VERSION_NAME} (${BuildConfig.APP_VERSION_CODE})" + if (BuildConfig.DEBUG) " (D)" else ""
            _state.update {
                it.copy(
                    magiskState = magiskState,
                    magiskInstalledVersion = Info.env.run { if (isActive) "$versionString ($versionCode)" + if (isDebug) " (D)" else "" else AppContext.getString(CoreR.string.not_available) },
                    appState = appState,
                    managerInstalledVersion = managerInstalled,
                    managerRemoteVersion = remote?.run { val isDebug = Config.updateChannel == Config.Value.DEBUG_CHANNEL; "$version ($versionCode)" + if (isDebug) " (D)" else "" } ?: AppContext.getString(CoreR.string.not_available),
                    managerReleaseNotes = remote?.note.orEmpty(),
                    updateChannelName = AppContext.resources.getStringArray(CoreR.array.update_channel).getOrElse(Config.updateChannel) { AppContext.getString(CoreR.string.settings_update_stable) },
                    packageName = AppContext.packageName,
                    envActive = Info.env.isActive,
                    noticeVisible = Config.safetyNotice
                )
            }
            ensureEnv(magiskState)
        }
    }

    fun hideNotice() { Config.safetyNotice = false; _state.update { it.copy(noticeVisible = false) } }
    fun checkForMagiskUpdates() { refresh() }
    fun onHideRestorePressed() { _state.update { it.copy(showHideRestore = true) } }
    fun onHideRestoreConsumed() { _state.update { it.copy(showHideRestore = false) } }
    fun onEnvFixConsumed() { _state.update { it.copy(envFixCode = 0) } }
    fun onManagerPressed(onShowInstallSheet: () -> Unit) {
        when (_state.value.appState) {
            HomeViewModel.State.LOADING -> _messages.tryEmit(AppContext.getString(CoreR.string.loading))
            HomeViewModel.State.INVALID -> _messages.tryEmit(AppContext.getString(CoreR.string.no_connection))
            else -> onShowInstallSheet()
        }
    }
    fun restoreImages() {
        viewModelScope.launch {
            _messages.tryEmit(AppContext.getString(CoreR.string.restore_img_msg))
            val success = MagiskInstaller.Restore().exec { }
            _messages.emit(AppContext.getString(if (success) CoreR.string.restore_done else CoreR.string.restore_fail))
        }
    }
    fun openLink(c: android.content.Context, l: String) {
        try { c.startActivity(Intent(Intent.ACTION_VIEW, l.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { _messages.tryEmit(AppContext.getString(CoreR.string.open_link_failed_toast)) }
    }
    private suspend fun ensureEnv(magiskState: HomeViewModel.State) {
        if (magiskState == HomeViewModel.State.INVALID || checkedEnv) return
        val cmd = "env_check ${Info.env.versionString} ${Info.env.versionCode}"
        val code = runCatching { Shell.cmd(cmd).await().code }.getOrDefault(0)
        if (code != 0) _state.update { it.copy(envFixCode = code) }
        checkedEnv = true
    }
    companion object {
        private const val MIN_REFRESH_INTERVAL_MS = 1200L
        private const val CONTRIBUTORS_CACHE_TTL_MS = 30L * 60_000L
        private var contributorsCache: List<Contributor> = emptyList()
        private var contributorsCacheTimestamp: Long = 0
        private var checkedEnv = false
    }
    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return HomeComposeViewModel(ServiceLocator.networkService) as T
        }
    }
}
