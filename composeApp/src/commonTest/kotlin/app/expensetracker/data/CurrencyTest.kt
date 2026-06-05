package app.expensetracker.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrencyTest {

    // --- CurrencyConverter: round(amountMinor * rate), works regardless of either side's ISO digits ---

    @Test
    fun convert_two_decimal_to_two_decimal() {
        // ₹1,000.00 (100000 minor) at 0.016 SGD/INR -> S$16.00 (1600 minor)
        assertEquals(1600L, CurrencyConverter.convertMinor(100_000L, 0.016))
    }

    @Test
    fun convert_zero_decimal_source() {
        // ¥1,500 stored as 2-dp minor (150000) at 0.0089 SGD/JPY -> S$13.35 (1335 minor)
        assertEquals(1335L, CurrencyConverter.convertMinor(150_000L, 0.0089))
    }

    @Test
    fun convert_three_decimal_source() {
        // KWD 12.50 (1250 minor) at 4.40 SGD/KWD -> S$55.00 (5500 minor)
        assertEquals(5500L, CurrencyConverter.convertMinor(1_250L, 4.40))
    }

    @Test
    fun convert_rounds_to_nearest_minor_unit() {
        // 123 minor * 1.005 = 123.615 -> 124
        assertEquals(124L, CurrencyConverter.convertMinor(123L, 1.005))
    }

    @Test
    fun convert_rejects_non_positive_or_non_finite_rate() {
        assertNull(CurrencyConverter.convertMinor(1000L, 0.0))
        assertNull(CurrencyConverter.convertMinor(1000L, -1.0))
        assertNull(CurrencyConverter.convertMinor(1000L, Double.NaN))
        assertNull(CurrencyConverter.convertMinor(1000L, Double.POSITIVE_INFINITY))
    }

    // --- CurrencyMeta.format: honours each currency's natural decimal precision ---

    @Test
    fun format_two_decimal_currency() {
        assertEquals("₹1,000.00", CurrencyMeta.format(100_000L, "INR"))
        assertEquals("S$16.20", CurrencyMeta.format(1_620L, "SGD"))
    }

    @Test
    fun format_zero_decimal_currency_drops_decimals() {
        assertEquals("¥1,500", CurrencyMeta.format(150_000L, "JPY"))
        assertEquals("₩50,000", CurrencyMeta.format(5_000_000L, "KRW"))
    }

    @Test
    fun format_three_decimal_currency_pads() {
        // 1250 internal minor -> major 12.50 -> three-digit display "12.500"
        assertEquals("KWD12.500", CurrencyMeta.format(1_250L, "KWD"))
    }

    @Test
    fun format_unknown_code_falls_back_to_code_and_two_digits() {
        assertEquals("ZWL10.00", CurrencyMeta.format(1_000L, "ZWL"))
    }

    // --- CurrencyMeta lookups ---

    @Test
    fun symbol_and_digits_lookup() {
        assertEquals("S$", CurrencyMeta.symbolFor("SGD"))
        assertEquals(0, CurrencyMeta.digitsFor("JPY"))
        assertEquals(3, CurrencyMeta.digitsFor("BHD"))
        assertEquals(2, CurrencyMeta.digitsFor("UNKNOWN"))
    }

    @Test
    fun home_options_are_all_two_decimal() {
        // The home currency is stored/displayed as 2-dp minor units, so only 2-decimal currencies
        // are offered as the default; non-2dp ones (JPY/KWD) remain detectable as foreign.
        assertTrue(CurrencyMeta.homeOptions.all { it.decimalDigits == 2 })
        assertTrue(CurrencyMeta.homeOptions.any { it.code == "SGD" })
        assertTrue(CurrencyMeta.homeOptions.none { it.code == "JPY" || it.code == "KWD" })
    }
}
