package com.menu.app.di

import com.menu.core.data.TokenStorage
import com.menu.core.data.createDataStore
import com.menu.core.network.ApiClient
import com.menu.core.network.TokenProvider
import com.menu.core.network.createHttpClient
import com.menu.feature.user.data.UserApi
import com.menu.feature.user.data.UserRepositoryImpl
import com.menu.feature.user.domain.*
import com.menu.feature.user.presentation.login.LoginViewModel
import com.menu.feature.user.presentation.profile.ProfileViewModel
import com.menu.feature.user.presentation.register.RegisterViewModel
import com.menu.feature.recipe.data.*
import com.menu.feature.recipe.domain.*
import com.menu.feature.recipe.presentation.list.RecipeListViewModel
import com.menu.feature.recipe.presentation.detail.RecipeDetailViewModel
import com.menu.feature.recipe.presentation.create.RecipeCreateViewModel
import com.menu.feature.social.data.*
import com.menu.feature.social.presentation.favorites.FavoritesViewModel

class AppComponent(dataStorePath: String) {

    private val dataStore by lazy { createDataStore(dataStorePath) }
    private val tokenStorage by lazy { TokenStorage(dataStore) }
    val tokenProvider: TokenProvider get() = tokenStorage
    private val httpClient by lazy { createHttpClient(tokenProvider) }
    private val apiClient by lazy { ApiClient(httpClient) }

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

    fun loginViewModel() = LoginViewModel(loginUseCase)
    fun registerViewModel() = RegisterViewModel(registerUseCase)
    fun profileViewModel() = ProfileViewModel(getProfileUseCase)

    fun recipeListViewModel() = RecipeListViewModel(searchRecipesUseCase, getRandomRecipesUseCase)
    fun recipeDetailViewModel() = RecipeDetailViewModel(getRecipeDetailUseCase, recipeRepository, socialRepository)
    fun recipeCreateViewModel() = RecipeCreateViewModel(recipeRepository)

    fun favoritesViewModel() = FavoritesViewModel(socialRepository)
}
