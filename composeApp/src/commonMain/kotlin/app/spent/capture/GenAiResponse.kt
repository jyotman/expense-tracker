package app.spent.capture

/**
 * Tolerant readers for the JSON an on-device LLM emits — it may wrap the object in markdown fences,
 * add prose, quote numbers, or include thousands separators. Pure + testable (the platform extractor
 * in androidMain delegates here so this logic can be covered in commonTest).
 */
object GenAiResponse {

    /** A JSON string field; null when missing, blank, or the literal "null". */
    fun field(json: String, key: String): String? {
        val value = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            .find(json)?.groupValues?.getOrNull(1)?.trim()
        return value?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    /**
     * A JSON number field as a raw string (quoted or bare, optional thousands commas); null if absent.
     * A comma must sit between digit groups — a trailing JSON delimiter (e.g. `"amount": 250,`) is
     * not swallowed into the number.
     */
    fun number(json: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*\"?([0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?)\"?", RegexOption.IGNORE_CASE)
            .find(json)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
