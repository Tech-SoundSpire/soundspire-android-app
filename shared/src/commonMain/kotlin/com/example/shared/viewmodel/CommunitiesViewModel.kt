package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CommunitySubscription
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunitiesUiState(
    val loading: Boolean = true,
    val communities: List<CommunitySubscription> = emptyList(),
)

// Port of CommunitiesScreen.kt — the user's subscribed communities.
class CommunitiesViewModel(private val api: SoundSpireApi) : ViewModel() {
    private val _state = MutableStateFlow(CommunitiesUiState())
    val state: StateFlow<CommunitiesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = runCatching { api.getSession().user?.id }.getOrNull()
            val communities = if (userId != null) {
                runCatching { api.getSubscriptions(userId).communities }.getOrDefault(emptyList())
            } else emptyList()
            _state.value = CommunitiesUiState(loading = false, communities = communities)
        }
    }
}
