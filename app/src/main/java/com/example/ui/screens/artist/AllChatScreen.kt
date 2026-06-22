package com.example.ui.screens.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.ForumMessage
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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val ArtistOrange = Color(0xFFFA6400)
private val Bg = Color(0xFF1A0A2E)

/** Parse the JSONB `reactions` column ({emoji: [userId,...]}) from a realtime record. */
private fun parseReactions(rec: JsonObject?): Map<String, List<String>>? {
    return try {
        (rec?.get("reactions") as? JsonObject)?.mapValues { (_, v) ->
            (v as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        }
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllChatScreen(
    forumId: String,
    currentUserId: String?,
    communityId: String? = null,
    communityName: String? = null,
    artistName: String? = null,
    currentUserName: String? = null,
    memberCount: Int = 0,
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var messages by remember { mutableStateOf<List<ForumMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<Uri?>(null) }
    var sending by remember { mutableStateOf(false) }
    var onlineUsers by remember { mutableStateOf<List<SupabaseManager.OnlineUser>>(emptyList()) }
    var showOnlineList by remember { mutableStateOf(false) }
    var selectedMsgId by remember { mutableStateOf<String?>(null) }
    var replyingTo by remember { mutableStateOf<ForumMessage?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var expandedThreads by remember { mutableStateOf<Set<String>>(emptySet()) }
    val listState = rememberLazyListState()
    val reactionEmojis = listOf("👍", "❤️", "😂", "🔥", "🎵")

    // Presence: track + observe online users
    LaunchedEffect(communityId, currentUserId) {
        if (communityId != null && currentUserId != null) {
            try {
                SupabaseManager.communityPresence(communityId, currentUserId, currentUserName ?: "User").collect { online ->
                    onlineUsers = online
                }
            } catch (_: Exception) { }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingImage = uri
    }

    // Initial fetch directly from Supabase (mirrors the website). The backend /messages
    // endpoint strips parent_post_id + reactions, which flattened threads and dropped
    // reactions on every remount — reading from Supabase preserves both.
    LaunchedEffect(forumId) {
        try {
            val rows = SupabaseManager.fetchMessages(forumId)
            // Enrich each row with its author (deduped — backend getUserById bypasses RLS)
            val userIds = rows.mapNotNull { it.user_id }.distinct()
            val userMap = userIds.associateWith { uid ->
                try { api.getUserById(uid).user } catch (_: Exception) { null }
            }
            messages = rows.map { r ->
                ForumMessage(
                    forum_post_id = r.forum_post_id,
                    forum_id = r.forum_id,
                    user_id = r.user_id,
                    content = r.content,
                    media_type = r.media_type,
                    media_urls = r.media_urls,
                    parent_post_id = r.parent_post_id,
                    created_at = r.created_at,
                    user = r.user_id?.let { userMap[it] },
                    reactions = r.reactions,
                )
            }
        } catch (e: Exception) { android.util.Log.e("AllChat", "initial fetch failed", e) }
        loading = false
    }

    // Realtime subscription via Supabase
    LaunchedEffect(forumId) {
        try {
            SupabaseManager.forumChanges(forumId).collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val rec = action.record
                        val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull ?: return@collect
                        if (messages.none { it.forum_post_id == postId }) {
                            val uid = rec["user_id"]?.jsonPrimitive?.contentOrNull
                            val mediaUrls = try {
                                (rec["media_urls"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }
                            } catch (_: Exception) { null }
                            // Enrich with user info from backend (bypasses RLS)
                            val user = try { uid?.let { api.getUserById(it).user } } catch (_: Exception) { null }
                            val msg = ForumMessage(
                                forum_post_id = postId,
                                forum_id = forumId,
                                user_id = uid,
                                content = rec["content"]?.jsonPrimitive?.contentOrNull,
                                media_type = rec["media_type"]?.jsonPrimitive?.contentOrNull,
                                media_urls = mediaUrls,
                                parent_post_id = rec["parent_post_id"]?.jsonPrimitive?.contentOrNull,
                                created_at = rec["created_at"]?.jsonPrimitive?.contentOrNull,
                                user = user,
                                reactions = parseReactions(rec),
                            )
                            messages = messages + msg
                        }
                    }
                    is PostgresAction.Update -> {
                        val rec = action.record
                        val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull ?: return@collect
                        val newContent = rec["content"]?.jsonPrimitive?.contentOrNull
                        val newReactions = parseReactions(rec)
                        messages = messages.map {
                            if (it.forum_post_id == postId) it.copy(content = newContent ?: it.content, reactions = newReactions) else it
                        }
                    }
                    is PostgresAction.Delete -> {
                        val rec = action.oldRecord
                        val postId = rec["forum_post_id"]?.jsonPrimitive?.contentOrNull
                        if (postId != null) messages = messages.filter { it.forum_post_id != postId }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) { android.util.Log.e("AllChat", "realtime subscription failed", e) }
    }

    // Top-level messages and replies grouped by parent (Slack-style threads)
    val topLevel = messages.filter { it.parent_post_id == null }
    val repliesByParent = messages.filter { it.parent_post_id != null }.groupBy { it.parent_post_id }

    // Auto-scroll to bottom on new top-level message
    LaunchedEffect(topLevel.size) {
        if (topLevel.isNotEmpty()) listState.animateScrollToItem(topLevel.size - 1)
    }

    fun toggleReaction(mid: String, emoji: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = api.reactToMessage(forumId, mid, mapOf("userId" to (currentUserId ?: ""), "emoji" to emoji))
                resp.reactions?.let { newR ->
                    messages = messages.map { if (it.forum_post_id == mid) it.copy(reactions = newR) else it }
                }
            } catch (_: Exception) { }
        }
    }

    // Reusable message bubble (used for both top-level messages and thread replies)
    val MessageBubble: @Composable (ForumMessage, Boolean) -> Unit = { msg, isReply ->
        val isOwn = msg.user_id == currentUserId
        val avatarSize = if (isReply) 26.dp else 28.dp
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
            ) {
                if (!isOwn) {
                    AsyncImage(model = resolveImageUrl(msg.user?.profile_picture_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(avatarSize).clip(CircleShape).background(Color.DarkGray))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(
                    modifier = Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isOwn) ArtistOrange else Color(0xFF2D2838))
                        .clickable { selectedMsgId = if (selectedMsgId == msg.forum_post_id) null else msg.forum_post_id }
                        .padding(10.dp)
                ) {
                    if (!isOwn && msg.user?.username != null) {
                        Text(msg.user!!.username!!, color = ArtistOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!msg.media_urls.isNullOrEmpty()) {
                        AsyncImage(model = resolveImageUrl(msg.media_urls!!.first()), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    if (editingId == msg.forum_post_id) {
                        var editText by remember(msg.forum_post_id) { mutableStateOf(msg.content ?: "") }
                        OutlinedTextField(value = editText, onValueChange = { editText = it }, modifier = Modifier.fillMaxWidth(), singleLine = false, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1A1625), unfocusedContainerColor = Color(0xFF1A1625), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Save", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                val newText = editText
                                editingId = null
                                CoroutineScope(Dispatchers.IO).launch { try { SupabaseManager.updateMessageContent(msg.forum_post_id, newText) } catch (_: Exception) {} }
                                messages = messages.map { if (it.forum_post_id == msg.forum_post_id) it.copy(content = newText) else it }
                            })
                            Text("Cancel", color = TextMuted, fontSize = 12.sp, modifier = Modifier.clickable { editingId = null })
                        }
                    } else if (!msg.content.isNullOrBlank()) {
                        Text(msg.content!!, color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            // Reactions row (chips)
            val reactions = msg.reactions
            if (!reactions.isNullOrEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 2.dp, start = if (isOwn) 0.dp else 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reactions.forEach { (emoji, users) ->
                        if (users.isNotEmpty()) {
                            val mine = currentUserId != null && users.contains(currentUserId)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                    .background(if (mine) ArtistOrange else Color(0xFF2D2838))
                                    .clickable { toggleReaction(msg.forum_post_id, emoji) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${users.size}", color = if (mine) Color.White else TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Action bar (shown when message tapped)
            if (selectedMsgId == msg.forum_post_id && editingId == null) {
                Row(modifier = Modifier.padding(top = 2.dp, start = if (isOwn) 0.dp else 36.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    reactionEmojis.forEach { emoji ->
                        Text(emoji, fontSize = 16.sp, modifier = Modifier.clickable {
                            selectedMsgId = null
                            toggleReaction(msg.forum_post_id, emoji)
                        })
                    }
                    // Only top-level messages can start a thread
                    if (!isReply) {
                        Text("Reply", color = ArtistOrange, fontSize = 12.sp, modifier = Modifier.clickable { replyingTo = msg; selectedMsgId = null })
                    }
                    if (isOwn) {
                        Text("Edit", color = TextMuted, fontSize = 12.sp, modifier = Modifier.clickable { editingId = msg.forum_post_id; selectedMsgId = null })
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.clickable {
                            selectedMsgId = null
                            messages = messages.filter { it.forum_post_id != msg.forum_post_id }
                            CoroutineScope(Dispatchers.IO).launch { try { SupabaseManager.deleteMessage(msg.forum_post_id) } catch (_: Exception) {} }
                        })
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Community info header (matches website)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF221C2F)).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (communityName != null) Text(communityName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("#${artistName ?: ""}", color = ArtistOrange, fontSize = 12.sp)
                Text("$memberCount ${if (memberCount == 1) "member" else "members"}", color = TextMuted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showOnlineList = !showOnlineList }) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${onlineUsers.size} online", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (showOnlineList) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2838)).padding(12.dp)) {
                Text("Online now", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                if (onlineUsers.isEmpty()) Text("No one online", color = TextMuted, fontSize = 12.sp)
                else onlineUsers.forEach { u ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(u.username, color = TextWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(28.dp))
            }
        } else if (topLevel.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No messages yet. Say hi!", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                items(topLevel.size) { idx ->
                    val msg = topLevel[idx]
                    val replies = repliesByParent[msg.forum_post_id].orEmpty()
                    val expanded = expandedThreads.contains(msg.forum_post_id)

                    MessageBubble(msg, false)

                    // Thread toggle + indented replies (Slack-style)
                    if (replies.isNotEmpty()) {
                        Text(
                            "${if (expanded) "▼" else "▶"} ${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 36.dp, top = 2.dp, bottom = 2.dp).clickable {
                                expandedThreads = if (expanded) expandedThreads - msg.forum_post_id else expandedThreads + msg.forum_post_id
                            }
                        )
                        if (expanded) {
                            // Left thread line drawn behind the replies column. Drawing it
                            // (instead of a fillMaxHeight Box inside IntrinsicSize.Min) avoids
                            // forcing min-intrinsic measurement on the bubbles — which collapsed
                            // reply images (fillMaxWidth + no height) into a tiny box.
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp)
                                    .drawBehind {
                                        drawRect(
                                            color = Color(0xFF3D3848),
                                            topLeft = Offset(0f, 0f),
                                            size = Size(2.dp.toPx(), size.height)
                                        )
                                    }
                                    .padding(start = 12.dp)
                            ) {
                                replies.forEach { reply ->
                                    MessageBubble(reply, true)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reply quote banner
        if (replyingTo != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1625)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(3.dp).height(32.dp).background(ArtistOrange))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Replying to ${replyingTo?.user?.username ?: "message"}", color = ArtistOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(replyingTo?.content?.take(60) ?: "", color = TextMuted, fontSize = 12.sp, maxLines = 1)
                }
                Icon(Icons.Default.Close, "Cancel reply", tint = TextMuted, modifier = Modifier.size(18.dp).clickable { replyingTo = null })
            }
        }

        // Pending image preview
        if (pendingImage != null) {
            Box(modifier = Modifier.background(Color(0xFF0A0612)).padding(start = 12.dp, top = 8.dp)) {
                AsyncImage(model = pendingImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(18.dp).clickable { pendingImage = null })
            }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0612)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Default.Image, "Attach", tint = TextMuted)
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("Message...", color = TextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2D2838), unfocusedContainerColor = Color(0xFF2D2838), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            IconButton(
                enabled = !sending,
                onClick = {
                    val text = input.trim()
                    val img = pendingImage
                    if ((text.isBlank() && img == null) || currentUserId == null) return@IconButton
                    val parentId = replyingTo?.forum_post_id
                    input = ""; pendingImage = null; replyingTo = null; sending = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val mediaUrls = mutableListOf<String>()
                            if (img != null) {
                                S3Uploader.upload(context, img, "chat/${System.nanoTime()}.jpg")?.let { mediaUrls.add(it) }
                            }
                            SupabaseManager.insertMessage(SupabaseManager.ForumPostInsert(
                                forum_id = forumId,
                                user_id = currentUserId,
                                content = text,
                                media_type = if (mediaUrls.isNotEmpty()) "image" else "text",
                                media_urls = mediaUrls,
                                parent_post_id = parentId,
                            ))
                        } catch (e: Exception) { android.util.Log.e("AllChat", "insert failed", e) }
                        sending = false
                    }
                }
            ) {
                if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ArtistOrange, strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = ArtistOrange)
            }
        }
    }
}
