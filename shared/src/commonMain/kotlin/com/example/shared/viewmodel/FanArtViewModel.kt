package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.BlockRequest
import com.example.shared.data.model.FanArtComment
import com.example.shared.data.model.FanArtCreateRequest
import com.example.shared.data.model.FanArtPost
import com.example.shared.data.model.PostMessageRequest
import com.example.shared.data.model.ReportRequest
import com.example.shared.network.SoundSpireApi
import com.example.shared.supabase.SupabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FanArtUiState(
    val loading: Boolean = true,
    val posts: List<FanArtPost> = emptyList(),
)

// Port of FanArtScreen.kt: backend posts enriched with reactions + comments from Supabase.
class FanArtViewModel(
    private val api: SoundSpireApi,
    private val forumId: String,
    private val currentUserId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(FanArtUiState())
    val state: StateFlow<FanArtUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            try {
                val backendPosts = api.getFanArt(forumId).posts
                val rawRows = runCatching { SupabaseManager.fetchMessages(forumId) }.getOrDefault(emptyList())
                val blockedIds = runCatching { api.getBlocks().blocks.map { it.blocked_user_id }.toSet() }.getOrDefault(emptySet())
                val rows = rawRows.filter { it.user_id == null || it.user_id !in blockedIds }
                val byId = rows.associateBy { it.forum_post_id }
                val childrenByParent = rows.filter { it.parent_post_id != null }.groupBy { it.parent_post_id }
                val userMap = rows.filter { it.parent_post_id != null }.mapNotNull { it.user_id }.distinct()
                    .associateWith { uid -> runCatching { api.getUserById(uid).user }.getOrNull() }

                fun toComments(rowId: String): List<FanArtComment> =
                    childrenByParent[rowId].orEmpty().map { r ->
                        FanArtComment(
                            forum_post_id = r.forum_post_id, user_id = r.user_id, parent_post_id = r.parent_post_id,
                            content = r.content, created_at = r.created_at,
                            user = r.user_id?.let { userMap[it] }, reactions = r.reactions,
                        )
                    }

                val posts = backendPosts.map { p ->
                    p.copy(reactions = byId[p.forum_post_id]?.reactions, comments = toComments(p.forum_post_id))
                }
                _state.value = FanArtUiState(loading = false, posts = posts)
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun like(postId: String) {
        viewModelScope.launch { runCatching { api.likeFanArt(postId) } }
    }

    fun react(postId: String, emoji: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            runCatching {
                val resp = api.reactToFanArt(forumId, postId, mapOf("userId" to currentUserId, "emoji" to emoji))
                resp.reactions?.let { newR ->
                    _state.value = _state.value.copy(posts = _state.value.posts.map { p ->
                        if (p.forum_post_id == postId) p.copy(reactions = newR)
                        else p.copy(comments = p.comments.map { c -> if (c.forum_post_id == postId) c.copy(reactions = newR) else c })
                    })
                }
            }
        }
    }

    fun addComment(parentPostId: String, content: String) {
        if (currentUserId == null || content.isBlank()) return
        viewModelScope.launch {
            runCatching { api.postForumMessage(forumId, PostMessageRequest(content = content, media_type = "text", parent_post_id = parentPostId)) }
            reload()
        }
    }

    fun report(postId: String, reason: String, details: String?) {
        viewModelScope.launch { runCatching { api.submitReport(ReportRequest("fan_art", postId, reason, details?.ifBlank { null })) } }
    }

    fun block(userId: String) {
        _state.value = _state.value.copy(posts = _state.value.posts.filter { it.user_id != userId })
        viewModelScope.launch { runCatching { api.blockUser(BlockRequest(userId)) } }
    }

    fun createFanArt(title: String, description: String, imageUrls: List<String>) {
        if (imageUrls.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                api.createFanArt(forumId, FanArtCreateRequest(
                    title = title.ifBlank { "Fan Art" },
                    content = description.ifBlank { null },
                    imageUrls = imageUrls,
                ))
            }
            reload()
        }
    }
}
