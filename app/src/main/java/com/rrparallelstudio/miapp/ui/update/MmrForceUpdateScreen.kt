package com.rrparallelstudio.miapp.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.platform.MmrPlayStoreIntents
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton

@Composable
fun MmrForceUpdateScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.force_update_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.force_update_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        MmrPrimaryActionButton(
            text = stringResource(R.string.force_update_open_play),
            onClick = {
                MmrPlayStoreIntents.openAppListing(
                    context,
                    context.packageName,
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
