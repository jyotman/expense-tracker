package app.expensetracker.data

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

    /** A symbol-less, editable representation of an amount ("12" or "12.50") for text fields. */
    fun toEditString(amountMinor: Long): String {
        val major = amountMinor / 100
        val minor = (amountMinor % 100).absoluteValue.toString().padStart(2, '0')
        return if (amountMinor % 100 == 0L) major.toString() else "$major.$minor"
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
     * Parse a user- or model-supplied amount into minor units. Handles both grouping/decimal
     * conventions: "1,234.56", "1.234,56", "12,50" (EUR cents) and "12.50" all parse correctly.
     * When both separators appear the rightmost is the decimal point; a lone comma is treated as a
     * decimal only when it looks like cents (1–2 trailing digits), otherwise it's a thousands group.
     * Returns null unless the result is a strictly positive amount.
     */
    fun parseToMinor(input: String): Long? {
        val cleaned = input.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null
        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')
        val normalized = when {
            // Both separators present: the rightmost one is the decimal point, the other groups.
            lastDot >= 0 && lastComma >= 0 -> {
                val dec = maxOf(lastDot, lastComma)
                cleaned.substring(0, dec).replace(",", "").replace(".", "") +
                    "." + cleaned.substring(dec + 1)
            }
            // A single comma that looks like cents → decimal comma (e.g. "12,50").
            lastComma >= 0 && cleaned.count { it == ',' } == 1 && cleaned.length - lastComma - 1 in 1..2 ->
                cleaned.replace(',', '.')
            // Otherwise any commas are thousands separators.
            else -> cleaned.replace(",", "")
        }
        val value = normalized.toDoubleOrNull() ?: return null
        if (value <= 0) return null
        return (value * 100).roundToLong()
    }
}
