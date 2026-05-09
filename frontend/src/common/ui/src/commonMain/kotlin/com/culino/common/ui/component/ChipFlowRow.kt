package com.culino.common.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
fun ChipFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Int = 8,
    verticalSpacing: Int = 8,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hSpacingPx = (horizontalSpacing * density).toInt()
        val vSpacingPx = (verticalSpacing * density).toInt()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }

        var currentX = 0
        var currentY = 0
        var rowHeight = 0

        val positions = placeables.map { placeable ->
            if (currentX + placeable.width > constraints.maxWidth && currentX > 0) {
                currentX = 0
                currentY += rowHeight + vSpacingPx
                rowHeight = 0
            }
            val pos = Pair(currentX, currentY)
            currentX += placeable.width + hSpacingPx
            rowHeight = maxOf(rowHeight, placeable.height)
            pos
        }

        val totalHeight = if (placeables.isEmpty()) 0 else currentY + rowHeight
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.placeRelative(x, y)
            }
        }
    }
}
