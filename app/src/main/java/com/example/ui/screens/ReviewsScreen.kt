package com.example.ui.screens

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.SongReview
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ReviewsScreen(onSearchClick: () -> Unit = {}, onReviewClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var reviews by remember { mutableStateOf<List<SongReview>>(emptyList()) }
    var lists by remember { mutableStateOf<List<com.example.data.remote.ListItem>>(emptyList()) }
    var diary by remember { mutableStateOf<List<com.example.data.remote.DiaryEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf("activity") }

    LaunchedEffect(Unit) {
        try { reviews = api.getReviewsFeed().reviews } catch (_: Exception) { }
        try { lists = api.getMyLists().lists } catch (_: Exception) { }
        try { diary = api.getDiary().entries } catch (_: Exception) { }
        loading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Sub-tab bar (Activity / Lists / Journal)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDarkPurple.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("activity" to "Activity", "lists" to "Lists", "journal" to "Journal").forEach { (key, label) ->
                    val isActive = activeTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isActive) CardBackground else Color.Transparent)
                            .clickable { activeTab = key }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TText(label, color = if (isActive) TextWhite else TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Header + Search
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TText("REVIEWS", color = HeadingPeach, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { onSearchClick() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            TText("Add a review...", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        when (activeTab) {
            "activity" -> {
                if (loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (reviews.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            TText("No reviews yet.", color = TextMuted, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            TText("Search for a song above to write the first review!", color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }
                } else {
                    items(reviews) { review ->
                        ActivityReviewCard(review, onReviewClick)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
            "lists" -> {
                // Create new list button
                item {
                    var showCreateDialog by remember { mutableStateOf(false) }
                    var newListTitle by remember { mutableStateOf("") }
                    var creating by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TText("+ Create New List", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (showCreateDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showCreateDialog = false; newListTitle = "" },
                            title = { TText("Create New List", color = TextWhite, fontWeight = FontWeight.Bold) },
                            text = {
                                OutlinedTextField(
                                    value = newListTitle,
                                    onValueChange = { newListTitle = it },
                                    placeholder = { TText("List title", color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newListTitle.isBlank()) return@Button
                                        creating = true
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try {
                                                api.createList(com.example.data.remote.CreateListRequest(newListTitle.trim()))
                                                lists = api.getMyLists().lists
                                                newListTitle = ""
                                                showCreateDialog = false
                                            } catch (_: Exception) { }
                                            creating = false
                                        }
                                    },
                                    enabled = newListTitle.isNotBlank() && !creating,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                ) {
                                    TText(if (creating) "Creating..." else "Create", color = Color.White)
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showCreateDialog = false; newListTitle = "" }) {
                                    TText("Cancel", color = TextMuted)
                                }
                            },
                            containerColor = Color(0xFF1F2937),
                        )
                    }
                }

                if (lists.isEmpty()) {
                    item { TText("No lists yet. Create one above!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(32.dp)) }
                } else {
                    items(lists) { list ->
                        var expanded by remember { mutableStateOf(false) }
                        var listItems by remember { mutableStateOf<List<com.example.data.remote.ListDetailItem>>(emptyList()) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable {
                                expanded = !expanded
                                if (expanded && listItems.isEmpty()) {
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        try { listItems = api.getListItems(list.list_id).items } catch (_: Exception) { }
                                    }
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        TText(list.title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        if (!list.description.isNullOrBlank()) TText(list.description!!, color = TextMuted, fontSize = 13.sp, maxLines = if (expanded) Int.MAX_VALUE else 1)
                                    }
                                    TText(if (expanded) "▲" else "▼", color = TextMuted, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                TText("${list.like_count} likes • ${list.created_at?.take(10) ?: ""}", color = TextMuted, fontSize = 12.sp)

                                if (expanded && listItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    listItems.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { item.spotify_track_id?.let { onReviewClick(it) } }.padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(model = item.song?.album_art_url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                TText(item.song?.track_name ?: item.spotify_track_id ?: "", color = TextWhite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                TText(item.song?.artist_name ?: "", color = TextMuted, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "journal" -> {
                if (diary.isEmpty()) {
                    item { TText("No journal entries yet. Log a listen from the web app!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(32.dp)) }
                } else {
                    items(diary) { entry ->
                        var trackName by remember { mutableStateOf<String?>(null) }
                        var trackArtist by remember { mutableStateOf<String?>(null) }
                        var trackArt by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(entry.spotify_track_id) {
                            try {
                                val meta = api.getTrackMetadata(entry.spotify_track_id)
                                trackName = meta.track_name
                                trackArtist = meta.artist_name
                                trackArt = meta.album_art_url
                            } catch (_: Exception) { }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onReviewClick(entry.spotify_track_id) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = trackArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color.DarkGray))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    TText(trackName ?: entry.spotify_track_id, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    TText(trackArtist ?: "", color = AccentOrange.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
                                    if (entry.listened_date != null) TText(entry.listened_date!!.take(10), color = TextMuted, fontSize = 11.sp)
                                    if (!entry.notes.isNullOrBlank()) TText(entry.notes!!, color = TextMuted, fontSize = 11.sp, maxLines = 2)
                                }
                                if (entry.rating != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row { repeat(entry.rating!!.toInt().coerceAtMost(5)) { Icon(Icons.Default.Star, null, tint = AccentOrange, modifier = Modifier.size(12.dp)) } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityReviewCard(review: SongReview, onReviewClick: (String) -> Unit = {}) {
    val albumArt = review.song?.album_art_url ?: defaultProfileImageUrl()
    val profilePic = resolveImageUrl(review.user?.profile_picture_url) ?: defaultProfileImageUrl()
    var expanded by remember { mutableStateOf(false) }
    val displayText = review.review_text

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onReviewClick(review.spotify_track_id) }
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            AsyncImage(
                model = albumArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = profilePic, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.DarkGray))
                    Spacer(modifier = Modifier.width(6.dp))
                    TText(review.user?.username ?: "user", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    TText(" reviewed", color = TextMuted, fontSize = 13.sp)
                }

                if (review.song != null) {
                    TText(review.song!!.track_name ?: "", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (review.rating != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rating = review.rating!!.toInt()
                        repeat(5) { i -> Icon(Icons.Default.Star, null, tint = if (i < rating) AccentOrange else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(14.dp)) }
                        Spacer(modifier = Modifier.width(4.dp))
                        TText("${review.rating}/5", color = TextMuted, fontSize = 11.sp)
                    }
                }

                if (!displayText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TText(
                        displayText,
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        TText("${review.like_count}", color = TextMuted, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        TText("${review.comment_count}", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
