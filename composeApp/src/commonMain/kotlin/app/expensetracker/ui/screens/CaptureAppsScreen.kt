@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.platform.InstalledApp
import app.expensetracker.viewmodel.CaptureAppsViewModel

@Composable
fun CaptureAppsScreen(onBack: () -> Unit) {
    val vm: CaptureAppsViewModel = viewModel { CaptureAppsViewModel() }
    val state by vm.state.collectAsState()
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apps to monitor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Which apps should I add?")
                    }
                },
            )
        },
    ) { padding ->
        if (showHelp) {
            ModalBottomSheet(onDismissRequest = { showHelp = false }) { PickerHelpSheet() }
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val filtered = state.filtered
        val selectedApps = state.selectedApps
        val unselectedApps = state.unselectedApps

        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = vm::setSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No apps match your search",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                appGroup("Monitored", selectedApps, checked = true, onToggle = vm::toggle)
                if (selectedApps.isNotEmpty() && unselectedApps.isNotEmpty()) {
                    item(key = "divider") { HorizontalDivider() }
                }
                appGroup("Other apps", unselectedApps, checked = false, onToggle = vm::toggle)
            }
        }
    }
}

/** A titled group of app rows whose checkboxes share the given [checked] state. No-op if empty. */
private fun LazyListScope.appGroup(
    header: String,
    apps: List<InstalledApp>,
    checked: Boolean,
    onToggle: (String) -> Unit,
) {
    if (apps.isEmpty()) return
    item(key = "header_$header") { SectionLabel(header) }
    items(apps, key = { it.packageName }) { app ->
        ListItem(
            headlineContent = { Text(app.label) },
            supportingContent = { Text(app.packageName) },
            trailingContent = {
                Checkbox(checked = checked, onCheckedChange = { onToggle(app.packageName) })
            },
            modifier = Modifier.clickable { onToggle(app.packageName) },
        )
    }
}

/** Plain-language help, shown in a bottom sheet from the app bar's info icon (keeps the list clean). */
@Composable
private fun PickerHelpSheet() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Which apps should I add?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Add only the apps that alert you about payments — your bank, credit card or wallet apps. " +
                "Everything else is ignored, which keeps wrong entries out of your inbox.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "If two apps tell you about the same payment — say Google Pay and the bank card it used — " +
                    "add just one, so the app doesn't catch and notify you twice for the same transaction.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}
