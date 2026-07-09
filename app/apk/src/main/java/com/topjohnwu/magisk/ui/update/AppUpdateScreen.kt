package com.topjohnwu.magisk.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.ui.MainActivity
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
import com.topjohnwu.magisk.ui.component.MagiskMarkdown
import com.topjohnwu.magisk.ui.component.MagiskSection
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.component.card.MagiskCardAction
import com.topjohnwu.magisk.ui.component.card.MagiskStatusCard
import com.topjohnwu.magisk.ui.component.card.MagiskStatusMetric
import com.topjohnwu.magisk.utils.APKInstall
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.update.AppUpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppUpdateScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: AppUpdateViewModel = viewModel(factory = AppUpdateViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val downloadReady = state.downloadProgress?.let { it >= 1f } == true

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = when (text) {
                is UiText.Plain -> text.value
                is UiText.Resource -> context.getString(text.resId, *text.args.toTypedArray())
            }
            SystemToastManager.show(context, messageString)
        }
    }

    DisposableEffect(lifecycleOwner) {
        DownloadEngine.observeProgress(lifecycleOwner) { progress, subject ->
            if (subject is Subject.App) {
                viewModel.onDownloadProgress(progress)
            }
        }
        onDispose { }
    }

    if (state.loading && !state.hasUpdateInfo) {
        MagiskLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    MagiskLazyContent(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MagiskStatusCard(
                title = stringResource(CoreR.string.home_app_title),
                statusText = when {
                    state.downloadFailed -> stringResource(CoreR.string.download_file_error)
                    state.updateAvailable -> stringResource(CoreR.string.update_available)
                    state.hasUpdateInfo -> stringResource(CoreR.string.updated_channel)
                    else -> stringResource(CoreR.string.no_connection)
                },
                statusColor = when {
                    state.downloadFailed -> MaterialTheme.colorScheme.error
                    state.updateAvailable -> MaterialTheme.colorScheme.primary
                    state.hasUpdateInfo -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                icon = Icons.Rounded.Android,
                iconContainerColor = MaterialTheme.colorScheme.primary,
                iconTint = MaterialTheme.colorScheme.onPrimary,
                metrics = listOf(
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_installed_version),
                        value = state.installedVersion
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_version_code),
                        value = state.installedVersionCode
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_latest_version),
                        value = state.latestVersion.ifEmpty {
                            stringResource(CoreR.string.not_available)
                        }
                    ),
                    MagiskStatusMetric(
                        label = stringResource(CoreR.string.home_latest_version_code),
                        value = state.latestVersionCode.ifEmpty {
                            stringResource(CoreR.string.not_available)
                        }
                    )
                ),
                primaryAction = if (state.hasUpdateInfo) {
                    MagiskCardAction(
                        text = stringResource(
                            when {
                                downloadReady && state.updateAvailable -> CoreR.string.install
                                !state.updateAvailable -> CoreR.string.reinstall
                                else -> CoreR.string.download
                            }
                        ),
                        icon = if (downloadReady) {
                            Icons.Rounded.SystemUpdate
                        } else {
                            Icons.Rounded.Download
                        },
                        onClick = {
                            val activity = context as? MainActivity ?: return@MagiskCardAction
                            if (downloadReady) {
                                coroutineScope.launch {
                                    val installed =
                                        installDownloadedApk(activity, Subject.App(state.update))
                                    if (!installed) {
                                        startAppDownload(activity, viewModel, state)
                                    }
                                }
                            } else {
                                startAppDownload(activity, viewModel, state)
                            }
                        }
                    )
                } else {
                    null
                },
                secondaryAction = MagiskCardAction(
                    text = stringResource(CoreR.string.settings_check_update_title),
                    icon = Icons.Rounded.Refresh,
                    onClick = { viewModel.refresh(force = true) }
                ),
                actionsStacked = true
            )
        }

        state.downloadProgress?.takeIf { it < 1f }?.let { progress ->
            item {
                MagiskCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(CoreR.string.download),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (progress >= 0f) {
                            LinearWavyProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        if (state.update.note.isNotBlank()) {
            item {
                MagiskSection(
                    title = stringResource(CoreR.string.release_notes),
                    icon = Icons.AutoMirrored.Rounded.Article
                ) {
                    MagiskCard(modifier = Modifier.fillMaxWidth()) {
                        MagiskMarkdown(
                            markdown = state.update.note,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        } else if (!state.loading && !state.hasUpdateInfo) {
            item {
                MagiskEmptyState(
                    title = stringResource(CoreR.string.no_connection),
                    icon = Icons.Rounded.SystemUpdate
                )
            }
        }
    }
}

private fun startAppDownload(
    activity: MainActivity,
    viewModel: AppUpdateViewModel,
    state: com.topjohnwu.magisk.viewmodel.update.AppUpdateUiState
) {
    viewModel.onDownloadStarted()
    DownloadEngine.startWithActivity(
        activity = activity,
        extension = activity.extension,
        subject = Subject.App(state.update)
    )
}

private suspend fun installDownloadedApk(
    activity: MainActivity,
    subject: Subject.App
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val session = APKInstall.startSession(activity)
        activity.contentResolver.openInputStream(subject.file)?.use { input ->
            session.openStream(activity).use { output ->
                APKInstall.transfer(input, output)
            }
        } ?: return@withContext false
        session.waitIntent()?.let { intent ->
            withContext(Dispatchers.Main) {
                activity.startActivity(intent)
            }
        }
        true
    }.getOrDefault(false)
}
