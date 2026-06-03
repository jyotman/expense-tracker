@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package app.spent.viewmodel

import androidx.lifecycle.viewModelScope
import app.spent.ServiceLocator
import app.spent.data.CategorySpend
import app.spent.data.MonthPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class SummaryUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val totalMinor: Long = 0,
    val breakdown: List<CategorySpend> = emptyList(),
    val currencySymbol: String = "$",
)

class SummaryViewModel : PeriodScopedViewModel() {
    private val expenses = ServiceLocator.expenseRepository
    private val categories = ServiceLocator.categoryRepository

    val uiState: StateFlow<SummaryUiState> = combine(
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeCategoryTotals(s, e) },
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeTotalInRange(s, e) },
        categories.observeAll(),
        period,
    ) { totals, total, cats, period ->
        SummaryUiState(
            period = period,
            totalMinor = total,
            breakdown = totals.toBreakdown(cats.associateBy { it.id }),
            currencySymbol = currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())
}
