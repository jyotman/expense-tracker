package app.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.data.CategorySpend
import app.expensetracker.data.Money
import app.expensetracker.ui.colorFromHex
import app.expensetracker.ui.components.CategoryAvatar
import app.expensetracker.ui.components.EmptyState
import app.expensetracker.ui.components.MonthSelector
import app.expensetracker.ui.components.ProportionBar
import app.expensetracker.viewmodel.SummaryViewModel

@Composable
fun SummaryScreen(onEditExpense: (Long) -> Unit) {
    val vm: SummaryViewModel = viewModel { SummaryViewModel() }
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        MonthSelector(
            period = state.period,
            onPrev = vm::showPrevMonth,
            onNext = vm::showNextMonth,
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Spent this month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Money.format(state.totalMinor, state.currencySymbol),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }

        if (state.breakdown.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Savings,
                title = "No spending yet",
                subtitle = "Tap “Expense” to log your first one, or let auto-capture do it for you.",
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            val maxTotal = state.breakdown.maxOf { it.totalMinor }.coerceAtLeast(1)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.breakdown, key = { it.category?.id ?: -1L }) { spend ->
                    CategorySpendRow(spend, maxTotal, state.currencySymbol)
                }
            }
        }
    }
}

@Composable
private fun CategorySpendRow(spend: CategorySpend, maxTotal: Long, symbol: String) {
    val color = spend.category?.let { colorFromHex(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryAvatar(spend.category)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    spend.category?.name ?: "Uncategorized",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    Money.format(spend.totalMinor, symbol),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            ProportionBar(
                fraction = spend.totalMinor.toFloat() / maxTotal.toFloat(),
                color = color,
            )
        }
    }
}
