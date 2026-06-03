@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A calendar month. The summary/reports work in whole months (the app's default period).
 */
data class MonthPeriod(val year: Int, val month: Int) {

    /** [start, end) instant range for this month in the given timezone. */
    fun range(tz: TimeZone): Pair<Instant, Instant> {
        val start = LocalDate(year, month, 1).atStartOfDayIn(tz)
        val nextMonthDate =
            if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        val end = nextMonthDate.atStartOfDayIn(tz)
        return start to end
    }

    fun prev(): MonthPeriod =
        if (month == 1) MonthPeriod(year - 1, 12) else MonthPeriod(year, month - 1)

    fun next(): MonthPeriod =
        if (month == 12) MonthPeriod(year + 1, 1) else MonthPeriod(year, month + 1)

    fun label(): String = "${monthName(month)} $year"

    /** Short label like "Jun" used in charts. */
    fun shortLabel(): String = monthName(month).take(3)

    companion object {
        fun current(tz: TimeZone = TimeZone.currentSystemDefault()): MonthPeriod {
            val now: LocalDateTime = Clock.System.now().toLocalDateTime(tz)
            return MonthPeriod(now.year, now.month.number)
        }

        fun of(instant: Instant, tz: TimeZone = TimeZone.currentSystemDefault()): MonthPeriod {
            val d = instant.toLocalDateTime(tz)
            return MonthPeriod(d.year, d.month.number)
        }

        private fun monthName(month: Int): String = when (Month(month)) {
            Month.JANUARY -> "January"
            Month.FEBRUARY -> "February"
            Month.MARCH -> "March"
            Month.APRIL -> "April"
            Month.MAY -> "May"
            Month.JUNE -> "June"
            Month.JULY -> "July"
            Month.AUGUST -> "August"
            Month.SEPTEMBER -> "September"
            Month.OCTOBER -> "October"
            Month.NOVEMBER -> "November"
            Month.DECEMBER -> "December"
        }
    }
}
