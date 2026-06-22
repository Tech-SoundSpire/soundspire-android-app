package com.example.ui.screens.artist

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuthInputBorder
import com.example.ui.theme.AuthTextDark
import com.example.ui.theme.AuthTextMuted
import com.example.ui.theme.AuthWhite
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.AuthViewModel

private val ArtistOrange = Color(0xFFFA6400)

@Composable
fun ArtistLoginScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notArtistError by remember { mutableStateOf<String?>(null) }

    val isLoading by authViewModel.authActionLoading.collectAsState()
    val error by authViewModel.authError.collectAsState()
    val displayError = notArtistError ?: error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF2D1B4E), Color(0xFF0A0612))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TText("Welcome Back, Artist", color = ArtistOrange, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            TText("Your Stage,\nYour Voice,\nYour Community.", color = TextWhite.copy(alpha = 0.7f), fontSize = 16.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, lineHeight = 20.sp)

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth().background(AuthWhite, RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TText(if (isLoading) "Logging you in..." else "Artist Login", color = AuthTextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(20.dp))

                TText("Email", color = AuthTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it; notArtistError = null; authViewModel.clearError() },
                    placeholder = { TText("Enter your email", color = AuthTextMuted.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArtistOrange, unfocusedBorderColor = AuthInputBorder, focusedTextColor = AuthTextDark, unfocusedTextColor = AuthTextDark)
                )

                Spacer(modifier = Modifier.height(16.dp))

                TText("Password", color = AuthTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it; notArtistError = null; authViewModel.clearError() },
                    placeholder = { TText("Enter your password", color = AuthTextMuted.copy(alpha = 0.6f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArtistOrange, unfocusedBorderColor = AuthInputBorder, focusedTextColor = AuthTextDark, unfocusedTextColor = AuthTextDark)
                )

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TText(displayError, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            authViewModel.loginAsArtist(email, password,
                                onArtistConfirmed = onLoginSuccess,
                                onNotArtist = { notArtistError = "This account is not registered as an artist." }
                            )
                        }
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange, disabledContainerColor = ArtistOrange.copy(alpha = 0.5f))
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else TText("Login  →", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                TText("Back to Onboarding", color = AuthTextMuted, fontSize = 13.sp, modifier = Modifier.clickable { onBack() })
            }
        }
    }
}
