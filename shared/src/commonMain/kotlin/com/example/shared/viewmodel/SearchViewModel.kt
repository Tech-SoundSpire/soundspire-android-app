package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CatalogAlbum
import com.example.shared.data.model.CatalogArtist
import com.example.shared.data.model.SearchCommunityResult
import com.example.shared.data.model.SearchReviewResult
import com.example.shared.data.model.SearchSongResult
import com.example.shared.data.model.SearchUserResult
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class SearchUiState(
    val loading: Boolean = false,
    val hasQuery: Boolean = false, // query length >= 2
    val songs: List<SearchSongResult> = emptyList(),
    val catalogArtists: List<CatalogArtist> = emptyList(),
    val catalogAlbums: List<CatalogAlbum> = emptyList(),
    val communities: List<SearchCommunityResult> = emptyList(),
    val reviews: List<SearchReviewResult> = emptyList(),
    val users: List<SearchUserResult> = emptyList(),
)

// Port of SearchScreen.kt: debounced hybrid search (internal + Spotify catalog).
class SearchViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    @OptIn(FlowPreview::class)
    private fun observe() {
        viewModelScope.launch {
            _query.debounce(400).collectLatest { q ->
                if (q.length < 2) {
                    _state.value = SearchUiState()
                    return@collectLatest
                }
                _state.value = _state.value.copy(loading = true, hasQuery = true)
                val internal = runCatching { api.search(q) }.getOrNull()
                val cat = runCatching { api.searchCatalog(q, "artist,album", 5) }.getOrNull()
                _state.value = SearchUiState(
                    loading = false,
                    hasQuery = true,
                    songs = internal?.songs ?: emptyList(),
                    communities = internal?.communities ?: emptyList(),
                    reviews = internal?.reviews ?: emptyList(),
                    users = internal?.users ?: emptyList(),
                    catalogArtists = cat?.artists?.items ?: emptyList(),
                    catalogAlbums = cat?.albums?.items ?: emptyList(),
                )
            }
        }
    }

    init { observe() }

    fun setQuery(q: String) {
        _query.value = q
        if (q.length < 2) _state.value = SearchUiState()
    }
}
