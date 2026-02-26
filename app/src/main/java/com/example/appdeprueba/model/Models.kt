package com.example.appdeprueba.model

data class Exercise(
    val name: String,
    val reps: Int,
    val sets: Int,
    val weight: Int,
    val rest: Int,
    val muscleGroup: String
)

data class Routine(
    val name: String,
    val exerciseNames: List<String>
)
