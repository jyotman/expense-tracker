package app.expensetracker.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {

    @Test
    fun parses_plain_and_grouped_amounts() {
        assertEquals(1200L, Money.parseToMinor("12"))
        assertEquals(1250L, Money.parseToMinor("12.5"))
        assertEquals(1250L, Money.parseToMinor("12.50"))
        assertEquals(123456L, Money.parseToMinor("1,234.56"))
        assertEquals(120000L, Money.parseToMinor("1,200"))
    }

    @Test
    fun parses_european_conventions() {
        assertEquals(1250L, Money.parseToMinor("12,50"))       // lone comma = cents
        assertEquals(123456L, Money.parseToMinor("1.234,56"))  // dot groups, comma decimal
    }

    @Test
    fun rejects_non_positive_and_garbage() {
        assertNull(Money.parseToMinor("0"))
        assertNull(Money.parseToMinor("0.00"))
        assertNull(Money.parseToMinor("-5"))
        assertNull(Money.parseToMinor(""))
        assertNull(Money.parseToMinor("abc"))
    }

    @Test
    fun edit_string_round_trips_through_parse() {
        assertEquals("12", Money.toEditString(1200))
        assertEquals("12.50", Money.toEditString(1250))
        assertEquals(1250L, Money.parseToMinor(Money.toEditString(1250)))
    }
}
