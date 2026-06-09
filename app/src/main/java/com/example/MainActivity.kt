package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.local.SoundSpireDatabase
import com.example.data.repository.SoundSpireRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PreferencesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.SoundSpireViewModel
import com.example.ui.viewmodel.SoundSpireViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database holding our local indies feed
    val db = SoundSpireDatabase.getDatabase(applicationContext)
    val repo = SoundSpireRepository(
      userProfileDao = db.userProfileDao(),
      artistDao = db.artistDao(),
      reviewDao = db.reviewDao(),
      commentDao = db.commentDao()
    )

    setContent {
      MyApplicationTheme {
        val viewModel: SoundSpireViewModel by viewModels {
          SoundSpireViewModelFactory(repo)
        }

        // Active state navigation routes controller
        val authState by viewModel.authUiState.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        val selectedArtistId by viewModel.selectedArtistId.collectAsState()

        var currentScreen by remember { mutableStateOf("auth") }

        // Handle offline profile state resolution on load
        remember(userProfile) {
          if (userProfile != null && userProfile!!.email != "guest@soundspire.online" && userProfile!!.faveGenres != "None") {
            currentScreen = "home"
          } else {
            currentScreen = "auth"
          }
          Unit
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when {
              currentScreen == "auth" -> {
                AuthScreen(
                  viewModel = viewModel,
                  onAuthSuccess = { currentScreen = "home" }
                )
              }
              selectedArtistId != null -> {
                ProfileScreen(
                  viewModel = viewModel
                )
              }
              currentScreen == "preferences" -> {
                PreferencesScreen(
                  viewModel = viewModel,
                  onBack = { currentScreen = "home" },
                  onLogout = { currentScreen = "auth" }
                )
              }
              else -> {
                HomeScreen(
                  viewModel = viewModel,
                  onNavigateToPreferences = { currentScreen = "preferences" }
                )
              }
            }
          }
        }
      }
    }
  }
}

