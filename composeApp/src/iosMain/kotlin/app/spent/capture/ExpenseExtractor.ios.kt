package app.spent.capture

// iOS can't read other apps' notifications, so this is never invoked in practice.
actual fun createExpenseExtractor(aiEnabled: Boolean): ExpenseExtractor =
    object : ExpenseExtractor {
        override suspend fun extract(appLabel: String, title: String, text: String): ParsedExpense? = null
    }
