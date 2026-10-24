package com.mmr.app.ui.state

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrPrimaryActionButton
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
fun MmrLoadingState(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MmrErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier =
        Modifier
            .fillMaxSize()
            .padding(MmrDimens.spaceLg),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(12.dp))
                MmrPrimaryActionButton(
                    text = "Retry",
                    onClick = onRetry,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
fun MmrEmptyState(
    message: String,
    modifier: Modifier =
        Modifier
            .fillMaxSize()
            .padding(MmrDimens.spaceLg),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MmrBrandColors.TextMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MmrStatusBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isError: Boolean = true,
) {
    val colors =
        if (isError) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        }
    val contentColor =
        if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

    Card(modifier = modifier, colors = colors) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
