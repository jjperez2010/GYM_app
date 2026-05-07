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
import java.time.temporal.WeekFields
import java.util.*
import java.util.concurrent.TimeUnit

class WeeklyGoalWorker(
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
                }.distinct()

                val now = LocalDate.now()
                val weekFields = WeekFields.of(Locale.getDefault())
                val currentWeek = now.get(weekFields.weekOfWeekBasedYear())

                val thisWeekWorkouts = workoutDates.filter { date ->
                    val workoutWeek = date.get(weekFields.weekOfWeekBasedYear())
                    workoutWeek == currentWeek && date.year == now.year
                }

                val workoutsThisWeek = thisWeekWorkouts.size

                // Obtener configuración
                val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
                val weeklyGoal = prefs.getInt("weekly_workout_goal", 4) // Por defecto 4 sesiones/semana

                if (notificationsEnabled && workoutsThisWeek >= weeklyGoal) {
                    NotificationHelper.sendNotification(
                        applicationContext,
                        "¡Semana completa!",
                        "¡Felicitaciones! Has completado tu objetivo semanal de $weeklyGoal entrenamientos."
                    )
                }
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }

    companion object {
        fun scheduleWork(context: Context) {
            // Ejecutar semanalmente, los domingos a las 20:00
            val workRequest = PeriodicWorkRequestBuilder<WeeklyGoalWorker>(
                7, TimeUnit.DAYS
            ).setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "weekly_goal_check",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("weekly_goal_check")
        }

        private fun calculateInitialDelay(): Long {
            val now = Calendar.getInstance()
            val targetTime = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Si ya pasó el domingo, programar para el próximo
                if (before(now)) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            return targetTime.timeInMillis - now.timeInMillis
        }
    }
}
