package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.remote.GenreArtistItem
import com.example.ui.components.TText
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl

// Artists in a genre, ranked by subscriber count. On-platform → community, off-platform → vote page.
@Composable
fun GenreArtistsScreen(
    genreId: String,
    genreName: String,
    onBack: () -> Unit = {},
    onArtistCommunityClick: (String) -> Unit = {},
    onArtistVoteClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var artists by remember { mutableStateOf<List<GenreArtistItem>>(emptyList()) }
    var title by remember { mutableStateOf(genreName) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(genreId) {
        try {
            val resp = api.getGenreArtists(genreId)
            resp.genre?.name?.let { if (it.isNotBlank()) title = it }
            artists = resp.artists
        } catch (_: Exception) { }
        loading = false
    }

    fun openArtist(a: GenreArtistItem) {
        if (a.onSoundSpire && !a.slug.isNullOrBlank()) onArtistCommunityClick(a.slug!!)
        else if (!a.soundcharts_uuid.isNullOrBlank()) onArtistVoteClick(a.soundcharts_uuid!!)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            TText(title, color = HeadingPeach, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
            artists.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TText("No artists in this genre yet.", color = TextMuted, fontSize = 15.sp)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(artists, key = { it.artist_id }) { a ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().clickable { openArtist(a) }
                    ) {
                        AsyncImage(
                            model = resolveImageUrl(a.imageUrl) ?: defaultProfileImageUrl(),
                            contentDescription = a.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray)
                        )
                        Spacer(Modifier.height(6.dp))
                        TText(
                            a.name ?: "Artist", color = TextWhite, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                        if (a.onSoundSpire) {
                            TText("${a.subscriberCount} ${if (a.subscriberCount == 1) "member" else "members"}", color = TextMuted, fontSize = 11.sp)
                        } else {
                            TText("Vote to bring them in", color = AccentOrange, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
