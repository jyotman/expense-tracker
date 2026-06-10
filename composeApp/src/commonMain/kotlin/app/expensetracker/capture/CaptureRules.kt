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
        "INR", "USD", "SGD", "EUR", "GBP", "AED", "SAR", "JPY", "CNY", "AUD", "CAD", "CHF", "HKD",
    )

    /**
     * Words that signal money leaving the account (not a credit/refund). Matched on word
     * boundaries, so inflections must be listed explicitly ("payment" does not cover "payments").
     */
    val spendKeywords: List<String> = listOf(
        "spent", "debited", "debit", "paid", "payment", "payments", "purchase", "purchases",
        "purchased", "charged", "charge", "charges", "txn", "transaction", "transactions",
        "sent", "withdrawn", "withdrawal", "bought", "deducted", "transfer", "transferred",
        "recharge", "recharged",
    )

    /** Words that mean money came IN — skipped, since this is a spends-only tracker. */
    val incomeKeywords: List<String> = listOf(
        "credited", "received", "refund", "refunded", "refunds", "cashback", "reversed",
        "reversal", "salary", "deposit", "deposited", "deposits",
    )

    /**
     * Marketing words that flag a notification as likely promotional. Banks post promos from the
     * same package as transaction alerts, and promo copy pairs numbers with transactional words
     * ("...bonus miles on every purchase"). When one of these appears and the detected amount has
     * no currency marker, the number is promo math ("500 bonus miles", "25% off"), not a payment.
     * A currency-tagged amount still counts — real alerts sometimes carry rewards footers.
     */
    val promoKeywords: List<String> = listOf(
        "offer", "offers", "bonus", "discount", "miles", "reward", "rewards", "points",
        "voucher", "coupon", "promo", "promotion", "sale", "deal", "deals", "earn", "earned",
        "win", "free", "congratulations", "exclusive", "discover more", "shop now",
        "limited time", "don't miss", "t&c", "t&cs",
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
