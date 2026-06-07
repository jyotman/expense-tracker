@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.data.CurrencyMeta
import app.expensetracker.platform.AiAvailability
import app.expensetracker.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenNotificationCapture: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel() }
    val state by vm.state.collectAsState()
    // Re-read the grant whenever we return here (e.g. from the system notification-access screen).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader("General")
        val currencyInfo = state.currencyCode?.let { CurrencyMeta.forCode(it) }
        ListItem(
            leadingContent = { Icon(Icons.Filled.Paid, null) },
            headlineContent = { Text("Currency") },
            supportingContent = { Text(currencyInfo?.displayName ?: "Choose your default currency") },
            trailingContent = { Text(state.currencySymbol, style = MaterialTheme.typography.titleMedium) },
            modifier = Modifier.clickable(onClick = onOpenCurrencyPicker),
        )
        ListItem(
            leadingContent = { Icon(Icons.Filled.Category, null) },
            headlineContent = { Text("Categories") },
            supportingContent = { Text("Add, edit or archive your categories") },
            trailingContent = { NavChevron() },
            modifier = Modifier.clickable(onClick = onOpenCategories),
        )
        ListItem(
            leadingContent = { Icon(Icons.Filled.Autorenew, null) },
            headlineContent = { Text("Recurring expenses") },
            supportingContent = { Text("Rent, subscriptions and other repeats") },
            trailingContent = { NavChevron() },
            modifier = Modifier.clickable(onClick = onOpenRecurring),
        )

        SectionHeader("Auto-capture")
        if (state.captureSupported) {
            ListItem(
                leadingContent = { Icon(Icons.Filled.NotificationsActive, null) },
                headlineContent = { Text("Capture from notifications") },
                supportingContent = {
                    Text(
                        if (state.captureEnabled && state.notificationAccessGranted) "On — reading bank & wallet alerts"
                        else if (state.captureEnabled) "On — needs notification access"
                        else "Off",
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenNotificationCapture),
            )
            val aiUnavailable = state.aiAvailability == AiAvailability.UNAVAILABLE
            val aiDownloading = state.aiAvailability == AiAvailability.DOWNLOADING
            ListItem(
                leadingContent = { Icon(Icons.Filled.AutoAwesome, null) },
                headlineContent = { Text("On-device AI parsing") },
                supportingContent = {
                    Column {
                        Text(
                            when {
                                state.aiChecking -> "Checking this device…"
                                aiUnavailable -> "Not available on this device — using built-in rules"
                                aiDownloading -> state.aiDownloadFraction?.let { "Downloading model… ${(it * 100).toInt()}%" }
                                    ?: "Downloading model…"
                                state.aiAvailability == AiAvailability.DOWNLOADABLE -> "Tap to download the model"
                                state.aiAvailability == AiAvailability.AVAILABLE -> "Gemini Nano ready — verifies amount, merchant & category"
                                else -> "Use Gemini Nano to verify amount, merchant & category"
                            },
                        )
                        if (aiDownloading) {
                            val frac = state.aiDownloadFraction
                            if (frac != null) {
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                            }
                        }
                    }
                },
                trailingContent = {
                    if (state.aiChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Switch(
                            checked = state.aiEnabled && !aiUnavailable,
                            enabled = !aiUnavailable && !aiDownloading,
                            onCheckedChange = { vm.setAiEnabled(it) },
                        )
                    }
                },
                // Tap to download (DOWNLOADABLE) or re-probe; disabled while busy.
                modifier = if (!aiUnavailable && !state.aiChecking && !aiDownloading) {
                    Modifier.clickable { vm.checkAiAvailability() }
                } else {
                    Modifier
                },
            )
        } else {
            ListItem(
                leadingContent = { Icon(Icons.Filled.NotificationsActive, null) },
                headlineContent = { Text("Capture from notifications") },
                supportingContent = { Text("Not available on this platform yet") },
            )
        }

        SectionHeader("Backup")
        ListItem(
            leadingContent = { Icon(Icons.Filled.CloudUpload, null) },
            headlineContent = { Text("Backup & restore") },
            supportingContent = { Text("Export or import your data as a file") },
            trailingContent = { NavChevron() },
            modifier = Modifier.clickable(onClick = onOpenBackup),
        )
    }
}

/** Small muted chevron marking a row that navigates to another screen. */
@Composable
private fun NavChevron() {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}
