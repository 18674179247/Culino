package com.culino.feature.tool.presentation.shoppinglist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.culino.core.ui.component.CulinoTopBar
import com.culino.feature.tool.data.ParsedShoppingItem
import com.culino.feature.tool.data.ShoppingListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListDetailScreen(
    listId: String,
    viewModel: ShoppingListDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    var title by remember { mutableStateOf("购物清单") }
    LaunchedEffect(state) {
        (state as? ShoppingListDetailState.Success)?.detail?.list?.title?.let { title = it }
    }
    LaunchedEffect(listId) { viewModel.loadDetail(listId) }
    Scaffold(
        topBar = { CulinoTopBar(title = title, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val s = state) {
            is ShoppingListDetailState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ShoppingListDetailState.Success -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(s.detail.items, key = { it.id }) { item ->
                            ShoppingItemRow(item, onToggle = { viewModel.toggleItem(listId, item.id, !item.isChecked) }, onDelete = { viewModel.deleteItem(listId, item.id) }, onEdit = { name, amount -> viewModel.updateItemName(listId, item.id, name, amount) }, modifier = Modifier.animateItem(fadeInSpec = tween(300), fadeOutSpec = tween(300), placementSpec = tween(300)))
                        }
                        if (s.detail.items.isEmpty()) {
                            item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { Text("清单为空，在下方输入物品让 AI 帮你整理", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        }
                    }
                    AnimatedVisibility(visible = aiState is AiParseState.Success, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                        val parsedItems = (aiState as? AiParseState.Success)?.items ?: emptyList()
                        AiResultCard(parsedItems, onConfirm = { viewModel.batchAdd(listId, parsedItems) }, onDismiss = { viewModel.dismissAiResult() }, onUpdateItem = { i, n, a -> viewModel.updateParsedItem(i, n, a) }, onRemoveItem = { viewModel.removeParsedItem(it) })
                    }
                    SmartInputArea(aiState, onParse = { viewModel.parseText(it) })
                }
            }
            is ShoppingListDetailState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.loadDetail(listId) }, shape = RoundedCornerShape(20.dp)) { Text("重试") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingListItem, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: (String, String?) -> Unit, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    var editName by remember(item.name) { mutableStateOf(item.name) }
    var editAmount by remember(item.amount) { mutableStateOf(item.amount ?: "") }

    if (editing) {
        Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = editName, onValueChange = { editName = it }, modifier = Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, modifier = Modifier.width(80.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, placeholder = { Text("数量") })
            IconButton(onClick = { onEdit(editName, editAmount.ifBlank { null }); editing = false }) { Icon(Icons.Default.Add, "确认", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { editing = false; editName = item.name; editAmount = item.amount ?: "" }) { Icon(Icons.Default.Close, "取消") }
        }
    } else {
        Row(modifier.fillMaxWidth().alpha(if (item.isChecked) 0.5f else 1f).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).clickable { editing = true }) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None))
                if (!item.amount.isNullOrBlank()) { Text(item.amount, style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
        }
    }
}

@Composable
private fun SmartInputArea(aiState: AiParseState, onParse: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val isLoading = aiState is AiParseState.Loading
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("输入购物清单，如：番茄2个 鸡蛋一盒 酱油...") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onParse(text); text = "" }, enabled = text.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                if (isLoading) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
                Icon(Icons.Outlined.Star, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("AI 整理")
            }
            if (aiState is AiParseState.Error) { Spacer(Modifier.height(4.dp)); Text("AI 解析失败: ${aiState.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AiResultCard(items: List<ParsedShoppingItem>, onConfirm: () -> Unit, onDismiss: () -> Unit, onUpdateItem: (Int, String, String) -> Unit, onRemoveItem: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("AI 识别结果", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "关闭", Modifier.size(16.dp)) } }
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { index, item ->
                var editingName by remember(item.name) { mutableStateOf(item.name) }
                var editingAmount by remember(item.amount) { mutableStateOf(item.amount) }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = editingName, onValueChange = { editingName = it; onUpdateItem(index, it, editingAmount) }, modifier = Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = editingAmount, onValueChange = { editingAmount = it; onUpdateItem(index, editingName, it) }, modifier = Modifier.width(72.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    IconButton(onClick = { onRemoveItem(index) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "移除", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onConfirm, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("全部添加 (${items.size}项)") }
        }
    }
}
