package com.example.Gym_App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.Gym_App.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseForm(
    isEditMode: Boolean,
    initial: Exercise?,
    existingMuscles: List<String>,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var muscle by remember { mutableStateOf(initial?.muscleGroup ?: "General") }
    var expanded by remember { mutableStateOf(false) }
    var isAddingNewMuscle by remember { mutableStateOf(false) }
    var newMuscleName by remember { mutableStateOf("") }
    
    var sets by remember { mutableIntStateOf(initial?.sets ?: 3) }
    var reps by remember { mutableIntStateOf(initial?.reps ?: 10) }
    var weight by remember { mutableIntStateOf(initial?.weight ?: 30) }
    var rest by remember { mutableIntStateOf(initial?.rest ?: 120) }
    var duration by remember { mutableIntStateOf(initial?.duration ?: 60) }
    var updateReminderDays by remember { mutableIntStateOf(initial?.updateReminderDays ?: 30) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("¿Eliminar Ejercicio?", color = Color.White) },
            text = { Text("Esta acción no se puede deshacer.", color = Color.Gray) },
            containerColor = Color(0xFF1A1C20),
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteConfirmation = false
                    onDelete() 
                }) { Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditMode) "Editar Ejercicio" else "Nuevo Ejercicio",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (!isAddingNewMuscle) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = muscle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grupo Muscular") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded, 
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF1A1C20))
                        ) {
                            existingMuscles.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection, color = Color.White) }, 
                                    onClick = { muscle = selection; expanded = false }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Agregar nuevo...", color = Color(0xFF00FF00)) }, 
                                onClick = { isAddingNewMuscle = true; expanded = false }
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newMuscleName,
                            onValueChange = { newMuscleName = it },
                            label = { Text("Nuevo Grupo") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        IconButton(onClick = { isAddingNewMuscle = false }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                        IconButton(onClick = { if (newMuscleName.isNotBlank()) { muscle = newMuscleName; isAddingNewMuscle = false } }) { Icon(Icons.Default.Check, null, tint = Color.Green) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                NumericStepper("Series", sets, 1) { sets = it }
                NumericStepper("Repeticiones", reps, 1) { reps = it }
                NumericStepper("Peso (kg)", weight, 1, 5) { weight = it }
                NumericStepper("Descanso (seg)", rest, 10) { rest = it }
                NumericStepper("Ejecución (seg)", duration, 5) { duration = it }
                
                Spacer(Modifier.height(24.dp))
                Text("Progreso y Avisos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Días sin cambios de peso para alertar:", color = Color.Gray, fontSize = 12.sp)
                NumericStepper("Días (0=Desactivado)", updateReminderDays, 1, 7) { updateReminderDays = it }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(32.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEditMode) { 
                        MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { showDeleteConfirmation = true }
                    }
                    MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }
                    MenuButton("Guardar", Modifier.weight(1f), bColor = Color(0xFF00FF00)) { 
                        if (name.isNotBlank()) onSave(Exercise(name, reps, sets, weight, rest, duration, muscle, updateReminderDays, initial?.lastUpdateDate ?: 0L)) 
                    }
                }
            }
        }
    }
}
