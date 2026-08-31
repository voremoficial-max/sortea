package com.vorem.sortea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vorem.sortea.ui.theme.SorteaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SorteaTheme { SorteaApp() } }
    }
}

@Composable
fun SorteaApp() {
    val navController = rememberNavController()
    val uiState = remember { SorteaUiState() }
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onGenerarBoletas = { navController.navigate("config") },
                    onSettings = { navController.navigate("settings") },
                    onAbout = { navController.navigate("about") }
                )
            }
            composable("config") {
                ConfigScreen(
                    state = uiState,
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate("settings") },
                    onGenerate = { navController.navigate("generate") }
                )
            }
            composable("settings") {
                SettingsScreen(state = uiState, onBack = { navController.popBackStack() })
            }
            composable("generate") {
                GenerateScreen(state = uiState, onBack = { navController.popBackStack() })
            }
            composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
