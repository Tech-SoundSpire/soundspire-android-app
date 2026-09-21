package com.example.shared

import com.example.shared.i18n.DynamicTranslator
import com.example.shared.i18n.StaticTranslations
import com.example.shared.network.IosCookieStore
import com.example.shared.network.MediaUploader
import com.example.shared.network.SoundSpireApi
import com.example.shared.network.createHttpClient
import com.example.shared.viewmodel.AlbumDetailViewModel
import com.example.shared.viewmodel.AllChatViewModel
import com.example.shared.viewmodel.ArtistCatalogViewModel
import com.example.shared.viewmodel.ArtistDashboardViewModel
import com.example.shared.viewmodel.ArtistDetailsViewModel
import com.example.shared.viewmodel.ArtistVoteViewModel
import com.example.shared.viewmodel.AuthViewModel
import com.example.shared.viewmodel.ExploreViewModel
import com.example.shared.viewmodel.GenreArtistsViewModel
import com.example.shared.viewmodel.CommunitiesViewModel
import com.example.shared.viewmodel.CommunityDetailViewModel
import com.example.shared.viewmodel.CompleteProfileViewModel
import com.example.shared.viewmodel.FanArtViewModel
import com.example.shared.viewmodel.FeedViewModel
import com.example.shared.viewmodel.ForumViewModel
import com.example.shared.viewmodel.ModerationViewModel
import com.example.shared.viewmodel.NotificationsViewModel
import com.example.shared.viewmodel.PreferenceViewModel
import com.example.shared.viewmodel.ProfileViewModel
import com.example.shared.viewmodel.SettingsViewModel
import com.example.shared.viewmodel.ReviewDetailViewModel
import com.example.shared.viewmodel.ReviewsViewModel
import com.example.shared.viewmodel.SearchViewModel

/**
 * iOS composition root. Wires the persistent cookie store + Ktor client so SwiftUI can
 * grab ready-made view models without touching Ktor.
 */
object IosDependencies {
    private val cookieStore = IosCookieStore()
    val api: SoundSpireApi by lazy { SoundSpireApi(createHttpClient(cookieStore)) }
    val mediaUploader: MediaUploader by lazy { MediaUploader(api) }
    val dynamicTranslator: DynamicTranslator by lazy { DynamicTranslator(api) }

    // Synchronous static-dictionary lookup for the SwiftUI TText component.
    fun staticTranslate(text: String, lang: String): String = StaticTranslations.translate(text, lang)

    // Google sign-in client IDs (from .env). iOS client for the native SDK; server client
    // (the web OAuth client) so the minted ID token is verifiable by the backend.
    val googleIosClientId: String get() = SharedConfig.GOOGLE_IOS_CLIENT_ID
    val googleServerClientId: String get() = SharedConfig.GOOGLE_OAUTH_CLIENT_ID

    fun authViewModel(): AuthViewModel = AuthViewModel(api, cookieStore)
    fun exploreViewModel(): ExploreViewModel = ExploreViewModel(api)
    fun genreArtistsViewModel(genreId: String): GenreArtistsViewModel = GenreArtistsViewModel(api, genreId)
    fun reviewsViewModel(): ReviewsViewModel = ReviewsViewModel(api)
    fun reviewDetailViewModel(trackId: String): ReviewDetailViewModel = ReviewDetailViewModel(api, trackId)
    fun communitiesViewModel(): CommunitiesViewModel = CommunitiesViewModel(api)
    fun communityDetailViewModel(slug: String): CommunityDetailViewModel = CommunityDetailViewModel(api, slug)
    fun searchViewModel(): SearchViewModel = SearchViewModel(api)
    fun allChatViewModel(forumId: String, currentUserId: String?, communityId: String?, currentUserName: String?): AllChatViewModel =
        AllChatViewModel(api, forumId, currentUserId, communityId, currentUserName)
    fun fanArtViewModel(forumId: String, currentUserId: String?): FanArtViewModel = FanArtViewModel(api, forumId, currentUserId)
    // Suggestions reuses the All Chat VM with presence off.
    fun suggestionsViewModel(forumId: String, currentUserId: String?, communityId: String?, currentUserName: String?): AllChatViewModel =
        AllChatViewModel(api, forumId, currentUserId, communityId, currentUserName, enablePresence = false)
    fun preferenceViewModel(): PreferenceViewModel = PreferenceViewModel(api)
    fun completeProfileViewModel(): CompleteProfileViewModel = CompleteProfileViewModel(api)
    fun feedViewModel(): FeedViewModel = FeedViewModel(api)
    fun notificationsViewModel(): NotificationsViewModel = NotificationsViewModel(api)
    fun profileViewModel(): ProfileViewModel = ProfileViewModel(api)
    fun settingsViewModel(): SettingsViewModel = SettingsViewModel(api, SharedConfig.API_BASE_URL)
    fun albumDetailViewModel(albumId: String): AlbumDetailViewModel = AlbumDetailViewModel(api, albumId)
    fun artistCatalogViewModel(spotifyId: String, name: String): ArtistCatalogViewModel = ArtistCatalogViewModel(api, spotifyId, name)
    fun artistVoteViewModel(uuid: String): ArtistVoteViewModel = ArtistVoteViewModel(api, uuid)
    fun moderationViewModel(): ModerationViewModel = ModerationViewModel(api)
    fun artistDashboardViewModel(): ArtistDashboardViewModel = ArtistDashboardViewModel(api)
    fun artistDetailsViewModel(soundchartsUuid: String): ArtistDetailsViewModel = ArtistDetailsViewModel(api, soundchartsUuid)
    fun forumViewModel(communityId: String, artistId: String?, currentUserId: String?): ForumViewModel = ForumViewModel(api, communityId, artistId, currentUserId)
}
