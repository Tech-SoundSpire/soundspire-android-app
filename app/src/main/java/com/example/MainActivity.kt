package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// import com.example.ui.components.UpdateChecker  // in-app update prompt disabled
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.LanguageManager
import com.example.util.LocalLanguage

class MainActivity : ComponentActivity() {
    // App Link (https://app.soundspire.online/...) waiting to be opened; AppNavigation opens it
    // once login/onboarding are done, then clears it.
    private var pendingLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch, so a rotation doesn't re-open the same link.
        if (savedInstanceState == null) pendingLink = intent?.data

        LanguageManager.init(applicationContext)

        val authViewModel: AuthViewModel by viewModels()

        setContent {
            val lang by LanguageManager.lang.collectAsState()
            MyApplicationTheme {
                CompositionLocalProvider(LocalLanguage provides lang) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        pendingLink = pendingLink,
                        onLinkHandled = { pendingLink = null },
                    )
                    // Checks /api/app-version on launch; prompts to update if this build is behind.
                    // UpdateChecker()  // in-app update prompt disabled
                }
            }
        }
    }

    // A link tapped while the app is already open (singleTop) arrives here instead of onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { pendingLink = it }
    }
}
