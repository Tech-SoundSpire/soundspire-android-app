package com.example.ui.screens.artist

import com.example.ui.components.TText

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.ArtistSignupRequest
import com.example.data.remote.ArtistSocial
import com.example.data.remote.CreateCommunityRequest
import com.example.ui.theme.TextMuted
import com.example.util.S3Uploader
import com.example.util.citySuggestions
import com.example.util.countryForCity
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ArtistOrange = Color(0xFFFA6400)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ArtistDetailsScreen(
    artistId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var loadingPrefill by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }

    var artistName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var genreInput by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var phoneLen by remember { mutableStateOf(10) }
    var phone by remember { mutableStateOf("") }
    var distributionCompany by remember { mutableStateOf("") }
    var communityName by remember { mutableStateOf("") }
    var communityDescription by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var remoteCities by remember { mutableStateOf<List<com.example.data.remote.CityResult>>(emptyList()) }
    var cityQuery by remember { mutableStateOf("") }

    var socials by remember { mutableStateOf(listOf(ArtistSocial("instagram", ""), ArtistSocial("youtube", ""))) }

    var profileUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var prefillImage by remember { mutableStateOf<String?>(null) }

    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showVerification by remember { mutableStateOf(false) }

    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) profileUri = uri }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) coverUri = uri }

    LaunchedEffect(artistId) {
        try {
            isLoggedIn = (api.getSession().user != null)
        } catch (_: Exception) { }
        try {
            val detail = api.getArtistByUuid(artistId)
            artistName = (detail["name"] as? String) ?: ""
            prefillImage = detail["imageUrl"] as? String
            (detail["biography"] as? String)?.let { bio = it }
        } catch (_: Exception) { }
        try {
            val ids = api.getArtistIdentifiers(artistId).items
            val mapped = ids.mapNotNull { id ->
                val platform = (id.platformName ?: id.platform ?: "").lowercase()
                val url = id.url ?: ""
                if (platform.isNotBlank() && url.isNotBlank() && platform in listOf("instagram", "youtube", "facebook", "twitter", "x", "tiktok"))
                    ArtistSocial(if (platform == "x") "twitter" else platform, url)
                else null
            }.distinctBy { it.platform }
            if (mapped.isNotEmpty()) socials = mapped
        } catch (_: Exception) { }
        loadingPrefill = false
    }

    // Debounced city search against the backend (full country-state-city dataset).
    LaunchedEffect(cityQuery) {
        val q = cityQuery.trim()
        if (q.length < 2) { remoteCities = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(300)
        remoteCities = try { api.searchCities(q).cities } catch (_: Exception) { emptyList() }
    }

    if (showVerification) {
        VerificationDialog(onContinue = onComplete)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Color(0xFF281545), Color.Black), radius = 1600f))
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            TText("Artist Details", color = Color(0xFFFFD0C2), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (loadingPrefill) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(28.dp))
            }
            return@Column
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Cover photo
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2D2838))
                    .clickable { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    AsyncImage(model = coverUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    TText("Tap to add cover photo", color = TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(-40.dp))
            // Profile photo overlapping
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFF2D2838)).border(3.dp, Color.Black, CircleShape)
                        .clickable { profilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    val model = profileUri ?: resolveImageUrl(prefillImage)
                    if (model != null) AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    else TText("Photo", color = TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ArtistField("Artist Name *", artistName) { artistName = it }
            ArtistField("Username *", username) { username = it }
            if (!isLoggedIn) {
                ArtistField("Email *", email, keyboardType = KeyboardType.Email) { email = it }
                ArtistField("Password *", password, isPassword = true) { password = it }
                ArtistField("Confirm Password *", confirmPassword, isPassword = true) { confirmPassword = it }
            }
            ArtistField("Bio *", bio, singleLine = false) { bio = it }

            // Genres — chips with orange outline + add field
            Spacer(modifier = Modifier.height(8.dp))
            TText("Genres *", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (selectedGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedGenres.forEach { g ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, ArtistOrange, RoundedCornerShape(50))
                                .background(ArtistOrange.copy(alpha = 0.12f))
                                .clickable { selectedGenres = selectedGenres - g }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            TText(g, color = ArtistOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, null, tint = ArtistOrange, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = genreInput,
                    onValueChange = { genreInput = it },
                    placeholder = { TText("Type a genre and tap Add", color = TextMuted) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    colors = artistFieldColors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val g = genreInput.trim()
                        if (g.isNotEmpty() && g !in selectedGenres) { selectedGenres = selectedGenres + g; genreInput = "" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange),
                    shape = RoundedCornerShape(8.dp),
                ) { TText("Add", color = Color.White, fontSize = 13.sp) }
            }

            // City (searchable) — selecting a city auto-fills the country + phone code,
            // mirroring the website's city-first flow.
            Spacer(modifier = Modifier.height(8.dp))
            TText("City *", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { cityExpanded = it }) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { input ->
                        city = input
                        cityQuery = input            // triggers debounced remote search
                        cityExpanded = input.isNotBlank()
                        countryForCity(input)?.let { ci ->
                            country = ci.name; countryCode = ci.dialCode; phoneLen = ci.phoneLen
                        }
                    },
                    placeholder = { TText("Type your city", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    colors = artistFieldColors()
                )
                // Prefer the full remote dataset; fall back to the curated list when offline.
                val remoteItems = remoteCities.map { Triple(it.city, it.country ?: "", Triple(it.country ?: "", it.dialCode ?: "", it.phoneLen)) }
                val curatedItems = citySuggestions(city).map { (c, n) ->
                    val ci = countryForCity(c)
                    Triple(c, n, Triple(ci?.name ?: n, ci?.dialCode ?: "", ci?.phoneLen ?: 10))
                }
                val suggestions = (if (remoteItems.isNotEmpty()) remoteItems else curatedItems)
                if (suggestions.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                        suggestions.forEach { (cityName, countryName, meta) ->
                            DropdownMenuItem(text = { TText(if (countryName.isNotBlank()) "$cityName — $countryName" else cityName) }, onClick = {
                                city = cityName
                                val (cName, dial, len) = meta
                                if (cName.isNotBlank()) country = cName
                                if (dial.isNotBlank()) countryCode = dial
                                phoneLen = len
                                phone = ""
                                cityExpanded = false
                            })
                        }
                    }
                }
            }

            // Country — always derived from the selected city, never editable.
            Spacer(modifier = Modifier.height(8.dp))
            TText("Country *", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = if (country.isNotEmpty()) "$country ($countryCode)" else "",
                onValueChange = {}, readOnly = true, enabled = false,
                placeholder = { TText("Auto-filled from city", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = artistFieldColors()
            )

            // Phone
            Spacer(modifier = Modifier.height(8.dp))
            TText("Phone ($phoneLen digits) *", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = phone, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= phoneLen) phone = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = artistFieldColors()
            )

            ArtistField("Distribution Company", distributionCompany) { distributionCompany = it }
            ArtistField("Community Name (auto-generated if left empty)", communityName) { communityName = it }
            ArtistField("Describe your community", communityDescription, singleLine = false) { communityDescription = it }

            // Socials
            Spacer(modifier = Modifier.height(16.dp))
            TText("Social Links", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            socials.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    TText(s.platform.replaceFirstChar { it.uppercase() }, color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                    OutlinedTextField(
                        value = s.url, onValueChange = { newUrl -> socials = socials.toMutableList().also { it[i] = s.copy(url = newUrl) } },
                        placeholder = { TText("https://...", color = TextMuted) }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = artistFieldColors()
                    )
                    IconButton(onClick = { socials = socials.filterIndexed { j, _ -> j != i } }) {
                        Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { socials = socials + ArtistSocial("instagram", "") }.padding(vertical = 4.dp)) {
                Icon(Icons.Default.Add, null, tint = ArtistOrange, modifier = Modifier.size(16.dp))
                TText("Add Link", color = ArtistOrange, fontSize = 13.sp)
            }

            // Terms
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acceptTerms, onCheckedChange = { acceptTerms = it }, colors = CheckboxDefaults.colors(checkedColor = ArtistOrange))
                TText("I accept the Terms & Conditions", color = Color.White, fontSize = 13.sp)
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TText(error!!, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    error = validate(acceptTerms, artistName, username, bio, selectedGenres.joinToString(","), email, phone, city, country, isLoggedIn, password, confirmPassword, phoneLen)
                    if (error != null) return@Button
                    submitting = true
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            var profileUrl = resolveImageUrl(prefillImage)
                            if (profileUri != null) {
                                S3Uploader.upload(context, profileUri!!, "images/artists/profile-${System.nanoTime()}.jpg")?.let { profileUrl = it }
                            }
                            var coverUrl: String? = null
                            if (coverUri != null) {
                                coverUrl = S3Uploader.upload(context, coverUri!!, "images/artists/cover-${System.nanoTime()}.jpg")
                            }

                            val resp = api.artistSignup(ArtistSignupRequest(
                                artist_name = artistName.trim(),
                                username = username.trim().ifBlank { null },
                                email = email.trim().ifBlank { null },
                                password_hash = password.ifBlank { null },
                                bio = bio.trim(),
                                phone = if (countryCode.isNotBlank()) "$countryCode-$phone" else phone,
                                city = city.trim(),
                                country = country,
                                socials = socials.filter { it.url.isNotBlank() },
                                genre_names = selectedGenres,
                                profile_picture_url = profileUrl,
                                cover_photo_url = coverUrl,
                                community_name = communityName.trim().ifBlank { null },
                                community_description = communityDescription.trim().ifBlank { null },
                                distribution_company = distributionCompany.trim().ifBlank { null },
                                third_party_platform = "soundcharts",
                                third_party_id = artistId,
                            ))
                            val newArtistId = resp.artist?.artist_id
                            if (resp.error != null && newArtistId == null) {
                                error = resp.error
                            } else {
                                if (newArtistId != null) {
                                    try {
                                        val cName = communityName.trim().ifBlank { "$artistName's Community" }
                                        val cDesc = communityDescription.trim().ifBlank { "Welcome to $artistName's official community!" }
                                        api.createCommunity(CreateCommunityRequest(
                                            artist_id = newArtistId,
                                            name = cName,
                                            description = cDesc
                                        ))
                                    } catch (_: Exception) { }
                                }
                                showVerification = true
                            }
                        } catch (e: retrofit2.HttpException) {
                            val body = e.response()?.errorBody()?.string()
                            error = body?.let { runCatching { org.json.JSONObject(it).optString("error") }.getOrNull() } ?: "Signup failed (${e.code()})"
                        } catch (e: Exception) {
                            error = e.message ?: "Signup failed"
                        } finally {
                            submitting = false
                        }
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange)
            ) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else TText("Create Artist Profile", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VerificationDialog(onContinue: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A0A2E)), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TText("✅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            TText("Artist Profile Created!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            TText("We've sent a verification email. Please verify your email, then log in.", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange), shape = RoundedCornerShape(8.dp)) {
                TText("Go to Artist Login", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun artistFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF1A1625),
    unfocusedContainerColor = Color(0xFF1A1625),
    focusedBorderColor = ArtistOrange,
    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    // Read-only/derived fields (e.g. Country) use enabled=false; keep them legible on dark bg.
    disabledContainerColor = Color(0xFF1A1625),
    disabledBorderColor = Color.Gray.copy(alpha = 0.4f),
    disabledTextColor = Color.White.copy(alpha = 0.75f),
    disabledPlaceholderColor = Color.Gray,
)

@Composable
private fun ArtistField(label: String, value: String, isPassword: Boolean = false, singleLine: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    TText(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().then(if (!singleLine) Modifier.height(100.dp) else Modifier),
        singleLine = singleLine,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = artistFieldColors()
    )
}

private fun validate(
    acceptTerms: Boolean, artistName: String, username: String, bio: String, genres: String,
    email: String, phone: String, city: String, country: String, isLoggedIn: Boolean,
    password: String, confirmPassword: String, phoneLen: Int
): String? {
    if (!acceptTerms) return "Please accept terms"
    if (artistName.isBlank()) return "Artist name is required"
    if (username.isBlank()) return "Username is required"
    if (bio.isBlank()) return "Bio is required"
    if (genres.isBlank()) return "At least one genre is required"
    if (email.isBlank() && !isLoggedIn) return "Email is required"
    if (phone.isBlank()) return "Phone number is required"
    if (phone.length != phoneLen) return "Phone must be $phoneLen digits for this country"
    if (city.isBlank()) return "City is required"
    if (country.isBlank()) return "Country is required"
    if (!isLoggedIn) {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.contains(Regex("[a-z]")) || !password.contains(Regex("[A-Z]")) || !password.contains(Regex("\\d")) || !password.contains(Regex("[@\$!%*?&#]")))
            return "Password must include upper, lower, number & special char"
        if (password != confirmPassword) return "Passwords do not match"
    }
    return null
}
