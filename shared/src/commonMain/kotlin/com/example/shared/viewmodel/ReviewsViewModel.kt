package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.DiaryEntry
import com.example.shared.data.model.ListDetailItem
import com.example.shared.data.model.ListItem
import com.example.shared.data.model.SongReview
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A diary entry enriched with track metadata (name/artist/art), since the raw entry
// only has a spotify_track_id.
data class DiaryDisplay(
    val entry: DiaryEntry,
    val trackName: String?,
    val artistName: String?,
    val albumArt: String?,
)

data class ReviewsUiState(
    val loading: Boolean = true,
    val activeTab: String = "activity",   // "activity" | "lists" | "journal"
    val reviews: List<SongReview> = emptyList(),
    val lists: List<ListItem> = emptyList(),
    val diary: List<DiaryDisplay> = emptyList(),
)

// Port of ReviewsScreen.kt's data loading (Activity/Lists/Journal tabs).
class ReviewsViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(ReviewsUiState())
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()

    init { load() }

    // Re-fetch (e.g. when the Reviews tab is re-selected, to surface a just-submitted review).
    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            val reviews = runCatching { api.getReviewsFeed().reviews }.getOrDefault(emptyList())
            val lists = runCatching { api.getMyLists().lists }.getOrDefault(emptyList())
            val entries = runCatching { api.getDiary().entries }.getOrDefault(emptyList())
            val diary = entries.map { e ->
                val meta = runCatching { api.getTrackMetadata(e.spotify_track_id) }.getOrNull()
                DiaryDisplay(e, meta?.track_name, meta?.artist_name, meta?.album_art_url)
            }
            _state.value = _state.value.copy(loading = false, reviews = reviews, lists = lists, diary = diary)
        }
    }

    fun selectTab(tab: String) { _state.value = _state.value.copy(activeTab = tab) }

    // Swift awaits this (suspend -> async) when a list row is expanded.
    suspend fun listItems(listId: String): List<ListDetailItem> =
        runCatching { api.getListItems(listId).items }.getOrDefault(emptyList())

    fun createList(title: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.createList(com.example.shared.data.model.CreateListRequest(title.trim())) }
            val lists = runCatching { api.getMyLists().lists }.getOrDefault(emptyList())
            _state.value = _state.value.copy(lists = lists)
            onDone()
        }
    }
}
