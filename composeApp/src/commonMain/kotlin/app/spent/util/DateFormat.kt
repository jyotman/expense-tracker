@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.spent.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateFormat {
    private val tz get() = TimeZone.currentSystemDefault()

    /** "Mon, 01 Jun 2026" */
    fun full(instant: Instant): String {
        val d = instant.toLocalDateTime(tz)
        val day = d.day.toString().padStart(2, '0')
        return "${dow(d.dayOfWeek)}, $day ${mon(d.month)} ${d.year}"
    }

    /** "01 Jun" */
    fun dayMonth(instant: Instant): String {
        val d = instant.toLocalDateTime(tz)
        val day = d.day.toString().padStart(2, '0')
        return "$day ${mon(d.month)}"
    }

    /** Section header: "Today", "Yesterday", or "Mon, 01 Jun". */
    fun relativeDay(instant: Instant, now: Instant): String {
        val day: LocalDate = instant.toLocalDateTime(tz).date
        val today: LocalDate = now.toLocalDateTime(tz).date
        return when (day) {
            today -> "Today"
            today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
            else -> full(instant).substringBeforeLast(" ") // drop year for compactness
        }
    }

    private fun dow(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
        else -> ""
    }

    private fun mon(m: Month): String = when (m.number) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> ""
    }
}
