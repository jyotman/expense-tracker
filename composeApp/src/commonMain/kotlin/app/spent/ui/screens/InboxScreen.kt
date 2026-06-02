@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)

package app.spent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.spent.ServiceLocator
import app.spent.data.CapturedNotificationItem
import app.spent.data.Money
import app.spent.ui.components.EmptyState
import app.spent.util.DateFormat
import kotlin.time.Clock

@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onOpenCaptured: (Long) -> Unit,
) {
    val vm: app.spent.viewmodel.InboxViewModel = viewModel { app.spent.viewmodel.InboxViewModel() }
    val items by vm.items.collectAsState()
    val symbol = remember { ServiceLocator.settings.currencySymbol }
    val now = remember { Clock.System.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detected") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (items.any { !it.isRead }) {
                        TextButton(onClick = vm::markAllRead) { Text("Mark all read") }
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = "Nothing detected yet",
                subtitle = "Payment notifications from your bank & wallet apps will appear here to review.",
                modifier = Modifier.padding(padding).padding(top = 32.dp),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(items, key = { it.id }) { item ->
                InboxRow(item, symbol, now, onClick = { onOpenCaptured(item.id) })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun InboxRow(item: CapturedNotificationItem, symbol: String, now: kotlin.time.Instant, onClick: () -> Unit) {
    val saved = item.expenseId != null
    val rowBg = if (!item.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(rowBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!item.isRead) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        } else {
            Box(Modifier.size(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = item.merchant?.takeIf { it.isNotBlank() } ?: item.appLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
            )
            Text(
                text = "${item.appLabel} · ${DateFormat.relativeDay(item.postedAt, now)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.amountMinor?.let { Money.format(it, symbol) } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (saved) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text("Added", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text("Tap to add", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
