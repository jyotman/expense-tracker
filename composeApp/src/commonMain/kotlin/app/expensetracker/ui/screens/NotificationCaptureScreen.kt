@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    // Re-read the system grant + chosen-app count every time we return here (e.g. from the
    // system access screen or the app picker), so the warning below stays accurate.
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
                "Turn the payment alerts from your bank and wallet apps into expenses you can review — " +
                    "nothing is saved without your tap, and nothing leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HowItWorksCard()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enable auto-capture", style = MaterialTheme.typography.titleMedium)
                        // Switch reflects the user's intent (captureEnabled) so it can always be
                        // toggled off; a missing access grant is surfaced by the status below.
                        Switch(
                            checked = state.captureEnabled,
                            onCheckedChange = { on ->
                                vm.setCaptureEnabled(on)
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

            AppsToMonitorCard(
                captureEnabled = state.captureEnabled,
                appCount = state.captureAppCount,
                onClick = onOpenCaptureApps,
            )
        }
    }
}

/** Collapsible explainer — keeps the longer "how/why/privacy" copy off the screen until tapped. */
@Composable
private fun HowItWorksCard() {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                headlineContent = { Text("How it works") },
                trailingContent = {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                },
                modifier = Modifier.clickable { expanded = !expanded },
            )
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InfoLine(
                        "Expense Tracker reads the payment alerts your chosen apps post, picks out the amount " +
                            "and merchant right on your phone, and drops each one in your inbox. You tap to " +
                            "confirm, and a category is suggested for you on the spot.",
                    )
                    InfoLine(
                        "It uses a sensitive permission, so it only looks at notifications that read like a " +
                            "payment and ignores everything else. Nothing is ever sent off your device.",
                    )
                    InfoLine(
                        "Even if Expense Tracker's own alerts are turned off, every detected payment still " +
                            "lands in your in-app inbox — so nothing is missed.",
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The "Apps to monitor" entry point. Turns into a highlighted warning when auto-capture is on but
 * the user hasn't chosen any apps yet — otherwise nothing would ever be captured and they might not
 * realise they need to opt apps in. Shows the chosen count once at least one app is selected.
 */
@Composable
private fun AppsToMonitorCard(captureEnabled: Boolean, appCount: Int, onClick: () -> Unit) {
    val warn = captureEnabled && appCount == 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (warn) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = if (warn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                supportingColor = if (warn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            headlineContent = { Text("Apps to monitor") },
            supportingContent = {
                Text(
                    when {
                        warn -> "⚠ No apps chosen yet — pick the apps whose payment alerts you want captured, or nothing will be tracked."
                        appCount > 0 -> "$appCount app${if (appCount == 1) "" else "s"} selected"
                        else -> "Choose which apps' notifications to watch"
                    },
                )
            },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = if (warn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}
