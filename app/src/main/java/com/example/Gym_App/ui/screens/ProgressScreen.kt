package com.example.Gym_App.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
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
import com.example.Gym_App.viewmodel.GymViewModel
import com.example.Gym_App.ui.components.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val selectedGradientColor = prefs.getInt("trainingGradientColor", Color(0xFF424242).toArgb())
    
    val history by viewModel.history.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()
    val analytics by viewModel.analyticsStats.collectAsState()
    val muscleStats by viewModel.muscleStats.collectAsState()

    var selectedExerciseForChart by remember { mutableStateOf<String?>(null) }
    val now = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var comparisonMode by remember { mutableStateOf(false) }
    var showWeeklyComparison by remember { mutableStateOf(false) }

    Scaffold(bottomBar = { BottomNavBar(navController, "progress") }) { innerPadding ->
        val gradientBg = Brush.verticalGradient(listOf(Color.Black, Color(selectedGradientColor)))
        Column(Modifier.fillMaxSize().background(gradientBg).padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState())) {
            
            HeaderSection(comparisonMode, showWeeklyComparison, onCompare = { comparisonMode = !comparisonMode }, onWeekly = { showWeeklyComparison = !showWeeklyComparison })
            
            MonthSelector(selectedMonth) { selectedMonth = it }

            Spacer(Modifier.height(16.dp))

            DashboardStats(analytics, comparisonMode)

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Evolución de Carga", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                val exercisesWithHistory = history.map { it.exerciseName }.distinct().sorted()
                var showExMenu by remember { mutableStateOf(false) }
                
                Box {
                    Text(
                        text = selectedExerciseForChart ?: "Seleccionar Ejercicio",
                        color = Color(0xFF00FF00),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { showExMenu = true }.padding(8.dp)
                    )
                    DropdownMenu(expanded = showExMenu, onDismissRequest = { showExMenu = false }, modifier = Modifier.background(Color(0xFF1A1C20))) {
                        exercisesWithHistory.forEach { ex ->
                            DropdownMenuItem(text = { Text(ex, color = Color.White) }, onClick = { selectedExerciseForChart = ex; showExMenu = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            ChartCard { 
                val chartData = if (selectedExerciseForChart != null) {
                    history.filter { it.exerciseName == selectedExerciseForChart }
                } else emptyList()
                WeightLineChart(chartData) 
            }

            Spacer(Modifier.height(24.dp))
            Text("Volumen Semanal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ChartCard { VolumeChart(history) }

            Spacer(Modifier.height(24.dp))
            Text("Por Grupo Muscular", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            muscleStats.forEach { (muscle, data) ->
                MuscleExpandableCard(muscle, data.first, data.second)
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeaderSection(compare: Boolean, weekly: Boolean, onCompare: () -> Unit, onWeekly: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tu Progreso", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Comparativa Semanal
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onWeekly() }.padding(8.dp)) {
                    Icon(
                        Icons.Default.DateRange, 
                        null, 
                        tint = if (weekly) Color(0xFF00AAFF) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Semanal", color = if (weekly) Color(0xFF00AAFF) else Color.Gray, fontSize = 9.sp)
                }
                
                Spacer(Modifier.width(8.dp))

                // Botón Modo Comparar
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCompare() }.padding(8.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.CompareArrows, 
                        null, 
                        tint = if (compare) Color(0xFF00FF00) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Comparar", color = if (compare) Color(0xFF00FF00) else Color.Gray, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun MonthSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        months.forEachIndexed { i, m ->
            FilterChip(
                selected = selected == i,
                onClick = { onSelect(i) },
                label = { Text(m) },
                modifier = Modifier.padding(end = 4.dp),
                colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedContainerColor = Color(0xFF00FF00), selectedLabelColor = Color.Black)
            )
        }
    }
}

@Composable
fun DashboardStats(stats: Map<String, Any>, compare: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Máx Peso", "%.1f".format(stats["maxWeight"] ?: 0.0), Modifier.weight(1f), Color(0xFF00FF00))
        StatCard("1RM Est.", "%.1f".format(stats["estimated1RM"] ?: 0.0), Modifier.weight(1f), Color(0xFFBB86FC))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Promedio", "%.1f".format(stats["avgWeight"] ?: 0.0), Modifier.weight(1f), Color(0xFF00AAFF))
        StatCard("Tendencia", stats["trend"]?.toString() ?: "→", Modifier.weight(1f), Color.White)
    }
}

@Composable
fun ChartCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.White.copy(0.1f))) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun MuscleExpandableCard(muscle: String, best: Double, hist: List<com.example.Gym_App.model.WorkoutHistoryEntity>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(muscle, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Mejor: ${best.toInt()}kg", color = Color(0xFF00FF00), fontSize = 12.sp)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    hist.forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(it.exerciseName, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                            Text("${it.weight.toInt()}kg x ${it.reps}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
