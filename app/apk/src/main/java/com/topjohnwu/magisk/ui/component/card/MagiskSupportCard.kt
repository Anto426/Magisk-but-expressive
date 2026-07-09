package com.topjohnwu.magisk.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
    actionsStacked: Boolean = false
) {
    MagiskCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        containerColor = MagiskComponentDefaults.CardContainer,
        border = MagiskComponentDefaults.CardBorder,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MagiskComponentDefaults.PrimaryText,
                maxLines = 1,
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

        if (actionsStacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MagiskActionButton(
                    action = primaryAction,
                    modifier = Modifier.fillMaxWidth(),
                    alignContentStart = true
                )
                secondaryAction?.let {
                    MagiskActionButton(
                        action = it,
                        modifier = Modifier.fillMaxWidth(),
                        alignContentStart = true
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
