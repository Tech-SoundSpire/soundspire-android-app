package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.components.UpdateChecker
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.LanguageManager
import com.example.util.LocalLanguage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        LanguageManager.init(applicationContext)

        val authViewModel: AuthViewModel by viewModels()

        setContent {
            val lang by LanguageManager.lang.collectAsState()
            MyApplicationTheme {
                CompositionLocalProvider(LocalLanguage provides lang) {
                    AppNavigation(authViewModel = authViewModel)
                    // Checks /api/app-version on launch; prompts to update if this build is behind.
                    UpdateChecker()
                }
            }
        }
    }
}
