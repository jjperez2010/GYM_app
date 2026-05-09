package com.example.Gym_App.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
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
import com.example.Gym_App.model.ExerciseEntity
import com.example.Gym_App.viewmodel.GymViewModel
import com.example.Gym_App.ui.components.BottomNavBar
import com.example.Gym_App.ui.components.ExerciseForm
import com.example.Gym_App.ui.components.MenuButton

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.net.toUri
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    
    val selectedTopColor = prefs.getInt("trainingGradientTopColor", Color.Black.toArgb())
    val selectedBottomColor = prefs.getInt("trainingGradientBottomColor", Color(0xFF424242).toArgb())
    
    val filteredExercises by viewModel.filteredExercises.collectAsState()
    val selectedMuscle by viewModel.selectedMuscle.collectAsState()
    val selectedEquipment by viewModel.selectedEquipment.collectAsState()
    
    var showForm by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val muscleGroups = listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core")
    val equipmentTypes = listOf("Barra", "Mancuerna", "Máquina", "Peso Corporal", "Polea")

    Scaffold(
        bottomBar = { BottomNavBar(navController, "ejercicios") }
    ) { innerPadding ->
        val gradientEx = Brush.verticalGradient(listOf(Color(selectedTopColor), Color(selectedBottomColor)))

        Box(Modifier.fillMaxSize().background(brush = gradientEx).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                
                Spacer(Modifier.height(16.dp))
                Text("Biblioteca de Ejercicios", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(16.dp))

                // Barra de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar ejercicio...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF00),
                        unfocusedBorderColor = Color.Gray.copy(0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Filtro de Grupos Musculares (Chips)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMuscle == null,
                            onClick = { viewModel.updateMuscleFilter(null) },
                            label = { Text("Todos") },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedContainerColor = Color(0xFF00FF00)
                            )
                        )
                    }
                    items(muscleGroups) { muscle ->
                        FilterChip(
                            selected = selectedMuscle == muscle,
                            onClick = { viewModel.updateMuscleFilter(if(selectedMuscle == muscle) null else muscle) },
                            label = { Text(muscle) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black,
                                selectedContainerColor = Color(0xFF00FF00)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Filtro de Equipamiento
                var showEquipMenu by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showEquipMenu = true },
                        label = { Text(selectedEquipment ?: "Equipamiento") },
                        leadingIcon = { Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color.Gray)
                    )
                    if (selectedEquipment != null) {
                        TextButton(onClick = { viewModel.updateEquipmentFilter(null) }) {
                            Text("Limpiar", color = Color.Red.copy(0.7f), fontSize = 12.sp)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showEquipMenu,
                        onDismissRequest = { showEquipMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1C20))
                    ) {
                        equipmentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = Color.White) },
                                onClick = { 
                                    viewModel.updateEquipmentFilter(type)
                                    showEquipMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (showForm) {
                    ExerciseForm(
                        isEditMode = editingExercise != null,
                        initial = editingExercise?.let { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.duration, it.muscleGroup, it.updateReminderDays, it.lastUpdateDate) },
                        existingMuscles = muscleGroups,
                        onCancel = { showForm = false; editingExercise = null },
                        onDelete = {
                            editingExercise?.let { viewModel.deleteExercise(it.name) }
                            showForm = false; editingExercise = null
                        },
                        onSave = { updatedEx ->
                            val now = System.currentTimeMillis()
                            val lastUpdate = if (editingExercise != null && 
                                (editingExercise!!.weight != updatedEx.weight || 
                                 editingExercise!!.reps != updatedEx.reps || 
                                 editingExercise!!.sets != updatedEx.sets)) now else (editingExercise?.lastUpdateDate ?: 0L)
                            
                            viewModel.addExercise(ExerciseEntity(
                                updatedEx.name, updatedEx.reps, updatedEx.sets,
                                updatedEx.weight, updatedEx.rest, updatedEx.duration, updatedEx.muscleGroup,
                                editingExercise?.equipmentType ?: selectedEquipment ?: "Peso Corporal",
                                updateReminderDays = updatedEx.updateReminderDays,
                                lastUpdateDate = lastUpdate
                            ))
                            showForm = false; editingExercise = null
                        }
                    )
                } else {
                    MenuButton("Nuevo Ejercicio") {
                        editingExercise = null
                        showForm = true
                    }

                    Spacer(Modifier.height(16.dp))

                    if (filteredExercises.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron ejercicios", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredExercises) { ex ->
                                val totalMin = (ex.sets * ex.duration + (ex.sets - 1) * ex.rest) / 60
                                val isOverdue = ex.updateReminderDays > 0 && ex.lastUpdateDate > 0 && 
                                    (System.currentTimeMillis() - ex.lastUpdateDate) > (ex.updateReminderDays.toLong() * 24 * 60 * 60 * 1000)

                                Card(
                                    modifier = Modifier.fillMaxWidth().height(100.dp).clickable { 
                                        editingExercise = ex
                                        showForm = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20)),
                                    border = if (isOverdue) BorderStroke(1.5.dp, Color.Yellow) else BorderStroke(0.5.dp, Color.White.copy(0.1f))
                                ) {
                                    Box(Modifier.fillMaxSize()) {
                                        // Imagen de fondo basada en grupo muscular
                                        val imageName = ex.muscleGroup.lowercase()
                                            .replace("í", "i").replace("é", "e").replace("á", "a")
                                            .replace("ó", "o").replace("ú", "u")
                                        val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                                        
                                        if (resId != 0) {
                                            Image(
                                                painter = androidx.compose.ui.res.painterResource(id = resId),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                alpha = 0.4f
                                            )
                                        }

                                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(0.85f), Color.Transparent))))

                                        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(ex.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                    if (isOverdue) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Icon(Icons.Default.Timer, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                                Text("${ex.sets}x${ex.reps} — ${ex.weight}kg", color = Color(0xFFC6FF00), fontSize = 14.sp, fontWeight = FontWeight.Black)
                                                
                                                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(ex.muscleGroup, color = Color.White.copy(0.6f), fontSize = 11.sp)
                                                    Spacer(Modifier.width(8.dp))
                                                    Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                                                    Text(" ${totalMin}m", color = Color.Gray, fontSize = 11.sp)
                                                }
                                            }
                                            IconButton(onClick = { editingExercise = ex; showForm = true }) {
                                                Icon(Icons.Default.FilterAlt, null, tint = Color.White.copy(0.3f))
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
    }
}
