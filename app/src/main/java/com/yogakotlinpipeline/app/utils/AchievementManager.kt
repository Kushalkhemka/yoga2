package com.yogakotlinpipeline.app.utils

import android.content.Context

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: String = "ic_star",
    val isUnlocked: Boolean = false,
    val unlockedDate: Long = 0L
)

object AchievementManager {
    
    /**
     * Get recent achievements based on user progress
     */
    fun getRecentAchievements(context: Context): List<Achievement> {
        val achievements = mutableListOf<Achievement>()
        val sessionTracker = SessionTracker
        val userProfile = LoginCache.getInstance(context).getUserProfile()
        
        val currentStreak = sessionTracker.getCurrentStreak(context)
        val totalWorkouts = sessionTracker.getTotalWorkouts(context)
        val totalPoses = sessionTracker.getTotalPosesCompleted(context)
        val advancedPoses = sessionTracker.getAdvancedPosesAttempted(context)
        val consecutivePoses = sessionTracker.getConsecutivePoses(context)
        val bestConsecutive = sessionTracker.getBestConsecutivePoses(context)
        
        // Streak achievements
        when {
            currentStreak >= 7 -> achievements.add(
                Achievement(
                    id = "streak_7",
                    title = "7-Day Streak",
                    description = "Completed 7 consecutive days of yoga",
                    isUnlocked = true
                )
            )
            currentStreak >= 3 -> achievements.add(
                Achievement(
                    id = "streak_3",
                    title = "3-Day Streak",
                    description = "Completed 3 consecutive days of yoga",
                    isUnlocked = true
                )
            )
            currentStreak >= 1 -> achievements.add(
                Achievement(
                    id = "streak_1",
                    title = "First Day",
                    description = "Completed your first day of yoga",
                    isUnlocked = true
                )
            )
        }
        
        // Workout count achievements
        when {
            totalWorkouts >= 50 -> achievements.add(
                Achievement(
                    id = "workouts_50",
                    title = "Yoga Master",
                    description = "Completed 50 yoga workouts",
                    isUnlocked = true
                )
            )
            totalWorkouts >= 20 -> achievements.add(
                Achievement(
                    id = "workouts_20",
                    title = "Consistency Champion",
                    description = "Completed 20 yoga workouts",
                    isUnlocked = true
                )
            )
            totalWorkouts >= 10 -> achievements.add(
                Achievement(
                    id = "workouts_10",
                    title = "Dedicated Practitioner",
                    description = "Completed 10 yoga workouts",
                    isUnlocked = true
                )
            )
            totalWorkouts >= 5 -> achievements.add(
                Achievement(
                    id = "workouts_5",
                    title = "Getting Started",
                    description = "Completed 5 yoga workouts",
                    isUnlocked = true
                )
            )
        }
        
        // Pose completion achievements
        when {
            bestConsecutive >= 10 -> achievements.add(
                Achievement(
                    id = "poses_consecutive_10",
                    title = "Pose Master",
                    description = "Completed 10 poses in a row",
                    isUnlocked = true
                )
            )
            bestConsecutive >= 5 -> achievements.add(
                Achievement(
                    id = "poses_consecutive_5",
                    title = "Flow Master",
                    description = "Completed 5 poses in a row",
                    isUnlocked = true
                )
            )
            consecutivePoses >= 3 -> achievements.add(
                Achievement(
                    id = "poses_consecutive_3",
                    title = "In the Flow",
                    description = "Completed 3 poses in a row",
                    isUnlocked = true
                )
            )
        }
        
        // Advanced pose achievements
        if (advancedPoses > 0) {
            achievements.add(
                Achievement(
                    id = "advanced_pose",
                    title = "Challenge Accepted",
                    description = "You have tried an advanced asana! Keep going!",
                    isUnlocked = true
                )
            )
        }
        
        // Level-based achievements
        when (userProfile.level.lowercase()) {
            "intermediate" -> achievements.add(
                Achievement(
                    id = "level_intermediate",
                    title = "Level Up",
                    description = "Reached Intermediate level",
                    isUnlocked = true
                )
            )
            "advanced" -> achievements.add(
                Achievement(
                    id = "level_advanced",
                    title = "Advanced Practitioner",
                    description = "Reached Advanced level",
                    isUnlocked = true
                )
            )
        }
        
        // Time-based achievements
        val totalTimeMinutes = sessionTracker.getTotalSessionTimeMinutes(context)
        when {
            totalTimeMinutes >= 1440 -> achievements.add( // 24 hours
                Achievement(
                    id = "time_24h",
                    title = "Time Master",
                    description = "Spent 24+ hours practicing yoga",
                    isUnlocked = true
                )
            )
            totalTimeMinutes >= 600 -> achievements.add( // 10 hours
                Achievement(
                    id = "time_10h",
                    title = "Dedicated Time",
                    description = "Spent 10+ hours practicing yoga",
                    isUnlocked = true
                )
            )
            totalTimeMinutes >= 300 -> achievements.add( // 5 hours
                Achievement(
                    id = "time_5h",
                    title = "Time Investment",
                    description = "Spent 5+ hours practicing yoga",
                    isUnlocked = true
                )
            )
        }
        
        // Return the 3 most recent achievements (or all if less than 3)
        return achievements.takeLast(3).reversed()
    }
    
    /**
     * Get achievement suggestions for newbies
     */
    fun getNewbieAchievements(context: Context): List<Achievement> {
        val achievements = mutableListOf<Achievement>()
        val sessionTracker = SessionTracker
        val userProfile = LoginCache.getInstance(context).getUserProfile()
        
        val currentStreak = sessionTracker.getCurrentStreak(context)
        val totalWorkouts = sessionTracker.getTotalWorkouts(context)
        val totalPoses = sessionTracker.getTotalPosesCompleted(context)
        
        // Encourage first steps
        if (currentStreak == 0 && totalWorkouts == 0) {
            achievements.add(
                Achievement(
                    id = "newbie_start",
                    title = "Ready to Begin",
                    description = "Complete your first yoga session to unlock this achievement!",
                    isUnlocked = false
                )
            )
        }
        
        if (totalWorkouts == 0) {
            achievements.add(
                Achievement(
                    id = "newbie_first_workout",
                    title = "First Steps",
                    description = "Complete your first yoga workout",
                    isUnlocked = false
                )
            )
        }
        
        if (currentStreak == 0 && totalWorkouts > 0) {
            achievements.add(
                Achievement(
                    id = "newbie_streak",
                    title = "Build Momentum",
                    description = "Complete yoga sessions on consecutive days",
                    isUnlocked = false
                )
            )
        }
        
        if (totalPoses < 5) {
            achievements.add(
                Achievement(
                    id = "newbie_poses",
                    title = "Pose Explorer",
                    description = "Complete 5 different yoga poses",
                    isUnlocked = false
                )
            )
        }
        
        return achievements.take(3)
    }
    
    /**
     * Check if user is a newbie (less than 7 days streak and less than 10 workouts)
     */
    fun isNewbie(context: Context): Boolean {
        val sessionTracker = SessionTracker
        val currentStreak = sessionTracker.getCurrentStreak(context)
        val totalWorkouts = sessionTracker.getTotalWorkouts(context)
        
        return currentStreak < 7 && totalWorkouts < 10
    }
}
