package com.example.Gym_App.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Gym_App.database.GymDatabase
import com.example.Gym_App.model.ExerciseEntity
import com.example.Gym_App.model.RoutineEntity
import com.example.Gym_App.model.WorkoutHistoryEntity
import com.example.Gym_App.model.WeightEntity
import com.example.Gym_App.repository.GymRepository
import com.example.Gym_App.utils.ConsistencyCheckWorker
import com.example.Gym_App.utils.NotificationHelper
import com.example.Gym_App.utils.NotificationReceiver
import com.example.Gym_App.utils.WeeklyGoalWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class GymViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext

    // Lazy initialization de la base de datos para evitar bloquear el MainThread
    private val db: GymDatabase by lazy {
        GymDatabase.getDatabase(appContext)
    }

    private val repository: GymRepository by lazy {
        GymRepository(db.gymDao())
    }

    val exercises: StateFlow<List<ExerciseEntity>> by lazy {
        repository.allExercises
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val routines: StateFlow<List<RoutineEntity>> by lazy {
        repository.allRoutines
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val history: StateFlow<List<WorkoutHistoryEntity>> by lazy {
        repository.workoutHistory
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val weightHistory: StateFlow<List<WeightEntity>> by lazy {
        repository.weightHistory
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val workoutDates: StateFlow<List<LocalDate>> by lazy {
        history.map { list ->
            list.map { 
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() 
            }.distinct()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // --- LÓGICA DE FILTRADO PARA EJERCICIOS ---
    private val _selectedMuscle = MutableStateFlow<String?>(null)
    private val _selectedEquipment = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val filteredExercises: StateFlow<List<ExerciseEntity>> by lazy {
        combine(
            exercises, _selectedMuscle, _selectedEquipment, _searchQuery
        ) { list, muscle, equip, query ->
            list.filter { ex ->
                (muscle == null || ex.muscleGroup == muscle) &&
                (equip == null || ex.equipmentType == equip) &&
                (query.isEmpty() || ex.name.contains(query, ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun updateMuscleFilter(muscle: String?) { _selectedMuscle.value = muscle }
    fun updateEquipmentFilter(equipment: String?) { _selectedEquipment.value = equipment }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    val selectedMuscle: StateFlow<String?> = _selectedMuscle
    val selectedEquipment: StateFlow<String?> = _selectedEquipment

    // --- ACCIONES CRUD ---
    fun addExercise(exercise: ExerciseEntity) = viewModelScope.launch {
        repository.insertExercise(exercise)
    }

    fun deleteExercise(name: String) = viewModelScope.launch {
        repository.deleteExercise(name)
    }

    fun addRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            repository.insertRoutine(routine)
        }
    }

    fun deleteRoutine(name: String) = viewModelScope.launch {
        repository.deleteRoutine(name)
    }

    fun saveWorkout(historyItem: WorkoutHistoryEntity) = viewModelScope.launch {
        repository.logWorkout(historyItem)
    }

    fun logCompletedWorkout(routineName: String) = viewModelScope.launch {
        val historyItem = WorkoutHistoryEntity(
            date = System.currentTimeMillis(),
            exerciseName = "Rutina: $routineName",
            weight = 0.0,
            reps = 0,
            sets = 0,
            volume = 0.0
        )
        repository.logWorkout(historyItem)
    }

    fun saveWeight(weight: Double) = viewModelScope.launch {
        repository.insertWeight(WeightEntity(date = System.currentTimeMillis(), weight = weight))
    }

    fun resetWeightHistory() = viewModelScope.launch {
        repository.deleteAllWeight()
    }

    private val defaultExercises = listOf(
        // Pecho
        ExerciseEntity("Press de Banca", 10, 3, 60, 90, muscleGroup = "Pecho", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Press Inclinado", 10, 3, 50, 90, muscleGroup = "Pecho", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Aperturas", 12, 3, 12, 60, muscleGroup = "Pecho", equipmentType = "Mancuerna", updateReminderDays = 30),
        ExerciseEntity("Flexiones", 15, 3, 0, 60, muscleGroup = "Pecho", equipmentType = "Peso Corporal", updateReminderDays = 30),
        ExerciseEntity("Cruce de Poleas", 15, 3, 15, 60, muscleGroup = "Pecho", equipmentType = "Polea", updateReminderDays = 30),

        // Espalda
        ExerciseEntity("Peso Muerto", 8, 3, 100, 150, muscleGroup = "Espalda", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Dominadas", 8, 3, 0, 90, muscleGroup = "Espalda", equipmentType = "Peso Corporal", updateReminderDays = 30),
        ExerciseEntity("Remo con Barra", 10, 3, 60, 90, muscleGroup = "Espalda", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Jalón al Pecho", 12, 3, 50, 60, muscleGroup = "Espalda", equipmentType = "Polea", updateReminderDays = 30),
        ExerciseEntity("Remo en Polea Baja", 12, 3, 45, 60, muscleGroup = "Espalda", equipmentType = "Polea", updateReminderDays = 30),

        // Piernas
        ExerciseEntity("Sentadillas", 12, 4, 80, 120, muscleGroup = "Piernas", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Prensa de Piernas", 12, 3, 120, 90, muscleGroup = "Piernas", equipmentType = "Máquina", updateReminderDays = 30),
        ExerciseEntity("Zancadas", 12, 3, 20, 60, muscleGroup = "Piernas", equipmentType = "Mancuerna", updateReminderDays = 30),
        ExerciseEntity("Extensión de Cuádriceps", 15, 3, 40, 60, muscleGroup = "Piernas", equipmentType = "Máquina", updateReminderDays = 30),
        ExerciseEntity("Curl Femoral", 15, 3, 35, 60, muscleGroup = "Piernas", equipmentType = "Máquina", updateReminderDays = 30),
        ExerciseEntity("Elevación de Talones", 20, 4, 50, 45, muscleGroup = "Piernas", equipmentType = "Máquina", updateReminderDays = 30),

        // Hombros
        ExerciseEntity("Press Militar", 10, 3, 40, 90, muscleGroup = "Hombros", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Vuelos Laterales", 12, 3, 10, 60, muscleGroup = "Hombros", equipmentType = "Mancuerna", updateReminderDays = 30),
        ExerciseEntity("Press Arnold", 10, 3, 15, 60, muscleGroup = "Hombros", equipmentType = "Mancuerna", updateReminderDays = 30),
        ExerciseEntity("Pájaros", 15, 3, 8, 60, muscleGroup = "Hombros", equipmentType = "Mancuerna", updateReminderDays = 30),
        ExerciseEntity("Remo al Mentón", 12, 3, 30, 60, muscleGroup = "Hombros", equipmentType = "Barra", updateReminderDays = 30),

        // Brazos
        ExerciseEntity("Curl de Bíceps con Barra", 12, 3, 30, 60, muscleGroup = "Brazos", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Curl Martillo", 12, 3, 12, 60, muscleGroup = "Brazos", equipmentType = "Mancuerna", updateReminderDays = 30),
        ExerciseEntity("Tríceps en Polea Alta", 15, 3, 20, 60, muscleGroup = "Brazos", equipmentType = "Polea", updateReminderDays = 30),
        ExerciseEntity("Press Francés", 12, 3, 25, 60, muscleGroup = "Brazos", equipmentType = "Barra", updateReminderDays = 30),
        ExerciseEntity("Fondos en Paralelas", 10, 3, 0, 90, muscleGroup = "Brazos", equipmentType = "Peso Corporal", updateReminderDays = 30),

        // Core
        ExerciseEntity("Plancha", 1, 3, 0, 60, muscleGroup = "Core", equipmentType = "Peso Corporal", updateReminderDays = 30),
        ExerciseEntity("Crunch Abdominal", 20, 3, 0, 45, muscleGroup = "Core", equipmentType = "Peso Corporal", updateReminderDays = 30),
        ExerciseEntity("Elevación de Piernas", 15, 3, 0, 60, muscleGroup = "Core", equipmentType = "Peso Corporal", updateReminderDays = 30)
    )

    private val defaultRoutines = listOf(
        RoutineEntity("Pecho y Tríceps", "Press de Banca,Aperturas,Tríceps en Polea Alta,Flexiones"),
        RoutineEntity("Espalda y Bíceps", "Remo con Barra,Jalón al Pecho,Curl Martillo,Dominadas"),
        RoutineEntity("Piernas Completo", "Sentadillas,Prensa de Piernas,Extensión de Cuádriceps,Elevación de Talones"),
        RoutineEntity("Hombros y Core", "Press Militar,Vuelos Laterales,Plancha,Elevación de Piernas")
    )

    fun restoreDefaults(keepCustomExercises: Boolean, keepCustomRoutines: Boolean) = viewModelScope.launch {
        if (!keepCustomExercises) {
            repository.deleteAllExercises()
        }
        defaultExercises.forEach { repository.insertExercise(it) }

        if (!keepCustomRoutines) {
            repository.deleteAllRoutines()
        }
        defaultRoutines.forEach { repository.insertRoutine(it) }
    }

    // Mantenemos estas por compatibilidad si se usan en otros sitios o para simplificar
    fun restoreDefaultExercises(keepCustom: Boolean) = restoreDefaults(keepCustom, true)

    fun resetEverything() = viewModelScope.launch {
        repository.deleteAllExercises()
        repository.deleteAllRoutines()
        repository.deleteAllWorkoutHistory()
        repository.deleteAllWeight()
    }

    fun getExercisesByRoutine(routineName: String): Flow<List<ExerciseEntity>> {
        return routines.map { list ->
            val routine = list.find { it.name == routineName }
            val names = routine?.exerciseNames?.split(",") ?: emptyList()
            exercises.value.filter { it.name in names }
        }
    }

    fun saveWorkoutSession(routineId: String) {
        logCompletedWorkout(routineId)
        Log.d("GymViewModel", "Entrenamiento completado: $routineId")
    }

    // --- ANALYTICS ---
    val analyticsStats: StateFlow<Map<String, Any>> by lazy {
        combine(history, weightHistory) { hist, weightHist ->
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            
            val recentWeight = weightHist.filter { it.date >= thirtyDaysAgo }.sortedBy { it.date }
            val maxWeight = recentWeight.maxOfOrNull { it.weight } ?: 0.0
            val avgWeight = if (recentWeight.isNotEmpty()) recentWeight.map { it.weight }.average() else 0.0
            
            val trend = if (recentWeight.size >= 2) {
                val first = recentWeight.first().weight
                val last = recentWeight.last().weight
                when {
                    last > first + 0.1 -> "↑"
                    last < first - 0.1 -> "↓"
                    else -> "→"
                }
            } else "→"

            // 1RM estimado (Epley) para el mejor levantamiento histórico
            val bestLift = hist.maxByOrNull { it.weight * (1 + it.reps / 30.0) }
            val estimated1RM = bestLift?.let { it.weight * (1 + it.reps / 30.0) } ?: 0.0

            mapOf(
                "maxWeight" to maxWeight,
                "avgWeight" to avgWeight,
                "trend" to trend,
                "estimated1RM" to estimated1RM
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    val muscleStats: StateFlow<Map<String, Pair<Double, List<WorkoutHistoryEntity>>>> by lazy {
        combine(history, exercises) { hist, exs ->
            val muscleGroups = exs.map { it.muscleGroup }.distinct()
            muscleGroups.associateWith { muscle ->
                val muscleExs = exs.filter { it.muscleGroup == muscle }.map { it.name }
                val muscleHist = hist.filter { it.exerciseName in muscleExs }
                val bestWeight = muscleHist.maxOfOrNull { it.weight } ?: 0.0
                val muscleHistory = muscleHist.sortedByDescending { it.date }.take(5)
                Pair(bestWeight, muscleHistory)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    private val _isWaitingForUser = MutableStateFlow(false)
    val isWaitingForUser: StateFlow<Boolean> = _isWaitingForUser

    fun confirmNextExercise() { _isWaitingForUser.value = false }

    // --- NOTIFICATIONS ---

    init {
        // Inicializar canal de notificaciones (operación rápida)
        NotificationHelper.createNotificationChannel(appContext)
        
        // Cargar datos iniciales en background
        viewModelScope.launch {
            try {
                val currentExercises = repository.allExercises.first()
                if (currentExercises.isEmpty()) {
                    defaultExercises.forEach { repository.insertExercise(it) }
                }

                val currentRoutines = repository.allRoutines.first()
                if (currentRoutines.isEmpty()) {
                    defaultRoutines.forEach { repository.insertRoutine(it) }
                }
            } catch (e: Exception) {
                Log.e("GymViewModel", "Error during initialization", e)
            }
        }
    }

    fun sendPRNotification(exerciseName: String, weight: Double) {
        val prefs = appContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val prNotificationsEnabled = prefs.getBoolean("pr_notifications_enabled", true)

        if (prNotificationsEnabled) {
            NotificationHelper.sendNotification(
                appContext,
                "¡Nuevo PR!",
                "Felicitaciones! Nuevo récord personal en $exerciseName: ${weight}kg"
            )
        }
    }

    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, programar para mañana
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            putExtra("title", "¡Hora de entrenar!")
            putExtra("message", "Es momento de tu rutina diaria")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleConsistencyCheck() {
        ConsistencyCheckWorker.scheduleWork(appContext)
    }

    fun cancelConsistencyCheck() {
        ConsistencyCheckWorker.cancelWork(appContext)
    }

    fun scheduleWeeklyGoalCheck() {
        WeeklyGoalWorker.scheduleWork(appContext)
    }

    fun cancelWeeklyGoalCheck() {
        WeeklyGoalWorker.cancelWork(appContext)
    }

    fun updateNotificationSettings(
        enabled: Boolean,
        dailyHour: Int,
        dailyMinute: Int,
        maxDaysWithoutWorkout: Int,
        prEnabled: Boolean,
        consistencyEnabled: Boolean,
        weeklyGoalEnabled: Boolean,
        weeklyGoal: Int
    ) {
        val prefs = appContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("notifications_enabled", enabled)
        editor.putInt("daily_reminder_hour", dailyHour)
        editor.putInt("daily_reminder_minute", dailyMinute)
        editor.putInt("max_days_without_workout", maxDaysWithoutWorkout)
        editor.putBoolean("pr_notifications_enabled", prEnabled)
        editor.putBoolean("consistency_notifications_enabled", consistencyEnabled)
        editor.putBoolean("weekly_goal_notifications_enabled", weeklyGoalEnabled)
        editor.putInt("weekly_workout_goal", weeklyGoal)
        editor.apply()

        if (enabled) {
            scheduleDailyReminder(dailyHour, dailyMinute)
            if (consistencyEnabled) scheduleConsistencyCheck()
            if (weeklyGoalEnabled) scheduleWeeklyGoalCheck()
        } else {
            cancelDailyReminder()
            cancelConsistencyCheck()
            cancelWeeklyGoalCheck()
        }
    }
}
