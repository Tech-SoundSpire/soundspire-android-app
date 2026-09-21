package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CommunityPost
import com.example.shared.data.model.CommunityPostCreateRequest
import com.example.shared.data.model.PostLike
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForumUiState(
    val loading: Boolean = true,
    val posts: List<CommunityPost> = emptyList(),
    val posting: Boolean = false,
)

// Port of ArtistForumScreen.kt: community forum posts (announcements). Artist-only composer.
// Same like/comment logic as the Feed; used by both the fan community detail and artist dashboard.
class ForumViewModel(
    private val api: SoundSpireApi,
    private val communityId: String,
    private val artistId: String?,
    val currentUserId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(ForumUiState())
    val state: StateFlow<ForumUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val posts = runCatching { api.getCommunityPosts(communityId) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(loading = false, posts = posts)
        }
    }

    fun toggleLike(post: CommunityPost) {
        val uid = currentUserId ?: return
        val liked = post.likes.any { it.user_id == uid }
        _state.value = _state.value.copy(posts = _state.value.posts.map {
            if (it.post_id == post.post_id) {
                val newLikes = if (liked) it.likes.filter { l -> l.user_id != uid } else it.likes + PostLike(user_id = uid)
                it.copy(likes = newLikes)
            } else it
        })
        viewModelScope.launch {
            try {
                val body = mapOf("user_id" to uid, "post_id" to post.post_id)
                if (liked) api.unlikePost(body) else api.likePost(body)
            } catch (_: Exception) { reload() }
        }
    }

    fun submitComment(postId: String, content: String, parentId: String?) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                val body = buildMap {
                    put("user_id", uid); put("content", content); put("post_id", postId)
                    if (parentId != null) put("parent_comment_id", parentId)
                }
                api.commentOnPost(body)
            } catch (_: Exception) {}
            reload()
        }
    }

    // mediaUrls are already-uploaded s3 paths (SwiftUI uploads via MediaUploader first).
    fun createPost(content: String, mediaUrls: List<String>) {
        val aid = artistId ?: return
        _state.value = _state.value.copy(posting = true)
        viewModelScope.launch {
            runCatching {
                api.createCommunityPost(CommunityPostCreateRequest(artist_id = aid, community_id = communityId, content_text = content, media_urls = mediaUrls))
            }
            _state.value = _state.value.copy(posting = false)
            reload()
        }
    }
}
