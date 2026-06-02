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

data class SummaryUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val totalMinor: Long = 0,
    val breakdown: List<CategorySpend> = emptyList(),
    val currencySymbol: String = "$",
)

class SummaryViewModel : ViewModel() {
    private val expenses = ServiceLocator.expenseRepository
    private val categories = ServiceLocator.categoryRepository
    private val tz = TimeZone.currentSystemDefault()

    val period: StateFlow<MonthPeriod> = PeriodController.period

    private val rangeFlow = period.map { it.range(tz) }

    val uiState: StateFlow<SummaryUiState> = combine(
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeCategoryTotals(s, e) },
        rangeFlow.flatMapLatest { (s, e) -> expenses.observeTotalInRange(s, e) },
        categories.observeAll(),
        period,
    ) { totals, total, cats, period ->
        val byId = cats.associateBy { it.id }
        val breakdown = totals
            .map { CategorySpend(it.categoryId?.let(byId::get), it.totalMinor, it.count) }
            .sortedByDescending { it.totalMinor }
        SummaryUiState(
            period = period,
            totalMinor = total,
            breakdown = breakdown,
            currencySymbol = ServiceLocator.settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    fun showPrevMonth() = PeriodController.prev()
    fun showNextMonth() = PeriodController.next()
    fun setPeriod(period: MonthPeriod) = PeriodController.set(period)
}
