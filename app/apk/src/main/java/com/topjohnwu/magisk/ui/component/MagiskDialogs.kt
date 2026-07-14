package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MagiskDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false
)

@Composable
fun MagiskDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    textContent: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    confirmAction: MagiskDialogAction? = null,
    dismissAction: MagiskDialogAction? = null
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MagiskComponentDefaults.PrimaryIconTint
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MagiskComponentDefaults.PrimaryText
            )
        },
        text = when {
            textContent != null -> textContent
            text != null -> {
                {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MagiskComponentDefaults.SecondaryText
                    )
                }
            }

            else -> null
        },
        confirmButton = {
            if (confirmAction != null) {
                Button(
                    onClick = confirmAction.onClick,
                    enabled = confirmAction.enabled,
                    colors = if (confirmAction.destructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmAction.text)
                }
            }
        },
        dismissButton = {
            if (dismissAction != null) {
                TextButton(
                    onClick = dismissAction.onClick,
                    enabled = dismissAction.enabled
                ) {
                    Text(dismissAction.text)
                }
            }
        },
        shape = MagiskComponentDefaults.CardShape,
        containerColor = MagiskComponentDefaults.PanelContainer
    )
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
        shape = MagiskComponentDefaults.CardShape,
        containerColor = MagiskComponentDefaults.PanelContainer,
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
        modifier = modifier
            .widthIn(min = 220.dp)
            .heightIn(max = 480.dp),
        shape = MagiskComponentDefaults.CardShape,
        containerColor = MagiskComponentDefaults.PanelContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        content = {
            Column(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content
            )
        }
    )
}

@Composable
fun MagiskDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    alignWithIcons: Boolean = false
) {
    val labelColor = when {
        !enabled -> MagiskComponentDefaults.PrimaryText.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MagiskComponentDefaults.PrimaryText
    }
    val iconContainerColor = when {
        destructive -> MaterialTheme.colorScheme.errorContainer
        else -> MagiskComponentDefaults.AccentContainer
    }
    val iconContentColor = when {
        !enabled -> MagiskComponentDefaults.PrimaryText.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.onErrorContainer
        else -> MagiskComponentDefaults.AccentContent
    }

    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = labelColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = labelColor.copy(alpha = 0.62f)
                    )
                }
            }
        },
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (subtitle != null) 56.dp else 48.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        leadingIcon = when {
            leadingIcon != null -> {
                {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        color = iconContainerColor,
                        contentColor = iconContentColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = iconContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            alignWithIcons -> {
                {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
            else -> null
        },
        trailingIcon = trailingContent ?: if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MagiskComponentDefaults.PrimaryIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            null
        }
    )
}

/**
 * Componente unico per pannelli bottom-sheet con lista opzioni.
 *
 * Usato come picker (con radio button) quando [selectedIndex] >= 0,
 * o come menu azioni (senza radio) quando [selectedIndex] < 0.
 */
@Composable
fun MagiskOptionsSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectedIndex: Int = -1,
    items: List<Triple<ImageVector?, String, () -> Unit>>
) {
    MagiskBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            MagiskSettingsGroup(
                title = title,
                icon = icon,
                items = items.mapIndexed { index, (itemIcon, label, action) ->
                    {
                        val isSelected = selectedIndex >= 0 && index == selectedIndex
                        MagiskSettingsListItem(
                            title = label,
                            selected = isSelected,
                            leadingIcon = itemIcon,
                            trailingContent = if (selectedIndex >= 0) {
                                {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MagiskComponentDefaults.PrimaryIconTint,
                                            unselectedColor = MagiskComponentDefaults.SecondaryIconTint
                                        )
                                    )
                                }
                            } else null,
                            onClick = { action(); onDismiss() },
                            interactionRole = if (selectedIndex >= 0) {
                                Role.RadioButton
                            } else {
                                Role.Button
                            },
                            selectionValue = if (selectedIndex >= 0) isSelected else null
                        )
                    }
                }
            )
        }
    }
}

/**
 * Bottom-sheet per confermare azioni (es. installazione, riavvio, fix dell'ambiente).
 * Sostituisce i vecchi dialoghi modali per uniformare lo stile bottom-sheet.
 */
@Composable
fun MagiskConfirmSheet(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    confirmText: String = stringResource(android.R.string.ok),
    dismissText: String = stringResource(android.R.string.cancel)
) {
    MagiskBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MagiskComponentDefaults.AccentContainer,
                    contentColor = MagiskComponentDefaults.AccentContent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon ?: Icons.Rounded.Warning,
                            contentDescription = null
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Text(text = confirmText)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MagiskComponentDefaults.ControlShape
                ) {
                    Text(text = dismissText)
                }
            }
        }
    }
}
