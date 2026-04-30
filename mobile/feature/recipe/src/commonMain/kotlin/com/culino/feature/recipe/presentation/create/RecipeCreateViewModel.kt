package com.culino.feature.recipe.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.core.common.AppResult
import com.culino.core.model.RecognizeRecipeResponse
import com.culino.core.network.AiApiService
import com.culino.core.network.ApiResponse
import com.culino.core.network.ImageUploadApi
import com.culino.feature.recipe.data.CreateRecipeRequest
import com.culino.feature.recipe.data.CreateRecipeStep
import com.culino.feature.recipe.data.RecipeRepository
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

sealed class AiRecognitionState {
    object Idle : AiRecognitionState()
    object NeedTitle : AiRecognitionState()
    object Loading : AiRecognitionState()
    data class Success(val result: RecognizeRecipeResponse) : AiRecognitionState()
    data class Error(val message: String) : AiRecognitionState()
}

data class IngredientInput(
    val name: String = "",
    val amount: String = ""
)

data class StepInput(
    val description: String = "",
    val imageUrl: String? = null,
    val isUploadingImage: Boolean = false
)

data class RecipeFormState(
    val name: String = "",
    val description: String = "",
    val difficulty: String = "简单",
    val cookingTime: String = "",
    val servings: String = "1",
    val coverImageUrl: String? = null,
    val recipeImages: List<String> = emptyList(),
    val isUploadingCover: Boolean = false,
    val isUploadingImages: Boolean = false,
    val ingredients: List<IngredientInput> = listOf(IngredientInput()),
    val steps: List<StepInput> = listOf(StepInput())
)

class RecipeCreateViewModel(
    private val repository: RecipeRepository,
    private val imageUploadApi: ImageUploadApi,
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeCreateUiState>(RecipeCreateUiState.Idle)
    val uiState: StateFlow<RecipeCreateUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(RecipeFormState())
    val formState: StateFlow<RecipeFormState> = _formState.asStateFlow()

    private var editRecipeId: String? = null
    val isEditMode: Boolean get() = editRecipeId != null

    fun loadForEdit(recipeId: String) {
        editRecipeId = recipeId
        viewModelScope.launch {
            when (val result = repository.getRecipeDetail(recipeId)) {
                is AppResult.Success -> {
                    val detail = result.data
                    val difficultyStr = when (detail.recipe.difficulty) {
                        1, 2 -> "简单"
                        3, 4 -> "中等"
                        5 -> "困难"
                        else -> "简单"
                    }
                    _formState.value = RecipeFormState(
                        name = detail.recipe.title,
                        description = detail.recipe.description ?: "",
                        difficulty = difficultyStr,
                        cookingTime = detail.recipe.cookingTime?.toString() ?: "",
                        servings = detail.recipe.servings?.toString() ?: "1",
                        coverImageUrl = detail.recipe.coverImage,
                        ingredients = detail.ingredients.map { IngredientInput(it.ingredientName, it.amount) }.ifEmpty { listOf(IngredientInput()) },
                        steps = detail.steps.map { StepInput(it.content, it.image) }.ifEmpty { listOf(StepInput()) }
                    )
                }
                is AppResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _aiRecognitionState = MutableStateFlow<AiRecognitionState>(AiRecognitionState.Idle)
    val aiRecognitionState: StateFlow<AiRecognitionState> = _aiRecognitionState.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
        if (_uiState.value is RecipeCreateUiState.Error) {
            _uiState.value = RecipeCreateUiState.Idle
        }
    }

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

    fun updateServings(servings: String) {
        _formState.value = _formState.value.copy(servings = servings)
    }

    fun uploadCoverImage(bytes: ByteArray, fileName: String, contentType: String) {
        _formState.value = _formState.value.copy(isUploadingCover = true)
        viewModelScope.launch {
            when (val result = imageUploadApi.uploadImage(bytes, fileName, contentType)) {
                is AppResult.Success -> _formState.value = _formState.value.copy(
                    coverImageUrl = result.data,
                    isUploadingCover = false
                )
                is AppResult.Error -> {
                    _formState.value = _formState.value.copy(isUploadingCover = false)
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun removeCoverImage() {
        _formState.value = _formState.value.copy(coverImageUrl = null)
    }

    fun uploadRecipeImages(images: List<Triple<ByteArray, String, String>>) {
        _formState.value = _formState.value.copy(isUploadingImages = true)
        viewModelScope.launch {
            val urls = mutableListOf<String>()
            for ((bytes, fileName, contentType) in images) {
                when (val result = imageUploadApi.uploadImage(bytes, fileName, contentType)) {
                    is AppResult.Success -> urls.add(result.data)
                    is AppResult.Error -> {
                        _formState.value = _formState.value.copy(isUploadingImages = false)
                        _errorMessage.value = result.message
                        return@launch
                    }
                }
            }
            _formState.value = _formState.value.copy(
                recipeImages = _formState.value.recipeImages + urls,
                isUploadingImages = false
            )
        }
    }

    fun removeRecipeImage(index: Int) {
        val current = _formState.value.recipeImages.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _formState.value = _formState.value.copy(recipeImages = current)
        }
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
        current.add(StepInput())
        _formState.value = _formState.value.copy(steps = current)
    }

    fun updateStep(index: Int, description: String) {
        val current = _formState.value.steps.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(description = description)
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

    fun uploadStepImage(index: Int, bytes: ByteArray, fileName: String, contentType: String) {
        val current = _formState.value.steps.toMutableList()
        if (index !in current.indices) return
        current[index] = current[index].copy(isUploadingImage = true)
        _formState.value = _formState.value.copy(steps = current)

        viewModelScope.launch {
            when (val result = imageUploadApi.uploadImage(bytes, fileName, contentType)) {
                is AppResult.Success -> {
                    val steps = _formState.value.steps.toMutableList()
                    if (index in steps.indices) {
                        steps[index] = steps[index].copy(imageUrl = result.data, isUploadingImage = false)
                        _formState.value = _formState.value.copy(steps = steps)
                    }
                }
                is AppResult.Error -> {
                    val steps = _formState.value.steps.toMutableList()
                    if (index in steps.indices) {
                        steps[index] = steps[index].copy(isUploadingImage = false)
                        _formState.value = _formState.value.copy(steps = steps)
                    }
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun removeStepImage(index: Int) {
        val current = _formState.value.steps.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(imageUrl = null)
            _formState.value = _formState.value.copy(steps = current)
        }
    }

    fun createRecipe() {
        val form = _formState.value

        if (form.name.isBlank()) {
            _errorMessage.value = "请输入菜谱名称"
            return
        }

        val cookingTimeInt = form.cookingTime.toIntOrNull()
        if (cookingTimeInt == null || cookingTimeInt <= 0) {
            _errorMessage.value = "请输入有效的烹饪时间"
            return
        }

        val validIngredients = form.ingredients.filter { it.name.isNotBlank() }
        if (validIngredients.isEmpty()) {
            _errorMessage.value = "请至少添加一个食材"
            return
        }

        val validSteps = form.steps.filter { it.description.isNotBlank() }
        if (validSteps.isEmpty()) {
            _errorMessage.value = "请至少添加一个步骤"
            return
        }

        viewModelScope.launch {
            _uiState.value = RecipeCreateUiState.Loading

            val difficultyInt = when (form.difficulty) {
                "简单" -> 1
                "中等" -> 3
                "困难" -> 5
                else -> 3
            }

            val request = CreateRecipeRequest(
                title = form.name,
                description = form.description.ifBlank { null },
                coverImage = form.coverImageUrl,
                difficulty = difficultyInt,
                cookingTime = cookingTimeInt,
                prepTime = null,
                servings = form.servings.toIntOrNull() ?: 1,
                ingredients = null,
                seasonings = null,
                steps = validSteps.mapIndexed { index, step ->
                    CreateRecipeStep(
                        stepNumber = index + 1,
                        content = step.description,
                        image = step.imageUrl,
                        duration = null
                    )
                },
                tagIds = null
            )

            when (val result = if (editRecipeId != null) repository.updateRecipe(editRecipeId!!, request) else repository.createRecipe(request)) {
                is AppResult.Success -> {
                    _uiState.value = RecipeCreateUiState.Success(result.data.recipe.id)
                }
                is AppResult.Error -> {
                    _uiState.value = RecipeCreateUiState.Idle
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun recognizeFromImage() {
        val imageUrl = _formState.value.coverImageUrl ?: return
        val existingTitle = _formState.value.name.ifBlank { null }
        if (existingTitle == null) {
            _aiRecognitionState.value = AiRecognitionState.NeedTitle
            return
        }
        startRecognition(imageUrl, existingTitle)
    }

    fun recognizeWithTitle(title: String) {
        val imageUrl = _formState.value.coverImageUrl ?: return
        _formState.value = _formState.value.copy(name = title)
        startRecognition(imageUrl, title)
    }

    private fun startRecognition(imageUrl: String, title: String) {
        _aiRecognitionState.value = AiRecognitionState.Loading
        viewModelScope.launch {
            when (val result = aiApiService.recognizeRecipe(imageUrl, title)) {
                is ApiResponse.Success -> {
                    _aiRecognitionState.value = AiRecognitionState.Success(result.data)
                }
                is ApiResponse.Error -> {
                    _aiRecognitionState.value = AiRecognitionState.Error(result.message)
                }
            }
        }
    }

    fun applyRecognition(result: RecognizeRecipeResponse) {
        val form = _formState.value
        _formState.value = form.copy(
            name = form.name.ifBlank { result.title },
            description = form.description.ifBlank { result.description ?: "" },
            difficulty = if (form.difficulty == "简单" && result.difficulty != null) {
                when (result.difficulty) {
                    in 1..2 -> "简单"
                    in 3..4 -> "中等"
                    else -> "困难"
                }
            } else form.difficulty,
            cookingTime = form.cookingTime.ifBlank { result.cookingTime?.toString() ?: "" },
            servings = if (result.servings != null) result.servings.toString() else form.servings,
            ingredients = if (form.ingredients.size == 1 && form.ingredients[0].name.isBlank() && result.ingredients.isNotEmpty()) {
                result.ingredients.map { IngredientInput(it.name, it.amount) }
            } else form.ingredients,
            steps = if (form.steps.size == 1 && form.steps[0].description.isBlank() && result.steps.isNotEmpty()) {
                result.steps.map { StepInput(description = it) }
            } else form.steps
        )
        _aiRecognitionState.value = AiRecognitionState.Idle
    }

    fun dismissRecognition() {
        _aiRecognitionState.value = AiRecognitionState.Idle
    }
}
