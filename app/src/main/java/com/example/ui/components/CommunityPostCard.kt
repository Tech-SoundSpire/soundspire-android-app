package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.data.remote.CommunityPost
import com.example.data.remote.PostComment
import com.example.ui.theme.TextMuted
import com.example.util.resolveImageUrl

private val ArtistOrange = Color(0xFFFA6400)

/**
 * Interactive community post card with like, comment, and one-level threaded replies.
 * Shared by the Feed and the community Forum so behavior matches the website on both.
 *
 * The parent owns mutation + reload; this card only renders and reports user intent.
 */
@Composable
fun CommunityPostCard(
    post: CommunityPost,
    currentUserId: String?,
    cardColor: Color = Color(0xFF221C2F),
    onToggleLike: (CommunityPost) -> Unit,
    onSubmitComment: (postId: String, content: String, parentCommentId: String?) -> Unit,
) {
    val liked = currentUserId != null && post.likes.any { it.user_id == currentUserId }
    var commentsOpen by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<PostComment?>(null) }

    val topComments = post.comments.filter { it.parent_comment_id == null }
    val repliesByParent = post.comments.filter { it.parent_comment_id != null }.groupBy { it.parent_comment_id }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(cardColor, RoundedCornerShape(12.dp)).padding(14.dp)) {
        // Artist header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = resolveImageUrl(post.artist?.profile_picture_url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(post.artist?.artist_name ?: "Artist", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (!post.content_text.isNullOrBlank()) {
            Text(post.content_text!!, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (!post.media_urls.isNullOrEmpty()) {
            AsyncImage(
                model = resolveImageUrl(post.media_urls!!.first()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Like / comment action row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onToggleLike(post) }) {
                Icon(
                    if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) Color(0xFFEF4444) else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("${post.likes.size}", color = TextMuted, fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                commentsOpen = !commentsOpen; replyTo = null; commentDraft = ""
            }) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = TextMuted, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${post.comments.size}", color = TextMuted, fontSize = 13.sp)
            }
        }

        // Comments section
        if (commentsOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            if (topComments.isEmpty()) {
                Text("No comments yet. Be the first!", color = TextMuted, fontSize = 12.sp)
            }
            topComments.forEach { comment ->
                CommentRow(comment, indent = false, onReply = { replyTo = comment })
                repliesByParent[comment.comment_id].orEmpty().forEach { reply ->
                    CommentRow(reply, indent = true, onReply = { replyTo = comment })
                }
            }

            // Comment composer
            Spacer(modifier = Modifier.height(8.dp))
            if (replyTo != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Text("Replying to ${replyTo?.user?.username ?: "comment"}", color = ArtistOrange, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✕", color = TextMuted, fontSize = 11.sp, modifier = Modifier.clickable { replyTo = null })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentDraft, onValueChange = { commentDraft = it },
                    placeholder = { Text(if (replyTo != null) "Write a reply..." else "Write a comment...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2D2838), unfocusedContainerColor = Color(0xFF2D2838), focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                IconButton(onClick = {
                    val text = commentDraft.trim()
                    if (text.isNotBlank()) {
                        onSubmitComment(post.post_id, text, replyTo?.comment_id)
                        commentDraft = ""; replyTo = null
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = ArtistOrange)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: PostComment, indent: Boolean, onReply: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = if (indent) 32.dp else 0.dp, top = 6.dp)) {
        AsyncImage(
            model = resolveImageUrl(comment.user?.profile_picture_url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(if (indent) 24.dp else 28.dp).clip(CircleShape).background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.background(Color(0xFF2D2838), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Column {
                    Text(comment.user?.username ?: "User", color = ArtistOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (!comment.content.isNullOrBlank()) Text(comment.content!!, color = Color.White, fontSize = 13.sp)
                }
            }
            // Only top-level comments can be replied to (single nesting level, matches web)
            if (!indent) {
                Text("Reply", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, top = 2.dp).clickable { onReply() })
            }
        }
    }
}
