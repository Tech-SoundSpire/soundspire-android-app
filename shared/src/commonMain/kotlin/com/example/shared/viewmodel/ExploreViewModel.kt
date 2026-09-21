package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ExploreArtist
import com.example.shared.data.model.GenreItem
import com.example.shared.data.model.SongReview
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val loading: Boolean = true,
    val artists: List<ExploreArtist> = emptyList(),
    val allArtists: List<ExploreArtist> = emptyList(),
    val showAllArtists: Boolean = false,
    val reviews: List<SongReview> = emptyList(),
    val genres: List<GenreItem> = emptyList(),
)

// Port of ExploreScreen.kt's data loading. Shows suggested artists first, then all
// onboarded artists (deduped), plus the reviews feed and genres.
class ExploreViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val userId = runCatching { api.getSession().user?.id }.getOrNull()
                val suggested = if (userId != null) {
                    runCatching { api.getSuggestedArtists(userId).artists }.getOrDefault(emptyList())
                } else emptyList()
                val onboarded = runCatching { api.getExploreArtists() }.getOrDefault(emptyList())
                val seen = HashSet<String>()
                val merged = (suggested + onboarded).filter { seen.add(it.slug ?: it.soundcharts_uuid ?: it.artist_id) }
                val reviews = runCatching { api.getReviewsFeed().reviews }.getOrDefault(emptyList())
                val genres = runCatching { api.getGenres() }.getOrDefault(emptyList())
                _state.value = _state.value.copy(loading = false, artists = merged, reviews = reviews, genres = genres)
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun toggleShowAll() {
        val s = _state.value
        if (s.showAllArtists) {
            _state.value = s.copy(showAllArtists = false)
            return
        }
        if (s.allArtists.isNotEmpty()) {
            _state.value = s.copy(showAllArtists = true)
            return
        }
        viewModelScope.launch {
            val userId = runCatching { api.getSession().user?.id }.getOrNull()
            val suggested = if (userId != null) {
                runCatching { api.getSuggestedArtists(userId).artists }.getOrDefault(emptyList())
            } else emptyList()
            val all = runCatching {
                api.getExploreArtists("").map { a ->
                    a.copy(
                        onSoundSpire = a.onSoundSpire ?: (a.user_id != null),
                        soundcharts_uuid = a.soundcharts_uuid ?: a.third_party_id,
                    )
                }
            }.getOrDefault(emptyList())
            val seen = HashSet<String>()
            val allArtists = (suggested + _state.value.artists + all)
                .filter { seen.add(it.slug ?: it.soundcharts_uuid ?: it.artist_id) }
            _state.value = _state.value.copy(allArtists = allArtists, showAllArtists = true)
        }
    }
}
