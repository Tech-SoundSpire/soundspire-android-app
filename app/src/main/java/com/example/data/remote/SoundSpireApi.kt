package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Request/Response DTOs ---

data class LoginRequest(val email: String, val password_hash: String)
data class SignupRequest(val username: String, val email: String, val password_hash: String)
data class LoginResponse(val message: String, val redirect: String? = null)
data class SignupResponse(val message: String? = null, val success: Boolean = false, val error: String? = null)
data class ForgotPasswordRequest(val email: String)
data class ForgotPasswordResponse(val message: String? = null)
data class AppVersionResponse(
    val latestVersionCode: Int = 0,
    val versionName: String? = null,
    val downloadUrl: String? = null,
    val mandatory: Boolean = false,
    val message: String? = null,
)
data class CityResult(
    val city: String,
    val country: String? = null,
    val dialCode: String? = null,
    val phoneLen: Int = 10,
)
data class CitySearchResponse(val cities: List<CityResult> = emptyList())

data class SessionResponse(val user: SessionUser? = null)
data class SessionUser(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val photoURL: String? = null,
    val provider: String? = null,
    val role: String? = null,
    val isAlsoArtist: Boolean = false,
    val artistId: String? = null,
)

data class ProfileResponse(
    val user_id: String? = null,
    val full_name: String? = null,
    val username: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val mobile_number: String? = null,
    val date_of_birth: String? = null,
    val city: String? = null,
    val country: String? = null,
    val profile_picture_url: String? = null,
    val error: String? = null,
)

data class ProfileUpdateRequest(
    val email: String,
    val full_name: String,
    val username: String,
    val gender: String? = null,
    val mobile_number: String? = null,
    val date_of_birth: String? = null,
    val city: String? = null,
    val country: String? = null,
    val profile_picture_url: String? = null,
    val spotify_linked: Boolean? = null,
)

data class PreferencesCheckResponse(val hasPreferences: Boolean = false)
data class AvailableGenresResponse(val genres: List<GenreItem> = emptyList())
data class AvailableArtistsResponse(val artists: List<ExploreArtist> = emptyList())
data class LanguageItem(val language_id: String, val name: String)
data class AvailableLanguagesResponse(val languages: List<LanguageItem> = emptyList())
data class SoundchartsArtistItem(val uuid: String? = null, val name: String? = null, val imageUrl: String? = null)
data class SoundchartsArtistsResponse(val items: List<SoundchartsArtistItem> = emptyList())
data class CreateListRequest(val title: String, val description: String? = null)
data class SavePreferencesRequest(
    val userId: String,
    val genres: List<String>,
    val languages: List<String> = emptyList(),
    val favoriteArtists: List<FavoriteArtistPref> = emptyList(),
)
data class FavoriteArtistPref(val name: String, val soundcharts_uuid: String? = null, val imageUrl: String? = null)
data class ArtistVoteRequest(val soundcharts_uuid: String, val artist_name: String, val image_url: String? = null, val userId: String)
data class ArtistVoteResponse(val count: Int = 0, val userVoted: Boolean = false, val alreadyVoted: Boolean = false)

data class ExploreArtist(
    val artist_id: String,
    val artist_name: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val profile_picture_url: String? = null,
    val imageUrl: String? = null,
    val bio: String? = null,
    val onSoundSpire: Boolean? = null,
    val soundcharts_uuid: String? = null,
    // From /api/explore/artists: user_id present => onboarded; third_party_id => soundcharts uuid
    val user_id: String? = null,
    val third_party_id: String? = null,
)

data class GenreItem(val genre_id: String, val name: String)

data class SongReview(
    val review_id: String,
    val spotify_track_id: String,
    val review_text: String? = null,
    val rating: Double? = null,
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val created_at: String? = null,
    val user: ReviewUser? = null,
    val song: ReviewSong? = null,
)
data class ReviewComment(
    val comment_id: String? = null,
    val comment_text: String? = null,
    val username: String? = null,
    val profile_picture_url: String? = null,
    val created_at: String? = null,
)
data class ReviewCommentsResponse(val comments: List<ReviewComment> = emptyList())
data class ReviewCommentRequest(val comment_text: String)

data class ReviewUser(
    val user_id: String? = null,
    val username: String? = null,
    val profile_picture_url: String? = null,
)

data class ReviewSong(
    val track_name: String? = null,
    val artist_name: String? = null,
    val album_art_url: String? = null,
)

data class ReviewsFeedResponse(val reviews: List<SongReview> = emptyList())

data class CommunitySubscription(
    val name: String? = null,
    val artist_slug: String? = null,
    val artist_name: String? = null,
    val artist_profile_picture_url: String? = null,
    val artist_cover_photo_url: String? = null,
    val subscriber_count: Int = 0,
)

data class SubscriptionsResponse(
    val communities: List<CommunitySubscription> = emptyList(),
    val user: SubscriptionUser? = null,
)

data class SubscriptionUser(val profile_picture_url: String? = null)

data class PostItem(
    val post_id: String,
    val content_text: String? = null,
    val media_urls: List<String>? = null,
    val created_at: String? = null,
    val likes: List<Any> = emptyList(),
    val comments: List<Any> = emptyList(),
    val artist: PostArtist? = null,
)

data class PostArtist(
    val artist_name: String? = null,
    val profile_picture_url: String? = null,
    val slug: String? = null,
)

data class NotificationItem(
    val notification_id: String,
    val type: String? = null,
    val message: String,
    val link: String? = null,
    val is_read: Boolean = false,
    val actor_image: String? = null,
    val thumbnail: String? = null,
    val created_at: String? = null,
)

data class NotificationsResponse(val notifications: List<NotificationItem> = emptyList(), val unreadCount: Int = 0)

data class SearchResult(
    val type: String? = null,
    val id: String? = null,
    val name: String? = null,
    val image: String? = null,
    val slug: String? = null,
)

data class SearchResponse(
    val artists: List<SearchArtistResult> = emptyList(),
    val reviews: List<SearchReviewResult> = emptyList(),
    val communities: List<SearchCommunityResult> = emptyList(),
    val songs: List<SearchSongResult> = emptyList(),
    val users: List<SearchUserResult> = emptyList(),
)

data class SearchArtistResult(val artist_name: String? = null, val slug: String? = null, val profile_picture_url: String? = null)
data class SearchReviewResult(val review_id: String? = null, val title: String? = null, val spotify_track_id: String? = null, val rating: Double? = null)
data class SearchCommunityResult(val name: String? = null, val artist_slug: String? = null, val profile_picture_url: String? = null)
data class SearchSongResult(val spotify_track_id: String? = null, val track_name: String? = null, val artist_name: String? = null, val album_art_url: String? = null)
data class SearchUserResult(val user_id: String? = null, val username: String? = null, val full_name: String? = null, val profile_picture_url: String? = null)

data class SuggestedArtistsResponse(val artists: List<ExploreArtist> = emptyList())

// Catalog search (Spotify — paginated wrapper)
data class CatalogSearchResponse(
    val tracks: SpotifyPaginatedList<CatalogTrack>? = null,
    val artists: SpotifyPaginatedList<CatalogArtist>? = null,
    val albums: SpotifyPaginatedList<CatalogAlbum>? = null,
)
data class SpotifyPaginatedList<T>(val items: List<T>? = null, val total: Int? = null)
data class CatalogTrack(val id: String, val name: String, val duration_ms: Int? = null, val album: CatalogAlbum? = null, val artists: List<CatalogArtist>? = null)
data class CatalogAlbum(val id: String? = null, val name: String? = null, val images: List<CatalogImage>? = null, val artists: List<CatalogArtist>? = null)
data class CatalogArtist(val id: String? = null, val name: String? = null, val images: List<CatalogImage>? = null)
data class CatalogImage(val url: String? = null)

// Artist catalog page DTOs (match /api/catalog/artist/{id} and /albums)
data class CatalogArtistDetail(
    val id: String? = null,
    val name: String? = null,
    val images: List<CatalogImage>? = null,
    val genres: List<String>? = null,
    val spotify_url: String? = null,
    val top_tracks: List<CatalogTopTrack>? = null,
)
data class CatalogTopTrack(
    val id: String? = null,
    val name: String? = null,
    val album_name: String? = null,
    val album_art: String? = null,
    val duration_ms: Int? = null,
    val explicit: Boolean = false,
)
data class CatalogArtistAlbum(
    val id: String? = null,
    val name: String? = null,
    val total_tracks: Int? = null,
    val release_date: String? = null,
    val images: List<CatalogImage>? = null,
)
data class CatalogArtistAlbumsResponse(val albums: List<CatalogArtistAlbum> = emptyList(), val total: Int = 0)
data class ResolveArtistResponse(val soundchartsUuid: String? = null)

// Track metadata (from Spotify cache)
data class TrackMetadata(
    val spotify_track_id: String? = null,
    val track_name: String? = null,
    val artist_name: String? = null,
    val artist_id: String? = null,
    // Nullable: the API returns explicit null for these on some cache rows; a non-null
    // declaration makes Moshi throw on null, which silently failed the whole track fetch.
    val artists: List<TrackArtist>? = null,
    val album_name: String? = null,
    val album_art_url: String? = null,
    val duration_ms: Int? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val release_date: String? = null,
    val spotify_url: String? = null,
    val credits: List<TrackCredit>? = null,
)
data class TrackArtist(val id: String? = null, val name: String? = null)
data class TrackCredit(val name: String? = null, val role: String? = null)

// Album detail (mirrors /api/catalog/album/{id})
data class AlbumMetadata(
    val name: String? = null,
    val album_type: String? = null,
    val total_tracks: Int? = null,
    val release_date: String? = null,
    val images: List<CatalogImage>? = null,
    val artists: List<TrackArtist>? = null,
    val spotify_url: String? = null,
    val tracks: List<AlbumTrack>? = null,
)
data class AlbumTrack(
    val id: String? = null,
    val name: String? = null,
    val track_number: Int? = null,
    val duration_ms: Int? = null,
    val explicit: Boolean? = null,
    val artists: List<TrackArtist>? = null,
)
data class CacheAlbumRequest(
    val spotify_track_id: String,
    val track_name: String,
    val artist_name: String? = null,
    val artist_id: String? = null,
    val album_art_url: String? = null,
)

// Track ratings
data class TrackRatingResponse(
    val avg_rating: Double? = null,
    val rating_count: Int = 0,
    val review_count: Int = 0,
    val user_rating: Double? = null,
)

// Submit review/rating
data class SubmitReviewRequest(val spotify_track_id: String, val review_text: String, val rating: Double? = null)
data class SubmitRatingRequest(val spotify_track_id: String, val rating: Double)

// Lists & Journal
data class ListItem(val list_id: String, val title: String, val description: String? = null, val is_ranked: Boolean = false, val like_count: Int = 0, val created_at: String? = null)
data class ListsResponse(val lists: List<ListItem> = emptyList())
data class ListDetailItem(val item_id: String? = null, val spotify_track_id: String? = null, val position: Int? = null, val notes: String? = null, val song: ListDetailSong? = null)
data class ListDetailSong(val track_name: String? = null, val artist_name: String? = null, val album_art_url: String? = null, val duration_ms: Int? = null)
data class ListItemsResponse(val list: Any? = null, val items: List<ListDetailItem> = emptyList())
data class DiaryEntry(val entry_id: String, val spotify_track_id: String, val listened_date: String? = null, val rating: Double? = null, val liked: Boolean = false, val notes: String? = null)
data class DiaryResponse(val entries: List<DiaryEntry> = emptyList())

// Complete profile
data class CompleteProfileRequest(
    val full_name: String,
    val gender: String,
    val date_of_birth: String,
    val city: String,
    val country: String,
    val phone_number: String,
    val profile_picture_url: String? = null,
)

// Translation
data class TranslateRequest(val texts: List<String>, val targetLang: String)
data class TranslateResponse(val translations: List<String> = emptyList())

// Google mobile auth
data class GoogleMobileAuthRequest(val idToken: String)
data class GoogleMobileAuthResponse(
    val success: Boolean = false,
    val error: String? = null,
    val user: GoogleMobileUser? = null,
)
data class GoogleMobileUser(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val isNewUser: Boolean = false,
    val needsCompleteProfile: Boolean = false,
    val needsPreferences: Boolean = false,
)

// --- Artist flow DTOs ---
data class ArtistIdentifier(val platformName: String? = null, val platform: String? = null, val url: String? = null)
data class ArtistIdentifiersResponse(val items: List<ArtistIdentifier> = emptyList())

data class ArtistSignupRequest(
    val artist_name: String,
    val username: String? = null,
    val email: String? = null,
    val password_hash: String? = null,
    val bio: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val country: String? = null,
    val socials: List<ArtistSocial> = emptyList(),
    val genre_names: List<String> = emptyList(),
    val profile_picture_url: String? = null,
    val cover_photo_url: String? = null,
    val community_name: String? = null,
    val community_description: String? = null,
    val distribution_company: String? = null,
    val third_party_platform: String? = null,
    val third_party_id: String? = null,
)
data class ArtistSocial(val platform: String, val url: String)
data class ArtistSignupResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val artist: ArtistSignupArtist? = null,
    val requiresVerification: Boolean = false,
)
data class ArtistSignupArtist(val artist_id: String? = null, val artist_name: String? = null, val slug: String? = null)

data class CreateCommunityRequest(val artist_id: String, val name: String, val description: String)

data class ArtistMeResponse(val artist: ArtistMe? = null, val error: String? = null)
data class ArtistMe(
    val artist_id: String,
    val artist_name: String? = null,
    val bio: String? = null,
    val profile_picture_url: String? = null,
    val cover_photo_url: String? = null,
    val verification_status: String? = null,
    val slug: String? = null,
    val socials: List<ArtistSocial> = emptyList(),
    val community: ArtistCommunity? = null,
)
data class ArtistCommunity(val community_id: String? = null, val name: String? = null, val description: String? = null)

data class ArtistEditRequest(
    val bio: String? = null,
    val profile_picture_url: String? = null,
    val cover_photo_url: String? = null,
    val socials: List<ArtistSocial>? = null,
)

data class ArtistReview(
    val review_id: String,
    val title: String? = null,
    val text_content: String? = null,
    val author: String? = null,
    val review_date: String? = null,
    val created_at: String? = null,
    val image_urls: List<String>? = null,
)
data class ArtistReviewsResponse(val reviews: List<ArtistReview> = emptyList())

data class SwitchRoleRequest(val role: String)
data class SwitchRoleResponse(val success: Boolean = false, val role: String? = null)

data class UploadUrlRequest(val fileName: String, val fileType: String)
data class UploadUrlResponse(val uploadUrl: String? = null)

// --- Community / forum DTOs ---
data class CommunityForum(val forum_id: String, val community_id: String? = null, val name: String? = null, val forum_type: String? = null)
data class CommunityForumsResponse(val forums: List<CommunityForum> = emptyList())

data class ForumUser(val user_id: String? = null, val username: String? = null, val full_name: String? = null, val profile_picture_url: String? = null)
data class ForumMessage(
    val forum_post_id: String,
    val forum_id: String? = null,
    val user_id: String? = null,
    val content: String? = null,
    val media_type: String? = null,
    val media_urls: List<String>? = null,
    val parent_post_id: String? = null,
    val is_pinned: Boolean = false,
    val created_at: String? = null,
    val user: ForumUser? = null,
    val reactions: Map<String, List<String>>? = null,
)
data class ForumMessagesResponse(val messages: List<ForumMessage> = emptyList(), val hasMore: Boolean = false)

data class FanArtPost(
    val forum_post_id: String,
    val user_id: String? = null,
    val title: String? = null,
    val content: String? = null,
    val media_urls: List<String>? = null,
    val is_pinned: Boolean = false,
    val created_at: String? = null,
    val user: ForumUser? = null,
    val likes_count: Int = 0,
    val user_has_liked: Boolean = false,
    // Enriched client-side from Supabase (backend GET omits these):
    val reactions: Map<String, List<String>>? = null,
    val comments: List<FanArtComment> = emptyList(),
)
data class FanArtComment(
    val forum_post_id: String,
    val user_id: String? = null,
    val parent_post_id: String? = null,
    val content: String? = null,
    val created_at: String? = null,
    val user: ForumUser? = null,
    val reactions: Map<String, List<String>>? = null,
)
data class FanArtResponse(val posts: List<FanArtPost> = emptyList(), val total: Int = 0, val hasMore: Boolean = false)
data class FanArtCreateRequest(val title: String? = null, val content: String? = null, val imageUrls: List<String>)

data class CommunityPost(
    val post_id: String,
    val artist_id: String? = null,
    val community_id: String? = null,
    val content_text: String? = null,
    val media_urls: List<String>? = null,
    val created_at: String? = null,
    val likes: List<PostLike> = emptyList(),
    val comments: List<PostComment> = emptyList(),
    val artist: PostArtist? = null,
)
data class PostLike(val like_id: String? = null, val user_id: String? = null)
data class PostComment(
    val comment_id: String,
    val user_id: String? = null,
    val post_id: String? = null,
    val parent_comment_id: String? = null,
    val content: String? = null,
    val created_at: String? = null,
    val user: ForumUser? = null,
    val likes: List<PostLike> = emptyList(),
)
data class CommunityPostCreateRequest(val artist_id: String, val community_id: String, val content_text: String, val media_urls: List<String> = emptyList())

data class UserByIdResponse(val user: ForumUser? = null)
data class SubscriberCountResponse(val count: Int = 0)
data class SubscriptionStatusResponse(val subscribed: Boolean = false)
data class SubscribeRequest(
    val user_id: String,
    val community_id: String,
    val start_date: String,
    val end_date: String,
    val is_active: Boolean = true,
    val auto_renew: Boolean = true,
    val payment_id: String? = null,
    val created_at: String,
    val updated_at: String,
)
data class ReactionResponse(val reactions: Map<String, List<String>>? = null)

data class CommunitySlugResponse(val artist: CommunitySlugArtist? = null)
data class CommunitySlugArtist(
    val artist_id: String? = null,
    val artist_name: String? = null,
    val profile_picture_url: String? = null,
    val cover_photo_url: String? = null,
    val bio: String? = null,
    val socials: List<ArtistSocial> = emptyList(),
    val community: ArtistCommunity? = null,
)

// --- Retrofit Service ---

interface SoundSpireService {
    // Auth
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/users/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    @POST("api/users/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ForgotPasswordResponse

    @GET("api/app-version")
    suspend fun getAppVersion(): AppVersionResponse

    @GET("api/cities")
    suspend fun searchCities(@Query("q") q: String, @Query("limit") limit: Int = 10): CitySearchResponse

    @GET("api/auth/session")
    suspend fun getSession(): SessionResponse

    @POST("api/auth/logout")
    suspend fun logout(): Any

    @DELETE("api/users/delete-account")
    suspend fun deleteAccount(): Any

    // Profile
    @GET("api/profile")
    suspend fun getProfile(@Query("email") email: String): ProfileResponse

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Any

    @GET("api/check-username")
    suspend fun checkUsername(@Query("username") username: String): Map<String, Any>

    // Preferences
    // Backend requires ?userId= (returns 400 without it).
    @GET("api/preferences/check")
    suspend fun checkPreferences(@Query("userId") userId: String): PreferencesCheckResponse

    @POST("api/preferences/save")
    suspend fun savePreferences(@Body body: SavePreferencesRequest): Any

    @GET("api/preferences/available/genres")
    suspend fun getAvailableGenres(): AvailableGenresResponse

    @GET("api/preferences/available/artists")
    suspend fun getAvailableArtists(): AvailableArtistsResponse

    @GET("api/preferences/available/languages")
    suspend fun getAvailableLanguages(): AvailableLanguagesResponse

    @GET("api/artists")
    suspend fun searchArtistsSoundcharts(@Query("q") query: String): SoundchartsArtistsResponse

    @POST("api/catalog/lists")
    suspend fun createList(@Body body: CreateListRequest): Any

    // Artist details (SoundCharts)
    @GET("api/artists/{uuid}")
    suspend fun getArtistByUuid(@Path("uuid") uuid: String): Map<String, Any?>

    // Artist voting
    @GET("api/artist-vote")
    suspend fun getArtistVote(@Query("soundcharts_uuid") uuid: String, @Query("userId") userId: String? = null): ArtistVoteResponse

    @POST("api/artist-vote")
    suspend fun castArtistVote(@Body body: ArtistVoteRequest): ArtistVoteResponse

    // Explore. No `q` → featured artists only. q="" → ALL artists (incl. cached/not-onboarded
    // ones selected by any user), matching the website's "See More" behavior.
    @GET("api/explore/artists")
    suspend fun getExploreArtists(@Query("q") q: String? = null): List<ExploreArtist>

    @GET("api/explore/suggested")
    suspend fun getSuggestedArtists(@Query("userId") userId: String): SuggestedArtistsResponse

    @GET("api/explore/genres")
    suspend fun getGenres(): List<GenreItem>

    @GET("api/catalog/song-reviews/feed")
    suspend fun getReviewsFeed(@Query("page") page: Int = 1): ReviewsFeedResponse

    @GET("api/search")
    suspend fun search(@Query("search") query: String): SearchResponse

    @GET("api/catalog/track/{trackId}")
    suspend fun getTrackMetadata(@Path("trackId") trackId: String): TrackMetadata

    // Album detail (Spotify) + cache so album reviews display with art/name.
    @GET("api/catalog/album/{albumId}")
    suspend fun getAlbum(@Path("albumId") albumId: String): AlbumMetadata

    @POST("api/catalog/cache-album")
    suspend fun cacheAlbum(@Body body: CacheAlbumRequest): Any

    // Feed (same endpoint + shape as forum posts, scoped by userId across subscribed communities)
    @GET("api/community/posts")
    suspend fun getPosts(@Query("userId") userId: String): List<CommunityPost>

    // Communities
    @GET("api/community/subscribe")
    suspend fun getSubscriptions(@Query("user_id") userId: String): SubscriptionsResponse

    // Subscription status / subscribe / unsubscribe for a specific community
    @GET("api/community/subscribe")
    suspend fun getSubscriptionStatus(@Query("user_id") userId: String, @Query("community_id") communityId: String): SubscriptionStatusResponse

    @POST("api/community/subscribe")
    suspend fun subscribeToCommunity(@Body body: SubscribeRequest): Any

    @retrofit2.http.DELETE("api/community/subscribe")
    suspend fun unsubscribeFromCommunity(@Query("user_id") userId: String, @Query("community_id") communityId: String): Any

    @GET("api/communities/search")
    suspend fun searchCommunities(@Query("search") query: String): SubscriptionsResponse

    // Notifications
    @GET("api/notifications")
    suspend fun getNotifications(): NotificationsResponse

    @PATCH("api/notifications")
    suspend fun markNotificationsRead(@Body body: Map<String, String>): Any

    // Reviews
    @POST("api/catalog/song-reviews/{reviewId}/like")
    suspend fun likeReview(@Path("reviewId") reviewId: String): Any

    @DELETE("api/catalog/song-reviews/{reviewId}/like")
    suspend fun unlikeReview(@Path("reviewId") reviewId: String): Any

    @GET("api/catalog/song-reviews/{reviewId}/comments")
    suspend fun getReviewComments(@Path("reviewId") reviewId: String): ReviewCommentsResponse

    @POST("api/catalog/song-reviews/{reviewId}/comments")
    suspend fun commentOnReview(@Path("reviewId") reviewId: String, @Body body: ReviewCommentRequest): Any

    // Catalog search (Spotify)
    @GET("api/catalog/search")
    suspend fun searchCatalog(@Query("q") query: String, @Query("type") type: String = "track,artist,album", @Query("limit") limit: Int = 10): CatalogSearchResponse

    // Artist catalog page (Spotify): detail + discography. Both require the artist name hint.
    @GET("api/catalog/artist/{id}")
    suspend fun getCatalogArtist(@Path("id") id: String, @Query("name") name: String): CatalogArtistDetail

    @GET("api/catalog/artist/{id}/albums")
    suspend fun getCatalogArtistAlbums(@Path("id") id: String, @Query("name") name: String, @Query("limit") limit: Int = 20): CatalogArtistAlbumsResponse

    // Resolve a Spotify artist ID → SoundCharts uuid (for off-platform "vote" link).
    @GET("api/artists/resolve")
    suspend fun resolveSoundchartsUuid(@Query("spotifyId") spotifyId: String, @Query("name") name: String): ResolveArtistResponse

    // Track + ratings for review detail
    @GET("api/catalog/song-reviews/track/{trackId}")
    suspend fun getTrackReviews(@Path("trackId") trackId: String, @Query("sort") sort: String = "popular"): ReviewsFeedResponse

    @GET("api/catalog/ratings/track/{trackId}")
    suspend fun getTrackRatings(@Path("trackId") trackId: String): TrackRatingResponse

    @POST("api/catalog/song-reviews")
    suspend fun submitReview(@Body body: SubmitReviewRequest): Any

    @POST("api/catalog/ratings")
    suspend fun submitRating(@Body body: SubmitRatingRequest): Any

    // Lists & Journal
    @GET("api/catalog/lists/mine")
    suspend fun getMyLists(): ListsResponse

    @GET("api/catalog/diary")
    suspend fun getDiary(@Query("limit") limit: Int = 50): DiaryResponse

    @GET("api/catalog/lists/{listId}")
    suspend fun getListItems(@Path("listId") listId: String): ListItemsResponse

    @POST("api/catalog/lists/{listId}/items")
    suspend fun addToList(@Path("listId") listId: String, @Body body: Map<String, String>): Any

    // Complete profile (onboarding)
    @POST("api/users/complete-profile")
    suspend fun completeProfile(@Body body: CompleteProfileRequest): Any

    // Google mobile auth
    @POST("api/auth/google/mobile")
    suspend fun googleMobileAuth(@Body body: GoogleMobileAuthRequest): GoogleMobileAuthResponse

    // Translation (Gemini-powered)
    @POST("api/translate")
    suspend fun translate(@Body body: TranslateRequest): TranslateResponse

    // --- Artist flow ---
    @GET("api/artists/{uuid}/identifiers")
    suspend fun getArtistIdentifiers(@Path("uuid") uuid: String): ArtistIdentifiersResponse

    @POST("api/artist-signup")
    suspend fun artistSignup(@Body body: ArtistSignupRequest): ArtistSignupResponse

    @POST("api/community")
    suspend fun createCommunity(@Body body: CreateCommunityRequest): Any

    @GET("api/artist/me")
    suspend fun getArtistMe(): ArtistMeResponse

    @PUT("api/artist/me/edit")
    suspend fun editArtistMe(@Body body: ArtistEditRequest): Any

    // Returns song/album reviews that mention this (onboarded) artist, as SongReview rows.
    @GET("api/reviews/by-artist")
    suspend fun getReviewsByArtist(@Query("artistId") artistId: String): ReviewsFeedResponse

    @POST("api/auth/switch-role")
    suspend fun switchRole(@Body body: SwitchRoleRequest): SwitchRoleResponse

    @POST("api/upload")
    suspend fun getUploadUrl(@Body body: UploadUrlRequest): UploadUrlResponse

    // --- Community forums ---
    @GET("api/community/{slug}")
    suspend fun getCommunityBySlug(@Path("slug") slug: String): CommunitySlugResponse

    @GET("api/communities/{communityId}/forums")
    suspend fun getCommunityForums(@Path("communityId") communityId: String): CommunityForumsResponse

    @GET("api/forums/{forumId}/messages")
    suspend fun getForumMessages(@Path("forumId") forumId: String, @Query("limit") limit: Int = 50): ForumMessagesResponse

    @POST("api/forums/{forumId}/messages/{postId}/react")
    suspend fun reactToMessage(@Path("forumId") forumId: String, @Path("postId") postId: String, @Body body: Map<String, String>): ReactionResponse

    @GET("api/forums/{forumId}/fan-art")
    suspend fun getFanArt(@Path("forumId") forumId: String, @Query("limit") limit: Int = 20, @Query("offset") offset: Int = 0): FanArtResponse

    @POST("api/forums/{forumId}/fan-art")
    suspend fun createFanArt(@Path("forumId") forumId: String, @Body body: FanArtCreateRequest): Any

    @POST("api/forum-posts/{postId}/like")
    suspend fun likeFanArt(@Path("postId") postId: String): Any

    @GET("api/users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): UserByIdResponse

    @GET("api/community/posts")
    suspend fun getCommunityPosts(@Query("communityId") communityId: String): List<CommunityPost>

    @POST("api/community/posts")
    suspend fun createCommunityPost(@Body body: CommunityPostCreateRequest): Any

    // Forum post like / comment (web: /api/like, /api/posts/comment)
    @POST("api/like")
    suspend fun likePost(@Body body: Map<String, String>): Any

    @retrofit2.http.HTTP(method = "DELETE", path = "api/like", hasBody = true)
    suspend fun unlikePost(@Body body: Map<String, String>): Any

    @POST("api/posts/comment")
    suspend fun commentOnPost(@Body body: Map<String, String>): Any

    // Community subscriber count
    @GET("api/communities/{communityId}/subscribers")
    suspend fun getSubscriberCount(@Path("communityId") communityId: String): SubscriberCountResponse

    // Fan-art message reaction (toggle emoji)
    @POST("api/forums/{forumId}/messages/{postId}/react")
    suspend fun reactToFanArt(@Path("forumId") forumId: String, @Path("postId") postId: String, @Body body: Map<String, String>): ReactionResponse
}

// --- Cookie-based session persistence ---

class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("soundspire_cookies", Context.MODE_PRIVATE)
    private val cookies = mutableListOf<Cookie>()

    init {
        val stored = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        stored.forEach { raw -> deserialize(raw)?.let { cookies.add(it) } }
        // Drop anything already expired at startup.
        cookies.removeAll { it.expiresAt < System.currentTimeMillis() }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.removeAll { existing ->
            cookies.any { it.name == existing.name && it.domain == existing.domain }
        }
        this.cookies.addAll(cookies)
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val expired = cookies.any { it.expiresAt < now }
        cookies.removeAll { it.expiresAt < now }
        if (expired) persist()
        return cookies.filter { it.matches(url) }
    }

    fun clear() {
        cookies.clear()
        prefs.edit().clear().apply()
    }

    private fun persist() {
        prefs.edit().putStringSet("cookies", cookies.map { serialize(it) }.toSet()).apply()
    }

    // Serialize ALL fields (incl. expiry, host-only flag) so the cookie round-trips exactly.
    // The previous "name=value; Domain=...; Path=..." form re-parsed against a dummy host,
    // which OkHttp rejected on a domain mismatch — silently dropping every cookie on restart
    // (the "logged out on app close" bug) — and also lost the 7-day expiry.
    private fun serialize(c: Cookie): String = listOf(
        c.name, c.value, c.expiresAt.toString(), c.domain, c.path,
        c.secure.toString(), c.httpOnly.toString(), c.hostOnly.toString()
    ).joinToString(SEP)

    private fun deserialize(s: String): Cookie? {
        val p = s.split(SEP)
        if (p.size < 8) return null // old/broken format → ignore
        return try {
            val builder = Cookie.Builder()
                .name(p[0])
                .value(p[1])
                .path(p[4])
                .expiresAt(p[2].toLongOrNull() ?: return null)
            if (p[7].toBoolean()) builder.hostOnlyDomain(p[3]) else builder.domain(p[3])
            if (p[5].toBoolean()) builder.secure()
            if (p[6].toBoolean()) builder.httpOnly()
            builder.build()
        } catch (_: Exception) { null }
    }

    private companion object {
        // Unit Separator — won't appear in cookie names/values (JWTs are base64url + dots).
        const val SEP = "␟"
    }
}

// --- API Client Builder ---

object ApiClient {
    private var service: SoundSpireService? = null
    private var cookieJar: PersistentCookieJar? = null

    fun getService(context: Context): SoundSpireService {
        if (service == null) {
            cookieJar = PersistentCookieJar(context)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar!!)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val baseUrl = BuildConfig.SOUNDSPIRE_API_BASE_URL.let {
                if (it.endsWith("/")) it else "$it/"
            }

            service = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SoundSpireService::class.java)
        }
        return service!!
    }

    fun getCookieJar(context: Context): PersistentCookieJar {
        getService(context) // ensure initialized
        return cookieJar!!
    }

    fun clearSession(context: Context) {
        cookieJar?.clear()
    }
}
