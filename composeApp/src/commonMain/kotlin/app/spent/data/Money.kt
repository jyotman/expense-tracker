package app.spent.data

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Money is stored as minor units (e.g. cents) in a Long to avoid floating point drift.
 * Display formatting is currency-symbol + 2 decimals; this app is single-currency,
 * configured by the user (symbol only — no FX).
 */
object Money {
    /** "S$" + "1,234.50". symbol is a raw prefix like "$", "₹", "€". */
    fun format(amountMinor: Long, symbol: String): String {
        val negative = amountMinor < 0
        val abs = amountMinor.absoluteValue
        val major = abs / 100
        val minor = (abs % 100).toString().padStart(2, '0')
        val grouped = groupThousands(major)
        val sign = if (negative) "-" else ""
        return "$sign$symbol$grouped.$minor"
    }

    /** Compact form without grouping or minor units when whole, for tight UI like widgets. */
    fun formatShort(amountMinor: Long, symbol: String): String {
        val major = amountMinor / 100
        return "$symbol${groupThousands(major)}"
    }

    private fun groupThousands(value: Long): String {
        val s = value.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.indices.reversed()) {
            sb.append(s[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append(',')
        }
        return sb.reverse().toString()
    }

    /**
     * Parse a user-typed amount like "12", "12.5", "1,234.56" into minor units.
     * Returns null if it isn't a valid positive number.
     */
    fun parseToMinor(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return (value * 100).roundToLong()
    }
}
