package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiClient
import com.example.data.remote.LoginRequest
import com.example.data.remote.SessionUser
import com.example.data.remote.SignupRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getService(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<SessionUser?>(null)
    val currentUser: StateFlow<SessionUser?> = _currentUser

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _authActionLoading = MutableStateFlow(false)
    val authActionLoading: StateFlow<Boolean> = _authActionLoading

    private val _isArtistAccount = MutableStateFlow(false)
    val isArtistAccount: StateFlow<Boolean> = _isArtistAccount

    // Forgot-password flow state
    private val _forgotMessage = MutableStateFlow<String?>(null)
    val forgotMessage: StateFlow<String?> = _forgotMessage
    private val _forgotLoading = MutableStateFlow(false)
    val forgotLoading: StateFlow<Boolean> = _forgotLoading

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val session = api.getSession()
                val user = session.user
                if (user != null) {
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    if (user.role == "artist") {
                        _isArtistAccount.value = true
                        _needsCompleteProfile.value = false
                        _needsPreferences.value = false
                    } else {
                        // Fan session (incl. artist-turned-fan): gate on incomplete onboarding,
                        // mirroring the website's protected-route guard, so a relaunched user
                        // with a half-filled profile still lands on Complete Profile.
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

    private val _needsPreferences = MutableStateFlow(false)
    val needsPreferences: StateFlow<Boolean> = _needsPreferences

    private val _needsCompleteProfile = MutableStateFlow(false)
    val needsCompleteProfile: StateFlow<Boolean> = _needsCompleteProfile

    /**
     * Compute whether a fan-side account still needs to complete its profile.
     *
     * Mirrors the website's useCheckCompleteProfileOnRoute:
     *  - An artist-turned-fan (isAlsoArtist) only needs the extras an artist account lacks:
     *    gender + date_of_birth (name/city/country/phone came from artist signup).
     *  - A regular fan needs the full set: name, gender, dob, phone, city, country.
     * Returns true if the profile is INCOMPLETE.
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
            // On error, don't trap the user behind the gate.
            false
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
                    val session = api.getSession()
                    _currentUser.value = session.user
                    _isLoggedIn.value = true
                    // Artist accounts skip the fan onboarding and go to the dashboard
                    if (session.user?.role == "artist") {
                        _isArtistAccount.value = true
                        _needsCompleteProfile.value = false
                        _needsPreferences.value = false
                        onSuccess()
                        return@launch
                    }
                    _isArtistAccount.value = false
                    _needsCompleteProfile.value = computeNeedsCompleteProfile(email, session.user?.isAlsoArtist == true)
                    _needsPreferences.value = computeNeedsPreferences(session.user?.id)
                    onSuccess()
                } else {
                    _authError.value = response.message
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val parsed = try {
                    com.squareup.moshi.Moshi.Builder().build()
                        .adapter(com.example.data.remote.LoginResponse::class.java)
                        .fromJson(errorBody ?: "")
                } catch (_: Exception) { null }
                _authError.value = parsed?.message ?: when (e.code()) {
                    401 -> "Invalid email or password"
                    403 -> "Please verify your email before logging in"
                    else -> "Login failed (${e.code()})"
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Login failed"
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
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                // Parse the backend's {"error": "...", "message": "..."} body directly.
                val msg = errorBody?.let { body ->
                    try {
                        val json = org.json.JSONObject(body)
                        json.optString("error", "").ifBlank { json.optString("message", "") }
                    } catch (_: Exception) { "" }
                }?.takeIf { it.isNotBlank() } ?: "Signup failed (${e.code()})"
                if (msg.contains("verify", ignoreCase = true) || e.code() == 403) {
                    onSuccess()
                } else {
                    _authError.value = msg
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Signup failed"
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
                val response = api.googleMobileAuth(com.example.data.remote.GoogleMobileAuthRequest(idToken))
                if (response.success && response.user != null) {
                    val session = api.getSession()
                    _currentUser.value = session.user
                    _isLoggedIn.value = true
                    _needsCompleteProfile.value = response.user.needsCompleteProfile
                    _needsPreferences.value = response.user.needsPreferences
                    onSuccess()
                } else {
                    _authError.value = response.error ?: "Google sign-in failed"
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                _authError.value = "Google sign-in failed: ${e.code()}"
            } catch (e: Exception) {
                _authError.value = e.message ?: "Google sign-in failed"
            } finally {
                _authActionLoading.value = false
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (_: Exception) { }
            ApiClient.clearSession(getApplication())
            _currentUser.value = null
            _isLoggedIn.value = false
            onDone()
        }
    }

    fun clearError() {
        _authError.value = null
    }

    /** Mark the fan profile as complete after a successful Complete Profile submission. */
    fun markProfileComplete() {
        _needsCompleteProfile.value = false
    }

    /** Mark preferences as set after the user finishes preference selection. */
    fun markPreferencesComplete() {
        _needsPreferences.value = false
    }

    /**
     * Request a password-reset email. The backend emails a reset link that opens on the
     * website (/reset-password) — the reset itself happens there, so the app only needs
     * to trigger the email. Returns a success/error message via [forgotMessage].
     */
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _forgotLoading.value = true
            _forgotMessage.value = null
            try {
                val resp = api.forgotPassword(com.example.data.remote.ForgotPasswordRequest(email.trim()))
                _forgotMessage.value = resp.message ?: "Reset link sent! Check your inbox."
            } catch (e: retrofit2.HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { body ->
                    try { org.json.JSONObject(body).optString("message", "") } catch (_: Exception) { "" }
                }?.takeIf { it.isNotBlank() } ?: "Couldn't send reset link. Check the email and try again."
                _forgotMessage.value = msg
            } catch (e: Exception) {
                _forgotMessage.value = "Couldn't send reset link. Please try again."
            } finally {
                _forgotLoading.value = false
            }
        }
    }

    fun clearForgotMessage() {
        _forgotMessage.value = null
    }

    /** Artist login — logs in, then verifies the account is an artist. */
    fun loginAsArtist(email: String, password: String, onArtistConfirmed: () -> Unit, onNotArtist: () -> Unit) {
        viewModelScope.launch {
            _authActionLoading.value = true
            _authError.value = null
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.message == "Logged In Success") {
                    var session = api.getSession()
                    if (session.user?.role == "artist" || session.user?.isAlsoArtist == true) {
                        // Ensure the active role is artist so the dashboard/session is correct
                        try {
                            api.switchRole(com.example.data.remote.SwitchRoleRequest("artist"))
                            session = api.getSession()
                        } catch (_: Exception) { }
                        _currentUser.value = session.user
                        _isLoggedIn.value = true
                        // Artists bypass the fan onboarding flow entirely
                        _isArtistAccount.value = true
                        _needsCompleteProfile.value = false
                        _needsPreferences.value = false
                        onArtistConfirmed()
                    } else {
                        _currentUser.value = session.user
                        _isLoggedIn.value = true
                        onNotArtist()
                    }
                } else {
                    _authError.value = response.message
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val parsed = try {
                    com.squareup.moshi.Moshi.Builder().build()
                        .adapter(com.example.data.remote.LoginResponse::class.java)
                        .fromJson(errorBody ?: "")
                } catch (_: Exception) { null }
                _authError.value = parsed?.message ?: when (e.code()) {
                    401 -> "Invalid email or password"
                    403 -> "Please verify your email before logging in"
                    else -> "Login failed (${e.code()})"
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Login failed"
            } finally {
                _authActionLoading.value = false
            }
        }
    }

    /**
     * Switch the active role (artist <-> user). Re-fetches session and, when switching to the
     * fan role, recomputes onboarding needs so the caller can route an artist-turned-fan whose
     * fan profile is incomplete to Complete Profile / Preferences (matching the website's guard).
     * The caller should read [needsCompleteProfile] / [needsPreferences] / [isArtistAccount] in
     * [onDone] to decide the destination — just like the login flow does.
     */
    fun switchRole(role: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.switchRole(com.example.data.remote.SwitchRoleRequest(role))
                val session = api.getSession()
                _currentUser.value = session.user
                if (role == "artist") {
                    _isArtistAccount.value = true
                    _needsCompleteProfile.value = false
                    _needsPreferences.value = false
                } else {
                    _isArtistAccount.value = false
                    val user = session.user
                    _needsCompleteProfile.value = computeNeedsCompleteProfile(user?.email ?: "", user?.isAlsoArtist == true)
                    _needsPreferences.value = computeNeedsPreferences(user?.id)
                }
            } catch (_: Exception) { }
            onDone()
        }
    }
}
