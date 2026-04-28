package com.menu.feature.recipe.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.menu.feature.recipe.data.RecipeDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel,
    currentUserId: String? = null
) {
    val state by viewModel.state.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val favoriteState by viewModel.favoriteState.collectAsState()
    val isFavorited by viewModel.isFavorited.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        viewModel.loadRecipeDetail(recipeId)
    }

    // 监听删除状态
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                viewModel.resetDeleteState()
                onBack()
            }
            else -> {}
        }
    }

    // 监听收藏状态
    LaunchedEffect(favoriteState) {
        when (favoriteState) {
            is FavoriteState.Success -> {
                viewModel.resetFavoriteState()
            }
            else -> {}
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Error -> {
                snackbarHostState.showSnackbar((deleteState as DeleteState.Error).message)
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }
    LaunchedEffect(favoriteState) {
        when (favoriteState) {
            is FavoriteState.Error -> {
                snackbarHostState.showSnackbar((favoriteState as FavoriteState.Error).message)
                viewModel.resetFavoriteState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("菜谱详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 收藏按钮
                    if (state is RecipeDetailState.Success) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(recipeId) },
                            enabled = favoriteState !is FavoriteState.Loading
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorited) "取消收藏" else "收藏",
                                tint = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 删除按钮（只对自己创建的菜谱显示）
                    val detail = (state as? RecipeDetailState.Success)?.detail
                    if (detail != null && currentUserId != null && detail.recipe.authorId == currentUserId) {
                        if (deleteState is DeleteState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (showDeleteDialog) {
                            // 确认状态：显示红色文字按钮
                            TextButton(onClick = {
                                showDeleteDialog = false
                                viewModel.deleteRecipe(recipeId)
                            }) {
                                Text("确认删除", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("取消")
                            }
                        } else {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除菜谱")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val currentState = state) {
            is RecipeDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecipeDetailState.Success -> {
                RecipeDetailContent(
                    detail = currentState.detail,
                    modifier = Modifier.padding(padding)
                )
            }

            is RecipeDetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currentState.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadRecipeDetail(recipeId) }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailContent(
    detail: RecipeDetail,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = detail.recipe.title,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        detail.recipe.description?.let { desc ->
            item {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip("难度: ${detail.recipe.difficulty}")
                InfoChip("${detail.recipe.cookingTime}分钟")
                InfoChip("${detail.recipe.servings}人份")
            }
        }

        if (detail.ingredients.isNotEmpty()) {
            item {
                Text(
                    text = "食材",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(detail.ingredients) { ingredient ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ingredient.ingredientName)
                    Text(ingredient.amount, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (detail.seasonings.isNotEmpty()) {
            item {
                Text(
                    text = "调料",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(detail.seasonings) { seasoning ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(seasoning.seasoningName)
                    Text(seasoning.amount, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (detail.steps.isNotEmpty()) {
            item {
                Text(
                    text = "步骤",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(detail.steps) { step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "步骤 ${step.stepNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.content)
                        step.duration?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "约 $it 分钟",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        detail.nutrition?.let { nutrition ->
            item {
                Text(
                    text = "营养信息",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        NutritionRow("热量", "${nutrition.calories} kcal")
                        NutritionRow("蛋白质", "${nutrition.protein}g")
                        NutritionRow("脂肪", "${nutrition.fat}g")
                        NutritionRow("碳水化合物", "${nutrition.carbohydrate}g")
                        nutrition.fiber?.let { NutritionRow("膳食纤维", "${it}g") }
                        nutrition.sodium?.let { NutritionRow("钠", "${it}mg") }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}
