package com.menu.feature.recipe.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                viewModel.resetDeleteState()
                onBack()
            }
            else -> {}
        }
    }

    LaunchedEffect(favoriteState) {
        when (favoriteState) {
            is FavoriteState.Success -> {
                viewModel.resetFavoriteState()
            }
            else -> {}
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
                    if (state is RecipeDetailState.Success) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(recipeId) },
                            enabled = favoriteState !is FavoriteState.Loading
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorited) "取消收藏" else "收藏",
                                tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val detail = (state as? RecipeDetailState.Success)?.detail
                    if (detail != null && currentUserId != null && detail.recipe.authorId == currentUserId) {
                        if (deleteState is DeleteState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (showDeleteDialog) {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val currentState = state) {
            is RecipeDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadRecipeDetail(recipeId) },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

// PLACEHOLDER_DETAIL_CONTENT

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
        // 标题区域
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = detail.recipe.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                detail.recipe.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InfoChip("难度: ${detail.recipe.difficulty}")
                    InfoChip("${detail.recipe.cookingTime}分钟")
                    InfoChip("${detail.recipe.servings}人份")
                }
            }
        }

        // 食材区域
        if (detail.ingredients.isNotEmpty()) {
            item {
                SectionCard(title = "食材") {
                    detail.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                ingredient.ingredientName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                ingredient.amount,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 调料区域
        if (detail.seasonings.isNotEmpty()) {
            item {
                SectionCard(title = "调料") {
                    detail.seasonings.forEach { seasoning ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                seasoning.seasoningName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                seasoning.amount,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 步骤区域 - 时间线样式
        if (detail.steps.isNotEmpty()) {
            item {
                Text(
                    text = "制作步骤",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(detail.steps) { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 时间线
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${step.stepNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (step != detail.steps.last()) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                    // 步骤内容
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                step.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            step.duration?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "约 $it 分钟",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 营养信息
        detail.nutrition?.let { nutrition ->
            item {
                SectionCard(title = "营养信息") {
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

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
