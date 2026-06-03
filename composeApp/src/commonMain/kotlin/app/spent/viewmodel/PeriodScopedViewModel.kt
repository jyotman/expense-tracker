@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.viewmodel

import androidx.lifecycle.ViewModel
import app.spent.ServiceLocator
import app.spent.data.CategoryItem
import app.spent.data.CategorySpend
import app.spent.data.MonthPeriod
import app.spent.repository.CategoryTotal
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * Shared plumbing for the month-scoped screens (Summary, Transactions, Reports): the globally-synced
 * period, its instant range, the current currency symbol, and month navigation. Keeps the three
 * ViewModels from each re-declaring the same wiring (and drifting apart).
 */
abstract class PeriodScopedViewModel : ViewModel() {
    protected val tz: TimeZone = TimeZone.currentSystemDefault()

    val period: StateFlow<MonthPeriod> = PeriodController.period

    protected val rangeFlow = period.map { it.range(tz) }

    protected val currencySymbol: String get() = ServiceLocator.settings.currencySymbol

    fun showPrevMonth() = PeriodController.prev()
    fun showNextMonth() = PeriodController.next()
    fun setPeriod(period: MonthPeriod) = PeriodController.set(period)
}

/** Resolve aggregated category totals to display rows, attaching each category and sorting by spend. */
fun List<CategoryTotal>.toBreakdown(categoriesById: Map<Long, CategoryItem>): List<CategorySpend> =
    map { CategorySpend(it.categoryId?.let(categoriesById::get), it.totalMinor, it.count) }
        .sortedByDescending { it.totalMinor }
