package com.example.Gym_App.ui.screens


import androidx.compose.foundation.Image
import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.*
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
import com.example.Gym_App.model.ExerciseEntity
import com.example.Gym_App.model.Routine
import com.example.Gym_App.model.WorkoutHistoryEntity
import com.example.Gym_App.ui.components.*
import com.example.Gym_App.ui.components.BottomNavBar
import com.example.Gym_App.viewmodel.GymViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyRow
import com.example.Gym_App.utils.ImageStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import java.io.File




@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    
    val routinesEntity by viewModel.routines.collectAsState()
    val allExercisesEntity by viewModel.exercises.collectAsState()
    val workoutDates by viewModel.workoutDates.collectAsState()
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.duration, it.muscleGroup, it.updateReminderDays, it.lastUpdateDate) }
    val routinesList = routinesEntity.map { Routine(it.name, it.exerciseNames.split(","), it.imageId, it.customImageUri, it.assignedDays?.split(",")?.mapNotNull { d -> d.toIntOrNull() } ?: emptyList()) }

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

    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var editingMuscleForImage by remember { mutableStateOf<String?>(null) }
    var showMuscleImagePicker by remember { mutableStateOf(false) }
    var showExerciseForm by remember { mutableStateOf(false) }
    val muscleGroups = listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core")

    val imageOptions = listOf(
        "pecho" to "Pecho", "espalda" to "Espalda", "biceps" to "Bíceps",
        "triceps" to "Tríceps", "hombros" to "Hombros", "cuadriceps" to "Piernas",
        "abdominales" to "Abs", "core" to "Core"
    )

    val muscleImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = ImageStorage.saveImage(context, it)
            if (path != null) {
                editingMuscleForImage?.let { m ->
                    prefs.edit { putString("muscle_img_$m", path) }
                }
            }
        }
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var currentSet by rememberSaveable { mutableIntStateOf(1) }
    var timeLeft by rememberSaveable { mutableIntStateOf(0) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var phase by rememberSaveable { mutableIntStateOf(0) } 
    
    val globalWait = prefs.getInt("globalWait", 20)
    
    val selectedTopColor = prefs.getInt("trainingGradientTopColor", Color.Black.toArgb())
    val selectedBottomColor = prefs.getInt("trainingGradientBottomColor", Color(0xFF424242).toArgb())
    val fontColor1 = Color(prefs.getInt("mainFontColor1", Color.White.toArgb()))
    val fontColor2 = Color(prefs.getInt("mainFontColor2", Color(0xFFC6FF00).toArgb()))
    
    val isWaitingForUser by viewModel.isWaitingForUser.collectAsState()

    // Sonidos y vibraciones
    @Suppress("DEPRECATION")
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    // Sensor de ritmo cardíaco para descanso inteligente
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val heartRateSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }
    val bpmHistory = remember { mutableStateListOf<Float>() }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                    val bpm = event.values[0]
                    bpmHistory.add(bpm)
                    if (bpmHistory.size > 10) bpmHistory.removeAt(0)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    DisposableEffect(routineStarted) {
        val activity = context as? Activity
        if (routineStarted) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (heartRateSensor != null) {
                sensorManager.registerListener(sensorListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            sensorManager.unregisterListener(sensorListener)
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
                    1 -> { phase = 2; timeLeft = currentEx.rest + if (bpmHistory.isNotEmpty() && bpmHistory.average() > 150) 30 else 0 } // Descanso inteligente
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

                // Sonidos y vibraciones para transiciones
                if (phase == 1) { // Iniciar trabajo
                    // try {
                    //     MediaPlayer.create(context, R.raw.beep_work).start()
                    // } catch (_: Exception) { }
                    vibrator.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else if (phase == 2) { // Iniciar descanso
                    // try {
                    //     MediaPlayer.create(context, R.raw.beep_rest).start()
                    // } catch (_: Exception) { }
                    vibrator.vibrate(VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE))
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

                    // HEADER CON PROGRESO Y HEATMAP (Animado)
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        HomeHeader(workoutDates = workoutDates)
                    }

                    Spacer(Modifier.height(8.dp))

                    // CARD PRINCIPAL: TE TOCA HOY o CONTINUAR
                    val workoutDoneToday = workoutDates.contains(java.time.LocalDate.now())
                    val isContinuing = selectedExercises.isNotEmpty() && completedExercisesIndices.isNotEmpty() && completedExercisesIndices.size < selectedExercises.size

                    Box(Modifier.animateContentSize()) {
                    Box(Modifier.animateContentSize()) {
                        if (isContinuing) {
                            ContinueWorkoutCard(
                                routineName = currentRoutineName,
                                completedCount = completedExercisesIndices.size,
                                totalCount = selectedExercises.size,
                                onClick = { routineStarted = true; waitingToStart = true }
                            )
                        } else {
                            val today = java.time.LocalDate.now().dayOfWeek.value // 1 (Monday) to 7 (Sunday)
                            val routineForToday = routinesList.find { it.assignedDays.contains(today) } ?: routinesList.firstOrNull()

                            TodayWorkoutCard(
                                routine = routineForToday,
                                isDone = workoutDoneToday,
                                allExercises = allExercises,
                                onStart = { routine ->
                                    if (routine != null) {
                                        val routineExercises = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                                        if (routineExercises.isNotEmpty()) {
                                            selectedExercises = routineExercises
                                            currentIndex = 0
                                            currentSet = 1
                                            phase = 0
                                            timeLeft = globalWait
                                            routineStarted = true
                                            waitingToStart = true
                                            routineFinished = false
                                            currentRoutineName = routine.name
                                            completedExercisesIndices.clear()
                                        }
                                    }
                                }
                            )
                        }
                    }
                    }

                    Spacer(Modifier.height(24.dp))

                    // SECCIÓN DE MIS RUTINAS (AHORA SECUNDARIA)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mis Rutinas", color = fontColor1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.size(24.dp).background(fontColor2, CircleShape).clickable { navController.navigate("rutinas") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // GRID DE RUTINAS (3 COLUMNAS)
                    val expandedRoutines = remember { mutableStateMapOf<String, Boolean>() }
                    Column(Modifier.fillMaxWidth()) {
                        routinesList.chunked(3).forEach { rowRoutines ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowRoutines.forEach { routine ->
                                    val routineExercises = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                                    val isExpanded = expandedRoutines[routine.name] ?: false

                                    Column(Modifier.weight(1f)) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().height(100.dp).clickable {
                                                expandedRoutines[routine.name] = !isExpanded
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20))
                                        ) {
                                            Box(Modifier.fillMaxSize()) {
                                                // Imagen de fondo
                                                if (routine.customImageUri != null) {
                                                    val bitmap = remember(routine.customImageUri) {
                                                        try {
                                                            context.contentResolver.openInputStream(routine.customImageUri.toUri())?.use {
                                                                BitmapFactory.decodeStream(it)
                                                            }
                                                        } catch (_: Exception) { null }
                                                    }
                                                    bitmap?.let {
                                                        androidx.compose.foundation.Image(
                                                            painter = BitmapPainter(it.asImageBitmap()),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                            alpha = 0.4f
                                                        )
                                                    }
                                                } else {
                                                    val routineResId = context.resources.getIdentifier(routine.imageId, "drawable", context.packageName)
                                                    if (routineResId != 0) {
                                                        androidx.compose.foundation.Image(
                                                            painter = androidx.compose.ui.res.painterResource(id = routineResId),
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                            alpha = 0.4f
                                                        )
                                                    }
                                                }

                                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))
                                                
                                                Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                        // Botón Editar
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .background(Color.Black.copy(0.6f), CircleShape)
                                                                .clickable { navController.navigate("rutinas?edit=${routine.name}") },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        }
                                                        Spacer(Modifier.width(6.dp))
                                                        // Botón Play
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .background(fontColor2, CircleShape)
                                                                .clickable {
                                                                    if (routineExercises.isNotEmpty()) {
                                                                        selectedExercises = routineExercises
                                                                        currentIndex = 0
                                                                        currentSet = 1
                                                                        phase = 0
                                                                        timeLeft = globalWait
                                                                        routineStarted = true
                                                                        waitingToStart = true
                                                                        routineFinished = false
                                                                        currentRoutineName = routine.name
                                                                        completedExercisesIndices.clear()
                                                                    }
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                    Column {
                                                        Text(routine.name, color = fontColor1, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 11.sp)
                                                        if (routine.assignedDays.isNotEmpty()) {
                                                            val dayLabels = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
                                                            Text(
                                                                text = routine.assignedDays.sorted().joinToString(", ") { dayLabels[it - 1] },
                                                                color = fontColor2,
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Rellenar espacio si la fila no está completa
                                repeat(3 - rowRoutines.size) { Spacer(Modifier.weight(1f)) }
                            }
                            
                            // Mostrar detalles de la rutina si alguna está expandida en esta fila
                            rowRoutines.forEach { routine ->
                                val isExpanded = expandedRoutines[routine.name] ?: false
                                AnimatedVisibility(visible = isExpanded) {
                                    val routineExercises = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                                        Text(routine.name.uppercase(), color = fontColor2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        routineExercises.forEach { ex ->
                                            Text("• ${ex.name}", color = fontColor1, fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // SECCIÓN DE MIS EJERCICIOS (GRID 4 COLUMNAS)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Mis Ejercicios", color = fontColor1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(24.dp).background(fontColor2, CircleShape).clickable { navController.navigate("ejercicios") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    val grouped = allExercises.groupBy { it.muscleGroup }
                    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
                    
                    Column(Modifier.fillMaxWidth()) {
                        grouped.keys.toList().chunked(4).forEach { rowMuscles ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowMuscles.forEach { muscle ->
                                    val muscleExercises = grouped[muscle] ?: emptyList()
                                    val isExpanded = expandedGroups[muscle] ?: false
                                    
                                    val customImgPath = prefs.getString("muscle_img_$muscle", null)
                                    val isRes = customImgPath?.startsWith("res:") == true
                                    val bitmap = remember(customImgPath) { 
                                        if (customImgPath != null && !isRes) BitmapFactory.decodeFile(customImgPath) else null 
                                    }
                                    
                                    val resId = if (isRes) {
                                        context.resources.getIdentifier(customImgPath!!.removePrefix("res:"), "drawable", context.packageName)
                                    } else {
                                        val imageName = muscle.lowercase().replace("í", "i").replace("é", "e").replace("á", "a").replace("ó", "o").replace("ú", "u")
                                        context.resources.getIdentifier(imageName, "drawable", context.packageName)
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f).height(80.dp).clickable { expandedGroups[muscle] = !isExpanded },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20))
                                    ) {
                                        Box(Modifier.fillMaxSize()) {
                                            if (bitmap != null) {
                                                androidx.compose.foundation.Image(
                                                    painter = BitmapPainter(bitmap.asImageBitmap()),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    alpha = 0.4f
                                                )
                                            } else if (resId != 0) {
                                                androidx.compose.foundation.Image(
                                                    painter = androidx.compose.ui.res.painterResource(id = resId),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    alpha = 0.3f
                                                )
                                            }
                                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))
                                            
                                            // Icono para cambiar imagen
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                                    .background(Color.Black.copy(0.5f), CircleShape)
                                                    .clickable { editingMuscleForImage = muscle; showMuscleImagePicker = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(10.dp))
                                            }

                                            Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(muscle.uppercase(), color = fontColor1, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1, textAlign = TextAlign.Center)
                                                Text("${muscleExercises.size}", color = fontColor2, fontSize = 8.sp)
                                            }
                                        }
                                    }
                                }
                                repeat(4 - rowMuscles.size) { Spacer(Modifier.weight(1f)) }
                            }

                            // Detalles del grupo muscular
                            rowMuscles.forEach { muscle ->
                                val isExpanded = expandedGroups[muscle] ?: false
                                AnimatedVisibility(visible = isExpanded) {
                                    val muscleExercises = grouped[muscle] ?: emptyList()
                                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(10.dp)).padding(6.dp)) {
                                        Text(muscle.uppercase(), color = fontColor2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        muscleExercises.forEach { ex ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(ex.name, color = fontColor1, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                
                                                // Botón Editar Ejercicio
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(Color.White.copy(0.1f), CircleShape)
                                                        .clickable { editingExercise = ex; showExerciseForm = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Edit, null, tint = Color.Gray.copy(0.8f), modifier = Modifier.size(10.dp))
                                                }
                                                Spacer(Modifier.width(4.dp))
                                                // Botón Play Ejercicio
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(fontColor2, CircleShape)
                                                        .clickable {
                                                            selectedExercises = listOf(ex)
                                                            currentIndex = 0
                                                            currentSet = 1
                                                            phase = 0
                                                            timeLeft = globalWait
                                                            routineStarted = true
                                                            waitingToStart = true
                                                            routineFinished = false
                                                            currentRoutineName = ex.name
                                                            completedExercisesIndices.clear()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            } else if (routineFinished) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = fontColor2, modifier = Modifier.size(100.dp))
                    Spacer(Modifier.height(24.dp))
                    Text("¡TRABAJO COMPLETADO!", color = fontColor1, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(currentRoutineName, color = fontColor2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("Has finalizado todos los ejercicios. ¡Sigue así!", color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(48.dp))
                    Button(
                        onClick = { 
                            routineStarted = false
                            routineFinished = false
                            completedExercisesIndices.clear()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = fontColor2),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VOLVER AL MENÚ", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (waitingToStart) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿PREPARADO?", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(currentRoutineName, color = fontColor1, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    
                    // Resalte visual para el ejercicio seleccionado
                    val currentEx = selectedExercises[currentIndex]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = fontColor2.copy(0.1f)),
                        border = BorderStroke(2.dp, fontColor2)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            // CORRECCIÓN: Etiqueta dinámica
                            Text(
                                text = if (completedExercisesIndices.isEmpty()) "PRIMER EJERCICIO:" else "PRÓXIMO EJERCICIO:",
                                color = fontColor2, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Black
                            )
                            Text(currentEx.name, color = fontColor1, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("${currentEx.sets} series x ${currentEx.reps} reps — ${currentEx.weight}kg", color = fontColor2, fontSize = 14.sp)
                        }
                    }

                    Button(onClick = { waitingToStart = false }, modifier = Modifier.size(130.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = fontColor2)) {
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
                                        isSelected -> fontColor2.copy(0.15f)
                                        isCompleted -> Color.Gray.copy(0.1f)
                                        else -> Color.White.copy(0.05f)
                                    }
                                ),
                                border = if (isSelected) BorderStroke(1.dp, fontColor2) else null
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            ex.name, 
                                            color = when {
                                                isSelected -> fontColor2
                                                isCompleted -> Color.Gray
                                                else -> fontColor1
                                            }, 
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        Text("${ex.sets} series x ${ex.reps} reps — ${ex.weight}kg", color = if (isSelected) fontColor2.copy(0.7f) else Color.Gray, fontSize = 12.sp)
                                    }
                                    if (isSelected) Icon(Icons.Default.Check, null, tint = fontColor2)
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
                    routineImageUri = routinesList.find { it.name == currentRoutineName }?.customImageUri,
                    routineImageId = routinesList.find { it.name == currentRoutineName }?.imageId,
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
                    onStop = { showStopConfirmation = true },
                    onUpdateExercise = { name, sets, reps, weight ->
                        selectedExercises = selectedExercises.map { if (it.name == name) it.copy(sets = sets, reps = reps, weight = weight) else it }
                        viewModel.updateExercise(name, sets, reps, weight)
                    }
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

    if (showMuscleImagePicker && editingMuscleForImage != null) {
        AlertDialog(
            onDismissRequest = { showMuscleImagePicker = false },
            title = { Text("Imagen para $editingMuscleForImage", color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { muscleImageLauncher.launch("image/*"); showMuscleImagePicker = false }) {
                        Text("ELEGIR DE GALERÍA", color = fontColor2)
                    }
                    TextButton(onClick = { 
                        prefs.edit { remove("muscle_img_$editingMuscleForImage") }
                        showMuscleImagePicker = false 
                    }) {
                        Text("RESTAURAR POR DEFECTO", color = Color.Red)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Predefinidas:", color = Color.Gray, fontSize = 12.sp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(imageOptions) { option ->
                            val optResId = context.resources.getIdentifier(option.first, "drawable", context.packageName)
                            Card(
                                modifier = Modifier.size(70.dp).clickable {
                                    prefs.edit { putString("muscle_img_$editingMuscleForImage", "res:${option.first}") }
                                    showMuscleImagePicker = false
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box {
                                    if (optResId != 0) {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = optResId), 
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop, 
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        option.second, 
                                        color = Color.White, 
                                        fontSize = 8.sp, 
                                        modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(0.7f)).fillMaxWidth(), 
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMuscleImagePicker = false }) { Text("CERRAR", color = Color.Gray) } },
            containerColor = Color(0xFF1A1C20)
        )
    }

    if (showExerciseForm) {
        ExerciseForm(
            isEditMode = editingExercise != null,
            initial = editingExercise,
            existingMuscles = muscleGroups,
            onCancel = { showExerciseForm = false; editingExercise = null },
            onDelete = {
                editingExercise?.let { viewModel.deleteExercise(it.name) }
                showExerciseForm = false; editingExercise = null
            },
            onSave = { updatedEx ->
                val original = allExercisesEntity.find { it.name == (editingExercise?.name ?: "") }
                val now = System.currentTimeMillis()
                val lastUpdate = if (original != null && (original.weight != updatedEx.weight || original.reps != updatedEx.reps || original.sets != updatedEx.sets)) now else (original?.lastUpdateDate ?: 0L)

                viewModel.addExercise(ExerciseEntity(
                    updatedEx.name, updatedEx.reps, updatedEx.sets,
                    updatedEx.weight, updatedEx.rest, updatedEx.duration, updatedEx.muscleGroup,
                    original?.equipmentType ?: "Peso Corporal",
                    updateReminderDays = updatedEx.updateReminderDays,
                    lastUpdateDate = lastUpdate
                ))
                showExerciseForm = false; editingExercise = null
            }
        )
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
    routineImageUri: String?,
    routineImageId: String?,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onSkipExercise: () -> Unit,
    onStop: () -> Unit,
    onUpdateExercise: (String, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val fontColor1 = Color(prefs.getInt("mainFontColor1", Color.White.toArgb()))
    val fontColor2 = Color(prefs.getInt("mainFontColor2", Color(0xFFC6FF00).toArgb()))

    var showEditDialog by remember { mutableStateOf(false) }
    var editedSets by remember { mutableStateOf(exercise.sets.toString()) }
    var editedReps by remember { mutableStateOf(exercise.reps.toString()) }
    var editedWeight by remember { mutableStateOf(exercise.weight.toString()) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicador superior: EJERCICIO 1 DE 5
        Surface(
            color = Color.Black.copy(0.3f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, fontColor2.copy(0.5f))
        ) {
            Text(
                "EJERCICIO ${completedIndices.size + 1} DE $totalExercises",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = fontColor2,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))
        
        Text(exercise.name.uppercase(), color = fontColor1, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.clickable {
            showEditDialog = true
            editedSets = exercise.sets.toString()
            editedReps = exercise.reps.toString()
            editedWeight = exercise.weight.toString()
        })

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
            showEditDialog = true
            editedSets = exercise.sets.toString()
            editedReps = exercise.reps.toString()
            editedWeight = exercise.weight.toString()
        }) {
            Text("${exercise.sets}", color = fontColor2, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(" SERIES X ", color = Color.Gray, fontSize = 14.sp)
            Text("${exercise.reps}", color = fontColor2, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(" REPS — ", color = Color.Gray, fontSize = 14.sp)
            Text("${exercise.weight}KG", color = fontColor2, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        // Timer Circular Estilo Imagen
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            val phaseColor = when (phase) { 0 -> Color.Yellow; 1 -> fontColor2; else -> Color(0xFF00BFFF) }
            val totalPhaseTime = if (phase == 1) exercise.duration.toFloat() else if (phase == 2) exercise.rest.toFloat() else globalWait.toFloat()
            val progress = if (totalPhaseTime > 0) timeLeft / totalPhaseTime else 0f
            
            // Imagen de fondo circular
            CircleShape.let { shape ->
                Surface(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    shape = shape,
                    color = Color.Black.copy(0.3f)
                ) {
                    if (routineImageUri != null) {
                        val bitmap = remember(routineImageUri) {
                            try {
                                context.contentResolver.openInputStream(routineImageUri.toUri())?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            } catch (_: Exception) { null }
                        }
                        bitmap?.let {
                            androidx.compose.foundation.Image(
                                painter = BitmapPainter(it.asImageBitmap()),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = 0.3f
                            )
                        }
                    } else if (routineImageId != null) {
                        val resId = context.resources.getIdentifier(routineImageId, "drawable", context.packageName)
                        if (resId != 0) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = 0.3f
                            )
                        }
                    }
                }
            }

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = phaseColor,
                strokeWidth = 4.dp,
                trackColor = Color.White.copy(0.1f)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(300)), exit = fadeOut()) {
                    Text(when(phase){0->"PREPÁRATE"; 1->"¡DALE!"; else->"DESCANSO"}, color = phaseColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                AnimatedVisibility(visible = true, enter = scaleIn(animationSpec = tween(300)), exit = scaleOut()) {
                    Text("$timeLeft", color = fontColor1, fontSize = 64.sp, fontWeight = FontWeight.Black)
                }
                Text("SEGUNDOS", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Surface(color = Color.Black.copy(0.3f), shape = RoundedCornerShape(16.dp)) {
            Text("SERIE $currentSet DE ${exercise.sets}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = fontColor2, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    border = if (isCurrent) BorderStroke(1.dp, fontColor2.copy(0.4f)) else BorderStroke(0.5.dp, Color.White.copy(0.1f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Círculo de estado
                        Box(Modifier.size(28.dp).background(if (isCompleted) fontColor2 else Color.White.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            } else {
                                Text("${index + 1}", color = fontColor1, fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, color = fontColor1, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${ex.sets}×${ex.reps} — ${ex.weight}kg", color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        if (isCurrent) {
                            Surface(color = fontColor2.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("ACTUAL", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = fontColor2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                colors = ButtonDefaults.buttonColors(containerColor = fontColor2),
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
                IconButton(onClick = { onSkipExercise() }, modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape)) {
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

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("Editar Ejercicio", color = Color.White) },
            text = {
                Column {
                    // Series
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("Series", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = editedSets,
                            onValueChange = { editedSets = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { editedSets = (editedSets.toIntOrNull()?.let { maxOf(1, it - 1) } ?: 1).toString() }) {
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
                                    }
                                    IconButton(onClick = { editedSets = (editedSets.toIntOrNull()?.let { it + 1 } ?: 1).toString() }) {
                                        Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Gray)
                                    }
                                }
                            }
                        )
                    }
                    // Reps
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("Repeticiones", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = editedReps,
                            onValueChange = { editedReps = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { editedReps = (editedReps.toIntOrNull()?.let { maxOf(1, it - 1) } ?: 1).toString() }) {
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
                                    }
                                    IconButton(onClick = { editedReps = (editedReps.toIntOrNull()?.let { it + 1 } ?: 1).toString() }) {
                                        Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Gray)
                                    }
                                }
                            }
                        )
                    }
                    // Peso
                    Column(Modifier.fillMaxWidth()) {
                        Text("Peso (kg)", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = editedWeight,
                            onValueChange = { editedWeight = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { editedWeight = (editedWeight.toIntOrNull()?.let { maxOf(0, it - 1) } ?: 0).toString() }) {
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
                                    }
                                    IconButton(onClick = { editedWeight = (editedWeight.toIntOrNull()?.let { it + 1 } ?: 0).toString() }) {
                                        Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Gray)
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sets = editedSets.toIntOrNull() ?: exercise.sets
                    val reps = editedReps.toIntOrNull() ?: exercise.reps
                    val weight = editedWeight.toIntOrNull() ?: exercise.weight
                    onUpdateExercise(exercise.name, sets, reps, weight)
                    showEditDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00))) {
                    Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }
}
