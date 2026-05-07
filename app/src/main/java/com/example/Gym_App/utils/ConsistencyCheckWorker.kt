package com.example.Gym_App.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.Gym_App.database.GymDatabase
import com.example.Gym_App.repository.GymRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ConsistencyCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): ListenableWorker.Result {
        return try {
            runBlocking {
                val db = GymDatabase.getDatabase(applicationContext)
                val repository = GymRepository(db.gymDao())

                val workoutHistory = repository.workoutHistory.first()
                val workoutDates = workoutHistory.map {
                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                }.distinct().sortedDescending()

                val today = LocalDate.now()
                val lastWorkoutDate = workoutDates.firstOrNull()

                if (lastWorkoutDate != null) {
                    val daysSinceLastWorkout = java.time.temporal.ChronoUnit.DAYS.between(lastWorkoutDate, today).toInt()

                    // Obtener configuración de SharedPreferences
                    val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
                    val maxDaysWithoutWorkout = prefs.getInt("max_days_without_workout", 3)

                    if (notificationsEnabled && daysSinceLastWorkout >= maxDaysWithoutWorkout) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "¡Hora de entrenar!",
                            "Hace $daysSinceLastWorkout días que no entrenas. ¡Es momento de volver!"
                        )
                    }
                } else {
                    // Nunca ha entrenado
                    val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

                    if (notificationsEnabled) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "¡Comienza tu viaje fitness!",
                            "Aún no has registrado ningún entrenamiento. ¡Empieza hoy!"
                        )
                    }
                }
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }

    companion object {
        fun scheduleWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ConsistencyCheckWorker>(
                1, TimeUnit.DAYS
            ).setInitialDelay(1, TimeUnit.HOURS) // Primera ejecución en 1 hora
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "consistency_check",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("consistency_check")
        }
    }
}
