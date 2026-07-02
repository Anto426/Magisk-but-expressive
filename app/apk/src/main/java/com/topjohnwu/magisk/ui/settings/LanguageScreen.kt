package com.topjohnwu.magisk.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.navigation.AppRoute
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskSearchField
import com.topjohnwu.magisk.ui.component.MagiskSettingsGroup
import com.topjohnwu.magisk.ui.component.MagiskSettingsItemContent
import com.topjohnwu.magisk.ui.component.MagiskSettingsListItem
import com.topjohnwu.magisk.ui.component.MagiskTopBarIconButton
import com.topjohnwu.magisk.viewmodel.settings.SettingsViewModel
import java.util.Locale
import com.topjohnwu.magisk.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigate: (AppRoute) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val languages = remember { LocaleSetting.available.names.toList() }
    val tags = remember { LocaleSetting.available.tags.toList() }

    val filteredIndices = remember(state.languageSearchQuery, languages) {
        if (state.languageSearchQuery.isBlank()) {
            languages.indices.toList()
        } else {
            languages.indices.filter {
                languages[it].contains(state.languageSearchQuery, ignoreCase = true) ||
                        tags[it].contains(state.languageSearchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = state.languageSearchVisible) {
            MagiskSearchField(
                value = state.languageSearchQuery,
                onValueChange = viewModel::setLanguageSearchQuery,
                placeholder = stringResource(CoreR.string.hide_search),
                clearContentDescription = stringResource(CoreR.string.clear_search),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        MagiskLazyContent(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                MagiskSettingsGroup(
                    title = stringResource(CoreR.string.language),
                    icon = Icons.Rounded.Language,
                    items = filteredIndices.map { index ->
                        val name = languages[index]
                        val selected = state.languageIndex == index
                        val tag = tags.getOrNull(index).orEmpty()
                        val isDefault = tag.isEmpty()

                        val langCode = if (isDefault) "SYS"
                        else tag.substringBefore("-").substringBefore("_").take(3).uppercase()

                        val subtitleText = if (isDefault) "Default system language"
                        else {
                            val locale = Locale.forLanguageTag(tag)
                            val englishName = locale.getDisplayName(Locale.ENGLISH)
                                .replaceFirstChar { it.uppercase() }
                            "$englishName ($tag)"
                        }

                        val item: MagiskSettingsItemContent = {
                            MagiskSettingsListItem(
                                title = name,
                                subtitle = subtitleText,
                                selected = selected,
                                onClick = { viewModel.setLanguageByIndex(index) },
                                leadingContent = {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = langCode,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                        item
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageTopBarActions(
    searchVisible: Boolean,
    onToggleSearch: () -> Unit
) {
    MagiskTopBarIconButton(
        icon = if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
        contentDescription = stringResource(CoreR.string.hide_search),
        onClick = onToggleSearch
    )
}
