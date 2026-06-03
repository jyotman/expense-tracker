package app.expensetracker.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Hosts [ExpenseTrackerWidget]. Lives in the app module so it is always packaged (see the notification
 *  listener for the same reason). */
class ExpenseTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseTrackerWidget()
}
