package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CommunitySubscription
import com.example.shared.data.model.ProfileResponse
import com.example.shared.data.model.ProfileUpdateRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: ProfileResponse? = null,
    val subscriptions: List<CommunitySubscription> = emptyList(),
    val email: String? = null,
    val name: String? = null,
    val photoUrl: String? = null,
    val isAlsoArtist: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
)

// Port of ProfileScreen.kt: view + edit + subscriptions + delete account.
class ProfileViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val user = runCatching { api.getSession().user }.getOrNull()
            if (user?.email == null) { _state.value = _state.value.copy(loading = false); return@launch }
            val profile = runCatching { api.getProfile(user.email!!) }.getOrNull()
            val subs = runCatching { api.getSubscriptions(user.id).communities }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                loading = false, profile = profile, subscriptions = subs,
                email = user.email, name = user.name, photoUrl = user.photoURL, isAlsoArtist = user.isAlsoArtist,
            )
        }
    }

    fun save(
        fullName: String, username: String, gender: String?, dob: String?,
        phone: String?, city: String?, country: String?, profilePictureUrl: String?, onDone: () -> Unit,
    ) {
        val email = _state.value.email ?: return
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            try {
                api.updateProfile(ProfileUpdateRequest(
                    email = email,
                    full_name = fullName.ifBlank { "User" },
                    username = username.ifBlank { email.substringBefore("@") },
                    gender = gender?.ifBlank { null }, date_of_birth = dob?.ifBlank { null },
                    mobile_number = phone?.ifBlank { null }, city = city?.ifBlank { null },
                    country = country?.ifBlank { null }, profile_picture_url = profilePictureUrl,
                ))
                val updated = api.getProfile(email)
                _state.value = _state.value.copy(saving = false, profile = updated)
            } catch (_: Exception) {
                _state.value = _state.value.copy(saving = false)
            }
            onDone()
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        _state.value = _state.value.copy(deleting = true)
        viewModelScope.launch {
            runCatching { api.deleteAccount() }
            _state.value = _state.value.copy(deleting = false)
            onDone()
        }
    }
}
