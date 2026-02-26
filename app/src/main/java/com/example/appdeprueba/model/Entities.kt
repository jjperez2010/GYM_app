package com.example.appdeprueba.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val name: String,
    val reps: Int,
    val sets: Int,
    val weight: Int,
    val rest: Int,
    val muscleGroup: String,
    val equipmentType: String = "Peso Corporal"
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val name: String,
    val exerciseNames: String // CSV of exercise names
)

@Entity(tableName = "workout_history")
data class WorkoutHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Timestamp
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val sets: Int,
    val volume: Double
)

@Entity(tableName = "weight_history")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weight: Double
)
