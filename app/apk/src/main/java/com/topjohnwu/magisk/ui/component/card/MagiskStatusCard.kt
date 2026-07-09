package com.topjohnwu.magisk.ui.component.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskIconBadge
import com.topjohnwu.magisk.ui.component.MagiskStatusDot

data class MagiskStatusMetric(
    val label: String,
    val value: String
)

data class MagiskCardAction(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val style: MagiskCardActionStyle = MagiskCardActionStyle.Primary
)

enum class MagiskCardActionStyle {
    Primary,
    Secondary,
    Destructive
}

@Composable
fun MagiskStatusCard(
    title: String,
    statusText: String,
    statusColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MagiskComponentDefaults.CardShape,
    containerColor: Color = MagiskComponentDefaults.CardContainer,
    contentColor: Color = MagiskComponentDefaults.PrimaryText,
    supportingContentColor: Color = MagiskComponentDefaults.SecondaryText,
    border: BorderStroke? = MagiskComponentDefaults.CardBorder,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    metrics: List<MagiskStatusMetric> = emptyList(),
    primaryAction: MagiskCardAction? = null,
    secondaryAction: MagiskCardAction? = null,
    actionsStacked: Boolean = false
) {
    MagiskCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        border = border,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MagiskIconBadge(
                icon = icon,
                containerColor = iconContainerColor,
                iconTint = iconTint
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MagiskStatusDot(color = statusColor)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (metrics.isNotEmpty()) {
            HorizontalDivider(color = MagiskComponentDefaults.DividerColor)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                metrics.forEach { metric ->
                    MagiskMetricBlock(
                        metric = metric,
                        modifier = Modifier.fillMaxWidth(),
                        contentColor = contentColor,
                        supportingContentColor = supportingContentColor,
                        accentColor = statusColor
                    )
                }
            }
        }

        if (primaryAction != null || secondaryAction != null) {
            if (actionsStacked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    primaryAction?.let {
                        MagiskActionButton(
                            action = it,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    primaryAction?.let {
                        MagiskActionButton(
                            action = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
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

@Composable
fun MagiskActionButton(
    action: MagiskCardAction,
    modifier: Modifier = Modifier,
    alignContentStart: Boolean = false
) {
    when (action.style) {
        MagiskCardActionStyle.Primary -> {
            Button(
                onClick = action.onClick,
                modifier = modifier.heightIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                MagiskActionButtonContent(action, alignContentStart)
            }
        }

        MagiskCardActionStyle.Secondary -> {
            OutlinedButton(
                onClick = action.onClick,
                modifier = modifier.heightIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                MagiskActionButtonContent(action, alignContentStart)
            }
        }

        MagiskCardActionStyle.Destructive -> {
            OutlinedButton(
                onClick = action.onClick,
                modifier = modifier.heightIn(min = 40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                MagiskActionButtonContent(action, alignContentStart)
            }
        }
    }
}

@Composable
private fun MagiskMetricBlock(
    metric: MagiskStatusMetric,
    modifier: Modifier = Modifier,
    contentColor: Color = MagiskComponentDefaults.PrimaryText,
    supportingContentColor: Color = MagiskComponentDefaults.SecondaryText,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 24.dp)
                .background(accentColor.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = supportingContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MagiskActionButtonContent(
    action: MagiskCardAction,
    alignContentStart: Boolean
) {
    Row(
        modifier = if (alignContentStart) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignContentStart) Arrangement.Start else Arrangement.Center
    ) {
        action.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = action.text,
            modifier = if (alignContentStart) Modifier.weight(1f, fill = false) else Modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
