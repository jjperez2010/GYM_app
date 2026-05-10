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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.Gym_App.utils.ImageStorage
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.File
import android.net.Uri

@Composable
fun RoutineForm(
    allExercises: List<Exercise>,
    initialRoutine: Routine?,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Routine) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    var selectedImageId by remember { mutableStateOf(initialRoutine?.imageId ?: "default") }
    var customImageUri by remember { mutableStateOf(initialRoutine?.customImageUri) }
    val assignedDays = remember { mutableStateListOf<Int>().apply { if (initialRoutine != null) addAll(initialRoutine.assignedDays) } }
    val selected = remember { mutableStateListOf<String>().apply { if (initialRoutine != null) addAll(initialRoutine.exerciseNames) } }
    
    val savedImages = remember { mutableStateListOf<File>().apply { addAll(ImageStorage.getAllImages(context)) } }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Sincronizar estados cuando initialRoutine cambia (Carga de datos)
    LaunchedEffect(initialRoutine) {
        if (initialRoutine != null) {
            name = initialRoutine.name
            selectedImageId = initialRoutine.imageId
            customImageUri = initialRoutine.customImageUri
            assignedDays.clear()
            assignedDays.addAll(initialRoutine.assignedDays)
            selected.clear()
            selected.addAll(initialRoutine.exerciseNames)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            val savedPath = ImageStorage.saveImage(context, it)
            if (savedPath != null) {
                customImageUri = savedPath
                savedImages.clear()
                savedImages.addAll(ImageStorage.getAllImages(context))
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("¿Eliminar Rutina?", color = Color.White) },
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
    
    // Auto-detección de imagen por predominancia
    LaunchedEffect(selected.toList()) {
        if (selected.isNotEmpty()) {
            val muscles = selected.mapNotNull { name -> allExercises.find { it.name == name }?.muscleGroup }
            if (muscles.isNotEmpty()) {
                val mostCommon = muscles.groupingBy { it }.eachCount().maxBy { it.value }.key
                val mappedId = when (mostCommon.lowercase()) {
                    "pecho" -> "pecho"
                    "espalda" -> "espalda"
                    "bíceps", "biceps" -> "biceps"
                    "tríceps", "triceps" -> "triceps"
                    "hombros" -> "hombros"
                    "pierna", "cuádriceps", "isquios", "glúteos" -> "cuadriceps"
                    else -> "default"
                }
                if (selectedImageId == "default" || selectedImageId == "") {
                   selectedImageId = mappedId
                }
            }
        }
    }
    
    val imageOptions = listOf(
        "pecho" to "Pecho", "espalda" to "Espalda", "biceps" to "Bíceps",
        "triceps" to "Tríceps", "hombros" to "Hombros", "cuadriceps" to "Piernas",
        "abdominales" to "Abs", "core" to "Core"
    )

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (initialRoutine != null) {
                MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { showDeleteConfirmation = true }
            }
            MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }
            MenuButton("Guardar", Modifier.weight(1f), bColor = Color(0xFFBB86FC)) {
                if (name.isNotBlank() && selected.isNotEmpty()) {
                    onSave(Routine(name, selected.toList(), selectedImageId, customImageUri, assignedDays.toList()))
                }
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

            Text("Asignar días de la semana", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            androidx.compose.foundation.lazy.LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days.size) { index ->
                    val dayNum = index + 1
                    val isSelected = assignedDays.contains(dayNum)
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (isSelected) assignedDays.remove(dayNum) else assignedDays.add(dayNum) },
                        label = { Text(days[index]) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White,
                            selectedLabelColor = Color.Black,
                            selectedContainerColor = Color(0xFFC6FF00)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Imagen de la Rutina", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { imageLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))) {
                    Text("Nueva Imagen", color = Color.White)
                }
                if (customImageUri != null) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { customImageUri = null }) { Text("Quitar personalizada", color = Color.Red) }
                }
            }

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Imágenes guardadas en almacenamiento interno
                items(savedImages.size) { index ->
                    val file = savedImages[index]
                    val isSelected = customImageUri == file.absolutePath
                    val bitmap = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath) }

                    Card(
                        modifier = Modifier.width(100.dp).clickable { 
                            customImageUri = file.absolutePath
                            selectedImageId = "" 
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFFC6FF00)) else BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            bitmap?.let {
                                Image(
                                    painter = androidx.compose.ui.graphics.painter.BitmapPainter(it.asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().aspectRatio(1.2f),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (isSelected) {
                                Box(Modifier.fillMaxSize().background(Color(0xFFC6FF00).copy(0.2f)))
                            }
                        }
                    }
                }

                // Imágenes predefinidas de Drawable
                items(imageOptions.size) { index ->
                    val option = imageOptions[index]
                    val isSelected = selectedImageId == option.first && customImageUri == null
                    
                    val resId = context.resources.getIdentifier(option.first, "drawable", context.packageName)
                    
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { selectedImageId = option.first; customImageUri = null },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFFC6FF00)) else BorderStroke(1.dp, Color.White.copy(0.1f)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = option.second,
                                    modifier = Modifier.fillMaxSize().aspectRatio(1.2f),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))).padding(4.dp)) {
                                Text(option.second, color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter))
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
