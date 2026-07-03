package com.topjohnwu.magisk.ui.superuser

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.arch.UIActivity
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.ui.component.MagiskChipOption
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskExpandableListItem
import com.topjohnwu.magisk.ui.component.MagiskFilterChipRow
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskLoadingState
import com.topjohnwu.magisk.ui.component.MagiskSearchField
import com.topjohnwu.magisk.ui.component.MagiskSwitchItem
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.ui.component.card.MagiskActionCard
import com.topjohnwu.magisk.ui.motion.MagiskAnimatedVisibility
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MagiskMotionEngine
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.superuser.SuperuserViewModel
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SuperuserScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SuperuserViewModel = viewModel(factory = SuperuserViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    fun withSuAuthentication(action: () -> Unit) {
        if (!Config.suAuth) {
            action()
            return
        }
        val uiActivity = context as? UIActivity<*> ?: return
        uiActivity.withAuthentication { granted ->
            if (granted) {
                action()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { text ->
            val messageString = when (text) {
                is UiText.Plain -> text.value
                is UiText.Resource -> context.getString(text.resId, *text.args.toTypedArray())
            }
            SystemToastManager.show(context, messageString)
        }
    }

    if (state.loading) {
        MagiskLoadingState(modifier = modifier)
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            MagiskAnimatedVisibility(visible = state.searchVisible) {
                MagiskSearchField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = stringResource(CoreR.string.hide_search),
                    clearContentDescription = stringResource(CoreR.string.clear_search),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.filteredItems.isEmpty()) {
                MagiskEmptyState(
                    title = stringResource(CoreR.string.superuser_policy_none),
                    icon = Icons.Rounded.Security,
                    modifier = Modifier.weight(1f)
                )
            } else {
                MagiskLazyContent(modifier = Modifier.weight(1f)) {
                    items(
                        count = state.filteredItems.size, key = { index ->
                            val item = state.filteredItems[index]
                            "${item.uid}:${item.packageName}"
                        }) { index ->
                        val item = state.filteredItems[index]
                        val isActive =
                            item.policy == SuPolicy.ALLOW || item.policy == SuPolicy.RESTRICT
                        val alphaAnimation = MagiskMotionEngine.tweenSpec<Float>(
                            MagiskMotionDuration.Short
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0.65f,
                            animationSpec = alphaAnimation,
                            label = "SuCardAlpha"
                        )
                        MagiskExpandableListItem(
                            title = item.title,
                            subtitle = item.packageName,
                            expanded = item.expanded,
                            onClick = { viewModel.toggleExpanded(item.uid, item.packageName) },
                            modifier = Modifier.graphicsLayer(alpha = alpha),
                            leadingContent = {
                                AppIcon(
                                    packageName = item.iconPackageName ?: item.packageName,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            showArrow = false,
                            headerTrailingContent = {
                                if (item.showSlider) {
                                    MagiskTriStateSwitch(
                                        policy = item.policy,
                                        onPolicyChange = { newPolicy ->
                                            withSuAuthentication {
                                                viewModel.setPolicy(item.uid, newPolicy)
                                            }
                                        }
                                    )
                                } else {
                                    Switch(
                                        checked = item.policy == SuPolicy.ALLOW,
                                        onCheckedChange = { checked ->
                                            withSuAuthentication {
                                                viewModel.setPolicy(
                                                    item.uid,
                                                    if (checked) SuPolicy.ALLOW else SuPolicy.DENY
                                                )
                                            }
                                        }
                                    )
                                }
                            }) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // 1. Policy selection
                                val options = remember(item.showSlider) {
                                    val list = mutableListOf(
                                        MagiskChipOption(
                                            SuPolicy.DENY, context.getString(CoreR.string.deny)
                                        ), MagiskChipOption(
                                            SuPolicy.ALLOW, context.getString(CoreR.string.grant)
                                        )
                                    )
                                    if (item.showSlider) {
                                        list.add(
                                            1, MagiskChipOption(
                                                SuPolicy.RESTRICT,
                                                context.getString(CoreR.string.restrict)
                                            )
                                        )
                                    }
                                    list
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = stringResource(CoreR.string.superuser_setting),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MagiskFilterChipRow(
                                        options = options,
                                        selected = item.policy,
                                        onSelected = { newPolicy ->
                                            withSuAuthentication {
                                                viewModel.setPolicy(item.uid, newPolicy)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 2. Notification Toggle
                                MagiskSwitchItem(
                                    title = stringResource(CoreR.string.superuser_toggle_notification),
                                    checked = item.notification,
                                    onCheckedChange = { viewModel.toggleNotify(item) },
                                    leadingIcon = Icons.Rounded.Notifications
                                )

                                // 3. Logging Toggle
                                MagiskSwitchItem(
                                    title = stringResource(CoreR.string.superuser_toggle_logging),
                                    checked = item.logging,
                                    onCheckedChange = { viewModel.toggleLog(item) },
                                    leadingIcon = Icons.Rounded.History
                                )

                                // 4. Revoke Button
                                Button(
                                    onClick = {
                                        withSuAuthentication {
                                            viewModel.revoke(item)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(CoreR.string.superuser_toggle_revoke))
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
private fun SuperuserLogsEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskActionCard(
        onClick = onClick,
        modifier = modifier,
        minHeight = 64.dp
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(CoreR.string.superuser_logs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(CoreR.string.logs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(22.dp)
        )
    }
}

@Composable
fun SuperuserTopBarActions(
    searchVisible: Boolean,
    onToggleSearch: () -> Unit,
    onLogsClick: () -> Unit
) {
    MagiskTopBarIconButton(
        icon = if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
        contentDescription = stringResource(CoreR.string.hide_search),
        onClick = onToggleSearch
    )
    MagiskTopBarIconButton(
        icon = Icons.Rounded.History,
        contentDescription = stringResource(CoreR.string.superuser_logs),
        onClick = onLogsClick
    )
}

@Composable
fun MagiskTriStateSwitch(
    policy: Int,
    onPolicyChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetOffset = when (policy) {
        SuPolicy.DENY -> 0.dp
        SuPolicy.RESTRICT -> 22.dp
        SuPolicy.ALLOW -> 44.dp
        else -> 0.dp
    }
    val offsetAnimation = MagiskMotionEngine.tweenSpec<androidx.compose.ui.unit.Dp>(
        MagiskMotionDuration.Short
    )
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = offsetAnimation,
        label = "ThumbOffset"
    )

    val trackColor = when (policy) {
        SuPolicy.DENY -> MaterialTheme.colorScheme.errorContainer
        SuPolicy.RESTRICT -> MaterialTheme.colorScheme.tertiaryContainer
        SuPolicy.ALLOW -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val thumbColor = when (policy) {
        SuPolicy.DENY -> MaterialTheme.colorScheme.error
        SuPolicy.RESTRICT -> MaterialTheme.colorScheme.tertiary
        SuPolicy.ALLOW -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val thumbContentColor = when (policy) {
        SuPolicy.DENY -> MaterialTheme.colorScheme.onError
        SuPolicy.RESTRICT -> MaterialTheme.colorScheme.onTertiary
        SuPolicy.ALLOW -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.surface
    }

    val thumbIcon = when (policy) {
        SuPolicy.DENY -> Icons.Rounded.Close
        SuPolicy.RESTRICT -> Icons.Rounded.Remove
        SuPolicy.ALLOW -> Icons.Rounded.Check
        else -> Icons.Rounded.Close
    }

    Box(
        modifier = modifier
            .size(width = 76.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(4.dp)
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = thumbIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = thumbContentColor
            )
        }

        // Clickable regions
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPolicyChange(SuPolicy.DENY) }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPolicyChange(SuPolicy.RESTRICT) }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPolicyChange(SuPolicy.ALLOW) }
            )
        }
    }
}
