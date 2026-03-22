package com.example.Gym_App.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.Gym_App.viewmodel.GymViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(navController: NavController, viewModel: GymViewModel, routineId: String) {
    val context = LocalContext.current
    val exercises by viewModel.getExercisesByRoutine(routineId).collectAsState(initial = emptyList())
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    
    val selectedTopColor = prefs.getInt("trainingGradientTopColor", Color.Black.toArgb())
    val selectedBottomColor = prefs.getInt("trainingGradientBottomColor", Color(0xFF424242).toArgb())
    
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var restTimeLeft by remember { mutableIntStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var totalSecondsElapsed by remember { mutableIntStateOf(0) }
    
    var showNextExerciseSelector by remember { mutableStateOf(false) }
    val completedExercisesIndices = remember { mutableStateListOf<Int>() }
    
    // Estado para rastrear el ejercicio seleccionado en el diálogo antes de confirmar
    var tempSelectedIndex by remember { mutableIntStateOf(-1) }

    val currentExercise = exercises.getOrNull(currentExerciseIndex)

    // Cronómetro total de la sesión
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            totalSecondsElapsed++
        }
    }

    // Lógica del descanso
    LaunchedEffect(isResting, restTimeLeft) {
        if (isResting && restTimeLeft > 0) {
            delay(1000)
            restTimeLeft--
            if (restTimeLeft == 0) isResting = false
        }
    }

    // Cálculos globales
    val totalSetsCount = remember(exercises) { exercises.sumOf { it.sets } }
    val completedSetsCount = remember(currentExerciseIndex, currentSet, exercises, completedExercisesIndices.size) {
        if (exercises.isEmpty()) 0
        val fromCompleted = exercises.filterIndexed { index, _ -> completedExercisesIndices.contains(index) }.sumOf { it.sets }
        fromCompleted + (currentSet - 1)
    }
    val routineProgress = if (totalSetsCount > 0) completedSetsCount.toFloat() / totalSetsCount.toFloat() else 0f
    
    val totalTimeEst = remember(exercises) { exercises.sumOf { (it.sets * 60) + ((it.sets - 1) * it.rest) } / 60 }
    val timeRemainingEst = remember(routineProgress, totalTimeEst) { 
        val rem = (totalTimeEst * (1f - routineProgress)).toInt()
        if (rem < 1 && routineProgress < 1f) 1 else rem
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(routineId.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White, letterSpacing = 1.sp)
                        Text("SESIÓN EN CURSO", color = Color(0xFF00FF00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                actions = {
                    IconButton(onClick = { navController.navigate("menu") }) {
                        Icon(Icons.Default.History, null, tint = Color.Gray)
                    }
                }
            )
        }
    ) { innerPadding ->
        val bgGradient = Brush.verticalGradient(listOf(Color(selectedTopColor), Color(selectedBottomColor)))
        
        Box(Modifier.fillMaxSize().background(bgGradient).padding(innerPadding)) {
            if (exercises.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF00FF00))
            } else if (currentExercise != null) {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    
                    Spacer(Modifier.height(16.dp))

                    // 1. INDICADOR VISUAL DE PASOS (EJERCICIOS)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(exercises) { index, _ ->
                            val isCompleted = completedExercisesIndices.contains(index)
                            val isCurrent = index == currentExerciseIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompleted -> Color(0xFF00FF00)
                                            isCurrent -> Color(0xFFBB86FC)
                                            else -> Color.White.copy(0.1f)
                                        }
                                    )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))

                    // 2. DASHBOARD DE PROGRESO Y TIEMPOS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
                        border = BorderStroke(0.5.dp, Color.White.copy(0.1f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { routineProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color(0xFFBB86FC),
                                    strokeWidth = 6.dp,
                                    trackColor = Color.White.copy(0.05f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text("${(routineProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            
                            Spacer(Modifier.width(20.dp))
                            
                            Column(Modifier.weight(1f)) {
                                Text("TIEMPO TRANSCURRIDO", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Text(formatDuration(totalSecondsElapsed), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("RESTA APROX.", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Text("${timeRemainingEst} min", color = Color(0xFF00AAFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 3. TARJETA PRINCIPAL DEL EJERCICIO
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.05f))
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    currentExercise.muscleGroup.uppercase(),
                                    color = Color(0xFF00FF00),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = currentExercise.name,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 36.sp
                                )
                                // PARÁMETROS DEBAJO DEL NOMBRE
                                Text(
                                    text = "${currentExercise.sets} series x ${currentExercise.reps} reps — ${currentExercise.weight}kg",
                                    color = Color(0xFF00FF00).copy(0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Gráfico de Series (Dots)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(currentExercise.sets) { i ->
                                    val isSetDone = i < (currentSet - 1)
                                    val isSetCurrent = i == (currentSet - 1)
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .size(if (isSetCurrent) 16.dp else 12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSetDone) Color(0xFF00FF00)
                                                else if (isSetCurrent) Color.White
                                                else Color.White.copy(0.15f)
                                            )
                                            .then(if (isSetCurrent) Modifier.border(2.dp, Color(0xFF00FF00), CircleShape) else Modifier)
                                    )
                                }
                            }

                            // Datos secundarios (mantenidos para referencia visual rápida)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                BigStatItem("REPS", "${currentExercise.reps}")
                                BigStatItem("PESO", "${currentExercise.weight}kg")
                                BigStatItem("DESC.", "${currentExercise.rest}s")
                            }
                            
                            // Barra de progreso del ejercicio actual
                            val setProgressValue by animateFloatAsState(
                                targetValue = currentSet.toFloat() / currentExercise.sets.toFloat(),
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "setProgress"
                            )
                            Column(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SERIE ACTUAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("$currentSet / ${currentExercise.sets}", color = Color(0xFF00FF00), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { setProgressValue },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                    color = Color(0xFF00FF00),
                                    trackColor = Color.White.copy(0.05f),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 4. OVERLAY DE DESCANSO
                    AnimatedVisibility(
                        visible = isResting,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        Card(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF00AAFF).copy(0.2f)),
                            border = BorderStroke(2.dp, Color(0xFF00AAFF))
                        ) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Timer, null, tint = Color(0xFF00AAFF), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("DESCANSO ACTIVO", color = Color(0xFF00AAFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    Text("${restTimeLeft}s", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
                                }
                            }
                        }
                    }

                    // 5. BOTÓN DE ACCIÓN PRINCIPAL
                    Button(
                        onClick = {
                            if (currentSet < currentExercise.sets) {
                                currentSet++
                                restTimeLeft = currentExercise.rest
                                isResting = true
                            } else {
                                completedExercisesIndices.add(currentExerciseIndex)
                                if (completedExercisesIndices.size < exercises.size) {
                                    tempSelectedIndex = -1 // Resetear selección temporal
                                    showNextExerciseSelector = true
                                } else {
                                    viewModel.saveWorkoutSession(routineId)
                                    navController.navigate("menu") { popUpTo("menu") { inclusive = true } }
                                }
                            }
                        },
                        enabled = !isResting,
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF00),
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(0.1f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (currentSet == currentExercise.sets && completedExercisesIndices.size == exercises.size - 1) 
                                "FINALIZAR ENTRENAMIENTO" else "COMPLETAR SERIE", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 18.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    // Indicador de lo que viene (Próximo sugerido)
                    val nextSuggested = exercises.indices.firstOrNull { it != currentExerciseIndex && !completedExercisesIndices.contains(it) }
                    if (nextSuggested != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.padding(bottom = 20.dp).fillMaxWidth().clickable { showNextExerciseSelector = true },
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "SIGUIENTE SUGERIDO: ${exercises[nextSuggested].name}", 
                                color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO PARA SELECCIONAR SIGUIENTE EJERCICIO
    if (showNextExerciseSelector) {
        Dialog(onDismissRequest = { /* Forzar selección */ }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20)),
                border = BorderStroke(1.dp, Color(0xFF00FF00).copy(0.5f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Selecciona el siguiente ejercicio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val available = exercises.indices.filter { !completedExercisesIndices.contains(it) }
                        itemsIndexed(available) { _, originalIndex ->
                            val ex = exercises[originalIndex]
                            val isChosen = tempSelectedIndex == originalIndex
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    tempSelectedIndex = originalIndex
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) Color(0xFF00FF00).copy(0.2f) else Color.White.copy(0.05f)
                                ),
                                border = if (isChosen) BorderStroke(1.dp, Color(0xFF00FF00)) else null
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(ex.name, color = if (isChosen) Color(0xFF00FF00) else Color.White, fontWeight = FontWeight.Bold)
                                        Text("${ex.sets} series x ${ex.reps} reps — ${ex.weight}kg", color = if (isChosen) Color(0xFF00FF00).copy(0.8f) else Color.Gray, fontSize = 12.sp)
                                    }
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF00FF00))
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Button(
                        onClick = {
                            if (tempSelectedIndex != -1) {
                                currentExerciseIndex = tempSelectedIndex
                                currentSet = 1
                                restTimeLeft = exercises[tempSelectedIndex].rest
                                isResting = true
                                showNextExerciseSelector = false
                            }
                        },
                        enabled = tempSelectedIndex != -1,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black)
                    ) {
                        Text("CONFIRMAR SELECCIÓN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BigStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
    }
}
