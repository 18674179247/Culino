package com.menu.feature.recipe.domain

import com.menu.core.common.AppResult
import com.menu.feature.recipe.data.RecipeListItem
import com.menu.feature.recipe.data.RecipeRepository

class SearchRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(
        keyword: String? = null,
        difficulty: String? = null,
        page: Int = 1
    ) = repository.searchRecipes(keyword, difficulty, page)
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
