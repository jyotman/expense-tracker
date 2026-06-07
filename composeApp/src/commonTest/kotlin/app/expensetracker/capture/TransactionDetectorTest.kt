package app.expensetracker.capture

import app.expensetracker.data.CategoryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TransactionDetectorTest {

    @Test
    fun extracts_amount_with_symbol() {
        assertEquals(4520L, TransactionDetector.extractAmountMinor("You spent S\$45.20 at STARBUCKS"))
        assertEquals(120000L, TransactionDetector.extractAmountMinor("Debited ₹1,200 for Uber"))
        assertEquals(1299L, TransactionDetector.extractAmountMinor("\$12.99 charged"))
    }

    @Test
    fun ungrouped_large_amount_is_not_truncated_to_three_digits() {
        // Regression: "SGD15000.00" used to match only "150" because the grouped branch greedily
        // grabbed the first 3 digits and the alternation never tried the plain-number branch.
        val dbs = "An Own Funds Transfer of SGD15000.00 from A/C ending 7328 to A/C ending 7596 " +
            "on 09 Jun 16:46 (SGT) was completed. If unauthorised, call +65 63272265."
        assertEquals(1_500_000L, TransactionDetector.extractAmountMinor(dbs))
        assertEquals("SGD", TransactionDetector.extractCurrencyToken(dbs))
        // Same number without separators and without a decimal part.
        assertEquals(1_500_000L, TransactionDetector.extractAmountMinor("Paid SGD15000 to merchant"))
        // The grouped form still works and is preferred when separators are present.
        assertEquals(1_500_000L, TransactionDetector.extractAmountMinor("Paid SGD 15,000.00"))
    }

    @Test
    fun keeps_european_decimal_comma() {
        // The regex comment promises "EUR 12,50" — the cents must survive, not be dropped to €12.
        assertEquals(1250L, TransactionDetector.extractAmountMinor("EUR 12,50 spent at Cafe"))
    }

    @Test
    fun untagged_amount_is_not_the_longest_digit_run() {
        // A reference / card number must never win over the real amount.
        assertEquals(5000L, TransactionDetector.extractAmountMinor("Spent 50 at Store ref 7829341"))
        // A decimal amount is preferred over a bare card number of the same/greater length.
        assertEquals(999L, TransactionDetector.extractAmountMinor("Purchase 9.99 card ending 1234"))
    }

    // ---- isLikelySpend: strong spend keywords ----------------------------------------

    @Test
    fun strong_spend_always_captured() {
        // Every strong keyword with a plain amount
        assertTrue(TransactionDetector.isLikelySpend("You spent \$45.20 at Tesco"))
        assertTrue(TransactionDetector.isLikelySpend("INR 500 debited from your account"))
        assertTrue(TransactionDetector.isLikelySpend("INR 500 debit from your account"))
        assertTrue(TransactionDetector.isLikelySpend("SGD 18.39 charged at FoodPanda"))
        assertTrue(TransactionDetector.isLikelySpend("\$12.99 charge applied"))
        assertTrue(TransactionDetector.isLikelySpend("Purchase of AUD 89.00 at Amazon"))
        assertTrue(TransactionDetector.isLikelySpend("INR 2000 deducted for postpaid bill"))
        assertTrue(TransactionDetector.isLikelySpend("SGD 500 withdrawn at DBS ATM"))
        assertTrue(TransactionDetector.isLikelySpend("You bought 3 items for \$34.50"))
    }

    @Test
    fun strong_spend_beats_income_words_in_same_notification() {
        // Trust Bank: payment confirmation with cashback reward appended
        assertTrue(
            TransactionDetector.isLikelySpend(
                "You've spent SGD 42.38 at SHOPEE SINGAPORE GPAY on 7 Jun 2026 with Trust Cashback card. You'll receive estimated S\$0.42 cashback*."
            )
        )
        // Charged + "refund policy" mention
        assertTrue(TransactionDetector.isLikelySpend("SGD 99 charged at Hotel. Refund policy applies."))
        // Deducted + "salary" mention (deducted is unambiguous)
        assertTrue(TransactionDetector.isLikelySpend("INR 500 deducted from your salary account"))
    }

    @Test
    fun strong_spend_without_amount_is_rejected() {
        assertFalse(TransactionDetector.isLikelySpend("You spent at Tesco today"))
        assertFalse(TransactionDetector.isLikelySpend("Account debited"))
        assertFalse(TransactionDetector.isLikelySpend("Card charged successfully"))
    }

    // ---- isLikelySpend: weak spend keywords -----------------------------------------

    @Test
    fun weak_spend_captured_without_income_signal() {
        assertTrue(TransactionDetector.isLikelySpend("Payment of SGD 45.20 processed"))
        assertTrue(TransactionDetector.isLikelySpend("Transaction: SGD 99.00 to Shopee"))
        assertTrue(TransactionDetector.isLikelySpend("Txn of INR 1000 at Swiggy"))
        assertTrue(TransactionDetector.isLikelySpend("Transfer complete: SGD 500 to John"))
        assertTrue(TransactionDetector.isLikelySpend("Payment sent to GRAB: \$12.50"))
        assertTrue(TransactionDetector.isLikelySpend("You paid \$34.00 at NTUC"))
    }

    @Test
    fun weak_spend_rejected_when_strong_income_present() {
        assertFalse(TransactionDetector.isLikelySpend("Payment of \$5000 received from employer"))
        assertFalse(TransactionDetector.isLikelySpend("Salary payment of \$5000 credited to account"))
        assertFalse(TransactionDetector.isLikelySpend("Refund of \$20 credited to your account"))
        assertFalse(TransactionDetector.isLikelySpend("Transfer of \$1000 received from John"))
        assertFalse(TransactionDetector.isLikelySpend("Your transaction of \$500 has been reversed"))
        assertFalse(TransactionDetector.isLikelySpend("You received \$500 salary this month"))
        assertFalse(TransactionDetector.isLikelySpend("INR 50000 salary credited by employer"))
    }

    // ---- isLikelySpend: income-only notifications ------------------------------------

    @Test
    fun income_only_notifications_rejected() {
        assertFalse(TransactionDetector.isLikelySpend("INR 5000 credited to your account"))
        assertFalse(TransactionDetector.isLikelySpend("You received \$800 from John"))
        assertFalse(TransactionDetector.isLikelySpend("Refund of \$20 processed"))
        assertFalse(TransactionDetector.isLikelySpend("SGD 45 refunded to your card"))
        assertFalse(TransactionDetector.isLikelySpend("Payment reversed: INR 500"))
        assertFalse(TransactionDetector.isLikelySpend("SGD 0.42 cashback credited to your account"))
    }

    // ---- isLikelySpend: block phrases -----------------------------------------------

    @Test
    fun otp_notifications_always_rejected() {
        // Trust Bank OTP format — seen in production
        assertFalse(
            TransactionDetector.isLikelySpend(
                "Authenticate your purchase — Your one-time password is KGI-579945 for your online transaction of SGD 42.38"
            )
        )
        assertFalse(TransactionDetector.isLikelySpend("Your one time password is 123456 for transaction \$45"))
        assertFalse(TransactionDetector.isLikelySpend("Your OTP 482910 for payment of INR 500"))
        assertFalse(TransactionDetector.isLikelySpend("Enter passcode to authorise SGD 100 transfer"))
        assertFalse(TransactionDetector.isLikelySpend("Tap here to verify your payment of SGD 45.20"))
        assertFalse(TransactionDetector.isLikelySpend("Tap to verify your SGD 99 transaction"))
        assertFalse(TransactionDetector.isLikelySpend("Authenticate your purchase of \$99 via Trust App"))
    }

    @Test
    fun declined_and_failed_transactions_rejected() {
        assertFalse(TransactionDetector.isLikelySpend("Transaction declined: \$45.00 at GRAB"))
        assertFalse(TransactionDetector.isLikelySpend("Card declined at Starbucks for \$6.50"))
        assertFalse(TransactionDetector.isLikelySpend("Payment failed: SGD 99.00 to Shopee"))
        assertFalse(TransactionDetector.isLikelySpend("Transaction failed at POS terminal \$34.00"))
        assertFalse(TransactionDetector.isLikelySpend("Transfer failed: SGD 200 to John"))
        assertFalse(TransactionDetector.isLikelySpend("Insufficient funds for \$150 transaction at NTUC"))
    }

    @Test
    fun bill_due_reminders_rejected() {
        assertFalse(TransactionDetector.isLikelySpend("Payment due: \$500 credit card bill"))
        assertFalse(TransactionDetector.isLikelySpend("Your payment due on 15 Jun: SGD 350"))
        assertFalse(TransactionDetector.isLikelySpend("Minimum payment of \$50 due for your card"))
        assertFalse(TransactionDetector.isLikelySpend("Amount due: SGD 350 by end of month"))
    }

    // ---- isLikelySpend: no-signal rejections ----------------------------------------

    @Test
    fun no_spend_keyword_is_rejected_even_with_amount() {
        assertFalse(TransactionDetector.isLikelySpend("Your account balance is \$1234.56"))
        assertFalse(TransactionDetector.isLikelySpend("SGD 45.20 available credit"))
        assertFalse(TransactionDetector.isLikelySpend("Statement ready: SGD 350 total"))
        assertFalse(TransactionDetector.isLikelySpend("Your OTP is 4521"))
        assertFalse(TransactionDetector.isLikelySpend(""))
        assertFalse(TransactionDetector.isLikelySpend("   "))
    }

    // ---- isLikelySpend: keywords match whole words, not substrings -------------------

    @Test
    fun keywords_match_whole_words_only() {
        // "paid" must not fire on "unpaid"/"prepaid"
        assertFalse(TransactionDetector.isLikelySpend("Unpaid bill of SGD 50"))
        assertFalse(TransactionDetector.isLikelySpend("Prepaid balance of SGD 50 remaining"))
        // sanity: the bare words still capture
        assertTrue(TransactionDetector.isLikelySpend("Transfer of SGD 200 to your landlord"))
        assertTrue(TransactionDetector.isLikelySpend("You paid SGD 50 at NTUC"))
    }

    @Test
    fun transferred_is_captured_but_income_still_wins() {
        // "transferred" is a spend signal (recall > precision under review-to-confirm), so an
        // outgoing transfer is captured...
        assertTrue(TransactionDetector.isLikelySpend("You transferred SGD 500 to John"))
        // ...as is a bare incoming transfer with no income word (accepted false positive — one tap to dismiss).
        assertTrue(TransactionDetector.isLikelySpend("SGD 200 transferred to your account"))
        // ...but a genuine credit is still suppressed by the strong-income tier.
        assertFalse(TransactionDetector.isLikelySpend("Salary of SGD 5000 transferred to your account"))
    }

    @Test
    fun extracts_currency_token_from_notification_text() {
        assertEquals("S\$", TransactionDetector.extractCurrencyToken("You spent S\$45.20 at STARBUCKS"))
        assertEquals("₹", TransactionDetector.extractCurrencyToken("Debited ₹1,200 for Uber"))
        assertEquals("SGD", TransactionDetector.extractCurrencyToken("SGD 18.39 charged at fp*Food Panda"))
        assertEquals("USD", TransactionDetector.extractCurrencyToken("Transaction Alert: USD 45.00 debited"))
        assertEquals("\$", TransactionDetector.extractCurrencyToken("\$12.99 charged"))
        assertEquals("EUR", TransactionDetector.extractCurrencyToken("EUR 12,50 spent at Cafe"))
    }

    @Test
    fun returns_null_token_when_no_currency_marker_present() {
        assertNull(TransactionDetector.extractCurrencyToken("You spent 50 at Store ref 7829341"))
        assertNull(TransactionDetector.extractCurrencyToken("Purchase 9.99 card ending 1234"))
    }

    @Test
    fun format_captured_amount_symbol_tokens_no_space() {
        assertEquals("S\$45.20", formatCapturedAmount(4520L, "S\$"))
        assertEquals("₹1,200", formatCapturedAmount(120000L, "₹"))   // grouping applied
        assertEquals("₹1,200", formatCapturedAmount(120000L, null, "₹")) // null token falls back to homeSymbol
    }

    @Test
    fun format_captured_amount_iso_code_tokens_have_space() {
        assertEquals("SGD 18.39", formatCapturedAmount(1839L, "SGD"))
        assertEquals("USD 45.20", formatCapturedAmount(4520L, "USD"))
    }

    @Test
    fun format_captured_amount_bare_number_when_no_token_and_no_home_symbol() {
        assertEquals("45", formatCapturedAmount(4500L, null)) // no symbol available
        assertEquals("1,200", formatCapturedAmount(120000L, null)) // grouped even without token
    }

    @Test
    fun extracts_sar_currency_token() {
        assertEquals("SAR", TransactionDetector.extractCurrencyToken("SAR 150.00 debited"))
    }

    @Test
    fun comma_decimal_amount_preferred_over_earlier_bare_integer() {
        // Without the comma-in-withDecimal fix, "9876" would win as matches.first().
        assertEquals(1250L, TransactionDetector.extractAmountMinor("txn 9876 amount 12,50"))
    }

    @Test
    fun marketing_push_with_percent_bonus_is_not_a_spend() {
        // Real-world false positive: an airline-miles promo from a monitored bank app was captured
        // as "Add expense S$25" — the 25 came from "25% bonus" and the 10/19 from "10-19 Jun";
        // nothing in the text is a payment.
        val promo = "25% bonus miles on AirFrance-KLM — Transfer Max Miles to Flying Blue with a " +
            "25% bonus, 10-19 Jun. More miles for Air France, KLM or Transavia flights. Discover more."
        assertFalse(TransactionDetector.isLikelySpend(promo))
        assertNull(TransactionDetector.extractAmountMinor(promo))
    }

    @Test
    fun percentages_dates_and_times_are_not_amounts() {
        assertNull(TransactionDetector.extractAmountMinor("Get 20% off all purchases"))
        assertNull(TransactionDetector.extractAmountMinor("Sale runs 10-19 Jun"))
        assertNull(TransactionDetector.extractAmountMinor("Transfer completed at 16:46"))
        // The same digits as a real amount still extract.
        assertEquals(2000L, TransactionDetector.extractAmountMinor("You spent $20 at Zara"))
    }

    @Test
    fun promo_numbers_without_currency_marker_are_not_spends() {
        assertFalse(TransactionDetector.isLikelySpend("Earn 500 bonus miles with every purchase"))
        // An advertised discount is not a payment, even with a currency symbol.
        assertFalse(TransactionDetector.isLikelySpend("Get S\$20 off your next purchase at Zara"))
        // But a currency-tagged amount wins even when a rewards footer is present.
        val withFooter = "You spent S\$45.20 at STARBUCKS and earned 50 points"
        assertTrue(TransactionDetector.isLikelySpend(withFooter))
        assertEquals(4520L, TransactionDetector.extractAmountMinor(withFooter))
    }

    @Test
    fun keywords_respect_word_boundaries() {
        // "sent" must not fire inside "consent".
        assertFalse(TransactionDetector.isLikelySpend("Please give your consent to proceed with ref 12345"))
        assertTrue(TransactionDetector.isLikelySpend("Sent \$12 to Alex"))
        // "recharge" is a spend in its own right (no longer a substring hit on "charge").
        assertTrue(TransactionDetector.isLikelySpend("Recharge of ₹299 successful"))
    }

    @Test
    fun own_funds_transfer_passes_the_spend_gate() {
        // A real DBS transfer alert: "transfer" must be a spend keyword or this is never captured.
        val dbs = "An Own Funds Transfer of SGD15000.00 from A/C ending 7328 to A/C ending 7596 " +
            "on 09 Jun 16:46 (SGT) was completed. If unauthorised, call +65 63272265."
        assertTrue(TransactionDetector.isLikelySpend(dbs))
    }

    @Test
    fun guesses_merchant() {
        assertEquals("STARBUCKS", TransactionDetector.guessMerchant("You spent \$5 at STARBUCKS on Monday"))
        assertEquals("John", TransactionDetector.guessMerchant("Payment sent to John for lunch"))
    }

    @Test
    fun resolves_ai_category_name_to_user_category() {
        val cats = listOf(
            cat(1, "Food & Dining"), cat(2, "Transport"), cat(3, "Shopping"),
        )
        assertEquals(3L, CategoryMatcher.resolveByName("Shopping", cats))
        assertEquals(1L, CategoryMatcher.resolveByName("food", cats)) // partial, case-insensitive
        assertNull(CategoryMatcher.resolveByName("Healthcare", cats)) // no match
        assertNull(CategoryMatcher.resolveByName(null, cats))
    }

    @Test
    fun category_match_respects_word_boundaries_and_specificity() {
        val cats = listOf(cat(1, "Other"), cat(2, "Personal"), cat(3, "Personal care"))
        // A short category name must not match as a substring inside an unrelated word.
        assertNull(CategoryMatcher.resolveByName("Mother's day gift", listOf(cat(1, "Other"))))
        // The most specific (longest) matching category wins, deterministically.
        assertEquals(3L, CategoryMatcher.resolveByName("Personal care expenses", cats))
    }

    private fun cat(id: Long, name: String) =
        CategoryItem(id = id, name = name, iconKey = "other", colorHex = "#546E7A")
}
