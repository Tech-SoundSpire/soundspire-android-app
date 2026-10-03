package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ArtistSignupRequest
import com.example.shared.data.model.ArtistSocial
import com.example.shared.data.model.CityResult
import com.example.shared.data.model.CreateCommunityRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ArtistDetailsUiState(
    val loadingPrefill: Boolean = true,
    val isLoggedIn: Boolean = false,
    val prefillName: String? = null,
    val prefillBio: String? = null,
    val prefillImage: String? = null,
    val prefillSocials: List<ArtistSocial> = emptyList(),
    val cityResults: List<CityResult> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

// Port of ArtistDetailsScreen.kt: new-artist signup form (SoundCharts prefill + community).
class ArtistDetailsViewModel(private val api: SoundSpireApi, private val soundchartsUuid: String) : ViewModel() {

    private val _state = MutableStateFlow(ArtistDetailsUiState())
    val state: StateFlow<ArtistDetailsUiState> = _state.asStateFlow()
    private val _cityQuery = MutableStateFlow("")

    init { prefill(); observeCity() }

    private fun prefill() {
        viewModelScope.launch {
            val loggedIn = runCatching { api.getSession().user != null }.getOrDefault(false)
            val detail = runCatching { api.getArtistByUuid(soundchartsUuid) }.getOrNull()
            val name = detail?.get("name")?.jsonPrimitive?.contentOrNull
            val image = detail?.get("imageUrl")?.jsonPrimitive?.contentOrNull
            val bio = detail?.get("biography")?.jsonPrimitive?.contentOrNull
            val socials = runCatching {
                api.getArtistIdentifiers(soundchartsUuid).items.mapNotNull { id ->
                    val platform = (id.platformName ?: id.platform ?: "").lowercase()
                    val url = id.url ?: ""
                    if (url.isNotBlank() && platform in listOf("spotify", "instagram", "youtube", "facebook", "twitter", "x", "tiktok"))
                        ArtistSocial(if (platform == "x") "twitter" else platform, url) else null
                }.distinctBy { it.platform }
            }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                loadingPrefill = false, isLoggedIn = loggedIn,
                prefillName = name, prefillBio = bio, prefillImage = image,
                prefillSocials = socials.ifEmpty { listOf(ArtistSocial("spotify", ""), ArtistSocial("instagram", ""), ArtistSocial("youtube", "")) },
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeCity() {
        viewModelScope.launch {
            _cityQuery.debounce(300).collectLatest { q ->
                if (q.length < 2) { _state.value = _state.value.copy(cityResults = emptyList()); return@collectLatest }
                val r = runCatching { api.searchCities(q).cities }.getOrDefault(emptyList())
                _state.value = _state.value.copy(cityResults = r)
            }
        }
    }

    fun setCityQuery(q: String) {
        _cityQuery.value = q
        if (q.length < 2) _state.value = _state.value.copy(cityResults = emptyList())
    }

    fun submit(
        artistName: String, username: String, email: String, password: String, bio: String,
        phone: String, countryCode: String, city: String, country: String,
        genres: List<String>, socials: List<ArtistSocial>, distributionCompany: String,
        communityName: String, communityDescription: String,
        profilePictureUrl: String?, coverPhotoUrl: String?,
    ) {
        _state.value = _state.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            try {
                val resp = api.artistSignup(ArtistSignupRequest(
                    artist_name = artistName.trim(),
                    username = username.trim().ifBlank { null },
                    email = email.trim().ifBlank { null },
                    password_hash = password.ifBlank { null },
                    bio = bio.trim(),
                    phone = if (countryCode.isNotBlank()) "$countryCode-$phone" else phone,
                    city = city.trim(), country = country,
                    socials = socials.filter { it.url.isNotBlank() },
                    genre_names = genres,
                    profile_picture_url = profilePictureUrl ?: _state.value.prefillImage,
                    cover_photo_url = coverPhotoUrl,
                    community_name = communityName.trim().ifBlank { null },
                    community_description = communityDescription.trim().ifBlank { null },
                    distribution_company = distributionCompany.trim().ifBlank { null },
                    third_party_platform = "soundcharts", third_party_id = soundchartsUuid,
                ))
                val newId = resp.artist?.artist_id
                if (resp.error != null && newId == null) {
                    _state.value = _state.value.copy(submitting = false, error = resp.error)
                    return@launch
                }
                if (newId != null) {
                    runCatching {
                        api.createCommunity(CreateCommunityRequest(
                            artist_id = newId,
                            name = communityName.trim().ifBlank { "$artistName's Community" },
                            description = communityDescription.trim().ifBlank { "Welcome to $artistName's official community!" },
                        ))
                    }
                }
                _state.value = _state.value.copy(submitting = false, success = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, error = "Signup failed. Please try again.")
            }
        }
    }
}
