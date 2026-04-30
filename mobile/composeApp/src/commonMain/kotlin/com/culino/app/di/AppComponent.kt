package com.culino.app.di

import com.culino.core.data.TokenStorage
import com.culino.core.data.createDataStore
import com.culino.core.network.AiApiService
import com.culino.core.network.ApiClient
import com.culino.core.network.ImageUploadApi
import com.culino.core.network.TokenProvider
import com.culino.core.network.createHttpClient
import com.culino.feature.user.data.UserApi
import com.culino.feature.user.data.UserRepositoryImpl
import com.culino.feature.user.domain.*
import com.culino.feature.user.presentation.login.LoginViewModel
import com.culino.feature.user.presentation.profile.ProfileViewModel
import com.culino.feature.user.presentation.register.RegisterViewModel
import com.culino.feature.recipe.data.*
import com.culino.feature.recipe.domain.*
import com.culino.feature.recipe.presentation.list.RecipeListViewModel
import com.culino.feature.recipe.presentation.detail.RecipeDetailViewModel
import com.culino.feature.recipe.presentation.create.RecipeCreateViewModel
import com.culino.feature.social.data.*
import com.culino.feature.social.presentation.favorites.FavoritesViewModel
import com.culino.feature.social.presentation.cookinglog.CookingLogViewModel
import com.culino.feature.tool.data.*
import com.culino.feature.tool.presentation.shoppinglist.ShoppingListViewModel
import com.culino.feature.tool.presentation.shoppinglist.ShoppingListDetailViewModel
import com.culino.feature.tool.presentation.mealplan.MealPlanViewModel

class AppComponent(dataStorePath: String) {

    private val dataStore by lazy { createDataStore(dataStorePath) }
    private val tokenStorage by lazy { TokenStorage(dataStore) }
    val tokenProvider: TokenProvider get() = tokenStorage
    private val httpClient by lazy { createHttpClient(tokenProvider) }
    private val apiClient by lazy { ApiClient(httpClient) }
    val imageUploadApi by lazy { ImageUploadApi(httpClient) }
    private val aiBaseUrl by lazy {
        com.culino.core.common.Constants.API_BASE_URL.removeSuffix("/").removeSuffix("api/v1").removeSuffix("/")
    }
    val aiApiService by lazy { AiApiService(apiClient, aiBaseUrl) }

    // User feature
    private val userApi by lazy { UserApi(apiClient) }
    private val userRepository: UserRepository by lazy { UserRepositoryImpl(userApi, tokenProvider) }
    private val loginUseCase by lazy { LoginUseCase(userRepository) }
    private val registerUseCase by lazy { RegisterUseCase(userRepository) }
    private val getProfileUseCase by lazy { GetProfileUseCase(userRepository) }

    // Recipe feature
    private val recipeApi: RecipeApi by lazy { RecipeApiImpl(apiClient) }
    private val recipeRepository: RecipeRepository by lazy { RecipeRepositoryImpl(recipeApi) }
    private val searchRecipesUseCase by lazy { SearchRecipesUseCase(recipeRepository) }
    private val getRecipeDetailUseCase by lazy { GetRecipeDetailUseCase(recipeRepository) }
    private val getRandomRecipesUseCase by lazy { GetRandomRecipesUseCase(recipeRepository) }

    // Social feature
    private val socialApi: SocialApi by lazy { SocialApiImpl(apiClient) }
    private val socialRepository: SocialRepository by lazy { SocialRepositoryImpl(socialApi) }

    // Tool feature
    private val toolApi: ToolApi by lazy { ToolApiImpl(apiClient) }
    private val toolRepository: ToolRepository by lazy { ToolRepositoryImpl(toolApi) }

    fun loginViewModel() = LoginViewModel(loginUseCase)
    fun registerViewModel() = RegisterViewModel(registerUseCase)
    fun profileViewModel() = ProfileViewModel(getProfileUseCase, imageUploadApi, socialRepository, recipeRepository)

    fun recipeListViewModel() = RecipeListViewModel(searchRecipesUseCase, getRandomRecipesUseCase)
    fun recipeDetailViewModel() = RecipeDetailViewModel(getRecipeDetailUseCase, recipeRepository, socialRepository)
    fun recipeCreateViewModel() = RecipeCreateViewModel(recipeRepository, imageUploadApi, aiApiService)

    fun favoritesViewModel() = FavoritesViewModel(socialRepository)
    fun cookingLogViewModel() = CookingLogViewModel(socialRepository)
    fun shoppingListViewModel() = ShoppingListViewModel(toolRepository)
    fun shoppingListDetailViewModel() = ShoppingListDetailViewModel(toolRepository)
    fun mealPlanViewModel() = MealPlanViewModel(toolRepository)
}
