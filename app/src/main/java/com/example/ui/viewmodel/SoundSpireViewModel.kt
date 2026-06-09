package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Artist
import com.example.data.model.Comment
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.data.repository.SoundSpireRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val profile: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class SoundSpireViewModel(private val repository: SoundSpireRepository) : ViewModel() {

    // Cache database seeding
    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Auth profile
    val userProfile: StateFlow<UserProfile?> = repository.currentUserProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Global selected backend URL
    val backendUrl = MutableStateFlow("https://soundspire.online/api/")

    // Search and Filters
    val searchQuery = MutableStateFlow("")
    val selectedGenre = MutableStateFlow("All")

    // Filtered Artists List
    val filteredArtists: StateFlow<List<Artist>> = combine(
        repository.allArtists,
        searchQuery,
        selectedGenre
    ) { artists, query, genre ->
        artists.filter { artist ->
            val matchesQuery = artist.name.contains(query, ignoreCase = true) ||
                    artist.genres.contains(query, ignoreCase = true) ||
                    artist.bio.contains(query, ignoreCase = true)
            val matchesGenre = genre == "All" || artist.genres.contains(genre, ignoreCase = true)
            matchesQuery && matchesGenre
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Selected Artist Details
    private val _selectedArtistId = MutableStateFlow<String?>(null)
    val selectedArtistId: StateFlow<String?> = _selectedArtistId

    private val _activeArtist = MutableStateFlow<Artist?>(null)
    val activeArtist: StateFlow<Artist?> = _activeArtist

    private val _activeReviews = MutableStateFlow<List<Review>>(emptyList())
    val activeReviews: StateFlow<List<Review>> = _activeReviews

    private val _activeComments = MutableStateFlow<List<Comment>>(emptyList())
    val activeComments: StateFlow<List<Comment>> = _activeComments

    // Active auth states
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState

    // Track active performance media playing
    val isVideoPlaying = MutableStateFlow(false)

    fun selectArtist(artistId: String?) {
        _selectedArtistId.value = artistId
        if (artistId != null) {
            // Find in current flow and fetch related lists
            viewModelScope.launch {
                repository.getArtistById(artistId).collect {
                    _activeArtist.value = it
                }
            }
            viewModelScope.launch {
                repository.getReviewsForArtist(artistId).collect {
                    _activeReviews.value = it
                }
            }
            viewModelScope.launch {
                repository.getCommentsForArtist(artistId).collect {
                    _activeComments.value = it
                }
            }
        } else {
            _activeArtist.value = null
            _activeReviews.value = emptyList()
            _activeComments.value = emptyList()
            isVideoPlaying.value = false
        }
    }

    // Profile Actions
    fun updateProfile(name: String, email: String, role: String, faveGenres: String, bio: String) {
        viewModelScope.launch {
            val updated = UserProfile(
                id = "local_user",
                name = name,
                email = email,
                role = role,
                faveGenres = faveGenres,
                bio = bio
            )
            repository.saveUserProfile(updated)
        }
    }

    // Fan Interactions
    fun submitReview(artistId: String, rating: Int, commentText: String) {
        viewModelScope.launch {
            val user = userProfile.value ?: UserProfile(name = "Anonymous Fan")
            val newReview = Review(
                id = UUID.randomUUID().toString(),
                artistId = artistId,
                fanName = user.name,
                rating = rating,
                comment = commentText,
                timestamp = System.currentTimeMillis()
            )
            repository.addNewReview(newReview)
            // Trigger refresh local details
            selectArtist(artistId)
        }
    }

    fun submitComment(artistId: String, commentText: String, parentId: String? = null) {
        viewModelScope.launch {
            val user = userProfile.value ?: UserProfile(name = "Kavya")
            val newComment = Comment(
                id = UUID.randomUUID().toString(),
                artistId = artistId,
                commentText = commentText,
                userName = if (user.role == "artist") "${user.name} (Artist)" else user.name,
                userRole = user.role,
                parentId = parentId,
                timestamp = System.currentTimeMillis()
            )
            repository.addNewComment(newComment)
            // Update comments live list
            selectArtist(artistId)
        }
    }

    // Artist Actions
    fun registerNewArtist(name: String, bio: String, genres: String, trackTitle: String, videoLink: String, bannerLink: String, avatarLink: String) {
        viewModelScope.launch {
            val newArtist = Artist(
                id = "artist_" + UUID.randomUUID().toString().take(6),
                name = name,
                bio = bio,
                genres = genres,
                featuredTrackTitle = trackTitle,
                videoUrl = if (videoLink.isNotBlank()) videoLink else "https://assets.mixkit.co/videos/preview/mixkit-hand-playing-a-synthesizer-keyboard-41710-large.mp4",
                bannerUrl = if (bannerLink.isNotBlank()) bannerLink else "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80",
                avatarUrl = if (avatarLink.isNotBlank()) avatarLink else "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&auto=format&fit=crop&q=80",
                ratingCount = 0,
                averageRating = 0.0,
                isVerified = false
            )
            repository.addNewArtist(newArtist)
        }
    }

    // Simulation / Online Login Auth
    fun loginOrRegister(email: String, pass: String, role: String, name: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            // First try network sync if valid custom URL inputted and of realistic shape
            val directProfile = if (backendUrl.value.contains("soundspire.online") || backendUrl.value.contains("api")) {
                 repository.executeOnlineLogin(backendUrl.value, email, pass)
            } else null

            if (directProfile != null) {
                _authUiState.value = AuthUiState.Success(directProfile)
            } else {
                // Instantly register / login on our local high fidelity DB
                val resolvedName = if (name.isNotBlank()) name else email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val p = UserProfile(
                    id = "local_user",
                    name = resolvedName,
                    email = email,
                    role = role,
                    faveGenres = "Synthwave, Indie Rock, Ambient Acoustic, Garage",
                    bio = "Newly registered ${role} on SoundSpire."
                )
                repository.saveUserProfile(p)
                _authUiState.value = AuthUiState.Success(p)
            }
        }
    }

    fun logout() {
        _authUiState.value = AuthUiState.Idle
        viewModelScope.launch {
            repository.saveUserProfile(UserProfile(name = "Guest Fan", email = "guest@soundspire.online", role = "fan", faveGenres = "None", bio = "Logged out"))
        }
    }
}

class SoundSpireViewModelFactory(private val repository: SoundSpireRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoundSpireViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SoundSpireViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
