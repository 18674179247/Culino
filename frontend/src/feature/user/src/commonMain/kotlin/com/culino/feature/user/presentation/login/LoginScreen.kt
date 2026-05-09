package com.culino.feature.user.presentation.login

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.culino.common.ui.component.ErrorMessage
import com.culino.common.ui.component.LoadingButton
import com.culino.common.ui.component.CulinoTextField

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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Culino",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "发现美食，分享生活",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                    text = "欢迎回来",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))

                CulinoTextField(
                    value = state.username,
                    onValueChange = { viewModel.onIntent(LoginIntent.UpdateUsername(it)) },
                    label = "用户名"
                )
                Spacer(Modifier.height(16.dp))

                CulinoTextField(
                    value = state.password,
                    onValueChange = { viewModel.onIntent(LoginIntent.UpdatePassword(it)) },
                    label = "密码",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                state.error?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    ErrorMessage(
                        message = error,
                        onRetry = { viewModel.onIntent(LoginIntent.ClearError) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                LoadingButton(
                    text = "登录",
                    isLoading = state.isLoading,
                    onClick = { viewModel.onIntent(LoginIntent.Submit) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "没有账号？立即注册",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
