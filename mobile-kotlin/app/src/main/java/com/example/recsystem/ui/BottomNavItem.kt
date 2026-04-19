package com.example.recsystem.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem("dashboard", "Home", Icons.Default.Home)
    object Search : BottomNavItem("search", "Search", Icons.Default.Search)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person)
}
