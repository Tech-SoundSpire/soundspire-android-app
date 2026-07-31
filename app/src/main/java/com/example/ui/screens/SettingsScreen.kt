package com.example.ui.screens

import com.example.ui.components.TText

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import coil.compose.AsyncImage
import com.example.data.remote.ApiClient
import com.example.data.remote.BlockRow
import com.example.util.resolveImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDarkPurple
import com.example.ui.theme.BackgroundMidPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HeadingPeach
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.LanguageManager
import com.example.util.SUPPORTED_LANGUAGES

/**
 * App settings. Currently hosts language selection (moved here from the floating picker);
 * structured as sections so more settings can be added later.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onModeration: () -> Unit = {}) {
    val context = LocalContext.current
    val currentLang by LanguageManager.lang.collectAsState()
    val api = remember { ApiClient.getService(context) }
    var isAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try { isAdmin = api.getSession().user?.isAdmin == true } catch (_: Exception) {}
    }
    val baseUrl = com.example.BuildConfig.SOUNDSPIRE_API_BASE_URL.trimEnd('/')
    fun openUrl(path: String) {
        try {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("$baseUrl$path")))
        } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkPurple, BackgroundMidPurple, BackgroundDarkPurple))),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                TText("SETTINGS", color = HeadingPeach, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Language section
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                TText("Language", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            TText("Choose your preferred language for the app.", color = TextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(6.dp)
            ) {
                SUPPORTED_LANGUAGES.forEach { lang ->
                    val isActive = lang.code == currentLang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { LanguageManager.setLang(context, lang.code) }
                            .background(if (isActive) AccentOrange.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            TText(
                                lang.nativeLabel,
                                color = if (isActive) AccentOrange else TextWhite,
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                            )
                            TText(lang.label, color = TextMuted, fontSize = 12.sp)
                        }
                        if (isActive) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentOrange, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Blocked users section
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Block, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                TText("Blocked users", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            TText("People you've blocked. You won't see their content.", color = TextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            BlockedUsersSection()
        }

        // About & legal links (open the website)
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                TText("About & legal", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(6.dp)
            ) {
                if (isAdmin) SettingsLinkRow("Moderation dashboard", external = false) { onModeration() }
                SettingsLinkRow("Delete account") { openUrl("/delete-account") }
                SettingsLinkRow("Terms of Service") { openUrl("/terms") }
                SettingsLinkRow("Privacy Policy") { openUrl("/privacy") }
                SettingsLinkRow("Community Guidelines") { openUrl("/community-guidelines") }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsLinkRow(label: String, external: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TText(label, color = TextWhite, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(
            if (external) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun BlockedUsersSection() {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }
    var blocks by remember { mutableStateOf<List<BlockRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val res = api.getBlocks().blocks
            withContext(Dispatchers.Main) { blocks = res }
        } catch (_: Exception) {} finally { loading = false }
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(6.dp)
    ) {
        if (loading) {
            TText("Loading…", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        } else if (blocks.isEmpty()) {
            TText("You haven't blocked anyone.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        } else {
            blocks.forEach { b ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = resolveImageUrl(b.blockedUser?.profile_picture_url),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.DarkGray),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TText(b.blockedUser?.username ?: b.blocked_user_id, color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TText("Unblock", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                        val id = b.blocked_user_id
                        blocks = blocks.filter { it.blocked_user_id != id }
                        CoroutineScope(Dispatchers.IO).launch { try { api.unblockUser(id) } catch (_: Exception) {} }
                    })
                }
            }
        }
    }
}
