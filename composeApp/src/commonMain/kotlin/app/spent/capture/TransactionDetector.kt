package app.spent.capture

import kotlin.math.roundToLong

/**
 * Heuristics for spotting payment notifications and pulling the amount out of them. Used both as
 * a gate (is this notification worth parsing?) and as the rules fallback when on-device AI is off
 * or unavailable. Pure + testable.
 */
object TransactionDetector {

    /** Words that signal a money-leaving-your-account event (not a credit/refund). */
    private val spendKeywords = listOf(
        "spent", "debited", "debit", "paid", "payment", "purchase", "charged", "charge",
        "txn", "transaction", "sent", "withdrawn", "bought", "deducted",
    )

    /** Words that mean money came IN — we skip these (this is a spends-only tracker). */
    private val incomeKeywords = listOf(
        "credited", "received", "refund", "refunded", "cashback", "reversed", "salary", "deposit",
    )

    // $1,234.56 / S$45.20 / ₹1,200 / Rs. 500 / INR 500.00 / EUR 12,50 — symbol or code, either side.
    private val amountRegex = Regex(
        """(?:(?<sym>[$₹€£]|rs\.?|inr|usd|sgd|eur|gbp|aed|s\$)\s*)?(?<num>\d{1,3}(?:[,\s]\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?:\s*(?<sym2>inr|usd|sgd|eur|gbp|aed))?""",
        RegexOption.IGNORE_CASE,
    )

    /** Default set of bank / wallet / card app packages worth listening to. Extensible by the user. */
    val defaultPackages: Set<String> = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay (India)
        "com.google.android.apps.walletnfcrel",   // Google Wallet
        "com.revolut.revolut",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.paypal.android.p2pmobile",
        "com.wise.android",
        "com.monzo.android",
        "com.starlingbank.android",
        "com.chase.sig.android",
        "com.americanexpress.android.acctsvcs.us",
        "com.infonow.bofa",
    )

    fun isIncome(text: String): Boolean {
        val lower = text.lowercase()
        return incomeKeywords.any { lower.contains(it) }
    }

    /** True if the text looks like an outgoing payment with an amount. */
    fun isLikelySpend(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        if (incomeKeywords.any { lower.contains(it) }) return false
        val hasKeyword = spendKeywords.any { lower.contains(it) }
        return hasKeyword && extractAmountMinor(text) != null
    }

    /**
     * Extract the most likely transaction amount as minor units. Picks the first currency-tagged
     * amount, else the largest bare number with a decimal part (avoids matching "ending 1234").
     */
    fun extractAmountMinor(text: String): Long? {
        val matches = amountRegex.findAll(text).toList()
        if (matches.isEmpty()) return null

        // Prefer amounts that carry a currency symbol/code.
        val tagged = matches.firstOrNull { it.groups["sym"] != null || it.groups["sym2"] != null }
        val chosen = tagged ?: matches.maxByOrNull { (it.groups["num"]?.value?.length ?: 0) }
        val raw = chosen?.groups?.get("num")?.value ?: return null
        return parseNumberToMinor(raw)
    }

    private fun parseNumberToMinor(raw: String): Long? {
        val cleaned = raw.replace(" ", "").replace(",", "")
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0) return null
        return (value * 100).roundToLong()
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
