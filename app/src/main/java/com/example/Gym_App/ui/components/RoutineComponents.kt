package com.example.Gym_App.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.Gym_App.model.Exercise
import com.example.Gym_App.model.Routine

@Composable
fun RoutineForm(
    allExercises: List<Exercise>,
    initialRoutine: Routine?,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Routine) -> Unit
) {
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    var selectedImageId by remember { mutableStateOf(initialRoutine?.imageId ?: "default") }
    val selected = remember { mutableStateListOf<String>().apply { if (initialRoutine != null) addAll(initialRoutine.exerciseNames) } }
    
    val imageOptions = listOf(
        "pecho" to "Pecho", "espalda" to "Espalda", "biceps" to "Bíceps",
        "triceps" to "Tríceps", "hombros" to "Hombros", "cuadriceps" to "Cuádriceps",
        "isquios" to "Isquios", "gluteos" to "Glúteos", "pantorrillas" to "Pantorrillas",
        "abdominales" to "Abdominales", "antebrazos" to "Antebrazos", "core" to "Core",
        "aductores" to "Aductores", "pecho_superior" to "Pecho Sup.", "lumbar" to "Lumbar"
    )

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 8.dp)) {
            if (initialRoutine != null) {
                MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { onDelete() }
                Spacer(Modifier.width(8.dp))
            }
            MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }
            Spacer(Modifier.width(8.dp))
            MenuButton("Guardar", Modifier.weight(1f), bColor = Color(0xFFBB86FC)) {
                if (name.isNotBlank() && selected.isNotEmpty()) onSave(Routine(name, selected.toList(), selectedImageId))
            }
        }
        
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la Rutina") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(Modifier.height(16.dp))

            Text("Imagen de la Rutina", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(imageOptions.size) { index ->
                    val option = imageOptions[index]
                    val isSelected = selectedImageId == option.first
                    
                    // Intentar cargar el recurso por nombre
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val resId = context.resources.getIdentifier(option.first, "drawable", context.packageName)
                    
                    Card(
                        modifier = Modifier
                            .width(120.dp)
                            .clickable { selectedImageId = option.first },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFFC6FF00)) else BorderStroke(1.dp, Color.White.copy(0.1f)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = option.second,
                                    modifier = Modifier.fillMaxSize().aspectRatio(1.5f),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    option.second,
                                    color = if (isSelected) Color(0xFFC6FF00) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            val grouped = allExercises.groupBy { it.muscleGroup }
            grouped.forEach { (muscle, exercises) ->
                Text(
                    muscle,
                    color = Color(0xFF00FF00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                exercises.forEach { ex ->
                    val idx = selected.indexOf(ex.name)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (idx != -1) selected.remove(ex.name) else selected.add(ex.name)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(if (idx != -1) Color(0xFFBB86FC) else Color.Transparent, CircleShape)
                                .border(1.dp, Color.Gray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (idx != -1) Text((idx + 1).toString(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                ex.name,
                                color = if (idx != -1) Color(0xFFBB86FC) else Color.White,
                                fontWeight = if (idx != -1) FontWeight.Bold else FontWeight.Normal
                            )
                            // PARÁMETROS DEBAJO DEL NOMBRE
                            Text(
                                "${ex.sets} series x ${ex.reps} reps — ${ex.weight}kg",
                                color = if (idx != -1) Color(0xFFBB86FC).copy(0.7f) else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
