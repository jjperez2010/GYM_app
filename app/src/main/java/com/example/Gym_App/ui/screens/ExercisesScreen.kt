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
                        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                            items(filteredExercises) { ex ->
                                // Cálculo de tiempo detallado:
                                // preparacion (20s) + (sets * duration del usuario) + ((sets - 1) * rest)
                                val prepTime = 20
                                val executionTime = ex.sets * ex.duration
                                val totalRest = (ex.sets - 1) * ex.rest
                                val totalSecs = prepTime + executionTime + totalRest
                                val totalMin = totalSecs / 60
                                val totalRemainder = totalSecs % 60
                                
                                val isOverdue = ex.updateReminderDays > 0 && ex.lastUpdateDate > 0 && 
                                    (System.currentTimeMillis() - ex.lastUpdateDate) > (ex.updateReminderDays.toLong() * 24 * 60 * 60 * 1000)

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                        editingExercise = ex
                                        showForm = true
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                                    border = BorderStroke(if (isOverdue) 2.dp else 0.5.dp, if (isOverdue) Color.Yellow else Color.White.copy(0.1f))
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(ex.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                if (isOverdue) {
                                                    Icon(Icons.Default.Timer, "Subir carga", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("SUBIR CARGA", color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            // Parámetros debajo del nombre
                                            Text("${ex.sets} series x ${ex.reps} reps — ${ex.weight}kg", color = Color(0xFF00FF00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(ex.muscleGroup, color = Color.White.copy(0.6f), fontSize = 12.sp)
                                                Spacer(Modifier.width(8.dp))
                                                Box(Modifier.size(3.dp).background(Color.Gray, CircleShape))
                                                Spacer(Modifier.width(8.dp))
                                                Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                Text(" ${totalMin}m ${totalRemainder}s", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Text("Material: ${ex.equipmentType}", color = Color.Gray, fontSize = 11.sp)
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
