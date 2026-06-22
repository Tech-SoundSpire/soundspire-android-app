package com.example.ui.screens

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Search
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
import com.example.data.remote.SearchResponse
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.resolveImageUrl
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onTrackClick: (String) -> Unit = {},
    onArtistClick: (spotifyId: String, name: String) -> Unit = { _, _ -> },
    onAlbumClick: (albumId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var query by remember { mutableStateOf("") }
    var internalResults by remember { mutableStateOf<SearchResponse?>(null) }
    // Artists + albums come from the Spotify catalog (each carries a Spotify ID for its page);
    // communities/users/songs/reviews still come from internal search (hybrid).
    var catalogArtists by remember { mutableStateOf<List<com.example.data.remote.CatalogArtist>>(emptyList()) }
    var catalogAlbums by remember { mutableStateOf<List<com.example.data.remote.CatalogAlbum>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length < 2) { internalResults = null; catalogArtists = emptyList(); catalogAlbums = emptyList(); return@LaunchedEffect }
        searching = true
        delay(400)
        try {
            internalResults = api.search(query)
        } catch (_: Exception) { internalResults = null }
        try {
            val cat = api.searchCatalog(query, type = "artist,album", limit = 5)
            catalogArtists = cat.artists?.items.orEmpty()
            catalogAlbums = cat.albums?.items.orEmpty()
        } catch (_: Exception) { catalogArtists = emptyList(); catalogAlbums = emptyList() }
        searching = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite) }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { TText("Search songs, artists, communities...", color = TextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground, focusedBorderColor = AccentOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
            )
        }

        if (query.length < 2) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                TText("Type at least 2 characters to search", color = TextMuted, fontSize = 14.sp)
            }
        } else if (searching) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(24.dp))
            }
        } else {
            val resp = internalResults
            val hasSongs = !resp?.songs.isNullOrEmpty()
            val hasArtists = catalogArtists.isNotEmpty()
            val hasAlbums = catalogAlbums.isNotEmpty()
            val hasCommunities = !resp?.communities.isNullOrEmpty()
            val hasReviews = !resp?.reviews.isNullOrEmpty()
            val hasUsers = !resp?.users.isNullOrEmpty()
            val hasAnything = hasSongs || hasArtists || hasAlbums || hasCommunities || hasReviews || hasUsers

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (!hasAnything) {
                    item { TText("No results found for \"$query\"", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
                }

                // Artists (from Spotify catalog → routes to the rich artist page). Rendered
                // independently of internal search so it shows even if /api/search fails.
                if (hasArtists) {
                    item { SectionHeader("Artists") }
                    items(catalogArtists) { artist ->
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            val id = artist.id; val nm = artist.name
                            if (!id.isNullOrBlank() && !nm.isNullOrBlank()) onArtistClick(id, nm)
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = artist.images?.lastOrNull()?.url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.DarkGray))
                            Spacer(modifier = Modifier.width(12.dp))
                            TText(artist.name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            TText("Artist", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // Albums (from Spotify catalog → album review page)
                if (hasAlbums) {
                    item { SectionHeader("Albums") }
                    items(catalogAlbums) { album ->
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            album.id?.let { onAlbumClick(it) }
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = album.images?.firstOrNull()?.url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color.DarkGray))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TText(album.name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                album.artists?.mapNotNull { it.name }?.joinToString(", ")?.takeIf { it.isNotBlank() }?.let {
                                    TText(it, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            TText("Album", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                if (resp != null) {
                    // Songs
                    if (hasSongs) {
                        item { SectionHeader("Songs") }
                        items(resp.songs) { song ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { song.spotify_track_id?.let { onTrackClick(it) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = song.album_art_url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color.DarkGray))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    TText(song.track_name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    TText(song.artist_name ?: "", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TText("Song", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    // Communities
                    if (hasCommunities) {
                        item { SectionHeader("Communities") }
                        items(resp.communities) { community ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = resolveImageUrl(community.profile_picture_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.DarkGray))
                                Spacer(modifier = Modifier.width(12.dp))
                                TText(community.name ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                TText("Community", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    // Reviews
                    if (hasReviews) {
                        item { SectionHeader("Reviews") }
                        items(resp.reviews) { review ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { review.spotify_track_id?.let { onTrackClick(it) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    TText(review.title ?: "", color = TextWhite, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (review.rating != null) TText("★ ${review.rating}/5", color = AccentOrange, fontSize = 12.sp)
                                }
                                TText("Review", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    // Users
                    if (hasUsers) {
                        item { SectionHeader("Users") }
                        items(resp.users) { user ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = resolveImageUrl(user.profile_picture_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    TText(user.full_name ?: user.username ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    TText("@${user.username ?: ""}", color = TextMuted, fontSize = 12.sp)
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
private fun SectionHeader(title: String) {
    TText(title, color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}
