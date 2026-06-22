package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.BuildConfig
import com.example.data.remote.ApiClient
import com.example.data.remote.AppVersionResponse
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BASE_URL = BuildConfig.SOUNDSPIRE_API_BASE_URL.trimEnd('/')

/**
 * Checks the website's /api/app-version on launch and, if this build is behind the
 * latest published versionCode, shows an update dialog. Since the app isn't on the Play
 * Store, "Update" opens the download link in the browser. A release can be marked
 * `mandatory` server-side to make the dialog non-dismissable (blocking).
 *
 * Drop this once near the app root; it renders nothing until an update is found.
 */
@Composable
fun UpdateChecker() {
    val context = LocalContext.current
    val api = remember { ApiClient.getService(context) }
    var info by remember { mutableStateOf<AppVersionResponse?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val resp = try {
            withContext(Dispatchers.IO) { api.getAppVersion() }
        } catch (_: Exception) { null } ?: return@LaunchedEffect
        // Only surface when the server's latest is strictly newer than this build.
        if (resp.latestVersionCode > BuildConfig.VERSION_CODE) {
            info = resp
        }
    }

    val update = info ?: return
    if (dismissed && !update.mandatory) return

    fun openDownload() {
        val path = update.downloadUrl ?: "/download/android"
        val url = if (path.startsWith("http")) path else "$BASE_URL$path"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
    }

    AlertDialog(
        onDismissRequest = { if (!update.mandatory) dismissed = true },
        title = {
            Text(
                if (update.mandatory) "Update Required" else "Update Available",
                color = Color.White, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                buildString {
                    append(update.message ?: "A new version of SoundSpire is available.")
                    update.versionName?.let { append("\n\nLatest version: $it") }
                },
                color = TextMuted
            )
        },
        containerColor = Color(0xFF221C2F),
        confirmButton = {
            TextButton(onClick = { openDownload() }) {
                Text("Update", color = AccentOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (update.mandatory) null else {
            { TextButton(onClick = { dismissed = true }) { Text("Later", color = TextMuted) } }
        },
    )
}
