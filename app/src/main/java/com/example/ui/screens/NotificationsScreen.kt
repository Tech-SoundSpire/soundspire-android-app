package com.example.ui.screens

import com.example.ui.components.TText

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.NotificationItem
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.SubheadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.defaultProfileImageUrl
import com.example.util.resolveImageUrl

@Composable
fun NotificationsScreen(onNotificationClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }

    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = api.getNotifications()
            notifications = response.notifications
            // Mark all as read
            api.markNotificationsRead(mapOf("notificationIds" to "all"))
        } catch (_: Exception) { }
        loading = false
    }

    val grouped = groupNotifications(notifications)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        item {
            TText("NOTIFICATIONS", color = HeadingPeach, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(32.dp))
                }
            }
        } else if (notifications.isEmpty()) {
            item {
                TText("No notifications", color = TextMuted, fontSize = 16.sp, modifier = Modifier.padding(24.dp))
            }
        } else {
            grouped.forEach { (label, items) ->
                if (items.isNotEmpty()) {
                    item {
                        TText(label, color = SubheadingPeach, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    items(items) { notification ->
                        NotificationRow(notification, onNotificationClick)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationItem, onNotificationClick: (String) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (notification.link != null) Modifier.clickable { onNotificationClick(notification.link!!) } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = resolveImageUrl(notification.actor_image) ?: defaultProfileImageUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(12.dp))
        TText(
            text = notification.message,
            color = TextWhite,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        TText(
            text = timeAgo(notification.created_at),
            color = TextMuted,
            fontSize = 12.sp,
        )
        if (notification.thumbnail != null) {
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = resolveImageUrl(notification.thumbnail),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun groupNotifications(notifications: List<NotificationItem>): List<Pair<String, List<NotificationItem>>> {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    val weekMs = 7 * dayMs

    val today = mutableListOf<NotificationItem>()
    val week = mutableListOf<NotificationItem>()
    val earlier = mutableListOf<NotificationItem>()

    notifications.forEach { n ->
        val ts = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(n.created_at ?: "")?.time ?: 0L
        } catch (_: Exception) { 0L }
        val diff = now - ts
        when {
            diff < dayMs -> today.add(n)
            diff < weekMs -> week.add(n)
            else -> earlier.add(n)
        }
    }

    return listOf("Today" to today, "This Week" to week, "Earlier" to earlier)
}

private fun timeAgo(dateStr: String?): String {
    if (dateStr == null) return ""
    val ts = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(dateStr)?.time ?: return ""
    } catch (_: Exception) { return "" }
    val diff = System.currentTimeMillis() - ts
    val mins = diff / 60000
    if (mins < 1) return "now"
    if (mins < 60) return "${mins}m"
    val hrs = mins / 60
    if (hrs < 24) return "${hrs}h"
    return "${hrs / 24}d"
}
