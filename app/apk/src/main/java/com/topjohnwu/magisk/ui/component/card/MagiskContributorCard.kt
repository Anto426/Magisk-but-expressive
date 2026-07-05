package com.topjohnwu.magisk.ui.component.card

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.viewmodel.support.Contributor
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun MagiskContributorCard(
    contributor: Contributor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskCard(
        modifier = modifier.height(132.dp),
        shape = MagiskComponentDefaults.CardShape,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = contributor.login,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MagiskComponentDefaults.PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(CoreR.string.home_contributor),
                style = MaterialTheme.typography.bodySmall,
                color = MagiskComponentDefaults.SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
