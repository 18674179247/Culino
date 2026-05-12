package com.culino.feature.user.presentation.invite

import com.culino.common.model.InviteCode

data class InviteCodeState(
    val codes: List<InviteCode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // 创建面板
    val showCreateSheet: Boolean = false,
    val draftMaxUses: String = "1",
    val draftNote: String = "",
    val isCreating: Boolean = false,
    // 最近创建的码，用于给用户一个"复制"的机会
    val justCreated: InviteCode? = null,
    // 正在吊销的码，防止重复点击
    val revokingCode: String? = null,
)

sealed interface InviteCodeIntent {
    data object Refresh : InviteCodeIntent
    data object OpenCreateSheet : InviteCodeIntent
    data object CloseCreateSheet : InviteCodeIntent
    data class UpdateMaxUses(val value: String) : InviteCodeIntent
    data class UpdateNote(val value: String) : InviteCodeIntent
    data object SubmitCreate : InviteCodeIntent
    data class Revoke(val code: String) : InviteCodeIntent
    data object ClearJustCreated : InviteCodeIntent
    data object ClearError : InviteCodeIntent
}
