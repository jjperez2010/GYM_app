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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdeprueba.ui.theme.AppDePruebaTheme
import kotlinx.coroutines.delay

// --- MODELOS DE DATOS ---
data class Exercise(
    val name: String,
    val reps: Int,
    val sets: Int,
    val weight: Int,
    val wait: Int,
    val rest: Int
)

data class Routine(
    val name: String,
    val exerciseNames: List<String>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDePruebaTheme {
                var showWelcome by remember { mutableStateOf(true) }
                val navController = rememberNavController()

                if (showWelcome) {
                    WelcomeScreen { showWelcome = false }
                } else {
                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") { MainScreen(navController) }
                        composable("ejercicios") { ExercisesScreen(navController) }
                        composable("rutinas") { RoutinesScreen(navController) }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onTimeout() }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = "¡¡Bienvenido!!", color = Color(0xFF00FF00), fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}

// --- PANTALLA DE INICIO ---
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val routinesList = remember { mutableStateListOf<Routine>() }
    val allExercises = remember { mutableStateListOf<Exercise>() }
    
    var routineStarted by remember { mutableStateOf(false) }
    var selectedExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var showAdhocDialog by remember { mutableStateOf(false) }
    var showQuickEdit by remember { mutableStateOf(false) }

    // Estados del Entrenamiento
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) } // 0=Espera, 1=Ejercicio, 2=Descanso

    val gradientMain = Brush.verticalGradient(0.0f to Color.Black, 0.75f to Color.Black, 1.0f to Color(0xFF00008B))

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.getString("exercises", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach {
            val p = it.split(","); if (p.size == 6) allExercises.add(Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt()))
        }
        prefs.getString("routines", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach {
            val parts = it.split(":"); if (parts.size == 2) routinesList.add(Routine(parts[0], parts[1].split(",")))
        }
    }

    LaunchedEffect(routineStarted, phase, isPaused, timeLeft) {
        if (routineStarted && !isPaused && timeLeft > 0) {
            delay(1000); timeLeft--
        } else if (routineStarted && !isPaused && timeLeft <= 0) {
            val currentEx = selectedExercises.getOrNull(currentIndex) ?: return@LaunchedEffect
            when (phase) {
                0 -> { phase = 1; timeLeft = 60 }
                1 -> { phase = 2; timeLeft = currentEx.rest }
                2 -> {
                    if (currentSet < currentEx.sets) { currentSet++; phase = 1; timeLeft = 60 }
                    else if (currentIndex < selectedExercises.size - 1) { currentIndex++; currentSet = 1; phase = 0; timeLeft = selectedExercises[currentIndex].wait }
                    else { routineStarted = false; Toast.makeText(context, "¡Rutina Completada!", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    Scaffold(bottomBar = { BottomNavBar(navController, "menu") }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(brush = gradientMain).padding(innerPadding)) {
            if (!routineStarted) {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MenuButton("Rutina Rápida", Modifier.fillMaxWidth(), bColor = Color(0xFFFFA500), cColor = Color(0xFFFFA500).copy(0.1f)) { showAdhocDialog = true }
                    Spacer(Modifier.height(24.dp))
                    Text("Mis Rutinas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Box(modifier = Modifier.weight(0.45f)) {
                        LazyColumn {
                            items(routinesList) { routine ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    val exList = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                                    if (exList.isNotEmpty()) {
                                        selectedExercises = exList; currentIndex = 0; currentSet = 1; phase = 0; timeLeft = exList[0].wait; routineStarted = true
                                    }
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
                    Box(modifier = Modifier.weight(0.55f)) {
                        LazyColumn {
                            items(allExercises) { exercise ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    selectedExercises = listOf(exercise); currentIndex = 0; currentSet = 1; phase = 0; timeLeft = exercise.wait; routineStarted = true
                                }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color(0xFF00FF00).copy(0.3f))) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FitnessCenter, null, tint = Color(0xFF00FF00)); Spacer(Modifier.width(12.dp))
                                        Column { Text(exercise.name, color = Color.White, fontSize = 16.sp); Text("${exercise.sets}x${exercise.reps} - ${exercise.weight}kg", color = Color.Gray, fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                TrainingUI(
                    ex = selectedExercises[currentIndex],
                    idx = currentIndex,
                    total = selectedExercises.size,
                    set = currentSet,
                    ph = phase,
                    time = timeLeft,
                    paused = isPaused,
                    fullList = selectedExercises,
                    onPause = { isPaused = !isPaused },
                    onStop = { routineStarted = false },
                    onEdit = { showQuickEdit = true }
                )
            }
        }
    }

    if (showQuickEdit) {
        AlertDialog(onDismissRequest = { showQuickEdit = false }, containerColor = Color(0xFF1A1C20), title = { Text("Ajustar para hoy", color = Color.White) }, text = {
            ExerciseForm(false, selectedExercises[currentIndex], { showQuickEdit = false }, {}, { 
                val newList = selectedExercises.toMutableList()
                newList[currentIndex] = it; selectedExercises = newList; showQuickEdit = false
            })
        }, confirmButton = {})
    }

    if (showAdhocDialog) {
        val adhocSelection = remember { mutableStateListOf<Exercise>() }
        var adName by remember { mutableStateOf("") }
        var adSave by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAdhocDialog = false }, containerColor = Color(0xFF1A1C20), title = { Text("Nueva Rutina Rápida", color = Color.White) }, text = {
            Column {
                OutlinedTextField(value = adName, onValueChange = { adName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(adSave, { adSave = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FF00))); Text("Guardar", color = Color.White) }
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    itemsIndexed(allExercises) { _, ex ->
                        val sIdx = adhocSelection.indexOf(ex)
                        Row(Modifier.fillMaxWidth().clickable { if (sIdx != -1) adhocSelection.removeAt(sIdx) else adhocSelection.add(ex) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(24.dp).background(if (sIdx != -1) Color(0xFF00FF00) else Color.Transparent, CircleShape).border(1.dp, Color.Gray, CircleShape), contentAlignment = Alignment.Center) { if (sIdx != -1) Text((sIdx + 1).toString(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Text(ex.name, color = if (sIdx != -1) Color(0xFF00FF00) else Color.White, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = {
            if (adhocSelection.isNotEmpty()) {
                if (adSave && adName.isNotBlank()) {
                    val r = Routine(adName, adhocSelection.map { it.name }); routinesList.add(r)
                    val d = routinesList.joinToString("|") { "${it.name}:${it.exerciseNames.joinToString(",")}" }
                    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("routines", d).apply()
                }
                selectedExercises = adhocSelection.toList(); currentIndex = 0; currentSet = 1; phase = 0; timeLeft = selectedExercises[0].wait; routineStarted = true
            }
            showAdhocDialog = false
        }) { Text("ENTRENAR", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold) } })
    }
}

@Composable
fun TrainingUI(ex: Exercise, idx: Int, total: Int, set: Int, ph: Int, time: Int, paused: Boolean, fullList: List<Exercise>, onPause: () -> Unit, onStop: () -> Unit, onEdit: () -> Unit) {
    val phText = when(ph) { 0 -> "Espera"; 1 -> "¡Dale!"; else -> "Descanso" }
    val phColor = when(ph) { 1 -> Color(0xFF00FF00); 2 -> Color(0xFF00AAFF); else -> Color.Yellow }
    val maxTime = when(ph) { 0 -> ex.wait; 1 -> 60; else -> ex.rest }.toFloat()
    val progress = if (maxTime > 0) time / maxTime else 0f

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ejercicio ${idx + 1} de $total", color = Color.Gray); Text(ex.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Serie $set de ${ex.sets}", color = Color.White.copy(0.7f), fontSize = 20.sp); Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(progress = progress, modifier = Modifier.fillMaxSize(), color = phColor, strokeWidth = 10.dp, trackColor = Color.White.copy(0.1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(phText, color = phColor, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("%02d:%02d".format(time / 60, time % 60), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoCol("PESO", "${ex.weight}kg"); InfoCol("REPS", "${ex.reps}"); InfoCol("ESPERA", "${ex.wait}s"); InfoCol("DESC.", "${ex.rest}s")
        }
        if (total == 1) { MenuButton("EDITAR VALORES", Modifier.padding(top = 8.dp), bColor = Color.Yellow) { onEdit() } }
        Spacer(Modifier.height(24.dp))
        Text("RUTINA EN CURSO:", color = Color.White.copy(0.5f), fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
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

// --- PANTALLAS DE GESTIÓN ---
@Composable
fun ExercisesScreen(navController: NavController) {
    val context = LocalContext.current
    val gradientEx = Brush.verticalGradient(0.0f to Color.Black, 0.75f to Color.Black, 1.0f to Color(0xFF005000))
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val exercisesList = remember { mutableStateListOf<Exercise>() }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.getString("exercises", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { val p = it.split(","); if (p.size == 6) exercisesList.add(Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt())) }
    }
    fun save() {
        val data = exercisesList.joinToString("|") { "${it.name},${it.reps},${it.sets},${it.weight},${it.wait},${it.rest}" }
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("exercises", data).apply()
    }
    Scaffold(bottomBar = { BottomNavBar(navController, "ejercicios") }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(brush = gradientEx).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ejercicios", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
                if (showForm) {
                    ExerciseForm(editingIndex != null, editingIndex?.let { exercisesList[it] }, { showForm = false; editingIndex = null }, { editingIndex?.let { exercisesList.removeAt(it) }; save(); showForm = false; editingIndex = null }, { if (editingIndex != null) exercisesList[editingIndex!!] = it else exercisesList.add(it); save(); showForm = false; editingIndex = null })
                } else {
                    MenuButton("Agregar Ejercicio") { editingIndex = null; showForm = true }; Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) { itemsIndexed(exercisesList) { index, ex -> ExerciseItem(ex, editingIndex == index) { editingIndex = index; showForm = true } } }
                }
            }
        }
    }
}

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
        prefs.getString("exercises", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { val p = it.split(","); if (p.size == 6) allExercises.add(Exercise(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt())) }
        prefs.getString("routines", "")?.takeIf { it.isNotEmpty() }?.split("|")?.forEach { val parts = it.split(":"); if (parts.size == 2) routinesList.add(Routine(parts[0], parts[1].split(","))) }
    }
    fun save() {
        val data = routinesList.joinToString("|") { "${it.name}:${it.exerciseNames.joinToString(",")}" }
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("routines", data).apply()
    }
    Scaffold(bottomBar = { BottomNavBar(navController, "rutinas") }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(brush = gradientRoutine).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rutinas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
                if (showForm) {
                    RoutineForm(allExercises, editingIndex?.let { routinesList[it] }, { showForm = false; editingIndex = null }, { editingIndex?.let { routinesList.removeAt(it) }; save(); showForm = false; editingIndex = null }, { if (editingIndex != null) routinesList[editingIndex!!] = it else routinesList.add(it); save(); showForm = false; editingIndex = null })
                } else {
                    MenuButton("Crear Nueva Rutina", bColor = Color(0xFFBB86FC)) { showForm = true }; Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) { itemsIndexed(routinesList) { index, routine -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editingIndex = index; showForm = true }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)), border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.5f))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(routine.name, color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("${routine.exerciseNames.size} ejercicios", color = Color.White.copy(0.6f)) }; Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) } } } }
                }
            }
        }
    }
}

@Composable
fun RoutineForm(allExercises: List<Exercise>, initialRoutine: Routine?, onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Routine) -> Unit) {
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    val selected = remember { mutableStateListOf<String>().apply { if (initialRoutine != null) addAll(initialRoutine.exerciseNames) } }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(16.dp))
        allExercises.forEach { ex ->
            val idx = selected.indexOf(ex.name)
            Row(Modifier.fillMaxWidth().clickable { if (idx != -1) selected.remove(ex.name) else selected.add(ex.name) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(if (idx != -1) Color(0xFFBB86FC) else Color.Transparent, CircleShape).border(1.dp, Color.Gray, CircleShape), contentAlignment = Alignment.Center) { if (idx != -1) Text((idx + 1).toString(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Text(ex.name, color = if (idx != -1) Color(0xFFBB86FC) else Color.White, modifier = Modifier.padding(start = 12.dp))
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
    } }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF00FF00) else Color.Gray
    Column(Modifier.clickable { onClick() }.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, label, tint = color, modifier = Modifier.size(26.dp)); Text(label, color = color, fontSize = 11.sp) }
}

@Composable
fun ExerciseForm(isEditMode: Boolean, initial: Exercise?, onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sets by remember { mutableIntStateOf(initial?.sets ?: 3) }; var reps by remember { mutableIntStateOf(initial?.reps ?: 10) }; var weight by remember { mutableIntStateOf(initial?.weight ?: 30) }; var wait by remember { mutableIntStateOf(initial?.wait ?: 20) }; var rest by remember { mutableIntStateOf(initial?.rest ?: 120) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        NumericStepper("Series", sets, 1) { sets = it }; NumericStepper("Reps", reps, 1) { reps = it }; NumericStepper("Peso", weight, 1, 5) { weight = it }; NumericStepper("Espera", wait, 5) { wait = it }; NumericStepper("Descanso", rest, 10) { rest = it }
        Row(Modifier.padding(top = 16.dp)) {
            if (isEditMode) { MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { onDelete() }; Spacer(Modifier.width(8.dp)) }
            MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }; Spacer(Modifier.width(8.dp)); MenuButton("Guardar", Modifier.weight(1f)) { if (name.isNotBlank()) onSave(Exercise(name, reps, sets, weight, wait, rest)) }
        }
    }
}

@Composable
fun ExerciseItem(ex: Exercise, isEdit: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, border = BorderStroke(1.dp, if (isEdit) Color.Yellow else Color.Gray), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))) { Column(Modifier.padding(12.dp)) { Text(ex.name, fontWeight = FontWeight.Bold, color = Color(0xFF00FF00), fontSize = 18.sp); Text("${ex.sets} x ${ex.reps} - ${ex.weight}kg", color = Color.White) } }
}

@Composable
fun NumericStepper(label: String, value: Int, step: Int, bigStep: Int? = null, onValueChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bigStep != null) { IconButton(onClick = { if (value >= bigStep) onValueChange(value - bigStep) }) { Icon(Icons.Default.RemoveCircle, null, tint = Color.White.copy(0.5f)) } }
            IconButton(onClick = { if (value >= step) onValueChange(value - step) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
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
