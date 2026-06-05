package app.expensetracker.data

import kotlin.math.absoluteValue

/** Static ISO-4217 metadata for a currency the app can display or detect. */
data class CurrencyInfo(
    val code: String,        // ISO 4217, e.g. "SGD"
    val symbol: String,      // display prefix, e.g. "S$"
    val decimalDigits: Int,  // minor-unit exponent: 2 for most, 0 for JPY/KRW, 3 for KWD/BHD
    val displayName: String,
)

/**
 * The single source of truth mapping an ISO currency code to its display symbol, decimal precision
 * and name. The user's default currency is stored as a code (see [app.expensetracker.storage.SettingsStorage]);
 * the symbol shown around amounts is *derived* from it here. Amounts everywhere are stored as 2-decimal
 * minor units, so [decimalDigits] is used only when re-displaying a foreign original amount (e.g. the
 * "Originally ¥1,500" note) — never for the internal conversion math, which works on the 2-dp minor value.
 */
object CurrencyMeta {

    val all: List<CurrencyInfo> = listOf(
        CurrencyInfo("USD", "$", 2, "US Dollar"),
        CurrencyInfo("EUR", "€", 2, "Euro"),
        CurrencyInfo("GBP", "£", 2, "British Pound"),
        CurrencyInfo("INR", "₹", 2, "Indian Rupee"),
        CurrencyInfo("SGD", "S$", 2, "Singapore Dollar"),
        CurrencyInfo("AED", "AED", 2, "UAE Dirham"),
        CurrencyInfo("JPY", "¥", 0, "Japanese Yen"),
        CurrencyInfo("CNY", "CN¥", 2, "Chinese Yuan"),
        CurrencyInfo("AUD", "A$", 2, "Australian Dollar"),
        CurrencyInfo("CAD", "C$", 2, "Canadian Dollar"),
        CurrencyInfo("CHF", "CHF", 2, "Swiss Franc"),
        CurrencyInfo("HKD", "HK$", 2, "Hong Kong Dollar"),
        CurrencyInfo("KRW", "₩", 0, "South Korean Won"),
        CurrencyInfo("THB", "฿", 2, "Thai Baht"),
        CurrencyInfo("MYR", "RM", 2, "Malaysian Ringgit"),
        CurrencyInfo("IDR", "Rp", 2, "Indonesian Rupiah"),
        CurrencyInfo("VND", "₫", 0, "Vietnamese Dong"),
        CurrencyInfo("NZD", "NZ$", 2, "New Zealand Dollar"),
        CurrencyInfo("ZAR", "R", 2, "South African Rand"),
        CurrencyInfo("SAR", "SAR", 2, "Saudi Riyal"),
        CurrencyInfo("KWD", "KWD", 3, "Kuwaiti Dinar"),
        CurrencyInfo("BHD", "BHD", 3, "Bahraini Dinar"),
    )

    private val byCode: Map<String, CurrencyInfo> = all.associateBy { it.code }

    /**
     * Currencies offered as the *home* currency. Restricted to 2-decimal currencies because amounts
     * are stored and displayed as 2-decimal minor units (see [Money]); foreign currencies of other
     * precisions (JPY, KWD…) can still be detected and converted *from*.
     */
    val homeOptions: List<CurrencyInfo> = all.filter { it.decimalDigits == 2 }

    fun forCode(code: String): CurrencyInfo? = byCode[code.uppercase()]

    fun symbolFor(code: String): String = forCode(code)?.symbol ?: code.uppercase()

    fun digitsFor(code: String): Int = forCode(code)?.decimalDigits ?: 2

    /**
     * Format an amount held as 2-decimal minor units for display in [code], honouring that currency's
     * natural precision: "¥1,500" (0 digits), "₹1,000.00" (2), "1.250" (3). Used to echo a foreign
     * original amount; the app's own storage stays 2-dp regardless. The common 2-digit case defers to
     * [Money.format] so symbol/grouping/sign logic lives in one place.
     */
    fun format(amountMinor: Long, code: String): String {
        val info = forCode(code)
        val symbol = info?.symbol ?: code.uppercase()
        val digits = info?.decimalDigits ?: 2
        if (digits == 2) return Money.format(amountMinor, symbol)

        val negative = amountMinor < 0
        val abs = amountMinor.absoluteValue
        val grouped = Money.groupThousands(abs / 100)
        val sign = if (negative) "-" else ""
        if (digits == 0) return "$sign$symbol$grouped"
        // amountMinor carries 2 internal decimals; pad out to the currency's (larger) digit count.
        val fraction = (abs % 100).toString().padStart(2, '0').padEnd(digits, '0')
        return "$sign$symbol$grouped.$fraction"
    }
}
