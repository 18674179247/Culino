package com.menu.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.menu.app.di.AppComponent
import com.menu.core.network.parseUserIdFromToken
import com.menu.feature.recipe.presentation.create.RecipeCreateScreen
import com.menu.feature.recipe.presentation.detail.RecipeDetailScreen
import com.menu.feature.recipe.presentation.list.RecipeListScreen
import com.menu.feature.social.presentation.favorites.FavoritesScreen
import com.menu.feature.user.presentation.login.LoginScreen
import com.menu.feature.user.presentation.profile.ProfileScreen
import com.menu.feature.user.presentation.register.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val RECIPES = "recipes"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val RECIPE_CREATE = "recipe_create"

    fun recipeDetail(recipeId: String) = "recipe_detail/$recipeId"
}

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Recipes : BottomNavItem(Routes.RECIPES, Icons.Default.Home, "菜谱")
    object Favorites : BottomNavItem(Routes.FAVORITES, Icons.Default.Favorite, "收藏")
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Default.Person, "我的")
}

@Composable
fun MenuNavHost(
    appComponent: AppComponent,
    navController: NavHostController = rememberNavController()
) {
    // 检查是否已登录
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = appComponent.tokenProvider.getToken()
        startDestination = if (token != null) Routes.MAIN else Routes.LOGIN
    }

    // 等待检查完成
    if (startDestination == null) {
        return
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        // 登录流程
        composable(Routes.LOGIN) {
            val viewModel = remember { appComponent.loginViewModel() }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel = remember { appComponent.registerViewModel() }
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // 主界面（带底部导航栏）
        composable(Routes.MAIN) {
            MainScreen(
                appComponent = appComponent,
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreen(
    appComponent: AppComponent,
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 获取当前用户 ID
    var currentUserId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val token = appComponent.tokenProvider.getToken()
        currentUserId = token?.let { parseUserIdFromToken(it) }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Recipes,
        BottomNavItem.Favorites,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Routes.RECIPES) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.RECIPES,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.RECIPES) {
                val viewModel = remember { appComponent.recipeListViewModel() }
                RecipeListScreen(
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId))
                    },
                    onCreateClick = {
                        navController.navigate(Routes.RECIPE_CREATE)
                    }
                )
            }

            composable(Routes.FAVORITES) {
                val viewModel = remember { appComponent.favoritesViewModel() }
                FavoritesScreen(
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId))
                    }
                )
            }

            composable(Routes.PROFILE) {
                val viewModel = remember { appComponent.profileViewModel() }
                ProfileScreen(
                    viewModel = viewModel,
                    onLoggedOut = onLoggedOut
                )
            }

            composable(Routes.RECIPE_DETAIL) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                val viewModel = remember { appComponent.recipeDetailViewModel() }
                RecipeDetailScreen(
                    recipeId = recipeId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    currentUserId = currentUserId
                )
            }

            composable(Routes.RECIPE_CREATE) {
                val viewModel = remember { appComponent.recipeCreateViewModel() }
                RecipeCreateScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateSuccess = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId)) {
                            popUpTo(Routes.RECIPES)
                        }
                    }
                )
            }
        }
    }
}
