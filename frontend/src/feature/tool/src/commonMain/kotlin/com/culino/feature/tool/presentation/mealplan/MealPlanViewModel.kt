package com.culino.feature.tool.presentation.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.tool.data.CreateMealPlanRequest
import com.culino.feature.tool.data.MealPlan
import com.culino.feature.tool.data.ToolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*

sealed interface MealPlanState {
    data object Loading : MealPlanState
    data class Success(val plans: List<MealPlan>) : MealPlanState
    data class Error(val message: String) : MealPlanState
}

class MealPlanViewModel(
    private val repository: ToolRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MealPlanState>(MealPlanState.Loading)
    val state: StateFlow<MealPlanState> = _state.asStateFlow()

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _state.value = MealPlanState.Loading

            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val dayOfWeek = today.dayOfWeek.isoDayNumber // 1=Monday
            val monday = today.minus(dayOfWeek - 1, DateTimeUnit.DAY)
            val sunday = monday.plus(6, DateTimeUnit.DAY)

            val startDate = monday.toString() // yyyy-MM-dd
            val endDate = sunday.toString()

            when (val result = repository.getMealPlans(startDate, endDate)) {
                is AppResult.Success -> {
                    _state.value = MealPlanState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = MealPlanState.Error(result.message)
                }
            }
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch {
            when (repository.deleteMealPlan(id)) {
                is AppResult.Success -> loadPlans()
                is AppResult.Error -> {}
            }
        }
    }

    fun createPlan(recipeId: String, planDate: String, mealType: Int, note: String?) {
        viewModelScope.launch {
            val request = CreateMealPlanRequest(
                recipeId = recipeId,
                planDate = planDate,
                mealType = mealType,
                note = note
            )
            when (repository.createMealPlan(request)) {
                is AppResult.Success -> loadPlans()
                is AppResult.Error -> {}
            }
        }
    }
}
