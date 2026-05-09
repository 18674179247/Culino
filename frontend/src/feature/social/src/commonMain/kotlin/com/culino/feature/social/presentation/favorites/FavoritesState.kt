package com.culino.feature.social.presentation.favorites

import com.culino.feature.social.data.Favorite

sealed interface FavoritesState {
    data object Loading : FavoritesState
    data class Success(val favorites: List<Favorite>) : FavoritesState
    data class Error(val message: String) : FavoritesState
}
