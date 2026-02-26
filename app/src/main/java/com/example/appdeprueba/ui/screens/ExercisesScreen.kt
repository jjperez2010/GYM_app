package com.example.appdeprueba.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appdeprueba.model.Exercise
import com.example.appdeprueba.model.ExerciseEntity
import com.example.appdeprueba.viewmodel.GymViewModel
import com.example.appdeprueba.ui.components.BottomNavBar
import com.example.appdeprueba.ui.components.ExerciseForm
import com.example.appdeprueba.ui.components.MenuButton

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(navController: NavController, viewModel: GymViewModel) {
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
        val gradientEx = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF005000)))

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
                        initial = editingExercise?.let { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.muscleGroup) },
                        existingMuscles = muscleGroups,
                        onCancel = { showForm = false; editingExercise = null },
                        onDelete = {
                            editingExercise?.let { viewModel.deleteExercise(it.name) }
                            showForm = false; editingExercise = null
                        },
                        onSave = { updatedEx ->
                            viewModel.addExercise(ExerciseEntity(
                                updatedEx.name, updatedEx.reps, updatedEx.sets,
                                updatedEx.weight, updatedEx.rest, updatedEx.muscleGroup,
                                selectedEquipment ?: "Peso Corporal"
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
                                // Cálculo de tiempo estimado del ejercicio
                                val estMin = (ex.sets * 60 + (ex.sets - 1) * ex.rest) / 60
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                        editingExercise = ex
                                        showForm = true
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                                    border = BorderStroke(0.5.dp, Color.White.copy(0.1f))
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(ex.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(ex.muscleGroup, color = Color(0xFF00FF00).copy(0.7f), fontSize = 12.sp)
                                                Spacer(Modifier.width(8.dp))
                                                Box(Modifier.size(3.dp).background(Color.Gray, CircleShape))
                                                Spacer(Modifier.width(8.dp))
                                                Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                Text(" ${estMin}m", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Text("Material: ${ex.equipmentType}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${ex.sets}x${ex.reps}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                            Text("${ex.weight} kg", color = Color(0xFF00FF00), fontSize = 12.sp)
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
