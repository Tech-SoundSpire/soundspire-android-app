package com.example.ui.screens

import com.example.ui.components.TText

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.SongReviewCard
import com.example.data.remote.AlbumMetadata
import com.example.data.remote.ApiClient
import com.example.data.remote.CacheAlbumRequest
import com.example.data.remote.SongReview
import com.example.data.remote.SubmitRatingRequest
import com.example.data.remote.SubmitReviewRequest
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SpotifyGreen = Color(0xFF1DB954)

/**
 * Album review page. Albums reuse the song-review system keyed by "album:{id}", mirroring
 * the website's /reviews/album/[id]. Caches album metadata so the review/feed shows art+name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onTrackClick: (String) -> Unit,
    onArtistClick: (spotifyId: String, name: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }
    val reviewKey = "album:$albumId"

    var album by remember { mutableStateOf<AlbumMetadata?>(null) }
    var reviews by remember { mutableStateOf<List<SongReview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var userRating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(albumId) {
        try {
            val a = withContext(Dispatchers.IO) { api.getAlbum(albumId) }
            album = a
            // Cache for feed/review display (matches the website behavior).
            try {
                withContext(Dispatchers.IO) {
                    api.cacheAlbum(CacheAlbumRequest(
                        spotify_track_id = reviewKey,
                        track_name = a.name ?: "Album",
                        artist_name = a.artists?.mapNotNull { it.name }?.joinToString(", "),
                        artist_id = a.artists?.firstOrNull()?.id,
                        album_art_url = a.images?.firstOrNull()?.url,
                    ))
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        try { reviews = withContext(Dispatchers.IO) { api.getTrackReviews(reviewKey).reviews } } catch (_: Exception) {}
        try {
            val r = withContext(Dispatchers.IO) { api.getTrackRatings(reviewKey) }
            r.user_rating?.let { userRating = it.toInt() }
        } catch (_: Exception) {}
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite) }
                TText("Album", color = HeadingPeach, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (loading) {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp)) } }
            return@LazyColumn
        }

        val a = album
        // Hero
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = a?.images?.firstOrNull()?.url ?: defaultProfileImageUrl(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray))
                Spacer(modifier = Modifier.height(12.dp))
                if (!a?.album_type.isNullOrBlank()) TText(a!!.album_type!!.uppercase(), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TText(a?.name ?: "Album", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val artistList = a?.artists?.filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }.orEmpty()
                if (artistList.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        artistList.forEachIndexed { i, ar ->
                            TText(ar.name!! + if (i < artistList.size - 1) "," else "", color = AccentOrange, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onArtistClick(ar.id!!, ar.name!!) })
                        }
                    }
                }
                val meta = listOfNotNull(a?.release_date?.take(4), a?.total_tracks?.let { "$it tracks" }).joinToString(" • ")
                if (meta.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); TText(meta, color = TextMuted, fontSize = 12.sp) }
                if (!a?.spotify_url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TText("Open on Spotify", color = SpotifyGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a!!.spotify_url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                    })
                }
            }
        }

        // Rate + review the album
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TText("Rate this album", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { i ->
                            Icon(Icons.Default.Star, null, tint = if (i < userRating) AccentOrange else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(32.dp).clickable {
                                userRating = i + 1
                                CoroutineScope(Dispatchers.IO).launch { try { api.submitRating(SubmitRatingRequest(reviewKey, (i + 1).toDouble())) } catch (_: Exception) {} }
                            })
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = reviewText, onValueChange = { reviewText = it },
                        placeholder = { TText("Share your thoughts...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = TextMuted.copy(alpha = 0.3f), focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (reviewText.isBlank()) return@Button
                            submitting = true
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    api.submitReview(SubmitReviewRequest(reviewKey, reviewText, if (userRating > 0) userRating.toDouble() else null))
                                    reviewText = ""
                                    reviews = api.getTrackReviews(reviewKey).reviews
                                } catch (_: Exception) {}
                                submitting = false
                            }
                        },
                        enabled = !submitting && reviewText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, disabledContainerColor = AccentOrange.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                    ) { TText(if (submitting) "Submitting..." else "Submit Review", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Tracks
        val tracks = a?.tracks.orEmpty()
        if (tracks.isNotEmpty()) {
            item { TText("Tracks", color = HeadingPeach, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) }
            items(tracks.size) { idx ->
                val t = tracks[idx]
                Row(modifier = Modifier.fillMaxWidth().clickable { t.id?.let { onTrackClick(it) } }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TText("${t.track_number ?: idx + 1}", color = TextMuted, fontSize = 13.sp, modifier = Modifier.width(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TText(t.name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        t.artists?.mapNotNull { it.name }?.joinToString(", ")?.takeIf { it.isNotBlank() }?.let {
                            TText(it, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Existing album reviews
        if (reviews.isNotEmpty()) {
            item { TText("Reviews (${reviews.size})", color = HeadingPeach, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) }
            items(reviews.size) { idx ->
                SongReviewCard(reviews[idx], api)
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            item { TText("No reviews yet. Be the first!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
        }
    }
}
