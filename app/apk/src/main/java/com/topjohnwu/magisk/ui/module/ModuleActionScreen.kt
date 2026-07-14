package com.topjohnwu.magisk.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.arch.resolve
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenu
import com.topjohnwu.magisk.ui.component.MagiskDropdownMenuItem
import com.topjohnwu.magisk.ui.component.MagiskLoader
import com.topjohnwu.magisk.ui.component.MagiskTerminal
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.module.ModuleActionViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun ModuleActionScreen(
    actionId: String,
    actionName: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ModuleActionViewModel = viewModel(factory = ModuleActionViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val lines by viewModel.lines.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(actionId, actionName) {
        viewModel.start(actionId, actionName)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = text.resolve(context)
            SystemToastManager.show(context, messageString)
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
                MagiskLoader(inline = true)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(CoreR.string.module_action_running),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ModuleActionTopBarActions(
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
