package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.AlbumMetadata
import com.example.shared.data.model.CacheAlbumRequest
import com.example.shared.data.model.ReviewUser
import com.example.shared.data.model.SongReview
import com.example.shared.data.model.SubmitRatingRequest
import com.example.shared.data.model.SubmitReviewRequest
import com.example.shared.network.SoundSpireApi
import com.example.shared.network.soundSpireJson
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AlbumDetailUiState(
    val loading: Boolean = true,
    val album: AlbumMetadata? = null,
    val reviews: List<SongReview> = emptyList(),
    val userRating: Int = 0,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val currentUserId: String? = null,
)

// Port of AlbumDetailScreen.kt. Albums reuse the song-review system keyed by "album:{id}".
class AlbumDetailViewModel(private val api: SoundSpireApi, albumId: String) : ViewModel() {

    private val reviewKey = "album:$albumId"
    private val _state = MutableStateFlow(AlbumDetailUiState())
    val state: StateFlow<AlbumDetailUiState> = _state.asStateFlow()
    private var currentUser: ReviewUser? = null

    init { load(albumId) }

    private fun load(albumId: String) {
        viewModelScope.launch {
            val user = runCatching { api.getSession().user }.getOrNull()
            val uid = user?.id
            currentUser = user?.let { ReviewUser(user_id = it.id, username = it.name, profile_picture_url = it.photoURL) }
            val album = runCatching { api.getAlbum(albumId) }.getOrNull()
            if (album != null) {
                runCatching {
                    api.cacheAlbum(CacheAlbumRequest(
                        spotify_track_id = reviewKey,
                        track_name = album.name ?: "Album",
                        artist_name = album.artists?.mapNotNull { it.name }?.joinToString(", "),
                        artist_id = album.artists?.firstOrNull()?.id,
                        album_art_url = album.images?.firstOrNull()?.url,
                    ))
                }
            }
            val reviews = runCatching { api.getTrackReviews(reviewKey).reviews }.getOrDefault(emptyList())
            val rating = runCatching { api.getTrackRatings(reviewKey).user_rating?.toInt() }.getOrNull() ?: 0
            _state.value = _state.value.copy(loading = false, album = album, reviews = reviews, userRating = rating, currentUserId = uid)
        }
    }

    fun rate(stars: Int) {
        _state.value = _state.value.copy(userRating = stars)
        viewModelScope.launch { runCatching { api.submitRating(SubmitRatingRequest(reviewKey, stars.toDouble())) } }
    }

    fun submitReview(text: String) {
        if (text.isBlank()) return
        _state.value = _state.value.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            try {
                val r = _state.value.userRating
                api.submitReview(SubmitReviewRequest(reviewKey, text, if (r > 0) r.toDouble() else null))
                val refreshed = runCatching { api.getTrackReviews(reviewKey).reviews }.getOrDefault(_state.value.reviews)
                _state.value = _state.value.copy(submitting = false, reviews = withOptimistic(refreshed, text, r))
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, submitError = parseError(e))
            }
        }
    }

    fun clearSubmitError() { _state.value = _state.value.copy(submitError = null) }

    // Prepend the just-submitted review so it shows immediately (refetch may lag or sort it last),
    // unless the refetch already contains it.
    private fun withOptimistic(refreshed: List<SongReview>, text: String, rating: Int): List<SongReview> {
        val uid = currentUser?.user_id ?: return refreshed
        val mine = text.trim()
        val present = refreshed.any { it.user?.user_id == uid && it.review_text?.trim() == mine }
        if (present) return refreshed
        val optimistic = SongReview(
            review_id = "local-${mine.hashCode()}",
            spotify_track_id = reviewKey,
            review_text = mine,
            rating = if (rating > 0) rating.toDouble() else null,
            user = currentUser,
        )
        return listOf(optimistic) + refreshed
    }

    private suspend fun parseError(e: Throwable): String {
        if (e is ResponseException) {
            val body = runCatching { e.response.bodyAsText() }.getOrDefault("")
            val obj = runCatching { soundSpireJson.parseToJsonElement(body).jsonObject }.getOrNull()
            val msg = obj?.get("message")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: obj?.get("error")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            if (msg != null) return msg
            return "Couldn't submit review (${e.response.status.value})."
        }
        return "Couldn't submit review. Please try again."
    }
}
