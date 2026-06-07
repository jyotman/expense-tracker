package app.expensetracker.capture

import app.expensetracker.data.Money

/**
 * Format an amount for capture-time display (inbox row + push notification title).
 *
 * Uses the raw detected currency token from the original notification text as a prefix.
 * ISO-code tokens (e.g. "SGD", "USD") get a separating space ("SGD 18.39"); symbol tokens
 * ("₹", "S$") do not ("₹1,200"). Thousands are always grouped for readability.
 *
 * When no token was detected, falls back to [homeSymbol] if provided (home-currency display);
 * if neither is available, returns a bare grouped number. Never blocks on network or DB.
 */
fun formatCapturedAmount(amountMinor: Long, currencyToken: String?, homeSymbol: String? = null): String {
    val major = amountMinor / 100
    val cents = (amountMinor % 100).let { if (it < 0) -it else it }.toString().padStart(2, '0')
    val grouped = Money.groupThousands(major)
    val number = if (amountMinor % 100 == 0L) grouped else "$grouped.$cents"
    val effectiveToken = currencyToken ?: homeSymbol
    return when {
        effectiveToken == null -> number
        effectiveToken.uppercase() in CaptureRules.currencyCodes -> "$effectiveToken $number"
        else -> "$effectiveToken$number"
    }
}
