package com.example.ui.screens

import com.example.ui.components.TText

import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AuthInputBorder
import com.example.ui.theme.AuthTextDark
import com.example.ui.theme.AuthTextMuted
import com.example.ui.theme.AuthWhite
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.soundSpireLogoUrl
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    onArtistEntry: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLoading by authViewModel.authActionLoading.collectAsState()
    val error by authViewModel.authError.collectAsState()
    val forgotMessage by authViewModel.forgotMessage.collectAsState()
    val forgotLoading by authViewModel.forgotLoading.collectAsState()
    var showForgotDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // SoundSpire logo
            AsyncImage(
                model = soundSpireLogoUrl(),
                contentDescription = "SoundSpire",
                modifier = Modifier.height(48.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(16.dp))

            TText(
                text = "Welcome Back",
                color = AccentOrange,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TText(
                text = "Your Vibe,\nYour Beats,\nYour World Awaits.",
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuthWhite, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TText(
                    text = if (isLoading) "Logging you in..." else "Login",
                    color = AuthTextDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email
                TText("Email", color = AuthTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; authViewModel.clearError() },
                    placeholder = { TText("Enter your email", color = AuthTextMuted.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().testTag("login_email"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = AuthInputBorder, focusedTextColor = AuthTextDark, unfocusedTextColor = AuthTextDark)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password
                TText("Password", color = AuthTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; authViewModel.clearError() },
                    placeholder = { TText("Enter your password", color = AuthTextMuted.copy(alpha = 0.6f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("login_password"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (email.isNotBlank() && password.isNotBlank()) authViewModel.login(email, password, onLoginSuccess)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = AuthInputBorder, focusedTextColor = AuthTextDark, unfocusedTextColor = AuthTextDark)
                )

                Spacer(modifier = Modifier.height(8.dp))
                TText(
                    "Forgot password?",
                    color = AccentOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.End).clickable {
                        authViewModel.clearForgotMessage()
                        showForgotDialog = true
                    }
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TText(error!!, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login button
                Button(
                    onClick = { if (email.isNotBlank() && password.isNotBlank()) authViewModel.login(email, password, onLoginSuccess) },
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_submit"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, disabledContainerColor = AccentOrange.copy(alpha = 0.5f))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        TText("Login  →", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Divider(modifier = Modifier.weight(1f), color = AuthInputBorder)
                    TText("  or continue with  ", color = AuthTextMuted, fontSize = 12.sp)
                    Divider(modifier = Modifier.weight(1f), color = AuthInputBorder)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setServerClientId(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
                                    .setFilterByAuthorizedAccounts(false)
                                    .setAutoSelectEnabled(false)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                val result = credentialManager.getCredential(context, request)
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                authViewModel.handleGoogleIdToken(idToken, onLoginSuccess)
                            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                                Log.d("LoginScreen", "Google Sign-In cancelled by user")
                            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                                Log.e("LoginScreen", "No Google accounts available", e)
                                authViewModel.clearError()
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Google Sign-In error: ${e.javaClass.simpleName}: ${e.message}", e)
                                authViewModel.clearError()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_google"),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AccentOrange, AccentOrange))),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = AuthTextDark)
                ) {
                    TText("G", color = Color(0xFFDB4437), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    TText("Continue with Google", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                TText(
                    text = "Don't have an account yet? Sign up",
                    color = AuthTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onNavigateToSignup() }.testTag("login_to_signup")
                )

                Spacer(modifier = Modifier.height(10.dp))

                TText(
                    text = "Are you an artist?",
                    color = AccentOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onArtistEntry() }.testTag("login_artist_entry")
                )
            }
        }

        // Forgot-password dialog: collect email, trigger reset email (reset happens via web link)
        if (showForgotDialog) {
            var forgotEmail by remember { mutableStateOf(email) }
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                containerColor = AuthWhite,
                title = { TText("Reset Password", color = AuthTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        TText(
                            "Enter your email and we'll send you a link to reset your password.",
                            color = AuthTextMuted, fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            placeholder = { TText("Enter your email", color = AuthTextMuted.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = AuthInputBorder, focusedTextColor = AuthTextDark, unfocusedTextColor = AuthTextDark)
                        )
                        if (forgotMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TText(forgotMessage!!, color = AuthTextDark, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !forgotLoading && forgotEmail.isNotBlank(),
                        onClick = { authViewModel.forgotPassword(forgotEmail) }
                    ) {
                        TText(if (forgotLoading) "Sending..." else "Send Link", color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) {
                        TText("Close", color = AuthTextMuted)
                    }
                }
            )
        }
    }
}
