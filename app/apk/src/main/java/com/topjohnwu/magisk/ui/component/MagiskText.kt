package com.topjohnwu.magisk.ui.component

import android.widget.TextView
import android.text.method.LinkMovementMethod
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.di.ServiceLocator

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> stringResource(resId, *args.toTypedArray())
    }
}

@Composable
fun stringResource(text: UiText): String = text.asString()

@Composable
fun MagiskMarkdown(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                movementMethod = LinkMovementMethod.getInstance()
                setTextIsSelectable(true)
                textSize = 14f
                setLineSpacing(0f, 1.12f)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            ServiceLocator.markwon.setMarkdown(textView, markdown)
        }
    )
}

@Composable
fun MagiskIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = MagiskComponentDefaults.IconBadgeSize,
    iconSize: Dp = 19.dp,
    shape: Shape = MagiskComponentDefaults.ControlShape,
    containerColor: Color = MagiskComponentDefaults.AccentContainer,
    iconTint: Color = MagiskComponentDefaults.AccentContent
) {
    Surface(
        modifier = modifier.size(size), shape = shape, color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
    }
}

@Composable
fun MagiskInfoPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MagiskComponentDefaults.AccentContent,
    containerColor: Color = MagiskComponentDefaults.AccentContainer
) {
    Surface(
        modifier = modifier, shape = MagiskComponentDefaults.PillShape, color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MagiskInfoPill(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    color: Color = MagiskComponentDefaults.AccentContent,
    containerColor: Color = MagiskComponentDefaults.AccentContainer
) {
    MagiskInfoPill(
        text = stringResource(text),
        modifier = modifier,
        color = color,
        containerColor = containerColor
    )
}

@Composable
fun MagiskStatusDot(
    color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp
) {
    Badge(
        modifier = modifier.size(size), containerColor = color
    )
}
