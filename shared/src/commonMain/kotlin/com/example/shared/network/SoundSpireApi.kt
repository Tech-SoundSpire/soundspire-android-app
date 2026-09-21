package com.example.shared.network

import com.example.shared.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

/**
 * KMP port of the app's SoundSpireService (Retrofit) onto the Ktor client.
 * Plain request functions (Ktorfit deferred - see docs/ios Phase 1 note).
 * Endpoints returning `Any` in the app (fire-and-forget) return Unit here.
 * Endpoints returning dynamic maps return JsonObject.
 */
class SoundSpireApi(private val client: HttpClient) {

    // --- Auth ---
    suspend fun login(request: LoginRequest): LoginResponse =
        client.post("api/users/login") { contentType(ContentType.Application.Json); setBody(request) }.body()

    suspend fun signup(request: SignupRequest): SignupResponse =
        client.post("api/users/signup") { contentType(ContentType.Application.Json); setBody(request) }.body()

    suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse =
        client.post("api/users/forgot-password") { contentType(ContentType.Application.Json); setBody(request) }.body()

    suspend fun getAppVersion(): AppVersionResponse =
        client.get("api/app-version").body()

    suspend fun searchCities(q: String, limit: Int = 10): CitySearchResponse =
        client.get("api/cities") { parameter("q", q); parameter("limit", limit) }.body()

    suspend fun getSession(): SessionResponse =
        client.get("api/auth/session").body()

    suspend fun logout() {
        client.post("api/auth/logout")
    }

    suspend fun deleteAccount() {
        client.delete("api/users/delete-account")
    }

    // --- Profile ---
    suspend fun getProfile(email: String): ProfileResponse =
        client.get("api/profile") { parameter("email", email) }.body()

    suspend fun updateProfile(request: ProfileUpdateRequest) {
        client.put("api/profile") { contentType(ContentType.Application.Json); setBody(request) }
    }

    suspend fun checkUsername(username: String): JsonObject =
        client.get("api/check-username") { parameter("username", username) }.body()

    // --- Preferences ---
    suspend fun checkPreferences(userId: String): PreferencesCheckResponse =
        client.get("api/preferences/check") { parameter("userId", userId) }.body()

    suspend fun savePreferences(body: SavePreferencesRequest) {
        client.post("api/preferences/save") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun getAvailableGenres(): AvailableGenresResponse =
        client.get("api/preferences/available/genres").body()

    suspend fun getAvailableArtists(): AvailableArtistsResponse =
        client.get("api/preferences/available/artists").body()

    suspend fun getAvailableLanguages(): AvailableLanguagesResponse =
        client.get("api/preferences/available/languages").body()

    suspend fun searchArtistsSoundcharts(query: String): SoundchartsArtistsResponse =
        client.get("api/artists") { parameter("q", query) }.body()

    suspend fun createList(body: CreateListRequest) {
        client.post("api/catalog/lists") { contentType(ContentType.Application.Json); setBody(body) }
    }

    // --- Artist details (SoundCharts) ---
    suspend fun getArtistByUuid(uuid: String): JsonObject =
        client.get("api/artists/$uuid").body()

    // --- Artist voting ---
    suspend fun getArtistVote(uuid: String, userId: String? = null): ArtistVoteResponse =
        client.get("api/artist-vote") { parameter("soundcharts_uuid", uuid); parameter("userId", userId) }.body()

    suspend fun castArtistVote(body: ArtistVoteRequest): ArtistVoteResponse =
        client.post("api/artist-vote") { contentType(ContentType.Application.Json); setBody(body) }.body()

    // --- Explore ---
    suspend fun getExploreArtists(q: String? = null): List<ExploreArtist> =
        client.get("api/explore/artists") { parameter("q", q) }.body()

    suspend fun getSuggestedArtists(userId: String): SuggestedArtistsResponse =
        client.get("api/explore/suggested") { parameter("userId", userId) }.body()

    suspend fun getGenres(): List<GenreItem> =
        client.get("api/explore/genres").body()

    suspend fun getGenreArtists(genreId: String): GenreArtistsResponse =
        client.get("api/explore/genres/$genreId/artists").body()

    suspend fun getReviewsFeed(page: Int = 1): ReviewsFeedResponse =
        client.get("api/catalog/song-reviews/feed") { parameter("page", page) }.body()

    suspend fun search(query: String): SearchResponse =
        client.get("api/search") { parameter("search", query) }.body()

    suspend fun getTrackMetadata(trackId: String): TrackMetadata =
        client.get("api/catalog/track/$trackId").body()

    suspend fun getAlbum(albumId: String): AlbumMetadata =
        client.get("api/catalog/album/$albumId").body()

    suspend fun cacheAlbum(body: CacheAlbumRequest) {
        client.post("api/catalog/cache-album") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun getPosts(userId: String): List<CommunityPost> =
        client.get("api/community/posts") { parameter("userId", userId) }.body()

    // --- Communities ---
    suspend fun getSubscriptions(userId: String): SubscriptionsResponse =
        client.get("api/community/subscribe") { parameter("user_id", userId) }.body()

    suspend fun getSubscriptionStatus(userId: String, communityId: String): SubscriptionStatusResponse =
        client.get("api/community/subscribe") { parameter("user_id", userId); parameter("community_id", communityId) }.body()

    suspend fun subscribeToCommunity(body: SubscribeRequest) {
        client.post("api/community/subscribe") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun unsubscribeFromCommunity(userId: String, communityId: String) {
        client.delete("api/community/subscribe") { parameter("user_id", userId); parameter("community_id", communityId) }
    }

    suspend fun searchCommunities(query: String): SubscriptionsResponse =
        client.get("api/communities/search") { parameter("search", query) }.body()

    // --- Notifications ---
    suspend fun getNotifications(): NotificationsResponse =
        client.get("api/notifications").body()

    suspend fun markNotificationsRead(body: Map<String, String>) {
        client.patch("api/notifications") { contentType(ContentType.Application.Json); setBody(body) }
    }

    // --- Reviews ---
    suspend fun likeReview(reviewId: String) {
        client.post("api/catalog/song-reviews/$reviewId/like")
    }

    suspend fun unlikeReview(reviewId: String) {
        client.delete("api/catalog/song-reviews/$reviewId/like")
    }

    suspend fun getReviewComments(reviewId: String): ReviewCommentsResponse =
        client.get("api/catalog/song-reviews/$reviewId/comments").body()

    suspend fun commentOnReview(reviewId: String, body: ReviewCommentRequest) {
        client.post("api/catalog/song-reviews/$reviewId/comments") { contentType(ContentType.Application.Json); setBody(body) }
    }

    // --- Catalog search (Spotify) ---
    suspend fun searchCatalog(query: String, type: String = "track,artist,album", limit: Int = 10): CatalogSearchResponse =
        client.get("api/catalog/search") { parameter("q", query); parameter("type", type); parameter("limit", limit) }.body()

    suspend fun getCatalogArtist(id: String, name: String): CatalogArtistDetail =
        client.get("api/catalog/artist/$id") { parameter("name", name) }.body()

    suspend fun getCatalogArtistAlbums(id: String, name: String, limit: Int = 20): CatalogArtistAlbumsResponse =
        client.get("api/catalog/artist/$id/albums") { parameter("name", name); parameter("limit", limit) }.body()

    suspend fun resolveSoundchartsUuid(spotifyId: String, name: String): ResolveArtistResponse =
        client.get("api/artists/resolve") { parameter("spotifyId", spotifyId); parameter("name", name) }.body()

    // --- Track + ratings for review detail ---
    suspend fun getTrackReviews(trackId: String, sort: String = "popular"): ReviewsFeedResponse =
        client.get("api/catalog/song-reviews/track/$trackId") { parameter("sort", sort) }.body()

    suspend fun getTrackRatings(trackId: String): TrackRatingResponse =
        client.get("api/catalog/ratings/track/$trackId").body()

    suspend fun submitReview(body: SubmitReviewRequest) {
        client.post("api/catalog/song-reviews") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun submitRating(body: SubmitRatingRequest) {
        client.post("api/catalog/ratings") { contentType(ContentType.Application.Json); setBody(body) }
    }

    // --- Lists & Journal ---
    suspend fun getMyLists(): ListsResponse =
        client.get("api/catalog/lists/mine").body()

    suspend fun getDiary(limit: Int = 50): DiaryResponse =
        client.get("api/catalog/diary") { parameter("limit", limit) }.body()

    suspend fun getListItems(listId: String): ListItemsResponse =
        client.get("api/catalog/lists/$listId").body()

    suspend fun addToList(listId: String, body: Map<String, String>) {
        client.post("api/catalog/lists/$listId/items") { contentType(ContentType.Application.Json); setBody(body) }
    }

    // --- Complete profile (onboarding) ---
    suspend fun completeProfile(body: CompleteProfileRequest) {
        client.post("api/users/complete-profile") { contentType(ContentType.Application.Json); setBody(body) }
    }

    // --- Google mobile auth ---
    suspend fun googleMobileAuth(body: GoogleMobileAuthRequest): GoogleMobileAuthResponse =
        client.post("api/auth/google/mobile") { contentType(ContentType.Application.Json); setBody(body) }.body()

    // --- Translation ---
    suspend fun translate(body: TranslateRequest): TranslateResponse =
        client.post("api/translate") { contentType(ContentType.Application.Json); setBody(body) }.body()

    // --- Artist flow ---
    suspend fun getArtistIdentifiers(uuid: String): ArtistIdentifiersResponse =
        client.get("api/artists/$uuid/identifiers").body()

    suspend fun artistSignup(body: ArtistSignupRequest): ArtistSignupResponse =
        client.post("api/artist-signup") { contentType(ContentType.Application.Json); setBody(body) }.body()

    suspend fun createCommunity(body: CreateCommunityRequest) {
        client.post("api/community") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun getArtistMe(): ArtistMeResponse =
        client.get("api/artist/me").body()

    suspend fun editArtistMe(body: ArtistEditRequest) {
        client.put("api/artist/me/edit") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun getReviewsByArtist(artistId: String): ReviewsFeedResponse =
        client.get("api/reviews/by-artist") { parameter("artistId", artistId) }.body()

    suspend fun switchRole(body: SwitchRoleRequest): SwitchRoleResponse =
        client.post("api/auth/switch-role") { contentType(ContentType.Application.Json); setBody(body) }.body()

    suspend fun getUploadUrl(body: UploadUrlRequest): UploadUrlResponse =
        client.post("api/upload") { contentType(ContentType.Application.Json); setBody(body) }.body()

    // --- Community forums ---
    suspend fun getCommunityBySlug(slug: String): CommunitySlugResponse =
        client.get("api/community/$slug").body()

    suspend fun getCommunityForums(communityId: String): CommunityForumsResponse =
        client.get("api/communities/$communityId/forums").body()

    suspend fun getForumMessages(forumId: String, limit: Int = 50): ForumMessagesResponse =
        client.get("api/forums/$forumId/messages") { parameter("limit", limit) }.body()

    suspend fun reactToMessage(forumId: String, postId: String, body: Map<String, String>): ReactionResponse =
        client.post("api/forums/$forumId/messages/$postId/react") { contentType(ContentType.Application.Json); setBody(body) }.body()

    suspend fun postForumMessage(forumId: String, body: PostMessageRequest) {
        client.post("api/forums/$forumId/messages") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun editForumMessage(forumId: String, postId: String, body: EditMessageRequest) {
        client.patch("api/forums/$forumId/messages/$postId") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun deleteForumMessage(forumId: String, postId: String) {
        client.delete("api/forums/$forumId/messages/$postId")
    }

    // --- Moderation ---
    suspend fun submitReport(body: ReportRequest) {
        client.post("api/reports") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun blockUser(body: BlockRequest) {
        client.post("api/blocks") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun unblockUser(blockedUserId: String) {
        client.delete("api/blocks/$blockedUserId")
    }

    suspend fun getBlocks(): BlocksResponse =
        client.get("api/blocks").body()

    // --- Admin moderation ---
    suspend fun adminReports(status: String? = null, targetType: String? = null): AdminReportsResponse =
        client.get("api/admin/reports") { parameter("status", status); parameter("target_type", targetType) }.body()

    suspend fun adminHideContent(body: HideContentRequest) {
        client.post("api/admin/hide-content") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun adminBanUser(body: BanUserRequest) {
        client.post("api/admin/ban-user") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun adminUnbanUser(body: BanUserRequest) {
        client.post("api/admin/unban-user") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun adminDismissReport(reportId: String) {
        client.post("api/admin/reports/$reportId/dismiss")
    }

    suspend fun adminSearchUsers(q: String = ""): AdminUsersResponse =
        client.get("api/admin/users") { parameter("q", q) }.body()

    suspend fun adminActions(): AdminActionsResponse =
        client.get("api/admin/actions").body()

    // --- Fan art ---
    suspend fun getFanArt(forumId: String, limit: Int = 20, offset: Int = 0): FanArtResponse =
        client.get("api/forums/$forumId/fan-art") { parameter("limit", limit); parameter("offset", offset) }.body()

    suspend fun createFanArt(forumId: String, body: FanArtCreateRequest) {
        client.post("api/forums/$forumId/fan-art") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun likeFanArt(postId: String) {
        client.post("api/forum-posts/$postId/like")
    }

    // --- Users / community posts ---
    suspend fun getUserById(userId: String): UserByIdResponse =
        client.get("api/users/$userId").body()

    suspend fun getCommunityPosts(communityId: String): List<CommunityPost> =
        client.get("api/community/posts") { parameter("communityId", communityId) }.body()

    suspend fun createCommunityPost(body: CommunityPostCreateRequest) {
        client.post("api/community/posts") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun likePost(body: Map<String, String>) {
        client.post("api/like") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun unlikePost(body: Map<String, String>) {
        client.delete("api/like") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun commentOnPost(body: Map<String, String>) {
        client.post("api/posts/comment") { contentType(ContentType.Application.Json); setBody(body) }
    }

    suspend fun getSubscriberCount(communityId: String): SubscriberCountResponse =
        client.get("api/communities/$communityId/subscribers").body()

    suspend fun reactToFanArt(forumId: String, postId: String, body: Map<String, String>): ReactionResponse =
        client.post("api/forums/$forumId/messages/$postId/react") { contentType(ContentType.Application.Json); setBody(body) }.body()
}
