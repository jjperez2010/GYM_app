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
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.duration, it.muscleGroup) }
    val routinesList = routinesEntity.map { Routine(it.name, it.exerciseNames.split(","), it.imageId, it.customImageUri) }

    var showForm by remember { mutableStateOf(initialEditName != null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Sincronizar el estado inicial de edición si se pasa un nombre
    LaunchedEffect(initialEditName, routinesList) {
        if (initialEditName != null && editingIndex == null && routinesList.isNotEmpty()) {
            val index = routinesList.indexOfFirst { it.name == initialEditName }
            if (index != -1) {
                editingIndex = index
                showForm = true
            }
        }
    }

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
                        if (currentEditingRoutine != null && currentEditingRoutine.name != newRoutine.name) {
                            viewModel.deleteRoutine(currentEditingRoutine.name)
                        }
                        viewModel.addRoutine(RoutineEntity(newRoutine.name, newRoutine.exerciseNames.joinToString(","), newRoutine.imageId, newRoutine.customImageUri))
                        showForm = false; editingIndex = null 
                    }) 
                }
                else { 
                    MenuButton("Crear Nueva Rutina", bColor = Color(0xFFBB86FC)) { showForm = true }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                        itemsIndexed(routinesList) { index, routine -> 
                            val routineExs = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                            val totalMinutes = (routineExs.sumOf { it.sets * 60 + (it.sets - 1) * it.rest }) / 60
                            val muscles = routineExs.map { it.muscleGroup }.distinct().take(2).joinToString(", ")

                            Card(
                                modifier = Modifier.fillMaxWidth().height(140.dp).clickable { editingIndex = index; showForm = true },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20))
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    if (routine.customImageUri != null) {
                                        val bitmap = remember(routine.customImageUri) {
                                            try {
                                                context.contentResolver.openInputStream(routine.customImageUri.toUri())?.use {
                                                    BitmapFactory.decodeStream(it)
                                                }
                                            } catch (_: Exception) { null }
                                        }
                                        bitmap?.let {
                                            Image(
                                                painter = BitmapPainter(it.asImageBitmap()),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                alpha = 0.5f
                                            )
                                        }
                                    } else {
                                        val resId = context.resources.getIdentifier(routine.imageId, "drawable", context.packageName)
                                        if (resId != 0) {
                                            Image(
                                                painter = androidx.compose.ui.res.painterResource(id = resId),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                alpha = 0.5f
                                            )
                                        }
                                    }

                                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(0.9f), Color.Transparent))))
                                    
                                    Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(routine.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(muscles, color = Color(0xFFC6FF00), fontSize = 12.sp)
                                            Spacer(Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("$totalMinutes min", color = Color.Gray, fontSize = 12.sp)
                                                Spacer(Modifier.width(12.dp))
                                                Text("${routine.exerciseNames.size} ejercicios", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.5f))
                                    }
                                }
                            }
                        } 
                    } 
                }
            }
        }
    }
}
