package com.example.ui.screens

import com.example.ui.components.TText

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.remote.CatalogArtistAlbum
import com.example.data.remote.CatalogArtistDetail
import com.example.data.remote.CatalogTopTrack
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SpotifyGreen = Color(0xFF1DB954)

/**
 * Artist catalog page (Spotify-keyed), mirroring the website's /reviews/artist/[id]:
 * image, name, genres, Open on Spotify, Go-to-community (onboarded) / Vote (off-platform),
 * Top Tracks, and Discography.
 */
@Composable
fun ArtistCatalogScreen(
    spotifyId: String,
    name: String,
    onBack: () -> Unit,
    onTrackClick: (String) -> Unit,
    onCommunityClick: (slug: String) -> Unit,
    onVoteClick: (soundchartsUuid: String) -> Unit,
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var artist by remember { mutableStateOf<CatalogArtistDetail?>(null) }
    var albums by remember { mutableStateOf<List<CatalogArtistAlbum>>(emptyList()) }
    var communitySlug by remember { mutableStateOf<String?>(null) }
    var voteUuid by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("top") } // "top" | "discography"

    LaunchedEffect(spotifyId) {
        try {
            artist = withContext(Dispatchers.IO) { api.getCatalogArtist(spotifyId, name) }
        } catch (_: Exception) {}
        try {
            albums = withContext(Dispatchers.IO) { api.getCatalogArtistAlbums(spotifyId, name).albums }
        } catch (_: Exception) {}

        // Onboarded == has a real account (user_id set). A cached SoundCharts row also has a
        // slug but no user_id, so we must NOT treat slug alone as onboarded — otherwise an
        // off-platform artist links to an empty community page instead of the vote page.
        var slug: String? = null
        try {
            val matches = withContext(Dispatchers.IO) { api.getExploreArtists(name) }
            slug = matches.firstOrNull { it.user_id != null && it.artist_name?.equals(name, ignoreCase = true) == true }?.slug
            communitySlug = slug
        } catch (_: Exception) {}
        if (slug == null) {
            try {
                voteUuid = withContext(Dispatchers.IO) { api.resolveSoundchartsUuid(spotifyId, name).soundchartsUuid }
            } catch (_: Exception) {}
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)))) {
        // Top bar
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(28.dp))
            }
            return@Column
        }

        val a = artist
        if (a == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TText("Artist not found", color = TextMuted, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            // Hero
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = a.images?.firstOrNull()?.url ?: defaultProfileImageUrl(),
                        contentDescription = a.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(140.dp).clip(CircleShape).background(Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TText("ARTIST", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(a.name ?: name, color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    if (!a.genres.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TText(a.genres!!.joinToString(", "), color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!a.spotify_url.isNullOrBlank()) {
                            TText("Open on Spotify", color = SpotifyGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.spotify_url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                            })
                        }
                        when {
                            communitySlug != null -> TText("Go to their community →", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onCommunityClick(communitySlug!!) })
                            voteUuid != null -> TText("Go to their community →", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onVoteClick(voteUuid!!) })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Tabs
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    listOf("top" to "Top Tracks", "discography" to "Discography").forEach { (key, label) ->
                        Text(
                            label,
                            color = if (tab == key) AccentOrange else TextMuted,
                            fontSize = 15.sp,
                            fontWeight = if (tab == key) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable { tab = key }.padding(vertical = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (tab == "top") {
                val topTracks = a.top_tracks.orEmpty()
                if (topTracks.isEmpty()) {
                    item { TText("No tracks found.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(20.dp)) }
                } else {
                    items(topTracks.size) { idx ->
                        TopTrackRow(topTracks[idx], idx + 1, onClick = { topTracks[idx].id?.let { onTrackClick(it) } })
                    }
                }
            } else {
                if (albums.isEmpty()) {
                    item { TText("No releases found.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(20.dp)) }
                } else {
                    items(albums.size) { idx ->
                        AlbumRow(albums[idx])
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTrackRow(track: CatalogTopTrack, index: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TText("$index", color = TextMuted, fontSize = 13.sp, modifier = Modifier.width(24.dp))
        AsyncImage(model = track.album_art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color.DarkGray))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!track.album_name.isNullOrBlank()) Text(track.album_name!!, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        track.duration_ms?.let {
            Spacer(modifier = Modifier.width(8.dp))
            TText(formatDuration(it), color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AlbumRow(album: CatalogArtistAlbum) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(model = album.images?.firstOrNull()?.url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(album.name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(
                album.release_date?.take(4),
                album.total_tracks?.let { "$it ${if (it == 1) "track" else "tracks"}" }
            ).joinToString(" • ")
            if (meta.isNotBlank()) Text(meta, color = TextMuted, fontSize = 12.sp)
        }
    }
}

private fun formatDuration(ms: Int): String {
    val min = ms / 60000
    val sec = (ms % 60000) / 1000
    return "%d:%02d".format(min, sec)
}
