package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MagiskLazyContent(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(
        start = MagiskComponentDefaults.ScreenHorizontalPadding,
        top = MagiskComponentDefaults.ScreenVerticalPadding,
        end = MagiskComponentDefaults.ScreenHorizontalPadding,
        bottom = MagiskComponentDefaults.ScreenBottomPadding
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(MagiskComponentDefaults.ItemSpacing),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun MagiskContentColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = MagiskComponentDefaults.ScreenHorizontalPadding,
        top = MagiskComponentDefaults.ScreenVerticalPadding,
        end = MagiskComponentDefaults.ScreenHorizontalPadding,
        bottom = MagiskComponentDefaults.ScreenBottomPadding
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(MagiskComponentDefaults.ItemSpacing),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}
