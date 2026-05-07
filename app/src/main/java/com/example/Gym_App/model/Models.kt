package com.example.Gym_App.model

data class Exercise(
    val name: String,
    val reps: Int,
    val sets: Int,
    val weight: Int,
    val rest: Int,
    val duration: Int = 60,
    val muscleGroup: String,
    val updateReminderDays: Int = 30,
    val lastUpdateDate: Long = 0
)

data class Routine(
    val name: String,
    val exerciseNames: List<String>
)
