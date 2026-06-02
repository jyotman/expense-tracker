package app.spent.capture

import app.spent.data.CategoryItem

/**
 * Resolves a category *name* (as suggested by the on-device AI in the foreground) to one of the
 * user's actual categories. We deliberately do NOT guess categories from keywords/merchants in the
 * background regex path — category is only ever AI-suggested in the foreground or chosen by the user.
 */
object CategoryMatcher {
    fun resolveByName(name: String?, categories: List<CategoryItem>): Long? {
        val guess = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val active = categories.filter { !it.isArchived }
        active.firstOrNull { it.name.equals(guess, ignoreCase = true) }?.let { return it.id }
        return active.firstOrNull {
            it.name.contains(guess, ignoreCase = true) || guess.contains(it.name, ignoreCase = true)
        }?.id
    }
}
