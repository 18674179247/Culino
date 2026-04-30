package com.menu.feature.social.presentation.cookinglog

import com.menu.feature.social.data.CookingLog

sealed interface CookingLogState {
    data object Loading : CookingLogState
    data class Success(val logs: List<CookingLog>) : CookingLogState
    data class Error(val message: String) : CookingLogState
}
