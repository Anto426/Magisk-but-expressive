package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
            .widthIn(min = 180.dp)
            .heightIn(max = 480.dp),
        shape = MagiskComponentDefaults.ControlShape,
        containerColor = MagiskComponentDefaults.PanelContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        content = content
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
    trailingContent: (@Composable () -> Unit)? = null
) {
    val contentColor = when {
        !enabled -> MagiskComponentDefaults.PrimaryText.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        selected -> MagiskComponentDefaults.PrimaryIconTint
        else -> MagiskComponentDefaults.PrimaryText
    }

    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.62f)
                    )
                }
            }
        },
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (subtitle != null) 56.dp else 48.dp),
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = trailingContent
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
                                        onClick = { action(); onDismiss() },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MagiskComponentDefaults.PrimaryIconTint,
                                            unselectedColor = MagiskComponentDefaults.SecondaryIconTint
                                        )
                                    )
                                }
                            } else null,
                            onClick = { action(); if (selectedIndex < 0) onDismiss() }
                        )
                    }
                }
            )
        }
    }
}
