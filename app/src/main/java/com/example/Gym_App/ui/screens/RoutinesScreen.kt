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
import androidx.compose.runtime.saveable.rememberSaveable
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun RoutinesScreen(navController: NavController, viewModel: GymViewModel, initialEditName: String? = null) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val selectedGradientColor = prefs.getInt("trainingGradientColor", Color(0xFF424242).toArgb())
    
    val routinesEntity by viewModel.routines.collectAsState()
    val allExercisesEntity by viewModel.exercises.collectAsState()
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.duration, it.muscleGroup, it.updateReminderDays, it.lastUpdateDate) }
    val routinesList = routinesEntity.map { 
        Routine(
            it.name, 
            it.exerciseNames.split(","), 
            it.imageId, 
            it.customImageUri,
            it.assignedDays?.split(",")?.mapNotNull { d -> d.toIntOrNull() } ?: emptyList()
        ) 
    }

    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // Sincronizar el estado inicial de edición si se pasa un nombre
    LaunchedEffect(initialEditName, routinesList) {
        if (initialEditName != null && routinesList.isNotEmpty()) {
            val index = routinesList.indexOfFirst { it.name == initialEditName }
            if (index != -1) {
                editingIndex = index
            }
        }
    }

    Scaffold(bottomBar = { BottomNavBar(navController, "rutinas") }) { innerPadding ->
        val gradientRoutine = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(selectedGradientColor)))
        Box(Modifier.fillMaxSize().background(brush = gradientRoutine).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val isEditing = editingIndex != null
                Text(
                    text = if (isEditing) "Editar Rutina" else "Nueva Rutina",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                
                val currentEditingRoutine = editingIndex?.let { routinesList.getOrNull(it) }
                RoutineForm(
                    allExercises = allExercises,
                    initialRoutine = currentEditingRoutine,
                    onCancel = { navController.popBackStack() },
                    onDelete = { 
                        currentEditingRoutine?.let { viewModel.deleteRoutine(it.name) }
                        navController.popBackStack()
                    },
                    onSave = { newRoutine -> 
                        if (currentEditingRoutine != null && currentEditingRoutine.name != newRoutine.name) {
                            viewModel.deleteRoutine(currentEditingRoutine.name)
                        }
                        viewModel.addRoutine(RoutineEntity(
                            name = newRoutine.name,
                            exerciseNames = newRoutine.exerciseNames.joinToString(","),
                            imageId = newRoutine.imageId,
                            customImageUri = newRoutine.customImageUri,
                            assignedDays = if (newRoutine.assignedDays.isEmpty()) null else newRoutine.assignedDays.joinToString(",")
                        ))
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
