package com.culino.app

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.culino.app.di.AppComponent
import com.culino.app.picker.rememberImagePickerLauncher
import com.culino.framework.network.parseUserIdFromToken
import com.culino.common.ui.component.LocalNavAnimatedVisibilityScope
import com.culino.common.ui.component.LocalSharedTransitionScope
import com.culino.feature.user.presentation.profile.ProfileIntent
import com.culino.feature.recipe.presentation.create.RecipeCreateScreen
import com.culino.feature.recipe.presentation.detail.RecipeDetailScreen
import com.culino.feature.recipe.presentation.fridge.FridgeSearchScreen
import com.culino.feature.recipe.presentation.list.RecipeListScreen
import com.culino.feature.social.presentation.favorites.FavoritesScreen
import com.culino.feature.user.presentation.login.LoginScreen
import com.culino.feature.user.presentation.profile.ProfileScreen
import com.culino.feature.user.presentation.register.RegisterScreen
import com.culino.feature.user.presentation.invite.InviteCodeScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val RECIPES = "recipes"
    const val MY_RECIPES = "my_recipes"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val RECIPE_CREATE = "recipe_create"
    const val RECIPE_EDIT = "recipe_edit/{recipeId}"
    const val COOKING_LOGS = "cooking_logs"
    const val SHOPPING_LISTS = "shopping_lists"
    const val SHOPPING_LIST_DETAIL = "shopping_list_detail/{listId}"
    const val MEAL_PLANS = "meal_plans"
    const val FRIDGE_SEARCH = "fridge_search"
    const val ADMIN_INVITE_CODES = "admin_invite_codes"

    fun recipeDetail(recipeId: String) = "recipe_detail/$recipeId"
    fun recipeEdit(recipeId: String) = "recipe_edit/$recipeId"
    fun shoppingListDetail(listId: String) = "shopping_list_detail/$listId"
}

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem(Routes.RECIPES, Icons.Filled.Home, Icons.Outlined.Home, "首页")
    object MyRecipes : BottomNavItem(Routes.MY_RECIPES, Icons.Outlined.Edit, Icons.Outlined.Edit, "菜谱")
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

/**
 * 托管 ViewModel 到当前 NavBackStackEntry(或最近的 ViewModelStoreOwner) 的 ViewModelStore,
 * 这样 Android 屏幕旋转 / 进程重建 / 导航返回时,状态不会丢失(不像 remember {} 随 Composition 销毁)。
 *
 * key 参数用于区分同屏多个同类型 VM(当前项目不需要,保留扩展位)。
 */
@Composable
private inline fun <reified VM : ViewModel> rememberScopedViewModel(
    key: String? = null,
    crossinline factory: () -> VM
): VM = viewModel(key = key) { factory() }

@Composable
fun CulinoNavHost(
    appComponent: AppComponent,
    navController: NavHostController = rememberNavController()
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = appComponent.tokenProvider.getToken()
        startDestination = if (token != null) Routes.MAIN else Routes.LOGIN
    }

    LaunchedEffect(Unit) {
        appComponent.tokenProvider.authExpiredEvent.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
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
            val viewModel = rememberScopedViewModel { appComponent.loginViewModel() }
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
            val viewModel = rememberScopedViewModel { appComponent.registerViewModel() }
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
        BottomNavItem.Home,
        BottomNavItem.MyRecipes,
        BottomNavItem.Favorites,
        BottomNavItem.Profile
    )

    val tabRoutes = bottomNavItems.map { it.route } + Routes.MY_RECIPES
    val showBottomNav = currentDestination?.route?.let { it in tabRoutes } ?: true

    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(tween(ANIM_DURATION)) { it } + fadeIn(tween(FADE_DURATION)),
                exit = slideOutVertically(tween(ANIM_DURATION)) { it } + fadeOut(tween(FADE_DURATION))
            ) {
            Box {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val leftItems = bottomNavItems.take(2)
                    val rightItems = bottomNavItems.drop(2)

                    leftItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick = {
                                fabExpanded = false
                                navController.navigate(item.route) {
                                    popUpTo(Routes.RECIPES) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surface,
                            )
                        )
                    }

                    NavigationBarItem(
                        icon = {
                            val rotation by animateFloatAsState(if (fabExpanded) 45f else 0f, tween(250))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "创建",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp).rotate(rotation)
                                )
                            }
                        },
                        label = { },
                        selected = false,
                        onClick = { fabExpanded = !fabExpanded },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.surface,
                        )
                    )

                    rightItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick = {
                                fabExpanded = false
                                navController.navigate(item.route) {
                                    popUpTo(Routes.RECIPES) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surface,
                            )
                        )
                    }
                }
            }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                val viewModel = rememberScopedViewModel { appComponent.recipeListViewModel() }
                RecipeListScreen(
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId))
                    }
                )
                }
            }

            composable(Routes.MY_RECIPES) {
                val viewModel = remember(currentUserId) { appComponent.recipeListViewModel(authorId = currentUserId) }
                RecipeListScreen(
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId))
                    },
                    title = "我的菜谱",
                    enableSharedElement = false
                )
            }

            composable(Routes.FAVORITES) {
                val viewModel = rememberScopedViewModel { appComponent.favoritesViewModel() }
                FavoritesScreen(
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId))
                    }
                )
            }

            composable(Routes.PROFILE) {
                val viewModel = rememberScopedViewModel { appComponent.profileViewModel() }
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
                    onPickAvatar = { avatarPicker.pickSingle() },
                    onNavigateToMyRecipes = {
                        navController.navigate(Routes.RECIPES) {
                            popUpTo(Routes.RECIPES) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToFavorites = {
                        navController.navigate(Routes.FAVORITES) {
                            popUpTo(Routes.RECIPES) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCookingLogs = { navController.navigate(Routes.COOKING_LOGS) },
                    onNavigateToShoppingLists = { navController.navigate(Routes.SHOPPING_LISTS) },
                    onNavigateToMealPlans = { navController.navigate(Routes.MEAL_PLANS) },
                    onNavigateToInviteCodes = { navController.navigate(Routes.ADMIN_INVITE_CODES) }
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
                val viewModel = rememberScopedViewModel { appComponent.recipeDetailViewModel() }
                RecipeDetailScreen(
                    recipeId = recipeId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    currentUserId = currentUserId,
                    onEdit = { id -> navController.navigate(Routes.recipeEdit(id)) }
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
                val viewModel = rememberScopedViewModel { appComponent.recipeCreateViewModel() }
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

            composable(
                Routes.RECIPE_EDIT,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                val viewModel = rememberScopedViewModel { appComponent.recipeCreateViewModel() }
                LaunchedEffect(recipeId) { viewModel.loadForEdit(recipeId) }
                val coverPicker = rememberImagePickerLauncher(
                    onSingleResult = { image -> image?.let { viewModel.uploadCoverImage(it.bytes, it.fileName, it.contentType) } },
                    onMultipleResult = {}
                )
                val imagesPicker = rememberImagePickerLauncher(
                    onSingleResult = {},
                    onMultipleResult = { images -> if (images.isNotEmpty()) { viewModel.uploadRecipeImages(images.map { Triple(it.bytes, it.fileName, it.contentType) }) } }
                )
                RecipeCreateScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateSuccess = { navController.popBackStack() },
                    onPickCoverImage = { coverPicker.pickSingle() },
                    onPickRecipeImages = { imagesPicker.pickMultiple() }
                )
            }

            composable(
                Routes.COOKING_LOGS,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) {
                val viewModel = rememberScopedViewModel { appComponent.cookingLogViewModel() }
                com.culino.feature.social.presentation.cookinglog.CookingLogScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Routes.SHOPPING_LISTS,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) {
                val viewModel = rememberScopedViewModel { appComponent.shoppingListViewModel() }
                com.culino.feature.tool.presentation.shoppinglist.ShoppingListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onListClick = { listId ->
                        navController.navigate(Routes.shoppingListDetail(listId))
                    }
                )
            }

            composable(
                Routes.SHOPPING_LIST_DETAIL,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
                val viewModel = rememberScopedViewModel { appComponent.shoppingListDetailViewModel() }
                com.culino.feature.tool.presentation.shoppinglist.ShoppingListDetailScreen(
                    listId = listId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Routes.MEAL_PLANS,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) {
                val viewModel = rememberScopedViewModel { appComponent.mealPlanViewModel() }
                com.culino.feature.tool.presentation.mealplan.MealPlanScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Routes.FRIDGE_SEARCH,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) {
                val viewModel = rememberScopedViewModel { appComponent.fridgeSearchViewModel() }
                FridgeSearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onRecipeClick = { recipeId ->
                        navController.navigate(Routes.recipeDetail(recipeId))
                    }
                )
            }

            composable(
                Routes.ADMIN_INVITE_CODES,
                enterTransition = { slideInFromRight },
                exitTransition = { slideOutToLeft },
                popEnterTransition = { slideInFromLeft },
                popExitTransition = { slideOutToRight }
            ) {
                val viewModel = rememberScopedViewModel { appComponent.inviteCodeViewModel() }
                InviteCodeScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
            }
        }

        // FAB expanded overlay
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { fabExpanded = false }
            )
        }

        // Vertical staggered FAB actions
        if (fabExpanded) {
            val actions = listOf(
                Triple(Icons.Outlined.DateRange, "膳食计划", Routes.MEAL_PLANS),
                Triple(Icons.Outlined.ShoppingCart, "购物清单", Routes.SHOPPING_LISTS),
                Triple(Icons.Outlined.Star, "记录烹饪", Routes.COOKING_LOGS),
                Triple(Icons.Outlined.Edit, "创建菜谱", Routes.RECIPE_CREATE),
                Triple(Icons.Filled.Search, "冰箱找菜", Routes.FRIDGE_SEARCH),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 72.dp)
            ) {
                actions.forEachIndexed { index, (icon, label, route) ->
                    val delay = (actions.size - 1 - index) * 50
                    val alpha = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(delay.toLong())
                        alpha.animateTo(1f, tween(150))
                    }
                    val offsetY = remember { Animatable(40f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(delay.toLong())
                        offsetY.animateTo(
                            0f,
                            spring(dampingRatio = 0.75f, stiffness = 600f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset(y = offsetY.value.dp)
                            .graphicsLayer { this.alpha = alpha.value }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                fabExpanded = false
                                navController.navigate(route)
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

