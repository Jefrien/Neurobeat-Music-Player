package dev.jefrien.neurobeat.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.jefrien.neurobeat.app.theme.LocalAppColors
import dev.jefrien.neurobeat.presentation.common.components.MiniPlayer
import dev.jefrien.neurobeat.presentation.screens.create.CreateScreen
import dev.jefrien.neurobeat.presentation.screens.discover.DiscoverScreen
import dev.jefrien.neurobeat.presentation.screens.library.AlbumDetailScreen
import dev.jefrien.neurobeat.presentation.screens.library.ArtistDetailScreen
import dev.jefrien.neurobeat.presentation.screens.library.LibraryScreen
import dev.jefrien.neurobeat.presentation.screens.library.PlaylistDetailScreen
import dev.jefrien.neurobeat.presentation.screens.login.LoginScreen
import dev.jefrien.neurobeat.presentation.screens.player.PlayerScreen
import dev.jefrien.neurobeat.presentation.screens.search.SearchScreen
import dev.jefrien.neurobeat.presentation.screens.settings.SettingsScreen
import dev.jefrien.neurobeat.presentation.viewmodel.LoginViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Discover.route,
        Screen.Search.route,
        Screen.Create.route,
        Screen.Library.route,
        Screen.Settings.route
    )

    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginState by loginViewModel.state.collectAsState()
    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loginViewModel.checkLoginStatus()
    }

    LaunchedEffect(loginState) {
        if (loginState !is LoginViewModel.LoginState.Idle) {
            isCheckingAuth = false
        }
    }

    val startDestination = when {
        isCheckingAuth -> Screen.Splash.route
        loginState is LoginViewModel.LoginState.Success -> Screen.Discover.route
        else -> Screen.Login.route
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Column {
                    MiniPlayer(
                        onExpand = { navController.navigate(Screen.Player.route) }
                    )
                    GlassBottomBar(
                        currentRoute = currentRoute ?: Screen.Discover.route,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Discover.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                val colors = LocalAppColors.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }

                LaunchedEffect(loginState, isCheckingAuth) {
                    if (!isCheckingAuth) {
                        val target = if (loginState is LoginViewModel.LoginState.Success) {
                            Screen.Discover.route
                        } else {
                            Screen.Login.route
                        }
                        navController.navigate(target) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Discover.route) { DiscoverScreen() }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Create.route) { CreateScreen() }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onArtistClick = { artistId ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                    },
                    onAlbumClick = { albumId ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                    },
                    onPlaylistClick = { playlistId ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Player.route) {
                PlayerScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("artistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                ArtistDetailScreen(
                    artistId = artistId,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumId ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                    }
                )
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                AlbumDetailScreen(
                    albumId = albumId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
