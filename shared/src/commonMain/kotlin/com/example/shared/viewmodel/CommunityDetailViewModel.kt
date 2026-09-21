package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CommunitySlugArtist
import com.example.shared.data.model.SongReview
import com.example.shared.data.model.SubscribeRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

data class CommunityDetailUiState(
    val loading: Boolean = true,
    val artist: CommunitySlugArtist? = null,
    val subscriberCount: Int = 0,
    val isSubscribed: Boolean = false,
    val isOwnCommunity: Boolean = false,
    val subBusy: Boolean = false,
    val reviews: List<SongReview> = emptyList(),
    val chatForumId: String? = null,
    val fanArtForumId: String? = null,
    val suggestionsForumId: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
)

// Port of CommunityDetailScreen.kt's data + subscribe/unsubscribe.
class CommunityDetailViewModel(
    private val api: SoundSpireApi,
    private val slug: String,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityDetailUiState())
    val state: StateFlow<CommunityDetailUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val artist = api.getCommunityBySlug(slug).artist
                val sessionUser = runCatching { api.getSession().user }.getOrNull()
                val isOwn = sessionUser?.artistId != null && sessionUser.artistId == artist?.artist_id
                val reviews = artist?.artist_id?.let {
                    runCatching { api.getReviewsByArtist(it).reviews }.getOrDefault(emptyList())
                } ?: emptyList()

                var subCount = 0
                var subscribed = false
                var chatId: String? = null
                var fanArtId: String? = null
                var sugId: String? = null
                val cid = artist?.community?.community_id
                if (cid != null) {
                    subCount = runCatching { api.getSubscriberCount(cid).count }.getOrDefault(0)
                    if (sessionUser?.id != null) {
                        subscribed = runCatching { api.getSubscriptionStatus(sessionUser.id, cid).subscribed }.getOrDefault(false)
                    }
                    val forums = runCatching { api.getCommunityForums(cid).forums }.getOrDefault(emptyList())
                    chatId = forums.firstOrNull { (it.name ?: "").contains("chat", true) || (it.forum_type ?: "").contains("chat", true) }?.forum_id
                        ?: forums.firstOrNull()?.forum_id
                    fanArtId = forums.firstOrNull { (it.name ?: "").contains("art", true) || (it.forum_type ?: "").contains("art", true) }?.forum_id
                    sugId = forums.firstOrNull { (it.name ?: "").contains("suggest", true) || (it.forum_type ?: "").contains("suggest", true) }?.forum_id
                }
                _state.value = CommunityDetailUiState(
                    loading = false, artist = artist, subscriberCount = subCount, isSubscribed = subscribed,
                    isOwnCommunity = isOwn, reviews = reviews, chatForumId = chatId, fanArtForumId = fanArtId,
                    suggestionsForumId = sugId,
                    currentUserId = sessionUser?.id, currentUserName = sessionUser?.name,
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun toggleSubscribe() {
        val s = _state.value
        val uid = s.currentUserId ?: return
        val cid = s.artist?.community?.community_id ?: return
        if (s.subBusy) return
        _state.value = s.copy(subBusy = true)
        val wasSubscribed = s.isSubscribed
        viewModelScope.launch {
            try {
                if (wasSubscribed) {
                    api.unsubscribeFromCommunity(uid, cid)
                    _state.value = _state.value.copy(isSubscribed = false, subscriberCount = (_state.value.subscriberCount - 1).coerceAtLeast(0))
                } else {
                    val now = Clock.System.now()
                    val nowIso = now.toString()
                    val endIso = now.plus(30.days).toString()
                    api.subscribeToCommunity(
                        SubscribeRequest(
                            user_id = uid, community_id = cid,
                            start_date = nowIso, end_date = endIso,
                            created_at = nowIso, updated_at = nowIso,
                        ),
                    )
                    _state.value = _state.value.copy(isSubscribed = true, subscriberCount = _state.value.subscriberCount + 1)
                }
            } catch (_: Exception) {}
            _state.value = _state.value.copy(subBusy = false)
        }
    }
}
