package com.topjohnwu.magisk.ui.flash

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.POST_NOTIFICATIONS_PERMISSION
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.ktx.activity
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenu
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenuItem
import com.topjohnwu.magisk.ui.component.MagiskTerminal
import com.topjohnwu.magisk.ui.component.MagiskTerminalActions
import com.topjohnwu.magisk.ui.component.MagiskTerminalButton
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.flash.FlashViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun FlashScreen(
    action: String,
    additionalData: String?,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: FlashViewModel = viewModel(factory = FlashViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val lines by viewModel.lines.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val view = LocalView.current
    val listState = rememberLazyListState()
    val uiActivity = remember(view) { view.activity as? UIActivity<*> }

    BackHandler(enabled = state.running) {}

    LaunchedEffect(action, additionalData) {
        val uri = additionalData?.let { Uri.parse(it) }
        val remoteDownload = uri?.scheme == "http" || uri?.scheme == "https"
        val needsNotificationPermission =
            remoteDownload &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        POST_NOTIFICATIONS_PERMISSION
                    ) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission && uiActivity != null) {
            uiActivity.withPermission(POST_NOTIFICATIONS_PERMISSION) {
                viewModel.start(action, uri)
            }
        } else {
            viewModel.start(action, uri)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = when (text) {
                is UiText.Plain -> text.value
                is UiText.Resource -> context.getString(text.resId, *text.args.toTypedArray())
            }
            SystemToastManager.show(context, messageString)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is UiEffect.Reboot) {
                com.topjohnwu.magisk.core.ktx.reboot(effect.reason)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MagiskTerminal(
            lines = lines,
            state = listState,
            emptyText = stringResource(CoreR.string.waiting_for_logs),
            modifier = Modifier.weight(1f)
        )

        // Progress or Action footer
        if (state.running) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(CoreR.string.flashing),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            if (state.showReboot && state.success) {
                MagiskTerminalActions {
                    MagiskTerminalButton(
                        text = stringResource(CoreR.string.reboot),
                        onClick = viewModel::rebootNow,
                        modifier = Modifier.weight(1f),
                        primary = true,
                        icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun FlashTopBarActions(
    onSaveLog: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        MagiskTopBarIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = stringResource(CoreR.string.more_options),
            onClick = { expanded = true }
        )
        MagiskDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MagiskDropdownMenuItem(
                text = stringResource(CoreR.string.save_log),
                leadingIcon = Icons.Rounded.Save,
                onClick = {
                    onSaveLog()
                    expanded = false
                }
            )
        }
    }
}
