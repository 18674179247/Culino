package com.culino.feature.ingredient.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.culino.feature.ingredient.data.Ingredient
import com.culino.feature.ingredient.data.IngredientCategory
import com.culino.feature.ingredient.data.Seasoning

data class SelectedIngredient(
    val id: Int,
    val name: String,
    val defaultUnit: String?
)

data class SelectedSeasoning(
    val id: Int,
    val name: String,
    val defaultUnit: String?
)

@Composable
fun IngredientSelectorContent(
    ingredients: List<Ingredient>,
    categories: List<IngredientCategory>,
    onSelect: (SelectedIngredient) -> Unit,
    onCustom: (String) -> Unit,
    dismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            debouncedQuery = ""
        } else {
            kotlinx.coroutines.delay(200L)
            debouncedQuery = query
        }
    }

    val filtered = remember(debouncedQuery, ingredients) {
        if (debouncedQuery.isBlank()) ingredients
        else ingredients.filter { it.name.contains(debouncedQuery, ignoreCase = true) }
    }

    val grouped = remember(filtered, categories) {
        val categoryMap = categories.associateBy { it.id }
        val groups = filtered.groupBy { it.categoryId }
        groups.entries
            .sortedBy { (catId, _) -> catId?.let { categoryMap[it]?.sortOrder } ?: Int.MAX_VALUE }
            .map { (catId, items) ->
                val catName = catId?.let { id -> categoryMap[id]?.name } ?: "其他"
                catName to items
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "选择食材",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索食材") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            if (query.isNotBlank() && filtered.isEmpty()) {
                item {
                    TextButton(
                        onClick = {
                            onCustom(query)
                            dismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用自定义食材「$query」")
                    }
                }
            }

            grouped.forEach { pair ->
                val categoryName = pair.first
                val categoryItems = pair.second
                item {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(categoryItems, key = { it.id }) { ingredient ->
                    ListItem(
                        headlineContent = { Text(ingredient.name) },
                        supportingContent = if (ingredient.unit != null) {
                            { Text(ingredient.unit) }
                        } else null,
                        modifier = Modifier.clickable {
                            onSelect(SelectedIngredient(ingredient.id, ingredient.name, ingredient.unit))
                            dismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SeasoningSelectorContent(
    seasonings: List<Seasoning>,
    onSelect: (SelectedSeasoning) -> Unit,
    onCustom: (String) -> Unit,
    dismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, seasonings) {
        if (query.isBlank()) seasonings
        else seasonings.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "选择调料",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索调料") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            if (query.isNotBlank() && filtered.isEmpty()) {
                item {
                    TextButton(
                        onClick = {
                            onCustom(query)
                            dismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用自定义调料「$query」")
                    }
                }
            }

            items(filtered, key = { it.id }) { seasoning ->
                ListItem(
                    headlineContent = { Text(seasoning.name) },
                    supportingContent = if (seasoning.unit != null) {
                        { Text(seasoning.unit) }
                    } else null,
                    modifier = Modifier.clickable {
                        onSelect(SelectedSeasoning(seasoning.id, seasoning.name, seasoning.unit))
                        dismiss()
                    }
                )
            }
        }
    }
}
