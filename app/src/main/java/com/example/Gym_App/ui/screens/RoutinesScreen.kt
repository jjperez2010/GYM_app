package com.example.Gym_App.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.Gym_App.model.Exercise
import com.example.Gym_App.model.Routine
import com.example.Gym_App.model.RoutineEntity
import com.example.Gym_App.viewmodel.GymViewModel
import com.example.Gym_App.ui.components.BottomNavBar
import com.example.Gym_App.ui.components.MenuButton
import com.example.Gym_App.ui.components.RoutineForm

@Composable
fun RoutinesScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val selectedGradientColor = prefs.getInt("trainingGradientColor", Color(0xFF424242).toArgb())
    
    val routinesEntity by viewModel.routines.collectAsState()
    val allExercisesEntity by viewModel.exercises.collectAsState()
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, muscleGroup = it.muscleGroup) }
    val routinesList = routinesEntity.map { Routine(it.name, it.exerciseNames.split(","), it.imageId) }

    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(bottomBar = { BottomNavBar(navController, "rutinas") }) { innerPadding ->
        val gradientRoutine = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(selectedGradientColor)))
        Box(Modifier.fillMaxSize().background(brush = gradientRoutine).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rutinas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                if (showForm) { 
                    val currentEditingRoutine = editingIndex?.let { routinesList[it] }
                    RoutineForm(allExercises, currentEditingRoutine, { showForm = false; editingIndex = null }, { 
                        editingIndex?.let { idx -> viewModel.deleteRoutine(routinesList[idx].name) }
                        showForm = false; editingIndex = null 
                    }, { newRoutine -> 
                        // Si el nombre cambió, eliminar la antigua
                        if (currentEditingRoutine != null && currentEditingRoutine.name != newRoutine.name) {
                            viewModel.deleteRoutine(currentEditingRoutine.name)
                        }
                        viewModel.addRoutine(RoutineEntity(newRoutine.name, newRoutine.exerciseNames.joinToString(","), newRoutine.imageId))
                        showForm = false; editingIndex = null 
                    }) 
                }
                else { 
                    MenuButton("Crear Nueva Rutina", bColor = Color(0xFFBB86FC)) { showForm = true }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) { 
                        itemsIndexed(routinesList) { index, routine -> 
                            // Cálculo detallado de la rutina:
                            // (preparación + ejecución + descansos) de cada ejercicio + 10s transición entre ejercicios
                            val routineExs = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                            val totalSecs = routineExs.sumOf { ex -> 
                                val prep = 20
                                val exec = ex.sets * ex.duration
                                val rest = (ex.sets - 1) * ex.rest
                                prep + exec + rest
                            } + (if (routineExs.size > 1) (routineExs.size - 1) * 15 else 0)
                            
                            val totalMin = totalSecs / 60
                            val muscleSum = routineExs.map { it.muscleGroup }.distinct().take(2).joinToString(", ")

                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editingIndex = index; showForm = true }, 
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)), 
                                border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.5f))
                            ) { 
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { 
                                    Column(Modifier.weight(1f)) { 
                                        Text(routine.name, color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${routine.exerciseNames.size} ej", color = Color.White.copy(0.6f), fontSize = 12.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Box(Modifier.size(3.dp).background(Color.Gray, CircleShape))
                                            Spacer(Modifier.width(8.dp))
                                            Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                            Text(" ~${totalMin} min", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        if (muscleSum.isNotEmpty()) {
                                            Text(muscleSum, color = Color(0xFF00FF00).copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) 
                                } 
                            } 
                        } 
                    } 
                }
            }
        }
    }
}
