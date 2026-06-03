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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MonthlyPoint(val period: MonthPeriod, val totalMinor: Long)

data class ReportsUiState(
    val monthly: List<MonthlyPoint> = emptyList(),
    val breakdown: List<CategorySpend> = emptyList(),
    val period: MonthPeriod = MonthPeriod.current(),
    val totalMinor: Long = 0,
    val currencySymbol: String = "$",
)

class ReportsViewModel : PeriodScopedViewModel() {
    private val expenses = ServiceLocator.expenseRepository
    private val categories = ServiceLocator.categoryRepository

    // 12-month trend ending at the selected period. Derived from the live period flow so it follows
    // month navigation and never goes stale against a calendar rollover (at launch period = current).
    private val monthlyFlow = period.flatMapLatest { selected ->
        val months = buildList {
            var p = selected
            repeat(12) { add(p); p = p.prev() }
        }.reversed()
        val seriesStart = months.first().range(tz).first
        val seriesEnd = months.last().range(tz).second
        expenses.observeInRange(seriesStart, seriesEnd).map { allInWindow ->
            val bucket = allInWindow.groupBy { MonthPeriod.of(it.occurredAt, tz) }
            months.map { m -> MonthlyPoint(m, bucket[m]?.sumOf { it.amountMinor } ?: 0L) }
        }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        monthlyFlow,
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeCategoryTotals(s, e) },
        categories.observeAll(),
        period,
    ) { monthly, totals, cats, period ->
        val breakdown = totals.toBreakdown(cats.associateBy { it.id })
        ReportsUiState(
            monthly = monthly,
            breakdown = breakdown,
            period = period,
            totalMinor = breakdown.sumOf { it.totalMinor },
            currencySymbol = currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}
