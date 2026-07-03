package com.topjohnwu.magisk.ui.component.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MagiskWarningCard(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissContentDescription: String? = null,
    icon: ImageVector = Icons.Rounded.Warning,
    onDismiss: (() -> Unit)? = null
) {
    MagiskCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title != null || onDismiss != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (onDismiss != null) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = dismissContentDescription,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
