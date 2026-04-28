package com.menu.feature.user.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedInUser) {
        if (state.loggedInUser != null) onLoginSuccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "欢迎回来",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(32.dp))

        MenuTextField(
            value = state.username,
            onValueChange = { viewModel.onIntent(LoginIntent.UpdateUsername(it)) },
            label = "用户名"
        )
        Spacer(Modifier.height(16.dp))

        MenuTextField(
            value = state.password,
            onValueChange = { viewModel.onIntent(LoginIntent.UpdatePassword(it)) },
            label = "密码",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(8.dp))

        state.error?.let { error ->
            ErrorMessage(
                message = error,
                onRetry = { viewModel.onIntent(LoginIntent.ClearError) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        LoadingButton(
            text = "登录",
            isLoading = state.isLoading,
            onClick = { viewModel.onIntent(LoginIntent.Submit) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("没有账号？去注册")
        }
    }
}
