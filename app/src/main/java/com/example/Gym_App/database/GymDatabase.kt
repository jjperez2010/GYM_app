package com.example.Gym_App.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.Gym_App.model.ExerciseEntity
import com.example.Gym_App.model.RoutineEntity
import com.example.Gym_App.model.WorkoutHistoryEntity
import com.example.Gym_App.model.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // Exercises
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @androidx.room.Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE name = :name")
    suspend fun deleteExerciseByName(name: String): Int

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    // Routines
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Query("DELETE FROM routines WHERE name = :name")
    suspend fun deleteRoutineByName(name: String): Int

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()

    // Workout History
    @Insert
    suspend fun insertHistory(history: WorkoutHistoryEntity): Long

    @Query("SELECT * FROM workout_history ORDER BY date DESC")
    fun getHistory(): Flow<List<WorkoutHistoryEntity>>

    @Query("DELETE FROM workout_history")
    suspend fun deleteAllWorkoutHistory()

    // Weight History
    @Insert
    suspend fun insertWeight(weight: WeightEntity): Long

    @Query("SELECT * FROM weight_history ORDER BY date DESC")
    fun getWeightHistory(): Flow<List<WeightEntity>>

    @Query("DELETE FROM weight_history")
    suspend fun deleteAllWeightEntries()

    // Stats queries
    @Query("SELECT date, SUM(volume) as totalVolume FROM workout_history GROUP BY date ORDER BY date DESC LIMIT 7")
    fun getWeeklyVolume(): Flow<List<VolumeStat>>

    @Query("SELECT * FROM workout_history WHERE exerciseName = :exerciseName ORDER BY date ASC")
    fun getExerciseProgress(exerciseName: String): Flow<List<WorkoutHistoryEntity>>
    
    @Query("SELECT exerciseName, MAX(weight) as maxWeight FROM workout_history GROUP BY exerciseName")
    fun getMaxWeights(): Flow<List<MaxWeightStat>>
}

data class VolumeStat(val date: Long, val totalVolume: Int)
data class MaxWeightStat(val exerciseName: String, val maxWeight: Int)

@Database(entities = [ExerciseEntity::class, RoutineEntity::class, WorkoutHistoryEntity::class, WeightEntity::class], version = 7)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: android.content.Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
