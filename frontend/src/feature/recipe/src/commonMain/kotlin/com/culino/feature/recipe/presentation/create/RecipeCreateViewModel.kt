package com.culino.feature.recipe.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.common.model.RecognizeRecipeResponse
import com.culino.common.api.AiApiService
import com.culino.framework.network.ApiResponse
import com.culino.common.api.ImageUploadApi
import com.culino.feature.ingredient.data.Ingredient
import com.culino.feature.ingredient.data.IngredientCategory
import com.culino.feature.ingredient.data.IngredientRepository
import com.culino.feature.ingredient.data.Seasoning
import com.culino.feature.recipe.data.CreateRecipeIngredient
import com.culino.feature.recipe.data.CreateRecipeRequest
import com.culino.feature.recipe.data.CreateRecipeSeasoning
import com.culino.feature.recipe.data.CreateRecipeStep
import com.culino.feature.recipe.data.RecipeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    val amount: String = "",
    val ingredientId: Int? = null
)

data class SeasoningInput(
    val name: String = "",
    val amount: String = "",
    val seasoningId: Int? = null
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
    val seasonings: List<SeasoningInput> = listOf(SeasoningInput()),
    val steps: List<StepInput> = listOf(StepInput()),
    val selectedTagIds: List<Int> = emptyList()
)

class RecipeCreateViewModel(
    private val repository: RecipeRepository,
    private val imageUploadApi: ImageUploadApi,
    private val aiApiService: AiApiService,
    private val ingredientRepository: IngredientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeCreateUiState>(RecipeCreateUiState.Idle)
    val uiState: StateFlow<RecipeCreateUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(RecipeFormState())
    val formState: StateFlow<RecipeFormState> = _formState.asStateFlow()

    private val _availableIngredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val availableIngredients: StateFlow<List<Ingredient>> = _availableIngredients.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<IngredientCategory>>(emptyList())
    val availableCategories: StateFlow<List<IngredientCategory>> = _availableCategories.asStateFlow()

    private val _availableSeasonings = MutableStateFlow<List<Seasoning>>(emptyList())
    val availableSeasonings: StateFlow<List<Seasoning>> = _availableSeasonings.asStateFlow()

    private val _availableTags = MutableStateFlow<List<com.culino.feature.ingredient.data.Tag>>(emptyList())
    val availableTags: StateFlow<List<com.culino.feature.ingredient.data.Tag>> = _availableTags.asStateFlow()

    private var editRecipeId: String? = null
    val isEditMode: Boolean get() = editRecipeId != null

    init {
        loadIngredientData()
    }

    private fun loadIngredientData() {
        viewModelScope.launch {
            when (val result = ingredientRepository.getIngredients()) {
                is AppResult.Success -> _availableIngredients.value = result.data
                is AppResult.Error -> {}
            }
        }
        viewModelScope.launch {
            when (val result = ingredientRepository.getCategories()) {
                is AppResult.Success -> _availableCategories.value = result.data
                is AppResult.Error -> {}
            }
        }
        viewModelScope.launch {
            when (val result = ingredientRepository.getSeasonings()) {
                is AppResult.Success -> _availableSeasonings.value = result.data
                is AppResult.Error -> {}
            }
        }
        viewModelScope.launch {
            when (val result = ingredientRepository.getTags()) {
                is AppResult.Success -> _availableTags.value = result.data
                is AppResult.Error -> {}
            }
        }
    }

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
                        ingredients = detail.ingredients.map {
                            IngredientInput(it.ingredientName, it.amount ?: "", ingredientId = it.ingredientId)
                        }.ifEmpty { listOf(IngredientInput()) },
                        seasonings = detail.seasonings.map {
                            SeasoningInput(it.seasoningName, it.amount ?: "", seasoningId = it.seasoningId)
                        }.ifEmpty { listOf(SeasoningInput()) },
                        steps = detail.steps.map { StepInput(it.content, it.image) }.ifEmpty { listOf(StepInput()) },
                        selectedTagIds = detail.tags.map { it.tagId }
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
        if (images.isEmpty()) return
        _formState.value = _formState.value.copy(isUploadingImages = true)
        viewModelScope.launch {
            // 并行上传,逐张结果独立处理;一张失败不拖累其它张
            val results = images.map { (bytes, fileName, contentType) ->
                async { imageUploadApi.uploadImage(bytes, fileName, contentType) }
            }.awaitAll()

            val successUrls = mutableListOf<String>()
            val failedIndices = mutableListOf<Int>()
            results.forEachIndexed { i, r ->
                when (r) {
                    is AppResult.Success -> successUrls.add(r.data)
                    is AppResult.Error -> failedIndices.add(i + 1)
                }
            }

            _formState.value = _formState.value.copy(
                recipeImages = _formState.value.recipeImages + successUrls,
                isUploadingImages = false
            )
            if (failedIndices.isNotEmpty()) {
                _errorMessage.value = "第 ${failedIndices.joinToString(", ")} 张图片上传失败,其它已保存"
            }
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

    fun selectIngredient(index: Int, id: Int, name: String) {
        val current = _formState.value.ingredients.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(ingredientId = id, name = name)
            _formState.value = _formState.value.copy(ingredients = current)
        }
    }

    fun addSeasoning() {
        val current = _formState.value.seasonings.toMutableList()
        current.add(SeasoningInput())
        _formState.value = _formState.value.copy(seasonings = current)
    }

    fun updateSeasoningName(index: Int, name: String) {
        val current = _formState.value.seasonings.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(name = name)
            _formState.value = _formState.value.copy(seasonings = current)
        }
    }

    fun updateSeasoningAmount(index: Int, amount: String) {
        val current = _formState.value.seasonings.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(amount = amount)
            _formState.value = _formState.value.copy(seasonings = current)
        }
    }

    fun removeSeasoning(index: Int) {
        val current = _formState.value.seasonings.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _formState.value = _formState.value.copy(seasonings = current)
        }
    }

    fun selectSeasoning(index: Int, id: Int, name: String) {
        val current = _formState.value.seasonings.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(seasoningId = id, name = name)
            _formState.value = _formState.value.copy(seasonings = current)
        }
    }

    fun toggleTag(tagId: Int) {
        val current = _formState.value.selectedTagIds
        _formState.value = _formState.value.copy(
            selectedTagIds = if (tagId in current) current - tagId else current + tagId
        )
    }

    fun createTag(name: String, type: String) {
        viewModelScope.launch {
            when (val result = ingredientRepository.createTag(name, type)) {
                is AppResult.Success -> {
                    val newTag = result.data
                    _availableTags.value = _availableTags.value + newTag
                    _formState.value = _formState.value.copy(
                        selectedTagIds = _formState.value.selectedTagIds + newTag.id
                    )
                }
                is AppResult.Error -> _errorMessage.value = result.message
            }
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
                ingredients = validIngredients
                    .mapIndexed { index, ing ->
                        CreateRecipeIngredient(
                            ingredientId = ing.ingredientId,
                            name = if (ing.ingredientId == null) ing.name else null,
                            amount = ing.amount.ifBlank { null },
                            sortOrder = index + 1
                        )
                    }.ifEmpty { null },
                seasonings = form.seasonings
                    .filter { it.name.isNotBlank() }
                    .mapIndexed { index, s ->
                        CreateRecipeSeasoning(
                            seasoningId = s.seasoningId,
                            name = if (s.seasoningId == null) s.name else null,
                            amount = s.amount.ifBlank { null },
                            sortOrder = index + 1
                        )
                    }.ifEmpty { null },
                steps = validSteps.mapIndexed { index, step ->
                    CreateRecipeStep(
                        stepNumber = index + 1,
                        content = step.description,
                        image = step.imageUrl,
                        duration = null
                    )
                },
                tagIds = form.selectedTagIds.ifEmpty { null }
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
        val matchedTagIds = if (result.tags.isNotEmpty() && form.selectedTagIds.isEmpty()) {
            val allTags = _availableTags.value
            result.tags.mapNotNull { tagName ->
                allTags.find { it.name == tagName }?.id
            }
        } else form.selectedTagIds

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
                val allIngredients = _availableIngredients.value
                result.ingredients.map { recognized ->
                    val matched = allIngredients.find { it.name == recognized.name }
                    IngredientInput(recognized.name, recognized.amount, ingredientId = matched?.id)
                }
            } else form.ingredients,
            seasonings = if (form.seasonings.size == 1 && form.seasonings[0].name.isBlank() && result.seasonings.isNotEmpty()) {
                val allSeasonings = _availableSeasonings.value
                result.seasonings.map { recognized ->
                    val matched = allSeasonings.find { it.name == recognized.name }
                    SeasoningInput(recognized.name, recognized.amount, seasoningId = matched?.id)
                }
            } else form.seasonings,
            steps = if (form.steps.size == 1 && form.steps[0].description.isBlank() && result.steps.isNotEmpty()) {
                result.steps.map { StepInput(description = it) }
            } else form.steps,
            selectedTagIds = matchedTagIds
        )
        _aiRecognitionState.value = AiRecognitionState.Idle
    }

    fun dismissRecognition() {
        _aiRecognitionState.value = AiRecognitionState.Idle
    }
}
