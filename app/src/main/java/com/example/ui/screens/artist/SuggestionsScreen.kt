package com.example.ui.screens.artist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.ForumMessage
import com.example.data.remote.PostMessageRequest
import com.example.data.remote.SupabaseManager
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.S3Uploader
import com.example.util.resolveImageUrl
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val SuggOrange = Color(0xFFFA6400)
private val SuggBg = Color(0xFF1A0A2E)

private fun parseReactionsRec(rec: JsonObject?): Map<String, List<String>>? = try {
    (rec?.get("reactions") as? JsonObject)?.mapValues { (_, v) ->
        (v as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    }
} catch (_: Exception) { null }

/**
 * Minimal realtime suggestions board. Subscribers post text+image suggestions and react;
 * only the community artist (isArtist) sees a reply box. No presence, no threads UI.
 * Reuses the forum message pipeline (Supabase read + realtime, authed writes).
 */
@Composable
fun SuggestionsScreen(
    forumId: String,
    currentUserId: String?,
    communityId: String? = null,
    currentUserName: String? = null,
    isArtist: Boolean = false,
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var messages by remember { mutableStateOf<List<ForumMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<Uri?>(null) }
    var sending by remember { mutableStateOf(false) }
    var replyDrafts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val reactionEmojis = listOf("👍", "❤️", "🔥", "💡")

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingImage = uri
    }

    LaunchedEffect(forumId) {
        try {
            val rows = SupabaseManager.fetchMessages(forumId)
            val blocked = try { api.getBlocks().blocks.map { it.blocked_user_id }.toSet() } catch (_: Exception) { emptySet() }
            val visible = rows.filter { it.user_id == null || it.user_id !in blocked }
            val userMap = visible.mapNotNull { it.user_id }.distinct().associateWith { uid ->
                try { api.getUserById(uid).user } catch (_: Exception) { null }
            }
            messages = visible.map { r ->
                ForumMessage(
                    forum_post_id = r.forum_post_id, forum_id = r.forum_id, user_id = r.user_id,
                    content = r.content, media_type = r.media_type, media_urls = r.media_urls,
                    parent_post_id = r.parent_post_id, created_at = r.created_at,
                    user = r.user_id?.let { userMap[it] }, reactions = r.reactions,
                )
            }
        } catch (_: Exception) { }
        loading = false
    }

    LaunchedEffect(forumId) {
        try {
            SupabaseManager.forumChanges(forumId).collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val rec = action.record
                        val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull ?: return@collect
                        if (messages.none { it.forum_post_id == postId }) {
                            val uid = rec["user_id"]?.jsonPrimitive?.contentOrNull
                            val mediaUrls = try { (rec["media_urls"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } } catch (_: Exception) { null }
                            val user = try { uid?.let { api.getUserById(it).user } } catch (_: Exception) { null }
                            messages = messages + ForumMessage(
                                forum_post_id = postId, forum_id = forumId, user_id = uid,
                                content = rec["content"]?.jsonPrimitive?.contentOrNull,
                                media_type = rec["media_type"]?.jsonPrimitive?.contentOrNull,
                                media_urls = mediaUrls,
                                parent_post_id = rec["parent_post_id"]?.jsonPrimitive?.contentOrNull,
                                created_at = rec["created_at"]?.jsonPrimitive?.contentOrNull,
                                user = user, reactions = parseReactionsRec(rec),
                            )
                        }
                    }
                    is PostgresAction.Update -> {
                        val rec = action.record
                        val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull ?: return@collect
                        val newReactions = parseReactionsRec(rec)
                        messages = messages.map { if (it.forum_post_id == postId) it.copy(reactions = newReactions) else it }
                    }
                    is PostgresAction.Delete -> {
                        val postId = action.oldRecord["forum_post_id"]?.jsonPrimitive?.contentOrNull
                        if (postId != null) messages = messages.filter { it.forum_post_id != postId }
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) { }
    }

    fun toggleReaction(mid: String, emoji: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.reactToMessage(forumId, mid, mapOf("userId" to (currentUserId ?: ""), "emoji" to emoji)).reactions?.let { newR ->
                    messages = messages.map { if (it.forum_post_id == mid) it.copy(reactions = newR) else it }
                }
            } catch (_: Exception) { }
        }
    }

    fun post(text: String, parentId: String?, img: Uri?) {
        if (text.isBlank() && img == null) return
        sending = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val mediaUrls = mutableListOf<String>()
                if (img != null) S3Uploader.upload(context, img, "suggestions/${System.nanoTime()}.jpg")?.let { mediaUrls.add(it) }
                api.postForumMessage(forumId, PostMessageRequest(
                    content = text,
                    media_type = if (mediaUrls.isNotEmpty()) "image" else "text",
                    media_urls = mediaUrls,
                    parent_post_id = parentId,
                ))
            } catch (_: Exception) { }
            sending = false
        }
    }

    val topLevel = messages.filter { it.parent_post_id == null }
    val repliesByParent = messages.filter { it.parent_post_id != null }.groupBy { it.parent_post_id }

    Column(modifier = Modifier.fillMaxSize().background(SuggBg)) {
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SuggOrange, modifier = Modifier.size(28.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                if (topLevel.isEmpty()) {
                    item { Text("No suggestions yet. Be the first!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(24.dp)) }
                }
                items(topLevel, key = { it.forum_post_id }) { m ->
                    SuggestionCard(m, repliesByParent[m.forum_post_id].orEmpty(), currentUserId, reactionEmojis, isArtist,
                        onReact = { e -> toggleReaction(m.forum_post_id, e) },
                        replyDraft = replyDrafts[m.forum_post_id] ?: "",
                        onReplyChange = { v -> replyDrafts = replyDrafts + (m.forum_post_id to v) },
                        onReplySend = {
                            val d = (replyDrafts[m.forum_post_id] ?: "").trim()
                            if (d.isNotEmpty()) { post(d, m.forum_post_id, null); replyDrafts = replyDrafts - m.forum_post_id }
                        })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Composer
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0612)).padding(8.dp)) {
            if (pendingImage != null) {
                Box(modifier = Modifier.padding(bottom = 6.dp)) {
                    AsyncImage(model = pendingImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(18.dp).clickable { pendingImage = null })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Default.Image, "Add image", tint = TextMuted)
                }
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("Share a suggestion...", color = TextMuted) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2D2838), unfocusedContainerColor = Color(0xFF2D2838), focusedBorderColor = SuggOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                IconButton(enabled = !sending, onClick = {
                    val text = input.trim(); val img = pendingImage
                    if (text.isBlank() && img == null) return@IconButton
                    input = ""; pendingImage = null
                    post(text, null, img)
                }) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SuggOrange, strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, "Post", tint = SuggOrange)
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    m: ForumMessage,
    replies: List<ForumMessage>,
    currentUserId: String?,
    reactionEmojis: List<String>,
    isArtist: Boolean,
    onReact: (String) -> Unit,
    replyDraft: String,
    onReplyChange: (String) -> Unit,
    onReplySend: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1625)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = resolveImageUrl(m.user?.profile_picture_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.DarkGray))
            Spacer(modifier = Modifier.width(8.dp))
            Text(m.user?.username ?: "User", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (!m.content.isNullOrBlank()) { Spacer(modifier = Modifier.height(6.dp)); Text(m.content!!, color = Color(0xFFE5E5E5), fontSize = 15.sp) }
        m.media_urls?.firstOrNull()?.let { url ->
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(model = resolveImageUrl(url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(8.dp)))
        }
        // Reactions
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            reactionEmojis.forEach { e ->
                val users = m.reactions?.get(e) ?: emptyList()
                val mine = currentUserId != null && users.contains(currentUserId)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (mine) SuggOrange.copy(alpha = 0.25f) else Color(0xFF2D2838))
                        .then(if (mine) Modifier.border(1.dp, SuggOrange, RoundedCornerShape(50)) else Modifier)
                        .clickable { onReact(e) }.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(e, fontSize = 13.sp)
                    if (users.isNotEmpty()) { Spacer(modifier = Modifier.width(3.dp)); Text("${users.size}", color = if (mine) Color.White else TextMuted, fontSize = 11.sp) }
                }
            }
        }
        // Artist replies
        replies.forEach { r ->
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                Text("${r.user?.username ?: "Artist"} · artist", color = SuggOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (!r.content.isNullOrBlank()) Text(r.content!!, color = Color(0xFFD1D5DB), fontSize = 13.sp)
            }
        }
        // Artist-only reply box
        if (isArtist) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replyDraft, onValueChange = onReplyChange,
                    placeholder = { Text("Reply as artist...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2D2838), unfocusedContainerColor = Color(0xFF2D2838), focusedBorderColor = SuggOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                IconButton(onClick = onReplySend) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = SuggOrange) }
            }
        }
    }
}
