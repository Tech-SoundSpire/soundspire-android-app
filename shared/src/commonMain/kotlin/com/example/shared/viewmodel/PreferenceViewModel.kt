package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ExploreArtist
import com.example.shared.data.model.FavoriteArtistPref
import com.example.shared.data.model.GenreItem
import com.example.shared.data.model.LanguageItem
import com.example.shared.data.model.SavePreferencesRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class PreferenceUiState(
    val loading: Boolean = true,
    val languages: List<LanguageItem> = emptyList(),
    val genres: List<GenreItem> = emptyList(),
    val artists: List<ExploreArtist> = emptyList(),
    val searchedArtists: List<ExploreArtist> = emptyList(),
    val artistSearchLoading: Boolean = false,
    val saving: Boolean = false,
)

// Port of PreferenceSelectionScreen.kt's data + save (3-step wizard; step/selection state
// lives in the SwiftUI view). Debounced SoundCharts artist search included.
class PreferenceViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(PreferenceUiState())
    val state: StateFlow<PreferenceUiState> = _state.asStateFlow()
    private val _artistQuery = MutableStateFlow("")

    init {
        load()
        observeArtistSearch()
    }

    private fun load() {
        viewModelScope.launch {
            val languages = runCatching { api.getAvailableLanguages().languages }.getOrDefault(emptyList())
            val genres = runCatching { api.getAvailableGenres().genres }.getOrNull()
                ?: runCatching { api.getGenres() }.getOrDefault(emptyList())
            val artists = runCatching { api.getAvailableArtists().artists }.getOrDefault(emptyList())
                .ifEmpty { runCatching { api.getExploreArtists() }.getOrDefault(emptyList()) }
            _state.value = _state.value.copy(loading = false, languages = languages, genres = genres, artists = artists)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeArtistSearch() {
        viewModelScope.launch {
            _artistQuery.debounce(600).collectLatest { q ->
                if (q.length < 2) {
                    _state.value = _state.value.copy(searchedArtists = emptyList(), artistSearchLoading = false)
                    return@collectLatest
                }
                _state.value = _state.value.copy(artistSearchLoading = true)
                val results = runCatching {
                    api.searchArtistsSoundcharts(q).items.map {
                        ExploreArtist(artist_id = it.uuid ?: "", artist_name = it.name, profile_picture_url = it.imageUrl)
                    }
                }.getOrDefault(emptyList())
                _state.value = _state.value.copy(searchedArtists = results, artistSearchLoading = false)
            }
        }
    }

    fun setArtistQuery(q: String) {
        _artistQuery.value = q
        if (q.length < 2) _state.value = _state.value.copy(searchedArtists = emptyList())
    }

    fun savePreferences(genres: List<String>, languages: List<String>, favoriteArtists: List<FavoriteArtistPref>, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            try {
                val userId = api.getSession().user?.id
                if (userId != null) {
                    api.savePreferences(SavePreferencesRequest(
                        userId = userId, genres = genres, languages = languages, favoriteArtists = favoriteArtists,
                    ))
                }
            } catch (_: Exception) {}
            _state.value = _state.value.copy(saving = false)
            onDone()
        }
    }
}
