package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.AdminActionRow
import com.example.shared.data.model.AdminReport
import com.example.shared.data.model.AdminUserRow
import com.example.shared.data.model.BanUserRequest
import com.example.shared.data.model.HideContentRequest
import com.example.shared.network.SoundSpireApi
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModerationUiState(
    val forbidden: Boolean = false,
    val reports: List<AdminReport> = emptyList(),
    val statusFilter: String = "open",
    val users: List<AdminUserRow> = emptyList(),
    val actions: List<AdminActionRow> = emptyList(),
)

// Port of ModerationScreen.kt (admin only; server enforces is_admin): Reports / Users / Audit.
class ModerationViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(ModerationUiState())
    val state: StateFlow<ModerationUiState> = _state.asStateFlow()

    init { loadReports() }

    fun setStatusFilter(status: String) {
        _state.value = _state.value.copy(statusFilter = status)
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            try {
                val reports = api.adminReports(status = _state.value.statusFilter.ifBlank { null }).reports
                _state.value = _state.value.copy(reports = reports, forbidden = false)
            } catch (e: ResponseException) {
                if (e.response.status.value == 403) _state.value = _state.value.copy(forbidden = true)
            } catch (_: Exception) {}
        }
    }

    fun searchUsers(q: String) {
        viewModelScope.launch {
            val users = runCatching { api.adminSearchUsers(q).users }.getOrDefault(emptyList())
            _state.value = _state.value.copy(users = users)
        }
    }

    fun loadActions() {
        viewModelScope.launch {
            val actions = runCatching { api.adminActions().actions }.getOrDefault(emptyList())
            _state.value = _state.value.copy(actions = actions)
        }
    }

    fun hideContent(r: AdminReport) {
        viewModelScope.launch { runCatching { api.adminHideContent(HideContentRequest(r.target_type, r.target_id)) }; loadReports() }
    }
    fun banFromReport(r: AdminReport) {
        viewModelScope.launch { runCatching { api.adminBanUser(BanUserRequest(r.target_id)) }; loadReports() }
    }
    fun dismiss(r: AdminReport) {
        viewModelScope.launch { runCatching { api.adminDismissReport(r.report_id) }; loadReports() }
    }
    fun toggleBan(u: AdminUserRow, currentQuery: String) {
        viewModelScope.launch {
            runCatching { if (u.is_banned) api.adminUnbanUser(BanUserRequest(u.user_id)) else api.adminBanUser(BanUserRequest(u.user_id)) }
            searchUsers(currentQuery)
        }
    }
}
