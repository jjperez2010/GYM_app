package com.example.Gym_App.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    
    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        
        Spacer(Modifier.height(8.dp))
        
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
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    existingMuscles.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = { muscle = selection; expanded = false })
                    }
                    DropdownMenuItem(text = { Text("+ Agregar nuevo...", color = Color(0xFF00FF00)) }, onClick = { isAddingNewMuscle = true; expanded = false })
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

        NumericStepper("Series", sets, 1) { sets = it }
        NumericStepper("Reps", reps, 1) { reps = it }
        NumericStepper("Peso (0=Corp, -1=Asist)", weight, 1, 5) { weight = it }
        NumericStepper("Descanso", rest, 10) { rest = it }
        
        Row(Modifier.padding(top = 16.dp)) {
            if (isEditMode) { MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { onDelete() }; Spacer(Modifier.width(8.dp)) }
            MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }; Spacer(Modifier.width(8.dp)); MenuButton("Guardar", Modifier.weight(1f)) { if (name.isNotBlank()) onSave(Exercise(name, reps, sets, weight, rest, muscle)) }
        }
    }
}
