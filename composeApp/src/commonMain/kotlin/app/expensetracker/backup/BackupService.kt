package app.expensetracker.backup

import app.expensetracker.db.Database
import app.expensetracker.db.ExpenseTrackerDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAtMillis: Long,
    val categories: List<CategoryDto>,
    val expenses: List<ExpenseDto>,
    val recurring: List<RecurringDto>,
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Long,
    val isArchived: Long,
)

@Serializable
data class ExpenseDto(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long?,
    val note: String?,
    val merchant: String?,
    val occurredAt: Long,
    val createdAt: Long,
    val source: String,
    val recurringRuleId: Long?,
    val sourceNotificationText: String? = null,
)

@Serializable
data class RecurringDto(
    val id: Long,
    val amountMinor: Long,
    val categoryId: Long?,
    val note: String?,
    val merchant: String?,
    val intervalUnit: String,
    val intervalCount: Long,
    val startAt: Long,
    val endAt: Long?,
    val lastGeneratedAt: Long?,
    val isActive: Long,
)

/** Serializes/Restores the whole local database as a single JSON document. */
object BackupService {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun export(nowMillis: Long, db: ExpenseTrackerDatabase = Database.instance): String {
        val categories = db.categoryQueries.selectAll().executeAsList().map {
            CategoryDto(it.id, it.name, it.iconKey, it.colorHex, it.sortOrder, it.isArchived)
        }
        val expenses = db.expenseQueries.selectAllForExport().executeAsList().map {
            ExpenseDto(it.id, it.amountMinor, it.categoryId, it.note, it.merchant, it.occurredAt, it.createdAt, it.source, it.recurringRuleId, it.sourceNotificationText)
        }
        val recurring = db.recurringRuleQueries.selectAll().executeAsList().map {
            RecurringDto(it.id, it.amountMinor, it.categoryId, it.note, it.merchant, it.intervalUnit, it.intervalCount, it.startAt, it.endAt, it.lastGeneratedAt, it.isActive)
        }
        return json.encodeToString(
            BackupData(
                exportedAtMillis = nowMillis,
                categories = categories,
                expenses = expenses,
                recurring = recurring,
            )
        )
    }

    fun restore(payload: String, db: ExpenseTrackerDatabase = Database.instance) {
        val data = json.decodeFromString<BackupData>(payload)
        db.transaction {
            db.expenseQueries.deleteAll()
            db.recurringRuleQueries.deleteAll()
            db.categoryQueries.deleteAll()
            data.categories.forEach {
                db.categoryQueries.insertWithId(it.id, it.name, it.iconKey, it.colorHex, it.sortOrder, it.isArchived)
            }
            data.recurring.forEach {
                db.recurringRuleQueries.insertWithId(
                    it.id, it.amountMinor, it.categoryId, it.note, it.merchant,
                    it.intervalUnit, it.intervalCount, it.startAt, it.endAt, it.lastGeneratedAt, it.isActive,
                )
            }
            data.expenses.forEach {
                db.expenseQueries.insertWithId(
                    it.id, it.amountMinor, it.categoryId, it.note, it.merchant,
                    it.occurredAt, it.createdAt, it.source, it.recurringRuleId, it.sourceNotificationText,
                )
            }
        }
    }
}
