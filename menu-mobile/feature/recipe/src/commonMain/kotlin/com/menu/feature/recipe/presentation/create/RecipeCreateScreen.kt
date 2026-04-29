package com.menu.feature.recipe.presentation.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreateScreen(
    viewModel: RecipeCreateViewModel,
    onNavigateBack: () -> Unit,
    onCreateSuccess: (String) -> Unit
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is RecipeCreateUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is RecipeCreateUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.createRecipe() },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("重试")
                        }
                    }
                }
                else -> {
                    RecipeForm(viewModel)
                }
            }
        }
    }
}

@Composable
private fun RecipeForm(viewModel: RecipeCreateViewModel) {
    val formState by viewModel.formState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 基本信息卡片
        FormSectionCard(title = "基本信息") {
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("菜谱名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("简介") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "难度 *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                value = formState.cookingTime,
                onValueChange = { viewModel.updateCookingTime(it) },
                label = { Text("烹饪时间（分钟）*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 食材列表卡片
        FormSectionCard(
            title = "食材列表",
            action = {
                IconButton(onClick = { viewModel.addIngredient() }) {
                    Icon(
                        Icons.Default.Add,
                        "添加食材",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        ) {
            formState.ingredients.forEachIndexed { index, ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    onNameChange = { viewModel.updateIngredientName(index, it) },
                    onAmountChange = { viewModel.updateIngredientAmount(index, it) },
                    onDelete = { viewModel.removeIngredient(index) }
                )
                if (index < formState.ingredients.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // 步骤列表卡片
        FormSectionCard(
            title = "制作步骤",
            action = {
                IconButton(onClick = { viewModel.addStep() }) {
                    Icon(
                        Icons.Default.Add,
                        "添加步骤",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        ) {
            formState.steps.forEachIndexed { index, step ->
                StepItem(
                    stepNumber = index + 1,
                    description = step,
                    onDescriptionChange = { viewModel.updateStep(index, it) },
                    onDelete = { viewModel.removeStep(index) }
                )
                if (index < formState.steps.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                action?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun IngredientItem(
    ingredient: IngredientInput,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = onNameChange,
                label = { Text("名称") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = ingredient.amount,
                onValueChange = onAmountChange,
                label = { Text("用量") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "$stepNumber",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Text(
                        text = "步骤 $stepNumber",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}