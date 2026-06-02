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

    private fun cat(id: Long, name: String) =
        CategoryItem(id = id, name = name, iconKey = "other", colorHex = "#546E7A")
}
