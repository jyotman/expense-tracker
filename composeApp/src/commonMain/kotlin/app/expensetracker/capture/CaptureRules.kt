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
     * Unambiguous outgoing-payment words. A notification containing one of these is treated as a
     * spend even when weak income words (e.g. "cashback", "reward") also appear in the same text.
     * Matched on word boundaries, so inflections must be listed explicitly ("charge" does not cover
     * "charges"). "recharge"/"recharged" are spends in their own right (a substring bug used to
     * capture them only via "charge").
     */
    val strongSpendKeywords: List<String> = listOf(
        "spent", "debited", "debit", "charged", "charge", "charges", "purchase", "purchases",
        "purchased", "deducted", "withdrawn", "withdrawal", "bought", "recharge", "recharged",
    )

    /**
     * Spend-adjacent words that can also appear in income notifications. These lose to
     * [strongIncomeKeywords] — e.g. "salary payment received" or "payment credited" → income.
     * Both "transfer" and "transferred" are spend signals: real outgoing alerts say either "Own
     * Funds Transfer … completed" or "You transferred …". Because capture is review-to-confirm (a
     * false positive is one inbox dismiss, a miss is an untracked spend), we accept that a bare
     * "X transferred to your account" with no income word gets captured; a genuine credit ("salary
     * transferred to your account") is still suppressed by the strong-income tier.
     */
    val weakSpendKeywords: List<String> = listOf(
        "paid", "payment", "payments", "transaction", "transactions", "txn", "sent",
        "transfer", "transferred",
    )

    /**
     * Strong income signals. These suppress [weakSpendKeywords] but NOT [strongSpendKeywords].
     * Note: "cashback" is intentionally absent — it routinely appears in payment confirmations
     * (e.g. Trust Bank appends cashback reward info). "deposit" is also absent; it is ambiguous
     * (fixed-deposit placement = spend; salary deposit = income) and covered by "credited".
     */
    val strongIncomeKeywords: List<String> = listOf(
        "credited", "received", "refund", "refunded", "refunds", "reversed", "reversal", "salary",
    )

    /**
     * Phrases that cause immediate hard rejection — money definitely did not leave the account,
     * regardless of what spend keywords are also present.
     */
    val blockPhrases: List<String> = listOf(
        // OTP / step-up authentication flows
        "one-time password", "one time password", "passcode", "otp",
        "authenticate your", "tap here to verify", "tap to verify",
        // Declined or failed — no money moved
        "declined", "payment failed", "transaction failed", "transfer failed", "insufficient",
        // Bill-due reminders — not a completed transaction
        "payment due", "amount due", "minimum payment",
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
}
