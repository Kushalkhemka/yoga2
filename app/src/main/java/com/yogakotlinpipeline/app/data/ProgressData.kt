package com.yogakotlinpipeline.app.data

import java.util.*

data class WorkoutSession(
    val id: String,
    val date: Date,
    val duration: Int, // in minutes
    val calories: Int,
    val poseCount: Int,
    val flexibilityScore: Float,
    val poseType: String
)

data class WeeklyProgress(
    val weekStartDate: Date,
    val totalWorkouts: Int,
    val totalDuration: Int, // in minutes
    val totalCalories: Int,
    val averageFlexibilityScore: Float,
    val dailySessions: List<DailySession>
)

data class DailySession(
    val date: Date,
    val duration: Int, // in minutes
    val calories: Int,
    val flexibilityScore: Float
)

data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastWorkoutDate: Date?
)

data class GoalProgress(
    val weeklyWorkoutsGoal: Int,
    val currentWeeklyWorkouts: Int,
    val flexibilityGoal: Float,
    val currentFlexibilityScore: Float
)

// Sample data generator for real-time updates
object ProgressDataGenerator {
    
    fun generateWeeklyData(): WeeklyProgress {
        val calendar = Calendar.getInstance()
        val dailySessions = mutableListOf<DailySession>()
        
        // Generate data for the last 7 days
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            val duration = (20..60).random()
            val calories = (duration * 8..duration * 12).random()
            val flexibilityScore = (60..95).random().toFloat()
            
            dailySessions.add(
                DailySession(
                    date = calendar.time,
                    duration = duration,
                    calories = calories,
                    flexibilityScore = flexibilityScore
                )
            )
        }
        
        return WeeklyProgress(
            weekStartDate = calendar.time,
            totalWorkouts = dailySessions.count { it.duration > 0 },
            totalDuration = dailySessions.sumOf { it.duration },
            totalCalories = dailySessions.sumOf { it.calories },
            averageFlexibilityScore = dailySessions.map { it.flexibilityScore }.average().toFloat(),
            dailySessions = dailySessions
        )
    }
    
    fun generateStreakData(): StreakData {
        return StreakData(
            currentStreak = (3..15).random(),
            longestStreak = (15..30).random(),
            lastWorkoutDate = Date()
        )
    }
    
    fun generateGoalProgress(): GoalProgress {
        return GoalProgress(
            weeklyWorkoutsGoal = 5,
            currentWeeklyWorkouts = (2..5).random(),
            flexibilityGoal = 80f,
            currentFlexibilityScore = (60..85).random().toFloat()
        )
    }
}
