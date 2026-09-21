package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.BlockRequest
import com.example.shared.data.model.EditMessageRequest
import com.example.shared.data.model.ForumMessage
import com.example.shared.data.model.PostMessageRequest
import com.example.shared.data.model.ReportRequest
import com.example.shared.network.SoundSpireApi
import com.example.shared.supabase.SupabaseManager
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class AllChatUiState(
    val loading: Boolean = true,
    val messages: List<ForumMessage> = emptyList(),
    val onlineUsers: List<SupabaseManager.OnlineUser> = emptyList(),
)

// Port of AllChatScreen.kt's data layer: direct Supabase read + realtime + presence,
// with authed writes (send/edit/delete/react) and moderation (report/block).
class AllChatViewModel(
    private val api: SoundSpireApi,
    private val forumId: String,
    private val currentUserId: String?,
    private val communityId: String?,
    private val currentUserName: String?,
    private val enablePresence: Boolean = true,
) : ViewModel() {

    private val _state = MutableStateFlow(AllChatUiState())
    val state: StateFlow<AllChatUiState> = _state.asStateFlow()
    private var blockedIds: Set<String> = emptySet()

    init {
        loadMessages()
        subscribeRealtime()
        // Suggestions reuses this VM but has no "active now" UI.
        if (enablePresence) trackPresence()
    }

    private fun parseReactions(rec: JsonObject?): Map<String, List<String>>? = try {
        (rec?.get("reactions") as? JsonObject)?.mapValues { (_, v) ->
            (v as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        }
    } catch (_: Exception) { null }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                val rows = SupabaseManager.fetchMessages(forumId)
                blockedIds = runCatching { api.getBlocks().blocks.map { it.blocked_user_id }.toSet() }.getOrDefault(emptySet())
                val visible = rows.filter { it.user_id == null || it.user_id !in blockedIds }
                val userMap = visible.mapNotNull { it.user_id }.distinct().associateWith { uid ->
                    runCatching { api.getUserById(uid).user }.getOrNull()
                }
                val msgs = visible.map { r ->
                    ForumMessage(
                        forum_post_id = r.forum_post_id, forum_id = r.forum_id, user_id = r.user_id,
                        content = r.content, media_type = r.media_type, media_urls = r.media_urls,
                        parent_post_id = r.parent_post_id, created_at = r.created_at,
                        user = r.user_id?.let { userMap[it] }, reactions = r.reactions,
                    )
                }
                _state.value = _state.value.copy(loading = false, messages = msgs)
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            runCatching {
                SupabaseManager.forumChanges(forumId).collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val rec = action.record
                            val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull ?: return@collect
                            if (_state.value.messages.none { it.forum_post_id == postId }) {
                                val uid = rec["user_id"]?.jsonPrimitive?.contentOrNull
                                if (uid != null && uid in blockedIds) return@collect
                                val mediaUrls = (rec["media_urls"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }
                                val user = runCatching { uid?.let { api.getUserById(it).user } }.getOrNull()
                                val msg = ForumMessage(
                                    forum_post_id = postId, forum_id = forumId, user_id = uid,
                                    content = rec["content"]?.jsonPrimitive?.contentOrNull,
                                    media_type = rec["media_type"]?.jsonPrimitive?.contentOrNull,
                                    media_urls = mediaUrls,
                                    parent_post_id = rec["parent_post_id"]?.jsonPrimitive?.contentOrNull,
                                    created_at = rec["created_at"]?.jsonPrimitive?.contentOrNull,
                                    user = user, reactions = parseReactions(rec),
                                )
                                _state.value = _state.value.copy(messages = _state.value.messages + msg)
                            }
                        }
                        is PostgresAction.Update -> {
                            val rec = action.record
                            val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull ?: return@collect
                            val newContent = rec["content"]?.jsonPrimitive?.contentOrNull
                            val newReactions = parseReactions(rec)
                            _state.value = _state.value.copy(messages = _state.value.messages.map {
                                if (it.forum_post_id == postId) it.copy(content = newContent ?: it.content, reactions = newReactions) else it
                            })
                        }
                        is PostgresAction.Delete -> {
                            val postId = action.oldRecord["forum_post_id"]?.jsonPrimitive?.contentOrNull
                            if (postId != null) _state.value = _state.value.copy(messages = _state.value.messages.filter { it.forum_post_id != postId })
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun trackPresence() {
        if (communityId != null && currentUserId != null) {
            viewModelScope.launch {
                runCatching {
                    SupabaseManager.communityPresence(communityId, currentUserId, currentUserName ?: "User").collect {
                        _state.value = _state.value.copy(onlineUsers = it)
                    }
                }
            }
        }
    }

    fun send(text: String, mediaUrls: List<String>, parentId: String?) {
        if (text.isBlank() && mediaUrls.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                api.postForumMessage(forumId, PostMessageRequest(
                    content = text,
                    media_type = if (mediaUrls.isNotEmpty()) "image" else "text",
                    media_urls = mediaUrls,
                    parent_post_id = parentId,
                ))
            }
            // The realtime INSERT adds it to the list.
        }
    }

    fun edit(id: String, text: String) {
        _state.value = _state.value.copy(messages = _state.value.messages.map { if (it.forum_post_id == id) it.copy(content = text) else it })
        viewModelScope.launch { runCatching { api.editForumMessage(forumId, id, EditMessageRequest(text)) } }
    }

    fun delete(id: String) {
        _state.value = _state.value.copy(messages = _state.value.messages.filter { it.forum_post_id != id })
        viewModelScope.launch { runCatching { api.deleteForumMessage(forumId, id) } }
    }

    fun react(id: String, emoji: String) {
        viewModelScope.launch {
            runCatching {
                val resp = api.reactToMessage(forumId, id, mapOf("userId" to (currentUserId ?: ""), "emoji" to emoji))
                resp.reactions?.let { newR ->
                    _state.value = _state.value.copy(messages = _state.value.messages.map { if (it.forum_post_id == id) it.copy(reactions = newR) else it })
                }
            }
        }
    }

    fun report(id: String, reason: String, details: String?) {
        viewModelScope.launch { runCatching { api.submitReport(ReportRequest("chat_message", id, reason, details?.ifBlank { null })) } }
    }

    fun block(userId: String) {
        _state.value = _state.value.copy(messages = _state.value.messages.filter { it.user_id != userId })
        viewModelScope.launch { runCatching { api.blockUser(BlockRequest(userId)) } }
    }
}
