package app.expensetracker.capture

import app.expensetracker.data.CategoryItem

/**
 * Resolves a category *name* (as suggested by the on-device AI in the foreground) to one of the
 * user's actual categories. We deliberately do NOT guess categories from keywords/merchants in the
 * background regex path — category is only ever AI-suggested in the foreground or chosen by the user.
 */
object CategoryMatcher {
    fun resolveByName(name: String?, categories: List<CategoryItem>): Long? {
        val guess = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val active = categories.filter { !it.isArchived }
        // 1. Exact, case-insensitive.
        active.firstOrNull { it.name.equals(guess, ignoreCase = true) }?.let { return it.id }
        // 2. Whole-word containment in either direction, most specific (longest name) first — so
        //    "Personal care" beats "Personal" and a short name like "Other" doesn't swallow
        //    unrelated guesses like "Mother's day gift".
        val lowerGuess = guess.lowercase()
        return active
            .filter { containsWord(it.name.lowercase(), lowerGuess) || containsWord(lowerGuess, it.name.lowercase()) }
            .maxByOrNull { it.name.length }
            ?.id
    }

    /** True if [needle] appears in [haystack] bounded by non-alphanumeric characters (whole word). */
    private fun containsWord(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        var idx = haystack.indexOf(needle)
        while (idx >= 0) {
            val before = idx == 0 || !haystack[idx - 1].isLetterOrDigit()
            val end = idx + needle.length
            val after = end == haystack.length || !haystack[end].isLetterOrDigit()
            if (before && after) return true
            idx = haystack.indexOf(needle, idx + 1)
        }
        return false
    }
}
