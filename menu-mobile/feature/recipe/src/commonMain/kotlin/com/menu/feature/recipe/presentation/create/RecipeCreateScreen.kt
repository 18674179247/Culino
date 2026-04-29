package com.menu.feature.recipe.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreateScreen(
    viewModel: RecipeCreateViewModel,
    onNavigateBack: () -> Unit,
    onCreateSuccess: (String) -> Unit,
    onPickCoverImage: () -> Unit = {},
    onPickRecipeImages: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is RecipeCreateUiState.Success) {
            val recipeId = (uiState as RecipeCreateUiState.Success).recipeId
            onCreateSuccess(recipeId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建菜谱") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.createRecipe() },
                        enabled = uiState !is RecipeCreateUiState.Loading
                    ) {
                        Text(
                            "保存",
                            color = if (uiState !is RecipeCreateUiState.Loading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is RecipeCreateUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is RecipeCreateUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.createRecipe() }, shape = RoundedCornerShape(20.dp)) { Text("重试") }
                    }
                }
                else -> RecipeForm(viewModel, onPickCoverImage, onPickRecipeImages)
            }
        }
    }
}

@Composable
private fun RecipeForm(
    viewModel: RecipeCreateViewModel,
    onPickCoverImage: () -> Unit,
    onPickRecipeImages: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 封面图
        FormSectionCard(title = "封面图片") {
            if (formState.coverImageUrl != null) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    AsyncImage(
                        model = formState.coverImageUrl,
                        contentDescription = "封面",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { viewModel.removeCoverImage() },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            .size(28.dp).background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Close, "移除", modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPickCoverImage() },
                    contentAlignment = Alignment.Center
                ) {
                    if (formState.isUploadingCover) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("添加封面图", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 菜谱图片（多图）
        FormSectionCard(
            title = "菜谱图片",
            action = {
                IconButton(onClick = onPickRecipeImages) {
                    Icon(Icons.Default.Add, "添加图片", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        ) {
            if (formState.recipeImages.isEmpty() && !formState.isUploadingImages) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPickRecipeImages() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("添加菜谱图片（可多选）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    formState.recipeImages.forEachIndexed { index, url ->
                        Box(modifier = Modifier.size(100.dp)) {
                            AsyncImage(
                                model = url,
                                contentDescription = "图片 ${index + 1}",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removeRecipeImage(index) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                                    .size(22.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "移除", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    if (formState.isUploadingImages) {
                        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        // 基本信息
        FormSectionCard(title = "基本信息") {
            OutlinedTextField(
                value = formState.name, onValueChange = { viewModel.updateName(it) },
                label = { Text("菜谱名称 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.description, onValueChange = { viewModel.updateDescription(it) },
                label = { Text("简介") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("难度 *", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("简单", "中等", "困难").forEach { difficulty ->
                    FilterChip(
                        selected = formState.difficulty == difficulty,
                        onClick = { viewModel.updateDifficulty(difficulty) },
                        label = { Text(difficulty) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.cookingTime, onValueChange = { viewModel.updateCookingTime(it) },
                label = { Text("烹饪时间（分钟）*") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
            )
        }

        // 食材列表
        FormSectionCard(
            title = "食材列表",
            action = { IconButton(onClick = { viewModel.addIngredient() }) { Icon(Icons.Default.Add, "添加食材", tint = MaterialTheme.colorScheme.secondary) } }
        ) {
            formState.ingredients.forEachIndexed { index, ingredient ->
                IngredientItem(ingredient, { viewModel.updateIngredientName(index, it) }, { viewModel.updateIngredientAmount(index, it) }, { viewModel.removeIngredient(index) })
                if (index < formState.ingredients.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        // 步骤列表
        FormSectionCard(
            title = "制作步骤",
            action = { IconButton(onClick = { viewModel.addStep() }) { Icon(Icons.Default.Add, "添加步骤", tint = MaterialTheme.colorScheme.secondary) } }
        ) {
            formState.steps.forEachIndexed { index, step ->
                StepItem(index + 1, step, { viewModel.updateStep(index, it) }, { viewModel.removeStep(index) })
                if (index < formState.steps.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                action?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun IngredientItem(ingredient: IngredientInput, onNameChange: (String) -> Unit, onAmountChange: (String) -> Unit, onDelete: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = ingredient.name, onValueChange = onNameChange, label = { Text("名称") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(value = ingredient.amount, onValueChange = onAmountChange, label = { Text("用量") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
        }
    }
}

@Composable
private fun StepItem(stepNumber: Int, description: String, onDescriptionChange: (String) -> Unit, onDelete: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("$stepNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Text("步骤 $stepNumber", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
            }
            OutlinedTextField(value = description, onValueChange = onDescriptionChange, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, shape = RoundedCornerShape(8.dp))
        }
    }
}