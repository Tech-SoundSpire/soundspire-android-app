package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ArtistEditRequest
import com.example.shared.data.model.ArtistMe
import com.example.shared.data.model.ArtistSocial
import com.example.shared.data.model.CommunityHighlight
import com.example.shared.data.model.NotificationItem
import com.example.shared.data.model.SongReview
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtistDashboardUiState(
    val loading: Boolean = true,
    val artist: ArtistMe? = null,
    val reviews: List<SongReview> = emptyList(),
    val currentUserId: String? = null,
    val subscriberCount: Int = 0,
    val chatForumId: String? = null,
    val fanArtForumId: String? = null,
    val suggestionsForumId: String? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val saving: Boolean = false,
)

// Port of ArtistDashboardScreen.kt. About/Home tab + edit (bio/socials/images). All-Chat and
// Fan-Art tabs reuse the shared AllChat/FanArt view models in SwiftUI.
class ArtistDashboardViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(ArtistDashboardUiState())
    val state: StateFlow<ArtistDashboardUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val artist = runCatching { api.getArtistMe().artist }.getOrNull()
            val uid = runCatching { api.getSession().user?.id }.getOrNull()
            val notifs = runCatching { api.getNotifications().notifications }.getOrDefault(emptyList())
            val reviews = artist?.artist_id?.let { runCatching { api.getReviewsByArtist(it).reviews }.getOrDefault(emptyList()) } ?: emptyList()
            var subs = 0; var chatId: String? = null; var artId: String? = null; var sugId: String? = null
            artist?.community?.community_id?.let { cid ->
                subs = runCatching { api.getSubscriberCount(cid).count }.getOrDefault(0)
                runCatching {
                    val forums = api.getCommunityForums(cid).forums
                    chatId = forums.firstOrNull { (it.name ?: "").contains("chat", true) || (it.forum_type ?: "").contains("chat", true) }?.forum_id ?: forums.firstOrNull()?.forum_id
                    artId = forums.firstOrNull { (it.name ?: "").contains("art", true) || (it.forum_type ?: "").contains("art", true) }?.forum_id
                    sugId = forums.firstOrNull { (it.name ?: "").contains("suggest", true) || (it.forum_type ?: "").contains("suggest", true) }?.forum_id
                }
            }
            _state.value = _state.value.copy(
                loading = false, artist = artist, reviews = reviews, currentUserId = uid,
                notifications = notifs, subscriberCount = subs, chatForumId = chatId, fanArtForumId = artId,
                suggestionsForumId = sugId,
            )
        }
    }

    fun markNotificationsRead() {
        _state.value = _state.value.copy(notifications = _state.value.notifications.map { it.copy(is_read = true) })
        viewModelScope.launch { runCatching { api.markNotificationsRead(mapOf("notificationIds" to "all")) } }
    }

    fun saveProfile(bio: String, socials: List<ArtistSocial>, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            runCatching { api.editArtistMe(ArtistEditRequest(bio = bio, socials = socials.filter { it.url.isNotBlank() })) }
            _state.value = _state.value.copy(saving = false)
            reload(); onDone()
        }
    }

    fun saveHighlights(highlights: List<CommunityHighlight>, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            val cleaned = highlights.map { it.copy(text = it.text.trim().take(120)) }
                .filter { !it.imageUrl.isNullOrBlank() || it.text.isNotBlank() }
            runCatching { api.editArtistMe(ArtistEditRequest(highlights = cleaned)) }
            _state.value = _state.value.copy(saving = false)
            reload(); onDone()
        }
    }

    fun updateImage(profilePictureUrl: String?, coverPhotoUrl: String?) {
        viewModelScope.launch {
            runCatching { api.editArtistMe(ArtistEditRequest(profile_picture_url = profilePictureUrl, cover_photo_url = coverPhotoUrl)) }
            reload()
        }
    }
}
