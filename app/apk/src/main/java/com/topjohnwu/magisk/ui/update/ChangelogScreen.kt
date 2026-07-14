package com.topjohnwu.magisk.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.topjohnwu.magisk.core.update.UpdateManager
import com.topjohnwu.magisk.ui.component.MagiskEmptyState
import com.topjohnwu.magisk.ui.component.MagiskLazyContent
import com.topjohnwu.magisk.ui.component.MagiskMarkdown
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.core.R as CoreR

/** Displays release notes exclusively from UpdateManager's in-memory cache. */
@Composable
fun ChangelogScreen(
    moduleId: String?,
    title: String?,
    modifier: Modifier = Modifier
) {
    val snapshot by UpdateManager.state.collectAsStateWithLifecycle()
    val normalizedModuleId = moduleId?.takeIf(String::isNotBlank)
    val markdown = if (normalizedModuleId == null) {
        snapshot.app.changelog
    } else {
        snapshot.modules.updateFor(normalizedModuleId)?.changelog.orEmpty()
    }
    val displayTitle = title?.takeIf(String::isNotBlank) ?: if (normalizedModuleId == null) {
        snapshot.app.updateInfo?.version.orEmpty()
    } else {
        snapshot.modules.updateFor(normalizedModuleId)?.name.orEmpty()
    }

    if (markdown.isBlank()) {
        MagiskEmptyState(
            title = stringResource(CoreR.string.not_available),
            message = stringResource(CoreR.string.release_notes),
            icon = Icons.AutoMirrored.Rounded.Article,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    MagiskLazyContent(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MagiskCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (displayTitle.isNotBlank()) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    MagiskMarkdown(markdown = markdown)
                }
            }
        }
    }
}
