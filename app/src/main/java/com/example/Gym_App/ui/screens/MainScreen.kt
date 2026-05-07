package com.example.Gym_App.ui.screens


import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.Gym_App.model.Exercise
import com.example.Gym_App.model.Routine
import com.example.Gym_App.model.WorkoutHistoryEntity
import com.example.Gym_App.ui.components.*
import com.example.Gym_App.ui.components.BottomNavBar
import com.example.Gym_App.viewmodel.GymViewModel
import kotlinx.coroutines.delay




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val routinesEntity by viewModel.routines.collectAsState()
    val allExercisesEntity by viewModel.exercises.collectAsState()
    val workoutDates by viewModel.workoutDates.collectAsState()
    val workoutHistory by viewModel.history.collectAsState()
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.duration, it.muscleGroup) }
    val routinesList = routinesEntity.map { Routine(it.name, it.exerciseNames.split(",")) }

    var routineStarted by rememberSaveable { mutableStateOf(false) }
    var waitingToStart by rememberSaveable { mutableStateOf(false) }
    var routineFinished by rememberSaveable { mutableStateOf(false) }
    var selectedExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var currentRoutineName by rememberSaveable { mutableStateOf("") }
    val showAdhocDialog = remember { mutableStateOf(false) }
    var showStopConfirmation by remember { mutableStateOf(false) }
    val pendingNavigation = remember { mutableStateOf<(() -> Unit)?>(null) }
    
    var showNextExerciseSelector by remember { mutableStateOf(false) }
    val completedExercisesIndices = remember { mutableStateListOf<Int>() }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var currentSet by rememberSaveable { mutableIntStateOf(1) }
    var timeLeft by rememberSaveable { mutableIntStateOf(0) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var phase by rememberSaveable { mutableIntStateOf(0) } 
    
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val globalWait = prefs.getInt("globalWait", 20)
    
    val selectedTopColor = prefs.getInt("trainingGradientTopColor", Color.Black.toArgb())
    val selectedBottomColor = prefs.getInt("trainingGradientBottomColor", Color(0xFF424242).toArgb())
    
    val isWaitingForUser by viewModel.isWaitingForUser.collectAsState()

    // --- MANTENER PANTALLA ENCENDIDA ---
    DisposableEffect(routineStarted) {
        val activity = context as? Activity
        if (routineStarted) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(routineStarted, phase, isPaused, timeLeft, waitingToStart, isWaitingForUser) {
        if (routineStarted && !waitingToStart && !isPaused && !isWaitingForUser && !routineFinished) {
            if (timeLeft > 0) {
                delay(1000)
                timeLeft--
            } else {
                val currentEx = selectedExercises.getOrNull(currentIndex) ?: return@LaunchedEffect
                when (phase) {
                    0 -> { phase = 1; timeLeft = currentEx.duration }
                    1 -> { phase = 2; timeLeft = currentEx.rest }
                    else -> {
                        val currentWeight = currentEx.weight.toDouble()

                        viewModel.saveWorkout(
                            WorkoutHistoryEntity(
                                date = System.currentTimeMillis(),
                                exerciseName = currentEx.name,
                                weight = currentWeight,
                                reps = currentEx.reps,
                                sets = 1,
                                volume = (if (currentWeight > 0) currentWeight else 1.0) * currentEx.reps.toDouble()
                            )
                        )

                        if (currentSet < currentEx.sets) {
                            currentSet++
                            phase = 1
                            timeLeft = currentEx.duration
                        } else {
                            if (!completedExercisesIndices.contains(currentIndex)) {
                                completedExercisesIndices.add(currentIndex)
                            }
                            
                            if (completedExercisesIndices.size < selectedExercises.size) {
                                showNextExerciseSelector = true
                                isPaused = true
                            } else {
                                viewModel.logCompletedWorkout(currentRoutineName)
                                routineFinished = true
                            }
                        }
                    }
                }
            }
        }
    }

    val secureNav: (() -> Unit) -> Unit = { destination ->
        if (routineStarted) pendingNavigation.value = destination else destination()
    }

    BackHandler(routineStarted && !routineFinished) { showStopConfirmation = true }
    BackHandler(routineFinished) { 
        routineStarted = false
        routineFinished = false
        completedExercisesIndices.clear()
    }

    Scaffold(bottomBar = { if (!routineStarted || routineFinished) BottomNavBar(navController, "menu") { secureNav(it) } }) { innerPadding ->
        val gradientMain = Brush.verticalGradient(listOf(Color(selectedTopColor), Color(selectedBottomColor)))
        Box(Modifier.fillMaxSize().background(brush = gradientMain).padding(innerPadding)) {
            if (!routineStarted) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))
                    
                    // CALENDARIO DE CONSISTENCIA
                    ConsistencyCalendar(
                        workoutDates = workoutDates, 
                        workoutHistory = workoutHistory,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // SECCIÓN DE MIS RUTINAS (MODO MATRIZ 3 COLUMNAS)
                    Text("Mis Rutinas", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    val gridItems = listOf("Rutina Rápida") + routinesList.map { it.name }
                    
                    Column {
                        gridItems.chunked(3).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { itemName ->
                                    val isAdhoc = itemName == "Rutina Rápida"
                                    
                                    // Cálculo de duración y grupos musculares para la tarjeta
                                    val routine = routinesList.find { it.name == itemName }
                                    val routineExercises = routine?.exerciseNames?.mapNotNull { name -> allExercises.find { it.name == name } } ?: emptyList()
                                    
                                    val totalMinutes = if (isAdhoc) 0 else {
                                        val workTime = routineExercises.sumOf { it.sets * 60 } // Asumimos 1 min por serie
                                        val restTime = routineExercises.sumOf { (it.sets - 1) * it.rest }
                                        val transitionTime = (routineExercises.size - 1) * globalWait
                                        (workTime + restTime + transitionTime) / 60
                                    }
                                    
                                    val muscles = routineExercises.map { it.muscleGroup }.distinct().take(2).joinToString(", ")

                                    Card(
                                        modifier = Modifier.weight(1f).padding(vertical = 4.dp).height(100.dp).clickable {
                                            if (isAdhoc) {
                                                showAdhocDialog.value = true
                                            } else {
                                                if (routineExercises.isNotEmpty()) {
                                                    selectedExercises = routineExercises; currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true; waitingToStart = true; routineFinished = false
                                                    currentRoutineName = itemName
                                                    completedExercisesIndices.clear()
                                                }
                                            }
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isAdhoc) Color(0xFFFFA500).copy(0.1f) else Color.White.copy(0.05f)
                                        ),
                                        border = BorderStroke(1.dp, if (isAdhoc) Color(0xFFFFA500).copy(0.3f) else Color(0xFFBB86FC).copy(0.2f))
                                    ) {
                                        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(
                                                imageVector = if (isAdhoc) Icons.Default.FlashOn else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isAdhoc) Color(0xFFFFA500) else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = itemName,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!isAdhoc) {
                                                Text(muscles, color = Color(0xFFBB86FC), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(8.dp))
                                                    Spacer(Modifier.width(2.dp))
                                                    Text("~$totalMinutes min", color = Color.Gray, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    // SECCIÓN DE MIS EJERCICIOS (MODO MATRIZ)
                    Text("Mis Ejercicios", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    val grouped = allExercises.groupBy { it.muscleGroup }
                    grouped.forEach { (muscle, exercises) ->
                        Text(
                            text = muscle.uppercase(),
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF00FF00),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Column {
                            exercises.chunked(2).forEach { rowExercises ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowExercises.forEach { exercise ->
                                        // Duración estimada del ejercicio (trabajo + descansos)
                                        val estMinutes = (exercise.sets * 60 + (exercise.sets - 1) * exercise.rest) / 60
                                        
                                        Card(
                                            Modifier.weight(1f).padding(vertical = 4.dp).height(80.dp).clickable {
                                                selectedExercises = listOf(exercise); currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true; waitingToStart = true; routineFinished = false
                                                currentRoutineName = exercise.name
                                                completedExercisesIndices.clear()
                                            },
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.02f)),
                                            border = BorderStroke(0.5.dp, Color.White.copy(0.1f))
                                        ) {
                                            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
                                                Text(exercise.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                // PARÁMETROS DEBAJO DEL NOMBRE
                                                Text("${exercise.sets}x${exercise.reps} — ${exercise.weight}kg", color = Color(0xFF00FF00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Timer, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(9.dp))
                                                        Spacer(Modifier.width(2.dp))
                                                        Text("${estMinutes}m", color = Color.Gray.copy(0.5f), fontSize = 9.sp)
                                                    }
                                                    Text(exercise.muscleGroup, color = Color(0xFF00FF00).copy(0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    if (rowExercises.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(32.dp))
                }
            } else if (routineFinished) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00FF00), modifier = Modifier.size(100.dp))
                    Spacer(Modifier.height(24.dp))
                    Text("¡TRABAJO COMPLETADO!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(currentRoutineName, color = Color(0xFF00FF00), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("Has finalizado todos los ejercicios. ¡Sigue así!", color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(48.dp))
                    Button(
                        onClick = { 
                            routineStarted = false
                            routineFinished = false
                            completedExercisesIndices.clear()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00)),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VOLVER AL MENÚ", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (waitingToStart) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿PREPARADO?", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(currentRoutineName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    
                    // Resalte visual para el ejercicio seleccionado
                    val currentEx = selectedExercises[currentIndex]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00FF00).copy(0.1f)),
                        border = BorderStroke(2.dp, Color(0xFF00FF00))
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            // CORRECCIÓN: Etiqueta dinámica
                            Text(
                                text = if (completedExercisesIndices.isEmpty()) "PRIMER EJERCICIO:" else "PRÓXIMO EJERCICIO:",
                                color = Color(0xFF00FF00), 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Black
                            )
                            Text(currentEx.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("${currentEx.sets} series x ${currentEx.reps} reps — ${currentEx.weight}kg", color = Color(0xFF00FF00), fontSize = 14.sp)
                        }
                    }

                    Button(onClick = { waitingToStart = false }, modifier = Modifier.size(130.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00))) {
                        Text("DALE", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    Text("Toca un ejercicio para empezar con él:", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 12.sp)
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        itemsIndexed(selectedExercises) { index, ex ->
                            val isSelected = index == currentIndex
                            val isCompleted = completedExercisesIndices.contains(index)
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !isCompleted) {
                                    currentIndex = index
                                }, 
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isSelected -> Color(0xFF00FF00).copy(0.15f)
                                        isCompleted -> Color.Gray.copy(0.1f)
                                        else -> Color.White.copy(0.05f)
                                    }
                                ),
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF00FF00)) else null
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            ex.name, 
                                            color = when {
                                                isSelected -> Color(0xFF00FF00)
                                                isCompleted -> Color.Gray
                                                else -> Color.White
                                            }, 
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text("${ex.sets} series x ${ex.reps} reps — ${ex.weight}kg", color = if (isSelected) Color(0xFF00FF00).copy(0.7f) else Color.Gray, fontSize = 12.sp)
                                    }
                                    if (isSelected) Icon(Icons.Default.Check, null, tint = Color(0xFF00FF00))
                                    if (isCompleted) Icon(Icons.Default.CheckCircle, null, tint = Color.Gray.copy(0.5f))
                                }
                            }
                        }
                    }
                    TextButton(onClick = { routineStarted = false; waitingToStart = false; completedExercisesIndices.clear() }) { Text("CANCELAR", color = Color.Red) }
                }
            } else {
                TrainingUI(
                    exercise = selectedExercises[currentIndex],
                    currentIndex = currentIndex,
                    totalExercises = selectedExercises.size,
                    currentSet = currentSet,
                    phase = phase,
                    timeLeft = timeLeft,
                    isPaused = isPaused,
                    allExercises = selectedExercises,
                    globalWait = globalWait,
                    completedIndices = completedExercisesIndices,
                    onTogglePause = { isPaused = !isPaused },
                    onFinish = {
                        // El ReleasedEffect maneja el cambio al llegar a 0
                        timeLeft = 0
                    },
                    onSkipExercise = {
                        // Posponer: NO añadimos a completados, solo abrimos selector
                        showNextExerciseSelector = true
                        isPaused = true
                    },
                    onStop = { showStopConfirmation = true }
                )
            }
        }
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("Finalizar Rutina", color = Color.White) },
            text = { Text("¿Estás seguro de que quieres finalizar el entrenamiento actual? No se guardará el progreso de este ejercicio.", color = Color.Gray) },
            confirmButton = { 
                TextButton(onClick = { 
                    showStopConfirmation = false
                    routineStarted = false
                    routineFinished = false
                    completedExercisesIndices.clear()
                    pendingNavigation.value?.invoke()
                    pendingNavigation.value = null
                }) { Text("SÍ, FINALIZAR", color = Color.Red) } 
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showStopConfirmation = false 
                    pendingNavigation.value = null
                }) { Text("CONTINUAR", color = Color.Gray) } 
            }
        )
    }

    pendingNavigation.value?.let { _ ->
        showStopConfirmation = true
    }

    if (isWaitingForUser) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color(0xFF1A1C20),
            title = { Text("¡Siguiente Ejercicio!", color = Color.White) },
            text = {
                val nextEx = selectedExercises.getOrNull(currentIndex)
                Text("¿Listo para empezar ${nextEx?.name}?\nPrepara el peso: ${formatWeight(nextEx?.weight ?: 0)}", color = Color.Gray)
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmNextExercise() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00))) {
                    Text("¡ESTOY LISTO!", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAdhocDialog.value) {
        AdhocRoutineDialog(
            allExercises = allExercises,
            onDismiss = { showAdhocDialog.value = false },
            onStart = { selection, adhocName ->
                selectedExercises = selection
                routineStarted = true
                routineFinished = false
                currentRoutineName = adhocName.ifBlank { "Rutina Rápida" }
                currentIndex = 0
                currentSet = 1
                phase = 0
                timeLeft = globalWait
                waitingToStart = true
                completedExercisesIndices.clear()
                showAdhocDialog.value = false
            }
        )
    }

    if (showNextExerciseSelector) {
        Dialog(onDismissRequest = { /* Forzar selección */ }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20)),
                border = BorderStroke(1.dp, Color(0xFF00FF00).copy(0.5f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Siguiente ejercicio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val available = selectedExercises.indices.filter { !completedExercisesIndices.contains(it) }
                        itemsIndexed(available) { _, originalIndex ->
                            val ex = selectedExercises[originalIndex]
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    currentIndex = originalIndex
                                    currentSet = 1
                                    phase = 0
                                    timeLeft = globalWait
                                    waitingToStart = true
                                    isPaused = false
                                    showNextExerciseSelector = false
                                },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(ex.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${ex.sets} series x ${ex.reps} reps — ${ex.weight}kg", color = Color(0xFF00FF00), fontSize = 12.sp)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdhocRoutineDialog(
    allExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onStart: (List<Exercise>, String) -> Unit
) {
    var selectedList by remember { mutableStateOf(setOf<Exercise>()) }
    var routineName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C20),
        title = { Text("Configurar Rutina Rápida", color = Color.White) },
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Nombre (opcional)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Text("Selecciona los ejercicios:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(allExercises) { ex ->
                        val isSelected = selectedList.contains(ex)
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                selectedList = if (isSelected) selectedList - ex else selectedList + ex
                            },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF00FF00).copy(0.2f) else Color.White.copy(0.05f)),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF00FF00)) else null
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isSelected, onCheckedChange = { selectedList = if (it) selectedList + ex else selectedList - ex })
                                Column {
                                    Text(ex.name, color = Color.White)
                                    Text("${ex.sets}x${ex.reps} — ${ex.weight}kg", color = Color(0xFF00FF00), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { if (selectedList.isNotEmpty()) onStart(selectedList.toList(), routineName) },
                enabled = selectedList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), disabledContainerColor = Color.Gray)
            ) { Text("EMPEZAR", color = Color.Black) } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) } }
    )
}

fun formatWeight(weight: Int): String {
    return if (weight > 0) "$weight kg" else "Peso corporal"
}

@Composable
fun TrainingUI(
    exercise: Exercise,
    currentIndex: Int,
    totalExercises: Int,
    currentSet: Int,
    phase: Int,
    timeLeft: Int,
    isPaused: Boolean,
    allExercises: List<Exercise>,
    globalWait: Int,
    completedIndices: List<Int>,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onSkipExercise: () -> Unit,
    onStop: () -> Unit
) {
    val neonGreen = Color(0xFFC6FF00)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicador superior: EJERCICIO 1 DE 5
        Surface(
            color = Color.Black.copy(0.3f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, neonGreen.copy(0.5f))
        ) {
            Text(
                "EJERCICIO ${completedIndices.size + 1} DE $totalExercises",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = neonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))
        
        Text(exercise.name.uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${exercise.sets}", color = neonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(" SERIES X ", color = Color.Gray, fontSize = 14.sp)
            Text("${exercise.reps}", color = neonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(" REPS — ", color = Color.Gray, fontSize = 14.sp)
            Text("${exercise.weight}KG", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        // Timer Circular Estilo Imagen
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            val phaseColor = when (phase) { 0 -> Color.Yellow; 1 -> neonGreen; else -> Color(0xFF00BFFF) }
            val totalPhaseTime = if (phase == 1) exercise.duration.toFloat() else if (phase == 2) exercise.rest.toFloat() else globalWait.toFloat()
            val progress = if (totalPhaseTime > 0) timeLeft / totalPhaseTime else 0f
            
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = phaseColor,
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(0.1f)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(when(phase){0->"PREPÁRATE"; 1->"¡DALE!"; else->"DESCANSO"}, color = phaseColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$timeLeft", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
                Text("SEGUNDOS", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Surface(color = Color.Black.copy(0.3f), shape = RoundedCornerShape(16.dp)) {
            Text("SERIE $currentSet DE ${exercise.sets}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = neonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        Text("PROGRESO DE LA RUTINA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(allExercises) { index, ex ->
                val isCompleted = completedIndices.contains(index)
                val isCurrent = index == currentIndex
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(if (isCurrent) 0.1f else 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isCurrent) BorderStroke(1.dp, neonGreen.copy(0.4f)) else BorderStroke(0.5.dp, Color.White.copy(0.1f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Círculo de estado
                        Box(Modifier.size(28.dp).background(if (isCompleted) neonGreen else Color.White.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            if (isCompleted) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            else Text("${index + 1}", color = Color.White, fontSize = 12.sp)
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${ex.sets}×${ex.reps} — ${ex.weight}kg", color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        if (isCurrent) {
                            Surface(color = neonGreen.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("ACTUAL", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = neonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Botones Inferiores Estilo Imagen
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onTogglePause,
                colors = ButtonDefaults.buttonColors(containerColor = neonGreen),
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPaused) "REANUDAR" else "PAUSAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            
            // Botones de acción rápida en fila para evitar superposición
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onFinish, modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape)) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
                IconButton(onClick = onSkipExercise, modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape)) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White)
                }
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                modifier = Modifier.width(80.dp).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color.Red.copy(0.5f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Stop, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Text("FIN", color = Color.White.copy(0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
