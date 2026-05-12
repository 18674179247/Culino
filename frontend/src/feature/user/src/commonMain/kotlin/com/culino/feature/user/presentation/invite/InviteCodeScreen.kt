package com.culino.feature.user.presentation.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.culino.common.model.InviteCode
import com.culino.common.ui.component.CulinoBottomSheetHost
import com.culino.common.ui.component.CulinoTextField
import com.culino.common.ui.component.CulinoTopBar
import com.culino.common.ui.component.ErrorMessage
import com.culino.common.ui.component.LoadingButton
import com.culino.common.ui.component.rememberCulinoBottomSheetState
import com.culino.common.ui.component.showConfirm

@Composable
fun InviteCodeScreen(
    viewModel: InviteCodeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberCulinoBottomSheetState()
    val clipboard = LocalClipboardManager.current
    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(Unit) { viewModel.onIntent(InviteCodeIntent.Refresh) }

    Scaffold(
        topBar = {
            CulinoTopBar(
                title = "邀请码管理",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.onIntent(InviteCodeIntent.OpenCreateSheet) }) {
                        Icon(Icons.Outlined.Add, contentDescription = "创建邀请码")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.codes.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                state.codes.isEmpty() && state.error == null -> {
                    EmptyState(
                        onCreate = { viewModel.onIntent(InviteCodeIntent.OpenCreateSheet) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.error?.let { error ->
                            item {
                                ErrorMessage(message = error, onRetry = {
                                    viewModel.onIntent(InviteCodeIntent.ClearError)
                                    viewModel.onIntent(InviteCodeIntent.Refresh)
                                })
                            }
                        }
                        items(state.codes, key = { it.code }) { code ->
                            InviteCodeCard(
                                code = code,
                                isRevoking = state.revokingCode == code.code,
                                onCopy = { clipboard.setText(AnnotatedString(code.code)) },
                                onRevoke = {
                                    sheetState.showConfirm(
                                        title = "吊销邀请码",
                                        message = "吊销后该码无法再用于注册。如果已有用户用该码注册，不会影响已有账号。",
                                        confirmText = "吊销",
                                        confirmColor = errorColor,
                                        icon = Icons.Outlined.Delete,
                                        onConfirm = { viewModel.onIntent(InviteCodeIntent.Revoke(code.code)) }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 新创建的码弹出展示，方便立即复制
            state.justCreated?.let { code ->
                JustCreatedBanner(
                    code = code,
                    onCopy = { clipboard.setText(AnnotatedString(code.code)) },
                    onDismiss = { viewModel.onIntent(InviteCodeIntent.ClearJustCreated) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    // 创建表单走 ModalBottomSheet
    if (state.showCreateSheet) {
        CreateInviteCodeSheet(
            maxUses = state.draftMaxUses,
            note = state.draftNote,
            isCreating = state.isCreating,
            onMaxUsesChange = { viewModel.onIntent(InviteCodeIntent.UpdateMaxUses(it)) },
            onNoteChange = { viewModel.onIntent(InviteCodeIntent.UpdateNote(it)) },
            onSubmit = { viewModel.onIntent(InviteCodeIntent.SubmitCreate) },
            onDismiss = { viewModel.onIntent(InviteCodeIntent.CloseCreateSheet) }
        )
    }

    CulinoBottomSheetHost(sheetState)
}

@Composable
private fun InviteCodeCard(
    code: InviteCode,
    isRevoking: Boolean,
    onCopy: () -> Unit,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = code.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (code.isExhausted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "复制")
                }
                IconButton(onClick = onRevoke, enabled = !isRevoking) {
                    if (isRevoking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "吊销",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UsageBadge(
                    used = code.usedCount,
                    max = code.maxUses,
                    exhausted = code.isExhausted
                )
                val note = code.note
                if (note != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(note, style = MaterialTheme.typography.labelSmall) },
                        border = null,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "创建于 ${code.createdAt.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsageBadge(used: Int, max: Int, exhausted: Boolean) {
    val bg = if (exhausted)
        MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.primaryContainer
    val fg = if (exhausted)
        MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = if (exhausted) "已用尽 $used/$max" else "$used/$max",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun JustCreatedBanner(
    code: InviteCode,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "邀请码已创建",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                code.code,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("关闭") }
                Button(
                    onClick = { onCopy(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("复制")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInviteCodeSheet(
    maxUses: String,
    note: String,
    isCreating: Boolean,
    onMaxUsesChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("创建邀请码", style = MaterialTheme.typography.titleLarge)

            CulinoTextField(
                value = maxUses,
                onValueChange = onMaxUsesChange,
                label = "可使用次数",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CulinoTextField(
                value = note,
                onValueChange = onNoteChange,
                label = "备注（如 \"给老王\"，最长 200 字）"
            )

            LoadingButton(
                text = "生成邀请码",
                isLoading = isCreating,
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("还没有邀请码", style = MaterialTheme.typography.titleMedium)
        Text(
            "生成一个邀请码分享给朋友，他们就能注册了",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onCreate, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("生成邀请码")
        }
    }
}
