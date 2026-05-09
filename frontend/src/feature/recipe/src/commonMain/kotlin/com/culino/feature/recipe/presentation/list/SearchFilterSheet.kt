package com.culino.feature.recipe.presentation.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.culino.common.ui.component.ChipFlowRow
import com.culino.feature.ingredient.data.Ingredient
import com.culino.feature.ingredient.data.IngredientCategory
import com.culino.feature.ingredient.data.Tag

@Composable
fun SearchFilterContent(
    tags: List<Tag>,
    ingredients: List<Ingredient>,
    categories: List<IngredientCategory>,
    selectedTagIds: List<Int>,
    maxCookingTime: Int?,
    selectedIngredientIds: List<Int>,
    onApply: (tagIds: List<Int>, maxCookingTime: Int?, ingredientIds: List<Int>) -> Unit,
    onReset: () -> Unit,
    dismiss: () -> Unit
) {
    var localTagIds by remember { mutableStateOf(selectedTagIds) }
    var localMaxTime by remember { mutableStateOf(maxCookingTime) }
    var localIngredientIds by remember { mutableStateOf(selectedIngredientIds) }
    var timeSliderValue by remember { mutableStateOf(maxCookingTime?.toFloat() ?: 120f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("筛选条件", style = MaterialTheme.typography.titleMedium)

        if (tags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("标签", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                ChipFlowRow {
                    tags.forEach { tag ->
                        val selected = tag.id in localTagIds
                        FilterChip(
                            selected = selected,
                            onClick = { localTagIds = if (selected) localTagIds - tag.id else localTagIds + tag.id },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("最长烹饪时间", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(if (timeSliderValue >= 120f) "不限" else "${timeSliderValue.toInt()} 分钟", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = timeSliderValue, onValueChange = { timeSliderValue = it; localMaxTime = if (it >= 120f) null else it.toInt() }, valueRange = 10f..120f, steps = 10)
        }

        if (ingredients.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("食材", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                val categoryMap = categories.associateBy { it.id }
                val grouped = ingredients.groupBy { it.categoryId }.entries.sortedBy { (catId, _) -> catId?.let { categoryMap[it]?.sortOrder } ?: Int.MAX_VALUE }
                grouped.forEach { (catId, items) ->
                    val catName = catId?.let { id -> categoryMap[id]?.name } ?: "其他"
                    Text(catName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChipFlowRow(horizontalSpacing = 6, verticalSpacing = 6) {
                        items.forEach { ingredient ->
                            val selected = ingredient.id in localIngredientIds
                            FilterChip(selected = selected, onClick = { localIngredientIds = if (selected) localIngredientIds - ingredient.id else localIngredientIds + ingredient.id }, label = { Text(ingredient.name, style = MaterialTheme.typography.bodySmall) })
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onReset(); dismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("重置") }
            Button(onClick = { onApply(localTagIds, localMaxTime, localIngredientIds); dismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("应用") }
        }
    }
}