@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    var showHelp by remember { mutableStateOf(false) }
    // Re-read the system grant + chosen-app count every time we return here (e.g. from the
    // system access screen or the app picker), so the setup rows below stay accurate.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    // Enabling without access is pointless, so flipping the master toggle on jumps straight to the
    // system access screen; the access row then reflects the result on return.
    val onToggleCapture: (Boolean) -> Unit = { on ->
        vm.setCaptureEnabled(on)
        if (on && !state.notificationAccessGranted) vm.openNotificationAccessSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "How auto-capture works")
                    }
                },
            )
        },
    ) { padding ->
        if (showHelp) {
            ModalBottomSheet(onDismissRequest = { showHelp = false }) { HowItWorksSheet() }
        }

        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Log expenses automatically",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Expense Tracker turns payment alerts from your chosen apps into expenses in your " +
                        "inbox — you confirm each one, and nothing leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // One grouped card: the master switch, then the two setup steps it depends on.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Auto-capture") },
                        supportingContent = { Text(if (state.captureEnabled) "On" else "Off") },
                        trailingContent = {
                            Switch(checked = state.captureEnabled, onCheckedChange = onToggleCapture)
                        },
                        modifier = Modifier.clickable { onToggleCapture(!state.captureEnabled) },
                    )

                    HorizontalDivider()

                    val (accessText, accessColor) = when {
                        state.notificationAccessGranted -> "Granted" to MaterialTheme.colorScheme.primary
                        state.captureEnabled -> "Required — tap to allow" to MaterialTheme.colorScheme.error
                        else -> "Not granted" to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    SetupRow(
                        headline = "Notification access",
                        supporting = accessText,
                        supportingColor = accessColor,
                        warn = false,
                        enabled = state.captureEnabled,
                        onClick = vm::openNotificationAccessSettings,
                    )

                    HorizontalDivider()

                    val noApps = state.captureEnabled && state.captureAppCount == 0
                    SetupRow(
                        headline = "Apps to monitor",
                        supporting = when {
                            noApps -> "None chosen yet — tap to pick the apps to watch"
                            state.captureAppCount > 0 ->
                                "${state.captureAppCount} app${if (state.captureAppCount == 1) "" else "s"} selected"
                            else -> "Choose which apps' alerts to watch"
                        },
                        supportingColor = if (noApps) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        warn = noApps,
                        enabled = state.captureEnabled,
                        onClick = onOpenCaptureApps,
                    )
                }
            }
        }
    }
}

/**
 * A tappable setup row inside the card. When [warn] is set (a required step the user hasn't done
 * yet) the row is tinted with the error container so it stands out without a separate alert block.
 * When [enabled] is false (auto-capture off, so the step is irrelevant) the row is greyed out and
 * non-interactive.
 */
@Composable
private fun SetupRow(
    headline: String,
    supporting: String,
    supportingColor: Color,
    warn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = if (warn) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
            headlineColor = when {
                !enabled -> disabled
                warn -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
        ),
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting, color = if (enabled) supportingColor else disabled) },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = when {
                    !enabled -> disabled
                    warn -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

/**
 * The "how/why/privacy" explainer, shown in a bottom sheet from the app bar's info icon — mirrors
 * the picker's help sheet so both capture screens reveal detail the same way.
 */
@Composable
private fun HowItWorksSheet() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "How auto-capture works",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
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

@Composable
private fun InfoLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
