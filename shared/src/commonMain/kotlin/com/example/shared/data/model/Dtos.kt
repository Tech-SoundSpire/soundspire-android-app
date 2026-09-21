package com.example.shared.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Ported from app SoundSpireApi.kt DTOs. No Moshi annotations were used
// (property names match JSON), so kotlinx.serialization needs only @Serializable.
// Optional fields keep their defaults so an absent/null field never fails the parse.

@Serializable
data class LoginRequest(val email: String, val password_hash: String)
@Serializable
data class SignupRequest(val username: String, val email: String, val password_hash: String)
@Serializable
data class LoginResponse(val message: String, val redirect: String? = null)
@Serializable
data class SignupResponse(val message: String? = null, val success: Boolean = false, val error: String? = null)
@Serializable
data class ForgotPasswordRequest(val email: String)
@Serializable
data class ForgotPasswordResponse(val message: String? = null)
@Serializable
data class AppVersionResponse(
    val latestVersionCode: Int = 0,
    val versionName: String? = null,
    val downloadUrl: String? = null,
    val mandatory: Boolean = false,
    val message: String? = null,
)
@Serializable
data class CityResult(
    val city: String,
    val country: String? = null,
    val dialCode: String? = null,
    val phoneLen: Int = 10,
)
@Serializable
data class CitySearchResponse(val cities: List<CityResult> = emptyList())

@Serializable
data class SessionResponse(val user: SessionUser? = null)
@Serializable
data class SessionUser(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val photoURL: String? = null,
    val provider: String? = null,
    val role: String? = null,
    val isAlsoArtist: Boolean = false,
    val artistId: String? = null,
    val isAdmin: Boolean = false,
)

@Serializable
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

@Serializable
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

@Serializable
data class PreferencesCheckResponse(val hasPreferences: Boolean = false)
@Serializable
data class AvailableGenresResponse(val genres: List<GenreItem> = emptyList())
@Serializable
data class AvailableArtistsResponse(val artists: List<ExploreArtist> = emptyList())
@Serializable
data class LanguageItem(val language_id: String, val name: String)
@Serializable
data class AvailableLanguagesResponse(val languages: List<LanguageItem> = emptyList())
@Serializable
data class SoundchartsArtistItem(val uuid: String? = null, val name: String? = null, val imageUrl: String? = null)
@Serializable
data class SoundchartsArtistsResponse(val items: List<SoundchartsArtistItem> = emptyList())
@Serializable
data class CreateListRequest(val title: String, val description: String? = null)
@Serializable
data class SavePreferencesRequest(
    val userId: String,
    val genres: List<String>,
    val languages: List<String> = emptyList(),
    val favoriteArtists: List<FavoriteArtistPref> = emptyList(),
)
@Serializable
data class FavoriteArtistPref(val name: String, val soundcharts_uuid: String? = null, val imageUrl: String? = null)
@Serializable
data class ArtistVoteRequest(val soundcharts_uuid: String, val artist_name: String, val image_url: String? = null, val userId: String)
@Serializable
data class ArtistVoteResponse(val count: Int = 0, val userVoted: Boolean = false, val alreadyVoted: Boolean = false)

@Serializable
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

@Serializable
data class GenreItem(val genre_id: String, val name: String)
@Serializable
data class GenreArtistItem(
    val artist_id: String,
    val name: String? = null,
    val imageUrl: String? = null,
    val slug: String? = null,
    val onSoundSpire: Boolean = false,
    val soundcharts_uuid: String? = null,
    val subscriberCount: Int = 0,
)
@Serializable
data class GenreArtistsResponse(val genre: GenreItem? = null, val artists: List<GenreArtistItem> = emptyList())

@Serializable
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
@Serializable
data class ReviewComment(
    val comment_id: String? = null,
    val comment_text: String? = null,
    val username: String? = null,
    val profile_picture_url: String? = null,
    val created_at: String? = null,
)
@Serializable
data class ReviewCommentsResponse(val comments: List<ReviewComment> = emptyList())
@Serializable
data class ReviewCommentRequest(val comment_text: String)

@Serializable
data class ReviewUser(
    val user_id: String? = null,
    val username: String? = null,
    val profile_picture_url: String? = null,
)

@Serializable
data class ReviewSong(
    val track_name: String? = null,
    val artist_name: String? = null,
    val album_art_url: String? = null,
)

@Serializable
data class ReviewsFeedResponse(val reviews: List<SongReview> = emptyList())

@Serializable
data class CommunitySubscription(
    val name: String? = null,
    val artist_slug: String? = null,
    val artist_name: String? = null,
    val artist_profile_picture_url: String? = null,
    val artist_cover_photo_url: String? = null,
    val subscriber_count: Int = 0,
)

@Serializable
data class SubscriptionsResponse(
    val communities: List<CommunitySubscription> = emptyList(),
    val user: SubscriptionUser? = null,
)

@Serializable
data class SubscriptionUser(val profile_picture_url: String? = null)

@Serializable
data class PostItem(
    val post_id: String,
    val content_text: String? = null,
    val media_urls: List<String>? = null,
    val created_at: String? = null,
    val likes: List<JsonElement> = emptyList(),
    val comments: List<JsonElement> = emptyList(),
    val artist: PostArtist? = null,
)

@Serializable
data class PostArtist(
    val artist_name: String? = null,
    val profile_picture_url: String? = null,
    val slug: String? = null,
)

@Serializable
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

@Serializable
data class NotificationsResponse(val notifications: List<NotificationItem> = emptyList(), val unreadCount: Int = 0)

@Serializable
data class SearchResult(
    val type: String? = null,
    val id: String? = null,
    val name: String? = null,
    val image: String? = null,
    val slug: String? = null,
)

@Serializable
data class SearchResponse(
    val artists: List<SearchArtistResult> = emptyList(),
    val reviews: List<SearchReviewResult> = emptyList(),
    val communities: List<SearchCommunityResult> = emptyList(),
    val songs: List<SearchSongResult> = emptyList(),
    val users: List<SearchUserResult> = emptyList(),
)

@Serializable
data class SearchArtistResult(val artist_name: String? = null, val slug: String? = null, val profile_picture_url: String? = null)
@Serializable
data class SearchReviewResult(val review_id: String? = null, val title: String? = null, val spotify_track_id: String? = null, val rating: Double? = null)
@Serializable
data class SearchCommunityResult(val name: String? = null, val artist_slug: String? = null, val profile_picture_url: String? = null)
@Serializable
data class SearchSongResult(val spotify_track_id: String? = null, val track_name: String? = null, val artist_name: String? = null, val album_art_url: String? = null)
@Serializable
data class SearchUserResult(val user_id: String? = null, val username: String? = null, val full_name: String? = null, val profile_picture_url: String? = null)

@Serializable
data class SuggestedArtistsResponse(val artists: List<ExploreArtist> = emptyList())

// Catalog search (Spotify — paginated wrapper)
@Serializable
data class CatalogSearchResponse(
    val tracks: SpotifyPaginatedList<CatalogTrack>? = null,
    val artists: SpotifyPaginatedList<CatalogArtist>? = null,
    val albums: SpotifyPaginatedList<CatalogAlbum>? = null,
)
@Serializable
data class SpotifyPaginatedList<T>(val items: List<T>? = null, val total: Int? = null)
@Serializable
data class CatalogTrack(val id: String, val name: String, val duration_ms: Int? = null, val album: CatalogAlbum? = null, val artists: List<CatalogArtist>? = null)
@Serializable
data class CatalogAlbum(val id: String? = null, val name: String? = null, val images: List<CatalogImage>? = null, val artists: List<CatalogArtist>? = null)
@Serializable
data class CatalogArtist(val id: String? = null, val name: String? = null, val images: List<CatalogImage>? = null)
@Serializable
data class CatalogImage(val url: String? = null)

// Artist catalog page DTOs (match /api/catalog/artist/{id} and /albums)
@Serializable
data class CatalogArtistDetail(
    val id: String? = null,
    val name: String? = null,
    val images: List<CatalogImage>? = null,
    val genres: List<String>? = null,
    val spotify_url: String? = null,
    val top_tracks: List<CatalogTopTrack>? = null,
)
@Serializable
data class CatalogTopTrack(
    val id: String? = null,
    val name: String? = null,
    val album_name: String? = null,
    val album_art: String? = null,
    val duration_ms: Int? = null,
    val explicit: Boolean = false,
)
@Serializable
data class CatalogArtistAlbum(
    val id: String? = null,
    val name: String? = null,
    val total_tracks: Int? = null,
    val release_date: String? = null,
    val images: List<CatalogImage>? = null,
)
@Serializable
data class CatalogArtistAlbumsResponse(val albums: List<CatalogArtistAlbum> = emptyList(), val total: Int = 0)
@Serializable
data class ResolveArtistResponse(val soundchartsUuid: String? = null)

// Track metadata (from Spotify cache)
@Serializable
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
@Serializable
data class TrackArtist(val id: String? = null, val name: String? = null)
@Serializable
data class TrackCredit(val name: String? = null, val role: String? = null)

// Album detail (mirrors /api/catalog/album/{id})
@Serializable
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
@Serializable
data class AlbumTrack(
    val id: String? = null,
    val name: String? = null,
    val track_number: Int? = null,
    val duration_ms: Int? = null,
    val explicit: Boolean? = null,
    val artists: List<TrackArtist>? = null,
)
@Serializable
data class CacheAlbumRequest(
    val spotify_track_id: String,
    val track_name: String,
    val artist_name: String? = null,
    val artist_id: String? = null,
    val album_art_url: String? = null,
)

// Track ratings
@Serializable
data class TrackRatingResponse(
    val avg_rating: Double? = null,
    val rating_count: Int = 0,
    val review_count: Int = 0,
    val user_rating: Double? = null,
)

// Submit review/rating
@Serializable
data class SubmitReviewRequest(val spotify_track_id: String, val review_text: String, val rating: Double? = null)
@Serializable
data class SubmitRatingRequest(val spotify_track_id: String, val rating: Double)

// Lists & Journal
@Serializable
data class ListItem(val list_id: String, val title: String, val description: String? = null, val is_ranked: Boolean = false, val like_count: Int = 0, val created_at: String? = null)
@Serializable
data class ListsResponse(val lists: List<ListItem> = emptyList())
@Serializable
data class ListDetailItem(val item_id: String? = null, val spotify_track_id: String? = null, val position: Int? = null, val notes: String? = null, val song: ListDetailSong? = null)
@Serializable
data class ListDetailSong(val track_name: String? = null, val artist_name: String? = null, val album_art_url: String? = null, val duration_ms: Int? = null)
@Serializable
data class ListItemsResponse(val list: JsonElement? = null, val items: List<ListDetailItem> = emptyList())
@Serializable
data class DiaryEntry(val entry_id: String, val spotify_track_id: String, val listened_date: String? = null, val rating: Double? = null, val liked: Boolean = false, val notes: String? = null)
@Serializable
data class DiaryResponse(val entries: List<DiaryEntry> = emptyList())

// Complete profile
@Serializable
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
@Serializable
data class TranslateRequest(val texts: List<String>, val targetLang: String)
@Serializable
data class TranslateResponse(val translations: List<String> = emptyList())

// Google mobile auth
@Serializable
data class GoogleMobileAuthRequest(val idToken: String)
@Serializable
data class GoogleMobileAuthResponse(
    val success: Boolean = false,
    val error: String? = null,
    val user: GoogleMobileUser? = null,
)
@Serializable
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
@Serializable
data class ArtistIdentifier(val platformName: String? = null, val platform: String? = null, val url: String? = null)
@Serializable
data class ArtistIdentifiersResponse(val items: List<ArtistIdentifier> = emptyList())

@Serializable
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
@Serializable
data class ArtistSocial(val platform: String, val url: String)
@Serializable
data class ArtistSignupResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val artist: ArtistSignupArtist? = null,
    val requiresVerification: Boolean = false,
)
@Serializable
data class ArtistSignupArtist(val artist_id: String? = null, val artist_name: String? = null, val slug: String? = null)

@Serializable
data class CreateCommunityRequest(val artist_id: String, val name: String, val description: String)

@Serializable
data class ArtistMeResponse(val artist: ArtistMe? = null, val error: String? = null)
@Serializable
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
@Serializable
data class CommunityHighlight(val imageUrl: String? = null, val text: String = "")

@Serializable
data class ArtistCommunity(
    val community_id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val highlights: List<CommunityHighlight> = emptyList(),
)

@Serializable
data class ArtistEditRequest(
    val bio: String? = null,
    val profile_picture_url: String? = null,
    val cover_photo_url: String? = null,
    val socials: List<ArtistSocial>? = null,
    val highlights: List<CommunityHighlight>? = null,
)

@Serializable
data class ArtistReview(
    val review_id: String,
    val title: String? = null,
    val text_content: String? = null,
    val author: String? = null,
    val review_date: String? = null,
    val created_at: String? = null,
    val image_urls: List<String>? = null,
)
@Serializable
data class ArtistReviewsResponse(val reviews: List<ArtistReview> = emptyList())

@Serializable
data class SwitchRoleRequest(val role: String)
@Serializable
data class SwitchRoleResponse(val success: Boolean = false, val role: String? = null)

@Serializable
data class UploadUrlRequest(val fileName: String, val fileType: String)
@Serializable
data class UploadUrlResponse(val uploadUrl: String? = null)

// --- Community / forum DTOs ---
@Serializable
data class CommunityForum(val forum_id: String, val community_id: String? = null, val name: String? = null, val forum_type: String? = null)
@Serializable
data class CommunityForumsResponse(val forums: List<CommunityForum> = emptyList())

@Serializable
data class ForumUser(val user_id: String? = null, val username: String? = null, val full_name: String? = null, val profile_picture_url: String? = null)

// Moderation request/response bodies.
@Serializable
data class ReportRequest(val target_type: String, val target_id: String, val reason: String, val details: String? = null)
@Serializable
data class BlockRequest(val blocked_user_id: String)
@Serializable
data class BlockedUser(val user_id: String? = null, val username: String? = null, val full_name: String? = null, val profile_picture_url: String? = null)
@Serializable
data class BlockRow(val block_id: String, val blocked_user_id: String, val blockedUser: BlockedUser? = null)
@Serializable
data class BlocksResponse(val blocks: List<BlockRow> = emptyList())

// Admin moderation DTOs.
@Serializable
data class AdminReporter(val user_id: String? = null, val username: String? = null, val full_name: String? = null)
@Serializable
data class AdminReport(
    val report_id: String,
    val reporter_user_id: String? = null,
    val target_type: String,
    val target_id: String,
    val reason: String,
    val details: String? = null,
    val status: String,
    val created_at: String? = null,
    val reporter: AdminReporter? = null,
)
@Serializable
data class AdminReportsResponse(val reports: List<AdminReport> = emptyList())
@Serializable
data class HideContentRequest(val target_type: String, val target_id: String, val hidden_reason: String? = null)
@Serializable
data class BanUserRequest(val user_id: String)
@Serializable
data class AdminUserRow(
    val user_id: String,
    val username: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val is_banned: Boolean = false,
    val is_admin: Boolean = false,
)
@Serializable
data class AdminUsersResponse(val users: List<AdminUserRow> = emptyList())
@Serializable
data class AdminActionRow(
    val action_id: String,
    val action: String,
    val target_type: String,
    val target_id: String,
    val note: String? = null,
    val created_at: String? = null,
    val moderator_username: String? = null,
)
@Serializable
data class AdminActionsResponse(val actions: List<AdminActionRow> = emptyList())
@Serializable
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
@Serializable
data class ForumMessagesResponse(val messages: List<ForumMessage> = emptyList(), val hasMore: Boolean = false)

// Body for authed message create; user_id is derived from the JWT server-side.
@Serializable
data class PostMessageRequest(
    val content: String,
    val media_type: String = "text",
    val media_urls: List<String> = emptyList(),
    val parent_post_id: String? = null,
)
@Serializable
data class EditMessageRequest(val content: String)

@Serializable
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
@Serializable
data class FanArtComment(
    val forum_post_id: String,
    val user_id: String? = null,
    val parent_post_id: String? = null,
    val content: String? = null,
    val created_at: String? = null,
    val user: ForumUser? = null,
    val reactions: Map<String, List<String>>? = null,
)
@Serializable
data class FanArtResponse(val posts: List<FanArtPost> = emptyList(), val total: Int = 0, val hasMore: Boolean = false)
@Serializable
data class FanArtCreateRequest(val title: String? = null, val content: String? = null, val imageUrls: List<String>)

@Serializable
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
@Serializable
data class PostLike(val like_id: String? = null, val user_id: String? = null)
@Serializable
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
@Serializable
data class CommunityPostCreateRequest(val artist_id: String, val community_id: String, val content_text: String, val media_urls: List<String> = emptyList())

@Serializable
data class UserByIdResponse(val user: ForumUser? = null)
@Serializable
data class SubscriberCountResponse(val count: Int = 0)
@Serializable
data class SubscriptionStatusResponse(val subscribed: Boolean = false)
@Serializable
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
@Serializable
data class ReactionResponse(val reactions: Map<String, List<String>>? = null)

@Serializable
data class CommunitySlugResponse(val artist: CommunitySlugArtist? = null)
@Serializable
data class CommunitySlugArtist(
    val artist_id: String? = null,
    val artist_name: String? = null,
    val profile_picture_url: String? = null,
    val cover_photo_url: String? = null,
    val bio: String? = null,
    val socials: List<ArtistSocial> = emptyList(),
    val community: ArtistCommunity? = null,
)
