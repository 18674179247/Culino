package com.culino.feature.recipe.presentation.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.culino.core.ui.component.LocalNavAnimatedVisibilityScope
import com.culino.core.ui.component.LocalSharedTransitionScope
import com.culino.core.ui.component.CulinoBottomSheetHost
import com.culino.core.ui.component.rememberCulinoBottomSheetState
import com.culino.core.ui.component.showConfirm
import com.culino.core.ui.component.showError
import com.culino.core.ui.component.ShimmerBox
import com.culino.feature.recipe.data.RecipeDetail
import com.culino.feature.social.data.RecipeComment
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel,
    currentUserId: String? = null,
    onEdit: ((String) -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val isFavorited by viewModel.isFavorited.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val likeCount by viewModel.likeCount.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentCount by viewModel.commentCount.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val sheetState = rememberCulinoBottomSheetState()

    LaunchedEffect(recipeId) { viewModel.loadRecipeDetail(recipeId) }

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> { viewModel.resetDeleteState(); onBack() }
            is DeleteState.Error -> {
                sheetState.showError(message = (deleteState as DeleteState.Error).message, onDismiss = { viewModel.resetDeleteState() })
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let {
            sheetState.showError(message = it, onDismiss = { viewModel.clearActionError() })
            viewModel.clearActionError()
        }
    }

    CulinoBottomSheetHost(sheetState)

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is RecipeDetailState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(240.dp), cornerRadius = 16.dp)
                    Spacer(Modifier.height(16.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f), height = 24.dp)
                    Spacer(Modifier.height(12.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 16.dp)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(modifier = Modifier.width(60.dp), height = 28.dp, cornerRadius = 14.dp)
                        ShimmerBox(modifier = Modifier.width(60.dp), height = 28.dp, cornerRadius = 14.dp)
                        ShimmerBox(modifier = Modifier.width(60.dp), height = 28.dp, cornerRadius = 14.dp)
                    }
                    Spacer(Modifier.height(24.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 16.dp)
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f), height = 16.dp)
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 16.dp)
                    Spacer(Modifier.height(24.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 16.dp)
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f), height = 16.dp)
                }
            }
            is RecipeDetailState.Success -> {
                val detail = currentState.detail
                val errorColor = MaterialTheme.colorScheme.error

                var contentVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(50)
                    contentVisible = true
                }

                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val allImages = buildList {
                            detail.recipe.coverImage?.let { add(it) }
                            detail.steps.forEach { step -> step.image?.let { add(it) } }
                        }

                        if (allImages.isNotEmpty()) {
                            item { ImageCarousel(images = allImages, recipeId = recipeId) }
                        }

                        detail.author?.let { author ->
                            item {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(tween(300, delayMillis = 100)) { 30 }
                                ) {
                                    AuthorInfoRow(author = author, createdAt = detail.recipe.createdAt)
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(
                                visible = contentVisible,
                                enter = fadeIn(tween(300, delayMillis = 150)) + slideInVertically(tween(300, delayMillis = 150)) { 30 }
                            ) {
                                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(detail.recipe.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                                    detail.recipe.description?.let {
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(
                                visible = contentVisible,
                                enter = fadeIn(tween(300, delayMillis = 200)) + slideInVertically(tween(300, delayMillis = 200)) { 30 }
                            ) {
                                Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    detail.recipe.difficulty?.let { InfoChip("难度: $it") }
                                    detail.recipe.cookingTime?.let { InfoChip("${it}分钟") }
                                    detail.recipe.servings?.let { InfoChip("${it}人份") }
                                }
                            }
                        }

                        if (detail.tags.isNotEmpty()) {
                            item {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(300, delayMillis = 200)) + slideInVertically(tween(300, delayMillis = 200)) { 30 }
                                ) {
                                    SectionCard(title = "标签", modifier = Modifier.padding(horizontal = 16.dp)) {
                                        com.culino.core.ui.component.ChipFlowRow {
                                            detail.tags.forEach { tag ->
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(tag.tagName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (detail.ingredients.isNotEmpty()) {
                            item {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(300, delayMillis = 250)) + slideInVertically(tween(300, delayMillis = 250)) { 30 }
                                ) {
                                    SectionCard(title = "食材", modifier = Modifier.padding(horizontal = 16.dp)) {
                                        detail.ingredients.forEach { ing ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(ing.ingredientName, style = MaterialTheme.typography.bodyMedium)
                                                Text(ing.amount ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (detail.seasonings.isNotEmpty()) {
                            item {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(300, delayMillis = 300)) + slideInVertically(tween(300, delayMillis = 300)) { 30 }
                                ) {
                                    SectionCard(title = "调料", modifier = Modifier.padding(horizontal = 16.dp)) {
                                        detail.seasonings.forEach { s ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(s.seasoningName, style = MaterialTheme.typography.bodyMedium)
                                                Text(s.amount ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (detail.steps.isNotEmpty()) {
                            item {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(300, delayMillis = 350)) + slideInVertically(tween(300, delayMillis = 350)) { 30 }
                                ) {
                                    Text("制作步骤", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                            items(detail.steps) { step ->
                                StepTimelineItem(step = step, isLast = step == detail.steps.last())
                            }
                        }

                        detail.nutrition?.let { n ->
                            item {
                                AnimatedVisibility(
                                    visible = contentVisible,
                                    enter = fadeIn(tween(300, delayMillis = 400)) + slideInVertically(tween(300, delayMillis = 400)) { 30 }
                                ) {
                                    com.culino.core.ui.component.NutritionCard(
                                        nutrition = com.culino.core.model.RecipeNutrition(
                                            calories = n.calories, protein = n.protein, fat = n.fat,
                                            carbohydrate = n.carbohydrate, fiber = n.fiber, sodium = n.sodium,
                                            healthScore = n.healthScore, healthTags = n.healthTags,
                                            suitableFor = n.suitableFor, analysisText = n.analysisText,
                                            servingSize = n.servingSize, trafficLight = n.trafficLight,
                                            overallRating = n.overallRating, summary = n.summary, cautions = n.cautions
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = slideInVertically(tween(300, delayMillis = 200)) { it } + fadeIn(tween(300, delayMillis = 200)),
                        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150))
                    ) {
                        BottomActionBar(
                            isLiked = isLiked,
                            likeCount = likeCount,
                            isFavorited = isFavorited,
                            commentCount = commentCount,
                            isAuthor = currentUserId != null && detail.recipe.authorId == currentUserId,
                            onLike = { viewModel.toggleLike(recipeId) },
                            onFavorite = { viewModel.toggleFavorite(recipeId) },
                            onComment = {
                                sheetState.show { dismiss ->
                                    CommentSheet(
                                        comments = comments,
                                        commentCount = commentCount,
                                        currentUserId = currentUserId,
                                        onPost = { content ->
                                            viewModel.postComment(recipeId, content)
                                        },
                                        onDelete = { id -> viewModel.deleteComment(id) }
                                    )
                                }
                            },
                            onEdit = { onEdit?.invoke(recipeId) },
                            onDelete = {
                                sheetState.showConfirm(
                                    title = "删除菜谱",
                                    message = "确认删除「${detail.recipe.title}」？删除后无法恢复。",
                                    confirmText = "删除",
                                    confirmColor = errorColor,
                                    icon = Icons.Outlined.Warning,
                                    onConfirm = { viewModel.deleteRecipe(recipeId) }
                                )
                            }
                        )
                    }
                }
            }
            is RecipeDetailState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(currentState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.loadRecipeDetail(recipeId) }, shape = RoundedCornerShape(20.dp)) { Text("重试") }
                    }
                }
            }
        }

        // Floating back button with entrance animation
        var backButtonVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(100)
            backButtonVisible = true
        }
        AnimatedVisibility(
            visible = backButtonVisible,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.8f),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageCarousel(images: List<String>, recipeId: String) {
    val pagerState = rememberPagerState(pageCount = { images.size })
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    Column {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(260.dp)) { page ->
            val imageModifier = if (page == 0 && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "recipe_image_$recipeId"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .fillMaxSize()
                }
            } else {
                Modifier.fillMaxSize()
            }
            AsyncImage(model = images[page], contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
        }
        if (images.size > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                repeat(images.size) { i ->
                    Box(Modifier.padding(horizontal = 3.dp).size(if (pagerState.currentPage == i) 8.dp else 6.dp).clip(CircleShape)
                        .background(if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}

@Composable
private fun AuthorInfoRow(author: com.culino.feature.recipe.data.AuthorInfo, createdAt: kotlinx.datetime.Instant?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AsyncImage(model = author.avatar, contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Text(author.nickname ?: author.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        createdAt?.let { Text(it.toString().take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

// PLACEHOLDER_MORE

@Composable
private fun StepTimelineItem(step: com.culino.feature.recipe.data.RecipeStep, isLast: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("${step.stepNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            if (!isLast) Box(Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.outline))
        }
        Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(12.dp)) {
                step.image?.let { url ->
                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(8.dp))
                }
                Text(step.content, style = MaterialTheme.typography.bodyMedium)
                step.duration?.let {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("约 $it 分钟", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentSheet(
    comments: List<RecipeComment>,
    commentCount: Long,
    currentUserId: String?,
    onPost: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).heightIn(min = 200.dp, max = 400.dp)) {
        Text("评论 ($commentCount)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        if (comments.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("暂无评论", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(comments) { c ->
                    CommentItem(comment = c, isOwner = currentUserId == c.userId, onDelete = { onDelete(c.id) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("写评论...") }, shape = RoundedCornerShape(20.dp), singleLine = true)
            IconButton(onClick = { if (text.isNotBlank()) { onPost(text.trim()); text = "" } }, enabled = text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// PLACEHOLDER_BOTTOM

@Composable
private fun CommentItem(comment: RecipeComment, isOwner: Boolean, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AsyncImage(model = comment.avatar, contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f)) {
            Text(comment.nickname ?: comment.username, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            comment.createdAt?.let { Text(it.toString().take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
        if (isOwner) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    isLiked: Boolean, likeCount: Long, isFavorited: Boolean, commentCount: Long,
    isAuthor: Boolean, onLike: () -> Unit, onFavorite: () -> Unit, onComment: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            BottomBarItem(if (isLiked) Icons.Filled.Star else Icons.Outlined.Star, if (likeCount > 0) "$likeCount" else "点赞", isLiked, onLike, Modifier.weight(1f))
            BottomBarItem(if (isFavorited) Icons.Filled.Favorite else Icons.Default.FavoriteBorder, "收藏", isFavorited, onFavorite, Modifier.weight(1f))
            BottomBarItem(Icons.Outlined.Email, if (commentCount > 0) "$commentCount" else "评论", false, onComment, Modifier.weight(1f))
            if (isAuthor) {
                BottomBarItem(Icons.Outlined.Edit, "编辑", false, onEdit, Modifier.weight(1f))
                BottomBarItem(Icons.Default.Delete, "删除", false, onDelete, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BottomBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = color, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
