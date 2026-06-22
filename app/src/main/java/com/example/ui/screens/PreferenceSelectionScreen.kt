package com.example.ui.screens

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val genreEmojis = mapOf(
    "Alternative" to "🎸", "Blues" to "🎷", "Country" to "🤠", "Electronic" to "🎛️",
    "Hip Hop Rap" to "🎤", "Jazz" to "🎺", "Latino" to "💃", "Metal" to "🤘",
    "Pop" to "🎵", "Punk" to "⚡", "R B Soul" to "🎙️", "Reggae" to "🌴", "Rock" to "🎸",
    "Classical" to "🎻", "Folk" to "🪕", "Indie" to "🎹", "R&B" to "🎙️", "Soul" to "🎙️",
)

private val genreColors = mapOf(
    "Alternative" to Color(0xFF9333EA), "Blues" to Color(0xFF3B82F6), "Country" to Color(0xFFF59E0B),
    "Electronic" to Color(0xFF06B6D4), "Hip Hop Rap" to Color(0xFFEF4444), "Jazz" to Color(0xFFEAB308),
    "Latino" to Color(0xFFF97316), "Metal" to Color(0xFF6B7280), "Pop" to Color(0xFFEC4899),
    "Punk" to Color(0xFF22C55E), "R B Soul" to Color(0xFF8B5CF6), "Reggae" to Color(0xFF10B981),
    "Rock" to Color(0xFFF43F5E), "Classical" to Color(0xFF7C3AED), "Folk" to Color(0xFF84CC16),
    "Indie" to Color(0xFFA78BFA), "R&B" to Color(0xFF8B5CF6), "Soul" to Color(0xFFD946EF),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferenceSelectionScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var step by remember { mutableStateOf(1) } // 1=languages, 2=genres, 3=artists
    var languages by remember { mutableStateOf<List<com.example.data.remote.LanguageItem>>(emptyList()) }
    var genres by remember { mutableStateOf<List<GenreItem>>(emptyList()) }
    var artists by remember { mutableStateOf<List<ExploreArtist>>(emptyList()) }
    var selectedLanguages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedArtists by remember { mutableStateOf<Map<String, ExploreArtist>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var languageSearch by remember { mutableStateOf("") }
    var genreSearch by remember { mutableStateOf("") }
    var artistSearch by remember { mutableStateOf("") }
    var searchedArtists by remember { mutableStateOf<List<ExploreArtist>>(emptyList()) }
    var artistSearchLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { languages = api.getAvailableLanguages().languages } catch (_: Exception) { }
        try {
            genres = api.getAvailableGenres().genres
        } catch (_: Exception) {
            try { genres = api.getGenres() } catch (_: Exception) { }
        }
        try {
            val available = api.getAvailableArtists().artists
            artists = if (available.isNotEmpty()) available else api.getExploreArtists()
        } catch (_: Exception) {
            try { artists = api.getExploreArtists() } catch (_: Exception) { }
        }
        loading = false
    }

    // Debounced artist search via SoundCharts
    LaunchedEffect(artistSearch) {
        if (artistSearch.length < 2) { searchedArtists = emptyList(); return@LaunchedEffect }
        artistSearchLoading = true
        delay(600)
        try {
            val result = api.searchArtistsSoundcharts(artistSearch)
            searchedArtists = result.items.map { item ->
                ExploreArtist(
                    artist_id = item.uuid ?: "",
                    artist_name = item.name,
                    profile_picture_url = item.imageUrl,
                )
            }
        } catch (_: Exception) { searchedArtists = emptyList() }
        artistSearchLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0518))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TText("Set Your Preferences", color = HeadingPeach, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        TText("Step $step of 3", color = TextMuted, fontSize = 14.sp)

        // Progress dots
        Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (i + 1 == step) TextWhite else TextMuted.copy(alpha = 0.4f)))
            }
        }

        if (loading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
            }
        } else {
            // Content area
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (step) {
                    1 -> {
                        TText("Choose Your Languages", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TText("Choose ${selectedLanguages.size}/5 languages", color = AccentOrange, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = languageSearch,
                            onValueChange = { languageSearch = it },
                            placeholder = { TText("Search languages...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground, focusedBorderColor = AccentOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val filteredLangs = if (languageSearch.isBlank()) languages else languages.filter { it.name.contains(languageSearch, ignoreCase = true) }
                        val langRows = filteredLangs.chunked(2)
                        langRows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                row.forEach { lang ->
                                    val isSelected = lang.language_id in selectedLanguages
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(2.2f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(2.dp, if (isSelected) AccentOrange else Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                            .background(if (isSelected) AccentOrange.copy(alpha = 0.2f) else Color(0xFF8B5CF6).copy(alpha = 0.1f))
                                            .clickable {
                                                selectedLanguages = if (isSelected) selectedLanguages - lang.language_id
                                                else if (selectedLanguages.size < 5) selectedLanguages + lang.language_id
                                                else selectedLanguages
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TText(lang.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = AccentOrange, modifier = Modifier.align(Alignment.TopEnd).size(20.dp))
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    2 -> {
                        TText("Choose Your Favorite Genres", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TText("Choose ${selectedGenres.size}/5 (minimum 3 required)", color = AccentOrange, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Search
                        OutlinedTextField(
                            value = genreSearch,
                            onValueChange = { genreSearch = it },
                            placeholder = { TText("Search genres...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground, focusedBorderColor = AccentOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val filtered = if (genreSearch.isBlank()) genres else genres.filter { it.name.contains(genreSearch, ignoreCase = true) }

                        // Genre cards in 2-col grid (inline since we're in a scrollable column)
                        val rows = filtered.chunked(2)
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                row.forEach { genre ->
                                    val isSelected = genre.genre_id in selectedGenres
                                    val nameKey = genreColors.keys.firstOrNull { it.equals(genre.name, ignoreCase = true) }
                                    val color = nameKey?.let { genreColors[it] } ?: Color(0xFF6B7280)
                                    val emoji = nameKey?.let { genreEmojis[it] } ?: "🎶"

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.3f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(2.dp, if (isSelected) AccentOrange else color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                            .background(if (isSelected) AccentOrange.copy(alpha = 0.2f) else color.copy(alpha = 0.1f))
                                            .clickable {
                                                selectedGenres = if (isSelected) selectedGenres - genre.genre_id
                                                else if (selectedGenres.size < 5) selectedGenres + genre.genre_id
                                                else selectedGenres
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            TText(emoji, fontSize = 28.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TText(genre.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, null, tint = AccentOrange, modifier = Modifier.align(Alignment.TopEnd).size(20.dp))
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    3 -> {
                        TText("Follow Some Artists", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TText("Select up to 5 artists to follow", color = AccentOrange, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Search
                        OutlinedTextField(
                            value = artistSearch,
                            onValueChange = { artistSearch = it },
                            placeholder = { TText("Search artists...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground, focusedBorderColor = AccentOrange, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (artistSearchLoading) {
                            TText("Searching artists...", color = TextMuted, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // Show selected artists chips
                        if (selectedArtists.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                selectedArtists.values.forEach { a ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(AccentOrange.copy(alpha = 0.2f))
                                            .border(1.dp, AccentOrange, RoundedCornerShape(20.dp))
                                            .clickable { selectedArtists = selectedArtists - a.artist_id }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        TText("${a.artist_name ?: a.name ?: ""} ✕", color = AccentOrange, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        val filtered = if (artistSearch.length >= 2) searchedArtists else artists

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            filtered.forEach { artist ->
                                val isSelected = artist.artist_id in selectedArtists
                                val imgUrl = resolveImageUrl(artist.profile_picture_url ?: artist.imageUrl) ?: defaultProfileImageUrl()

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(72.dp).clickable {
                                        selectedArtists = if (isSelected) selectedArtists - artist.artist_id
                                        else if (selectedArtists.size < 5) selectedArtists + (artist.artist_id to artist)
                                        else selectedArtists
                                    }
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, if (isSelected) AccentOrange else Color.Transparent, CircleShape).background(Color.DarkGray)
                                        )
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.align(Alignment.BottomEnd).size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TText(artist.artist_name ?: artist.name ?: "", color = if (isSelected) AccentOrange else TextWhite, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (step > 1) {
                    IconButton(onClick = { step-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite) }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                val currentCount = when (step) { 1 -> selectedLanguages.size; 2 -> selectedGenres.size; else -> selectedArtists.size }
                TText("$currentCount selected", color = TextMuted, fontSize = 13.sp)

                Button(
                    onClick = {
                        if (step < 3) { step++ }
                        else {
                            saving = true
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val session = api.getSession()
                                    val userId = session.user?.id
                                    if (userId != null) {
                                        val selectedLanguageNames = languages.filter { it.language_id in selectedLanguages }.map { it.name }
                                        val selectedGenreNames = genres.filter { it.genre_id in selectedGenres }.map { it.name }
                                        val selectedArtistPrefs = selectedArtists.values.map {
                                            com.example.data.remote.FavoriteArtistPref(
                                                name = it.artist_name ?: it.name ?: "",
                                                soundcharts_uuid = it.soundcharts_uuid ?: it.artist_id,
                                                imageUrl = it.imageUrl ?: it.profile_picture_url,
                                            )
                                        }
                                        api.savePreferences(com.example.data.remote.SavePreferencesRequest(
                                            userId = userId,
                                            genres = selectedGenreNames,
                                            languages = selectedLanguageNames,
                                            favoriteArtists = selectedArtistPrefs,
                                        ))
                                    }
                                    onComplete()
                                } catch (_: Exception) { onComplete() }
                                saving = false
                            }
                        }
                    },
                    enabled = !saving && when (step) {
                        1 -> selectedLanguages.isNotEmpty()
                        2 -> selectedGenres.size >= 3
                        else -> true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, disabledContainerColor = AccentOrange.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else {
                        TText(if (step == 3) "Finish" else "Next", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
