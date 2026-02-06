package com.example.appdeprueba

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.*
import com.example.appdeprueba.ui.theme.AppDePruebaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- MODELOS ---
data class Exercise(val name: String, val reps: Int, val sets: Int, val weight: Int, val rest: Int, val muscleGroup: String)
data class Routine(val name: String, val exerciseNames: List<String>)

// --- ENTIDADES ROOM ---
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val name: String,
    val reps: Int,
    val sets: Int,
    val weight: Int,
    val rest: Int,
    val muscleGroup: String
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val name: String,
    val exerciseNames: String
)

@Entity(tableName = "workout_history")
data class WorkoutHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val exerciseName: String,
    val weight: Int,
    val reps: Int,
    val sets: Int,
    val volume: Int
)

// --- DAO ---
@Dao
interface GymDao {
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Insert
    suspend fun insertHistory(history: WorkoutHistoryEntity)

    @Query("SELECT * FROM workout_history ORDER BY date DESC")
    fun getHistory(): Flow<List<WorkoutHistoryEntity>>
}

// --- DATABASE ---
@Database(entities = [ExerciseEntity::class, RoutineEntity::class, WorkoutHistoryEntity::class], version = 1)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao
}

// --- VIEWMODEL ---
class GymViewModel(context: Context) : ViewModel() {
    private val db = Room.databaseBuilder(context, GymDatabase::class.java, "gym_db").build()
    private val dao = db.gymDao()

    val exercises = dao.getAllExercises()
    val routines = dao.getAllRoutines()
    val history = dao.getHistory()

    fun addExercise(ex: Exercise) {
        viewModelScope.launch { dao.insertExercise(ExerciseEntity(ex.name, ex.reps, ex.sets, ex.weight, ex.rest, ex.muscleGroup)) }
    }

    fun removeExercise(exName: String) {
        viewModelScope.launch { dao.deleteExercise(ExerciseEntity(exName, 0, 0, 0, 0, "")) }
    }

    fun addRoutine(routine: Routine) {
        viewModelScope.launch { dao.insertRoutine(RoutineEntity(routine.name, routine.exerciseNames.joinToString(","))) }
    }

    fun removeRoutine(routineName: String) {
        viewModelScope.launch { dao.deleteRoutine(RoutineEntity(routineName, "")) }
    }

    fun logWorkout(historyItem: WorkoutHistoryEntity) {
        viewModelScope.launch { dao.insertHistory(historyItem) }
    }

    fun checkAndSeed(context: Context) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("room_seeded", false)) {
            viewModelScope.launch {
                val savedEx = prefs.getString("exercises", "") ?: ""
                if (savedEx.isNotEmpty()) {
                    savedEx.split("|").forEach {
                        val p = it.split(",")
                        if (p.size == 6) dao.insertExercise(ExerciseEntity(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5]))
                    }
                }
                val savedRoutines = prefs.getString("routines", "") ?: ""
                if (savedRoutines.isNotEmpty()) {
                    savedRoutines.split("|").forEach {
                        val parts = it.split(":")
                        if (parts.size == 2) dao.insertRoutine(RoutineEntity(parts[0], parts[1]))
                    }
                }
                prefs.edit { putBoolean("room_seeded", true) }
            }
        }
    }
}

// --- UTILIDADES ---
fun formatWeight(weight: Int): String = when {
    weight == 0 -> "Corp."
    weight < 0 -> "Asist."
    else -> "${weight}kg"
}

// --- APP ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDePruebaTheme {
                val gymViewModel: GymViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return GymViewModel(applicationContext) as T
                    }
                })
                
                LaunchedEffect(Unit) { gymViewModel.checkAndSeed(applicationContext) }

                var showWelcome by rememberSaveable { mutableStateOf(true) }
                val navController = rememberNavController()
                if (showWelcome) { 
                    WelcomeScreen(onFinished = { showWelcome = false }) 
                } else {
                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") { MainScreen(navController, gymViewModel) }
                        composable("ejercicios") { ExercisesScreen(navController, gymViewModel) }
                        composable("rutinas") { RoutinesScreen(navController, gymViewModel) }
                        composable("progress") { ProgressScreen(navController, gymViewModel) }
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
    LaunchedEffect(Unit) { 
        delay(2500)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = welcomeText, color = Color(0xFF00FF00), fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 40.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val routinesEntity by viewModel.routines.collectAsState(initial = emptyList())
    val allExercisesEntity by viewModel.exercises.collectAsState(initial = emptyList())
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.muscleGroup) }
    val routinesList = routinesEntity.map { Routine(it.name, it.exerciseNames.split(",")) }

    var routineStarted by remember { mutableStateOf(false) }
    var waitingToStart by remember { mutableStateOf(false) }
    var selectedExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var currentRoutineName by remember { mutableStateOf("") }
    var showAdhocDialog by remember { mutableStateOf(false) }
    val pendingNavigation = remember { mutableStateOf<(() -> Unit)?>(null) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) } 
    
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val globalWait = prefs.getInt("globalWait", 20)

    LaunchedEffect(routineStarted, phase, isPaused, timeLeft, waitingToStart) {
        if (routineStarted && !waitingToStart && !isPaused) {
            if (timeLeft > 0) {
                delay(1000)
                timeLeft--
            } else {
                val currentEx = selectedExercises.getOrNull(currentIndex) ?: return@LaunchedEffect
                when (phase) {
                    0 -> { phase = 1; timeLeft = 60 }
                    1 -> { phase = 2; timeLeft = currentEx.rest }
                    else -> {
                        viewModel.logWorkout(WorkoutHistoryEntity(
                            date = System.currentTimeMillis(),
                            exerciseName = currentEx.name,
                            weight = currentEx.weight,
                            reps = currentEx.reps,
                            sets = 1,
                            volume = currentEx.reps * currentEx.weight.coerceAtLeast(1)
                        ))

                        if (currentSet < currentEx.sets) { currentSet++; phase = 1; timeLeft = 60 }
                        else if (currentIndex < selectedExercises.size - 1) { currentIndex++; currentSet = 1; phase = 0; timeLeft = globalWait; waitingToStart = true }
                        else { 
                            routineStarted = false
                            Toast.makeText(context, "Rutina Completada", Toast.LENGTH_LONG).show() 
                        }
                    }
                }
            }
        }
    }

    val secureNav: (() -> Unit) -> Unit = { destination ->
        if (routineStarted) pendingNavigation.value = destination else destination()
    }

    BackHandler(routineStarted) { pendingNavigation.value = { routineStarted = false } }

    Scaffold(bottomBar = { BottomNavBar(navController, "menu") { secureNav(it) } }) { innerPadding ->
        val gradientMain = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF00008B)))
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
                                    if (exList.isNotEmpty()) { 
                                        selectedExercises = exList; currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true; waitingToStart = true
                                        currentRoutineName = routine.name
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
                    Box(Modifier.weight(0.55f)) {
                        val grouped = allExercises.groupBy { it.muscleGroup }
                        LazyColumn {
                            grouped.forEach { (muscle, exercises) ->
                                stickyHeader { Text(muscle, modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.8f)).padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF00FF00), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                items(exercises) { exercise ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                        selectedExercises = listOf(exercise); currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true; waitingToStart = true
                                        currentRoutineName = exercise.name
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
            } else if (waitingToStart) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿PREPARADO?", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(currentRoutineName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { waitingToStart = false }, modifier = Modifier.size(150.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00))) {
                        Text("DALE", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("Próximos ejercicios:", color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        items(selectedExercises) { ex ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
                                Text(ex.name, color = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                    TextButton(onClick = { routineStarted = false; waitingToStart = false }) { Text("CANCELAR", color = Color.Red) }
                }
            } else {
                TrainingUI(selectedExercises[currentIndex], currentIndex, selectedExercises.size, currentSet, phase, timeLeft, isPaused, selectedExercises, globalWait, { isPaused = !isPaused }, { pendingNavigation.value = { routineStarted = false } })
            }
        }
    }

    pendingNavigation.value?.let { action ->
        AlertDialog(onDismissRequest = { pendingNavigation.value = null }, containerColor = Color(0xFF1A1C20), title = { Text("Finalizar Rutina", color = Color.White) }, text = { Text("¿Estás seguro de que quieres finalizar el entrenamiento actual?", color = Color.Gray) },
            confirmButton = { TextButton(onClick = { 
                pendingNavigation.value = null
                routineStarted = false
                action() 
            }) { Text("SÍ, FINALIZAR", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { pendingNavigation.value = null }) { Text("CONTINUAR", color = Color.White) } }
        )
    }

    if (showAdhocDialog) {
        val selection = remember { mutableStateListOf<Exercise>() }
        var adhocName by remember { mutableStateOf("") }
        var saveRoutine by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAdhocDialog = false }, containerColor = Color(0xFF1A1C20), title = { Text("Rutina Adhoc", color = Color.White) }, text = {
            Column {
                OutlinedTextField(value = adhocName, onValueChange = { adhocName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(saveRoutine, { saveRoutine = it }); Text("Guardar", color = Color.White) }
                val grouped = allExercises.groupBy { it.muscleGroup }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    grouped.forEach { (muscle, exercises) ->
                        stickyHeader { Text(muscle, modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2C32)).padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF00FF00), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
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
                if (saveRoutine && adhocName.isNotBlank()) {
                    viewModel.addRoutine(Routine(adhocName, selection.map { it.name }))
                }
                selectedExercises = selection.toList()
                currentIndex = 0; currentSet = 1; phase = 0; timeLeft = globalWait; routineStarted = true; waitingToStart = true
                currentRoutineName = adhocName.ifBlank { "Rutina Adhoc" }
            }
            showAdhocDialog = false
        }) { Text("DALE", color = Color(0xFF00FF00)) } })
    }
}

@Composable
fun TrainingUI(ex: Exercise, idx: Int, total: Int, set: Int, ph: Int, time: Int, paused: Boolean, fullList: List<Exercise>, globalWait: Int, onPause: () -> Unit, onStop: () -> Unit) {
    val phText = when(ph) { 0 -> "Espera"; 1 -> "¡Dale!"; else -> "Descanso" }
    val phColor = when(ph) { 1 -> Color(0xFF00FF00); 2 -> Color(0xFF00AAFF); else -> Color.Yellow }
    val maxTime = when(ph) { 0 -> globalWait; 1 -> 60; else -> ex.rest }.toFloat()
    val progress = if (maxTime > 0) time / maxTime else 0f

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ejercicio ${idx + 1} de $total - ${ex.muscleGroup}", color = Color.Gray); Text(ex.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Serie $set de ${ex.sets}", color = Color.White.copy(0.7f), fontSize = 20.sp); Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = phColor, strokeWidth = 10.dp, trackColor = Color.White.copy(0.1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(phText, color = phColor, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("%02d:%02d".format(time / 60, time % 60), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoCol("PESO", formatWeight(ex.weight)); InfoCol("REPS", "${ex.reps}"); InfoCol("ESPERA", "${globalWait}s"); InfoCol("DESC.", "${ex.rest}s")
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController, viewModel: GymViewModel) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val exercisesEntity by viewModel.exercises.collectAsState(initial = emptyList())
    val exercises = exercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.muscleGroup) }
    
    var selectedExercise by remember { mutableStateOf(if(exercises.isNotEmpty()) exercises[0].name else "Sentadilla libre") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(bottomBar = { BottomNavBar(navController, "progress") { it() } }) { innerPadding ->
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
                StatCard("Entrenos", workoutsThisMonth.toString(), Modifier.weight(1f), Color(0xFF00FF00))
                
                val totalVolume = history.sumOf { it.volume }
                StatCard("Volumen", "${totalVolume/1000}k", Modifier.weight(1f), Color(0xFF00AAFF))
            }

            Spacer(Modifier.height(24.dp))
            Text("Volumen Semanal", color = Color.Gray, fontSize = 14.sp)
            VolumeChart(history)

            Spacer(Modifier.height(32.dp))
            Text("Evolución de Peso", color = Color.Gray, fontSize = 14.sp)
            
            // Exercise Selector
            Box {
                OutlinedTextField(
                    value = selectedExercise,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true },
                    label = { Text("Ejercicio") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    exercises.forEach { ex ->
                        DropdownMenuItem(text = { Text(ex.name) }, onClick = { selectedExercise = ex.name; dropdownExpanded = false })
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            WeightLineChart(history.filter { it.exerciseName == selectedExercise })
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, color.copy(0.3f))) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun VolumeChart(history: List<WorkoutHistoryEntity>) {
    val weeklyVolume = remember(history) {
        val last7Days = (0..6).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(cal.time)
            val vol = history.filter { h ->
                val hCal = Calendar.getInstance().apply { timeInMillis = h.date }
                hCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            }.sumOf { it.volume }
            dateStr to vol
        }.reversed()
        last7Days
    }

    if (weeklyVolume.all { it.second == 0 }) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("Sin datos semanales", color = Color.Gray) }
    } else {
        val maxVol = weeklyVolume.maxOf { it.second }.coerceAtLeast(1).toFloat()
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(top = 16.dp)) {
            val barWidth = size.width / 14f
            weeklyVolume.forEachIndexed { i, item ->
                val vol = item.second.toFloat()
                val barHeight = (vol / maxVol) * size.height
                drawRect(
                    color = Color(0xFF00FF00),
                    topLeft = Offset(i * (size.width / 7f) + barWidth/2, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
fun WeightLineChart(exerciseHistory: List<WorkoutHistoryEntity>) {
    if (exerciseHistory.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("Sin historial para este ejercicio", color = Color.Gray) }
    } else {
        val weights = exerciseHistory.map { it.weight.toFloat() }
        val maxW = weights.maxOrNull() ?: 1f
        val minW = weights.minOrNull() ?: 0f
        val range = (maxW - minW).coerceAtLeast(1f)

        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 8.dp)) {
            val path = Path()
            val stepX = size.width / (weights.size - 1).coerceAtLeast(1)
            weights.forEachIndexed { i, w ->
                val x = i * stepX
                val y = size.height - ((w - minW) / range) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(Color(0xFF00AAFF), radius = 4.dp.toPx(), center = Offset(x, y))
            }
            drawPath(path, Color(0xFF00AAFF), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExercisesScreen(navController: NavController, viewModel: GymViewModel) {
    val exercisesListEntity by viewModel.exercises.collectAsState(initial = emptyList())
    val exercisesList = exercisesListEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.muscleGroup) }
    
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(bottomBar = { BottomNavBar(navController, "ejercicios") { it() } }) { innerPadding ->
        val gradientEx = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF005000)))
        Box(Modifier.fillMaxSize().background(brush = gradientEx).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ejercicios", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
                if (showForm) { 
                    val muscles = exercisesList.map { it.muscleGroup }.distinct().sorted()
                    ExerciseForm(editingIndex != null, editingIndex?.let { exercisesList[it] }, muscles, { showForm = false; editingIndex = null }, { 
                        editingIndex?.let { idx -> viewModel.removeExercise(exercisesList[idx].name) }
                        showForm = false; editingIndex = null 
                    }, { updatedEx -> 
                        viewModel.addExercise(updatedEx)
                        showForm = false; editingIndex = null 
                    }) 
                }
                else {
                    MenuButton("Agregar Ejercicio") { editingIndex = null; showForm = true }
                    Spacer(Modifier.height(16.dp))
                    val grouped = exercisesList.groupBy { it.muscleGroup }
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        grouped.forEach { (muscle, exercises) ->
                            stickyHeader { Text(muscle, modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.8f)).padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF00FF00), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            items(exercises) { ex ->
                                val realIdx = exercisesList.indexOf(ex); ExerciseItem(ex, editingIndex == realIdx) { editingIndex = realIdx; showForm = true }
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
fun RoutinesScreen(navController: NavController, viewModel: GymViewModel) {
    val routinesEntity by viewModel.routines.collectAsState(initial = emptyList())
    val allExercisesEntity by viewModel.exercises.collectAsState(initial = emptyList())
    
    val allExercises = allExercisesEntity.map { Exercise(it.name, it.reps, it.sets, it.weight, it.rest, it.muscleGroup) }
    val routinesList = routinesEntity.map { Routine(it.name, it.exerciseNames.split(",")) }

    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(bottomBar = { BottomNavBar(navController, "rutinas") { it() } }) { innerPadding ->
        val gradientRoutine = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF4B0082)))
        Box(Modifier.fillMaxSize().background(brush = gradientRoutine).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rutinas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
                if (showForm) { 
                    RoutineForm(allExercises, editingIndex?.let { routinesList[it] }, { showForm = false; editingIndex = null }, { 
                        editingIndex?.let { idx -> viewModel.removeRoutine(routinesList[idx].name) }
                        showForm = false; editingIndex = null 
                    }, { newRoutine -> 
                        viewModel.addRoutine(newRoutine)
                        showForm = false; editingIndex = null 
                    }) 
                }
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

    Scaffold(bottomBar = { BottomNavBar(navController, "settings") { it() } }) { innerPadding ->
        val gradientSettings = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF424242)))
        Box(Modifier.fillMaxSize().background(brush = gradientSettings).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Perfil y Configuración", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = name, onValueChange = { name = it; prefs.edit { putString("userName", it) } }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF00FF00), unfocusedLabelColor = Color.Gray))
                Spacer(Modifier.height(16.dp))
                Text("¿Cómo prefieres que te llamemos?", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Él", "Ella", "Elle").forEach { p -> FilterChip(selected = pronoun == p, onClick = { pronoun = p; prefs.edit { putString("userPronoun", p) } }, label = { Text(p) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00))) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Sexo", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Masculino", "Femenino", "Otro").forEach { g -> FilterChip(selected = gender == g, onClick = { gender = g; prefs.edit { putString("userGender", g) } }, label = { Text(g) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00).copy(0.7f))) }
                }
                Spacer(Modifier.height(16.dp))
                NumericStepper("Edad", age, 1) { age = it; prefs.edit { putInt("userAge", it) } }
                Spacer(Modifier.height(16.dp))
                Text("Nivel de Entrenamiento", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Novato", "Intermedio", "Avanzado").forEach { l -> FilterChip(selected = level == l, onClick = { level = l; prefs.edit { putString("userLevel", l) } }, label = { Text(l) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFFBB86FC))) }
                }
                Spacer(Modifier.height(16.dp))
                NumericStepper("Peso (kg)", weight, 1, 5) { weight = it; prefs.edit { putInt("userWeight", it) } }
                NumericStepper("Estatura (cm)", height, 1, 5) { height = it; prefs.edit { putInt("userHeight", it) } }
                HorizontalDivider(Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(0.3f))
                Text("Configuración de Rutina", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(16.dp))
                NumericStepper("Tiempo de Espera Global (s)", globalWait, 5) { globalWait = it; prefs.edit { putInt("globalWait", it) } }
                Spacer(Modifier.height(16.dp))
                MenuButton("RESTAURAR EJERCICIOS POR DEFECTO", Modifier.fillMaxWidth(), bColor = Color.Yellow) {
                    prefs.edit { putString("exercises", "") }
                    Toast.makeText(context, "Reinicia la app o ve al inicio", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String, onNav: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    Surface(color = Color(0xFF12151C), modifier = Modifier.fillMaxWidth()) { Row(Modifier.navigationBarsPadding().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavItem(Icons.Default.Home, "Inicio", currentRoute == "menu") { onNav { if (currentRoute != "menu") navController.navigate("menu") } }
        NavItem(Icons.Default.FitnessCenter, "Ejercicios", currentRoute == "ejercicios") { onNav { if (currentRoute != "ejercicios") navController.navigate("ejercicios") } }
        NavItem(Icons.AutoMirrored.Filled.ListAlt, "Rutinas", currentRoute == "rutinas") { onNav { if (currentRoute != "rutinas") navController.navigate("rutinas") } }
        NavItem(Icons.Default.BarChart, "Progreso", currentRoute == "progress") { onNav { if (currentRoute != "progress") navController.navigate("progress") } }
        NavItem(Icons.Default.Settings, "Perfil", currentRoute == "settings") { onNav { if (currentRoute != "settings") navController.navigate("settings") } }
        NavItem(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar", false) { (context as? Activity)?.finish() }
    } }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF00FF00) else Color.Gray
    Column(Modifier.clickable { onClick() }.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, label, tint = color, modifier = Modifier.size(26.dp)); Text(label, color = color, fontSize = 11.sp) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineForm(allExercises: List<Exercise>, initialRoutine: Routine?, onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Routine) -> Unit) {
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    val selected = remember { mutableStateListOf<String>().apply { if (initialRoutine != null) addAll(initialRoutine.exerciseNames) } }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 8.dp)) {
            if (initialRoutine != null) { MenuButton("Eliminar", Modifier.weight(1f), bColor = Color.Red) { onDelete() }; Spacer(Modifier.width(8.dp)) }
            MenuButton("Cancelar", Modifier.weight(1f), bColor = Color.Gray) { onCancel() }; Spacer(Modifier.width(8.dp)); MenuButton("Guardar", Modifier.weight(1f), bColor = Color(0xFFBB86FC)) { if (name.isNotBlank() && selected.isNotEmpty()) onSave(Routine(name, selected.toList())) }
        }
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
        }
    }
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
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    existingMuscles.forEach { selection -> DropdownMenuItem(text = { Text(selection) }, onClick = { muscle = selection; expanded = false }) }
                    DropdownMenuItem(text = { Text("+ Agregar nuevo...", color = Color(0xFF00FF00)) }, onClick = { isAddingNewMuscle = true; expanded = false })
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newMuscleName, onValueChange = { newMuscleName = it }, label = { Text("Nuevo Grupo") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
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
            if (bigStep != null) { IconButton(onClick = { if (value >= bigStep) onValueChange(value - bigStep) }) { Icon(Icons.Default.RemoveCircle, null, tint = Color.White.copy(0.5f)) } }
            IconButton(onClick = { if (value >= step) onValueChange(value - step) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
            Text("$value", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onValueChange(value + step) }) { Icon(Icons.Default.Add, null, tint = Color.White) }
            if (bigStep != null) { IconButton(onClick = { onValueChange(value + bigStep) }) { Icon(Icons.Default.AddCircle, null, tint = Color.White.copy(0.5f)) } }
        }
    }
}

@Composable
fun InfoCol(l: String, v: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(l, color = Color.Gray, fontSize = 10.sp); Text(v, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }

@Composable
fun MenuButton(text: String, modifier: Modifier = Modifier, bColor: Color = Color(0xFF00FF00), cColor: Color = Color.DarkGray, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = cColor), border = BorderStroke(1.dp, bColor), shape = RoundedCornerShape(8.dp)) { Text(text, color = Color.White, maxLines = 1) }
}
