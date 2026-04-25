package com.topjohnwu.magisk.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrowserUpdated
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.ui.component.MagiskCard
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.core.R as CoreR

@Composable
internal fun HomeStatusHeroCard(envActive: Boolean) {
    val isInstalled = envActive
    val primaryColor = if (isInstalled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val containerColor = if (isInstalled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val title = stringResource(
        id = if (isInstalled) CoreR.string.home_status_ready else CoreR.string.home_status_inactive
    )
    val subtitle = stringResource(
        id = if (isInstalled) CoreR.string.home_status_ready_subtitle else CoreR.string.home_status_inactive_subtitle
    )

    MagiskCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 120.dp,
            bottomStart = 120.dp,
            bottomEnd = 32.dp
        ),
        containerColor = containerColor,
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_magisk_outline),
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp, y = 30.dp)
                    .alpha(0.1f),
                tint = primaryColor
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isInstalled) Icons.Rounded.Verified else Icons.Rounded.GppMaybe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = primaryColor
                        )
                        Text(
                            text = stringResource(
                                id = if (isInstalled) {
                                    CoreR.string.home_state_up_to_date
                                } else {
                                    CoreR.string.home_state_inactive
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = primaryColor
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = primaryColor,
                    lineHeight = 38.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun HomeNotice(onHide: () -> Unit) {
    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MagiskUiDefaults.LargeShape,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(id = CoreR.string.home_notice_content),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onHide) {
                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun HomeMagiskCoreCard(
    magiskState: HomeViewModel.State,
    magiskInstalledVersion: String,
    onAction: () -> Unit
) {
    val isInstalled = magiskState != HomeViewModel.State.INVALID
    val containerColor = if (isInstalled) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isInstalled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    MagiskCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(
            topStart = 64.dp,
            bottomEnd = 80.dp,
            topEnd = 24.dp,
            bottomStart = 24.dp
        ),
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Box {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_magisk_outline),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .alpha(0.08f),
                tint = contentColor
            )

            Column(modifier = Modifier.padding(28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_magisk),
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(id = CoreR.string.magisk),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        HomeStatusBadge(magiskState)
                    }
                }
                Spacer(Modifier.height(28.dp))
                HomeBentoInfoGrid(
                    listOf(
                        stringResource(id = CoreR.string.home_installed_version) to magiskInstalledVersion,
                        stringResource(id = CoreR.string.zygisk) to stringResource(
                            id = if (Info.isZygiskEnabled) CoreR.string.yes else CoreR.string.no
                        ),
                        stringResource(id = CoreR.string.home_ramdisk) to stringResource(
                            id = if (Info.ramdisk) CoreR.string.yes else CoreR.string.no
                        )
                    ),
                    onContainerColor = if (isInstalled) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    } else null
                )
                Spacer(Modifier.height(28.dp))
                Button(
        onClick = onAction,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = MagiskUiDefaults.PillShape
                ) {
                    Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(
                            id = if (magiskState == HomeViewModel.State.OUTDATED) {
                                CoreR.string.update
                            } else {
                                CoreR.string.install
                            }
                        ).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeAppCard(
    appState: HomeViewModel.State,
    managerInstalledVersion: String,
    managerRemoteVersion: String,
    updateChannelName: String,
    packageName: String,
    isHidden: Boolean,
    onAction: () -> Unit,
    onHideRestore: () -> Unit
) {
    val isOutdated = appState == HomeViewModel.State.OUTDATED
    val containerColor = if (isOutdated) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isOutdated) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    MagiskCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(
            topStart = 24.dp,
            bottomEnd = 24.dp,
            topEnd = 80.dp,
            bottomStart = 80.dp
        ),
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.AppShortcut,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(id = CoreR.string.home_app_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    if (appState != HomeViewModel.State.INVALID && appState != HomeViewModel.State.LOADING) {
                        HomeStatusBadge(appState)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (Info.env.isActive) {
                    Surface(
                        onClick = onHideRestore,
                        shape = MagiskUiDefaults.PillShape,
                        color = if (isOutdated) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isOutdated) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeBentoInfoItem(
                    label = stringResource(id = CoreR.string.home_installed_version),
                    value = managerInstalledVersion,
                    modifier = Modifier.fillMaxWidth(),
                    onContainerColor = if (isOutdated) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else null
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeBentoInfoItem(
                        label = stringResource(id = CoreR.string.home_latest_version),
                        value = managerRemoteVersion,
                        modifier = Modifier.weight(1f),
                        onContainerColor = if (isOutdated) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else null
                    )
                    HomeBentoInfoItem(
                        label = stringResource(id = CoreR.string.home_channel),
                        value = updateChannelName,
                        modifier = Modifier.weight(1f),
                        onContainerColor = if (isOutdated) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else null
                    )
                }
                HomeBentoInfoItem(
                    label = stringResource(id = CoreR.string.home_package),
                    value = packageName,
                    modifier = Modifier.fillMaxWidth(),
                    valueMaxLines = 2,
                    onContainerColor = if (isOutdated) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else null
                )
            }
            if (appState != HomeViewModel.State.INVALID && appState != HomeViewModel.State.LOADING) {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = CircleShape,
                    colors = if (isOutdated) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = if (isOutdated) Icons.Rounded.BrowserUpdated else Icons.Rounded.SystemUpdateAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(
                            id = if (isOutdated) CoreR.string.update else CoreR.string.install
                        ).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeUninstallCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            bottomEnd = 16.dp,
            topEnd = 48.dp,
            bottomStart = 48.dp
        ),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.DeleteForever,
                        null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Text(
                stringResource(id = CoreR.string.home_uninstall_environment),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
internal fun HomeContributorsList(
    contributors: List<Contributor>,
    loading: Boolean,
    onOpen: (String) -> Unit
) {
    if (loading && contributors.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (contributors.isEmpty()) return

    val shapes = remember {
        listOf(
            MagiskUiDefaults.OrganicShape,
            MagiskUiDefaults.OrganicShapeReversed,
            MagiskUiDefaults.ExtraLargeShape
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        contributors.forEachIndexed { index, user ->
            val shape = shapes[index % shapes.size]
            MagiskCard(
                modifier = Modifier
                    .width(160.dp)
                    .height(200.dp)
                    .clip(shape)
                    .clickable { onOpen(user.htmlUrl) },
                shape = shape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        user.login,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        user.links.take(2).forEach { link ->
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { onOpen(link.url) },
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = link.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeSupportCard(onPatreon: () -> Unit, onPaypal: () -> Unit) {
    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MagiskUiDefaults.ExtraLargeShape
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = stringResource(id = CoreR.string.home_support_content),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSupportLinkButton(
                    label = stringResource(id = CoreR.string.patreon),
                    iconRes = CoreR.drawable.ic_patreon,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onPatreon,
                    modifier = Modifier.weight(1f)
                )
                HomeSupportLinkButton(
                    label = stringResource(id = CoreR.string.paypal),
                    iconRes = CoreR.drawable.ic_paypal,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onPaypal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeBentoInfoGrid(
    items: List<Pair<String, String>>,
    onContainerColor: Color? = null
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val first = items.firstOrNull() ?: return@Column
        HomeBentoInfoItem(
            label = first.first,
            value = first.second,
            modifier = Modifier.fillMaxWidth(),
            onContainerColor = onContainerColor
        )

        val remaining = items.drop(1)
        remaining.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (label, value) ->
                    HomeBentoInfoItem(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                        onContainerColor = onContainerColor
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeBentoInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 1,
    onContainerColor: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = onContainerColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = valueMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeStatusBadge(state: HomeViewModel.State) {
    val (text, color) = when (state) {
        HomeViewModel.State.UP_TO_DATE -> stringResource(id = CoreR.string.home_state_up_to_date) to Color(0xFF4CAF50)
        HomeViewModel.State.OUTDATED -> stringResource(id = CoreR.string.home_state_update_ready) to Color(0xFFFF9800)
        else -> stringResource(id = CoreR.string.home_state_inactive) to MaterialTheme.colorScheme.error
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun HomeSupportLinkButton(
    label: String,
    @DrawableRes iconRes: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(MagiskUiDefaults.PrimaryActionHeight),
        shape = MagiskUiDefaults.MediumShape,
        color = accentColor.copy(alpha = 0.12f),
        contentColor = accentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(id = iconRes), null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}
