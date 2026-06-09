package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.SoundSpireViewModel

@Composable
fun AuthScreen(
    viewModel: SoundSpireViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("fan") } // "fan" or "artist"
    var showAddressSettings by remember { mutableStateOf(false) }

    val rawBackendUrl by viewModel.backendUrl.collectAsState()
    val authUiState by viewModel.authUiState.collectAsState()

    // Listening for Auth success
    if (authUiState is AuthUiState.Success) {
        onAuthSuccess()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        // Aesthetic ambient studio lights in the background
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SoundSpireAccentPurple.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // SoundSpire Logo & Visual Branding Core
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        Brush.linearGradient(listOf(SoundSpireNeonTeal.copy(alpha = 0.1f), SoundSpireVibrantBlue.copy(alpha = 0.1f))),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = "SoundSpire Logo",
                    tint = SoundSpireNeonTeal,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SoundSpire",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "ARTIST COMMUNE",
                        color = SoundSpireVibrantBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isSignUp) "CREATE YOUR PORTAL" else "ENTER THE SOUNDSPIRE",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isSignUp) "Join artists and fans in discovering new musical horizons" else "Connect with verified musicians and interactive sessions",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auth Input Card Base
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = SoundSpireNeonTeal, spotColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Role Toggle for sign up
                    if (isSignUp) {
                        Text(
                            text = "SELECT YOUR PROFILE TYPE",
                            color = SoundSpireVibrantBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MidnightBlack)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedRole == "fan") SoundSpireNeonTeal else Color.Transparent)
                                    .clickable { selectedRole = "fan" }
                                    .testTag("role_fan_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Music Fan",
                                    color = if (selectedRole == "fan") MidnightBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedRole == "artist") SoundSpireNeonTeal else Color.Transparent)
                                    .clickable { selectedRole = "artist" }
                                    .testTag("role_artist_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Artist / Band",
                                    color = if (selectedRole == "artist") MidnightBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Fields
                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SoundSpireNeonTeal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoundSpireNeonTeal,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = SoundSpireNeonTeal
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = SoundSpireNeonTeal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoundSpireNeonTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = SoundSpireNeonTeal
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Security Access Token") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SoundSpireNeonTeal) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoundSpireNeonTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = SoundSpireNeonTeal
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error display if any
                    if (authUiState is AuthUiState.Error) {
                        Text(
                            text = (authUiState as AuthUiState.Error).message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Main Submit Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.loginOrRegister(email, password, selectedRole, name)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_auth_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoundSpireNeonTeal)
                    ) {
                        if (authUiState is AuthUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MidnightBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "GENERATE PORTAL" else "SYCHRONIZE",
                                color = MidnightBlack,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch Mode Text
                    Text(
                        text = if (isSignUp) "Already registered? Sign In" else "New to SoundSpire? Create Local Account",
                        color = SoundSpireVibrantBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { isSignUp = !isSignUp }
                            .padding(vertical = 4.dp)
                            .testTag("switch_auth_mode_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Backend URL Expander Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddressSettings = !showAddressSettings },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Developer Server Connection",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showAddressSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showAddressSettings,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Run a custom SoundSpire backend? Enter your base API URL below. If blank, the app runs locally on SQLite Cache.",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = rawBackendUrl,
                                onValueChange = { viewModel.backendUrl.value = it },
                                label = { Text("Base API URL", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backend_url_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoundSpireNeonTeal,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = SoundSpireNeonTeal
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
