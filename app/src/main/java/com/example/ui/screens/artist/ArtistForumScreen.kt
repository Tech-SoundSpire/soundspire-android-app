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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.util.S3Uploader
import com.example.util.resolveImageUrl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiClient
import com.example.data.remote.CommunityPost
import com.example.data.remote.CommunityPostCreateRequest
import com.example.ui.components.CommunityPostCard
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ArtistOrange = Color(0xFFFA6400)
private val Bg = Color(0xFF1A0A2E)

@Composable
fun ArtistForumScreen(communityId: String, artistId: String, isArtist: Boolean, currentUserId: String?) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    var pendingImage by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingImage = uri
    }

    fun reload() {
        CoroutineScope(Dispatchers.Main).launch {
            try { posts = api.getCommunityPosts(communityId) } catch (_: Exception) { }
            loading = false
        }
    }
    LaunchedEffect(communityId) { reload() }

    fun toggleLike(post: CommunityPost) {
        if (currentUserId == null) return
        val liked = post.likes.any { it.user_id == currentUserId }
        // optimistic update
        posts = posts.map {
            if (it.post_id == post.post_id) {
                val newLikes = if (liked) it.likes.filter { l -> l.user_id != currentUserId }
                               else it.likes + com.example.data.remote.PostLike(user_id = currentUserId)
                it.copy(likes = newLikes)
            } else it
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf("user_id" to currentUserId, "post_id" to post.post_id)
                if (liked) api.unlikePost(body) else api.likePost(body)
            } catch (_: Exception) { reload() }
        }
    }

    fun submitComment(postId: String, content: String, parentId: String?) {
        if (currentUserId == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = buildMap {
                    put("user_id", currentUserId)
                    put("content", content)
                    put("post_id", postId)
                    if (parentId != null) put("parent_comment_id", parentId)
                }
                api.commentOnPost(body)
            } catch (_: Exception) { }
            reload()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(28.dp)) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                if (posts.isEmpty()) {
                    item { Text("No posts yet.", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(24.dp)) }
                }
                items(posts.size) { idx ->
                    CommunityPostCard(
                        post = posts[idx],
                        currentUserId = currentUserId,
                        onToggleLike = { toggleLike(it) },
                        onSubmitComment = { pid, content, parent -> submitComment(pid, content, parent) },
                    )
                }
            }
        }

        // Only the artist can post announcements to their forum
        if (isArtist) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0612)).padding(8.dp)) {
                if (pendingImage != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        AsyncImage(model = pendingImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)))
                        Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(18.dp).clickable { pendingImage = null })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.Image, "Add media", tint = TextMuted)
                    }
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text("Share something with your community...", color = TextMuted) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2D2838), unfocusedContainerColor = Color(0xFF2D2838), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    IconButton(
                        enabled = !posting,
                        onClick = {
                            val text = input.trim()
                            val img = pendingImage
                            if ((text.isBlank() && img == null) || posting) return@IconButton
                            posting = true; input = ""; pendingImage = null
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val mediaUrls = mutableListOf<String>()
                                    if (img != null) S3Uploader.upload(context, img, "posts/$communityId/${System.nanoTime()}.jpg")?.let { mediaUrls.add(it) }
                                    api.createCommunityPost(CommunityPostCreateRequest(artist_id = artistId, community_id = communityId, content_text = text, media_urls = mediaUrls))
                                    reload()
                                } catch (_: Exception) { }
                                posting = false
                            }
                        }
                    ) {
                        if (posting) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ArtistOrange, strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, "Post", tint = ArtistOrange)
                    }
                }
            }
        }
    }
}
