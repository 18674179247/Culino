package com.menu.app

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.menu.app.di.AppComponent
import com.menu.app.picker.rememberImagePickerLauncher
import com.menu.core.network.parseUserIdFromToken
import com.menu.core.ui.component.LocalNavAnimatedVisibilityScope
import com.menu.core.ui.component.LocalSharedTransitionScope
import com.menu.feature.user.presentation.profile.ProfileIntent
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
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Recipes : BottomNavItem(Routes.RECIPES, Icons.Filled.Home, Icons.Outlined.Home, "菜谱")
    object Favorites : BottomNavItem(Routes.FAVORITES, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "收藏")
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Filled.Person, Icons.Outlined.Person, "我的")
}

private const val ANIM_DURATION = 300
private const val FADE_DURATION = 200

private val slideInFromRight = slideInHorizontally(tween(ANIM_DURATION)) { it }
private val slideOutToLeft = slideOutHorizontally(tween(ANIM_DURATION)) { -it / 3 } + fadeOut(tween(FADE_DURATION))
private val slideInFromLeft = slideInHorizontally(tween(ANIM_DURATION)) { -it / 3 } + fadeIn(tween(FADE_DURATION))
private val slideOutToRight = slideOutHorizontally(tween(ANIM_DURATION)) { it }

private val tabFadeIn = fadeIn(tween(FADE_DURATION))
private val tabFadeOut = fadeOut(tween(FADE_DURATION))

@Composable
fun MenuNavHost(
    appComponent: AppComponent,
    navController: NavHostController = rememberNavController()
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = appComponent.tokenProvider.getToken()
        startDestination = if (token != null) Routes.MAIN else Routes.LOGIN
    }

    if (startDestination == null) {
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
        enterTransition = { fadeIn(tween(ANIM_DURATION)) },
        exitTransition = { fadeOut(tween(ANIM_DURATION)) },
        popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
        popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
    ) {
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
        composable(
            Routes.MAIN,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
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

    val showBottomNav = currentDestination?.route?.let { route ->
        route != Routes.RECIPE_DETAIL && route != Routes.RECIPE_CREATE
    } ?: true

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(tween(ANIM_DURATION)) { it } + fadeIn(tween(FADE_DURATION)),
                exit = slideOutVertically(tween(ANIM_DURATION)) { it } + fadeOut(tween(FADE_DURATION))
            ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Routes.RECIPES) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        NavHost(
            navController = navController,
            startDestination = Routes.RECIPES,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { tabFadeIn },
            exitTransition = { tabFadeOut },
            popEnterTransition = { tabFadeIn },
            popExitTransition = { tabFadeOut }
        ) {
            composable(Routes.RECIPES) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
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
                val avatarPicker = rememberImagePickerLauncher(
                    onSingleResult = { image ->
                        image?.let {
                            viewModel.onIntent(ProfileIntent.UploadAvatar(it.bytes, it.fileName, it.contentType))
                        }
                    },
                    onMultipleResult = {}
                )
                ProfileScreen(
                    viewModel = viewModel,
                    onLoggedOut = onLoggedOut,
                    onPickAvatar = { avatarPicker.pickSingle() }
                )
            }

            composable(
                Routes.RECIPE_DETAIL,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) { backStackEntry ->
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@CompositionLocalProvider
                val viewModel = remember { appComponent.recipeDetailViewModel() }
                RecipeDetailScreen(
                    recipeId = recipeId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    currentUserId = currentUserId
                )
                }
            }

            composable(
                Routes.RECIPE_CREATE,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) {
                val viewModel = remember { appComponent.recipeCreateViewModel() }
                val coverPicker = rememberImagePickerLauncher(
                    onSingleResult = { image ->
                        image?.let { viewModel.uploadCoverImage(it.bytes, it.fileName, it.contentType) }
                    },
                    onMultipleResult = {}
                )
                val imagesPicker = rememberImagePickerLauncher(
                    onSingleResult = {},
                    onMultipleResult = { images ->
                        if (images.isNotEmpty()) {
                            viewModel.uploadRecipeImages(images.map { Triple(it.bytes, it.fileName, it.contentType) })
                        }
                    }
                )
                RecipeCreateScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateSuccess = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId)) {
                            popUpTo(Routes.RECIPES)
                        }
                    },
                    onPickCoverImage = { coverPicker.pickSingle() },
                    onPickRecipeImages = { imagesPicker.pickMultiple() }
                )
            }
        }
            }
        }
    }
}
