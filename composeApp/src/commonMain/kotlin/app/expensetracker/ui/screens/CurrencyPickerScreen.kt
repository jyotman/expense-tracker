@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.data.CurrencyInfo
import app.expensetracker.data.CurrencyMeta
import app.expensetracker.viewmodel.SettingsViewModel

@Composable
fun CurrencyPickerScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel() }
    val state by vm.state.collectAsState()
    var pendingCurrency by remember { mutableStateOf<CurrencyInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currency") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(CurrencyMeta.homeOptions, key = { it.code }) { c ->
                val selected = c.code == state.currencyCode
                ListItem(
                    headlineContent = { Text(c.displayName) },
                    supportingContent = { Text("${c.code} · ${c.symbol}") },
                    trailingContent = {
                        if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable {
                        when {
                            selected -> {}
                            // Only skip the warning when we positively know there are no expenses.
                            // The count loads async, so an unconfirmed 0 must route through the dialog.
                            state.expenseCountLoaded && state.expenseCount == 0L -> {
                                vm.setDefaultCurrency(c.code); onBack()
                            }
                            else -> pendingCurrency = c
                        }
                    },
                )
            }
        }
    }

    pendingCurrency?.let { target ->
        val plural = if (state.expenseCount == 1L) "expense" else "expenses"
        AlertDialog(
            onDismissRequest = { pendingCurrency = null },
            title = { Text("Change currency?") },
            text = {
                Text(
                    "Your ${state.expenseCount} existing $plural won't be converted — they'll keep their " +
                        "amounts and just show ${target.symbol}. New expenses will use " +
                        "${target.displayName} (${target.symbol}).",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.setDefaultCurrency(target.code); pendingCurrency = null; onBack() }) {
                    Text("Change")
                }
            },
            dismissButton = { TextButton(onClick = { pendingCurrency = null }) { Text("Cancel") } },
        )
    }
}
