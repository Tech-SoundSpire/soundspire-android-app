package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BottomNavBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val navItems = listOf(
    NavItem(Routes.EXPLORE, "Explore", Icons.Default.Explore),
    NavItem(Routes.FEED, "Feed", Icons.Outlined.DynamicFeed),
    NavItem(Routes.COMMUNITIES, "Communities", Icons.Default.Groups),
    NavItem(Routes.REVIEWS, "Reviews", Icons.Default.RateReview),
    NavItem(Routes.NOTIFICATIONS, "Alerts", Icons.Default.Notifications),
    NavItem(Routes.PROFILE, "Profile", Icons.Default.Person),
)

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String?) {
    Column {
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(BottomNavBorder)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(SurfaceDark.copy(alpha = 0.95f))
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isActive = currentRoute == item.route
                val color = if (isActive) AccentOrange else TextMuted

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.EXPLORE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = com.example.util.LanguageManager.translate(item.label, com.example.util.LocalLanguage.current),
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
