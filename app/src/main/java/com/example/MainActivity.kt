package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.UserManager
import com.example.ui.ChatViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            val preferences by chatViewModel.preferences.collectAsState()
            val user by chatViewModel.userState.collectAsState()
            val userManager = UserManager.getInstance(this)

            MyApplicationTheme(darkTheme = preferences.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    val destination = if (user != null) "home" else "auth"
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("auth") {
                            AuthScreen(
                                onLogin = { email, pass, rememberMe ->
                                    userManager.login(email, pass, rememberMe)
                                },
                                onRegister = { name, email, pass ->
                                    userManager.register(name, email, pass)
                                },
                                onAuthSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = chatViewModel,
                                onNavigateToFiles = { navController.navigate("files") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToProfile = { navController.navigate("profile") }
                            )
                        }

                        composable("files") {
                            val files by chatViewModel.allFiles.collectAsState()
                            FilesScreen(
                                files = files,
                                onBack = { navController.popBackStack() },
                                onAnalyzeFile = { preview ->
                                    chatViewModel.setAttachedFile(preview)
                                    navController.popBackStack()
                                },
                                onFileUploaded = { preview ->
                                    chatViewModel.setAttachedFile(preview)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            val apiHealth by chatViewModel.apiHealth.collectAsState()
                            SettingsScreen(
                                preferences = preferences,
                                user = user,
                                apiHealth = apiHealth,
                                onUpdatePreferences = { update ->
                                    userManager.updatePreferences(update)
                                },
                                onResetMemory = { callback ->
                                    chatViewModel.resetMemory { callback() }
                                },
                                onClearAllChats = {
                                    chatViewModel.clearAllChats()
                                },
                                onLogout = {
                                    userManager.logout()
                                    navController.navigate("auth") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("profile") {
                            val conversations by chatViewModel.filteredConversations.collectAsState()
                            val files by chatViewModel.allFiles.collectAsState()
                            ProfileScreen(
                                user = user,
                                conversationsCount = conversations.size,
                                filesCount = files.size,
                                onLogout = {
                                    userManager.logout()
                                    navController.navigate("auth") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Retained for tests and preview compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
