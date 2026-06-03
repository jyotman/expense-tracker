@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.viewmodel

import app.expensetracker.data.MonthPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.TimeZone

/** The month shown across Summary, Transactions and Reports — kept in sync app-wide. */
object PeriodController {
    private val tz = TimeZone.currentSystemDefault()
    private val _period = MutableStateFlow(MonthPeriod.current(tz))
    val period: StateFlow<MonthPeriod> = _period

    fun prev() { _period.value = _period.value.prev() }
    fun next() { _period.value = _period.value.next() }
    fun set(period: MonthPeriod) { _period.value = period }
    fun reset() { _period.value = MonthPeriod.current(tz) }
}
