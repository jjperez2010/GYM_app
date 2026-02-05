package com.example.appdeprueba

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdeprueba.ui.theme.AppDePruebaTheme
import kotlinx.coroutines.delay

// --- MODELOS ---
data class Exercise(val name: String, val reps: Int, val sets: Int, val weight: Int, val rest: Int, val muscleGroup: String)
data class Routine(val name: String, val exerciseNames: List<String>)

// --- DATOS POR DEFECTO ---
val defaultExercisesTable = listOf(
    // Nombre, Zona, H Nov, H Int, H Av, M Nov, M Int, M Av
    listOf("Sentadilla libre", "Piernas", 40, 70, 100, 25, 45, 70),
    listOf("Prensa de piernas", "Piernas", 80, 140, 220, 60, 110, 170),
    listOf("Extensión de piernas", "Cuádriceps", 25, 45, 70, 15, 30, 50),
    listOf("Curl femoral", "Isquiotibiales", 20, 40, 65, 15, 30, 45),
    listOf("Peso muerto rumano", "Posterior", 45, 80, 120, 30, 55, 85),
    listOf("Elevación de talones", "Gemelos", 50, 90, 140, 35, 65, 100),
    listOf("Press banca plano", "Pecho", 40, 70, 110, 25, 45, 70),
    listOf("Press inclinado", "Pecho sup.", 35, 60, 95, 20, 40, 60),
    listOf("Aperturas mancuernas", "Pecho", 8, 14, 22, 5, 10, 16),
    listOf("Fondos paralelas", "Pecho/Tríceps", 0, 10, 25, -1, 0, 10),
    listOf("Dominadas", "Espalda", -1, 0, 20, -1, -1, 0),
    listOf("Jalón al pecho", "Dorsal", 40, 60, 85, 30, 45, 65),
    listOf("Remo con barra", "Espalda", 40, 70, 110, 25, 45, 70),
    listOf("Remo mancuerna", "Espalda", 20, 32, 45, 12, 22, 32),
    listOf("Press militar", "Hombros", 30, 45, 70, 18, 30, 45),
    listOf("Elevaciones laterales", "Hombro lat.", 6, 10, 16, 4, 7, 12),
    listOf("Pájaros", "Hombro post.", 6, 10, 16, 4, 7, 12),
    listOf("Encogimientos", "Trapecio", 30, 50, 80, 20, 35, 55),
    listOf("Curl barra", "Bíceps", 20, 35, 55, 12, 22, 35),
    listOf("Curl alternado", "Bíceps", 10, 16, 24, 6, 10, 16),
    listOf("Curl martillo", "Bíceps", 12, 18, 26, 7, 12, 18),
    listOf("Press francés", "Tríceps", 20, 35, 55, 12, 22, 35),
    listOf("Tríceps polea", "Tríceps", 20, 35, 55, 12, 22, 35),
    listOf("Fondos banco", "Tríceps", 0, 10, 25, -1, 0, 10),
    listOf("Crunch polea", "Core", 15, 30, 50, 10, 20, 35),
    listOf("Elevación de piernas", "Core", 0, 0, 10, 0, 0, 5),
    listOf("Hip thrust", "Glúteos", 50, 90, 140, 35, 65, 100),
    listOf("Zancadas", "Piernas", 10, 20, 35, 6, 12, 20),
    listOf("Sentadilla búlgara", "Piernas", 10, 20, 35, 6, 12, 20),
    listOf("Farmer walk", "Core/Trap", 20, 35, 55, 12, 22, 35),
    listOf("Face pull", "Hombro post.", 20, 30, 45, 12, 20, 30),
    listOf("Remo al mentón", "Trap/Hombro", 20, 35, 55, 12, 22, 35),
    listOf("Press pecho máquina", "Pecho", 40, 70, 110, 25, 45, 70)
)

fun formatWeight(weight: Int): String {
    return when {
        weight == 0 -> "Corp."
        weight < 0 -> "Asist."
        else -> "${weight}kg"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDePruebaTheme {
                var showWelcome by remember { mutableStateOf(true) }
                val navController = rememberNavController()
                if (showWelcome) { WelcomeScreen { showWelcome = false } }
                else {
                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") { MainScreen(navController) }
                        composable("ejercicios") { ExercisesScreen(navController) }
                        composable("rutinas") { RoutinesScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    val pronoun = prefs.getString("userPronoun", "Él")
    val name = prefs.getString("userName", "") ?: ""
    val greetingBase = when(pronoun) { "Ella" -> "Bienvenida"; "Elle" -> "Bienvenide"; else -> "Bienvenido" }
    val welcomeText = if (name.isNotBlank()) "¡$greetingBase $name!\n¡Vamos a entrenar!" else "¡$greetingBase!\n¡Vamos a entrenar!"
    LaunchedEffect(Unit) { delay(2500); onFinished() }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = welcomeText, color = Color(0xFF00FF00), fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 40.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val routinesList = remember { mutableStateListOf<Routine>() }
    val allExercises = remember { mutableStateListOf<Exercise>() }
    var routineStarted by remember { mutableStateOf(false) }
    var selectedExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var showAdhocDialog by remember { mutableStateOf(false) }
    var showQuickEdit by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) } // 0=Espera, 1=Ejercicio, 2=Descanso
    var globalWait by remember { mutableIntStateOf(20) }
    val gradientMain = Brush.verticalGradient(0.0f to Color.Black, 0.75f to Color.Black, 1.0f to Color(0xFF00008B))

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        globalWait = prefs.getInt("globalWait", 20)
        val savedEx = prefs.getString("exercises", "") ?: ""
        if (savedEx.isEmpty()) {
            val gender = prefs.getString("userGender", "Masculino") ?: "Masculino"
            val level = prefs.getString("userLevel", "Novato") ?: "Novato"
            val colIdx = when {
                gender == "Femenino" -> when(level) { "Intermedio" -> 6; "Avanzado" -> 7; else -> 5 }
                else -> when(level) { "Intermedio" -> 3; "Avanzado" -> 4; else -> 2 }
            }
            defaultExercisesTable.forEach { row ->
                allExercises.add(Exercise(row[0] as String, 12, 3, row[colIdx] as Int, 120, row[1] as String))
            }
            val data = allExercises.joinToString("|") { "${it.name},${it.reps},${it.sets},${it.weight},${it.rest},${it.muscleGroup}" }
            prefs.edit().putString("exercises", data).apply()
        } else {
            savedEx.split("|").forEach {
                val p = it.split(",")
                if (p.size == 6) allExercises.add(Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5]))
            }
        }
        prefs.getString("routines", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach {
            val parts = it.split(":"); if (parts.size == 2) routinesList.add(Routine(parts[0], parts[1].split(",")))
        }
    }

    LaunchedEffect(routineStarted, phase, isPaused, timeLeft) {
        if (routineStarted && !isPaused && timeLeft > 0) { delay(1000); timeLeft-- }
        else if (routineStarted && !isPaused && timeLeft <= 0) {
            val currentEx = selectedExercises.getOrNull(currentIndex) ?: return@LaunchedEffect
            when (phase) {
                0 -> { phase = 1; timeLeft = 60 }
                1 -> { phase = 2; timeLeft = currentEx.rest }
                2 -> {
                    if (currentSet < currentEx.sets) { currentSet++; phase = 1; timeLeft = 60 }
                    else if (currentIndex < selectedExercises.size - 1) { currentIndex++; currentSet = 1; phase = 0; timeLeft = globalWait }
                    else { routineStarted = false; Toast.makeText(context, "Rutina Completada", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    Scaffold(bottomBar = { BottomNavBar(navController, "menu") }) { innerPadding ->
        Box(Modifier.fillMaxSize().background(brush = gradientMain).padding(innerPadding)) {
            if (!routineStarted) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MenuButton("Rutina Rápida", Modifier.fillMaxWidth(), bColor = Color(0xFFFFA500), cColor = Color(0xFFFFA500).copy(0.1f)) { showAdhocDialog = true }
                    Spacer(Modifier.height(24.dp))
                    Text("Mis Rutinas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Box(Modifier.weight(0.45f)) {
                        LazyColumn {
                            items(routinesList) { routine ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    val exList = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                                    if (exList.isNotEmpty()) { selectedExercises = exList; currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true }
                                }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.4f))) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFBB86FC)); Spacer(Modifier.width(12.dp)); Text(routine.name, color = Color.White, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Ejercicios Sueltos", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Box(Modifier.weight(0.55f)) {
                        val grouped = allExercises.groupBy { it.muscleGroup }
                        LazyColumn {
                            grouped.forEach { (muscle, exercises) ->
                                stickyHeader {
                                    Text(muscle, modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.8f)).padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF00FF00), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                items(exercises) { exercise ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                        selectedExercises = listOf(exercise); currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true
                                    }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color(0xFF00FF00).copy(0.3f))) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FitnessCenter, null, tint = Color(0xFF00FF00)); Spacer(Modifier.width(12.dp))
                                            Column { Text(exercise.name, color = Color.White, fontSize = 16.sp); Text("${exercise.sets}x${exercise.reps} - ${formatWeight(exercise.weight)}", color = Color.Gray, fontSize = 12.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                TrainingUI(selectedExercises[currentIndex], currentIndex, selectedExercises.size, currentSet, phase, timeLeft, isPaused, selectedExercises, globalWait, { isPaused = !isPaused }, { routineStarted = false }, { showQuickEdit = true })
            }
        }
    }

    if (showQuickEdit) {
        AlertDialog(onDismissRequest = { showQuickEdit = false }, containerColor = Color(0xFF1A1C20), title = { Text("Ajustar hoy", color = Color.White) }, text = {
            ExerciseForm(false, selectedExercises[currentIndex], emptyList(), { showQuickEdit = false }, {}, { 
                val newList = selectedExercises.toMutableList(); newList[currentIndex] = it; selectedExercises = newList; showQuickEdit = false
            })
        }, confirmButton = {})
    }

    if (showAdhocDialog) {
        val selection = remember { mutableStateListOf<Exercise>() }
        var name by remember { mutableStateOf("") }; var save by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAdhocDialog = false }, containerColor = Color(0xFF1A1C20), title = { Text("Rutina Adhoc", color = Color.White) }, text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(save, { save = it }); Text("Guardar", color = Color.White) }
                val grouped = allExercises.groupBy { it.muscleGroup }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    grouped.forEach { (muscle, exercises) ->
                        stickyHeader {
                            Text(muscle, modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2C32)).padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF00FF00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        items(exercises) { ex ->
                            val sIdx = selection.indexOf(ex)
                            Row(Modifier.fillMaxWidth().clickable { if (sIdx != -1) selection.removeAt(sIdx) else selection.add(ex) }.padding(8.dp)) {
                                Box(Modifier.size(20.dp).background(if (sIdx != -1) Color(0xFF00FF00) else Color.Transparent, CircleShape).border(1.dp, Color.Gray, CircleShape), contentAlignment = Alignment.Center) { if (sIdx != -1) Text((sIdx + 1).toString(), color = Color.Black, fontSize = 10.sp) }
                                Text(ex.name, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = {
            if (selection.isNotEmpty()) {
                if (save && name.isNotBlank()) {
                    routinesList.add(Routine(name, selection.map { it.name }))
                    val d = routinesList.joinToString("|") { "${it.name}:${it.exerciseNames.joinToString(",")}" }
                    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("routines", d).apply()
                }
                selectedExercises = selection.toList(); currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true
            }
            showAdhocDialog = false
        }) { Text("DALE", color = Color(0xFF00FF00)) } })
    }
}

@Composable
fun TrainingUI(ex: Exercise, idx: Int, total: Int, set: Int, ph: Int, time: Int, paused: Boolean, fullList: List<Exercise>, globalWait: Int, onPause: () -> Unit, onStop: () -> Unit, onEdit: () -> Unit) {
    val phText = when(ph) { 0 -> "Espera"; 1 -> "¡Dale!"; else -> "Descanso" }
    val phColor = when(ph) { 1 -> Color(0xFF00FF00); 2 -> Color(0xFF00AAFF); else -> Color.Yellow }
    val maxTime = when(ph) { 0 -> globalWait; 1 -> 60; else -> ex.rest }.toFloat()
    val progress = if (maxTime > 0) time / maxTime else 0f

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ejercicio ${idx + 1} de $total - ${ex.muscleGroup}", color = Color.Gray); Text(ex.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Serie $set de ${ex.sets}", color = Color.White.copy(0.7f), fontSize = 20.sp); Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(progress = progress, modifier = Modifier.fillMaxSize(), color = phColor, strokeWidth = 10.dp, trackColor = Color.White.copy(0.1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(phText, color = phColor, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("%02d:%02d".format(time / 60, time % 60), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoCol("PESO", formatWeight(ex.weight)); InfoCol("REPS", "${ex.reps}"); InfoCol("ESPERA", "${globalWait}s"); InfoCol("DESC.", "${ex.rest}s")
        }
        if (total == 1) { MenuButton("EDITAR", Modifier.padding(top = 8.dp), bColor = Color.Yellow) { onEdit() } }
        Spacer(Modifier.height(20.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(fullList) { i, item ->
                val isDone = i < idx; val isNow = i == idx
                val bg = if (isDone) Color(0xFF00FF00).copy(0.1f) else if (isNow) Color.White.copy(0.05f) else Color.Transparent
                Row(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(8.dp)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.Circle, null, tint = if (isDone) Color(0xFF00FF00) else Color.Gray, modifier = Modifier.size(14.dp))
                    Text(item.name, color = if (isDone) Color(0xFF00FF00) else if (isNow) Color.White else Color.Gray, modifier = Modifier.padding(start = 8.dp), fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.padding(top = 12.dp)) { MenuButton(if (paused) "REANUDAR" else "PAUSA", Modifier.weight(1f)) { onPause() }; Spacer(Modifier.width(8.dp)); MenuButton("FINALIZAR", Modifier.weight(1f), bColor = Color.Red) { onStop() } }
    }
}

@Composable
fun InfoCol(l: String, v: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(l, color = Color.Gray, fontSize = 10.sp); Text(v, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExercisesScreen(navController: NavController) {
    val context = LocalContext.current
    val gradientEx = Brush.verticalGradient(0.0f to Color.Black, 0.75f to Color.Black, 1.0f to Color(0xFF005000))
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val exercisesList = remember { mutableStateListOf<Exercise>() }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.getString("exercises", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { 
            val p = it.split(",")
            if (p.size == 6) exercisesList.add(Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5]))
        }
    }
    fun save() {
        val data = exercisesList.joinToString("|") { "${it.name},${it.reps},${it.sets},${it.weight},${it.rest},${it.muscleGroup}" }
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("exercises", data).apply()
    }
    Scaffold(bottomBar = { BottomNavBar(navController, "ejercicios") }) { innerPadding ->
        Box(Modifier.fillMaxSize().background(brush = gradientEx).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ejercicios", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
                if (showForm) { 
                    val muscles = exercisesList.map { it.muscleGroup }.distinct().sorted()
                    ExerciseForm(editingIndex != null, editingIndex?.let { exercisesList[it] }, muscles, { showForm = false; editingIndex = null }, { editingIndex?.let { exercisesList.removeAt(it) }; save(); showForm = false; editingIndex = null }, { if (editingIndex != null) exercisesList[editingIndex!!] = it else exercisesList.add(it); save(); showForm = false; editingIndex = null }) 
                }
                else {
                    MenuButton("Agregar Ejercicio") { editingIndex = null; showForm = true }
                    Spacer(Modifier.height(16.dp))
                    val grouped = exercisesList.groupBy { it.muscleGroup }
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        grouped.forEach { (muscle, exercises) ->
                            stickyHeader {
                                Text(muscle, modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.8f)).padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF00FF00), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            items(exercises) { ex ->
                                val realIdx = exercisesList.indexOf(ex)
                                ExerciseItem(ex, editingIndex == realIdx) { editingIndex = realIdx; showForm = true }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutinesScreen(navController: NavController) {
    val context = LocalContext.current
    val gradientRoutine = Brush.verticalGradient(0.0f to Color.Black, 0.75f to Color.Black, 1.0f to Color(0xFF4B0082))
    val allExercises = remember { mutableStateListOf<Exercise>() }
    val routinesList = remember { mutableStateListOf<Routine>() }
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.getString("exercises", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { 
            val p = it.split(",")
            if (p.size == 6) allExercises.add(Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5]))
        }
        prefs.getString("routines", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { val parts = it.split(":"); if (parts.size == 2) routinesList.add(Routine(parts[0], parts[1].split(","))) }
    }
    fun save() {
        val data = routinesList.joinToString("|") { "${it.name}:${it.exerciseNames.joinToString(",")}" }
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("routines", data).apply()
    }
    Scaffold(bottomBar = { BottomNavBar(navController, "rutinas") }) { innerPadding ->
        Box(Modifier.fillMaxSize().background(brush = gradientRoutine).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rutinas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
                if (showForm) { RoutineForm(allExercises, editingIndex?.let { routinesList[it] }, { showForm = false; editingIndex = null }, { editingIndex?.let { routinesList.removeAt(it) }; save(); showForm = false; editingIndex = null }, { if (editingIndex != null) routinesList[editingIndex!!] = it else routinesList.add(it); save(); showForm = false; editingIndex = null }) }
                else { MenuButton("Crear Nueva Rutina", bColor = Color(0xFFBB86FC)) { showForm = true }; Spacer(Modifier.height(16.dp)); LazyColumn(Modifier.fillMaxWidth().weight(1f)) { itemsIndexed(routinesList) { index, routine -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editingIndex = index; showForm = true }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)), border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.5f))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(routine.name, color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("${routine.exerciseNames.size} ejercicios", color = Color.White.copy(0.6f)) }; Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) } } } } }
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    var name by remember { mutableStateOf(prefs.getString("userName", "") ?: "") }
    var pronoun by remember { mutableStateOf(prefs.getString("userPronoun", "Él") ?: "Él") }
    var gender by remember { mutableStateOf(prefs.getString("userGender", "Masculino") ?: "Masculino") }
    var age by remember { mutableIntStateOf(prefs.getInt("userAge", 25)) }
    var level by remember { mutableStateOf(prefs.getString("userLevel", "Novato") ?: "Novato") }
    var weight by remember { mutableIntStateOf(prefs.getInt("userWeight", 70)) }
    var height by remember { mutableIntStateOf(prefs.getInt("userHeight", 170)) }
    var globalWait by remember { mutableIntStateOf(prefs.getInt("globalWait", 20)) }
    val gradientSettings = Brush.verticalGradient(0.0f to Color.Black, 0.75f to Color.Black, 1.0f to Color(0xFF424242))

    Scaffold(bottomBar = { BottomNavBar(navController, "settings") }) { innerPadding ->
        Box(Modifier.fillMaxSize().background(brush = gradientSettings).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Perfil y Configuración", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = name, onValueChange = { name = it; prefs.edit().putString("userName", it).apply() }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF00FF00), unfocusedLabelColor = Color.Gray))
                Spacer(Modifier.height(16.dp))
                Text("¿Cómo prefieres que te llamemos?", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Él", "Ella", "Elle").forEach { p ->
                        FilterChip(selected = pronoun == p, onClick = { pronoun = p; prefs.edit().putString("userPronoun", p).apply() }, label = { Text(p) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00)))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Sexo", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Masculino", "Femenino", "Otro").forEach { g ->
                        FilterChip(selected = gender == g, onClick = { gender = g; prefs.edit().putString("userGender", g).apply() }, label = { Text(g) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00).copy(0.7f)))
                    }
                }
                Spacer(Modifier.height(16.dp))
                NumericStepper("Edad", age, 1) { age = it; prefs.edit().putInt("userAge", it).apply() }
                Spacer(Modifier.height(16.dp))
                Text("Nivel de Entrenamiento", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Novato", "Intermedio", "Avanzado").forEach { l ->
                        FilterChip(selected = level == l, onClick = { level = l; prefs.edit().putString("userLevel", l).apply() }, label = { Text(l) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFFBB86FC)))
                    }
                }
                Spacer(Modifier.height(16.dp))
                NumericStepper("Peso (kg)", weight, 1, 5) { weight = it; prefs.edit().putInt("userWeight", it).apply() }
                NumericStepper("Estatura (cm)", height, 1, 5) { height = it; prefs.edit().putInt("userHeight", it).apply() }
                Divider(Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(0.3f))
                Text("Configuración de Rutina", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(16.dp))
                NumericStepper("Tiempo de Espera Global (s)", globalWait, 5) { globalWait = it; prefs.edit().putInt("globalWait", it).apply() }
                Spacer(Modifier.height(16.dp))
                MenuButton("RESTAURAR EJERCICIOS POR DEFECTO", Modifier.fillMaxWidth(), bColor = Color.Yellow) {
                    prefs.edit().putString("exercises", "").apply()
                    Toast.makeText(context, "Reinicia la app o ve al inicio", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineForm(allExercises: List<Exercise>, initialRoutine: Routine?, onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Routine) -> Unit) {
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    val selected = remember { mutableStateListOf<String>().apply { if (initialRoutine != null) addAll(initialRoutine.exerciseNames) } }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(16.dp))
        val grouped = allExercises.groupBy { it.muscleGroup }
        grouped.forEach { (muscle, exercises) ->
            Text(muscle, color = Color(0xFF00FF00), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            exercises.forEach { ex ->
                val idx = selected.indexOf(ex.name)
                Row(Modifier.fillMaxWidth().clickable { if (idx != -1) selected.remove(ex.name) else selected.add(ex.name) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).background(if (idx != -1) Color(0xFFBB86FC) else Color.Transparent, CircleShape).border(1.dp, Color.Gray, CircleShape), contentAlignment = Alignment.Center) { if (idx != -1) Text((idx + 1).toString(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Text(ex.name, color = if (idx != -1) Color(0xFFBB86FC) else Color.White, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Row(Modifier.padding(top = 20.dp)) {
            if (initialRoutine != null) { MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { onDelete() }; Spacer(Modifier.width(8.dp)) }
            MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }; Spacer(Modifier.width(8.dp)); MenuButton("Guardar", Modifier.weight(1f), bColor = Color(0xFFBB86FC)) { if (name.isNotBlank() && selected.isNotEmpty()) onSave(Routine(name, selected.toList())) }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String) {
    Surface(color = Color(0xFF12151C), modifier = Modifier.fillMaxWidth()) { Row(Modifier.navigationBarsPadding().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavItem(Icons.Default.Home, "Inicio", currentRoute == "menu") { if (currentRoute != "menu") navController.navigate("menu") }
        NavItem(Icons.Default.FitnessCenter, "Ejercicios", currentRoute == "ejercicios") { if (currentRoute != "ejercicios") navController.navigate("ejercicios") }
        NavItem(Icons.AutoMirrored.Filled.ListAlt, "Rutinas", currentRoute == "rutinas") { if (currentRoute != "rutinas") navController.navigate("rutinas") }
        NavItem(Icons.Default.Settings, "Perfil", currentRoute == "settings") { if (currentRoute != "settings") navController.navigate("settings") }
    } }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF00FF00) else Color.Gray
    Column(Modifier.clickable { onClick() }.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, label, tint = color, modifier = Modifier.size(26.dp)); Text(label, color = color, fontSize = 11.sp) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseForm(isEditMode: Boolean, initial: Exercise?, existingMuscles: List<String>, onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Exercise) -> Unit) {
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
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        
        Spacer(Modifier.height(8.dp))
        
        if (!isAddingNewMuscle) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = muscle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Grupo Muscular") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
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

@Composable
fun ExerciseItem(ex: Exercise, isEdit: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, border = BorderStroke(1.dp, if (isEdit) Color.Yellow else Color.Gray), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))) { Column(Modifier.padding(12.dp)) { Text(ex.name, fontWeight = FontWeight.Bold, color = Color(0xFF00FF00), fontSize = 18.sp); Row(verticalAlignment = Alignment.CenterVertically) { Text(ex.muscleGroup, color = Color.Gray, fontSize = 12.sp); Spacer(Modifier.width(8.dp)); Text("${ex.sets} x ${ex.reps} - ${formatWeight(ex.weight)}", color = Color.White, fontSize = 14.sp) } } }
}

@Composable
fun NumericStepper(label: String, value: Int, step: Int, bigStep: Int? = null, onValueChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bigStep != null) { IconButton(onClick = { onValueChange(value - bigStep) }) { Icon(Icons.Default.RemoveCircle, null, tint = Color.White.copy(0.5f)) } }
            IconButton(onClick = { onValueChange(value - step) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
            Text("$value", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onValueChange(value + step) }) { Icon(Icons.Default.Add, null, tint = Color.White) }
            if (bigStep != null) { IconButton(onClick = { onValueChange(value + bigStep) }) { Icon(Icons.Default.AddCircle, null, tint = Color.White.copy(0.5f)) } }
        }
    }
}

@Composable
fun MenuButton(text: String, modifier: Modifier = Modifier, bColor: Color = Color(0xFF00FF00), cColor: Color = Color.DarkGray, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = cColor), border = BorderStroke(1.dp, bColor), shape = RoundedCornerShape(8.dp)) { Text(text, color = Color.White, maxLines = 1) }
}
