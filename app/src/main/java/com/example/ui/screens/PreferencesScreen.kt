package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SoundSpireAccentPurple
import com.example.ui.theme.SoundSpireNeonTeal
import com.example.ui.theme.SoundSpireVibrantBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SoundSpireViewModel

@Composable
fun PreferencesScreen(
    viewModel: SoundSpireViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()

    var name by remember { mutableStateOf(profile?.name ?: "") }
    var email by remember { mutableStateOf(profile?.email ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var role by remember { mutableStateOf(profile?.role ?: "fan") }
    var faveGenres by remember { mutableStateOf(profile?.faveGenres ?: "Synthwave, Indie Rock") }

    val allGenresList = listOf("Synthwave", "Indie Rock", "Ambient Acoustic", "Techno", "Garage", "Jazz Fusion")

    // Sync state if profile state updates in viewmodel
    remember(profile) {
        if (profile != null) {
            name = profile!!.name
            email = profile!!.email
            bio = profile!!.bio
            role = profile!!.role
            faveGenres = profile!!.faveGenres
        }
        Unit
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        // Slim header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("preferences_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SoundSpireNeonTeal
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preferences Configuration",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SET PORTAL IDENTITY",
                color = SoundSpireVibrantBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pref_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Sync Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pref_email_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
            )

            // Bio
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Personal Summary") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .testTag("pref_bio_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
            )

            // Role switcher
            Text(
                text = "SWITCH CURRENT ROLE STATE",
                color = SoundSpireVibrantBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (role == "fan") SoundSpireNeonTeal else Color.Transparent)
                        .clickable { role = "fan" }
                        .testTag("pref_role_fan"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Music Fan",
                        color = if (role == "fan") MidnightBlack else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (role == "artist") SoundSpireNeonTeal else Color.Transparent)
                        .clickable { role = "artist" }
                        .testTag("pref_role_artist"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Artist / Band",
                        color = if (role == "artist") MidnightBlack else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Genre selector chips
            Text(
                text = "COSMIC INTEREST GENRES",
                color = SoundSpireAccentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allGenresList) { genreName ->
                    val isChecked = faveGenres.contains(genreName, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isChecked) SoundSpireAccentPurple else CardBackground)
                            .clickable {
                                faveGenres = if (isChecked) {
                                    faveGenres
                                        .replace(genreName, "")
                                        .replace(", ,", ",")
                                        .trim { it <= ' ' || it == ',' }
                                } else {
                                    if (faveGenres.isBlank()) genreName else "$faveGenres, $genreName"
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("pref_genre_toggle_$genreName"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = genreName,
                            color = if (isChecked) Color.White else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active Connection stats summary card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = SoundSpireNeonTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connection Console",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "API Endpoint: $backendUrl",
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = "Environment: Production Sandbox. When connection error occurs, the cache intercepts transactions seamlessly.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Commit Settings Button
            Button(
                onClick = {
                    viewModel.updateProfile(
                        name = if (name.isNotBlank()) name else "Indie Fan",
                        email = email,
                        role = role,
                        faveGenres = faveGenres,
                        bio = bio
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("preferences_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoundSpireNeonTeal)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = MidnightBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE & SYNCHRONIZE",
                    color = MidnightBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("preferences_logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(Color.Red.copy(alpha = 0.5f), Color.Red.copy(alpha = 0.5f)))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOGOUT SECURE CHANNEL",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
