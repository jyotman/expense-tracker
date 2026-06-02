@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package app.spent.viewmodel

import androidx.lifecycle.ViewModel
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
import kotlinx.datetime.TimeZone

data class MonthlyPoint(val period: MonthPeriod, val totalMinor: Long)

data class ReportsUiState(
    val monthly: List<MonthlyPoint> = emptyList(),
    val breakdown: List<CategorySpend> = emptyList(),
    val period: MonthPeriod = MonthPeriod.current(),
    val totalMinor: Long = 0,
    val currencySymbol: String = "$",
)

class ReportsViewModel : ViewModel() {
    private val expenses = ServiceLocator.expenseRepository
    private val categories = ServiceLocator.categoryRepository
    private val tz = TimeZone.currentSystemDefault()

    // Fixed 12-month window ending with the current calendar month.
    private val months: List<MonthPeriod> = buildList {
        var p = MonthPeriod.current(tz)
        repeat(12) { add(p); p = p.prev() }
    }.reversed()

    private val seriesStart = months.first().range(tz).first
    private val seriesEnd = months.last().range(tz).second

    private val period: StateFlow<MonthPeriod> = PeriodController.period
    private val rangeFlow = period.map { it.range(tz) }

    val uiState: StateFlow<ReportsUiState> = combine(
        expenses.observeInRange(seriesStart, seriesEnd),
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeCategoryTotals(s, e) },
        categories.observeAll(),
        period,
    ) { allInWindow, totals, cats, period ->
        val byId = cats.associateBy { it.id }
        val bucket = allInWindow.groupBy { MonthPeriod.of(it.occurredAt, tz) }
        val monthly = months.map { m -> MonthlyPoint(m, bucket[m]?.sumOf { it.amountMinor } ?: 0L) }
        val breakdown = totals
            .map { CategorySpend(it.categoryId?.let(byId::get), it.totalMinor, it.count) }
            .sortedByDescending { it.totalMinor }
        ReportsUiState(
            monthly = monthly,
            breakdown = breakdown,
            period = period,
            totalMinor = breakdown.sumOf { it.totalMinor },
            currencySymbol = ServiceLocator.settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}
