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

class AppComponent(dataStorePath: String) {

    private val dataStore by lazy { createDataStore(dataStorePath) }
    private val tokenStorage by lazy { TokenStorage(dataStore) }
    private val tokenProvider: TokenProvider get() = tokenStorage
    private val httpClient by lazy { createHttpClient(tokenProvider) }
    private val apiClient by lazy { ApiClient(httpClient) }

    // User feature
    private val userApi by lazy { UserApi(apiClient) }
    private val userRepository: UserRepository by lazy { UserRepositoryImpl(userApi, tokenProvider) }
    private val loginUseCase by lazy { LoginUseCase(userRepository) }
    private val registerUseCase by lazy { RegisterUseCase(userRepository) }
    private val getProfileUseCase by lazy { GetProfileUseCase(userRepository) }

    fun loginViewModel() = LoginViewModel(loginUseCase)
    fun registerViewModel() = RegisterViewModel(registerUseCase)
    fun profileViewModel() = ProfileViewModel(getProfileUseCase)
}
