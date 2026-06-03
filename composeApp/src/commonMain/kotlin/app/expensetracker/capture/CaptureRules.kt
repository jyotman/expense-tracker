package app.expensetracker.capture

/**
 * Declarative inputs for notification parsing — the single place to extend currency coverage,
 * detection keywords, or the default finance-app packages. [TransactionDetector] builds its matchers
 * from this, so adding a currency or keyword is a one-line data change here rather than editing a
 * hand-tuned regex literal. (Defaults lean English + common currencies; the package set is further
 * extensible per-user via settings.)
 */
object CaptureRules {

    /** Currency symbols recognised immediately before (or, for codes, after) an amount. */
    val currencySymbols: List<String> = listOf(
        "$", "₹", "€", "£", "¥", "₩", "₪", "₫", "₴", "₦", "฿", "₺", "₱",
        "R$", "S$", "kr", "Rs.", "Rs",
    )

    /** ISO-style currency codes recognised adjacent to an amount (either side). */
    val currencyCodes: List<String> = listOf(
        "INR", "USD", "SGD", "EUR", "GBP", "AED", "JPY", "CNY", "AUD", "CAD", "CHF", "HKD",
    )

    /** Words that signal money leaving the account (not a credit/refund). */
    val spendKeywords: List<String> = listOf(
        "spent", "debited", "debit", "paid", "payment", "purchase", "charged", "charge",
        "txn", "transaction", "sent", "withdrawn", "bought", "deducted",
    )

    /** Words that mean money came IN — skipped, since this is a spends-only tracker. */
    val incomeKeywords: List<String> = listOf(
        "credited", "received", "refund", "refunded", "cashback", "reversed", "salary", "deposit",
    )

    /** Default bank / wallet / card app packages worth listening to. Extended by user settings. */
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
}
