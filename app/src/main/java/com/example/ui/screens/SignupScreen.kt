package com.example.ui.screens

import com.example.ui.components.TText

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AuthInputBorder
import com.example.ui.theme.AuthTextDark
import com.example.ui.theme.AuthTextMuted
import com.example.ui.theme.AuthWhite
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.soundSpireLogoUrl
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun SignupScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var signupMessage by remember { mutableStateOf<String?>(null) }

    val isLoading by authViewModel.authActionLoading.collectAsState()
    val serverError by authViewModel.authError.collectAsState()
    val displayError = localError ?: serverError

    val passwordChecks = getPasswordChecks(password)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = soundSpireLogoUrl(),
                contentDescription = "SoundSpire",
                modifier = Modifier.height(48.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TText("Join SoundSpire", color = AccentOrange, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic)
            Spacer(modifier = Modifier.height(8.dp))
            TText("Discover music.\nConnect with artists.", color = TextWhite.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuthWhite, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TText("Create Account", color = AuthTextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(20.dp))

                SignupField(label = "Username*", value = username, onValueChange = { username = it; localError = null; authViewModel.clearError() }, testTag = "signup_username")
                Spacer(modifier = Modifier.height(12.dp))
                SignupField(label = "Email*", value = email, onValueChange = { email = it; localError = null; authViewModel.clearError() }, keyboardType = KeyboardType.Email, testTag = "signup_email")
                Spacer(modifier = Modifier.height(12.dp))
                SignupField(label = "Password*", value = password, onValueChange = { password = it; localError = null; authViewModel.clearError() }, isPassword = true, testTag = "signup_password")

                // Password requirements checklist
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        passwordChecks.forEach { (label, passed) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (passed) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                TText(label, color = if (passed) SuccessGreen else ErrorRed, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SignupField(label = "Confirm Password*", value = confirmPassword, onValueChange = { confirmPassword = it; localError = null }, isPassword = true, imeAction = ImeAction.Done, testTag = "signup_confirm")

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TText(displayError!!, color = ErrorRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                if (signupMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TText(signupMessage!!, color = SuccessGreen, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        localError = validateSignup(username, email, password, confirmPassword, passwordChecks)
                        if (localError == null) {
                            authViewModel.signup(username, email, password) {
                                signupMessage = "Verification email sent! Check your inbox and verify before logging in."
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("signup_submit"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, disabledContainerColor = AccentOrange.copy(alpha = 0.5f))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        TText("Sign Up  →", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TText(
                    text = "Already have an account? Login",
                    color = AuthTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onNavigateToLogin() }.testTag("signup_to_login")
                )
            }
        }
    }
}

@Composable
private fun SignupField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    testTag: String = "",
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TText(text = label, color = AuthTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { TText("Enter ${label.lowercase().removeSuffix("*")}", color = AuthTextMuted.copy(alpha = 0.6f)) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().testTag(testTag),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = AuthInputBorder, focusedTextColor = AuthTextDark, unfocusedTextColor = AuthTextDark)
        )
    }
}

private fun getPasswordChecks(password: String): List<Pair<String, Boolean>> {
    return listOf(
        "At least 8 characters" to (password.length >= 8),
        "One lowercase letter" to password.contains(Regex("[a-z]")),
        "One uppercase letter" to password.contains(Regex("[A-Z]")),
        "One number" to password.contains(Regex("\\d")),
        "One special character (#,@,\$,&,!,%,*,?)" to password.contains(Regex("[@\$!%*?&#]")),
    )
}

private fun validateSignup(username: String, email: String, password: String, confirmPassword: String, checks: List<Pair<String, Boolean>>): String? {
    if (username.isBlank() || email.isBlank() || password.isBlank()) return "All fields are required"
    if (username.length < 3) return "Username must be at least 3 characters"
    if (!email.contains("@") || !email.contains(".")) return "Invalid email format"
    if (checks.any { !it.second }) return "Password does not meet all requirements"
    if (password != confirmPassword) return "Passwords do not match"
    if (!"[a-zA-Z0-9_-]+".toRegex().matches(username)) return "Username can only contain letters, numbers, underscores, or hyphens"
    return null
}
