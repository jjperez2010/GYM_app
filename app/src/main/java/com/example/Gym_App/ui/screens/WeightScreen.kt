package com.example.Gym_App.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.Gym_App.model.WeightEntity
import com.example.Gym_App.ui.components.BottomNavBar
import com.example.Gym_App.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

@Composable
fun WeightScreen(navController: NavController, viewModel: GymViewModel) {
    val weightHistory by viewModel.weightHistory.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    val selectedGradientColor = prefs.getInt("trainingGradientColor", Color(0xFF424242).toArgb())

    var showAddWeightDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    
    var userHeight by remember { mutableIntStateOf(prefs.getInt("userHeight", 170)) }
    var targetWeight by remember { mutableFloatStateOf(prefs.getFloat("targetWeight", 70f)) }

    val currentWeight = weightHistory.firstOrNull()?.weight ?: prefs.getInt("userWeight", 70).toDouble()
    val hMetric = userHeight / 100.0
    val bmi = if (hMetric > 0) currentWeight / hMetric.pow(2.0) else 0.0
    
    // Peso Ideal basado en rango saludable de IMC (18.5 - 24.9)
    val minIdealWeight = 18.5 * hMetric.pow(2.0)
    val maxIdealWeight = 24.9 * hMetric.pow(2.0)
    val averageIdealWeight = (minIdealWeight + maxIdealWeight) / 2

    Scaffold(
        bottomBar = { BottomNavBar(navController, "weight") },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddWeightDialog = true }, containerColor = Color(0xFF00FF00)) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Peso", tint = Color.Black)
            }
        }
    ) { innerPadding ->
        val gradientBg = Brush.verticalGradient(listOf(Color.Black, Color(selectedGradientColor)))
        Column(Modifier.fillMaxSize().background(gradientBg).padding(innerPadding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Seguimiento de Peso", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, "Información", tint = Color.White.copy(0.7f))
                    }
                    IconButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, "Resetear historial", tint = Color.Red.copy(0.7f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // BMI and Progress Cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    Modifier.weight(1f).clickable { navController.navigate("settings") }, 
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), 
                    border = BorderStroke(1.dp, Color(0xFF00FF00).copy(0.3f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("IMC Actual", color = Color.Gray, fontSize = 11.sp)
                        Text("%.1f".format(bmi), color = Color(0xFF00FF00), fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(getBMICategory(bmi), color = Color.Gray, fontSize = 10.sp)
                        
                        // Escala visual de IMC
                        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).background(Color.White.copy(0.1f), RoundedCornerShape(2.dp))) {
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(18.5f).fillMaxHeight().background(Color.Blue.copy(0.5f)))
                                Box(Modifier.weight(6.4f).fillMaxHeight().background(Color.Green.copy(0.5f)))
                                Box(Modifier.weight(5.1f).fillMaxHeight().background(Color.Yellow.copy(0.5f)))
                                Box(Modifier.weight(10f).fillMaxHeight().background(Color.Red.copy(0.5f)))
                            }
                            // Indicador de posición actual
                            val indicatorPos = (bmi.toFloat() / 40f).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(indicatorPos)
                                    .padding(end = 0.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(Modifier.size(4.dp, 8.dp).background(Color.White))
                            }
                        }
                    }
                }
                
                // Cuadro superior derecho: AHORA SOLO MUESTRA EL PESO IDEAL SUGERIDO
                Card(
                    Modifier.weight(1f), 
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), 
                    border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Ideal Sugerido", color = Color.Gray, fontSize = 11.sp)
                        Text("%.1f kg".format(averageIdealWeight), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Basado en IMC", color = Color.Gray, fontSize = 10.sp)
                        Text("Rango: %.1f-%.1f".format(minIdealWeight, maxIdealWeight), color = Color.Gray.copy(0.5f), fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Card de Peso Objetivo (AHORA PRINCIPAL Y MÁS GRANDE)
            Card(
                Modifier.fillMaxWidth().clickable { showTargetWeightDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
                border = BorderStroke(1.dp, Color(0xFF00AAFF).copy(0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Peso Objetivo", color = Color.Gray, fontSize = 14.sp)
                            Text("%.1f kg".format(targetWeight), color = Color(0xFF00AAFF), fontSize = 32.sp, fontWeight = FontWeight.Black)
                            val diff = currentWeight - targetWeight
                            Text(
                                if (diff > 0) "Te faltan %.1f kg para tu meta" .format(diff) else "¡Has alcanzado tu objetivo!", 
                                color = Color.White.copy(0.7f), 
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Barra de progreso hacia el objetivo
                    LinearProgressIndicator(
                        progress = { 
                            // Un cálculo simple de progreso
                            0.5f 
                        },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF00AAFF),
                        trackColor = Color.White.copy(0.1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Evolución", color = Color.Gray, fontSize = 12.sp)
            WeightProgressChart(weightHistory.reversed(), targetWeight)

            Spacer(Modifier.height(16.dp))
            Text("Historial", color = Color.Gray, fontSize = 12.sp)
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(weightHistory) { entry ->
                    WeightHistoryItem(entry)
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("Justificación Médica", color = Color.White) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Cálculo del Peso Ideal:", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("El peso sugerido se basa en la fórmula del Índice de Masa Corporal (IMC) recomendada por la OMS. Se establece como 'ideal' el peso que te sitúa exactamente en el centro del rango saludable (IMC de 18.5 a 24.9).", color = Color.Gray)
                    
                    Spacer(Modifier.height(12.dp))
                    Text("¿Por qué es solo una sugerencia?", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("1. No distingue músculo de grasa: Un deportista con mucha masa muscular puede tener un IMC alto sin tener exceso de grasa.\n2. No considera complexión: La densidad ósea y constitución física varían entre personas.\n3. Salud integral: El peso es solo un factor; la presión arterial, niveles de glucosa y porcentaje de grasa son igualmente importantes.", color = Color.Gray)
                    
                    Spacer(Modifier.height(12.dp))
                    Text("Advertencia:", color = Color(0xFFFFA500), fontWeight = FontWeight.Bold)
                    Text("Usa este dato solo como una referencia orientativa. Consulta siempre con un profesional de la salud o nutricionista para establecer objetivos personalizados.", color = Color.Gray, fontStyle = FontStyle.Italic)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("ENTENDIDO", color = Color(0xFF00FF00)) }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("¿Resetear historial?", color = Color.White) },
            text = { Text("Se borrarán todos los registros de peso guardados. Esta acción no se puede deshacer.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetWeightHistory()
                    showResetConfirmDialog = false
                }) { Text("BORRAR TODO", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }

    if (showAddWeightDialog) {
        var weightInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWeightDialog = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("Registrar Peso", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Peso en kg") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    weightInput.toDoubleOrNull()?.let {
                        viewModel.saveWeight(it)
                        showAddWeightDialog = false
                    }
                }) { Text("GUARDAR", color = Color(0xFF00FF00)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeightDialog = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }

    if (showTargetWeightDialog) {
        var targetInput by remember { mutableStateOf(targetWeight.toString()) }
        AlertDialog(
            onDismissRequest = { showTargetWeightDialog = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("Editar Peso Objetivo", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        label = { Text("Nuevo Objetivo (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    targetInput.toFloatOrNull()?.let {
                        targetWeight = it
                        prefs.edit { putFloat("targetWeight", it) }
                        showTargetWeightDialog = false
                    }
                }) { Text("ACTUALIZAR", color = Color(0xFF00AAFF)) }
            },
            dismissButton = {
                TextButton(onClick = { showTargetWeightDialog = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun WeightProgressChart(history: List<WeightEntity>, targetWeight: Float) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("Registra datos para ver el gráfico", color = Color.Gray)
        }
        return
    }

    val weights = history.map { it.weight.toFloat() }
    val maxW = (weights + targetWeight).maxOrNull() ?: 100f
    val minW = (weights + targetWeight).minOrNull() ?: 0f
    val range = (maxW - minW).coerceAtLeast(5f) // Padding de al menos 5kg

    Canvas(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 8.dp, vertical = 8.dp)) {
        val path = Path()
        val stepX = if (weights.size > 1) size.width / (weights.size - 1) else size.width
        
        // Dibujar línea de peso objetivo (punteada o tenue)
        val targetY = size.height - ((targetWeight - minW) / range) * size.height
        drawLine(
            color = Color(0xFF00AAFF).copy(0.3f),
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 1.dp.toPx()
        )

        if (weights.size > 1) {
            weights.forEachIndexed { i, w ->
                val x = i * stepX
                val y = size.height - ((w - minW) / range) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(Color(0xFF00FF00), radius = 3.dp.toPx(), center = Offset(x, y))
            }
            drawPath(path, Color(0xFF00FF00), style = Stroke(width = 2.dp.toPx()))
        } else {
            // Solo un punto
            val y = size.height - ((weights[0] - minW) / range) * size.height
            drawCircle(Color(0xFF00FF00), radius = 4.dp.toPx(), center = Offset(size.width / 2, y))
        }
    }
}

@Composable
fun WeightHistoryItem(entry: WeightEntity) {
    val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(entry.date))
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(date, color = Color.Gray, fontSize = 13.sp)
            Text("%.1f kg".format(entry.weight), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

fun getBMICategory(bmi: Double): String = when {
    bmi < 18.5 -> "Bajo peso"
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> "Sobrepeso"
    else -> "Obesidad"
}
