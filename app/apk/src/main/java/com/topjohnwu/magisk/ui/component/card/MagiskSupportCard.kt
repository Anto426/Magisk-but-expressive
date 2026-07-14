package com.topjohnwu.magisk.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults

@Composable
fun MagiskSupportCard(
    title: String,
    message: String,
    primaryAction: MagiskCardAction,
    modifier: Modifier = Modifier,
    shape: Shape = MagiskComponentDefaults.CardShape,
    secondaryAction: MagiskCardAction? = null,
    actionsStacked: Boolean = false,
    icon: ImageVector = Icons.Rounded.Favorite
) {
    MagiskCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        containerColor = MagiskComponentDefaults.CardContainer,
        border = MagiskComponentDefaults.CardBorder,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon badge + Title and Message
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(MagiskComponentDefaults.IconBadgeSize),
                    shape = MagiskComponentDefaults.ControlShape,
                    color = MagiskComponentDefaults.AccentContainer,
                    contentColor = MagiskComponentDefaults.AccentContent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MagiskComponentDefaults.PrimaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MagiskComponentDefaults.SecondaryText,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Divider separating content from actions
            HorizontalDivider(color = MagiskComponentDefaults.DividerColor)

            // Action Buttons
            if (actionsStacked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MagiskActionButton(
                        action = primaryAction,
                        modifier = Modifier.fillMaxWidth()
                    )
                    secondaryAction?.let {
                        MagiskActionButton(
                            action = it,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MagiskActionButton(
                        action = primaryAction,
                        modifier = Modifier.weight(1f)
                    )
                    secondaryAction?.let {
                        MagiskActionButton(
                            action = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
