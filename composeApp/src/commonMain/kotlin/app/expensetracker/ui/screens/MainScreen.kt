@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.expensetracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.viewmodel.InboxViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Summary("Summary", Icons.Filled.PieChart),
    Transactions("Activity", Icons.Filled.ReceiptLong),
    Reports("Reports", Icons.Filled.BarChart),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun MainScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenNotificationCapture: () -> Unit,
    onOpenCurrencyPicker: () -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(Tab.Summary.ordinal) }
    val tab = Tab.entries[tabIndex]
    val inboxVm: InboxViewModel = viewModel { InboxViewModel() }
    val unreadCount by inboxVm.unreadCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.label) },
                actions = {
                    IconButton(onClick = onOpenInbox) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Inbox, contentDescription = "Inbox")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tabIndex = entry.ordinal },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.Summary || tab == Tab.Transactions) {
                ExtendedFloatingActionButton(
                    onClick = onAddExpense,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add expense") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Summary -> SummaryScreen(onEditExpense = onEditExpense)
                Tab.Transactions -> TransactionsScreen(onEditExpense = onEditExpense)
                Tab.Reports -> ReportsScreen()
                Tab.Settings -> SettingsScreen(
                    onOpenCategories = onOpenCategories,
                    onOpenRecurring = onOpenRecurring,
                    onOpenBackup = onOpenBackup,
                    onOpenNotificationCapture = onOpenNotificationCapture,
                    onOpenCurrencyPicker = onOpenCurrencyPicker,
                )
            }
        }
    }
}
