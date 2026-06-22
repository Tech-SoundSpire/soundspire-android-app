package com.example.ui.screens

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.components.SongReviewCard
import com.example.data.remote.ListItem
import com.example.data.remote.SongReview
import com.example.data.remote.SubmitReviewRequest
import com.example.data.remote.SubmitRatingRequest
import com.example.data.remote.TrackMetadata
import com.example.data.remote.TrackRatingResponse
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
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewDetailScreen(
    trackId: String,
    onBack: () -> Unit,
    onArtistClick: (spotifyId: String, name: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var reviews by remember { mutableStateOf<List<SongReview>>(emptyList()) }
    var trackMeta by remember { mutableStateOf<TrackMetadata?>(null) }
    var ratings by remember { mutableStateOf<TrackRatingResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var userRating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var infoTab by remember { mutableStateOf("credits") } // "credits" | "details"

    LaunchedEffect(trackId) {
        try { trackMeta = api.getTrackMetadata(trackId) } catch (_: Exception) { }
        try {
            val reviewsResponse = api.getTrackReviews(trackId)
            reviews = reviewsResponse.reviews
        } catch (_: Exception) { }
        try {
            ratings = api.getTrackRatings(trackId)
            ratings?.user_rating?.let { userRating = it.toInt() }
        } catch (_: Exception) { }
        loading = false
    }

    val albumArt = trackMeta?.album_art_url ?: reviews.firstOrNull()?.song?.album_art_url ?: defaultProfileImageUrl()
    val trackName = trackMeta?.track_name ?: reviews.firstOrNull()?.song?.track_name ?: "Song"
    val artistName = trackMeta?.artist_name ?: reviews.firstOrNull()?.song?.artist_name ?: "Artist"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite) }
                TText("Song Reviews", color = HeadingPeach, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (loading) {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp)) } }
        } else {
            // Hero
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray))
                    Spacer(modifier = Modifier.height(16.dp))
                    TText(trackName, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Clickable artists → artist catalog page (mirrors the website).
                    val artistList = trackMeta?.artists?.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }.orEmpty()
                    if (artistList.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            artistList.forEachIndexed { i, a ->
                                TText(
                                    text = a.name!! + if (i < artistList.size - 1) "," else "",
                                    color = AccentOrange,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onArtistClick(a.id!!, a.name!!) }
                                )
                            }
                        }
                    } else {
                        TText(artistName, color = AccentOrange, fontSize = 16.sp)
                    }
                    if (ratings != null && ratings!!.rating_count > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TText("Average: ${"%.1f".format(ratings!!.avg_rating ?: 0.0)}/5 (${ratings!!.rating_count} ratings)", color = TextMuted, fontSize = 13.sp)
                    }

                    // Action buttons: Add to List
                    Spacer(modifier = Modifier.height(12.dp))
                    var showListPicker by remember { mutableStateOf(false) }
                    var userLists by remember { mutableStateOf<List<ListItem>>(emptyList()) }
                    var addingToList by remember { mutableStateOf(false) }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                showListPicker = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    try { userLists = api.getMyLists().lists } catch (_: Exception) { }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            TText("+ Add to List", color = TextWhite, fontSize = 12.sp)
                        }
                    }

                    if (showListPicker) {
                        var newListTitle by remember { mutableStateOf("") }
                        var creatingList by remember { mutableStateOf(false) }

                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showListPicker = false },
                            title = { TText("Add to List", color = TextWhite, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    // Create new list inline
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = newListTitle,
                                            onValueChange = { newListTitle = it },
                                            placeholder = { TText("New list name...", color = TextMuted, fontSize = 13.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (newListTitle.isBlank()) return@Button
                                                creatingList = true
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    try {
                                                        api.createList(com.example.data.remote.CreateListRequest(newListTitle.trim()))
                                                        userLists = api.getMyLists().lists
                                                        newListTitle = ""
                                                    } catch (_: Exception) { }
                                                    creatingList = false
                                                }
                                            },
                                            enabled = newListTitle.isNotBlank() && !creatingList,
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                            shape = RoundedCornerShape(6.dp),
                                        ) {
                                            TText(if (creatingList) "..." else "Create", color = Color.White, fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (userLists.isEmpty()) {
                                        TText("No lists yet — create one above!", color = TextMuted, fontSize = 13.sp)
                                    } else {
                                        TText("Your Lists:", color = TextMuted, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        userLists.forEach { list ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    addingToList = true
                                                    CoroutineScope(Dispatchers.Main).launch {
                                                        try {
                                                            api.addToList(list.list_id, mapOf("spotify_track_id" to trackId))
                                                        } catch (_: Exception) { }
                                                        addingToList = false
                                                        showListPicker = false
                                                    }
                                                }.padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TText(list.title, color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                TText("+ Add", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { showListPicker = false }) {
                                    TText("Close", color = AccentOrange)
                                }
                            },
                            containerColor = Color(0xFF1F2937),
                        )
                    }
                }
            }

            // Rate this song
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TText("Rate this song", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Default.Star, null,
                                    tint = if (i < userRating) AccentOrange else TextMuted.copy(alpha = 0.3f),
                                    modifier = Modifier.size(32.dp).clickable {
                                        userRating = i + 1
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try { api.submitRating(SubmitRatingRequest(trackId, (i + 1).toDouble())) } catch (_: Exception) { }
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        TText("Write a review", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = reviewText, onValueChange = { reviewText = it },
                            placeholder = { TText("Share your thoughts...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (reviewText.isBlank()) return@Button
                                submitting = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        api.submitReview(SubmitReviewRequest(trackId, reviewText, if (userRating > 0) userRating.toDouble() else null))
                                        reviewText = ""
                                        val refreshed = api.getTrackReviews(trackId)
                                        reviews = refreshed.reviews
                                    } catch (_: Exception) { }
                                    submitting = false
                                }
                            },
                            enabled = !submitting && reviewText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, disabledContainerColor = AccentOrange.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            TText(if (submitting) "Submitting..." else "Submit Review", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Credits & Details
            item {
                val meta = trackMeta
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            listOf("credits" to "Credits", "details" to "Details").forEach { (key, label) ->
                                TText(
                                    label,
                                    color = if (infoTab == key) AccentOrange else TextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = if (infoTab == key) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.clickable { infoTab = key }.padding(vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (infoTab == "credits") {
                            val credits = meta?.credits.orEmpty()
                            if (credits.isEmpty()) {
                                TText("Credits not available for this track.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                credits.forEach { c ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TText(c.name ?: "", color = TextWhite.copy(alpha = 0.85f), fontSize = 14.sp)
                                        TText(c.role ?: "", color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            @Composable fun DetailRow(label: String, value: String) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TText(label, color = TextMuted, fontSize = 14.sp)
                                    TText(value, color = TextWhite.copy(alpha = 0.85f), fontSize = 14.sp)
                                }
                            }
                            if (!meta?.isrc.isNullOrBlank()) DetailRow("ISRC", meta!!.isrc!!)
                            if (!meta?.release_date.isNullOrBlank()) DetailRow("Release Date", meta!!.release_date!!)
                            meta?.duration_ms?.let { DetailRow("Duration", formatTrackDuration(it)) }
                            if (!meta?.album_name.isNullOrBlank()) DetailRow("Album", meta!!.album_name!!)
                            DetailRow("Explicit", if (meta?.explicit == true) "Yes" else "No")
                            if (!meta?.spotify_url.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TText("Open on Spotify", color = Color(0xFF1DB954), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable {
                                    try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(meta!!.spotify_url)).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                                })
                            }
                        }
                    }
                }
            }

            // Existing reviews
            if (reviews.isNotEmpty()) {
                item { TText("All Reviews (${reviews.size})", color = HeadingPeach, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) }
                items(reviews) { review ->
                    SongReviewCard(review, api)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                item { TText("No reviews yet. Be the first!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
            }
        }
    }
}

private fun formatTrackDuration(ms: Int): String {
    val min = ms / 60000
    val sec = (ms % 60000) / 1000
    return "%d:%02d".format(min, sec)
}

