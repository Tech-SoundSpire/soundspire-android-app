package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.CommunityPost
import com.example.shared.data.model.PostLike
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = true,
    val posts: List<CommunityPost> = emptyList(),
    val currentUserId: String? = null,
)

// Port of FeedScreen.kt. Optimistic like toggle; reload on comment or on failure.
class FeedViewModel(private val api: SoundSpireApi) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            try {
                val userId = api.getSession().user?.id ?: return@launch
                val posts = api.getPosts(userId)
                _state.value = _state.value.copy(loading = false, posts = posts, currentUserId = userId)
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun toggleLike(post: CommunityPost) {
        val uid = _state.value.currentUserId ?: return
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
        val uid = _state.value.currentUserId ?: return
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
}
