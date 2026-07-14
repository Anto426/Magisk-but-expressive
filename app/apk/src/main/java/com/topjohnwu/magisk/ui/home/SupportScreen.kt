package com.topjohnwu.magisk.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.topjohnwu.magisk.arch.UiEffect
import com.topjohnwu.magisk.arch.UiText
import com.topjohnwu.magisk.utils.openExternalUri
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskBottomSheet
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskListItem
import com.topjohnwu.magisk.ui.component.MagiskLoader
import com.topjohnwu.magisk.ui.component.MagiskSection
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.component.card.MagiskCardAction
import com.topjohnwu.magisk.ui.component.card.MagiskCardActionStyle
import com.topjohnwu.magisk.ui.component.card.MagiskContributorCard
import com.topjohnwu.magisk.ui.component.card.MagiskProfileCard
import com.topjohnwu.magisk.ui.component.card.MagiskSupportCard
import com.topjohnwu.magisk.view.SystemToastManager
import com.topjohnwu.magisk.viewmodel.support.Contributor
import com.topjohnwu.magisk.viewmodel.support.SupportViewModel
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SupportScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SupportViewModel = viewModel(factory = SupportViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedContributorForLinks by remember { mutableStateOf<Contributor?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.OpenUri -> {
                    context.openExternalUri(effect.uri)
                }
                else -> {}
            }
        }
    }

    if (selectedContributorForLinks != null) {
        val contributor = selectedContributorForLinks!!
        MagiskBottomSheet(
            onDismissRequest = { selectedContributorForLinks = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = contributor.avatarUrl,
                        contentDescription = contributor.login,
                        modifier = Modifier
                            .size(44.dp)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.25f
                                ),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = contributor.login,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MagiskComponentDefaults.PrimaryText
                    )
                }

                HorizontalDivider(color = MagiskComponentDefaults.DividerColor)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MagiskListItem(
                        title = stringResource(CoreR.string.github),
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(MagiskComponentDefaults.IconBadgeSize),
                                shape = MagiskComponentDefaults.ControlShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_github),
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        onClick = {
                            viewModel.openLink(contributor.htmlUrl)
                            selectedContributorForLinks = null
                        }
                    )

                    contributor.links.forEach { link ->
                        if (link.url != contributor.htmlUrl) {
                            MagiskListItem(
                                title = stringResource(link.labelRes),
                                leadingContent = {
                                    Surface(
                                        modifier = Modifier.size(MagiskComponentDefaults.IconBadgeSize),
                                        shape = MagiskComponentDefaults.ControlShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(link.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(19.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.openLink(link.url)
                                    selectedContributorForLinks = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    MagiskLazyContent(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = MagiskComponentDefaults.ScreenHorizontalPadding,
            top = 12.dp,
            end = MagiskComponentDefaults.ScreenHorizontalPadding,
            bottom = 132.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 0. Support Intro Hero Card ---
        item {
            MagiskCard(
                shape = MagiskComponentDefaults.CardShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(CoreR.string.support_hero_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(CoreR.string.support_hero_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MagiskComponentDefaults.SecondaryText
                        )
                    }
                }
            }
        }

        // --- 1. Magisk-but-expressive (MBE) Section ---
        item {
            MagiskSection(
                title = stringResource(CoreR.string.mbe_section_title),
                icon = Icons.Rounded.Terminal
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // MBE Support Card
                    MagiskSupportCard(
                        title = stringResource(CoreR.string.donate_to_anto),
                        message = stringResource(CoreR.string.mbe_support_content),
                        primaryAction = MagiskCardAction(
                            text = stringResource(CoreR.string.donate),
                            onClick = { viewModel.openLink(viewModel.mbeDonateUrl) },
                            icon = Icons.Rounded.Favorite
                        ),
                        secondaryAction = MagiskCardAction(
                            text = stringResource(CoreR.string.home_item_source),
                            onClick = { viewModel.openLink(viewModel.mbeSourceUrl) },
                            style = MagiskCardActionStyle.Secondary
                        ),
                        actionsStacked = false
                    )

                    // Prominent Maintainer Profile Card
                    val mbeMaintainer = state.contributors.firstOrNull { it.login.lowercase(Locale.US) == "anto426" }
                    mbeMaintainer?.let { contributor ->
                        MagiskProfileCard(
                            title = contributor.login,
                            subtitle = stringResource(CoreR.string.mbe_maintainer_label),
                            leadingContent = {
                                AsyncImage(
                                    model = contributor.avatarUrl,
                                    contentDescription = contributor.login,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.25f
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            onClick = { selectedContributorForLinks = contributor },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MagiskComponentDefaults.SecondaryIconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- 2. Official Magisk Project Section ---
        item {
            MagiskSection(
                title = stringResource(CoreR.string.official_magisk_section_title),
                icon = Icons.Rounded.People
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Official Magisk Support Card
                    MagiskSupportCard(
                        title = stringResource(CoreR.string.donate_to_topjohnwu),
                        message = stringResource(CoreR.string.home_support_content),
                        primaryAction = MagiskCardAction(
                            text = stringResource(CoreR.string.donate),
                            onClick = { viewModel.openLink(viewModel.officialDonateUrl) },
                            icon = Icons.Rounded.Favorite
                        ),
                        secondaryAction = MagiskCardAction(
                            text = stringResource(CoreR.string.documents),
                            onClick = { viewModel.openLink(viewModel.officialDocsUrl) },
                            style = MagiskCardActionStyle.Secondary
                        ),
                        actionsStacked = false
                    )

                    // Prominent Creator Profile Card
                    val topjohnwuContributor = state.contributors.firstOrNull { it.login.lowercase(Locale.US) == "topjohnwu" }
                    topjohnwuContributor?.let { contributor ->
                        MagiskProfileCard(
                            title = contributor.login,
                            subtitle = stringResource(CoreR.string.original_magisk_maintainer_label),
                            leadingContent = {
                                AsyncImage(
                                    model = contributor.avatarUrl,
                                    contentDescription = contributor.login,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.25f
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            onClick = { selectedContributorForLinks = contributor },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MagiskComponentDefaults.SecondaryIconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }

                    // Official Magisk Contributors List
                    if (state.contributorsLoading) {
                        MagiskLoader()
                    } else {
                        val otherContributors = state.contributors.filter {
                            val loginLower = it.login.lowercase(Locale.US)
                            loginLower != "anto426" && loginLower != "topjohnwu"
                        }
                        otherContributors.chunked(2).forEach { rowContributors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowContributors.forEachIndexed { index, contributor ->
                                    MagiskContributorCard(
                                        contributor = contributor,
                                        onClick = { selectedContributorForLinks = contributor },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowContributors.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
