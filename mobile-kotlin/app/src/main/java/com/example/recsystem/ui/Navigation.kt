package com.example.recsystem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recsystem.ui.auth.AuthState
import com.example.recsystem.ui.auth.AuthViewModel
import com.example.recsystem.ui.auth.LoginScreen
import com.example.recsystem.ui.auth.RegisterScreen
import com.example.recsystem.ui.discovery.DiscoveryScreen
import com.example.recsystem.ui.discovery.DiscoveryViewModel
import com.example.recsystem.ui.discovery.MovieDetailScreen
import com.example.recsystem.ui.discovery.MovieDetailViewModel
import com.example.recsystem.ui.profile.FullProfileScreen
import com.example.recsystem.ui.profile.ProfileViewModel
import com.example.recsystem.ui.search.SearchScreen
import com.example.recsystem.ui.search.SearchViewModel

// ── AppNavigator ──────────────────────────────────────────────────────────────
//
// Everyone lands on Discovery immediately — no login wall.
// Discovery and Search are fully public.
// Profile tab shows a soft login prompt when not signed in.
//
@Composable
fun AppNavigator(
    authViewModel:        AuthViewModel,
    discoveryViewModel:   DiscoveryViewModel,
    searchViewModel:      SearchViewModel,
    movieDetailViewModel: MovieDetailViewModel,
    profileViewModel:     ProfileViewModel
) {
    MainScreen(
        authViewModel        = authViewModel,
        discoveryViewModel   = discoveryViewModel,
        searchViewModel      = searchViewModel,
        movieDetailViewModel = movieDetailViewModel,
        profileViewModel     = profileViewModel
    )
}

// ── Auth sub-graph (embedded in Profile tab when not signed in) ───────────────

@Composable
fun AuthNavGraph(
    navController:    NavHostController,
    authViewModel:    AuthViewModel,
    onContinueAsGuest: (() -> Unit)? = null
) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel           = authViewModel,
                onLoginSuccess      = {},
                onNavigateToRegister = { navController.navigate("register") },
                onContinueAsGuest   = onContinueAsGuest
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel             = authViewModel,
                onRegisterSuccess     = {},
                onNavigateBackToLogin = { navController.popBackStack() },
                onContinueAsGuest     = onContinueAsGuest
            )
        }
    }
}

// ── MainScreen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel:        AuthViewModel,
    discoveryViewModel:   DiscoveryViewModel,
    searchViewModel:      SearchViewModel,
    movieDetailViewModel: MovieDetailViewModel,
    profileViewModel:     ProfileViewModel
) {
    val navController = rememberNavController()
    val navItems = listOf(BottomNavItem.Dashboard, BottomNavItem.Search, BottomNavItem.Profile)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute?.startsWith("details/") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFFF7E3C8),
                    contentColor   = Color.Black
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon     = { Icon(item.icon, contentDescription = item.title) },
                            label    = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            // ── Public: Discovery ─────────────────────────────────────────
            composable(BottomNavItem.Dashboard.route) {
                DiscoveryScreen(
                    viewModel    = discoveryViewModel,
                    onMovieClick = { type, id -> navController.navigate("details/$type/$id") }
                )
            }

            // ── Public: Search ────────────────────────────────────────────
            composable(BottomNavItem.Search.route) {
                SearchScreen(
                    viewModel    = searchViewModel,
                    onMovieClick = { type, id -> navController.navigate("details/$type/$id") }
                )
            }

            // ── Gated: Profile ────────────────────────────────────────────
            composable(BottomNavItem.Profile.route) {
                val authState by authViewModel.authState
                if (authState is AuthState.Success) {
                    FullProfileScreen(
                        user         = (authState as AuthState.Success).user,
                        viewModel    = profileViewModel,
                        onMovieClick = { type, id -> navController.navigate("details/$type/$id") },
                        onLogout     = { authViewModel.logout() }
                    )
                } else {
                    GuestProfileScreen(
                        authViewModel     = authViewModel,
                        onContinueAsGuest = {
                            navController.navigate(BottomNavItem.Dashboard.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            // ── Public: Movie / TV detail ─────────────────────────────────
            composable(
                route     = "details/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id")   { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "movie"
                val id   = backStackEntry.arguments?.getString("id")   ?: return@composable
                MovieDetailScreen(
                    type             = type,
                    movieId          = id,
                    viewModel        = movieDetailViewModel,
                    onBack           = { navController.popBackStack() },
                    authViewModel    = authViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}

// ── GuestProfileScreen — soft prompt, no hard wall ───────────────────────────

@Composable
fun GuestProfileScreen(authViewModel: AuthViewModel, onContinueAsGuest: () -> Unit) {
    val authNavController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        AuthNavGraph(
            navController     = authNavController,
            authViewModel     = authViewModel,
            onContinueAsGuest = onContinueAsGuest
        )
    }
}
