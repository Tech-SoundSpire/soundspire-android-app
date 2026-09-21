package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.GenreArtistItem
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GenreArtistsUiState(
    val loading: Boolean = true,
    val genreName: String = "",
    val artists: List<GenreArtistItem> = emptyList(),
)

// Artists in a genre, ranked by popularity. Each links to a community (onboarded) or the vote
// page (off-platform) - the SwiftUI/Compose view decides using onSoundSpire + slug/soundcharts_uuid.
class GenreArtistsViewModel(private val api: SoundSpireApi, private val genreId: String) : ViewModel() {

    private val _state = MutableStateFlow(GenreArtistsUiState())
    val state: StateFlow<GenreArtistsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val resp = runCatching { api.getGenreArtists(genreId) }.getOrNull()
            _state.value = GenreArtistsUiState(
                loading = false,
                genreName = resp?.genre?.name ?: "",
                artists = resp?.artists ?: emptyList(),
            )
        }
    }
}
