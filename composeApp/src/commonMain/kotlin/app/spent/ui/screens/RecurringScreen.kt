@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)

package app.spent.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.spent.data.Money
import app.spent.ui.components.CategoryAvatar
import app.spent.ui.components.EmptyState
import app.spent.viewmodel.RecurringViewModel
import app.spent.ServiceLocator

@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val vm: RecurringViewModel = viewModel { RecurringViewModel() }
    val rows by vm.rows.collectAsState()
    val symbol = ServiceLocator.settings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Autorenew,
                title = "No recurring expenses",
                subtitle = "Turn on “Repeat” when adding an expense to set up a recurring one.",
                modifier = Modifier.padding(padding).padding(top = 32.dp),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(rows, key = { it.rule.id }) { row ->
                val rule = row.rule
                val cadence = "Every ${rule.intervalCount} ${rule.intervalUnit.name.lowercase()}" +
                    if (rule.intervalCount > 1) "s" else ""
                ListItem(
                    leadingContent = { CategoryAvatar(row.category) },
                    headlineContent = {
                        Text(row.category?.name ?: rule.merchant ?: rule.note ?: "Recurring expense")
                    },
                    supportingContent = { Text("${Money.format(rule.amountMinor, symbol)} · $cadence") },
                    trailingContent = {
                        Switch(
                            checked = rule.isActive,
                            onCheckedChange = { vm.toggleActive(rule.id, it) },
                        )
                    },
                )
            }
        }
    }
}
