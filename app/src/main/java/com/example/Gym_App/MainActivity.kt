package com.example.Gym_App

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.Gym_App.ui.screens.*
import com.example.Gym_App.viewmodel.GymViewModel
import com.example.Gym_App.ui.theme.AppDePruebaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDePruebaTheme {
                val gymViewModel: GymViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return GymViewModel(applicationContext) as T
                    }
                })

                var showWelcome by rememberSaveable { mutableStateOf(true) }
                val navController = rememberNavController()
                
                if (showWelcome) { 
                    WelcomeScreen(onFinished = { showWelcome = false }) 
                } else {
                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") { MainScreen(navController, gymViewModel) }
                        composable("ejercicios") { ExercisesScreen(navController, gymViewModel) }
                        composable("rutinas") { RoutinesScreen(navController, gymViewModel) }
                        composable("weight") { WeightScreen(navController, gymViewModel) }
                        composable("progress") { ProgressScreen(navController, gymViewModel) }
                        composable("settings") { SettingsScreen(navController, gymViewModel) }
                        composable("active_workout/{routineId}") { backStackEntry ->
                            val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
                            ActiveWorkoutScreen(navController, gymViewModel, routineId)
                        }
                    }
                }
            }
        }
    }
}
