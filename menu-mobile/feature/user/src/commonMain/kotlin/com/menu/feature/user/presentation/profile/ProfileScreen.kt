package com.menu.feature.user.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.menu.core.ui.component.ErrorMessage
import com.menu.core.ui.component.LoadingButton
import com.menu.core.ui.component.MenuTextField

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "个人资料", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        if (state.isLoading && state.user == null) {
            CircularProgressIndicator()
        } else {
            state.error?.let { error ->
                ErrorMessage(message = error, onRetry = { viewModel.onIntent(ProfileIntent.ClearError) })
                Spacer(Modifier.height(16.dp))
            }

            state.user?.let { user ->
                Text("用户名: ${user.username}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("角色: ${user.roleCode}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))

                if (state.isEditing) {
                    MenuTextField(
                        value = state.editNickname,
                        onValueChange = { viewModel.onIntent(ProfileIntent.UpdateNickname(it)) },
                        label = "昵称"
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleEdit) }) {
                            Text("取消")
                        }
                        LoadingButton(
                            text = "保存",
                            isLoading = state.isLoading,
                            onClick = { viewModel.onIntent(ProfileIntent.SaveProfile) }
                        )
                    }
                } else {
                    Text("昵称: ${user.nickname ?: "未设置"}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleEdit) }) {
                        Text("编辑资料")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.onIntent(ProfileIntent.Logout) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
        }
    }
}
