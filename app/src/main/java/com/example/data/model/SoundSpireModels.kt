package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "local_user",
    val name: String = "Guest Fan",
    val email: String = "guest@soundspire.online",
    val role: String = "fan", // "fan" or "artist"
    val faveGenres: String = "Indie Rock, Synthwave, Lo-Fi", // comma-separated
    val avatarUrl: String = "",
    val bio: String = "Just loving music."
)

@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bio: String,
    val genres: String, // comma-separated
    val avatarUrl: String,
    val bannerUrl: String,
    val videoUrl: String, // Streaming track URL or .m3u8 demo link
    val ratingCount: Int = 0,
    val averageRating: Double = 0.0,
    val isVerified: Boolean = false,
    val featuredTrackTitle: String = "Live Session"
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val artistId: String,
    val fanName: String,
    val rating: Int, // 1-5
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val artistId: String,
    val commentText: String,
    val userName: String,
    val userRole: String, // "fan" or "artist"
    val parentId: String? = null, // null if top-level, or ID of other comment
    val timestamp: Long = System.currentTimeMillis()
)
