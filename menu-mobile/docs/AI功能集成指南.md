# 前端 AI 功能集成指南

## 📦 已创建的文件

### 1. 数据模型
- `core/model/src/commonMain/kotlin/com/menu/core/model/AI.kt`
  - `RecipeNutrition` - 营养信息
  - `RecommendationItem` - 推荐项
  - `UserPreference` - 用户偏好
  - `BehaviorLogRequest` - 行为日志请求

### 2. 网络服务
- `core/network/src/commonMain/kotlin/com/menu/core/network/AiApiService.kt`
  - 完整的 AI API 调用封装
  - 营养分析、推荐、偏好、行为日志

### 3. UI 组件
- `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/NutritionCard.kt`
  - `NutritionCard` - 营养信息卡片
  - `HealthScoreBadge` - 健康评分徽章
  - `NutritionLoadingPlaceholder` - 加载占位符

- `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/RecommendationList.kt`
  - `RecommendationList` - 推荐列表
  - `RecommendationCard` - 推荐卡片
  - `RecommendationTypeSelector` - 类型选择器

## 🚀 使用示例

### 1. 初始化 AI API 服务

```kotlin
// 在你的 DI 模块中
val aiApiService = AiApiService(
    apiClient = apiClient,
    baseUrl = "http://your-backend-url"
)
```

### 2. 在菜谱详情页展示营养信息

```kotlin
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    viewModel: RecipeDetailViewModel
) {
    val recipe by viewModel.recipe.collectAsState()
    val nutrition by viewModel.nutrition.collectAsState()

    LazyColumn {
        // 菜谱基本信息
        item {
            RecipeHeader(recipe)
        }

        // 营养信息
        item {
            when {
                nutrition == null -> {
                    // 营养信息加载中或未分析
                    NutritionLoadingPlaceholder()
                }
                else -> {
                    NutritionCard(nutrition = nutrition!!)
                }
            }
        }

        // 其他内容...
    }
}

// ViewModel
class RecipeDetailViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _nutrition = MutableStateFlow<RecipeNutrition?>(null)
    val nutrition: StateFlow<RecipeNutrition?> = _nutrition

    fun loadNutrition(recipeId: String) {
        viewModelScope.launch {
            when (val result = aiApiService.getNutrition(recipeId)) {
                is AppResult.Success -> {
                    _nutrition.value = result.data.data
                }
                is AppResult.Error -> {
                    // 营养信息不存在或加载失败，不影响主流程
                    _nutrition.value = null
                }
            }
        }
    }
}
```

### 3. 创建推荐页面

```kotlin
@Composable
fun RecommendationScreen(
    viewModel: RecommendationViewModel,
    onRecipeClick: (String) -> Unit
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column {
        // 类型选择器
        RecommendationTypeSelector(
            selectedType = selectedType,
            onTypeSelected = { viewModel.selectType(it) }
        )

        // 推荐列表
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            recommendations.isEmpty() -> {
                EmptyState(message = "暂无推荐")
            }
            else -> {
                RecommendationList(
                    recommendations = recommendations,
                    onRecipeClick = onRecipeClick
                )
            }
        }
    }
}

// ViewModel
class RecommendationViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _recommendations = MutableStateFlow<List<RecommendationItem>>(emptyList())
    val recommendations: StateFlow<List<RecommendationItem>> = _recommendations

    private val _selectedType = MutableStateFlow("personalized")
    val selectedType: StateFlow<String> = _selectedType

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadRecommendations()
    }

    fun selectType(type: String) {
        _selectedType.value = type
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = when (_selectedType.value) {
                "personalized" -> aiApiService.getPersonalizedRecommendations()
                "trending" -> aiApiService.getTrendingRecommendations()
                "similar" -> {
                    // 需要传入当前浏览的菜谱 ID
                    // aiApiService.getSimilarRecommendations(currentRecipeId)
                    aiApiService.getTrendingRecommendations()
                }
                else -> aiApiService.getTrendingRecommendations()
            }

            when (result) {
                is AppResult.Success -> {
                    _recommendations.value = result.data.data ?: emptyList()
                }
                is AppResult.Error -> {
                    // 处理错误
                    _recommendations.value = emptyList()
                }
            }

            _isLoading.value = false
        }
    }
}
```

### 4. 记录用户行为

```kotlin
// 在菜谱详情页
class RecipeDetailViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    fun onRecipeViewed(recipeId: String) {
        viewModelScope.launch {
            // 异步记录，不阻塞 UI
            aiApiService.logView(recipeId)
        }
    }
}

// 在收藏功能中
class FavoriteViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            // 调用收藏 API
            // ...

            // 记录行为
            if (isFavorite) {
                aiApiService.logFavorite(recipeId)
            } else {
                aiApiService.logUnfavorite(recipeId)
            }
        }
    }
}

// 在烹饪记录中
class CookingLogViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    fun createCookingLog(recipeId: String, rating: Int?) {
        viewModelScope.launch {
            // 调用创建烹饪记录 API
            // ...

            // 记录行为
            aiApiService.logCook(recipeId, rating)
        }
    }
}
```

### 5. 用户偏好页面

```kotlin
@Composable
fun UserPreferenceScreen(
    viewModel: PreferenceViewModel
) {
    val preference by viewModel.preference.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "我的口味偏好",
            style = MaterialTheme.typography.headlineMedium
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else if (preference != null) {
            // 喜欢的菜系
            preference!!.favoriteCuisines?.let { cuisines ->
                PreferenceSection(
                    title = "喜欢的菜系",
                    items = cuisines.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .map { "${it.key} (${(it.value * 100).toInt()}%)" }
                )
            }

            // 喜欢的口味
            preference!!.favoriteTastes?.let { tastes ->
                PreferenceSection(
                    title = "喜欢的口味",
                    items = tastes.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .map { "${it.key} (${(it.value * 100).toInt()}%)" }
                )
            }

            // 统计信息
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("收藏菜谱：${preference!!.totalFavorites ?: 0} 个")
                    Text("烹饪记录：${preference!!.totalCookingLogs ?: 0} 次")
                    preference!!.avgRating?.let {
                        Text("平均评分：${String.format("%.1f", it)} 分")
                    }
                }
            }

            // 刷新按钮
            Button(
                onClick = { viewModel.analyzePreference() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新分析偏好")
            }
        }
    }
}

@Composable
fun PreferenceSection(
    title: String,
    items: List<String>
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ViewModel
class PreferenceViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _preference = MutableStateFlow<UserPreference?>(null)
    val preference: StateFlow<UserPreference?> = _preference

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadPreference()
    }

    fun loadPreference() {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = aiApiService.getPreferenceProfile()) {
                is AppResult.Success -> {
                    _preference.value = result.data.data
                }
                is AppResult.Error -> {
                    // 如果没有偏好，可以提示用户分析
                    _preference.value = null
                }
            }

            _isLoading.value = false
        }
    }

    fun analyzePreference() {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = aiApiService.analyzePreference()) {
                is AppResult.Success -> {
                    _preference.value = result.data.data
                }
                is AppResult.Error -> {
                    // 处理错误
                }
            }

            _isLoading.value = false
        }
    }
}
```

## 📱 导航集成

在你的导航图中添加推荐页面：

```kotlin
// Navigation.kt
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Recommendation : Screen("recommendation")
    object RecipeDetail : Screen("recipe/{id}")
    object UserPreference : Screen("preference")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onRecommendationClick = {
                    navController.navigate(Screen.Recommendation.route)
                }
            )
        }

        composable(Screen.Recommendation.route) {
            RecommendationScreen(
                viewModel = viewModel(),
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                }
            )
        }

        composable(Screen.RecipeDetail.route) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("id")
            RecipeDetailScreen(
                recipeId = recipeId!!,
                viewModel = viewModel()
            )
        }

        composable(Screen.UserPreference.route) {
            UserPreferenceScreen(viewModel = viewModel())
        }
    }
}
```

## 🎨 UI 设计建议

### 1. 首页添加推荐入口

```kotlin
@Composable
fun HomeScreen() {
    Column {
        // 顶部推荐卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { /* 跳转到推荐页面 */ }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("为你推荐", style = MaterialTheme.typography.titleMedium)
            }
        }

        // 其他内容...
    }
}
```

### 2. 菜谱详情页布局

```
┌─────────────────────────┐
│   封面图片              │
├─────────────────────────┤
│   标题                  │
│   作者 · 难度 · 时间    │
├─────────────────────────┤
│   营养信息卡片          │  ← 新增
│   - 健康评分            │
│   - 营养成分            │
│   - 健康标签            │
├─────────────────────────┤
│   食材列表              │
├─────────────────────────┤
│   烹饪步骤              │
├─────────────────────────┤
│   相似推荐              │  ← 新增
└─────────────────────────┘
```

## ⚠️ 注意事项

1. **错误处理**
   - AI 功能失败不应影响主流程
   - 营养信息为空时显示占位符或隐藏

2. **性能优化**
   - 行为日志异步记录，不阻塞 UI
   - 推荐结果可以缓存

3. **用户体验**
   - 首次使用时提示用户"多收藏菜谱可获得更精准推荐"
   - 营养分析中显示加载状态

4. **隐私**
   - 行为日志仅在用户登录时记录
   - 用户可以查看和管理自己的偏好

## 🔄 完整流程示例

### 用户浏览菜谱
```
1. 用户打开菜谱详情
2. 加载菜谱基本信息
3. 异步加载营养信息
4. 如果有营养信息，显示 NutritionCard
5. 如果没有，显示 "营养分析中" 或隐藏
6. 记录浏览行为（后台异步）
```

### 用户获取推荐
```
1. 用户打开推荐页面
2. 默认显示"为你推荐"
3. 如果用户没有偏好数据，自动分析
4. 显示推荐列表
5. 用户可以切换推荐类型
```

## 📊 数据流

```
用户操作 → ViewModel → AiApiService → 后端 API
                ↓
            StateFlow
                ↓
            UI 更新
```

## 🎯 下一步

1. 根据你的项目结构调整代码
2. 添加依赖注入配置
3. 实现 ViewModel 和 Repository 层
4. 测试 API 调用
5. 优化 UI 样式

需要我帮你实现具体的某个页面或功能吗？
