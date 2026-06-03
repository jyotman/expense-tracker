package app.expensetracker.capture

/** Regex/keyword extractor — fast, deterministic, no model required. */
class RulesExpenseExtractor : ExpenseExtractor {
    override suspend fun extract(appLabel: String, title: String, text: String): ParsedExpense? {
        val combined = listOf(title, text).filter { it.isNotBlank() }.joinToString(" — ")
        if (!TransactionDetector.isLikelySpend(combined)) return null
        val amount = TransactionDetector.extractAmountMinor(combined) ?: return null
        return ParsedExpense(
            amountMinor = amount,
            merchant = TransactionDetector.guessMerchant(combined),
            categoryGuess = null,
        )
    }
}
