@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    kotlin.time.ExperimentalTime::class,
)

package app.spent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.spent.data.IntervalUnit
import app.spent.util.DateFormat
import app.spent.viewmodel.ExpenseFormViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun ExpenseFormScreen(
    expenseId: Long?,
    capturedId: Long?,
    onDone: () -> Unit,
) {
    val vm: ExpenseFormViewModel = viewModel { ExpenseFormViewModel() }
    val state by vm.state.collectAsState()

    LaunchedEffect(expenseId, capturedId) {
        when {
            capturedId != null -> vm.loadCaptured(capturedId)
            expenseId != null -> vm.loadExpense(expenseId)
        }
    }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    var showDatePicker by remember { mutableStateOf(false) }
    val tz = TimeZone.currentSystemDefault()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit expense" else "Add expense") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                },
                actions = {
                    TextButton(onClick = vm::save) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::setAmount,
                label = { Text("Amount") },
                prefix = { Text(state.currencySymbol) },
                isError = state.amountError,
                trailingIcon = aiFieldTrailing(loading = state.aiSuggesting, suggested = state.aiSuggestedAmount),
                supportingText = if (state.amountError) {
                    { Text("Enter a valid amount") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().aiShimmer(state.aiSuggesting).aiReveal(state.aiSuggestedAmount),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                if (state.aiSuggesting) AiSparkleLoading()
                else if (state.aiSuggestedCategory) AiBadge()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.aiShimmer(state.aiSuggesting).aiReveal(state.aiSuggestedCategory),
            ) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.categoryId == category.id,
                        onClick = {
                            vm.setCategory(if (state.categoryId == category.id) null else category.id)
                        },
                        label = { Text(category.name) },
                    )
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Date: ${DateFormat.full(state.occurredAt)}")
            }

            OutlinedTextField(
                value = state.merchant,
                onValueChange = vm::setMerchant,
                label = { Text("Merchant (optional)") },
                trailingIcon = aiFieldTrailing(loading = state.aiSuggesting, suggested = state.aiSuggestedMerchant),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().aiShimmer(state.aiSuggesting).aiReveal(state.aiSuggestedMerchant),
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.recurringRuleId != null) {
                // This expense was generated by a recurring rule — recurrence is managed on the rule.
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Part of a recurring series", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Manage it in Settings › Recurring expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Add, or editing a one-off expense: offer to repeat it (a forward-only series on edit).
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Repeat", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.recurring, onCheckedChange = vm::setRecurring)
                }
                if (state.recurring) {
                    if (state.isEdit) {
                        Text(
                            "Keeps this expense and starts a series from the next one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.intervalCount,
                            onValueChange = vm::setIntervalCount,
                            label = { Text("Every") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IntervalUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = state.intervalUnit == unit,
                                    onClick = { vm.setIntervalUnit(unit) },
                                    label = { Text(unit.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                )
                            }
                        }
                    }
                }
            }

            if (state.isEdit) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = vm::delete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = state.occurredAt.toEpochMilliseconds()
        val pickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // DatePicker returns a UTC midnight; map that calendar date to local start-of-day.
                        val utcDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        vm.setOccurredAt(utcDate.atStartOfDayIn(tz))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Trailing slot for an AI-fillable field: a twinkling sparkle while Nano reads, then a steady badge. */
private fun aiFieldTrailing(loading: Boolean, suggested: Boolean): (@Composable () -> Unit)? = when {
    loading -> { { AiSparkleLoading() } }
    suggested -> { { AiBadge() } }
    else -> null
}

/** The "AI is thinking" loader: the sparkle twinkles — pulsing scale, alpha, and a gentle wobble. */
@Composable
private fun AiSparkleLoading() {
    val transition = rememberInfiniteTransition(label = "aiSparkle")
    val scale by transition.animateFloat(
        initialValue = 0.7f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse), label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse), label = "alpha",
    )
    val wobble by transition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse), label = "wobble",
    )
    Icon(
        imageVector = Icons.Filled.AutoAwesome,
        contentDescription = "Reading with AI",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp).graphicsLayer {
            scaleX = scale; scaleY = scale; this.alpha = alpha; rotationZ = wobble
        },
    )
}

/** Small steady sparkle marking a value as AI-suggested; fades + scales in when it appears. */
@Composable
private fun AiBadge() {
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = appear, enter = fadeIn() + scaleIn()) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "Suggested by AI",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A soft accent band that sweeps across a field while AI fills it — the "generating" shimmer. */
@Composable
private fun Modifier.aiShimmer(active: Boolean): Modifier {
    val transition = rememberInfiniteTransition(label = "aiShimmer")
    val x by transition.animateFloat(
        initialValue = -0.4f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x",
    )
    val color = MaterialTheme.colorScheme.primary
    return if (!active) this else this.drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = 0.18f), Color.Transparent),
                startX = (x - 0.4f) * w,
                endX = (x + 0.4f) * w,
            ),
        )
    }
}

/** A one-shot accent highlight behind a field that fades out — the "AI just filled this" pulse. */
@Composable
private fun Modifier.aiReveal(active: Boolean): Modifier {
    val highlight = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            highlight.snapTo(1f)
            highlight.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 900))
        }
    }
    val color = MaterialTheme.colorScheme.primary
    return this.drawBehind {
        if (highlight.value > 0f) {
            drawRoundRect(
                color = color.copy(alpha = 0.16f * highlight.value),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            )
        }
    }
}
