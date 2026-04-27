package com.menu.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.menu.core.model.RecipeNutrition

/**
 * 营养信息卡片
 */
@Composable
fun NutritionCard(
    nutrition: RecipeNutrition,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Text(
                text = "营养信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 健康评分
            nutrition.healthScore?.let { score ->
                HealthScoreBadge(score = score)
            }

            // 营养成分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                nutrition.calories?.let {
                    NutritionItem("热量", "${it.toInt()} 千卡")
                }
                nutrition.protein?.let {
                    NutritionItem("蛋白质", "${String.format("%.1f", it)} 克")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                nutrition.fat?.let {
                    NutritionItem("脂肪", "${String.format("%.1f", it)} 克")
                }
                nutrition.carbohydrate?.let {
                    NutritionItem("碳水", "${String.format("%.1f", it)} 克")
                }
            }

            // 健康标签
            nutrition.healthTags?.takeIf { it.isNotEmpty() }?.let { tags ->
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        HealthTag(tag)
                    }
                }
            }

            // 分析文本
            nutrition.analysisText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 适合人群
            nutrition.suitableFor?.takeIf { it.isNotEmpty() }?.let { suitable ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "适合人群",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = suitable.joinToString("、"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

/**
 * 健康评分徽章
 */
@Composable
fun HealthScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val color = when {
        score >= 80 -> Color(0xFF4CAF50) // 绿色
        score >= 60 -> Color(0xFFFFC107) // 黄色
        else -> Color(0xFFFF5722) // 橙色
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "健康评分",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 营养项
 */
@Composable
fun RowScope.NutritionItem(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 健康标签
 */
@Composable
fun HealthTag(tag: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4CAF50)
        )
    }
}

/**
 * 营养分析加载中占位符
 */
@Composable
fun NutritionLoadingPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(
                text = "AI 正在分析营养成分...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
