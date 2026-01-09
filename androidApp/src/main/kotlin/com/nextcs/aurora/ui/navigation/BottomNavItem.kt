package com.nextcs.aurora.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Social : BottomNavItem("social", Icons.Default.AccountCircle, "Social")
    object Activity : BottomNavItem("activity", Icons.Default.Notifications, "Activity")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}
