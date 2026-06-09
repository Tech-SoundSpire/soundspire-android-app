package com.example.data.remote

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.data.model.Artist
import com.example.data.model.Comment
import com.example.data.model.Review
import com.example.data.model.UserProfile

// Retrofit DTOs
data class AuthRequest(val email: String, val password: String, val role: String? = null, val name: String? = null)
data class AuthResponse(val token: String, val userId: String, val user: UserProfile)
data class SyncResponse(val success: Boolean, val message: String)

interface SoundSpireService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    @GET("users/profile")
    suspend fun getProfile(@Header("Authorization") token: String): UserProfile

    @PUT("users/profile/update")
    suspend fun updateProfile(@Header("Authorization") token: String, @Body profile: UserProfile): UserProfile

    @GET("preferences")
    suspend fun getPreferences(@Header("Authorization") token: String): List<String>

    @POST("preferences")
    suspend fun setPreferences(@Header("Authorization") token: String, @Body preferences: List<String>): SyncResponse

    @GET("community/artists")
    suspend fun getArtists(@Query("genre") genre: String? = null): List<Artist>

    @GET("reviews/{artistId}")
    suspend fun getArtistReviews(@Path("artistId") artistId: String): List<Review>

    @POST("reviews")
    suspend fun postReview(@Header("Authorization") token: String, @Body review: Review): SyncResponse

    @GET("comments/{artistId}")
    suspend fun getComments(@Path("artistId") artistId: String): List<Comment>

    @POST("comments")
    suspend fun postComment(@Header("Authorization") token: String, @Body comment: Comment): SyncResponse
}

object SoundSpireApiBuilder {
    private var activeBaseUrl = "https://soundspire.online/api/"

    fun getService(baseUrl: String? = null): SoundSpireService {
        val url = if (baseUrl.isNullOrBlank()) activeBaseUrl else {
            if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        }
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SoundSpireService::class.java)
    }
}
