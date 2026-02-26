package com.example.appdeprueba.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.appdeprueba.viewmodel.GymViewModel
import com.example.appdeprueba.ui.components.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController, viewModel: GymViewModel) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val exercises by viewModel.exercises.collectAsState(initial = emptyList())
    val weightHistory by viewModel.weightHistory.collectAsState(initial = emptyList())
    
    var selectedExercise by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Inicializar ejercicio seleccionado si está vacío y hay ejercicios disponibles
    LaunchedEffect(exercises) {
        if (selectedExercise.isEmpty() && exercises.isNotEmpty()) {
            selectedExercise = exercises[0].name
        }
    }

    Scaffold(bottomBar = { BottomNavBar(navController, "progress") }) { innerPadding ->
        val gradientBg = Brush.verticalGradient(listOf(Color.Black, Color(0xFF1A1C20)))
        Column(Modifier.fillMaxSize().background(gradientBg).padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Tu Progreso", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            // Dashboard cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val workoutsThisMonth = history.filter { 
                    val workoutCal = Calendar.getInstance().apply { timeInMillis = it.date }
                    workoutCal.get(Calendar.MONTH) == currentMonth 
                }.size
                StatCard("Entrenos Mes", workoutsThisMonth.toString(), Modifier.weight(1f), Color(0xFF00FF00))
                
                val totalVolume = history.sumOf { it.volume }
                StatCard("Volumen Total", "${totalVolume/1000}k", Modifier.weight(1f), Color(0xFF00AAFF))
            }

            Spacer(Modifier.height(32.dp))
            Text("Evolución Peso Corporal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            // Gráfica de peso corporal
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Reutilizamos el componente de gráfico de peso (WeightProgressChart definido en WeightScreen) 
                    // o creamos uno similar aquí si no es accesible directamente.
                    // Dado que WeightProgressChart está en WeightScreen.kt como una función privada o interna, 
                    // aquí usamos una versión adaptada.
                    Box(Modifier.height(180.dp).fillMaxWidth()) {
                        WeightHistoryGraph(weightHistory.reversed())
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Resumen de récords de peso
                    if (weightHistory.isNotEmpty()) {
                        val currentW = weightHistory.first().weight
                        val maxW = weightHistory.maxOf { it.weight }
                        val minW = weightHistory.minOf { it.weight }
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            RecordItem("Mínimo", "%.1f kg".format(minW), Color(0xFF00FF00))
                            RecordItem("Actual", "%.1f kg".format(currentW), Color.White)
                            RecordItem("Máximo", "%.1f kg".format(maxW), Color(0xFFFF5252))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Progreso en Ejercicios", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            // Selector de Ejercicio
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedExercise,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true },
                    label = { Text("Seleccionar Ejercicio") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00AAFF),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                DropdownMenu(
                    expanded = dropdownExpanded, 
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF1A1C20))
                ) {
                    exercises.forEach { ex ->
                        DropdownMenuItem(
                            text = { Text(ex.name, color = Color.White) }, 
                            onClick = { selectedExercise = ex.name; dropdownExpanded = false }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            WeightLineChart(history.filter { it.exerciseName == selectedExercise })
            
            Spacer(Modifier.height(32.dp))
            Text("Volumen Semanal", color = Color.Gray, fontSize = 14.sp)
            VolumeChart(history)
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun RecordItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WeightHistoryGraph(history: List<com.example.appdeprueba.model.WeightEntity>) {
    if (history.size < 2) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Registra al menos 2 pesos para ver la evolución", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    val weights = history.map { it.weight.toFloat() }
    val maxW = weights.maxOrNull() ?: 100f
    val minW = weights.minOrNull() ?: 0f
    val range = (maxW - minW).coerceAtLeast(1f)

    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
        val path = androidx.compose.ui.graphics.Path()
        val stepX = size.width / (weights.size - 1)
        
        weights.forEachIndexed { i, w ->
            val x = i * stepX
            val y = size.height - ((w - minW) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            
            // Dibujar punto
            drawCircle(
                color = Color(0xFF00FF00),
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
        
        // Dibujar línea
        drawPath(
            path = path,
            color = Color(0xFF00FF00),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Efecto de área debajo de la línea (opcional pero profesional)
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00FF00).copy(0.2f), Color.Transparent)
            )
        )
    }
}
