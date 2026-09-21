package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CatalogArtistAlbum
import com.example.shared.data.model.CatalogArtistDetail
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtistCatalogUiState(
    val loading: Boolean = true,
    val artist: CatalogArtistDetail? = null,
    val albums: List<CatalogArtistAlbum> = emptyList(),
    val communitySlug: String? = null,
    val voteUuid: String? = null,
)

// Port of ArtistCatalogScreen.kt (Spotify-keyed): detail + albums, resolve community slug
// (onboarded) or SoundCharts vote UUID (off-platform).
class ArtistCatalogViewModel(
    private val api: SoundSpireApi,
    private val spotifyId: String,
    private val name: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistCatalogUiState())
    val state: StateFlow<ArtistCatalogUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val artist = runCatching { api.getCatalogArtist(spotifyId, name) }.getOrNull()
            val albums = runCatching { api.getCatalogArtistAlbums(spotifyId, name).albums }.getOrDefault(emptyList())
            _state.value = _state.value.copy(loading = false, artist = artist, albums = albums)

            // Onboarded = a real account (user_id set). A cached SoundCharts row also has a slug
            // but no user_id, so slug alone is NOT onboarded — else off-platform artists link to
            // an empty community page instead of the vote page.
            val slug = runCatching {
                api.getExploreArtists(name).firstOrNull { it.user_id != null && it.artist_name?.equals(name, ignoreCase = true) == true }?.slug
            }.getOrNull()
            if (slug != null) {
                _state.value = _state.value.copy(communitySlug = slug)
            } else {
                val uuid = runCatching { api.resolveSoundchartsUuid(spotifyId, name).soundchartsUuid }.getOrNull()
                _state.value = _state.value.copy(voteUuid = uuid)
            }
        }
    }
}
