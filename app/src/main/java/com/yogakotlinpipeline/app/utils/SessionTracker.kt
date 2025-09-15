package com.yogakotlinpipeline.app.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*

object SessionTracker {
    private const val PREF_NAME = "session_tracker"
    private const val KEY_APP_OPEN_DATES = "app_open_dates"
    private const val KEY_TOTAL_SESSION_TIME = "total_session_time"
    private const val KEY_TOTAL_WORKOUTS = "total_workouts"
    private const val KEY_CURRENT_SESSION_START = "current_session_start"
    private const val KEY_LAST_SESSION_DATE = "last_session_date"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_TOTAL_POSES_COMPLETED = "total_poses_completed"
    private const val KEY_ADVANCED_POSES_ATTEMPTED = "advanced_poses_attempted"
    private const val KEY_CONSECUTIVE_POSES = "consecutive_poses"
    private const val KEY_BEST_CONSECUTIVE_POSES = "best_consecutive_poses"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Track app opening for streak calculation
     */
    fun trackAppOpen(context: Context) {
        val prefs = getSharedPreferences(context)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val openDates = getAppOpenDates(context).toMutableSet()
        val wasAlreadyOpenedToday = openDates.contains(today)
        
        if (!wasAlreadyOpenedToday) {
            openDates.add(today)
            prefs.edit().putStringSet(KEY_APP_OPEN_DATES, openDates.map { it.toString() }.toSet()).apply()
            updateStreak(context)
        }
    }

    /**
     * Start tracking a yoga session
     */
    fun startYogaSession(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putLong(KEY_CURRENT_SESSION_START, System.currentTimeMillis()).apply()
    }

    /**
     * End tracking a yoga session and update totals
     */
    fun endYogaSession(context: Context, posesCompleted: Int = 0) {
        val prefs = getSharedPreferences(context)
        val sessionStart = prefs.getLong(KEY_CURRENT_SESSION_START, 0L)
        
        if (sessionStart > 0) {
            val sessionDuration = (System.currentTimeMillis() - sessionStart) / 1000 // in seconds
            val totalTime = prefs.getLong(KEY_TOTAL_SESSION_TIME, 0L) + sessionDuration
            val totalWorkouts = prefs.getInt(KEY_TOTAL_WORKOUTS, 0) + 1
            
            prefs.edit()
                .putLong(KEY_TOTAL_SESSION_TIME, totalTime)
                .putInt(KEY_TOTAL_WORKOUTS, totalWorkouts)
                .putLong(KEY_CURRENT_SESSION_START, 0L)
                .putLong(KEY_LAST_SESSION_DATE, System.currentTimeMillis())
                .apply()

            if (posesCompleted > 0) {
                addPosesCompleted(context, posesCompleted)
            }
        }
    }

    /**
     * Add completed poses to total count
     */
    fun addPosesCompleted(context: Context, count: Int) {
        val prefs = getSharedPreferences(context)
        val currentTotal = prefs.getInt(KEY_TOTAL_POSES_COMPLETED, 0)
        val currentConsecutive = prefs.getInt(KEY_CONSECUTIVE_POSES, 0)
        val bestConsecutive = prefs.getInt(KEY_BEST_CONSECUTIVE_POSES, 0)
        
        val newTotal = currentTotal + count
        val newConsecutive = currentConsecutive + count
        val newBestConsecutive = maxOf(bestConsecutive, newConsecutive)
        
        prefs.edit()
            .putInt(KEY_TOTAL_POSES_COMPLETED, newTotal)
            .putInt(KEY_CONSECUTIVE_POSES, newConsecutive)
            .putInt(KEY_BEST_CONSECUTIVE_POSES, newBestConsecutive)
            .apply()
    }

    /**
     * Track attempt of advanced pose
     */
    fun trackAdvancedPoseAttempt(context: Context) {
        val prefs = getSharedPreferences(context)
        val current = prefs.getInt(KEY_ADVANCED_POSES_ATTEMPTED, 0)
        prefs.edit().putInt(KEY_ADVANCED_POSES_ATTEMPTED, current + 1).apply()
    }

    /**
     * Reset consecutive poses count (when user skips a day)
     */
    fun resetConsecutivePoses(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putInt(KEY_CONSECUTIVE_POSES, 0).apply()
    }

    /**
     * Get app open dates for streak calculation
     */
    private fun getAppOpenDates(context: Context): Set<Long> {
        val prefs = getSharedPreferences(context)
        val dateStrings = prefs.getStringSet(KEY_APP_OPEN_DATES, emptySet()) ?: emptySet()
        return dateStrings.mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * Update streak based on app open dates
     */
    private fun updateStreak(context: Context) {
        val prefs = getSharedPreferences(context)
        val openDates = getAppOpenDates(context).sortedDescending()
        
        if (openDates.isEmpty()) {
            prefs.edit()
                .putInt(KEY_CURRENT_STREAK, 0)
                .putInt(KEY_LONGEST_STREAK, 0)
                .apply()
            return
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        val calendar = Calendar.getInstance()
        
        // Calculate current streak
        for (i in 0 until 365) { // Check last year
            calendar.timeInMillis = today - (i * 24 * 60 * 60 * 1000L)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val checkDate = calendar.timeInMillis
            
            if (openDates.contains(checkDate)) {
                if (i == 0 || tempStreak == i - 1) {
                    tempStreak = i
                    currentStreak = i + 1
                } else {
                    break
                }
            } else if (i == 0) {
                // Today not opened, check yesterday
                continue
            } else {
                break
            }
        }

        // Calculate longest streak
        tempStreak = 0
        val sortedDates = openDates.sorted()
        for (i in sortedDates.indices) {
            if (i == 0) {
                tempStreak = 1
            } else {
                val daysDiff = (sortedDates[i] - sortedDates[i - 1]) / (24 * 60 * 60 * 1000L)
                if (daysDiff == 1L) {
                    tempStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                }
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .apply()
    }

    /**
     * Get current day streak
     */
    fun getCurrentStreak(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    /**
     * Get longest streak
     */
    fun getLongestStreak(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_LONGEST_STREAK, 0)
    }

    /**
     * Get total session time in minutes
     */
    fun getTotalSessionTimeMinutes(context: Context): Long {
        val prefs = getSharedPreferences(context)
        return prefs.getLong(KEY_TOTAL_SESSION_TIME, 0L) / 60
    }

    /**
     * Get total workouts count
     */
    fun getTotalWorkouts(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_TOTAL_WORKOUTS, 0)
    }

    /**
     * Get total poses completed
     */
    fun getTotalPosesCompleted(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_TOTAL_POSES_COMPLETED, 0)
    }

    /**
     * Get advanced poses attempted
     */
    fun getAdvancedPosesAttempted(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_ADVANCED_POSES_ATTEMPTED, 0)
    }

    /**
     * Get consecutive poses completed
     */
    fun getConsecutivePoses(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_CONSECUTIVE_POSES, 0)
    }

    /**
     * Get best consecutive poses
     */
    fun getBestConsecutivePoses(context: Context): Int {
        val prefs = getSharedPreferences(context)
        return prefs.getInt(KEY_BEST_CONSECUTIVE_POSES, 0)
    }

    /**
     * Get last session date
     */
    fun getLastSessionDate(context: Context): Long {
        val prefs = getSharedPreferences(context)
        return prefs.getLong(KEY_LAST_SESSION_DATE, 0L)
    }

    /**
     * Format time duration for display
     */
    fun formatTimeDuration(minutes: Long): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
            else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
        }
    }

    /**
     * Check if user has been active today
     */
    fun hasBeenActiveToday(context: Context): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return getAppOpenDates(context).contains(today)
    }
}
