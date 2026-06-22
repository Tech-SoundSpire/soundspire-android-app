package com.example.ui.screens

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiClient
import com.example.data.remote.CommunityPost
import com.example.data.remote.PostLike
import com.example.ui.components.CommunityPostCard
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FeedScreen() {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var currentUserId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val session = api.getSession()
                val userId = session.user?.id ?: return@launch
                currentUserId = userId
                posts = api.getPosts(userId)
            } catch (_: Exception) { }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    fun toggleLike(post: CommunityPost) {
        val uid = currentUserId ?: return
        val liked = post.likes.any { it.user_id == uid }
        posts = posts.map {
            if (it.post_id == post.post_id) {
                val newLikes = if (liked) it.likes.filter { l -> l.user_id != uid }
                               else it.likes + PostLike(user_id = uid)
                it.copy(likes = newLikes)
            } else it
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf("user_id" to uid, "post_id" to post.post_id)
                if (liked) api.unlikePost(body) else api.likePost(body)
            } catch (_: Exception) { reload() }
        }
    }

    fun submitComment(postId: String, content: String, parentId: String?) {
        val uid = currentUserId ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = buildMap {
                    put("user_id", uid)
                    put("content", content)
                    put("post_id", postId)
                    if (parentId != null) put("parent_comment_id", parentId)
                }
                api.commentOnPost(body)
            } catch (_: Exception) { }
            reload()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))
            ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            TText(
                text = "POSTS",
                color = HeadingPeach,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
                }
            }
        } else if (posts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TText("No posts yet.", color = TextMuted, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TText("Subscribe to communities to see posts here.", color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        } else {
            items(posts) { post ->
                CommunityPostCard(
                    post = post,
                    currentUserId = currentUserId,
                    cardColor = CardBackground,
                    onToggleLike = { toggleLike(it) },
                    onSubmitComment = { pid, content, parent -> submitComment(pid, content, parent) },
                )
            }
        }
    }
}
