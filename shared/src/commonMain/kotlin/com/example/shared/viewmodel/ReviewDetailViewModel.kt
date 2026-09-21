package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ReviewUser
import com.example.shared.data.model.SongReview
import com.example.shared.data.model.SubmitRatingRequest
import com.example.shared.data.model.SubmitReviewRequest
import com.example.shared.data.model.TrackMetadata
import com.example.shared.data.model.TrackRatingResponse
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

data class ReviewDetailUiState(
    val loading: Boolean = true,
    val trackMeta: TrackMetadata? = null,
    val reviews: List<SongReview> = emptyList(),
    val ratings: TrackRatingResponse? = null,
    val userRating: Int = 0,
    val submitting: Boolean = false,
    val submitError: String? = null,
)

// Port of ReviewDetailScreen.kt's data + actions for one track.
class ReviewDetailViewModel(
    private val api: SoundSpireApi,
    private val trackId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewDetailUiState())
    val state: StateFlow<ReviewDetailUiState> = _state.asStateFlow()
    private var currentUser: ReviewUser? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val meta = runCatching { api.getTrackMetadata(trackId) }.getOrNull()
            val reviews = runCatching { api.getTrackReviews(trackId).reviews }.getOrDefault(emptyList())
            val ratings = runCatching { api.getTrackRatings(trackId) }.getOrNull()
            runCatching { api.getSession().user }.getOrNull()?.let {
                currentUser = ReviewUser(user_id = it.id, username = it.name, profile_picture_url = it.photoURL)
            }
            _state.value = _state.value.copy(
                loading = false,
                trackMeta = meta,
                reviews = reviews,
                ratings = ratings,
                userRating = ratings?.user_rating?.toInt() ?: 0,
            )
        }
    }

    fun rate(stars: Int) {
        _state.value = _state.value.copy(userRating = stars)
        viewModelScope.launch {
            runCatching { api.submitRating(SubmitRatingRequest(trackId, stars.toDouble())) }
        }
    }

    fun submitReview(text: String) {
        if (text.isBlank()) return
        _state.value = _state.value.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            try {
                val rating = _state.value.userRating
                api.submitReview(SubmitReviewRequest(trackId, text, if (rating > 0) rating.toDouble() else null))
                val refreshed = runCatching { api.getTrackReviews(trackId).reviews }.getOrDefault(_state.value.reviews)
                _state.value = _state.value.copy(submitting = false, reviews = withOptimistic(refreshed, text, rating))
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, submitError = parseError(e))
            }
        }
    }

    fun clearSubmitError() { _state.value = _state.value.copy(submitError = null) }

    // The just-submitted review can be missing from the immediate refetch (read-after-write lag,
    // or sort=popular pushing a 0-like review to the bottom). Prepend an optimistic copy so the
    // user sees it right away, unless the refetch already contains it.
    private fun withOptimistic(refreshed: List<SongReview>, text: String, rating: Int): List<SongReview> {
        val uid = currentUser?.user_id ?: return refreshed
        val mine = text.trim()
        val present = refreshed.any { it.user?.user_id == uid && it.review_text?.trim() == mine }
        if (present) return refreshed
        val optimistic = SongReview(
            review_id = "local-${mine.hashCode()}",
            spotify_track_id = trackId,
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
