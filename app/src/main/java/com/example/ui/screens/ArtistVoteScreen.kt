package com.example.ui.screens

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.ArtistVoteRequest
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ArtistVoteData(
    val name: String? = null,
    val imageUrl: String? = null,
    val biography: String? = null,
    val genres: List<String> = emptyList(),
)

@Composable
fun ArtistVoteScreen(uuid: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var artist by remember { mutableStateOf<ArtistVoteData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var voteCount by remember { mutableStateOf(0) }
    var userVoted by remember { mutableStateOf(false) }
    var voting by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uuid) {
        try {
            val session = api.getSession()
            userId = session.user?.id

            // Fetch artist info
            // The /api/artists/{uuid} endpoint returns artist details from SoundCharts
            // For simplicity, we'll use the data we already have passed through navigation
            // But let's try the endpoint
            try {
                val response = api.getArtistByUuid(uuid)
                artist = ArtistVoteData(
                    name = response["name"] as? String,
                    imageUrl = response["imageUrl"] as? String,
                    biography = response["biography"] as? String,
                )
            } catch (_: Exception) { }

            // Fetch vote count
            try {
                val voteResponse = api.getArtistVote(uuid, userId)
                voteCount = voteResponse.count
                userVoted = voteResponse.userVoted
            } catch (_: Exception) { }
        } catch (_: Exception) { }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)))
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
            }
        } else {
            // Banner
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val imgUrl = resolveImageUrl(artist?.imageUrl)
                if (imgUrl != null) {
                    AsyncImage(model = imgUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, BackgroundDarkPurple))))

                // Back button
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                }

                // Profile photo centered at bottom
                AsyncImage(
                    model = resolveImageUrl(artist?.imageUrl),
                    contentDescription = artist?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                TText(
                    text = artist?.name ?: "Artist",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                if (artist?.genres?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TText(artist!!.genres.joinToString(" · "), color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                if (!artist?.biography.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TText(artist!!.biography!!, color = TextMuted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }
            }

            // Fixed bottom vote section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDarkPurple)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TText(
                    "This artist is not yet on SoundSpire. Community features are not available.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (userVoted || voting || userId == null) return@Button
                        voting = true
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val result = api.castArtistVote(ArtistVoteRequest(
                                    soundcharts_uuid = uuid,
                                    artist_name = artist?.name ?: "",
                                    image_url = artist?.imageUrl,
                                    userId = userId!!,
                                ))
                                voteCount = result.count
                                userVoted = true
                            } catch (_: Exception) { }
                            voting = false
                        }
                    },
                    enabled = !userVoted && !voting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userVoted) SuccessGreen.copy(alpha = 0.2f) else AccentOrange,
                        disabledContainerColor = if (userVoted) SuccessGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.5f),
                    ),
                ) {
                    TText(
                        text = when {
                            voting -> "Voting..."
                            userVoted -> "✓ Voted · $voteCount ${if (voteCount == 1) "vote" else "votes"}"
                            else -> "⭐ Vote for ${artist?.name ?: "this artist"} to be on SoundSpire"
                        },
                        color = if (userVoted) SuccessGreen else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
                if (!userVoted && voteCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TText("$voteCount ${if (voteCount == 1) "vote" else "votes"} so far", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
