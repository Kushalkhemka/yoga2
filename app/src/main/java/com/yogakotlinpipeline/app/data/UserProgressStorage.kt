package com.yogakotlinpipeline.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class UserProgressStorage(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_progress", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_WORKOUT_SESSIONS = "workout_sessions"
        private const val KEY_USER_STATS = "user_stats"
        private const val KEY_STREAK_DATA = "streak_data"
        private const val KEY_GOALS = "user_goals"
    }
    
    // Save workout session
    fun saveWorkoutSession(session: WorkoutSession) {
        val sessions = getWorkoutSessions().toMutableList()
        sessions.add(session)
        
        // Keep only last 30 days of sessions
        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.time
        
        val filteredSessions = sessions.filter { it.date.after(thirtyDaysAgo) }
        
        val sessionsJson = gson.toJson(filteredSessions)
        prefs.edit().putString(KEY_WORKOUT_SESSIONS, sessionsJson).apply()
        
        // Update streak data
        updateStreakData()
    }
    
    // Get all workout sessions
    fun getWorkoutSessions(): List<WorkoutSession> {
        val sessionsJson = prefs.getString(KEY_WORKOUT_SESSIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<WorkoutSession>>() {}.type
        return gson.fromJson(sessionsJson, type) ?: emptyList()
    }
    
    // Get weekly progress
    fun getWeeklyProgress(): WeeklyProgress {
        val sessions = getWorkoutSessions()
        val calendar = Calendar.getInstance()
        
        // Get sessions from last 7 days
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        
        val weeklySessions = sessions.filter { it.date.after(sevenDaysAgo) }
        
        val dailySessions = mutableListOf<DailySession>()
        
        // Create daily sessions for the last 7 days
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = calendar.time
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.time
            
            val daySessions = weeklySessions.filter { 
                it.date.after(dayStart) && it.date.before(dayEnd) 
            }
            
            val totalDuration = daySessions.sumOf { it.duration }
            val totalCalories = daySessions.sumOf { it.calories }
            val avgFlexibility = if (daySessions.isNotEmpty()) {
                daySessions.map { it.flexibilityScore }.average().toFloat()
            } else {
                0f
            }
            
            dailySessions.add(
                DailySession(
                    date = dayStart,
                    duration = totalDuration,
                    calories = totalCalories,
                    flexibilityScore = avgFlexibility
                )
            )
        }
        
        return WeeklyProgress(
            weekStartDate = sevenDaysAgo,
            totalWorkouts = weeklySessions.size,
            totalDuration = weeklySessions.sumOf { it.duration },
            totalCalories = weeklySessions.sumOf { it.calories },
            averageFlexibilityScore = if (weeklySessions.isNotEmpty()) {
                weeklySessions.map { it.flexibilityScore }.average().toFloat()
            } else {
                0f
            },
            dailySessions = dailySessions
        )
    }
    
    // Get streak data
    fun getStreakData(): StreakData {
        val streakJson = prefs.getString(KEY_STREAK_DATA, null)
        if (streakJson != null) {
            return gson.fromJson(streakJson, StreakData::class.java)
        }
        
        // Initialize with default values
        val defaultStreak = StreakData(
            currentStreak = 0,
            longestStreak = 0,
            lastWorkoutDate = null
        )
        saveStreakData(defaultStreak)
        return defaultStreak
    }
    
    // Save streak data
    private fun saveStreakData(streakData: StreakData) {
        val streakJson = gson.toJson(streakData)
        prefs.edit().putString(KEY_STREAK_DATA, streakJson).apply()
    }
    
    // Update streak data based on recent sessions
    private fun updateStreakData() {
        val sessions = getWorkoutSessions().sortedByDescending { it.date }
        val currentStreak = calculateCurrentStreak(sessions)
        val longestStreak = calculateLongestStreak(sessions)
        val lastWorkoutDate = sessions.firstOrNull()?.date
        
        val streakData = StreakData(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastWorkoutDate = lastWorkoutDate
        )
        
        saveStreakData(streakData)
    }
    
    // Calculate current streak
    private fun calculateCurrentStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        var streak = 0
        var currentDate = calendar.time
        
        for (session in sessions) {
            val sessionDate = session.date
            val daysDifference = ((currentDate.time - sessionDate.time) / (1000 * 60 * 60 * 24)).toInt()
            
            if (daysDifference == streak) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = calendar.time
            } else {
                break
            }
        }
        
        return streak
    }
    
    // Calculate longest streak
    private fun calculateLongestStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val sessionsByDate = sessions.groupBy { 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date)
        }
        
        val sortedDates = sessionsByDate.keys.sorted()
        var maxStreak = 0
        var currentStreak = 0
        var lastDate: String? = null
        
        for (date in sortedDates) {
            if (lastDate == null) {
                currentStreak = 1
            } else {
                val lastCalendar = Calendar.getInstance().apply {
                    time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastDate)!!
                }
                val currentCalendar = Calendar.getInstance().apply {
                    time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
                }
                
                val daysDifference = ((currentCalendar.timeInMillis - lastCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                
                if (daysDifference == 1) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
            lastDate = date
        }
        
        return maxOf(maxStreak, currentStreak)
    }
    
    // Get user goals
    fun getUserGoals(): GoalProgress {
        val goalsJson = prefs.getString(KEY_GOALS, null)
        if (goalsJson != null) {
            return gson.fromJson(goalsJson, GoalProgress::class.java)
        }
        
        // Initialize with default goals
        val defaultGoals = GoalProgress(
            weeklyWorkoutsGoal = 5,
            currentWeeklyWorkouts = 0,
            flexibilityGoal = 80f,
            currentFlexibilityScore = 0f
        )
        saveUserGoals(defaultGoals)
        return defaultGoals
    }
    
    // Save user goals
    fun saveUserGoals(goals: GoalProgress) {
        val goalsJson = gson.toJson(goals)
        prefs.edit().putString(KEY_GOALS, goalsJson).apply()
    }
    
    // Update goals based on current progress
    fun updateGoalsProgress() {
        val weeklyProgress = getWeeklyProgress()
        val currentGoals = getUserGoals()
        
        val updatedGoals = currentGoals.copy(
            currentWeeklyWorkouts = weeklyProgress.totalWorkouts,
            currentFlexibilityScore = weeklyProgress.averageFlexibilityScore
        )
        
        saveUserGoals(updatedGoals)
    }
    
    // Add sample data for demonstration
    fun addSampleData() {
        val calendar = Calendar.getInstance()
        val sessions = mutableListOf<WorkoutSession>()
        
        // Add sessions for the last 7 days
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            // Skip some days to make it realistic
            if (i == 1 || i == 4) continue
            
            val session = WorkoutSession(
                id = "session_$i",
                date = calendar.time,
                duration = (25..45).random(),
                calories = (200..400).random(),
                poseCount = (8..15).random(),
                flexibilityScore = (65..85).random().toFloat(),
                poseType = "Morning Flow"
            )
            sessions.add(session)
        }
        
        // Save all sessions
        sessions.forEach { saveWorkoutSession(it) }
        
        // Update goals
        updateGoalsProgress()
    }
    
    // Clear all data (for testing)
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
