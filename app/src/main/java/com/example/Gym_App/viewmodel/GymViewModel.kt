package com.example.Gym_App.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.Gym_App.database.GymDatabase
import com.example.Gym_App.model.ExerciseEntity
import com.example.Gym_App.model.RoutineEntity
import com.example.Gym_App.model.WorkoutHistoryEntity
import com.example.Gym_App.model.WeightEntity
import com.example.Gym_App.repository.GymRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GymViewModel(context: Context) : ViewModel() {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        GymDatabase::class.java, "gym_database"
    ).fallbackToDestructiveMigrationOnDowngrade(true).build()
    
    private val repository = GymRepository(db.gymDao())

    val exercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<WorkoutHistoryEntity>> = repository.workoutHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightHistory: StateFlow<List<WeightEntity>> = repository.weightHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutDates: StateFlow<List<LocalDate>> = history.map { list ->
        list.map { 
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() 
        }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- LÓGICA DE FILTRADO PARA EJERCICIOS ---
    private val _selectedMuscle = MutableStateFlow<String?>(null)
    private val _selectedEquipment = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val filteredExercises: StateFlow<List<ExerciseEntity>> = combine(
        exercises, _selectedMuscle, _selectedEquipment, _searchQuery
    ) { list, muscle, equip, query ->
        list.filter { ex ->
            (muscle == null || ex.muscleGroup == muscle) &&
            (equip == null || ex.equipmentType == equip) &&
            (query.isEmpty() || ex.name.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        ExerciseEntity("Press de Banca", 10, 3, 60, 90, "Pecho", "Barra"),
        ExerciseEntity("Press Inclinado", 10, 3, 50, 90, "Pecho", "Barra"),
        ExerciseEntity("Aperturas", 12, 3, 12, 60, "Pecho", "Mancuerna"),
        ExerciseEntity("Flexiones", 15, 3, 0, 60, "Pecho", "Peso Corporal"),
        ExerciseEntity("Cruce de Poleas", 15, 3, 15, 60, "Pecho", "Polea"),

        // Espalda
        ExerciseEntity("Peso Muerto", 8, 3, 100, 150, "Espalda", "Barra"),
        ExerciseEntity("Dominadas", 8, 3, 0, 90, "Espalda", "Peso Corporal"),
        ExerciseEntity("Remo con Barra", 10, 3, 60, 90, "Espalda", "Barra"),
        ExerciseEntity("Jalón al Pecho", 12, 3, 50, 60, "Espalda", "Polea"),
        ExerciseEntity("Remo en Polea Baja", 12, 3, 45, 60, "Espalda", "Polea"),

        // Piernas
        ExerciseEntity("Sentadillas", 12, 4, 80, 120, "Piernas", "Barra"),
        ExerciseEntity("Prensa de Piernas", 12, 3, 120, 90, "Piernas", "Máquina"),
        ExerciseEntity("Zancadas", 12, 3, 20, 60, "Piernas", "Mancuerna"),
        ExerciseEntity("Extensión de Cuádriceps", 15, 3, 40, 60, "Piernas", "Máquina"),
        ExerciseEntity("Curl Femoral", 15, 3, 35, 60, "Piernas", "Máquina"),
        ExerciseEntity("Elevación de Talones", 20, 4, 50, 45, "Piernas", "Máquina"),

        // Hombros
        ExerciseEntity("Press Militar", 10, 3, 40, 90, "Hombros", "Barra"),
        ExerciseEntity("Vuelos Laterales", 12, 3, 10, 60, "Hombros", "Mancuerna"),
        ExerciseEntity("Press Arnold", 10, 3, 15, 60, "Hombros", "Mancuerna"),
        ExerciseEntity("Pájaros", 15, 3, 8, 60, "Hombros", "Mancuerna"),
        ExerciseEntity("Remo al Mentón", 12, 3, 30, 60, "Hombros", "Barra"),

        // Brazos
        ExerciseEntity("Curl de Bíceps con Barra", 12, 3, 30, 60, "Brazos", "Barra"),
        ExerciseEntity("Curl Martillo", 12, 3, 12, 60, "Brazos", "Mancuerna"),
        ExerciseEntity("Tríceps en Polea Alta", 15, 3, 20, 60, "Brazos", "Polea"),
        ExerciseEntity("Press Francés", 12, 3, 25, 60, "Brazos", "Barra"),
        ExerciseEntity("Fondos en Paralelas", 10, 3, 0, 90, "Brazos", "Peso Corporal"),

        // Core
        ExerciseEntity("Plancha", 1, 3, 0, 60, "Core", "Peso Corporal"),
        ExerciseEntity("Crunch Abdominal", 20, 3, 0, 45, "Core", "Peso Corporal"),
        ExerciseEntity("Elevación de Piernas", 15, 3, 0, 60, "Core", "Peso Corporal")
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

    private val _isWaitingForUser = MutableStateFlow(false)
    val isWaitingForUser: StateFlow<Boolean> = _isWaitingForUser

    fun confirmNextExercise() { _isWaitingForUser.value = false }
}
