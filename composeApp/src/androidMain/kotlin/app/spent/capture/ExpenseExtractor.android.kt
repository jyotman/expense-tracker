package app.spent.capture

import app.spent.ServiceLocator
import app.spent.data.Money
import co.touchlab.kermit.Logger
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

actual fun createExpenseExtractor(aiEnabled: Boolean): ExpenseExtractor =
    if (aiEnabled) MlKitGenAiExpenseExtractor() else RulesExpenseExtractor()

/**
 * On-device extraction with Gemini Nano via the ML Kit GenAI Prompt API. When AI is on and the model
 * is ready, Nano reads the raw notification and extracts amount + merchant + category itself — we do
 * NOT pre-fill from regex in this path (the caller shows a loading state until this returns). Returns
 * null when the model isn't available or inference fails, so the caller can fall back to regex.
 * Foreground-only — Gemini Nano can't run in the background.
 */
class MlKitGenAiExpenseExtractor : ExpenseExtractor {
    private val log = Logger.withTag("MlKitGenAi")

    override suspend fun extract(appLabel: String, title: String, text: String): ParsedExpense? {
        val model = runCatching { Generation.getClient() }.getOrNull() ?: return null
        return try {
            if (model.checkStatus() != FeatureStatus.AVAILABLE) return null // not ready → caller uses regex

            val categories = ServiceLocator.categoryRepository.listActive().joinToString(", ") { it.name }
            val request = generateContentRequest(TextPart(buildPrompt(appLabel, title, text, categories))) {
                temperature = 0.0f // deterministic extraction, not creative writing
                topK = 1
                seed = 0
                candidateCount = 1
                maxOutputTokens = 128
            }
            val output = runCatching { model.generateContent(request).candidates.firstOrNull()?.text }
                .onFailure { log.w(it) { "Nano inference failed" } }
                .getOrNull()
            if (output.isNullOrBlank()) return null

            val amountMinor = GenAiResponse.number(output, "amount")
                ?.let { Money.parseToMinor(it) }
                ?.takeIf { it > 0 }
                ?: return null // no usable amount → fall back to regex

            ParsedExpense(
                amountMinor = amountMinor,
                merchant = GenAiResponse.field(output, "merchant"),
                categoryGuess = GenAiResponse.field(output, "category"),
            )
        } catch (e: Throwable) {
            log.w(e) { "GenAI extract failed" }
            null
        } finally {
            runCatching { model.close() }
        }
    }

    private fun buildPrompt(appLabel: String, title: String, text: String, categories: String): String = """
        Extract the single spending transaction from this payment notification.
        Reply with ONLY compact JSON, no prose or markdown:
        {"amount": <number actually spent, digits only>, "merchant": <store/payee name or null>, "category": <one of: $categories — or null if unsure>}
        Only the amount that actually left the account — ignore available balance, limits, and remaining funds.
        App: $appLabel
        Notification: "${title.take(160)} — ${text.take(400)}"
    """.trimIndent()
}
