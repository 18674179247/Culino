package com.culino.feature.recipe.domain

import com.culino.core.common.AppResult
import com.culino.feature.recipe.data.RecipeListItem
import com.culino.feature.recipe.data.RecipeRepository

class SearchRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(
        keyword: String? = null,
        difficulty: String? = null,
        authorId: String? = null,
        page: Int = 1,
        maxCookingTime: Int? = null,
        tagIds: List<Int>? = null,
        ingredientIds: List<Int>? = null
    ) = repository.searchRecipes(keyword, difficulty, authorId, page, maxCookingTime = maxCookingTime, tagIds = tagIds, ingredientIds = ingredientIds)
}

class GetRecipeDetailUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(id: String) = repository.getRecipeDetail(id)
}

class GetRandomRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(count: Int = 5) = repository.getRandomRecipes(count)
}

class DeleteRecipeUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(id: String) = repository.deleteRecipe(id)
}
