@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.spent.data.ExpenseSource
import app.spent.data.Money
import app.spent.ui.components.CategoryAvatar
import app.spent.ui.components.EmptyState
import app.spent.ui.components.MonthSelector
import app.spent.util.DateFormat
import app.spent.viewmodel.TransactionRow
import app.spent.viewmodel.TransactionsViewModel
import kotlin.time.Clock

@Composable
fun TransactionsScreen(onEditExpense: (Long) -> Unit) {
    val vm: TransactionsViewModel = viewModel { TransactionsViewModel() }
    val state by vm.uiState.collectAsState()
    val now = remember { Clock.System.now() }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthSelector(
            period = state.period,
            onPrev = vm::prevMonth,
            onNext = vm::nextMonth,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${state.rows.size} transactions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Money.format(state.totalMinor, state.currencySymbol),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.rows.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ReceiptLong,
                title = "Nothing logged",
                subtitle = "Expenses you add this month will show up here.",
                modifier = Modifier.padding(top = 32.dp),
            )
            return
        }

        // Rows are sorted newest-first; group consecutive rows by day.
        val groups = remember(state.rows) {
            val out = mutableListOf<Pair<String, List<TransactionRow>>>()
            var currentLabel: String? = null
            var bucket = mutableListOf<TransactionRow>()
            state.rows.forEach { row ->
                val label = DateFormat.relativeDay(row.expense.occurredAt, now)
                if (label != currentLabel) {
                    if (bucket.isNotEmpty()) out.add(currentLabel!! to bucket)
                    currentLabel = label
                    bucket = mutableListOf()
                }
                bucket.add(row)
            }
            if (bucket.isNotEmpty() && currentLabel != null) out.add(currentLabel!! to bucket)
            out
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            groups.forEach { (label, rows) ->
                item(key = "header_$label") { DayHeader(label) }
                rows.forEach { row ->
                    item(key = row.expense.id) {
                        TransactionRowItem(row, state.currencySymbol, onClick = { onEditExpense(row.expense.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun TransactionRowItem(row: TransactionRow, symbol: String, onClick: () -> Unit) {
    val e = row.expense
    val title = e.merchant?.takeIf { it.isNotBlank() }
        ?: row.category?.name
        ?: e.note?.takeIf { it.isNotBlank() }
        ?: "Expense"
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryAvatar(row.category)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (e.source == ExpenseSource.AUTO) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "Auto-captured",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (e.recurringRuleId != null) {
                    Icon(
                        Icons.Filled.Autorenew,
                        contentDescription = "Recurring",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            val subtitle = row.category?.name?.takeIf { it != title } ?: e.note.orEmpty()
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            Money.format(e.amountMinor, symbol),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
