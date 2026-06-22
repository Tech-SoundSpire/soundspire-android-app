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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.CommunitySubscription
import com.example.data.remote.ProfileResponse
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.SubheadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import com.example.data.remote.ProfileUpdateRequest
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onSwitchToArtist: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }
    val currentUser by authViewModel.currentUser.collectAsState()

    var profile by remember { mutableStateOf<ProfileResponse?>(null) }
    var subscriptions by remember { mutableStateOf<List<CommunitySubscription>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        val email = currentUser?.email ?: return@LaunchedEffect
        try {
            profile = api.getProfile(email)
            val userId = currentUser?.id ?: return@LaunchedEffect
            subscriptions = api.getSubscriptions(userId).communities
        } catch (_: Exception) { }
        loading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TText("PROFILE", color = HeadingPeach, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp).clickable { onSettings() }
                    )
                    OutlinedButton(
                        onClick = { authViewModel.logout(onLogout) },
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                    ) {
                        TText("Logout", color = TextWhite, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { isEditing = !isEditing },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        TText("Edit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
                }
            }
        } else {
            // Avatar + Name
            item {
                val profileImageUrl = resolveImageUrl(profile?.profile_picture_url ?: currentUser?.photoURL) ?: defaultProfileImageUrl()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        TText(
                            text = profile?.full_name ?: currentUser?.name ?: "User",
                            color = TextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TText(
                            text = "@${profile?.username ?: currentUser?.email?.substringBefore("@") ?: ""}",
                            color = SubheadingPeach,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Info fields
            item {
                if (isEditing) {
                    EditableProfileForm(
                        profile = profile,
                        email = currentUser?.email,
                        api = api,
                        onSaved = { updatedProfile ->
                            profile = updatedProfile
                            isEditing = false
                        },
                        onCancel = { isEditing = false }
                    )
                } else {
                    ProfileInfoGrid(profile, currentUser?.email)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Subscriptions
            if (subscriptions.isNotEmpty()) {
                item {
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(20.dp))
                    TText("My Subscriptions", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(subscriptions) { sub ->
                            val subImg = resolveImageUrl(sub.artist_profile_picture_url ?: sub.artist_cover_photo_url) ?: defaultProfileImageUrl()
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                AsyncImage(
                                    model = subImg,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TText(sub.name ?: "", color = TextWhite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // Artist mode
            if (currentUser?.isAlsoArtist == true) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(20.dp))
                    TText("Artist Mode", color = Color(0xFFA855F6), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TText("You have an artist profile. Switch to manage your community, posts, and fans.", color = TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSwitchToArtist,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        TText("Switch to Artist Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Danger zone — only for non-artists (matches website)
            if (currentUser?.isAlsoArtist != true) item {
                var showDeleteDialog by remember { mutableStateOf(false) }
                var deleteInput by remember { mutableStateOf("") }
                var deleting by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(20.dp))
                TText("Danger Zone", color = ErrorRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TText("Permanently delete your account and all associated data. This cannot be undone.", color = TextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    TText("Delete My Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (showDeleteDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDeleteDialog = false; deleteInput = "" },
                        title = { TText("Are you sure?", color = ErrorRed, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                TText("This will permanently delete your account, preferences, reviews, comments, and all data.", color = TextWhite, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                TText("Type \"confirm delete\" to proceed:", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = deleteInput,
                                    onValueChange = { deleteInput = it },
                                    placeholder = { TText("confirm delete") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (deleteInput != "confirm delete") return@Button
                                    deleting = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        try {
                                            api.deleteAccount()
                                            authViewModel.logout(onLogout)
                                        } catch (_: Exception) { }
                                        deleting = false
                                    }
                                },
                                enabled = deleteInput == "confirm delete" && !deleting,
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            ) {
                                TText(if (deleting) "Deleting..." else "Delete!", color = Color.White)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeleteDialog = false; deleteInput = "" }) {
                                TText("Cancel", color = TextWhite)
                            }
                        },
                        containerColor = Color(0xFF1F2937),
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun EditableProfileForm(
    profile: ProfileResponse?,
    email: String?,
    api: com.example.data.remote.SoundSpireService,
    onSaved: (ProfileResponse) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(profile?.full_name ?: "") }
    var username by remember { mutableStateOf(profile?.username ?: "") }
    var phone by remember { mutableStateOf(profile?.mobile_number ?: "") }
    var dob by remember { mutableStateOf(profile?.date_of_birth?.take(10) ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "Other") }
    var city by remember { mutableStateOf(profile?.city ?: "") }
    var country by remember { mutableStateOf(profile?.country ?: "") }
    var saving by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedContainerColor = CardBackground,
        unfocusedContainerColor = CardBackground,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditField("Full Name *", fullName, { fullName = it }, fieldColors)
        EditField("Username *", username, { username = it }, fieldColors)

        // DOB with date picker
        Column(modifier = Modifier.fillMaxWidth()) {
            TText("Date of Birth", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    androidx.compose.material3.IconButton(onClick = {
                        val cal = java.util.Calendar.getInstance()
                        android.app.DatePickerDialog(context, { _, y, m, d ->
                            dob = "%04d-%02d-%02d".format(y, m + 1, d)
                        }, cal.get(java.util.Calendar.YEAR) - 18, cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                    }) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Edit, null, tint = AccentOrange)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = fieldColors,
            )
            val dobTooYoung = dob.isNotBlank() && kotlin.runCatching {
                val year = dob.split("-")[0].toInt()
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - year < 13
            }.getOrDefault(false)
            if (dobTooYoung) TText("Must be at least 13 years old", color = ErrorRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }

        EditField("Gender", gender, { gender = it }, fieldColors)

        // Phone with digits-only validation
        Column(modifier = Modifier.fillMaxWidth()) {
            TText("Phone Number", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '+' || c == '-' }) phone = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors,
            )
            if (phone.isNotBlank() && !phone.matches(Regex("^[+]?[\\d-]+$"))) {
                TText("Invalid phone number format", color = ErrorRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }
        }

        EditField("City", city, { city = it }, fieldColors)
        EditField("Country", country, { country = it }, fieldColors)

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(8.dp)) {
                TText("Cancel", color = TextWhite)
            }
            Button(
                onClick = {
                    saving = true
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            api.updateProfile(ProfileUpdateRequest(
                                email = email ?: "",
                                full_name = fullName.ifBlank { "User" },
                                username = username.ifBlank { email?.substringBefore("@") ?: "user" },
                                gender = gender.ifBlank { null },
                                date_of_birth = dob.ifBlank { null },
                                mobile_number = phone.ifBlank { null },
                                city = city.ifBlank { null },
                                country = country.ifBlank { null },
                            ))
                            val updated = api.getProfile(email ?: "")
                            onSaved(updated)
                        } catch (_: Exception) {
                            onCancel()
                        }
                        saving = false
                    }
                },
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else TText("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onValueChange: (String) -> Unit, colors: androidx.compose.material3.TextFieldColors, keyboardType: KeyboardType = KeyboardType.Text) {
    Column {
        TText(label, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = colors,
        )
    }
}

@Composable
private fun ProfileInfoGrid(profile: ProfileResponse?, email: String?) {
    val fields = listOf(
        "Email" to (profile?.email ?: email ?: "Not provided"),
        "Phone" to (profile?.mobile_number ?: "Not provided"),
        "DOB" to (profile?.date_of_birth?.take(10) ?: "Not provided"),
        "Gender" to (profile?.gender ?: "Other"),
        "City" to (profile?.city ?: "Not provided"),
        "Country" to (profile?.country ?: "Not provided"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fields.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        TText(label, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(CardBackground, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            TText(value, color = TextWhite, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
