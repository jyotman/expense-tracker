@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package app.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.expensetracker.data.CategoryItem
import app.expensetracker.data.DefaultCategories
import app.expensetracker.ui.colorFromHex
import app.expensetracker.ui.iconForKey
import app.expensetracker.viewmodel.CategoriesViewModel

@Composable
fun CategoryEditScreen(
    categoryId: Long?,
    onDone: () -> Unit,
) {
    val vm: CategoriesViewModel = viewModel { CategoriesViewModel() }
    val existing = remember(categoryId) { categoryId?.let { vm.getById(it) } }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: DefaultCategories.iconKeys.first()) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: DefaultCategories.palette.first()) }
    var archived by remember { mutableStateOf(existing?.isArchived ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit category" else "New category") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            if (existing != null) {
                                vm.update(
                                    CategoryItem(
                                        id = existing.id,
                                        name = name,
                                        iconKey = iconKey,
                                        colorHex = colorHex,
                                        sortOrder = existing.sortOrder,
                                        isArchived = archived,
                                    )
                                )
                            } else {
                                vm.add(name, iconKey, colorHex)
                            }
                            onDone()
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            Text("Icon", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DefaultCategories.iconKeys.forEach { key ->
                    val selected = key == iconKey
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colorFromHex(colorHex).copy(alpha = if (selected) 0.30f else 0.12f))
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) colorFromHex(colorHex) else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { iconKey = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconForKey(key), contentDescription = key, tint = colorFromHex(colorHex), modifier = Modifier.size(22.dp))
                    }
                }
            }

            Text("Color", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DefaultCategories.palette.forEach { hex ->
                    val selected = hex == colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorFromHex(hex))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            // Black or white check, whichever reads against the swatch colour.
                            val checkTint = if (colorFromHex(hex).luminance() > 0.5f) Color.Black else Color.White
                            Icon(Icons.Filled.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (existing != null) {
                // Toggles local state only — like every other field, it's applied on Save.
                OutlinedButton(onClick = { archived = !archived }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (archived) "Unarchive category" else "Archive category")
                }
                if (archived != existing.isArchived) {
                    Text(
                        if (archived) "Will be hidden from new expenses after you save."
                        else "Will be available again after you save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
