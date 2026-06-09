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

    @Test
    fun flags_spend_vs_income() {
        assertTrue(TransactionDetector.isLikelySpend("You spent \$45.20 at Tesco"))
        assertTrue(TransactionDetector.isLikelySpend("INR 500 debited for electricity bill"))
        assertFalse(TransactionDetector.isLikelySpend("You received \$500 salary"))
        assertFalse(TransactionDetector.isLikelySpend("Refund of \$20 credited"))
        assertFalse(TransactionDetector.isLikelySpend("Your OTP is 4521"))
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
