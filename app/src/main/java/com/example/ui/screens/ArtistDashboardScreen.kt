package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.ArtistEditRequest
import com.example.data.remote.ArtistMe
import com.example.data.remote.ArtistSocial
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.screens.artist.AllChatScreen
import com.example.ui.screens.artist.ArtistForumScreen
import com.example.ui.screens.artist.FanArtScreen
import com.example.util.S3Uploader
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ArtistOrange = Color(0xFFFA6400)
private val DashBg = Color(0xFF1A0A2E)

@Composable
fun ArtistDashboardScreen(
    onSwitchToFan: () -> Unit,
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var artist by remember { mutableStateOf<ArtistMe?>(null) }
    var reviews by remember { mutableStateOf<List<com.example.data.remote.SongReview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf(false) }
    var editBio by remember { mutableStateOf("") }
    var editSocials by remember { mutableStateOf<List<ArtistSocial>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var imgBust by remember { mutableStateOf(0L) }

    // Tabs: about / forum / all-chat / fan-art
    var tab by remember { mutableStateOf("about") }
    var chatForumId by remember { mutableStateOf<String?>(null) }
    var fanArtForumId by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var subscriberCount by remember { mutableStateOf(0) }
    var notifications by remember { mutableStateOf<List<com.example.data.remote.NotificationItem>>(emptyList()) }
    var showNotifPanel by remember { mutableStateOf(false) }

    fun reload() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val me = api.getArtistMe()
                artist = me.artist
                me.artist?.artist_id?.let { id ->
                    try { reviews = api.getReviewsByArtist(id).reviews } catch (_: Exception) { }
                }
                try { currentUserId = api.getSession().user?.id } catch (_: Exception) { }
                try { notifications = api.getNotifications().notifications } catch (_: Exception) { }
                // Resolve forum ids for All Chat & Fan Art
                me.artist?.community?.community_id?.let { cid ->
                    try { subscriberCount = api.getSubscriberCount(cid).count } catch (_: Exception) { }
                    try {
                        val forums = api.getCommunityForums(cid).forums
                        chatForumId = forums.firstOrNull { (it.name ?: "").contains("chat", true) || (it.forum_type ?: "").contains("chat", true) }?.forum_id ?: forums.firstOrNull()?.forum_id
                        fanArtForumId = forums.firstOrNull { (it.name ?: "").contains("art", true) || (it.forum_type ?: "").contains("art", true) }?.forum_id
                    } catch (e: Exception) { android.util.Log.e("ArtistDash", "forums failed", e) }
                }
            } catch (e: Exception) { android.util.Log.e("ArtistDash", "getArtistMe failed", e) }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && artist != null) uploadImage(context, uri, "profile", artist!!) { reload(); imgBust = System.nanoTime() }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && artist != null) uploadImage(context, uri, "cover", artist!!) { reload(); imgBust = System.nanoTime() }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize().background(DashBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(32.dp))
        }
        return
    }

    val a = artist
    if (a == null) {
        Column(modifier = Modifier.fillMaxSize().background(DashBg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("No artist data found.", color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onSwitchToFan, colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange)) { Text("Switch to Fan", color = Color.White) }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DashBg, Color(0xFF2D1B4E), Color(0xFF0A0612))))) {
        // Persistent header bar
        Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Artist Studio", color = ArtistOrange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Notification bell
                val unread = notifications.count { !it.is_read }
                Box {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp).clickable {
                            showNotifPanel = !showNotifPanel
                            // Mark all read on open (matches website)
                            if (showNotifPanel && unread > 0) {
                                notifications = notifications.map { it.copy(is_read = true) }
                                CoroutineScope(Dispatchers.IO).launch {
                                    try { api.markNotificationsRead(mapOf("notificationIds" to "all")) } catch (_: Exception) { }
                                }
                            }
                        }
                    )
                    if (unread > 0) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(ArtistOrange), contentAlignment = Alignment.Center) {
                            Text(if (unread > 9) "9+" else "$unread", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Button(onClick = onSwitchToFan, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))) { Text("View as Fan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = onLogout, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange)) { Text("Logout", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }

        // Tab bar
        Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f)).padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("about" to "Home", "forum" to "Forum", "all-chat" to "All Chat", "fan-art" to "Fan Art").forEach { (key, label) ->
                Text(
                    label,
                    color = if (tab == key) ArtistOrange else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (tab == key) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { tab = key }.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        // Notification panel
        if (showNotifPanel) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF221C2F)).padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Notifications", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("✕", color = TextMuted, fontSize = 14.sp, modifier = Modifier.clickable { showNotifPanel = false })
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (notifications.isEmpty()) {
                    Text("No notifications", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                } else {
                    notifications.take(20).forEach { n ->
                        Column(modifier = Modifier.fillMaxWidth().clickable {
                            showNotifPanel = false
                            val link = n.link ?: ""
                            tab = when {
                                link.contains("/all-chat") -> "all-chat"
                                link.contains("/fan-art") -> "fan-art"
                                link.contains("/forum") -> "forum"
                                else -> "forum"
                            }
                        }.padding(vertical = 8.dp)) {
                            Text(n.message, color = TextWhite, fontSize = 13.sp)
                            if (n.created_at != null) Text(n.created_at!!.take(10), color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        when (tab) {
            "forum" -> {
                val cid = a.community?.community_id
                if (cid != null) ArtistForumScreen(communityId = cid, artistId = a.artist_id, isArtist = true, currentUserId = currentUserId)
                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No community found.", color = TextMuted) }
                return@Column
            }
            "all-chat" -> {
                val fid = chatForumId
                if (fid != null) AllChatScreen(
                    forumId = fid,
                    currentUserId = currentUserId,
                    communityId = a.community?.community_id,
                    communityName = a.community?.name,
                    artistName = a.artist_name,
                    currentUserName = a.artist_name,
                    memberCount = subscriberCount,
                )
                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(24.dp)) }
                return@Column
            }
            "fan-art" -> {
                val fid = fanArtForumId
                if (fid != null) FanArtScreen(forumId = fid, currentUserId = currentUserId)
                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArtistOrange, modifier = Modifier.size(24.dp)) }
                return@Column
            }
        }

        // About tab content
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Cover + profile
        item {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).clickable { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                AsyncImage(
                    model = resolveImageUrl(a.cover_photo_url)?.let { "$it?v=$imgBust" } ?: defaultProfileImageUrl(),
                    contentDescription = "Cover", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 0.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(110.dp).clip(CircleShape).border(4.dp, DashBg, CircleShape).clickable { profilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    AsyncImage(
                        model = resolveImageUrl(a.profile_picture_url)?.let { "$it?v=$imgBust" } ?: defaultProfileImageUrl(),
                        contentDescription = a.artist_name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.DarkGray)
                    )
                }
            }
        }

        // Name + community
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(a.artist_name ?: "Artist", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                a.community?.name?.let { Text(it, color = TextMuted, fontSize = 13.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                if (!editing) {
                    OutlinedButton(onClick = { editBio = a.bio ?: ""; editSocials = a.socials.map { it.copy() }; editing = true }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Edit, null, tint = TextWhite, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Edit Profile", color = TextWhite, fontSize = 12.sp)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = false }, shape = RoundedCornerShape(8.dp)) { Text("Cancel", color = TextWhite, fontSize = 12.sp) }
                        Button(
                            onClick = {
                                saving = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        api.editArtistMe(ArtistEditRequest(bio = editBio, socials = editSocials.filter { it.url.isNotBlank() }))
                                        editing = false
                                        reload()
                                    } catch (_: Exception) { }
                                    saving = false
                                }
                            },
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange), shape = RoundedCornerShape(8.dp)
                        ) { Text(if (saving) "Saving..." else "Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // Community info block (matches website: #ArtistName / N members / Guidelines)
        item {
            DashCard(a.community?.name ?: "${a.artist_name} Community") {
                Text("#${a.artist_name}", color = ArtistOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("$subscriberCount ${if (subscriberCount == 1) "member" else "members"}", color = TextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Guidelines for ${a.artist_name} Community", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (!a.community?.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(a.community!!.description!!, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }

        // About
        item {
            DashCard("About") {
                if (editing) {
                    OutlinedTextField(value = editBio, onValueChange = { editBio = it }, modifier = Modifier.fillMaxWidth().height(120.dp), colors = dashFieldColors())
                } else if (!a.bio.isNullOrBlank()) {
                    Text(a.bio!!, color = Color(0xFFD1D5DB), fontSize = 14.sp, lineHeight = 20.sp)
                } else {
                    Text("No bio yet. Click Edit Profile to add one.", color = TextMuted, fontSize = 13.sp)
                }
            }
        }

        // Socials
        item {
            DashCard("Social Links") {
                if (editing) {
                    Column {
                        editSocials.forEachIndexed { i, s ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(s.platform.replaceFirstChar { it.uppercase() }, color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(76.dp))
                                OutlinedTextField(value = s.url, onValueChange = { v -> editSocials = editSocials.toMutableList().also { it[i] = s.copy(url = v) } }, placeholder = { Text("https://...", color = TextMuted) }, modifier = Modifier.weight(1f), singleLine = true, colors = dashFieldColors())
                                IconButton(onClick = { editSocials = editSocials.filterIndexed { j, _ -> j != i } }) { Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editSocials = editSocials + ArtistSocial("instagram", "") }.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Add, null, tint = ArtistOrange, modifier = Modifier.size(16.dp)); Text("Add Link", color = ArtistOrange, fontSize = 13.sp)
                        }
                    }
                } else if (a.socials.isNotEmpty()) {
                    Column {
                        a.socials.forEach { s ->
                            Text("${s.platform.replaceFirstChar { it.uppercase() }}: ${s.url}", color = Color(0xFFD1D5DB), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                } else {
                    Text("No social links. Click Edit Profile to add some.", color = TextMuted, fontSize = 13.sp)
                }
            }
        }

        // Reviews
        item {
            DashCard("Reviews") {
                if (reviews.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reviews.forEach { r ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(r.song?.track_name ?: "Review", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (!r.review_text.isNullOrBlank()) Text(r.review_text!!.take(150), color = Color(0xFFD1D5DB), fontSize = 12.sp)
                                Text("@${r.user?.username ?: "user"}", color = ArtistOrange, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Text("No reviews yet. Reviews mentioning you will appear here.", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
    } // outer Column
}

@Composable
private fun DashCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF221C2F)).padding(16.dp)) {
        Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun dashFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DashBg, unfocusedContainerColor = DashBg,
    focusedBorderColor = ArtistOrange, unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
)

private fun uploadImage(context: android.content.Context, uri: Uri, type: String, artist: ArtistMe, onDone: () -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val fileName = "images/artists/${artist.artist_id}-$type-${System.nanoTime()}.jpg"
        val s3Path = S3Uploader.upload(context, uri, fileName) ?: return@launch
        try {
            val api = ApiClient.getService(context)
            api.editArtistMe(if (type == "profile") ArtistEditRequest(profile_picture_url = s3Path) else ArtistEditRequest(cover_photo_url = s3Path))
            onDone()
        } catch (_: Exception) { }
    }
}
