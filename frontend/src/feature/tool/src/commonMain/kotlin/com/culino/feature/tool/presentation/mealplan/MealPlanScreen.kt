package com.culino.feature.tool.presentation.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.culino.common.ui.component.CulinoTopBar
import com.culino.feature.tool.data.MealPlan
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { CulinoTopBar(title = "膳食计划", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val currentState = state) {
            is MealPlanState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is MealPlanState.Success -> {
                MealPlanContent(
                    plans = currentState.plans,
                    onDeletePlan = { viewModel.deletePlan(it) },
                    onAddPlan = { date, mealType, note ->
                        viewModel.createPlan(
                            recipeId = "00000000-0000-0000-0000-000000000000",
                            planDate = date,
                            mealType = mealType,
                            note = note
                        )
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }

            is MealPlanState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadPlans() },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPlanContent(
    plans: List<MealPlan>,
    onDeletePlan: (String) -> Unit,
    onAddPlan: (date: String, mealType: Int, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var addDialogTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }

    addDialogTarget?.let { (date, mealType) ->
        val mealLabel = when (mealType) { 1 -> "早餐"; 2 -> "午餐"; 3 -> "晚餐"; else -> "加餐" }
        AddMealDialog(
            title = "添加$mealLabel",
            onDismiss = { addDialogTarget = null },
            onConfirm = { note ->
                onAddPlan(date, mealType, note)
                addDialogTarget = null
            }
        )
    }

    val today = remember {
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val dayOfWeek = today.dayOfWeek.isoDayNumber
    val monday = remember(today) { today.minus(dayOfWeek - 1, DateTimeUnit.DAY) }

    val weekDays = remember(monday) {
        (0..6).map { offset -> monday.plus(offset, DateTimeUnit.DAY) }
    }

    val plansByDateAndType = remember(plans) {
        plans.groupBy { it.planDate }
            .mapValues { (_, dayPlans) -> dayPlans.associateBy { it.mealType } }
    }

    if (plans.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    "本周还没有膳食计划",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(weekDays) { date ->
            DayCard(
                date = date,
                mealsForDay = plansByDateAndType[date.toString()] ?: emptyMap(),
                onDeletePlan = onDeletePlan,
                onAddMeal = { mealType -> addDialogTarget = date.toString() to mealType }
            )
        }
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    mealsForDay: Map<Int, MealPlan>,
    onDeletePlan: (String) -> Unit,
    onAddMeal: (mealType: Int) -> Unit
) {
    val dayLabel = when (date.dayOfWeek.isoDayNumber) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }
    val monthDay = "${date.monthNumber.toString().padStart(2, '0')}/${date.dayOfMonth.toString().padStart(2, '0')}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$dayLabel $monthDay",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            val mealSlots = listOf(
                1 to "早餐",
                2 to "午餐",
                3 to "晚餐",
                4 to "加餐"
            )

            mealSlots.forEachIndexed { index, (type, label) ->
                if (index > 0) {
                    Spacer(Modifier.height(8.dp))
                }
                MealSlot(
                    label = label,
                    plan = mealsForDay[type],
                    onDelete = { mealsForDay[type]?.let { onDeletePlan(it.id) } },
                    onAdd = { onAddMeal(type) }
                )
            }
        }
    }
}

@Composable
private fun MealSlot(
    label: String,
    plan: MealPlan?,
    onDelete: () -> Unit,
    onAdd: () -> Unit
) {
    if (plan != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = plan.note ?: "已安排",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AddMealDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（如：菜名或计划）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(note.ifBlank { title }) },
                    ) { Text("添加") }
                }
            }
        }
    }
}
