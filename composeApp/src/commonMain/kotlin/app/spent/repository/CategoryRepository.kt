package app.spent.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.spent.data.CategoryItem
import app.spent.data.DefaultCategories
import app.spent.db.Database
import app.spent.db.SpentDatabase
import app.spent.db.Category as DbCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val db: SpentDatabase = Database.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries get() = db.categoryQueries

    /** Insert the default category set on first launch (no-op afterwards). */
    fun ensureSeeded() {
        if (queries.countAll().executeAsOne() > 0L) return
        db.transaction {
            DefaultCategories.seeds.forEachIndexed { index, seed ->
                queries.insert(seed.name, seed.iconKey, seed.colorHex, index.toLong())
            }
        }
    }

    fun observeActive(): Flow<List<CategoryItem>> =
        queries.selectActive().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toItem() } }

    fun observeAll(): Flow<List<CategoryItem>> =
        queries.selectAll().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toItem() } }

    fun observeById(id: Long): Flow<CategoryItem?> =
        queries.selectById(id).asFlow().mapToOneOrNull(dispatcher).map { it?.toItem() }

    fun getById(id: Long): CategoryItem? = queries.selectById(id).executeAsOneOrNull()?.toItem()

    fun listActive(): List<CategoryItem> = queries.selectActive().executeAsList().map { it.toItem() }

    fun add(name: String, iconKey: String, colorHex: String): Long {
        val nextOrder = (queries.countAll().executeAsOne())
        queries.insert(name, iconKey, colorHex, nextOrder)
        return queries.lastInsertedId().executeAsOne()
    }

    fun update(item: CategoryItem) {
        queries.update(item.name, item.iconKey, item.colorHex, item.sortOrder, item.id)
    }

    fun setArchived(id: Long, archived: Boolean) {
        queries.setArchived(if (archived) 1L else 0L, id)
    }

    fun delete(id: Long) = queries.deleteById(id)
}

private fun DbCategory.toItem() = CategoryItem(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isArchived = isArchived == 1L,
)
