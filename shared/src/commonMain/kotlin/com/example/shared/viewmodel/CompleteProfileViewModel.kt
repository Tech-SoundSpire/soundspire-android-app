package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CityResult
import com.example.shared.data.model.CompleteProfileRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class CompleteProfileUiState(
    val submitting: Boolean = false,
    val error: String? = null,
    val isAlsoArtist: Boolean = false,
    val prefillName: String? = null,
    val prefillCity: String? = null,
    val prefillCountry: String? = null,
    val prefillPhone: String? = null,
    val prefillPicture: String? = null,
    val cityResults: List<CityResult> = emptyList(),
)

// Port of CompleteProfileScreen.kt's data + submit. City suggestions come from the backend
// (`searchCities` returns country + dial code + phone length), so no offline geo dataset needed.
class CompleteProfileViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(CompleteProfileUiState())
    val state: StateFlow<CompleteProfileUiState> = _state.asStateFlow()
    private val _cityQuery = MutableStateFlow("")

    init {
        prefill()
        observeCity()
    }

    private fun prefill() {
        viewModelScope.launch {
            val user = runCatching { api.getSession().user }.getOrNull()
            val isArtist = user?.isAlsoArtist == true
            val p = user?.email?.let { runCatching { api.getProfile(it) }.getOrNull() }
            _state.value = _state.value.copy(
                isAlsoArtist = isArtist,
                prefillName = p?.full_name ?: user?.name,
                prefillCity = p?.city,
                prefillCountry = p?.country,
                prefillPhone = p?.mobile_number,
                prefillPicture = p?.profile_picture_url,
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeCity() {
        viewModelScope.launch {
            _cityQuery.debounce(350).collectLatest { q ->
                if (q.length < 2) { _state.value = _state.value.copy(cityResults = emptyList()); return@collectLatest }
                val results = runCatching { api.searchCities(q, 10).cities }.getOrDefault(emptyList())
                _state.value = _state.value.copy(cityResults = results)
            }
        }
    }

    fun setCityQuery(q: String) {
        _cityQuery.value = q
        if (q.length < 2) _state.value = _state.value.copy(cityResults = emptyList())
    }

    fun submit(
        fullName: String, gender: String, dob: String, phone: String,
        city: String, country: String, profilePictureUrl: String?, onDone: () -> Unit,
    ) {
        _state.value = _state.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            try {
                api.completeProfile(CompleteProfileRequest(
                    full_name = fullName, gender = gender, date_of_birth = dob,
                    city = city, country = country, phone_number = phone,
                    profile_picture_url = profilePictureUrl,
                ))
                _state.value = _state.value.copy(submitting = false)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, error = "Couldn't save profile. Please try again.")
            }
        }
    }
}
