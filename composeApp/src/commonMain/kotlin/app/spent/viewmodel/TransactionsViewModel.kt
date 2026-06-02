@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.spent.ServiceLocator
import app.spent.data.CategoryItem
import app.spent.data.ExpenseItem
import app.spent.data.MonthPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

/** An expense joined with its (possibly null / deleted) category, for list rendering. */
data class TransactionRow(val expense: ExpenseItem, val category: CategoryItem?)

data class TransactionsUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val rows: List<TransactionRow> = emptyList(),
    val totalMinor: Long = 0,
    val currencySymbol: String = "$",
)

class TransactionsViewModel : ViewModel() {
    private val expenses = ServiceLocator.expenseRepository
    private val categories = ServiceLocator.categoryRepository
    private val tz = TimeZone.currentSystemDefault()

    val period: StateFlow<MonthPeriod> = PeriodController.period
    private val rangeFlow = period.map { it.range(tz) }

    val uiState: StateFlow<TransactionsUiState> = combine(
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeInRange(s, e) },
        categories.observeAll(),
        period,
    ) { items, cats, period ->
        val byId = cats.associateBy { it.id }
        TransactionsUiState(
            period = period,
            rows = items.map { TransactionRow(it, it.categoryId?.let(byId::get)) },
            totalMinor = items.sumOf { it.amountMinor },
            currencySymbol = ServiceLocator.settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun delete(id: Long) = expenses.delete(id)
    fun prevMonth() = PeriodController.prev()
    fun nextMonth() = PeriodController.next()
}
