package app.expensetracker

import android.content.Context
import android.content.Intent
import app.expensetracker.navigation.DeepLink

/** Builds and parses the Intents that the widget and capture notifications use to open the app. */
object AndroidEntry {
    const val ACTION_ADD = "app.expensetracker.action.ADD_EXPENSE"
    const val ACTION_OPEN_CAPTURED = "app.expensetracker.action.OPEN_CAPTURED"
    const val ACTION_OPEN_INBOX = "app.expensetracker.action.OPEN_INBOX"
    const val EXTRA_CAPTURED_ID = "captured_id"

    private const val MAIN_ACTIVITY = "app.expensetracker.MainActivity"

    fun addExpenseIntent(context: Context): Intent = base(context, ACTION_ADD)

    fun openCapturedIntent(context: Context, capturedId: Long): Intent =
        base(context, ACTION_OPEN_CAPTURED).putExtra(EXTRA_CAPTURED_ID, capturedId)

    fun openInboxIntent(context: Context): Intent = base(context, ACTION_OPEN_INBOX)

    private fun base(context: Context, action: String): Intent =
        Intent(action).apply {
            setClassName(context.packageName, MAIN_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    /** Translate a launch Intent into a navigation deep link, or null for a normal launch. */
    fun parse(intent: Intent?): DeepLink? = when (intent?.action) {
        ACTION_ADD -> DeepLink.AddExpense
        ACTION_OPEN_INBOX -> DeepLink.OpenInbox
        ACTION_OPEN_CAPTURED -> {
            val id = intent.getLongExtra(EXTRA_CAPTURED_ID, -1L)
            if (id > 0) DeepLink.OpenCaptured(id) else null
        }
        else -> null
    }
}
