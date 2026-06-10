package app.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.data.Money
import app.expensetracker.ui.colorFromHex
import app.expensetracker.ui.components.CategoryAvatar
import app.expensetracker.ui.components.EmptyState
import app.expensetracker.ui.components.MonthSelector
import app.expensetracker.viewmodel.PeriodController
import app.expensetracker.viewmodel.ReportsViewModel
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.columnSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.common.ProvideVicoTheme
import com.patrykandpatrick.vico.multiplatform.common.VicoTheme
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore

@Composable
fun ReportsScreen() {
    val vm: ReportsViewModel = viewModel { ReportsViewModel() }
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Last 12 months",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        MonthlySpendChart(
            values = state.monthly.map { it.totalMinor / 100.0 },
            labels = state.monthly.map { it.period.shortLabel() },
            symbol = state.currencySymbol,
            modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 12.dp),
        )

        MonthSelector(
            period = state.period,
            onPrev = { PeriodController.prev() },
            onNext = { PeriodController.next() },
            onToday = { PeriodController.reset() },
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Breakdown · ${Money.format(state.totalMinor, state.currencySymbol)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        if (state.breakdown.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.BarChart,
                title = "No data for this month",
                subtitle = "Expenses logged in this month will show up in the breakdown.",
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            val total = state.totalMinor.coerceAtLeast(1)
            state.breakdown.forEach { spend ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryAvatar(spend.category)
                    Text(
                        spend.category?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(spend.totalMinor * 100 / total)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Money.format(spend.totalMinor, state.currencySymbol),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthlySpendChart(
    values: List<Double>,
    labels: List<String>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val theme = remember(scheme) {
        VicoTheme(
            candlestickCartesianLayerColors = VicoTheme.CandlestickCartesianLayerColors(
                bullish = scheme.primary, neutral = scheme.outlineVariant, bearish = scheme.error,
            ),
            columnCartesianLayerColors = listOf(scheme.primary),
            lineColor = scheme.outlineVariant,
            textColor = scheme.onSurface,
        )
    }
    val labelKey = remember { ExtraStore.Key<List<String>>() }

    ProvideVicoTheme(theme) {
        val modelProducer = remember { CartesianChartModelProducer() }
        LaunchedEffect(values, labels) {
            modelProducer.runTransaction {
                columnSeries { series(values) }
                extras { it[labelKey] = labels }
            }
        }
        CartesianChartHost(
            rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = CartesianValueFormatter { _, value, _ ->
                        Money.formatShort((value * 100).toLong(), symbol)
                    },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = CartesianValueFormatter { context, x, _ ->
                        context.model.extraStore[labelKey].getOrElse(x.toInt()) { "" }
                    },
                ),
            ),
            modelProducer,
            modifier = modifier,
        )
    }
}
