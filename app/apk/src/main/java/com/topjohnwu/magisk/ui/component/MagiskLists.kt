package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.motion.MagiskAnimatedVisibility

object MagiskListItemDefaults {
    val ContainerColor: Color
        @Composable get() = Color.Transparent

    val SelectedContainerColor: Color
        @Composable get() = MagiskComponentDefaults.SelectedContainer
}

@Composable
fun MagiskListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    clickCenterOnly: Boolean = false,
    shape: Shape = MagiskComponentDefaults.ControlShape,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick == null || clickCenterOnly) {
        modifier
    } else {
        modifier
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }

    val centerClickModifier = if (clickCenterOnly && onClick != null) {
        Modifier
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    ListItem(
        modifier = clickModifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        leadingContent = leadingContent ?: leadingIcon?.let {
            { MagiskIconBadge(icon = it) }
        },
        trailingContent = trailingContent?.let {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = it
                )
            }
        },
        overlineContent = null,
        supportingContent = if (!clickCenterOnly) {
            subtitle?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MagiskListItemDefaults.SelectedContainerColor
            } else {
                MagiskListItemDefaults.ContainerColor
            },
            headlineColor = if (enabled) MagiskComponentDefaults.PrimaryText else MaterialTheme.colorScheme.outline,
            supportingColor = MagiskComponentDefaults.SecondaryText
        ),
        elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
        content = {
            Column(modifier = centerClickModifier) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (clickCenterOnly && subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
    )
}

@Composable
fun MagiskSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    MagiskListItem(
        title = title,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        enabled = enabled,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                } else {
                    null
                })
        })
}

@Composable
fun MagiskExpandableListItem(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    clickCenterOnly: Boolean = false,
    showArrow: Boolean = true,
    headerTrailingContent: (@Composable RowScope.() -> Unit)? = null,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    val headerShape = if (expanded) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    } else {
        MagiskComponentDefaults.CardShape
    }

    MagiskCard(
        modifier = modifier.fillMaxWidth(),
        shape = MagiskComponentDefaults.CardShape,
        containerColor = MagiskComponentDefaults.CardContainer,
        border = MagiskComponentDefaults.CardBorder,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MagiskListItem(
                title = title,
                subtitle = subtitle,
                leadingIcon = leadingIcon,
                leadingContent = leadingContent,
                onClick = onClick,
                clickCenterOnly = clickCenterOnly,
                shape = headerShape,
                trailingContent = {
                    headerTrailingContent?.invoke(this)
                    if (showArrow) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MagiskComponentDefaults.SecondaryIconTint
                        )
                    }
                })
            MagiskAnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = expandedContent
                )
            }
        }
    }
}

@Composable
fun MagiskLanguageItem(
    name: String,
    langCode: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = if (selected) {
            MagiskComponentDefaults.PrimaryIconTint.copy(alpha = 0.12f)
        } else {
            MagiskComponentDefaults.CardContainer
        },
        border = if (selected) {
            BorderStroke(1.dp, MagiskComponentDefaults.PrimaryIconTint)
        } else {
            MagiskComponentDefaults.CardBorder
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular leading badge with 2/3-letter language code (Compact: 36.dp)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (selected) {
                                listOf(
                                    MagiskComponentDefaults.PrimaryIconTint,
                                    MagiskComponentDefaults.PrimaryIconTint.copy(alpha = 0.8f)
                                )
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = langCode,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MagiskComponentDefaults.SecondaryText
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content: Name and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MagiskComponentDefaults.PrimaryIconTint
                    } else {
                        MagiskComponentDefaults.PrimaryText
                    }
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MagiskComponentDefaults.SecondaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Custom selector badge indicator (Compact: 20.dp)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (selected) MagiskComponentDefaults.PrimaryIconTint else Color.Transparent,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (selected) MagiskComponentDefaults.PrimaryIconTint else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
