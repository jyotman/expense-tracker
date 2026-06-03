@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.expensetracker.AndroidEntry
import app.expensetracker.AppInitializer
import app.expensetracker.ServiceLocator
import app.expensetracker.data.Money
import app.expensetracker.data.MonthPeriod
import kotlinx.datetime.TimeZone

/** Home-screen widget: current month's total spend + a quick "add expense" tap target. */
class ExpenseTrackerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppInitializer.init(context)
        val tz = TimeZone.currentSystemDefault()
        val period = MonthPeriod.current(tz)
        val (start, end) = period.range(tz)
        val total = ServiceLocator.expenseRepository.totalInRange(start, end)
        val symbol = ServiceLocator.settings.currencySymbol

        provideContent {
            GlanceTheme {
                WidgetBody(
                    monthLabel = period.label(),
                    amount = Money.format(total, symbol),
                    context = context,
                )
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = ExpenseTrackerWidget().updateAll(context)
    }
}

@Composable
private fun WidgetBody(monthLabel: String, amount: String, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
            .clickable(actionStartActivity(AndroidEntry.addExpenseIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Spent in $monthLabel",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )
        Text(
            text = amount,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = "＋ Add expense",
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
    }
}
