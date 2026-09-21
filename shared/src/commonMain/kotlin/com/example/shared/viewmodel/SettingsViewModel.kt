package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.BlockRow
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isAdmin: Boolean = false,
    val blocksLoading: Boolean = true,
    val blocks: List<BlockRow> = emptyList(),
    val apiBaseUrl: String = "",
)

// Port of SettingsScreen.kt's data (admin flag + blocked users). Language selection +
// legal links live in the SwiftUI view.
class SettingsViewModel(private val api: SoundSpireApi, baseUrl: String) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(apiBaseUrl = baseUrl.trimEnd('/')))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val admin = runCatching { api.getSession().user?.isAdmin == true }.getOrDefault(false)
            val blocks = runCatching { api.getBlocks().blocks }.getOrDefault(emptyList())
            _state.value = _state.value.copy(isAdmin = admin, blocksLoading = false, blocks = blocks)
        }
    }

    fun unblock(blockedUserId: String) {
        _state.value = _state.value.copy(blocks = _state.value.blocks.filter { it.blocked_user_id != blockedUserId })
        viewModelScope.launch { runCatching { api.unblockUser(blockedUserId) } }
    }
}
