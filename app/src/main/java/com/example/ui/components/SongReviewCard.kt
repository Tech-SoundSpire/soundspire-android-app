package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ReviewComment
import com.example.data.remote.ReviewCommentRequest
import com.example.data.remote.SongReview
import com.example.data.remote.SoundSpireService
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.CardBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive song/album review card: rating, like toggle, and comments. Shared by the song
 * review screen and album review screen. The track endpoint doesn't return per-review
 * user_liked, so likes start unfilled and toggle optimistically (matches the website).
 */
@Composable
fun SongReviewCard(review: SongReview, api: SoundSpireService) {
    val profilePic = resolveImageUrl(review.user?.profile_picture_url) ?: defaultProfileImageUrl()
    var liked by remember(review.review_id) { mutableStateOf(false) }
    var likeCount by remember(review.review_id) { mutableStateOf(review.like_count) }
    var showComments by remember(review.review_id) { mutableStateOf(false) }
    var comments by remember(review.review_id) { mutableStateOf<List<ReviewComment>>(emptyList()) }
    var commentText by remember(review.review_id) { mutableStateOf("") }
    var commentCount by remember(review.review_id) { mutableStateOf(review.comment_count) }

    fun loadComments() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val c = api.getReviewComments(review.review_id).comments
                withContext(Dispatchers.Main) { comments = c }
            } catch (_: Exception) {}
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = profilePic, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.DarkGray))
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(review.user?.username ?: "user", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                if (review.rating != null) {
                    Row { repeat(5) { i -> Icon(Icons.Default.Star, null, tint = if (i < review.rating!!.toInt()) AccentOrange else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(14.dp)) } }
                }
            }
            if (!review.review_text.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.material3.Text(review.review_text!!, color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    val wasLiked = liked
                    liked = !wasLiked
                    likeCount += if (wasLiked) -1 else 1
                    CoroutineScope(Dispatchers.IO).launch {
                        try { if (wasLiked) api.unlikeReview(review.review_id) else api.likeReview(review.review_id) } catch (_: Exception) {}
                    }
                }) {
                    Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (liked) Color(0xFFEF4444) else TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    androidx.compose.material3.Text("$likeCount", color = TextMuted, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    showComments = !showComments
                    if (showComments) loadComments()
                }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    androidx.compose.material3.Text("$commentCount", color = TextMuted, fontSize = 12.sp)
                }
            }

            if (showComments) {
                Spacer(modifier = Modifier.height(10.dp))
                comments.forEach { c ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        AsyncImage(model = resolveImageUrl(c.profile_picture_url) ?: defaultProfileImageUrl(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            androidx.compose.material3.Text(c.username ?: "user", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (!c.comment_text.isNullOrBlank()) androidx.compose.material3.Text(c.comment_text!!, color = TextWhite.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText, onValueChange = { commentText = it },
                        placeholder = { androidx.compose.material3.Text("Write a comment...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = TextMuted.copy(alpha = 0.3f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    IconButton(onClick = {
                        val text = commentText.trim()
                        if (text.isBlank()) return@IconButton
                        commentText = ""
                        commentCount += 1
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                api.commentOnReview(review.review_id, ReviewCommentRequest(text))
                                val c = api.getReviewComments(review.review_id).comments
                                withContext(Dispatchers.Main) { comments = c; commentCount = c.size }
                            } catch (_: Exception) {}
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = AccentOrange) }
                }
            }
        }
    }
}
