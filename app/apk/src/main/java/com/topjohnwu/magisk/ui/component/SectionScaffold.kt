package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.ui.animation.MagiskMotion

object SectionDefaults {
    val expressiveCardShape: Shape
        @Composable get() = MagiskUiDefaults.OrganicShape
}

@Composable
fun ExpressiveSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconContainerSize: Dp = 32.dp,
    iconPadding: Dp = 6.dp,
    iconShape: Shape = MaterialTheme.shapes.extraSmall,
    iconContainerAlpha: Float = 0.7f,
    cardShape: Shape = SectionDefaults.expressiveCardShape,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.animateContentSize(MagiskMotion.contentSizeSpring())
    ) {
        SectionHeader(
            title = title,
            icon = icon,
            iconContainerSize = iconContainerSize,
            iconPadding = iconPadding,
            iconShape = iconShape,
            iconContainerAlpha = iconContainerAlpha
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = MagiskUiDefaults.CardElevation)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconContainerSize: Dp = 38.dp,
    iconPadding: Dp = 9.dp,
    iconShape: Shape = MaterialTheme.shapes.extraSmall,
    iconContainerAlpha: Float = 0.85f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(contentPadding)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = iconContainerAlpha),
            shape = iconShape,
            modifier = Modifier.size(iconContainerSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.padding(iconPadding),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
