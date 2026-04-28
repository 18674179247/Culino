package com.menu.feature.recipe.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.recipe.data.CreateRecipeRequest
import com.menu.feature.recipe.data.CreateRecipeStep
import com.menu.feature.recipe.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecipeCreateUiState {
    object Idle : RecipeCreateUiState()
    object Loading : RecipeCreateUiState()
    data class Success(val recipeId: String) : RecipeCreateUiState()
    data class Error(val message: String) : RecipeCreateUiState()
}

data class IngredientInput(
    val name: String = "",
    val amount: String = ""
)

data class RecipeFormState(
    val name: String = "",
    val description: String = "",
    val difficulty: String = "简单",
    val cookingTime: String = "",
    val ingredients: List<IngredientInput> = listOf(IngredientInput()),
    val steps: List<String> = listOf("")
)

class RecipeCreateViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeCreateUiState>(RecipeCreateUiState.Idle)
    val uiState: StateFlow<RecipeCreateUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(RecipeFormState())
    val formState: StateFlow<RecipeFormState> = _formState.asStateFlow()

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description)
    }

    fun updateDifficulty(difficulty: String) {
        _formState.value = _formState.value.copy(difficulty = difficulty)
    }

    fun updateCookingTime(time: String) {
        _formState.value = _formState.value.copy(cookingTime = time)
    }

    fun addIngredient() {
        val current = _formState.value.ingredients.toMutableList()
        current.add(IngredientInput())
        _formState.value = _formState.value.copy(ingredients = current)
    }

    fun updateIngredientName(index: Int, name: String) {
        val current = _formState.value.ingredients.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(name = name)
            _formState.value = _formState.value.copy(ingredients = current)
        }
    }

    fun updateIngredientAmount(index: Int, amount: String) {
        val current = _formState.value.ingredients.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(amount = amount)
            _formState.value = _formState.value.copy(ingredients = current)
        }
    }

    fun removeIngredient(index: Int) {
        val current = _formState.value.ingredients.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _formState.value = _formState.value.copy(ingredients = current)
        }
    }

    fun addStep() {
        val current = _formState.value.steps.toMutableList()
        current.add("")
        _formState.value = _formState.value.copy(steps = current)
    }

    fun updateStep(index: Int, description: String) {
        val current = _formState.value.steps.toMutableList()
        if (index in current.indices) {
            current[index] = description
            _formState.value = _formState.value.copy(steps = current)
        }
    }

    fun removeStep(index: Int) {
        val current = _formState.value.steps.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _formState.value = _formState.value.copy(steps = current)
        }
    }

    fun createRecipe() {
        val form = _formState.value

        // 验证
        if (form.name.isBlank()) {
            _uiState.value = RecipeCreateUiState.Error("请输入菜谱名称")
            return
        }

        val cookingTimeInt = form.cookingTime.toIntOrNull()
        if (cookingTimeInt == null || cookingTimeInt <= 0) {
            _uiState.value = RecipeCreateUiState.Error("请输入有效的烹饪时间")
            return
        }

        val validIngredients = form.ingredients.filter { it.name.isNotBlank() }
        if (validIngredients.isEmpty()) {
            _uiState.value = RecipeCreateUiState.Error("请至少添加一个食材")
            return
        }

        val validSteps = form.steps.filter { it.isNotBlank() }
        if (validSteps.isEmpty()) {
            _uiState.value = RecipeCreateUiState.Error("请至少添加一个步骤")
            return
        }

        viewModelScope.launch {
            _uiState.value = RecipeCreateUiState.Loading

            // 映射难度：简单=1, 中等=3, 困难=5
            val difficultyInt = when (form.difficulty) {
                "简单" -> 1
                "中等" -> 3
                "困难" -> 5
                else -> 3
            }

            val request = CreateRecipeRequest(
                title = form.name,
                description = form.description.ifBlank { null },
                coverImage = null,
                difficulty = difficultyInt,
                cookingTime = cookingTimeInt,
                prepTime = null,
                servings = 2, // 默认2人份
                ingredients = null, // 暂时跳过食材
                seasonings = null, // 暂时跳过调料
                steps = validSteps.mapIndexed { index, description ->
                    CreateRecipeStep(
                        stepNumber = index + 1,
                        content = description,
                        image = null,
                        duration = null
                    )
                },
                tagIds = null
            )

            when (val result = repository.createRecipe(request)) {
                is AppResult.Success -> {
                    _uiState.value = RecipeCreateUiState.Success(result.data.recipe.id)
                }
                is AppResult.Error -> {
                    _uiState.value = RecipeCreateUiState.Error(result.message)
                }
            }
        }
    }
}
