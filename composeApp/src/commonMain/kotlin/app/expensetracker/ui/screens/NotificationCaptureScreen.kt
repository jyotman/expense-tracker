@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.viewmodel.SettingsViewModel

@Composable
fun NotificationCaptureScreen(onBack: () -> Unit, onOpenCaptureApps: () -> Unit) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel() }
    val state by vm.state.collectAsState()
    // Re-read the system grant every time we return to this screen (e.g. from system settings).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Log expenses automatically",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Expense Tracker watches the payment notifications your bank, card and wallet apps post " +
                    "(GPay, Revolut, etc.), reads the amount and merchant on-device, and drops each one " +
                    "in your inbox to review. Nothing is saved automatically and nothing leaves your phone — " +
                    "you tap to confirm, and the category is suggested for you on the spot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text("Enable auto-capture", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.captureEnabled,
                            onCheckedChange = { on ->
                                vm.setCaptureEnabled(on)
                                // Switching on does nothing until access is granted — take them there now.
                                if (on && !state.notificationAccessGranted) vm.openNotificationAccessSettings()
                            },
                        )
                    }
                    val (statusText, statusColor) = when {
                        !state.captureEnabled ->
                            "Turn on, then grant notification access below." to MaterialTheme.colorScheme.onSurfaceVariant
                        state.notificationAccessGranted ->
                            "✓ Notification access granted — you're all set." to MaterialTheme.colorScheme.primary
                        else ->
                            "⚠ Notification access not granted yet — tap the button below." to MaterialTheme.colorScheme.error
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
            }

            Button(
                onClick = vm::openNotificationAccessSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.notificationAccessGranted) "Manage notification access" else "Grant notification access")
            }

            Text(
                "This is a sensitive permission — Expense Tracker only inspects notifications that look like " +
                    "transactions and ignores everything else.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Don't see the heads-up alerts? Every detected payment is also saved to your in-app " +
                    "inbox, so nothing is missed even if Expense Tracker's own notifications are turned off.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Apps to monitor") },
                    supportingContent = { Text("Choose which apps' notifications to watch") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.clickable(onClick = onOpenCaptureApps),
                )
            }
        }
    }
}
