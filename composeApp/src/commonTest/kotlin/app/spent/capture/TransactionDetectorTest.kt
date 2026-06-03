package app.spent.capture

import app.spent.data.CategoryItem
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
