package com.topjohnwu.magisk.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun AppIcon(
    packageName: String, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }
    if (icon != null) {
        AsyncImage(
            model = icon, contentDescription = null, modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Android,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PolicyStatusBadge(policy: Int) {
    val text = when (policy) {
        SuPolicy.ALLOW -> stringResource(CoreR.string.grant)
        SuPolicy.DENY -> stringResource(CoreR.string.deny)
        SuPolicy.RESTRICT -> stringResource(CoreR.string.restrict)
        else -> stringResource(CoreR.string.deny)
    }
    val color = when (policy) {
        SuPolicy.ALLOW -> MaterialTheme.colorScheme.primary
        SuPolicy.DENY -> MaterialTheme.colorScheme.error
        SuPolicy.RESTRICT -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}
