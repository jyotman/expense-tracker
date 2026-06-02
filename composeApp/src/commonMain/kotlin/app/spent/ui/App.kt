package app.spent.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.spent.navigation.DeepLink
import app.spent.navigation.DeepLinks
import app.spent.navigation.Routes
import app.spent.ui.screens.BackupScreen
import app.spent.ui.screens.CategoryEditScreen
import app.spent.ui.screens.CategoriesScreen
import app.spent.ui.screens.ExpenseFormScreen
import app.spent.ui.screens.InboxScreen
import app.spent.ui.screens.MainScreen
import app.spent.ui.screens.NotificationCaptureScreen
import app.spent.ui.screens.RecurringScreen
import app.spent.ui.theme.AppTheme

@Composable
fun App(onThemeModeChanged: (Boolean) -> Unit = {}) {
    val nav = rememberNavController()
    val darkTheme = isSystemInDarkTheme()

    LaunchedEffect(darkTheme) { onThemeModeChanged(darkTheme) }

    // Objects passed between destinations without serialising into route args.
    var editingExpenseId by remember { mutableStateOf<Long?>(null) }
    var capturedId by remember { mutableStateOf<Long?>(null) }
    var editingCategoryId by remember { mutableStateOf<Long?>(null) }

    fun openCaptured(id: Long) {
        editingExpenseId = null; capturedId = id
        nav.navigate(Routes.EXPENSE_FORM)
    }

    // React to launches from the widget / capture push / inbox.
    LaunchedEffect(Unit) {
        DeepLinks.events.collect { link ->
            when (link) {
                DeepLink.AddExpense -> {
                    editingExpenseId = null; capturedId = null
                    nav.navigate(Routes.EXPENSE_FORM)
                }
                is DeepLink.OpenCaptured -> openCaptured(link.capturedId)
                DeepLink.OpenInbox -> nav.navigate(Routes.INBOX)
            }
            DeepLinks.clear()
        }
    }

    AppTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = nav, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) {
                    MainScreen(
                        onAddExpense = {
                            editingExpenseId = null; capturedId = null
                            nav.navigate(Routes.EXPENSE_FORM)
                        },
                        onEditExpense = { id ->
                            editingExpenseId = id; capturedId = null
                            nav.navigate(Routes.EXPENSE_FORM)
                        },
                        onOpenInbox = { nav.navigate(Routes.INBOX) },
                        onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                        onOpenRecurring = { nav.navigate(Routes.RECURRING) },
                        onOpenBackup = { nav.navigate(Routes.BACKUP) },
                        onOpenNotificationCapture = { nav.navigate(Routes.NOTIFICATION_CAPTURE) },
                    )
                }
                composable(Routes.EXPENSE_FORM) {
                    ExpenseFormScreen(
                        expenseId = editingExpenseId,
                        capturedId = capturedId,
                        onDone = { nav.popBackStack() },
                    )
                }
                composable(Routes.INBOX) {
                    InboxScreen(
                        onBack = { nav.popBackStack() },
                        onOpenCaptured = { id -> openCaptured(id) },
                    )
                }
                composable(Routes.CATEGORIES) {
                    CategoriesScreen(
                        onBack = { nav.popBackStack() },
                        onAddCategory = { editingCategoryId = null; nav.navigate(Routes.CATEGORY_EDIT) },
                        onEditCategory = { id -> editingCategoryId = id; nav.navigate(Routes.CATEGORY_EDIT) },
                    )
                }
                composable(Routes.CATEGORY_EDIT) {
                    CategoryEditScreen(
                        categoryId = editingCategoryId,
                        onDone = { nav.popBackStack() },
                    )
                }
                composable(Routes.RECURRING) {
                    RecurringScreen(onBack = { nav.popBackStack() })
                }
                composable(Routes.BACKUP) {
                    BackupScreen(onBack = { nav.popBackStack() })
                }
                composable(Routes.NOTIFICATION_CAPTURE) {
                    NotificationCaptureScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
