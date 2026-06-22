package com.example.ui.screens

import com.example.ui.components.TText

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.ExploreArtist
import com.example.data.remote.GenreItem
import com.example.data.remote.SongReview
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import com.example.util.soundSpireLogoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Genre card gradient colors
private val genreGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),
    listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)),
    listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)),
)

@Composable
fun ExploreScreen(onSearchClick: () -> Unit = {}, onSeeAllReviews: () -> Unit = {}, onSeeMoreArtists: () -> Unit = {}, onReviewClick: (String) -> Unit = {}, onArtistVoteClick: (String) -> Unit = {}, onArtistCommunityClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var artists by remember { mutableStateOf<List<ExploreArtist>>(emptyList()) }
    var allArtists by remember { mutableStateOf<List<ExploreArtist>>(emptyList()) }
    var showAllArtists by remember { mutableStateOf(false) }
    var reviews by remember { mutableStateOf<List<SongReview>>(emptyList()) }
    var genres by remember { mutableStateOf<List<GenreItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // Show the user's suggested artists first (personalization), then ALL onboarded
            // artists — so every artist on the platform appears by default, whether or not the
            // user picked them in preferences. (Both lists come from /api/explore/* which now
            // returns all onboarded artists for the default request.)
            val session = try { api.getSession() } catch (_: Exception) { null }
            val userId = session?.user?.id
            val suggested = if (userId != null) {
                try { api.getSuggestedArtists(userId).artists } catch (_: Exception) { emptyList() }
            } else emptyList()
            val onboarded = try { api.getExploreArtists() } catch (_: Exception) { emptyList() }
            val seen = HashSet<String>()
            artists = (suggested + onboarded).filter { a ->
                val key = a.slug ?: a.soundcharts_uuid ?: a.artist_id
                seen.add(key)
            }
            reviews = api.getReviewsFeed().reviews
            genres = api.getGenres()
        } catch (_: Exception) { }
        loading = false
    }

    // Navigate to community (on-platform) or vote page (off-platform), matching the web.
    fun openArtist(artist: ExploreArtist) {
        val onPlatform = artist.onSoundSpire != false
        if (onPlatform && !artist.slug.isNullOrBlank()) {
            onArtistCommunityClick(artist.slug!!)
        } else if (!artist.soundcharts_uuid.isNullOrBlank()) {
            onArtistVoteClick(artist.soundcharts_uuid!!)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header + Search
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                AsyncImage(
                    model = soundSpireLogoUrl(),
                    contentDescription = "SoundSpire",
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TText("Explore", color = HeadingPeach, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable { onSearchClick() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        Spacer(modifier = Modifier.width(12.dp))
                        TText("Search artists, reviews, communities...", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
                }
            }
        } else {
            // Banner carousel placeholder
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2D1B4E), Color(0xFF4A2C6E), Color(0xFF1A0A2E)))),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        TText("DISCOVER NEW MUSIC", color = HeadingPeach, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        TText("Explore indie artists, read reviews,\nand join communities.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Suggested Artists
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TText("SUGGESTED ARTISTS", color = HeadingPeach, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TText(
                        if (showAllArtists) "Show Less" else "See More",
                        color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            if (!showAllArtists && allArtists.isEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        // Fetch fresh, mirroring the website: preference-selected
                                        // artists (onboarded or not) first, then ALL platform artists
                                        // (q="" returns cached/not-onboarded ones too, so artists any
                                        // user selected become discoverable + votable).
                                        val session = try { api.getSession() } catch (_: Exception) { null }
                                        val uid = session?.user?.id
                                        val suggested = if (uid != null) {
                                            try { api.getSuggestedArtists(uid).artists } catch (_: Exception) { emptyList() }
                                        } else emptyList()
                                        val all = try {
                                            api.getExploreArtists("").map { a ->
                                                // Normalize explore rows: user_id => onboarded, third_party_id => sc uuid
                                                a.copy(
                                                    onSoundSpire = a.onSoundSpire ?: (a.user_id != null),
                                                    soundcharts_uuid = a.soundcharts_uuid ?: a.third_party_id,
                                                )
                                            }
                                        } catch (_: Exception) { emptyList() }
                                        // Merge suggested first, dedupe by slug/uuid/id
                                        val seen = HashSet<String>()
                                        allArtists = (suggested + artists + all).filter { a ->
                                            val key = a.slug ?: a.soundcharts_uuid ?: a.artist_id
                                            seen.add(key)
                                        }
                                    } catch (_: Exception) { allArtists = artists }
                                    showAllArtists = true
                                }
                            } else {
                                showAllArtists = !showAllArtists
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showAllArtists) {
                // Expanded grid in place
                item {
                    val rows = allArtists.chunked(4)
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { artist ->
                                    Box(modifier = Modifier.weight(1f)) { ArtistAvatar(artist) { openArtist(artist) } }
                                }
                                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            } else {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(artists) { artist ->
                            Box(modifier = Modifier.width(80.dp)) { ArtistAvatar(artist) { openArtist(artist) } }
                        }
                    }
                }
            }

            // Reviews Section
            if (reviews.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TText("REVIEWS", color = HeadingPeach, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TText("See All", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onSeeAllReviews() })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(reviews.take(5)) { review ->
                            ReviewCard(review, onReviewClick)
                        }
                    }
                }
            }

            // Genres Section
            if (genres.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    TText("DISCOVER BY GENRE", color = HeadingPeach, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    val rows = genres.chunked(2)
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEachIndexed { rowIdx, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEachIndexed { colIdx, genre ->
                                    val gradientIdx = (rowIdx * 2 + colIdx) % genreGradients.size
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(4f / 3f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(genreGradients[gradientIdx]))
                                            .clickable { },
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                                        TText(
                                            text = genre.name.uppercase(),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistAvatar(artist: ExploreArtist, onClick: () -> Unit) {
    val imgUrl = resolveImageUrl(artist.profile_picture_url ?: artist.imageUrl) ?: defaultProfileImageUrl()
    val isOnPlatform = artist.onSoundSpire != false
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        AsyncImage(
            model = imgUrl,
            contentDescription = artist.artist_name ?: artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        TText(
            text = artist.artist_name ?: artist.name ?: "",
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (!isOnPlatform) {
            TText("Not on platform", color = TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ReviewCard(review: SongReview, onReviewClick: (String) -> Unit = {}) {
    val albumArt = review.song?.album_art_url ?: defaultProfileImageUrl()
    val profilePic = resolveImageUrl(review.user?.profile_picture_url) ?: defaultProfileImageUrl()

    Column(
        modifier = Modifier
            .width(150.dp)
            .height(260.dp) // fixed height so all cards in the row align regardless of review text
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1529))
            .clickable { onReviewClick(review.spotify_track_id) }
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(model = albumArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            if (review.rating != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rating = review.rating!!
                    val full = rating.toInt()
                    val hasHalf = rating - full >= 0.5
                    repeat(full) { Icon(Icons.Default.Star, null, tint = AccentOrange, modifier = Modifier.size(10.dp)) }
                    if (hasHalf) Icon(Icons.AutoMirrored.Filled.StarHalf, null, tint = AccentOrange, modifier = Modifier.size(10.dp))
                }
            }
        }

        // Fill the remaining height so the author/like footer pins to the bottom of every card.
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            TText(review.song?.track_name ?: "Unknown", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!review.song?.artist_name.isNullOrBlank()) {
                TText(review.song!!.artist_name!!, color = AccentOrange.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!review.review_text.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                TText("\"${review.review_text}\"", color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    AsyncImage(model = profilePic, contentDescription = null, modifier = Modifier.size(14.dp).clip(CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    TText("@${review.user?.username ?: "user"}", color = TextMuted.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (review.like_count > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, tint = TextMuted, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        TText("${review.like_count}", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
