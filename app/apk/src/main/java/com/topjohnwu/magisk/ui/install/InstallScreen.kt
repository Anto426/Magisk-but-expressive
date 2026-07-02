package com.topjohnwu.magisk.ui.install

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.VMFactory
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MagiskDialogAction
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskMarkdown
import com.topjohnwu.magisk.ui.component.MagiskSection
import com.topjohnwu.magisk.ui.component.MagiskSettingsGroup
import com.topjohnwu.magisk.ui.component.MagiskSettingsListItem
import com.topjohnwu.magisk.ui.component.MagiskSettingsSwitchItem
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.install.InstallViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun InstallScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: InstallViewModel = viewModel(factory = VMFactory)
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Resolve string resources for step 1
    val patchTitle = stringResource(CoreR.string.select_patch_file)
    val patchSubtitle = state.patchUri?.let { getFileName(it, context) }
        ?: stringResource(CoreR.string.select_patch_file_summary)
    val directTitle = stringResource(CoreR.string.direct_install)
    val directSubtitle = stringResource(CoreR.string.direct_install_summary)
    val inactiveTitle = stringResource(CoreR.string.install_inactive_slot)
    val inactiveSubtitle = stringResource(CoreR.string.install_inactive_slot_summary)

    // Keep state values reactive in Compose
    var keepVerity by remember { mutableStateOf(Config.keepVerity) }
    var keepEnc by remember { mutableStateOf(Config.keepEnc) }

    // File Picker Launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPatchFileSelected(uri)
        }
        viewModel.onFilePickerConsumed()
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.RequestFilePicker -> {
                    filePicker.launch("*/*")
                }

                is UiEffect.Navigate -> {
                    onNavigate(effect.route)
                }

                is UiEffect.Message -> {
                    val messageString = when (val text = effect.text) {
                        is UiText.Plain -> text.value
                        is UiText.Resource -> context.getString(
                            text.resId, *text.args.toTypedArray()
                        )
                    }
                    SystemToastManager.show(context, messageString)
                }

                else -> {}
            }
        }
    }

    if (state.showSecondSlotWarning) {
        MagiskDialog(
            onDismissRequest = viewModel::onSecondSlotWarningConsumed,
            title = stringResource(CoreR.string.install_inactive_slot),
            text = stringResource(CoreR.string.install_inactive_slot_msg),
            icon = Icons.Rounded.Restore,
            confirmAction = MagiskDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = {
                    viewModel.onSecondSlotWarningConsumed()
                    viewModel.install()
                }
            ),
            dismissAction = MagiskDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = viewModel::onSecondSlotWarningConsumed
            )
        )
    }

    MagiskLazyContent(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp)
    ) {
        if (state.step == 0) {
            // --- STEP 0: OPTIONS ---
            item {
                MagiskSettingsGroup(
                    title = stringResource(CoreR.string.install_options_title),
                    icon = Icons.Rounded.Tune,
                    items = listOf(
                        {
                            MagiskSettingsSwitchItem(
                                title = stringResource(CoreR.string.keep_dm_verity),
                                checked = keepVerity,
                                onCheckedChange = { checked ->
                                    keepVerity = checked
                                    Config.keepVerity = checked
                                },
                                leadingIcon = Icons.Rounded.Security
                            )
                        },
                        {
                            MagiskSettingsSwitchItem(
                                title = stringResource(CoreR.string.keep_force_encryption),
                                checked = keepEnc,
                                onCheckedChange = { checked ->
                                    keepEnc = checked
                                    Config.keepEnc = checked
                                },
                                leadingIcon = Icons.Rounded.Lock
                            )
                        }
                    )
                )
            }

            item {
                Button(
                    onClick = viewModel::nextStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(android.R.string.ok))
                }
            }

        } else {
            // --- STEP 1: METHODS & CHANGELOG ---

            val methods = buildList {
                add(
                    InstallMethodOption(
                        method = InstallViewModel.Method.PATCH,
                        title = patchTitle,
                        subtitle = patchSubtitle,
                        icon = Icons.AutoMirrored.Rounded.Article
                    )
                )
                if (viewModel.isRooted) {
                    add(
                        InstallMethodOption(
                            method = InstallViewModel.Method.DIRECT,
                            title = directTitle,
                            subtitle = directSubtitle,
                            icon = Icons.Rounded.Bolt
                        )
                    )
                }
                if (!viewModel.noSecondSlot) {
                    add(
                        InstallMethodOption(
                            method = InstallViewModel.Method.INACTIVE_SLOT,
                            title = inactiveTitle,
                            subtitle = inactiveSubtitle,
                            icon = Icons.Rounded.Restore
                        )
                    )
                }
            }

            item {
                MagiskSettingsGroup(
                    title = stringResource(CoreR.string.install_method_title),
                    icon = Icons.Rounded.Build,
                    items = methods.map { option ->
                        {
                            InstallMethodItem(
                                option = option,
                                selected = state.method == option.method,
                                onClick = { viewModel.selectMethod(option.method) }
                            )
                        }
                    }
                )
            }

            // Changelog Section
            if (state.notes.isNotBlank()) {
                item {
                    MagiskSection(
                        title = stringResource(CoreR.string.app_changelog),
                        icon = Icons.AutoMirrored.Rounded.Article
                    ) {
                        MagiskCard(modifier = Modifier.fillMaxWidth()) {
                            ChangelogContent(markdown = state.notes)
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun InstallTopBarActions(
    state: InstallViewModel.UiState,
    canInstall: Boolean,
    onInstall: () -> Unit
) {
    if (state.step == 1 && state.method != InstallViewModel.Method.NONE) {
        MagiskTopBarIconButton(
            icon = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(CoreR.string.install_start),
            onClick = onInstall,
            enabled = canInstall
        )
    }
}

@Composable
private fun ChangelogContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    MagiskMarkdown(
        markdown = markdown,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    )
}


private fun getFileName(uri: Uri, context: Context): String {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }
    }
    return uri.path?.substringAfterLast('/') ?: uri.toString()
}

data class InstallMethodOption(
    val method: InstallViewModel.Method,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun InstallMethodItem(
    option: InstallMethodOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    MagiskSettingsListItem(
        title = option.title,
        subtitle = option.subtitle,
        leadingIcon = option.icon,
        selected = selected,
        trailingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        onClick = onClick
    )
}
