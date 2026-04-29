package com.menu.feature.user.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.menu.core.ui.component.ErrorMessage
import com.menu.core.ui.component.LoadingButton
import com.menu.core.ui.component.MenuTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.registeredUser) {
        if (state.registeredUser != null) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "加入 Menu",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "开启你的美食之旅",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "创建账号",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))

                MenuTextField(
                    value = state.username,
                    onValueChange = { viewModel.onIntent(RegisterIntent.UpdateUsername(it)) },
                    label = "用户名"
                )
                Spacer(Modifier.height(16.dp))

                MenuTextField(
                    value = state.password,
                    onValueChange = { viewModel.onIntent(RegisterIntent.UpdatePassword(it)) },
                    label = "密码（至少6位）",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(16.dp))

                MenuTextField(
                    value = state.nickname,
                    onValueChange = { viewModel.onIntent(RegisterIntent.UpdateNickname(it)) },
                    label = "昵称（可选）"
                )

                state.error?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    ErrorMessage(
                        message = error,
                        onRetry = { viewModel.onIntent(RegisterIntent.ClearError) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                LoadingButton(
                    text = "注册",
                    isLoading = state.isLoading,
                    onClick = { viewModel.onIntent(RegisterIntent.Submit) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "已有账号？去登录",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
