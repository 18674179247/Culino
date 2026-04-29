package com.menu.feature.user.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.menu.core.ui.component.ErrorMessage
import com.menu.core.ui.component.LoadingButton
import com.menu.core.ui.component.MenuTextField

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit,
    onPickAvatar: () -> Unit = {},
    onNavigateToMyRecipes: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToCookingLogs: () -> Unit = {},
    onNavigateToShoppingLists: () -> Unit = {},
    onNavigateToMealPlans: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.onIntent(ProfileIntent.LoadProfile) }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Avatar header
        ProfileHeader(
            state = state,
            onPickAvatar = onPickAvatar
        )

        if (state.isLoading && state.user == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.error?.let { error ->
                    ErrorMessage(message = error, onRetry = { viewModel.onIntent(ProfileIntent.ClearError) })
                }

                // Stats row
                StatsRow(
                    stats = state.stats,
                    onRecipesClick = onNavigateToMyRecipes,
                    onFavoritesClick = onNavigateToFavorites,
                    onCookingLogsClick = onNavigateToCookingLogs
                )

                // Feature entries
                FeatureGrid(
                    onCookingLogs = onNavigateToCookingLogs,
                    onShoppingLists = onNavigateToShoppingLists,
                    onMealPlans = onNavigateToMealPlans
                )

                // Profile info card
                state.user?.let { user ->
                    ProfileInfoCard(
                        state = state,
                        onToggleEdit = { viewModel.onIntent(ProfileIntent.ToggleEdit) },
                        onUpdateNickname = { viewModel.onIntent(ProfileIntent.UpdateNickname(it)) },
                        onSave = { viewModel.onIntent(ProfileIntent.SaveProfile) }
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.onIntent(ProfileIntent.Logout) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("退出登录", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(state: ProfileState, onPickAvatar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                val avatarUrl = state.user?.avatar
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onPickAvatar() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            .clickable { onPickAvatar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                if (state.isUploadingAvatar) {
                    CircularProgressIndicator(modifier = Modifier.size(72.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                } else {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "更换头像", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            state.user?.let { user ->
                Text(user.nickname ?: user.username, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)) {
                    Text(user.roleCode, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    stats: ProfileStats,
    onRecipesClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCookingLogsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("菜谱", "${stats.recipeCount}", Icons.Outlined.Home, onRecipesClick, Modifier.weight(1f))
        StatCard("收藏", "${stats.favoriteCount}", Icons.Outlined.FavoriteBorder, onFavoritesClick, Modifier.weight(1f))
        StatCard("烹饪", "${stats.cookingLogCount}", Icons.Outlined.Star, onCookingLogsClick, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeatureGrid(
    onCookingLogs: () -> Unit,
    onShoppingLists: () -> Unit,
    onMealPlans: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureEntry(Icons.Outlined.Star, "烹饪记录", onCookingLogs)
            FeatureEntry(Icons.Outlined.ShoppingCart, "购物清单", onShoppingLists)
            FeatureEntry(Icons.Outlined.DateRange, "膳食计划", onMealPlans)
        }
    }
}

@Composable
private fun FeatureEntry(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileInfoCard(
    state: ProfileState,
    onToggleEdit: () -> Unit,
    onUpdateNickname: (String) -> Unit,
    onSave: () -> Unit
) {
    val user = state.user ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoRow("用户名", user.username)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            if (state.isEditing) {
                MenuTextField(value = state.editNickname, onValueChange = onUpdateNickname, label = "昵称")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onToggleEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) { Text("取消") }
                    LoadingButton(text = "保存", isLoading = state.isLoading, onClick = onSave, modifier = Modifier.weight(1f))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("昵称", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(user.nickname ?: "未设置", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onToggleEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}