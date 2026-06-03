package app.expensetracker.data

/** A category template used to seed a fresh install. iconKey maps to a Material icon in the UI. */
data class CategorySeed(val name: String, val iconKey: String, val colorHex: String)

object DefaultCategories {
    val seeds: List<CategorySeed> = listOf(
        CategorySeed("Food & Dining", "restaurant", "#EF6C00"),
        CategorySeed("Groceries", "groceries", "#2E7D32"),
        CategorySeed("Transport", "transport", "#1565C0"),
        CategorySeed("Shopping", "shopping", "#AD1457"),
        CategorySeed("Bills & Utilities", "bills", "#00838F"),
        CategorySeed("Entertainment", "entertainment", "#6A1B9A"),
        CategorySeed("Health", "health", "#C62828"),
        CategorySeed("Travel", "travel", "#0277BD"),
        CategorySeed("Rent & Home", "home", "#4E342E"),
        CategorySeed("Education", "education", "#283593"),
        CategorySeed("Personal", "personal", "#00695C"),
        CategorySeed("Other", "other", "#546E7A"),
    )

    /** Palette offered when the user creates a custom category. */
    val palette: List<String> = listOf(
        "#EF6C00", "#2E7D32", "#1565C0", "#AD1457", "#00838F", "#6A1B9A",
        "#C62828", "#0277BD", "#4E342E", "#283593", "#00695C", "#546E7A",
        "#F9A825", "#558B2F", "#5E35B1", "#D81B60",
    )

    /** Icon keys offered in the category editor. */
    val iconKeys: List<String> = listOf(
        "restaurant", "groceries", "transport", "shopping", "bills", "entertainment",
        "health", "travel", "home", "education", "personal", "coffee", "fuel", "gift",
        "pets", "fitness", "phone", "other",
    )
}
