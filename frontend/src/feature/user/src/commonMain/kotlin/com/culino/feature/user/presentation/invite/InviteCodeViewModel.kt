package com.culino.feature.user.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.user.domain.InviteCodeUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InviteCodeViewModel(
    private val useCases: InviteCodeUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(InviteCodeState())
    val state: StateFlow<InviteCodeState> = _state.asStateFlow()

    fun onIntent(intent: InviteCodeIntent) {
        when (intent) {
            is InviteCodeIntent.Refresh -> refresh()
            is InviteCodeIntent.OpenCreateSheet -> _state.update {
                it.copy(showCreateSheet = true, draftMaxUses = "1", draftNote = "")
            }
            is InviteCodeIntent.CloseCreateSheet -> _state.update {
                it.copy(showCreateSheet = false)
            }
            is InviteCodeIntent.UpdateMaxUses -> _state.update { it.copy(draftMaxUses = intent.value.filter { ch -> ch.isDigit() }) }
            is InviteCodeIntent.UpdateNote -> _state.update { it.copy(draftNote = intent.value) }
            is InviteCodeIntent.SubmitCreate -> submitCreate()
            is InviteCodeIntent.Revoke -> revoke(intent.code)
            is InviteCodeIntent.ClearJustCreated -> _state.update { it.copy(justCreated = null) }
            is InviteCodeIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = useCases.list()) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, codes = result.data)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun submitCreate() {
        val current = _state.value
        val maxUses = current.draftMaxUses.toIntOrNull()
        if (maxUses == null || maxUses < 1) {
            _state.update { it.copy(error = "使用次数必须是大于 0 的整数") }
            return
        }
        val note = current.draftNote.trim().ifBlank { null }
        _state.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            when (val result = useCases.create(maxUses, null, note)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            isCreating = false,
                            showCreateSheet = false,
                            justCreated = result.data,
                            codes = listOf(result.data) + it.codes
                        )
                    }
                }
                is AppResult.Error -> _state.update {
                    it.copy(isCreating = false, error = result.message)
                }
            }
        }
    }

    private fun revoke(code: String) {
        _state.update { it.copy(revokingCode = code, error = null) }
        viewModelScope.launch {
            when (val result = useCases.revoke(code)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        revokingCode = null,
                        codes = it.codes.filterNot { c -> c.code == code }
                    )
                }
                is AppResult.Error -> _state.update {
                    it.copy(revokingCode = null, error = result.message)
                }
            }
        }
    }
}
