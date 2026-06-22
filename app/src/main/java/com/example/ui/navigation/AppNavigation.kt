package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ArtistDashboardScreen
import com.example.ui.screens.ArtistVoteScreen
import com.example.ui.screens.ReviewDetailScreen
import com.example.ui.screens.CommunitiesScreen
import com.example.ui.screens.CommunityDetailScreen
import com.example.ui.screens.CompleteProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PreferenceSelectionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistCatalogScreen
import com.example.ui.screens.ReviewsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.artist.ArtistDetailsScreen
import com.example.ui.screens.artist.ArtistLoginScreen
import com.example.ui.screens.artist.ArtistOnboardingScreen
import com.example.ui.screens.artist.FindArtistProfileScreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.viewmodel.AuthViewModel

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val COMPLETE_PROFILE = "complete_profile"
    const val PREFERENCES = "preferences"
    const val EXPLORE = "explore"
    const val FEED = "feed"
    const val COMMUNITIES = "communities"
    const val REVIEWS = "reviews"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val ARTIST_DASHBOARD = "artist_dashboard"
    const val REVIEW_DETAIL = "review_detail"
    const val ARTIST_VOTE = "artist_vote"
    const val ARTIST_ONBOARDING = "artist_onboarding"
    const val ARTIST_LOGIN = "artist_login"
    const val FIND_ARTIST = "find_artist"
    const val ARTIST_DETAILS = "artist_details"
    const val COMMUNITY_DETAIL = "community_detail"
    const val SETTINGS = "settings"
    const val ARTIST_CATALOG = "artist_catalog"
    const val ALBUM_DETAIL = "album_detail"
}

val bottomNavRoutes = listOf(
    Routes.EXPLORE,
    Routes.FEED,
    Routes.COMMUNITIES,
    Routes.REVIEWS,
    Routes.NOTIFICATIONS,
    Routes.PROFILE,
)

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentOrange)
        }
        return
    }

    val needsCompleteProfile by authViewModel.needsCompleteProfile.collectAsState()
    val needsPreferences by authViewModel.needsPreferences.collectAsState()

    val isArtistAccount by authViewModel.isArtistAccount.collectAsState()
    val startRoute = when {
        !isLoggedIn -> Routes.LOGIN
        isArtistAccount -> Routes.ARTIST_DASHBOARD
        needsCompleteProfile -> Routes.COMPLETE_PROFILE
        needsPreferences -> Routes.PREFERENCES
        else -> Routes.EXPLORE
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        containerColor = BackgroundDarkPurple,
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                    onLoginSuccess = {
                        val dest = when {
                            authViewModel.isArtistAccount.value -> Routes.ARTIST_DASHBOARD
                            authViewModel.needsCompleteProfile.value -> Routes.COMPLETE_PROFILE
                            authViewModel.needsPreferences.value -> Routes.PREFERENCES
                            else -> Routes.EXPLORE
                        }
                        navController.navigate(dest) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onArtistEntry = { navController.navigate(Routes.ARTIST_ONBOARDING) }
                )
            }
            composable(Routes.SIGNUP) {
                SignupScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() },
                    onSignupSuccess = {
                        // After signup, go back to login (user needs to verify email first)
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.COMPLETE_PROFILE) {
                CompleteProfileScreen(
                    authViewModel = authViewModel,
                    onComplete = {
                        // Skip preferences if the user already has them (e.g. artist-turned-fan).
                        val dest = if (authViewModel.needsPreferences.value) Routes.PREFERENCES else Routes.EXPLORE
                        navController.navigate(dest) {
                            popUpTo(Routes.COMPLETE_PROFILE) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.PREFERENCES) {
                PreferenceSelectionScreen(
                    onComplete = {
                        authViewModel.markPreferencesComplete()
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(Routes.PREFERENCES) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onSeeAllReviews = {
                        navController.navigate(Routes.REVIEWS) {
                            popUpTo(Routes.EXPLORE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSeeMoreArtists = {
                        navController.navigate(Routes.COMMUNITIES) {
                            popUpTo(Routes.EXPLORE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReviewClick = { trackId -> navController.navigate("${Routes.REVIEW_DETAIL}/$trackId") },
                    onArtistVoteClick = { uuid -> navController.navigate("${Routes.ARTIST_VOTE}/$uuid") },
                    onArtistCommunityClick = { slug -> navController.navigate("${Routes.COMMUNITY_DETAIL}/$slug/about") }
                )
            }
            composable(Routes.FEED) {
                FeedScreen()
            }
            composable(Routes.COMMUNITIES) {
                CommunitiesScreen(
                    onCommunityClick = { slug -> navController.navigate("${Routes.COMMUNITY_DETAIL}/$slug/about") }
                )
            }
            composable(Routes.REVIEWS) {
                ReviewsScreen(
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onReviewClick = { trackId -> navController.navigate("${Routes.REVIEW_DETAIL}/$trackId") }
                )
            }
            composable("${Routes.REVIEW_DETAIL}/{trackId}") { backStackEntry ->
                val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
                ReviewDetailScreen(
                    trackId = trackId,
                    onBack = { navController.popBackStack() },
                    onArtistClick = { spotifyId, name ->
                        val encoded = java.net.URLEncoder.encode(name, "UTF-8")
                            .replace("+", "%20").replace("%2F", "-").replace("%2f", "-")
                        navController.navigate("${Routes.ARTIST_CATALOG}/$spotifyId/$encoded")
                    }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onNotificationClick = { link ->
                        // Map web links to in-app destinations
                        when {
                            link.contains("/reviews/song/") -> {
                                val trackId = link.substringAfter("/reviews/song/").substringBefore("?").substringBefore("/")
                                if (trackId.isNotBlank()) navController.navigate("${Routes.REVIEW_DETAIL}/$trackId")
                            }
                            // /community/{slug}/{tab}?highlight=... → community detail with the right tab
                            link.contains("/community/") -> {
                                val rest = link.substringAfter("/community/").substringBefore("?")
                                val slug = rest.substringBefore("/")
                                val tab = if (rest.contains("/")) rest.substringAfter("/") else "forum"
                                if (slug.isNotBlank()) navController.navigate("${Routes.COMMUNITY_DETAIL}/$slug/$tab")
                            }
                            link.startsWith("/feed") -> {
                                navController.navigate(Routes.FEED) {
                                    popUpTo(Routes.EXPLORE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            else -> {}
                        }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSwitchToArtist = {
                        // If already an artist, go straight to dashboard (switching role); else onboard
                        if (authViewModel.currentUser.value?.isAlsoArtist == true) {
                            authViewModel.switchRole("artist") {
                                navController.navigate(Routes.ARTIST_DASHBOARD)
                            }
                        } else {
                            navController.navigate(Routes.ARTIST_ONBOARDING)
                        }
                    },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onTrackClick = { trackId -> navController.navigate("${Routes.REVIEW_DETAIL}/$trackId") },
                    onArtistClick = { spotifyId, name ->
                        // Encode for a single path segment: URLEncoder uses '+' for spaces and
                        // %2F for slashes, both of which break nav path matching — normalize them.
                        val encoded = java.net.URLEncoder.encode(name, "UTF-8")
                            .replace("+", "%20").replace("%2F", "-").replace("%2f", "-")
                        navController.navigate("${Routes.ARTIST_CATALOG}/$spotifyId/$encoded")
                    },
                    onAlbumClick = { albumId -> navController.navigate("${Routes.ALBUM_DETAIL}/$albumId") }
                )
            }
            composable("${Routes.ALBUM_DETAIL}/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                AlbumDetailScreen(
                    albumId = albumId,
                    onBack = { navController.popBackStack() },
                    onTrackClick = { trackId -> navController.navigate("${Routes.REVIEW_DETAIL}/$trackId") },
                    onArtistClick = { spotifyId, name ->
                        val encoded = java.net.URLEncoder.encode(name, "UTF-8")
                            .replace("+", "%20").replace("%2F", "-").replace("%2f", "-")
                        navController.navigate("${Routes.ARTIST_CATALOG}/$spotifyId/$encoded")
                    }
                )
            }
            composable("${Routes.ARTIST_CATALOG}/{spotifyId}/{name}") { backStackEntry ->
                val spotifyId = backStackEntry.arguments?.getString("spotifyId") ?: ""
                val name = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
                ArtistCatalogScreen(
                    spotifyId = spotifyId,
                    name = name,
                    onBack = { navController.popBackStack() },
                    onTrackClick = { trackId -> navController.navigate("${Routes.REVIEW_DETAIL}/$trackId") },
                    onCommunityClick = { slug -> navController.navigate("${Routes.COMMUNITY_DETAIL}/$slug/about") },
                    onVoteClick = { uuid -> navController.navigate("${Routes.ARTIST_VOTE}/$uuid") },
                )
            }
            composable("${Routes.ARTIST_VOTE}/{uuid}") { backStackEntry ->
                val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                ArtistVoteScreen(uuid = uuid, onBack = { navController.popBackStack() })
            }
            composable("${Routes.COMMUNITY_DETAIL}/{slug}/{tab}") { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: ""
                val tab = backStackEntry.arguments?.getString("tab") ?: "forum"
                CommunityDetailScreen(
                    slug = slug,
                    initialTab = tab,
                    onBack = { navController.popBackStack() },
                    onReviewClick = { reviewTrackId ->
                        // album reviews are keyed "album:{id}" → open album page, else song page.
                        if (reviewTrackId.startsWith("album:")) navController.navigate("${Routes.ALBUM_DETAIL}/${reviewTrackId.removePrefix("album:")}")
                        else navController.navigate("${Routes.REVIEW_DETAIL}/$reviewTrackId")
                    }
                )
            }
            composable(Routes.ARTIST_DASHBOARD) {
                ArtistDashboardScreen(
                    onSwitchToFan = {
                        authViewModel.switchRole("user") {
                            // Route based on fan-onboarding state, mirroring the website's guard:
                            // an artist switching to fan for the first time completes their profile
                            // (gender + DOB) and preferences before reaching Explore.
                            val dest = when {
                                authViewModel.needsCompleteProfile.value -> Routes.COMPLETE_PROFILE
                                authViewModel.needsPreferences.value -> Routes.PREFERENCES
                                else -> Routes.EXPLORE
                            }
                            navController.navigate(dest) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onLogout = {
                        authViewModel.logout {
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    }
                )
            }
            composable(Routes.ARTIST_ONBOARDING) {
                ArtistOnboardingScreen(
                    onSelectArtist = { navController.navigate(Routes.FIND_ARTIST) },
                    onArtistLogin = { navController.navigate(Routes.ARTIST_LOGIN) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ARTIST_LOGIN) {
                ArtistLoginScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Routes.ARTIST_DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.FIND_ARTIST) {
                FindArtistProfileScreen(
                    onBack = { navController.popBackStack() },
                    onArtistSelected = { uuid -> navController.navigate("${Routes.ARTIST_DETAILS}/$uuid") }
                )
            }
            composable("${Routes.ARTIST_DETAILS}/{artistId}") { backStackEntry ->
                val aid = backStackEntry.arguments?.getString("artistId") ?: ""
                ArtistDetailsScreen(
                    artistId = aid,
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        navController.navigate(Routes.ARTIST_LOGIN) {
                            popUpTo(Routes.ARTIST_ONBOARDING) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
