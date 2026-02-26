package com.example.appdeprueba.repository

import com.example.appdeprueba.database.GymDao
import com.example.appdeprueba.model.ExerciseEntity
import com.example.appdeprueba.model.RoutineEntity
import com.example.appdeprueba.model.WorkoutHistoryEntity
import com.example.appdeprueba.model.WeightEntity
import kotlinx.coroutines.flow.Flow

class GymRepository(private val gymDao: GymDao) {
    val allExercises: Flow<List<ExerciseEntity>> = gymDao.getAllExercises()
    val allRoutines: Flow<List<RoutineEntity>> = gymDao.getAllRoutines()
    val workoutHistory: Flow<List<WorkoutHistoryEntity>> = gymDao.getHistory()
    val weightHistory: Flow<List<WeightEntity>> = gymDao.getWeightHistory()

    suspend fun insertExercise(exercise: ExerciseEntity) = gymDao.insertExercise(exercise)
    suspend fun deleteExercise(name: String) = gymDao.deleteExerciseByName(name)
    suspend fun deleteAllExercises() = gymDao.deleteAllExercises()

    suspend fun insertRoutine(routine: RoutineEntity) = gymDao.insertRoutine(routine)
    suspend fun deleteRoutine(name: String) = gymDao.deleteRoutineByName(name)
    suspend fun deleteAllRoutines() = gymDao.deleteAllRoutines()

    suspend fun logWorkout(history: WorkoutHistoryEntity) = gymDao.insertHistory(history)
    suspend fun deleteAllWorkoutHistory() = gymDao.deleteAllWorkoutHistory()
    
    suspend fun insertWeight(weight: WeightEntity) = gymDao.insertWeight(weight)
    suspend fun deleteAllWeight() = gymDao.deleteAllWeightEntries()

    fun getWeeklyVolume() = gymDao.getWeeklyVolume()
    fun getExerciseProgress(name: String) = gymDao.getExerciseProgress(name)
}
