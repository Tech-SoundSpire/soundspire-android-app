package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.AdminActionRow
import com.example.data.remote.AdminReport
import com.example.data.remote.AdminUserRow
import com.example.data.remote.ApiClient
import com.example.data.remote.BanUserRequest
import com.example.data.remote.HideContentRequest
import com.example.ui.components.TText
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// In-app moderation dashboard (admin only; server enforces is_admin). Mirrors the
// web /admin/moderation: Reports / Users / Audit tabs.
@Composable
fun ModerationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }
    var tab by remember { mutableStateOf("reports") }
    var forbidden by remember { mutableStateOf(false) }

    // Reports
    var reports by remember { mutableStateOf<List<AdminReport>>(emptyList()) }
    var statusFilter by remember { mutableStateOf("open") }
    // Users
    var userQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<AdminUserRow>>(emptyList()) }
    // Audit
    var actions by remember { mutableStateOf<List<AdminActionRow>>(emptyList()) }

    suspend fun loadReports() {
        try { reports = api.adminReports(status = statusFilter.ifBlank { null }).reports }
        catch (e: retrofit2.HttpException) { if (e.code() == 403) forbidden = true }
        catch (_: Exception) {}
    }
    suspend fun loadUsers() { try { users = api.adminSearchUsers(userQuery).users } catch (_: Exception) {} }
    suspend fun loadActions() { try { actions = api.adminActions().actions } catch (_: Exception) {} }

    LaunchedEffect(statusFilter) { loadReports() }
    LaunchedEffect(userQuery, tab) { if (tab == "users") loadUsers() }
    LaunchedEffect(tab) { if (tab == "audit") loadActions() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
                Spacer(modifier = Modifier.width(12.dp))
                TText("MODERATION", color = HeadingPeach, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (forbidden) {
            item { TText("Access denied. Admins only.", color = Color(0xFFEF4444), fontSize = 15.sp) }
            return@LazyColumn
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("reports", "users", "audit").forEach { t ->
                    val active = tab == t
                    TText(
                        t.replaceFirstChar { it.uppercase() },
                        color = if (active) Color.White else TextMuted,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (active) AccentOrange else Color.White.copy(alpha = 0.06f))
                            .clickable { tab = t }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (tab) {
            "reports" -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        listOf("open", "actioned", "dismissed", "").forEach { s ->
                            val label = if (s.isBlank()) "all" else s
                            TText(label, color = if (statusFilter == s) AccentOrange else TextMuted, fontSize = 13.sp,
                                modifier = Modifier.clickable { statusFilter = s })
                        }
                    }
                }
                items(reports) { r ->
                    ReportCard(
                        r,
                        onHide = { CoroutineScope(Dispatchers.IO).launch { try { api.adminHideContent(HideContentRequest(r.target_type, r.target_id)); withContext(Dispatchers.Main) { loadReports() } } catch (_: Exception) {} } },
                        onBan = { CoroutineScope(Dispatchers.IO).launch { try { api.adminBanUser(BanUserRequest(r.target_id)); withContext(Dispatchers.Main) { loadReports() } } catch (_: Exception) {} } },
                        onDismiss = { CoroutineScope(Dispatchers.IO).launch { try { api.adminDismissReport(r.report_id); withContext(Dispatchers.Main) { loadReports() } } catch (_: Exception) {} } },
                    )
                }
                if (reports.isEmpty()) item { TText("No reports.", color = TextMuted, fontSize = 14.sp) }
            }
            "users" -> {
                item {
                    OutlinedTextField(
                        value = userQuery, onValueChange = { userQuery = it },
                        placeholder = { TText("Search username or email…", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AccentOrange, unfocusedBorderColor = TextMuted.copy(alpha = 0.3f)),
                    )
                }
                items(users) { u ->
                    UserRow(u) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                if (u.is_banned) api.adminUnbanUser(BanUserRequest(u.user_id)) else api.adminBanUser(BanUserRequest(u.user_id))
                                withContext(Dispatchers.Main) { loadUsers() }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            "audit" -> {
                items(actions) { a ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TText("${a.action} ${a.target_type}", color = TextWhite, fontSize = 13.sp)
                        TText(a.moderator_username ?: "?", color = TextMuted, fontSize = 12.sp)
                    }
                }
                if (actions.isEmpty()) item { TText("No actions yet.", color = TextMuted, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun ReportCard(r: AdminReport, onHide: () -> Unit, onBan: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
            .background(CardBackground).padding(12.dp),
    ) {
        Row {
            TText(r.target_type, color = TextMuted, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp))
            Spacer(modifier = Modifier.width(8.dp))
            TText(r.reason, color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            TText(r.status, color = if (r.status == "open") Color(0xFFEAB308) else TextMuted, fontSize = 12.sp)
        }
        r.details?.let { Spacer(modifier = Modifier.height(4.dp)); TText(it, color = TextWhite.copy(alpha = 0.8f), fontSize = 13.sp) }
        TText("by ${r.reporter?.username ?: r.reporter_user_id}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        if (r.status == "open") {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (r.target_type == "user") {
                    TText("Ban user", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onBan() })
                } else {
                    TText("Hide content", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onHide() })
                }
                TText("Dismiss", color = TextMuted, fontSize = 13.sp, modifier = Modifier.clickable { onDismiss() })
            }
        }
    }
}

@Composable
private fun UserRow(u: AdminUserRow, onToggleBan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp)).background(CardBackground).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TText(u.username ?: u.user_id, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (u.is_admin) { Spacer(modifier = Modifier.width(6.dp)); TText("admin", color = AccentOrange, fontSize = 11.sp) }
                if (u.is_banned) { Spacer(modifier = Modifier.width(6.dp)); TText("banned", color = Color(0xFFEF4444), fontSize = 11.sp) }
            }
            TText(u.email ?: "", color = TextMuted, fontSize = 12.sp)
        }
        if (!u.is_admin) {
            TText(if (u.is_banned) "Unban" else "Ban", color = if (u.is_banned) TextMuted else Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onToggleBan() })
        }
    }
}
