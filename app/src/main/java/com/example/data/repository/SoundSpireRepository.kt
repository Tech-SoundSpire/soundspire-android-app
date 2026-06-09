package com.example.data.repository

import android.util.Log
import com.example.data.local.ArtistDao
import com.example.data.local.CommentDao
import com.example.data.local.ReviewDao
import com.example.data.local.UserProfileDao
import com.example.data.model.Artist
import com.example.data.model.Comment
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.data.remote.AuthRequest
import com.example.data.remote.SoundSpireApiBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class SoundSpireRepository(
    private val userProfileDao: UserProfileDao,
    private val artistDao: ArtistDao,
    private val reviewDao: ReviewDao,
    private val commentDao: CommentDao
) {
    val currentUserProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    val allArtists: Flow<List<Artist>> = artistDao.getAllArtists()

    fun getArtistById(id: String): Flow<Artist?> = artistDao.getArtistById(id)
    fun getReviewsForArtist(artistId: String): Flow<List<Review>> = reviewDao.getReviewsForArtist(artistId)
    fun getCommentsForArtist(artistId: String): Flow<List<Comment>> = commentDao.getCommentsForArtist(artistId)

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertProfile(profile)
    }

    suspend fun addNewReview(review: Review) = withContext(Dispatchers.IO) {
        reviewDao.insertReview(review)
        // Recalculate and update candidate artist average rating
        val reviews = reviewDao.getReviewsForArtist(review.artistId).firstOrNull() ?: emptyList()
        val totalReviews = reviews.size
        val avgRating = if (totalReviews > 0) {
            reviews.sumOf { it.rating }.toDouble() / totalReviews
        } else {
            review.rating.toDouble()
        }
        artistDao.updateArtistRating(review.artistId, totalReviews, avgRating)
    }

    suspend fun addNewComment(comment: Comment) = withContext(Dispatchers.IO) {
        commentDao.insertComment(comment)
    }

    suspend fun addNewArtist(artist: Artist) = withContext(Dispatchers.IO) {
        artistDao.insertArtist(artist)
    }

    // Remote integration sync helper
    suspend fun executeOnlineLogin(baseUrl: String, email: String, pass: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val service = SoundSpireApiBuilder.getService(baseUrl)
            val response = service.login(AuthRequest(email, pass))
            if (response.token.isNotEmpty()) {
                val profile = response.user.copy(id = "local_user")
                userProfileDao.insertProfile(profile)
                profile
            } else null
        } catch (e: Exception) {
            Log.e("SoundSpireRepo", "Remote login failed", e)
            null
        }
    }

    // Seeding functionality for offline/first-use experience
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val currentArtists = artistDao.getAllArtists().firstOrNull()
        if (currentArtists.isNullOrEmpty()) {
            Log.d("SoundSpireRepo", "Seeding initial artist, review and comment records...")
            
            // Seed a default user profile to prevent null start
            userProfileDao.insertProfile(
                UserProfile(
                    id = "local_user",
                    name = "Jishnu",
                    email = "jishnu@gmail.online",
                    role = "fan",
                    faveGenres = "Synthwave, Indie Rock, Lo-Fi, Garage",
                    bio = "Co-founder of SoundSpire. Looking for raw performances."
                )
            )

            val artist1Id = "artist_astral_loom"
            val artist2Id = "artist_rust_threads"
            val artist3Id = "artist_echo_nebula"

            val initialArtists = listOf(
                Artist(
                    id = artist1Id,
                    name = "Astral Loom",
                    bio = "Nostalgic hardware synthesizers blended with dream-pop vocals. We are an indie electronic duo capturing late-night Berlin feelings.",
                    genres = "Synthwave, Dream Pop, Indie Electronic",
                    avatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-hand-playing-a-synthesizer-keyboard-41710-large.mp4",
                    ratingCount = 3,
                    averageRating = 4.8,
                    isVerified = true,
                    featuredTrackTitle = "Neon Afterglow (Attic Live Session)"
                ),
                Artist(
                    id = artist2Id,
                    name = "The Rust Threads",
                    bio = "Pure engine grime, heavy fuzz boxes, and whiskey garage vocals. Just raw underground rock direct from Detroit.",
                    genres = "Indie Rock, Blues, Garage",
                    avatarUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=200&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=800&auto=format&fit=crop&q=80",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-playing-an-electric-guitar-musician-close-up-41589-large.mp4",
                    ratingCount = 2,
                    averageRating = 4.3,
                    isVerified = true,
                    featuredTrackTitle = "Smokestack Jam (Live Garage Update)"
                ),
                Artist(
                    id = artist3Id,
                    name = "Echo Nebula",
                    bio = "Atmospheric fingerpicked acoustic textures paired with celestial lyrics. Recording our upcoming EP on misty mountain cliffs.",
                    genres = "Lo-Fi, Folk, Ambient Acoustic",
                    avatarUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200&auto=format&fit=crop&q=80",
                    bannerUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800&auto=format&fit=crop&q=80",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-spinning-vinyl-record-on-turntable-close-up-41582-large.mp4",
                    ratingCount = 2,
                    averageRating = 4.5,
                    isVerified = false,
                    featuredTrackTitle = "Stardust Lullaby (Acoustic Sunrise)"
                )
            )
            artistDao.insertAllArtists(initialArtists)

            // Seed Reviews
            val reviews = listOf(
                Review(
                    id = "rev_1",
                    artistId = artist1Id,
                    fanName = "Amara",
                    rating = 5,
                    comment = "Absolutely mesmerizing! The juno-106 bassline fits so perfectly with the dreamy vocal layers.",
                    timestamp = System.currentTimeMillis() - 86400000 * 2
                ),
                Review(
                    id = "rev_2",
                    artistId = artist1Id,
                    fanName = "Siddharth",
                    rating = 4,
                    comment = "Very retro yet feels so fresh. Hope they tour in India soon!",
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                Review(
                    id = "rev_3",
                    artistId = artist2Id,
                    fanName = "Liam",
                    rating = 5,
                    comment = "This is what real underground rock sounds like. Fuzz distortion is killer!",
                    timestamp = System.currentTimeMillis() - 36000000
                ),
                Review(
                    id = "rev_4",
                    artistId = artist3Id,
                    fanName = "Zoe",
                    rating = 4,
                    comment = "So peaceful. Gives me major Bon Iver vibes.",
                    timestamp = System.currentTimeMillis() - 40000000
                )
            )
            for (rev in reviews) {
                reviewDao.insertReview(rev)
            }

            // Seed Comments (including a nested reply!)
            val comment1Id = "com_1"
            val comment2Id = "com_2"
            val comments = listOf(
                Comment(
                    id = comment1Id,
                    artistId = artist1Id,
                    userName = "Kavya",
                    userRole = "fan",
                    commentText = "Wow, that synthesizer setup is absolute goals! What model is the red keyboard?",
                    timestamp = System.currentTimeMillis() - 72000000
                ),
                Comment(
                    id = "com_1_reply",
                    artistId = artist1Id,
                    userName = "Astral Loom (Artist)",
                    userRole = "artist",
                    commentText = "Thanks Kavya! It's a vintage Nord Lead 2. Glad you love the tones!",
                    parentId = comment1Id, // Nested reply!
                    timestamp = System.currentTimeMillis() - 60000000
                ),
                Comment(
                    id = comment2Id,
                    artistId = artist2Id,
                    userName = "Jishnu",
                    userRole = "fan",
                    commentText = "This garage acoustics is surprising good! Simple, clean recording setup.",
                    timestamp = System.currentTimeMillis() - 10000000
                )
            )
            for (com in comments) {
                commentDao.insertComment(com)
            }
        }
    }
}
