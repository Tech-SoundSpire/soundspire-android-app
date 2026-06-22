package com.example.ui.screens

import com.example.ui.components.TText

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.util.S3Uploader
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiClient
import com.example.data.remote.CompleteProfileRequest
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.COUNTRY_BY_NAME
import com.example.util.citySuggestions
import com.example.util.countryForCity
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    authViewModel: AuthViewModel,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var profileImage by remember { mutableStateOf<Uri?>(null) }
    // Existing (remote) profile picture URL prefilled for artist-turned-fan; shown until the
    // user picks a new local image.
    var existingPictureUrl by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var phoneDigitLength by remember { mutableStateOf(10) }
    var city by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var genderExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    // City search: remote results (full dataset via backend) with curated offline fallback.
    var remoteCities by remember { mutableStateOf<List<com.example.data.remote.CityResult>>(emptyList()) }
    var cityQuery by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) profileImage = uri
    }

    // An artist viewing the fan profile: their name/city/country/phone came from artist signup
    // and must NOT be editable here (only gender + DOB are theirs to add). Mirrors the website.
    val currentUser by authViewModel.currentUser.collectAsState()
    val isArtistSwitching = currentUser?.isAlsoArtist == true

    val genders = listOf("Male", "Female", "Other")
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedContainerColor = CardBackground,
        unfocusedContainerColor = CardBackground,
    )

    fun validate(): Map<String, String> {
        val errs = mutableMapOf<String, String>()
        if (fullName.isBlank()) errs["full_name"] = "Full name is required"
        else if (!fullName.matches(Regex("^[A-Za-z\\s]+$"))) errs["full_name"] = "Full name should only contain letters"
        if (gender.isBlank()) errs["gender"] = "Please select gender"
        if (dob.isBlank()) errs["dob"] = "Date of birth is required"
        else {
            try {
                val parts = dob.split("-")
                val year = parts[0].toInt()
                val age = Calendar.getInstance().get(Calendar.YEAR) - year
                if (age < 13) errs["dob"] = "You must be at least 13 years old"
            } catch (_: Exception) { errs["dob"] = "Invalid date" }
        }
        if (city.isBlank()) errs["city"] = "City is required"
        if (country.isBlank()) errs["country"] = "Please select a country"
        if (phone.isBlank()) errs["phone"] = "Phone number is required"
        else if (!phone.matches(Regex("^\\d+$"))) errs["phone"] = "Phone must contain only digits"
        else if (phone.length != phoneDigitLength) errs["phone"] = "Phone must be $phoneDigitLength digits for $country"
        return errs
    }

    // Prefill from the existing profile (e.g. an artist switching to fan keeps their name,
    // city, country, phone, photo). Mirrors the website's complete-profile prefill.
    LaunchedEffect(Unit) {
        try {
            val email = authViewModel.currentUser.value?.email
                ?: try { api.getSession().user?.email } catch (_: Exception) { null }
                ?: return@LaunchedEffect
            val p = api.getProfile(email)
            if (fullName.isBlank()) p.full_name?.let { fullName = it }
            if (gender.isBlank()) p.gender?.let { gender = it }
            if (dob.isBlank()) p.date_of_birth?.let { dob = it.take(10) } // normalize any ISO datetime to yyyy-MM-dd
            if (city.isBlank()) p.city?.let { city = it }
            if (profileImage == null && existingPictureUrl == null) p.profile_picture_url?.let { existingPictureUrl = it }
            // Derive country + dial code + phone length from the stored country name.
            if (country.isBlank()) p.country?.let { c ->
                country = c
                COUNTRY_BY_NAME[c]?.let { ci -> countryCode = ci.dialCode; phoneDigitLength = ci.phoneLen }
            }
            // Phone is stored as "+<code>-<number>"; show just the local number.
            if (phone.isBlank()) p.mobile_number?.let { m ->
                phone = m.substringAfterLast('-', m).filter { it.isDigit() }.takeLast(phoneDigitLength)
            }
        } catch (_: Exception) { /* leave fields empty on error */ }
    }

    // Debounced city search against the backend (full country-state-city dataset).
    LaunchedEffect(cityQuery) {
        val q = cityQuery.trim()
        if (q.length < 2) { remoteCities = emptyList(); return@LaunchedEffect }
        delay(300)
        remoteCities = try { api.searchCities(q).cities } catch (_: Exception) { emptyList() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            TText("Complete Your Profile", color = HeadingPeach, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            TText("Tell us about yourself to get started.", color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Profile picture picker
            Box(
                modifier = Modifier.size(110.dp).clip(CircleShape).background(CardBackground)
                    .clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                when {
                    profileImage != null -> AsyncImage(model = profileImage, contentDescription = "Profile picture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    existingPictureUrl != null -> AsyncImage(model = resolveImageUrl(existingPictureUrl), contentDescription = "Profile picture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else -> Icon(Icons.Default.PhotoCamera, contentDescription = "Add photo", tint = AccentOrange, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TText(if (profileImage != null || existingPictureUrl != null) "Tap to change photo" else "Add a profile picture (optional)", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Full Name (locked for artist-turned-fan when prefilled)
            val fullNameLocked = isArtistSwitching && fullName.isNotBlank()
            OutlinedTextField(value = fullName, onValueChange = { if (!fullNameLocked) fullName = it }, readOnly = fullNameLocked, label = { TText("Full Name *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = fieldColors, isError = errors.containsKey("full_name"))
            if (errors["full_name"] != null) TText(errors["full_name"]!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(14.dp))

            // Gender
            ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                OutlinedTextField(value = gender.ifEmpty { "Select Gender *" }, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = fieldColors, isError = errors.containsKey("gender"))
                ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    genders.forEach { g -> DropdownMenuItem(text = { TText(g) }, onClick = { gender = g; genderExpanded = false }) }
                }
            }
            if (errors["gender"] != null) TText(errors["gender"]!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(14.dp))

            // Date of Birth — tap to open date picker
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                readOnly = true,
                label = { TText("Date of Birth *") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            dob = "%04d-%02d-%02d".format(y, m + 1, d)
                        }, cal.get(Calendar.YEAR) - 18, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AccentOrange)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        dob = "%04d-%02d-%02d".format(y, m + 1, d)
                    }, cal.get(Calendar.YEAR) - 18, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                isError = errors.containsKey("dob")
            )
            if (errors["dob"] != null) TText(errors["dob"]!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(14.dp))

            // City (searchable) — selecting a city auto-fills the country + phone code,
            // mirroring the website's city-first flow. Locked for artist-turned-fan when prefilled.
            val cityLocked = isArtistSwitching && city.isNotBlank()
            ExposedDropdownMenuBox(expanded = cityExpanded && !cityLocked, onExpandedChange = { if (!cityLocked) cityExpanded = it }) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { input ->
                        if (cityLocked) return@OutlinedTextField
                        city = input
                        cityQuery = input            // triggers debounced remote search
                        cityExpanded = input.isNotBlank()
                        // Best-effort instant auto-fill from the curated offline list
                        countryForCity(input)?.let { ci ->
                            country = ci.name; countryCode = ci.dialCode; phoneDigitLength = ci.phoneLen
                        }
                    },
                    readOnly = cityLocked,
                    label = { TText("City *") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = fieldColors,
                    isError = errors.containsKey("city")
                )
                // Prefer the full remote dataset; fall back to the curated list when offline.
                val remoteItems = remoteCities.map { Triple(it.city, it.country ?: "", Triple(it.country ?: "", it.dialCode ?: "", it.phoneLen)) }
                val curatedItems = citySuggestions(city).map { (c, n) ->
                    val ci = countryForCity(c)
                    Triple(c, n, Triple(ci?.name ?: n, ci?.dialCode ?: "", ci?.phoneLen ?: 10))
                }
                val suggestions = if (cityLocked) emptyList() else (if (remoteItems.isNotEmpty()) remoteItems else curatedItems)
                if (suggestions.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                        suggestions.forEach { (cityName, countryName, meta) ->
                            DropdownMenuItem(
                                text = { TText(if (countryName.isNotBlank()) "$cityName — $countryName" else cityName) },
                                onClick = {
                                    city = cityName
                                    val (cName, dial, len) = meta
                                    if (cName.isNotBlank()) country = cName
                                    if (dial.isNotBlank()) countryCode = dial
                                    phoneDigitLength = len
                                    phone = ""
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (errors["city"] != null) TText(errors["city"]!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(14.dp))

            // Country — always derived from the selected city, never editable.
            OutlinedTextField(
                value = if (country.isNotEmpty()) "$country ($countryCode)" else "",
                onValueChange = {}, readOnly = true, enabled = false,
                label = { TText("Country *") },
                placeholder = { TText("Auto-filled from city", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = TextMuted.copy(alpha = 0.3f),
                    disabledTextColor = TextWhite.copy(alpha = 0.7f),
                    disabledContainerColor = CardBackground,
                    disabledLabelColor = TextMuted,
                    disabledPlaceholderColor = TextMuted.copy(alpha = 0.6f),
                ),
                isError = errors.containsKey("country")
            )
            if (errors["country"] != null) TText(errors["country"]!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(14.dp))

            // Phone — code is always derived (read-only); number is locked for artist-turned-fan
            // when prefilled.
            val phoneLocked = isArtistSwitching && phone.isNotBlank()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = countryCode, onValueChange = {}, readOnly = true, modifier = Modifier.weight(0.3f), shape = RoundedCornerShape(12.dp), colors = fieldColors, label = { TText("Code") })
                OutlinedTextField(value = phone, onValueChange = { if (!phoneLocked && it.all { c -> c.isDigit() } && it.length <= phoneDigitLength) phone = it }, readOnly = phoneLocked, label = { TText("Phone ($phoneDigitLength digits) *") }, modifier = Modifier.weight(0.7f), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors, isError = errors.containsKey("phone"))
            }
            if (errors["phone"] != null) TText(errors["phone"]!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val validationErrors = validate()
                    errors = validationErrors
                    if (validationErrors.isNotEmpty()) return@Button
                    loading = true
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // Upload a newly-picked image; otherwise keep the existing one
                            // (artist-turned-fan retains their artist photo).
                            val pictureUrl = profileImage?.let { uri ->
                                S3Uploader.upload(context, uri, "images/users/${System.nanoTime()}.jpg")
                            } ?: existingPictureUrl
                            api.completeProfile(CompleteProfileRequest(
                                full_name = fullName.trim(),
                                gender = gender,
                                date_of_birth = dob,
                                city = city.trim(),
                                country = country,
                                phone_number = "$countryCode-$phone",
                                profile_picture_url = pictureUrl,
                            ))
                            authViewModel.markProfileComplete()
                            onComplete()
                        } catch (e: Exception) {
                            val msg = if (e is retrofit2.HttpException) {
                                e.response()?.errorBody()?.string()?.let { body ->
                                    try { org.json.JSONObject(body).optString("error", "Failed") } catch (_: Exception) { null }
                                } ?: "Failed (${e.code()})"
                            } else e.message ?: "Failed to save"
                            errors = mapOf("submit" to msg)
                        } finally { loading = false }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else TText("Save & Continue  →", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            if (errors["submit"] != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TText(errors["submit"]!!, color = ErrorRed, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
