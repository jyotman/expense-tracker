package app.expensetracker.capture

import app.expensetracker.data.Money

/**
 * Heuristics for spotting payment notifications and pulling the amount out of them. Used both as
 * a gate (is this notification worth parsing?) and as the rules fallback when on-device AI is off
 * or unavailable. Pure + testable.
 */
object TransactionDetector {

    // Amount matcher, built from CaptureRules so currency coverage is data-driven, not baked into a
    // literal. Examples: $1,234.56 / S$45.20 / ₹1,200 / Rs. 500 / INR 500.00 / EUR 12,50 — a symbol
    // or code on either side. The decimal part accepts '.' or ',' so European cents ("12,50") survive
    // into Money.parseToMinor. Leading token may be a symbol or a code (e.g. "INR 500"); trailing is
    // a code (e.g. "500 INR"). Longer tokens are tried first so "Rs." beats "Rs" and "S$" beats "$".
    //
    // The first num branch is the *grouped* form and requires at least one thousands separator
    // (the `+`); without it, a plain run like "15000" would match only its first 3 digits ("150")
    // and the alternation would never reach the second branch — so the second branch handles the
    // ungrouped form ("15000", "15000.00") as a whole.
    private fun alternation(tokens: List<String>): String =
        tokens.sortedByDescending { it.length }.joinToString("|") { it.escapeRegex() }

    private val amountRegex: Regex = run {
        val leading = alternation(CaptureRules.currencySymbols + CaptureRules.currencyCodes)
        val trailing = alternation(CaptureRules.currencyCodes)
        Regex(
            """(?:(?<sym>$leading)\s*)?(?<num>\d{1,3}(?:[,\s]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)(?:\s*(?<sym2>$trailing))?""",
            RegexOption.IGNORE_CASE,
        )
    }

    /** Escape regex metacharacters so a literal token (e.g. "$", "Rs.") matches itself. */
    private fun String.escapeRegex(): String = buildString {
        for (c in this@escapeRegex) {
            if (c in """\.[]{}()*+?^$|""") append('\\')
            append(c)
        }
    }

    fun isIncome(text: String): Boolean {
        val lower = text.lowercase()
        return CaptureRules.incomeKeywords.any { lower.contains(it) }
    }

    /** True if the text looks like an outgoing payment with an amount. */
    fun isLikelySpend(text: String): Boolean {
        if (text.isBlank()) return false
        if (isIncome(text)) return false
        val hasKeyword = CaptureRules.spendKeywords.any { text.lowercase().contains(it) }
        return hasKeyword && extractAmountMinor(text) != null
    }

    /**
     * Extract the most likely transaction amount as minor units. Prefers an amount carrying a
     * currency symbol/code; failing that prefers one with a decimal part (a real amount), then
     * falls back to the first bare number — never the longest, which would latch onto card or
     * reference digits like "ending 1234".
     */
    fun extractAmountMinor(text: String): Long? = bestMatch(text)?.let {
        Money.parseToMinor(it.groups["num"]?.value ?: return null)
    }

    /**
     * Returns the raw currency token (symbol or code) that appeared next to the best amount match,
     * or null if no currency marker was present. The raw token is returned as-is from the text
     * ("S$", "SGD", "₹", "$", etc.) — callers must not assume it maps to any specific ISO code.
     */
    fun extractCurrencyToken(text: String): String? {
        val match = bestMatch(text) ?: return null
        return (match.groups["sym"] ?: match.groups["sym2"])?.value?.trim()
    }

    private fun bestMatch(text: String): MatchResult? {
        val matches = amountRegex.findAll(text).toList()
        if (matches.isEmpty()) return null
        val tagged = matches.firstOrNull { it.groups["sym"] != null || it.groups["sym2"] != null }
        val withDecimal = matches.firstOrNull { it.groups["num"]?.value?.let { n -> n.contains('.') || n.contains(',') } == true }
        return tagged ?: withDecimal ?: matches.first()
    }

    /** A rough merchant guess: text after "at"/"to" up to a delimiter. */
    fun guessMerchant(text: String): String? {
        val regex = Regex("""\b(?:at|to|@)\s+([A-Za-z0-9&'./ -]{2,40})""", RegexOption.IGNORE_CASE)
        val raw = regex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return null
        // Trim trailing connector words / punctuation noise.
        return raw.trimEnd('.', ',', ';', ':').substringBefore(" on ").substringBefore(" for ").trim()
            .takeIf { it.isNotBlank() }
    }
}
