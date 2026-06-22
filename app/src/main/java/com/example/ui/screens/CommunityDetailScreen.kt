package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.CommunitySlugArtist
import com.example.data.remote.SubscribeRequest
import com.example.ui.screens.artist.AllChatScreen
import com.example.ui.screens.artist.ArtistForumScreen
import com.example.ui.screens.artist.FanArtScreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ArtistOrange = Color(0xFFFA6400)
private val Bg = Color(0xFF1A0A2E)

/**
 * Fan-facing community detail screen. Mirrors the website's /community/[slug] pages
 * with Forum / All Chat / Fan Art tabs. `initialTab` deep-links from notifications
 * (e.g. an all-chat reaction notification opens the All Chat tab).
 */
@Composable
fun CommunityDetailScreen(
    slug: String,
    initialTab: String = "about",
    onBack: () -> Unit,
    onReviewClick: (trackId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var artist by remember { mutableStateOf<CommunitySlugArtist?>(null) }
    var reviews by remember { mutableStateOf<List<com.example.data.remote.SongReview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var chatForumId by remember { mutableStateOf<String?>(null) }
    var fanArtForumId by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var currentUserName by remember { mutableStateOf<String?>(null) }
    var subscriberCount by remember { mutableStateOf(0) }
    // Deep-links from notifications can request a content tab directly; only honored if subscribed.
    var tab by remember { mutableStateOf(initialTab) }
    var isSubscribed by remember { mutableStateOf(false) }
    var isOwnCommunity by remember { mutableStateOf(false) }
    var subBusy by remember { mutableStateOf(false) }
    var gateMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(slug) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val resp = api.getCommunityBySlug(slug)
                artist = resp.artist
                val sessionUser = try { api.getSession().user } catch (_: Exception) { null }
                currentUserId = sessionUser?.id; currentUserName = sessionUser?.name
                // An artist viewing their own community (in fan view) shouldn't see Subscribe.
                isOwnCommunity = sessionUser?.artistId != null && sessionUser.artistId == resp.artist?.artist_id
                resp.artist?.artist_id?.let { aid ->
                    try { reviews = api.getReviewsByArtist(aid).reviews } catch (_: Exception) {}
                }
                resp.artist?.community?.community_id?.let { cid ->
                    try { subscriberCount = api.getSubscriberCount(cid).count } catch (_: Exception) {}
                    if (currentUserId != null) {
                        try { isSubscribed = api.getSubscriptionStatus(currentUserId!!, cid).subscribed } catch (_: Exception) {}
                    }
                    try {
                        val forums = api.getCommunityForums(cid).forums
                        chatForumId = forums.firstOrNull { (it.name ?: "").contains("chat", true) || (it.forum_type ?: "").contains("chat", true) }?.forum_id ?: forums.firstOrNull()?.forum_id
                        fanArtForumId = forums.firstOrNull { (it.name ?: "").contains("art", true) || (it.forum_type ?: "").contains("art", true) }?.forum_id
                    } catch (_: Exception) {}
                }
                // If deep-linked to a gated tab but not allowed, fall back to About.
                if (tab != "about" && !(isSubscribed || isOwnCommunity)) tab = "about"
            } catch (e: Exception) { android.util.Log.e("CommunityDetail", "load failed", e) }
            loading = false
        }
    }

    val canAccess = isSubscribed || isOwnCommunity

    fun toggleSubscribe() {
        val uid = currentUserId ?: return
        val cid = artist?.community?.community_id ?: return
        if (subBusy) return
        subBusy = true
        val wasSubscribed = isSubscribed
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (wasSubscribed) {
                    api.unsubscribeFromCommunity(uid, cid)
                    isSubscribed = false
                    subscriberCount = (subscriberCount - 1).coerceAtLeast(0)
                    if (tab != "about") tab = "about"
                } else {
                    // Build ISO-8601 timestamps (start now, end +1 month), matching the web.
                    val now = java.time.OffsetDateTime.now()
                    val nowIso = now.toString()
                    val endIso = now.plusMonths(1).toString()
                    api.subscribeToCommunity(SubscribeRequest(
                        user_id = uid, community_id = cid,
                        start_date = nowIso, end_date = endIso,
                        created_at = nowIso, updated_at = nowIso,
                    ))
                    isSubscribed = true
                    subscriberCount += 1
                }
            } catch (_: Exception) { }
            subBusy = false
        }
    }

    fun selectTab(key: String) {
        if (key != "about" && !canAccess) {
            gateMessage = "Subscribe to access ${if (key == "all-chat") "All Chat" else key.replaceFirstChar { it.uppercase() }}"
            return
        }
        gateMessage = null
        tab = key
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = resolveImageUrl(artist?.profile_picture_url) ?: defaultProfileImageUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(artist?.community?.name ?: artist?.artist_name ?: "Community", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("$subscriberCount ${if (subscriberCount == 1) "member" else "members"}", color = TextMuted, fontSize = 11.sp)
            }
            // Subscribe / Unsubscribe (hidden on the artist's own community)
            if (!loading && !isOwnCommunity && artist?.community?.community_id != null) {
                Button(
                    onClick = { toggleSubscribe() },
                    enabled = !subBusy,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSubscribed) Color(0xFF44404F) else ArtistOrange)
                ) {
                    if (subBusy) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(if (isSubscribed) "Subscribed" else "Subscribe", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tab bar — content tabs show a lock when not subscribed
        Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f)).padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("about" to "About", "forum" to "Forum", "all-chat" to "All Chat", "fan-art" to "Fan Art").forEach { (key, label) ->
                val gated = key != "about" && !canAccess
                Text(
                    if (gated) "🔒 $label" else label,
                    color = if (tab == key) ArtistOrange else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (tab == key) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { selectTab(key) }.padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }

        if (gateMessage != null) {
            Text(
                gateMessage!!,
                color = ArtistOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().background(Color(0x33FA6400)).padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(28.dp))
            }
            return@Column
        }

        val cid = artist?.community?.community_id
        // Guard: never render gated content without access (covers deep-link + unsubscribe-while-viewing)
        val effectiveTab = if (tab != "about" && !canAccess) "about" else tab
        when (effectiveTab) {
            "about" -> CommunityAboutTab(artist, reviews, isSubscribed, isOwnCommunity, subBusy, api, onReviewClick, onToggleSubscribe = { toggleSubscribe() })
            "forum" -> {
                if (cid != null) ArtistForumScreen(communityId = cid, artistId = artist?.artist_id ?: "", isArtist = false, currentUserId = currentUserId)
                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No forum found.", color = TextMuted) }
            }
            "all-chat" -> {
                val fid = chatForumId
                if (fid != null) AllChatScreen(
                    forumId = fid,
                    currentUserId = currentUserId,
                    communityId = cid,
                    communityName = artist?.community?.name,
                    artistName = artist?.artist_name,
                    currentUserName = currentUserName,
                    memberCount = subscriberCount,
                )
                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(24.dp)) }
            }
            "fan-art" -> {
                val fid = fanArtForumId
                if (fid != null) FanArtScreen(forumId = fid, currentUserId = currentUserId)
                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun CommunityAboutTab(
    artist: CommunitySlugArtist?,
    reviews: List<com.example.data.remote.SongReview>,
    isSubscribed: Boolean,
    isOwnCommunity: Boolean,
    subBusy: Boolean,
    api: com.example.data.remote.SoundSpireService,
    onReviewClick: (trackId: String) -> Unit,
    onToggleSubscribe: () -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Cover + profile photo
        item {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(
                    model = resolveImageUrl(artist?.cover_photo_url) ?: defaultProfileImageUrl(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Color.DarkGray)
                )
                AsyncImage(
                    model = resolveImageUrl(artist?.profile_picture_url) ?: defaultProfileImageUrl(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.align(Alignment.BottomCenter).size(88.dp).clip(CircleShape).background(Color.DarkGray)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(artist?.artist_name ?: "Artist", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (artist?.community?.name != null) {
                Text(artist.community!!.name!!, color = TextMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Subscribe banner (not on own community)
        if (!isOwnCommunity) {
            item {
                Button(
                    onClick = onToggleSubscribe,
                    enabled = !subBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSubscribed) Color(0xFF44404F) else ArtistOrange),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    if (subBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(
                        if (isSubscribed) "Unsubscribe from ${artist?.community?.name ?: "${artist?.artist_name}'s community"}"
                        else "Subscribe to ${artist?.community?.name ?: "${artist?.artist_name}'s community"}",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // About
        item {
            SectionCard("About") {
                Text(
                    if (!artist?.bio.isNullOrBlank()) artist!!.bio!! else "No bio available yet.",
                    color = if (!artist?.bio.isNullOrBlank()) Color(0xFFD1D5DB) else TextMuted,
                    fontSize = 14.sp, lineHeight = 20.sp
                )
            }
        }

        // Community Highlights (static, mirrors web)
        if (artist?.community != null) {
            item {
                SectionCard("Community Highlights") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Be a part of the TRIBE", "Get Access to the Screens", "Tap into the Global Community").forEach { h ->
                            Box(
                                modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2D1B4E)).padding(12.dp),
                                contentAlignment = Alignment.BottomStart
                            ) { Text(h, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }

        // Reviews — song/album reviews that mention this artist (tap to open the full review).
        item {
            SectionCard("Reviews") {
                if (reviews.isEmpty()) {
                    Text("No reviews yet.", color = TextMuted, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        reviews.forEach { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1625))
                                    .clickable { onReviewClick(r.spotify_track_id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AsyncImage(
                                    model = resolveImageUrl(r.song?.album_art_url) ?: defaultProfileImageUrl(),
                                    contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.song?.track_name ?: "Review", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("@${r.user?.username ?: "user"}", color = AccentOrange.copy(alpha = 0.85f), fontSize = 11.sp)
                                        if (r.rating != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row { repeat(5) { i -> Icon(Icons.Default.Star, null, tint = if (i < r.rating!!.toInt()) AccentOrange else TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(11.dp)) } }
                                        }
                                    }
                                    if (!r.review_text.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(if (r.review_text!!.length > 150) r.review_text!!.take(150) + "..." else r.review_text!!, color = Color(0xFFD1D5DB), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF221C2F)).padding(16.dp)) {
        Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
