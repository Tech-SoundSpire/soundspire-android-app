package com.example.ui.screens.artist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.FanArtComment
import com.example.data.remote.FanArtCreateRequest
import com.example.data.remote.FanArtPost
import com.example.data.remote.SupabaseManager
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.S3Uploader
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ArtistOrange = Color(0xFFFA6400)
private val Bg = Color(0xFF1A0A2E)
private val reactionEmojis = listOf("👍", "❤️", "😂", "🔥", "🎵")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FanArtScreen(forumId: String, currentUserId: String? = null) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var posts by remember { mutableStateOf<List<FanArtPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    // which post has its comment section expanded, and which post's emoji picker is open
    var openCommentsFor by remember { mutableStateOf<String?>(null) }
    var reactPickerFor by remember { mutableStateOf<String?>(null) }
    // Upload modal state
    var showUploadModal by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadTitle by remember { mutableStateOf("") }
    var uploadDescription by remember { mutableStateOf("") }

    // Fetch backend posts (like state) and enrich with reactions + comments from Supabase,
    // mirroring the website (the backend fan-art endpoint omits reactions and comments).
    fun reload() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val backendPosts = api.getFanArt(forumId).posts
                val rows = try { SupabaseManager.fetchMessages(forumId) } catch (_: Exception) { emptyList() }
                val byId = rows.associateBy { it.forum_post_id }
                // comments/replies = rows whose parent chain points at a fan-art post
                val childrenByParent = rows.filter { it.parent_post_id != null }.groupBy { it.parent_post_id }
                // resolve usernames for comment authors (deduped)
                val commentUserIds = rows.filter { it.parent_post_id != null }.mapNotNull { it.user_id }.distinct()
                val userMap = commentUserIds.associateWith { uid -> try { api.getUserById(uid).user } catch (_: Exception) { null } }

                fun toComment(rowId: String): List<FanArtComment> =
                    childrenByParent[rowId].orEmpty().map { r ->
                        FanArtComment(
                            forum_post_id = r.forum_post_id,
                            user_id = r.user_id,
                            parent_post_id = r.parent_post_id,
                            content = r.content,
                            created_at = r.created_at,
                            user = r.user_id?.let { userMap[it] },
                            reactions = r.reactions,
                        )
                    }

                posts = backendPosts.map { p ->
                    p.copy(
                        reactions = byId[p.forum_post_id]?.reactions,
                        comments = toComment(p.forum_post_id),
                    )
                }
            } catch (_: Exception) { }
            loading = false
        }
    }
    LaunchedEffect(forumId) { reload() }

    fun toggleReaction(postId: String, emoji: String) {
        if (currentUserId == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = api.reactToFanArt(forumId, postId, mapOf("userId" to currentUserId, "emoji" to emoji))
                resp.reactions?.let { newR ->
                    posts = posts.map { p ->
                        if (p.forum_post_id == postId) p.copy(reactions = newR)
                        else p.copy(comments = p.comments.map { c -> if (c.forum_post_id == postId) c.copy(reactions = newR) else c })
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun addComment(postId: String, parentPostId: String, content: String) {
        if (currentUserId == null || content.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.insertMessage(SupabaseManager.ForumPostInsert(
                    forum_id = forumId,
                    user_id = currentUserId,
                    content = content,
                    media_type = "text",
                    parent_post_id = parentPostId,
                ))
            } catch (_: Exception) { }
            reload()
        }
    }

    // Single-image picker for the upload modal
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedImages = listOf(uri)
    }

    fun submitUpload() {
        if (selectedImages.isEmpty() || uploading) return
        uploading = true
        val images = selectedImages
        val title = uploadTitle.trim().ifBlank { "Fan Art" }
        val desc = uploadDescription.trim()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val urls = mutableListOf<String>()
                images.forEachIndexed { i, uri ->
                    S3Uploader.upload(context, uri, "fan-art/$forumId/${System.nanoTime()}-$i.jpg")?.let { urls.add(it) }
                }
                if (urls.isNotEmpty()) {
                    api.createFanArt(forumId, FanArtCreateRequest(title = title, content = desc.ifBlank { null }, imageUrls = urls))
                }
                // reset + close
                selectedImages = emptyList(); uploadTitle = ""; uploadDescription = ""; showUploadModal = false
                reload()
            } catch (_: Exception) { }
            uploading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(28.dp)) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                if (posts.isEmpty()) {
                    item { Text("No fan art yet. Tap + to add the first.", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(24.dp)) }
                }
                items(posts.size) { idx ->
                    val post = posts[idx]
                    var liked by remember(post.forum_post_id) { mutableStateOf(post.user_has_liked) }
                    var likeCount by remember(post.forum_post_id) { mutableStateOf(post.likes_count) }
                    val commentsOpen = openCommentsFor == post.forum_post_id

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF221C2F))) {
                        // Author row
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = resolveImageUrl(post.user?.profile_picture_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.DarkGray))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(post.user?.username ?: "user", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!post.media_urls.isNullOrEmpty()) {
                            AsyncImage(model = resolveImageUrl(post.media_urls!!.first()), contentDescription = post.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                        }

                        // Action row: like, comment, react
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                liked = !liked; likeCount += if (liked) 1 else -1
                                CoroutineScope(Dispatchers.IO).launch { try { api.likeFanArt(post.forum_post_id) } catch (_: Exception) { } }
                            }) {
                                Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (liked) Color(0xFFEF4444) else TextMuted, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("$likeCount", color = TextMuted, fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                openCommentsFor = if (commentsOpen) null else post.forum_post_id; reactPickerFor = null
                            }) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = TextMuted, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${post.comments.size}", color = TextMuted, fontSize = 13.sp)
                            }
                            Text("😀 React", color = TextMuted, fontSize = 13.sp, modifier = Modifier.clickable {
                                reactPickerFor = if (reactPickerFor == post.forum_post_id) null else post.forum_post_id
                            })
                        }

                        // Quick emoji picker
                        if (reactPickerFor == post.forum_post_id) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                reactionEmojis.forEach { emoji ->
                                    Text(emoji, fontSize = 20.sp, modifier = Modifier.clickable { reactPickerFor = null; toggleReaction(post.forum_post_id, emoji) })
                                }
                            }
                        }

                        // Reaction chips
                        val reactions = post.reactions
                        if (!reactions.isNullOrEmpty()) {
                            FlowRow(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                reactions.forEach { (emoji, users) ->
                                    if (users.isNotEmpty()) {
                                        val mine = currentUserId != null && users.contains(currentUserId)
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (mine) ArtistOrange else Color(0xFF1A1625)).clickable { toggleReaction(post.forum_post_id, emoji) }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                                            Text(emoji, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("${users.size}", color = if (mine) Color.White else TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (!post.title.isNullOrBlank() && post.title != "Untitled" && post.title != "Fan Art") {
                            Text(post.title!!, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                        }

                        // Comments section
                        if (commentsOpen) {
                            CommentsSection(
                                comments = post.comments,
                                currentUserId = currentUserId,
                                onAddComment = { text -> addComment(post.forum_post_id, post.forum_post_id, text) },
                                onAddReply = { parentCommentId, text -> addComment(post.forum_post_id, parentCommentId, text) },
                                onReactToComment = { commentId, emoji -> toggleReaction(commentId, emoji) },
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showUploadModal = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange),
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add fan art", tint = Color.White); Text("Add", color = Color.White)
        }
    }

    if (showUploadModal) {
        UploadFanArtDialog(
            selectedImages = selectedImages,
            title = uploadTitle,
            description = uploadDescription,
            uploading = uploading,
            onTitleChange = { uploadTitle = it },
            onDescriptionChange = { uploadDescription = it },
            onPickImages = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemoveImage = { uri -> selectedImages = selectedImages.filter { it != uri } },
            onDismiss = { if (!uploading) { showUploadModal = false; selectedImages = emptyList(); uploadTitle = ""; uploadDescription = "" } },
            onUpload = { submitUpload() },
        )
    }
}

@Composable
private fun UploadFanArtDialog(
    selectedImages: List<Uri>,
    title: String,
    description: String,
    uploading: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onDismiss: () -> Unit,
    onUpload: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2D2838)).padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Upload Fan Art", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Close, "Close", tint = TextMuted, modifier = Modifier.size(22.dp).clickable { onDismiss() })
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Select image (single)
            Text("Select Image", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            val selected = selectedImages.firstOrNull()
            if (selected == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1625)).clickable { onPickImages() }.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = ArtistOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose image", color = ArtistOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                // Preview with replace/remove
                Box {
                    AsyncImage(model = selected, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray).clickable { onPickImages() })
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).clickable { onRemoveImage(selected) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap image to replace", color = TextMuted, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Title", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title, onValueChange = onTitleChange,
                placeholder = { Text("Give your artwork a title", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF1A1625), unfocusedContainerColor = Color(0xFF1A1625), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Description (Optional)", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description, onValueChange = onDescriptionChange,
                placeholder = { Text("Tell us about your artwork...", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF1A1625), unfocusedContainerColor = Color(0xFF1A1625), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss, enabled = !uploading, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44404F)), shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel", color = Color.White) }
                Button(
                    onClick = onUpload, enabled = !uploading && selectedImages.isNotEmpty(), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange, disabledContainerColor = ArtistOrange.copy(alpha = 0.4f)), shape = RoundedCornerShape(10.dp)
                ) {
                    if (uploading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Upload", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentsSection(
    comments: List<FanArtComment>,
    currentUserId: String?,
    onAddComment: (String) -> Unit,
    onAddReply: (parentCommentId: String, text: String) -> Unit,
    onReactToComment: (commentId: String, emoji: String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<FanArtComment?>(null) }
    var reactPickerFor by remember { mutableStateOf<String?>(null) }

    // Top-level comments = those whose parent is the post (not another comment in this list)
    val commentIds = comments.map { it.forum_post_id }.toSet()
    val tops = comments.filter { it.parent_post_id !in commentIds }
    val repliesByParent = comments.filter { it.parent_post_id in commentIds }.groupBy { it.parent_post_id }

    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
        // Composer
        if (replyTo != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Text("Replying to ${replyTo?.user?.username ?: "comment"}", color = ArtistOrange, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("✕", color = TextMuted, fontSize = 11.sp, modifier = Modifier.clickable { replyTo = null })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                placeholder = { Text(if (replyTo != null) "Write a reply..." else "Comment...", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF1A1625), unfocusedContainerColor = Color(0xFF1A1625), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            IconButton(onClick = {
                val text = draft.trim()
                if (text.isNotBlank()) {
                    val parent = replyTo
                    if (parent != null) onAddReply(parent.forum_post_id, text) else onAddComment(text)
                    draft = ""; replyTo = null
                }
            }) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = ArtistOrange) }
        }

        Spacer(modifier = Modifier.height(6.dp))
        tops.forEach { comment ->
            FanArtCommentRow(comment, indent = false, currentUserId = currentUserId,
                onReply = { replyTo = comment },
                onReactToggle = { reactPickerFor = if (reactPickerFor == comment.forum_post_id) null else comment.forum_post_id },
                reactPickerOpen = reactPickerFor == comment.forum_post_id,
                onPickEmoji = { emoji -> reactPickerFor = null; onReactToComment(comment.forum_post_id, emoji) })
            repliesByParent[comment.forum_post_id].orEmpty().forEach { reply ->
                FanArtCommentRow(reply, indent = true, currentUserId = currentUserId,
                    onReply = { replyTo = comment },
                    onReactToggle = { reactPickerFor = if (reactPickerFor == reply.forum_post_id) null else reply.forum_post_id },
                    reactPickerOpen = reactPickerFor == reply.forum_post_id,
                    onPickEmoji = { emoji -> reactPickerFor = null; onReactToComment(reply.forum_post_id, emoji) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FanArtCommentRow(
    comment: FanArtComment,
    indent: Boolean,
    currentUserId: String?,
    onReply: () -> Unit,
    onReactToggle: () -> Unit,
    reactPickerOpen: Boolean,
    onPickEmoji: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = if (indent) 28.dp else 0.dp, top = 6.dp)) {
        AsyncImage(model = resolveImageUrl(comment.user?.profile_picture_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(if (indent) 22.dp else 26.dp).clip(CircleShape).background(Color.DarkGray))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.background(Color(0xFF1A1625), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Column {
                    Text(comment.user?.username ?: "User", color = ArtistOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (!comment.content.isNullOrBlank()) Text(comment.content!!, color = Color.White, fontSize = 13.sp)
                }
            }
            // reaction chips
            val reactions = comment.reactions
            if (!reactions.isNullOrEmpty()) {
                FlowRow(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reactions.forEach { (emoji, users) ->
                        if (users.isNotEmpty()) {
                            val mine = currentUserId != null && users.contains(currentUserId)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (mine) ArtistOrange else Color(0xFF2D2838)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(emoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("${users.size}", color = if (mine) Color.White else TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            // actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                if (!indent) Text("Reply", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onReply() })
                Text("React", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onReactToggle() })
            }
            if (reactPickerOpen) {
                Row(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    reactionEmojis.forEach { emoji ->
                        Text(emoji, fontSize = 18.sp, modifier = Modifier.clickable { onPickEmoji(emoji) })
                    }
                }
            }
        }
    }
}
