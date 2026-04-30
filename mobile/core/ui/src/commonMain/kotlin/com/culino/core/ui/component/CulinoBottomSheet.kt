package com.culino.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class SheetEntry(
    val id: Long,
    val content: @Composable (dismiss: () -> Unit) -> Unit
)

@Stable
class CulinoBottomSheetState {
    var sheets by mutableStateOf(listOf<SheetEntry>())
        private set

    private var nextId = 0L

    fun show(content: @Composable (dismiss: () -> Unit) -> Unit) {
        sheets = sheets + SheetEntry(nextId++, content)
    }

    fun dismiss() {
        if (sheets.isNotEmpty()) {
            sheets = sheets.dropLast(1)
        }
    }

    fun dismissAll() {
        sheets = emptyList()
    }
}

@Composable
fun rememberCulinoBottomSheetState(): CulinoBottomSheetState {
    return remember { CulinoBottomSheetState() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulinoBottomSheetHost(state: CulinoBottomSheetState) {
    for (entry in state.sheets) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { state.dismiss() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            entry.content { state.dismiss() }
        }
    }
}

fun CulinoBottomSheetState.showError(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    show { dismiss ->
        SheetErrorContent(
            message = message,
            onRetry = onRetry?.let { retry -> {
                dismiss()
                retry()
            }},
            onDismiss = {
                dismiss()
                onDismiss?.invoke()
            }
        )
    }
}

fun CulinoBottomSheetState.showConfirm(
    title: String,
    message: String,
    confirmText: String = "确认",
    cancelText: String = "取消",
    confirmColor: Color? = null,
    icon: ImageVector? = null,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    show { dismiss ->
        SheetConfirmContent(
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
            confirmColor = confirmColor,
            icon = icon,
            onConfirm = {
                dismiss()
                onConfirm()
            },
            onCancel = {
                dismiss()
                onCancel?.invoke()
            }
        )
    }
}

@Composable
private fun SheetErrorContent(
    message: String,
    onRetry: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("知道了")
            }
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("重试")
                }
            }
        }
    }
}

@Composable
private fun SheetConfirmContent(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    confirmColor: Color?,
    icon: ImageVector?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = confirmColor ?: MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(cancelText)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = if (confirmColor != null) {
                    ButtonDefaults.buttonColors(containerColor = confirmColor)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        }
    }
}
