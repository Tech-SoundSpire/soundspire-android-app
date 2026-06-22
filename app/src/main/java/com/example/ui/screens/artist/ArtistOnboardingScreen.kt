package com.example.ui.screens.artist

import com.example.ui.components.TText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ArtistOrange = Color(0xFFFA6400)

private data class RoleCard(val id: String, val title: String, val description: String, val icon: ImageVector, val active: Boolean)

private val roleCards = listOf(
    RoleCard("artist", "Artist", "Discover your sound and grow your fanbase. Access tools that help you create, promote, and thrive.", Icons.Default.MusicNote, true),
    RoleCard("record-label", "Record Label", "Manage rosters, track releases, and amplify your reach. Built to scale with your artists' success.", Icons.Default.Album, false),
    RoleCard("manager", "Manager", "Handle bookings, royalties, and team coordination. Stay in sync with your artists, always.", Icons.Default.Folder, false),
)

@Composable
fun ArtistOnboardingScreen(
    onSelectArtist: () -> Unit,
    onArtistLogin: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Color(0xFF281545), Color.Black), radius = 1400f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: back + artist login
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TText("← Back", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
                Button(
                    onClick = onArtistLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = ArtistOrange),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    TText("Artist Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TText("Welcome to SoundSpire", color = ArtistOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            TText("We're excited to have you on-board!", color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            TText("Which of the following best describes you?", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(32.dp))

            roleCards.forEach { card ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2D2838))
                        .then(if (card.active) Modifier.clickable { onSelectArtist() } else Modifier)
                        .border(
                            width = if (card.active) 0.dp else 0.dp,
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().then(if (!card.active) Modifier.padding(0.dp) else Modifier)) {
                        if (!card.active) {
                            Box(modifier = Modifier.align(Alignment.End).background(Color(0xFFEAB308), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                TText("Coming Soon", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(ArtistOrange), contentAlignment = Alignment.Center) {
                            Icon(card.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TText(card.title, color = if (card.active) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        TText(card.description, color = Color(0xFFD1D5DB).copy(alpha = if (card.active) 1f else 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
