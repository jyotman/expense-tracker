package app.expensetracker.capture

import app.expensetracker.data.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenAiResponseTest {

    @Test
    fun parses_clean_json() {
        val json = """{"amount": 250.00, "merchant": "BLUE TOKAI", "category": "Food & Dining"}"""
        assertEquals("250.00", GenAiResponse.number(json, "amount"))
        assertEquals("BLUE TOKAI", GenAiResponse.field(json, "merchant"))
        assertEquals("Food & Dining", GenAiResponse.field(json, "category"))
    }

    @Test
    fun tolerates_markdown_fences_and_prose() {
        val json = """
            Sure, here is the data:
            ```json
            { "amount" : "1,250.50", "merchant": "AMAZON", "category": "Shopping" }
            ```
        """.trimIndent()
        assertEquals("1,250.50", GenAiResponse.number(json, "amount"))
        assertEquals("AMAZON", GenAiResponse.field(json, "merchant"))
        assertEquals("Shopping", GenAiResponse.field(json, "category"))
    }

    @Test
    fun handles_bare_quoted_and_grouped_numbers() {
        assertEquals("89", GenAiResponse.number("""{"amount": 89}""", "amount"))
        assertEquals("89.00", GenAiResponse.number("""{"amount":"89.00"}""", "amount"))
        assertEquals("47,300.00", GenAiResponse.number("""{"amount": 47,300.00}""", "amount"))
    }

    @Test
    fun amount_string_converts_to_minor_units() {
        val json = """{"amount": "1,250.50"}"""
        assertEquals(125050L, GenAiResponse.number(json, "amount")?.let { Money.parseToMinor(it) })
    }

    @Test
    fun treats_null_blank_and_missing_as_absent() {
        assertNull(GenAiResponse.field("""{"merchant": null}""", "merchant"))
        assertNull(GenAiResponse.field("""{"merchant": ""}""", "merchant"))
        assertNull(GenAiResponse.field("""{"amount": 10}""", "merchant"))
        assertNull(GenAiResponse.number("""{"merchant": "x"}""", "amount"))
    }

    @Test
    fun ignores_non_amount_numbers_in_other_fields() {
        // The balance lives in "note", not "amount" — number("amount") must not pick it up.
        val json = """{"amount": 250, "note": "Avbl bal 47300"}"""
        assertEquals("250", GenAiResponse.number(json, "amount"))
    }
}
