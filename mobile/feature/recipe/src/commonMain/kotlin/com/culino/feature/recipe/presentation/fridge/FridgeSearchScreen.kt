package com.culino.feature.recipe.presentation.fridge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.culino.core.ui.component.ChipFlowRow
import com.culino.feature.recipe.data.RecipeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeSearchScreen(
    viewModel: FridgeSearchViewModel,
    onBack: () -> Unit,
    onRecipeClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("冰箱找菜") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.selectedIngredientIds.isNotEmpty()) {
                val loadState = uiState.ingredientsLoadState
                if (loadState is IngredientsLoadState.Success) {
                    val selected = loadState.ingredients.filter { it.id in uiState.selectedIngredientIds }
                    ChipFlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        selected.forEach { ingredient ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.toggleIngredient(ingredient.id) },
                                label = { Text(ingredient.name) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.searchRecipes() },
                    enabled = !uiState.isSearching,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("搜索菜谱（已选 ${uiState.selectedIngredientIds.size} 种食材）")
                }
            }

            val results = uiState.searchResults
            if (results != null) {
                ResultsSection(results, onRecipeClick, onClear = { viewModel.clearResults() })
            } else {
                IngredientSelectionList(uiState, onToggle = { viewModel.toggleIngredient(it) }, onQueryChange = { viewModel.updateSearchQuery(it) })
            }
        }
    }
}

@Composable
private fun ResultsSection(
    results: FridgeResultState,
    onRecipeClick: (String) -> Unit,
    onClear: () -> Unit
) {
    when (results) {
        is FridgeResultState.Success -> {
            if (results.recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("没有找到匹配的菜谱", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("找到 ${results.recipes.size} 个菜谱", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = onClear) { Text("返回选择") }
                        }
                    }
                    items(results.recipes, key = { it.id }) { recipe ->
                        FridgeRecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe.id) })
                    }
                }
            }
        }
        is FridgeResultState.Error -> {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("搜索失败：${results.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientSelectionList(
    uiState: FridgeSearchUiState,
    onToggle: (Int) -> Unit,
    onQueryChange: (String) -> Unit
) {
    when (val loadState = uiState.ingredientsLoadState) {
        is IngredientsLoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is IngredientsLoadState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载失败：${loadState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is IngredientsLoadState.Success -> {
            val query = uiState.ingredientSearchQuery
            val filtered = remember(query, loadState.ingredients) {
                if (query.isBlank()) loadState.ingredients
                else loadState.ingredients.filter { it.name.contains(query, ignoreCase = true) }
            }
            val categoryMap = remember(loadState.categories) { loadState.categories.associateBy { it.id } }
            val grouped = remember(filtered, categoryMap) {
                filtered.groupBy { it.categoryId }
                    .entries.sortedBy { (catId, _) -> catId?.let { categoryMap[it]?.sortOrder } ?: Int.MAX_VALUE }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("搜索食材") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
                grouped.forEach { (catId, items) ->
                    val catName = catId?.let { id -> categoryMap[id]?.name } ?: "其他"
                    item {
                        Text(catName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    }
                    item {
                        ChipFlowRow {
                            items.forEach { ingredient ->
                                FilterChip(
                                    selected = ingredient.id in uiState.selectedIngredientIds,
                                    onClick = { onToggle(ingredient.id) },
                                    label = { Text(ingredient.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FridgeRecipeCard(recipe: RecipeListItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recipe.coverImage != null) {
                AsyncImage(
                    model = recipe.coverImage,
                    contentDescription = recipe.title,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                recipe.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${recipe.cookingTime}分钟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${recipe.servings}人份", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
