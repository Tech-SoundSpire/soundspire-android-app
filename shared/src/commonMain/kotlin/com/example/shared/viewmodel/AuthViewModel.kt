package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ForgotPasswordRequest
import com.example.shared.data.model.GoogleMobileAuthRequest
import com.example.shared.data.model.LoginRequest
import com.example.shared.data.model.SessionUser
import com.example.shared.data.model.SignupRequest
import com.example.shared.data.model.SwitchRoleRequest
import com.example.shared.network.CookieStore
import com.example.shared.network.SoundSpireApi
import com.example.shared.network.soundSpireJson
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * KMP port of the app's AuthViewModel. Same session/onboarding logic; error handling
 * adapted from Retrofit's HttpException to Ktor's ResponseException. Consumed by Compose
 * on Android and (via SKIE, Phase 9) SwiftUI on iOS.
 */
class AuthViewModel(
    private val api: SoundSpireApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<SessionUser?>(null)
    val currentUser: StateFlow<SessionUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authActionLoading = MutableStateFlow(false)
    val authActionLoading: StateFlow<Boolean> = _authActionLoading.asStateFlow()

    private val _isArtistAccount = MutableStateFlow(false)
    val isArtistAccount: StateFlow<Boolean> = _isArtistAccount.asStateFlow()

    private val _needsPreferences = MutableStateFlow(false)
    val needsPreferences: StateFlow<Boolean> = _needsPreferences.asStateFlow()

    private val _needsCompleteProfile = MutableStateFlow(false)
    val needsCompleteProfile: StateFlow<Boolean> = _needsCompleteProfile.asStateFlow()

    private val _forgotMessage = MutableStateFlow<String?>(null)
    val forgotMessage: StateFlow<String?> = _forgotMessage.asStateFlow()
    private val _forgotLoading = MutableStateFlow(false)
    val forgotLoading: StateFlow<Boolean> = _forgotLoading.asStateFlow()

    // Combined snapshot for the native UI to observe as one thing (cleaner Swift interop
    // than watching seven flows). Android can keep using the individual flows above.
    val uiState: StateFlow<AuthUiState> = combine(
        combine(_isLoading, _isLoggedIn, _isArtistAccount) { a, b, c -> Triple(a, b, c) },
        combine(_needsCompleteProfile, _needsPreferences) { a, b -> a to b },
        combine(_authError, _authActionLoading, _currentUser) { e, l, u -> Triple(e, l, u) },
    ) { core, needs, extra ->
        AuthUiState(
            isLoading = core.first,
            isLoggedIn = core.second,
            isArtist = core.third,
            needsCompleteProfile = needs.first,
            needsPreferences = needs.second,
            authError = extra.first,
            actionLoading = extra.second,
            currentUser = extra.third,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AuthUiState(isLoading = true),
    )

    init { checkSession() }

    private fun checkSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = api.getSession().user
                if (user != null) {
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    if (user.role == "artist") {
                        _isArtistAccount.value = true
                        _needsCompleteProfile.value = false
                        _needsPreferences.value = false
                    } else {
                        _isArtistAccount.value = false
                        _needsCompleteProfile.value = computeNeedsCompleteProfile(user.email ?: "", user.isAlsoArtist)
                        _needsPreferences.value = computeNeedsPreferences(user.id)
                    }
                } else {
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
                _isLoggedIn.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mirrors the website's useCheckCompleteProfileOnRoute: an artist-turned-fan only needs
     * gender + date_of_birth; a regular fan needs the full set. Returns true if INCOMPLETE.
     */
    private suspend fun computeNeedsCompleteProfile(email: String, isAlsoArtist: Boolean): Boolean {
        return try {
            val p = api.getProfile(email)
            val required = if (isAlsoArtist) {
                listOf(p.gender, p.date_of_birth)
            } else {
                listOf(p.full_name, p.gender, p.date_of_birth, p.mobile_number, p.city, p.country)
            }
            required.any { it.isNullOrBlank() }
        } catch (_: Exception) {
            false // on error, don't trap the user behind the gate
        }
    }

    private suspend fun computeNeedsPreferences(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return try { !api.checkPreferences(userId).hasPreferences } catch (_: Exception) { false }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authActionLoading.value = true
            _authError.value = null
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.message == "Logged In Success") {
                    val user = api.getSession().user
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    if (user?.role == "artist") {
                        _isArtistAccount.value = true
                        _needsCompleteProfile.value = false
                        _needsPreferences.value = false
                        onSuccess()
                        return@launch
                    }
                    _isArtistAccount.value = false
                    _needsCompleteProfile.value = computeNeedsCompleteProfile(email, user?.isAlsoArtist == true)
                    _needsPreferences.value = computeNeedsPreferences(user?.id)
                    onSuccess()
                } else {
                    _authError.value = response.message
                }
            } catch (e: Exception) {
                _authError.value = parseError(e, fallback = "Login failed")
            } finally {
                _authActionLoading.value = false
            }
        }
    }

    fun signup(username: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authActionLoading.value = true
            _authError.value = null
            try {
                val response = api.signup(SignupRequest(username, email, password))
                if (response.success || (response.message?.contains("verification", ignoreCase = true) == true)) {
                    onSuccess()
                } else {
                    _authError.value = response.error ?: response.message ?: "Signup failed"
                }
            } catch (e: Exception) {
                val msg = parseError(e, fallback = "Signup failed")
                // A "verify email" style response means signup actually succeeded.
                if (msg.contains("verify", ignoreCase = true) || (e as? ResponseException)?.response?.status?.value == 403) {
                    onSuccess()
                } else {
                    _authError.value = msg
                }
            } finally {
                _authActionLoading.value = false
            }
        }
    }

    fun handleGoogleIdToken(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authActionLoading.value = true
            _authError.value = null
            try {
                val response = api.googleMobileAuth(GoogleMobileAuthRequest(idToken))
                if (response.success && response.user != null) {
                    _currentUser.value = api.getSession().user
                    _isLoggedIn.value = true
                    _needsCompleteProfile.value = response.user.needsCompleteProfile
                    _needsPreferences.value = response.user.needsPreferences
                    onSuccess()
                } else {
                    _authError.value = response.error ?: "Google sign-in failed"
                }
            } catch (e: Exception) {
                _authError.value = parseError(e, fallback = "Google sign-in failed")
            } finally {
                _authActionLoading.value = false
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            cookieStore.clear()
            _currentUser.value = null
            _isLoggedIn.value = false
            onDone()
        }
    }

    fun clearError() { _authError.value = null }
    // Lets native sign-in flows (e.g. iOS GoogleSignIn) surface an error in the shared UI state.
    fun setAuthErrorMessage(message: String) { _authError.value = message }
    fun markProfileComplete() {
        _needsCompleteProfile.value = false
        // Re-check preferences: a Google new-user reports needsPreferences=false at login
        // (profile was still incomplete then), so refresh it now the profile is done.
        viewModelScope.launch {
            val uid = runCatching { api.getSession().user?.id }.getOrNull()
            _needsPreferences.value = computeNeedsPreferences(uid)
        }
    }
    fun markPreferencesComplete() { _needsPreferences.value = false }
    fun clearForgotMessage() { _forgotMessage.value = null }

    /**
     * Request a password-reset email. The reset happens on the website; the app only
     * triggers the email. Result surfaces via [forgotMessage].
     */
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _forgotLoading.value = true
            _forgotMessage.value = null
            try {
                val resp = api.forgotPassword(ForgotPasswordRequest(email.trim()))
                _forgotMessage.value = resp.message ?: "Reset link sent! Check your inbox."
            } catch (e: Exception) {
                _forgotMessage.value = parseError(e, fallback = "Couldn't send reset link. Check the email and try again.")
            } finally {
                _forgotLoading.value = false
            }
        }
    }

    /** Artist login — logs in, then verifies the account is an artist. */
    fun loginAsArtist(email: String, password: String, onArtistConfirmed: () -> Unit, onNotArtist: () -> Unit) {
        viewModelScope.launch {
            _authActionLoading.value = true
            _authError.value = null
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.message == "Logged In Success") {
                    var user = api.getSession().user
                    if (user?.role == "artist" || user?.isAlsoArtist == true) {
                        try {
                            api.switchRole(SwitchRoleRequest("artist"))
                            user = api.getSession().user
                        } catch (_: Exception) {}
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        _isArtistAccount.value = true
                        _needsCompleteProfile.value = false
                        _needsPreferences.value = false
                        onArtistConfirmed()
                    } else {
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        onNotArtist()
                    }
                } else {
                    _authError.value = response.message
                }
            } catch (e: Exception) {
                _authError.value = parseError(e, fallback = "Login failed")
            } finally {
                _authActionLoading.value = false
            }
        }
    }

    /**
     * Switch the active role (artist <-> user). Re-fetches session and, when switching to fan,
     * recomputes onboarding needs. The caller reads [needsCompleteProfile]/[needsPreferences]/
     * [isArtistAccount] in [onDone] to decide the destination.
     */
    fun switchRole(role: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.switchRole(SwitchRoleRequest(role))
                val user = api.getSession().user
                _currentUser.value = user
                if (role == "artist") {
                    _isArtistAccount.value = true
                    _needsCompleteProfile.value = false
                    _needsPreferences.value = false
                } else {
                    _isArtistAccount.value = false
                    _needsCompleteProfile.value = computeNeedsCompleteProfile(user?.email ?: "", user?.isAlsoArtist == true)
                    _needsPreferences.value = computeNeedsPreferences(user?.id)
                }
            } catch (_: Exception) {}
            onDone()
        }
    }

    // Extracts the backend's error message from a Ktor ResponseException, falling back to
    // a status-based or generic message.
    private suspend fun parseError(e: Throwable, fallback: String): String {
        if (e is ResponseException) {
            val body = runCatching { e.response.bodyAsText() }.getOrDefault("")
            val obj = runCatching { soundSpireJson.parseToJsonElement(body).jsonObject }.getOrNull()
            val msg = obj?.get("message")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: obj?.get("error")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            if (msg != null) return msg
            return when (e.response.status.value) {
                401 -> "Invalid email or password"
                403 -> "Please verify your email before logging in"
                else -> fallback
            }
        }
        return fallback
    }
}

/** Combined auth snapshot for the native UI. */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isArtist: Boolean = false,
    val needsCompleteProfile: Boolean = false,
    val needsPreferences: Boolean = false,
    val authError: String? = null,
    val actionLoading: Boolean = false,
    val currentUser: SessionUser? = null,
)
