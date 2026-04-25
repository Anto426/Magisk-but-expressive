package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.core.R as CoreR

sealed interface ConfirmResult {
    data object Confirmed : ConfirmResult
    data object Canceled : ConfirmResult
}

data class DialogVisuals(
    val title: String = "",
    val content: String? = null,
    val markdown: Boolean = false,
    val confirm: String? = null,
    val dismiss: String? = null,
)

object MagiskDialogDefaults {
    val Shape
        @Composable get() = RoundedCornerShape(32.dp)

    val OptionShape
        @Composable get() = RoundedCornerShape(20.dp)

    val ContainerColor
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
fun MagiskDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title?.let {
            {
                MagiskDialogTitle(
                    title = it,
                    icon = icon,
                    iconTint = iconTint
                )
            }
        },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        shape = MagiskDialogDefaults.Shape,
        containerColor = MagiskDialogDefaults.ContainerColor
    )
}

@Composable
fun MagiskDialogTitle(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Surface(
                color = iconTint.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint
                    )
                }
            }
        }
        Text(
            text = title,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun MagiskDialogConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CircleShape,
        colors = if (destructive) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Text(
            text = text ?: stringResource(android.R.string.ok),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun MagiskDialogDismissButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape
    ) {
        Text(
            text = text ?: stringResource(android.R.string.cancel),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MagiskDialogOption(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    showRadio: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val containerColor by MagiskMotion.animateColor(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        },
        animationSpec = MagiskMotion.quickColorSpec(),
        label = "dialogOptionContainer"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MagiskDialogDefaults.OptionShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                showRadio -> {
                    RadioButton(selected = selected, onClick = null)
                    Spacer(Modifier.width(16.dp))
                }

                icon != null -> {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = accentColor
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagiskBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        content = content
    )
}

@Composable
fun MagiskDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        content = content
    )
}

@Composable
fun MagiskDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            when {
                selected -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                leadingIcon != null -> Icon(
                    imageVector = leadingIcon,
                    contentDescription = null
                )
            }
        }
    )
}

interface LoadingDialogHandle {
    suspend fun <R> withLoading(block: suspend () -> R): R
}

interface ConfirmDialogHandle {
    fun showConfirm(
        title: String,
        content: String? = null,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null
    )

    suspend fun awaitConfirm(
        title: String,
        content: String? = null,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null
    ): ConfirmResult
}

private class LoadingDialogHandleImpl(
    private val visible: MutableState<Boolean>,
) : LoadingDialogHandle {
    override suspend fun <R> withLoading(block: suspend () -> R): R {
        return try {
            withContext(Dispatchers.Main.immediate) { visible.value = true }
            block()
        } finally {
            withContext(Dispatchers.Main.immediate) { visible.value = false }
        }
    }
}

private class ConfirmDialogHandleImpl(
    private val visuals: MutableState<DialogVisuals?>,
) : ConfirmDialogHandle {

    var onResult: (ConfirmResult) -> Unit = {}
    private var awaitContinuation: CancellableContinuation<ConfirmResult>? = null

    override fun showConfirm(
        title: String,
        content: String?,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?
    ) {
        visuals.value = DialogVisuals(title, content, markdown, confirm, dismiss)
    }

    override suspend fun awaitConfirm(
        title: String,
        content: String?,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?
    ): ConfirmResult {
        visuals.value = DialogVisuals(title, content, markdown, confirm, dismiss)
        return suspendCancellableCoroutine { cont ->
            awaitContinuation?.cancel()
            awaitContinuation = cont
            cont.invokeOnCancellation {
                if (awaitContinuation === cont) {
                    awaitContinuation = null
                    visuals.value = null
                }
            }
        }
    }

    fun confirm() = complete(ConfirmResult.Confirmed)
    fun dismiss() = complete(ConfirmResult.Canceled)

    private fun complete(result: ConfirmResult) {
        visuals.value = null
        awaitContinuation?.takeIf { it.isActive }?.resume(result)
        awaitContinuation = null
        onResult(result)
    }
}

@Composable
fun rememberLoadingDialog(): LoadingDialogHandle {
    val visible = remember { mutableStateOf(false) }
    if (visible.value) {
        MagiskDialog(
            onDismissRequest = {},
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(text = stringResource(CoreR.string.loading))
                }
            },
            confirmButton = {}
        )
    }
    return remember { LoadingDialogHandleImpl(visible) }
}

@Composable
fun rememberConfirmDialog(
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
): ConfirmDialogHandle {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val visuals = remember { mutableStateOf<DialogVisuals?>(null) }
    val handle = remember { ConfirmDialogHandleImpl(visuals) }

    handle.onResult = { result ->
        when (result) {
            ConfirmResult.Confirmed -> currentOnConfirm?.invoke()
            ConfirmResult.Canceled -> currentOnDismiss?.invoke()
        }
    }

    ConfirmDialog(visuals = visuals, handle = handle)
    return handle
}

@Composable
private fun ConfirmDialog(
    visuals: State<DialogVisuals?>,
    handle: ConfirmDialogHandleImpl
) {
    val current = visuals.value ?: return
    MagiskDialog(
        onDismissRequest = { handle.dismiss() },
        title = current.title,
        text = {
            if (!current.content.isNullOrBlank()) {
                Text(text = current.content)
            }
        },
        dismissButton = {
            MagiskDialogDismissButton(
                onClick = { handle.dismiss() },
                text = current.dismiss
            )
        },
        confirmButton = {
            MagiskDialogConfirmButton(
                onClick = { handle.confirm() },
                text = current.confirm
            )
        }
    )
}
