package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Artist
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.GoldStar
import com.example.ui.theme.SoundSpireAccentPurple
import com.example.ui.theme.SoundSpireNeonTeal
import com.example.ui.theme.SoundSpireVibrantBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SoundSpireViewModel

@Composable
fun HomeScreen(
    viewModel: SoundSpireViewModel,
    onNavigateToPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val isArtist = profile?.role == "artist"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        // Cohesive App Header HUD
        HomeHeader(
            userName = profile?.name ?: "Guest",
            role = profile?.role ?: "fan",
            onPreferencesClick = onNavigateToPreferences
        )

        if (isArtist) {
            ArtistDashboardPortal(viewModel)
        } else {
            FanMusicDiscoveryPortal(viewModel)
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    role: String,
    onPreferencesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Welcome, $userName",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = SoundSpireNeonTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (role == "artist") "ARTIST STUDIO PORTAL" else "FAN DISCOVERY HUB",
                color = SoundSpireVibrantBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        // Circle Avatar with Profile Actions
        IconButton(
            onClick = onPreferencesClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SoundSpireNeonTeal.copy(alpha = 0.15f))
                .border(2.dp, SoundSpireNeonTeal, CircleShape)
                .testTag("preferences_nav_button")
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Settings",
                tint = SoundSpireNeonTeal,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FanMusicDiscoveryPortal(viewModel: SoundSpireViewModel) {
    val artists by viewModel.filteredArtists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()

    val genres = listOf("All", "Synthwave", "Dream Pop", "Indie Electronic", "Indie Rock", "Blues", "Garage", "Lo-Fi", "Folk", "Ambient Acoustic")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Search HUD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search electronic, synthwave, rust artists...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoundSpireNeonTeal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = SoundSpireNeonTeal,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }

        // Horizontal Genre Slider
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "DISCOVER BY GENRE",
                    color = SoundSpireVibrantBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(genres) { genre ->
                        val isActive = selectedGenre == genre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isActive) SoundSpireNeonTeal else CardBackground)
                                .clickable { viewModel.selectedGenre.value = genre }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("genre_chip_$genre"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = genre,
                                color = if (isActive) MidnightBlack else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Feature Spotlight Carousel Card
        if (searchQuery.isBlank() && selectedGenre == "All" && artists.isNotEmpty()) {
            val spotlightArtist = artists.first()
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(
                        text = "WEEKLY SPOTLIGHT SESSION",
                        color = SoundSpireAccentPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectArtist(spotlightArtist.id) }
                            .testTag("spotlight_card")
                    ) {
                        Box(modifier = Modifier.height(160.dp)) {
                            AsyncImage(
                                model = spotlightArtist.bannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Elegant shade gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = spotlightArtist.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = SoundSpireNeonTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = spotlightArtist.featuredTrackTitle,
                                    color = SoundSpireNeonTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feed Heading
        item {
            Text(
                text = "COMMUNITY CHANNELS",
                color = SoundSpireVibrantBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
            )
        }

        // Empty Feed State
        if (artists.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No artists matching your sync filters.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List of Artists
        items(artists) { artist ->
            ArtistItemCard(
                artist = artist,
                onClick = { viewModel.selectArtist(artist.id) }
            )
        }
    }
}

@Composable
private fun ArtistItemCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onClick() }
            .testTag("artist_item_${artist.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile circular avatar with Coil Compose
            AsyncImage(
                model = artist.avatarUrl,
                contentDescription = "${artist.name} Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artist.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Artist",
                            tint = SoundSpireNeonTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Featured track title
                Text(
                    text = artist.featuredTrackTitle,
                    color = SoundSpireNeonTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Bio preview
                Text(
                    text = artist.bio,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ratings indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating Star",
                            tint = GoldStar,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (artist.ratingCount > 0) String.format("%.1f", artist.averageRating) else "NEW",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (artist.ratingCount > 0) {
                            Text(
                                text = " (${artist.ratingCount})",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // First genre tag
                    val firstGenre = artist.genres.substringBefore(",")
                    Box(
                        modifier = Modifier
                            .background(SoundSpireAccentPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = firstGenre.trim(),
                            color = SoundSpireAccentPurple,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.DoubleArrow,
                contentDescription = null,
                tint = SoundSpireNeonTeal.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ArtistDashboardPortal(viewModel: SoundSpireViewModel) {
    val artists by viewModel.filteredArtists.collectAsState()
    val rawProfile by viewModel.userProfile.collectAsState()

    var showLaunchForm by remember { mutableStateOf(false) }

    // Form inputs
    var sessionName by remember { mutableStateOf("") }
    var genresInput by remember { mutableStateOf("") }
    var trackTitle by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }

    // Safe fallback name for self-dashboard
    val currentArtistProfileName = rawProfile?.name ?: "Indie Artist"
    val selfArtistRecord = artists.find { it.name.contains(currentArtistProfileName, ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Analytics Highlights Panel
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ARTIST PORTFOLIO DASHBOARD",
                    color = SoundSpireVibrantBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricBox(
                        icon = Icons.Default.VideoLibrary,
                        value = if (selfArtistRecord != null) "1 Session" else "0 Sessions",
                        label = "UPLOADS",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        icon = Icons.Default.Analytics,
                        value = "1.4k",
                        label = "STREAM VIEWS",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        icon = Icons.Default.Star,
                        value = if (selfArtistRecord != null && selfArtistRecord.ratingCount > 0) String.format("%.1f ★", selfArtistRecord.averageRating) else "0.0 ★",
                        label = "AVG RATING",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section Toggle: Upload Live session
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLaunchForm = !showLaunchForm },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SoundSpireNeonTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UPLOAD NEW LIVE SESSION",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showLaunchForm) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showLaunchForm,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(250))
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            OutlinedTextField(
                                value = sessionName,
                                onValueChange = { sessionName = it },
                                label = { Text("Artist/Band Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_artist_name"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = genresInput,
                                onValueChange = { genresInput = it },
                                placeholder = { Text("e.g. Techno, Indie Rock") },
                                label = { Text("Genres (Comma separated)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_genres"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = trackTitle,
                                onValueChange = { trackTitle = it },
                                label = { Text("Featured Live Session Song Title") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_track_title"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                placeholder = { Text("Leave blank for default synth live jam") },
                                label = { Text("HLS Stream (.m3u8) or MP4 Video Link") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_video_url"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = bioInput,
                                onValueChange = { bioInput = it },
                                label = { Text("Short Artist Bio") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("form_bio"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (sessionName.isNotBlank() && trackTitle.isNotBlank()) {
                                        viewModel.registerNewArtist(
                                            name = sessionName,
                                            bio = bioInput,
                                            genres = if (genresInput.isNotBlank()) genresInput else "Indie Rock, Acoustic",
                                            trackTitle = trackTitle,
                                            videoLink = videoUrl,
                                            bannerLink = bannerUrl,
                                            avatarLink = ""
                                        )
                                        // Reset
                                        sessionName = ""
                                        genresInput = ""
                                        trackTitle = ""
                                        videoUrl = ""
                                        bioInput = ""
                                        showLaunchForm = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_new_session_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SoundSpireNeonTeal)
                            ) {
                                Text("LAUNCH ACTIVE LIVE STREAM", color = MidnightBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // List of submissions
        item {
            Text(
                text = "ACTIVE PUBLIC DISCOVERY LINKS",
                color = SoundSpireAccentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )
        }

        // Render self artist profile details if populated, or else simple instruction card
        val selfArtistList = if (selfArtistRecord != null) listOf(selfArtistRecord) else emptyList()

        if (selfArtistList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = SoundSpireVibrantBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Setup Your Artist Channel",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You haven't launched a public session as an artist yet. Fill the quick launcher above to stream details onto the SoundSpire feed!",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            items(selfArtistList) { artist ->
                ArtistItemCard(
                    artist = artist,
                    onClick = { viewModel.selectArtist(artist.id) }
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SoundSpireNeonTeal,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
