package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Artist
import com.example.data.model.Comment
import com.example.data.model.Review
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun getUserProfile(id: String = "local_user"): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)
}

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY averageRating DESC")
    fun getAllArtists(): Flow<List<Artist>>

    @Query("SELECT * FROM artists WHERE id = :id LIMIT 1")
    fun getArtistById(id: String): Flow<Artist?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllArtists(artists: List<Artist>)

    @Query("UPDATE artists SET ratingCount = :count, averageRating = :avg WHERE id = :id")
    suspend fun updateArtistRating(id: String, count: Int, avg: Double)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE artistId = :artistId ORDER BY timestamp DESC")
    fun getReviewsForArtist(artistId: String): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE artistId = :artistId ORDER BY timestamp ASC")
    fun getCommentsForArtist(artistId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
}
