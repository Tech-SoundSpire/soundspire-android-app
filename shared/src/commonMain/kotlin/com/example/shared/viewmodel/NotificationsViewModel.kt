package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.NotificationItem
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = true,
    val notifications: List<NotificationItem> = emptyList(),
)

// Port of NotificationsScreen.kt: load + mark-all-read. Grouping (Today/This Week/Earlier)
// and timeAgo formatting live in the SwiftUI view.
class NotificationsViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val items = runCatching { api.getNotifications().notifications }.getOrDefault(emptyList())
            _state.value = NotificationsUiState(loading = false, notifications = items)
            runCatching { api.markNotificationsRead(mapOf("notificationIds" to "all")) }
        }
    }
}
