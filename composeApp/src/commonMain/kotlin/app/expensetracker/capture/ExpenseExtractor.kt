package app.expensetracker.capture

/** Structured result of parsing a payment notification. */
data class ParsedExpense(
    val amountMinor: Long,
    val merchant: String? = null,
    /** Free-text category name the model guessed (e.g. "Food"); resolved later to a real category. */
    val categoryGuess: String? = null,
    /** ISO 4217 code of the amount's currency, when the model could infer it; null otherwise. */
    val currencyCode: String? = null,
)

/**
 * Turns a payment notification's text into a [ParsedExpense]. The Android implementation uses
 * on-device Gemini Nano (with a rules fallback); iOS returns null (no notification access).
 */
interface ExpenseExtractor {
    suspend fun extract(appLabel: String, title: String, text: String): ParsedExpense?
}

/** Platform factory. [aiEnabled] toggles on-device LLM use where available. */
expect fun createExpenseExtractor(aiEnabled: Boolean): ExpenseExtractor
