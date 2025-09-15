# Profile Tab Integration Guide

This guide explains how the updated profile tab now displays real user data and integrates with the user preferences journey.

## Features Implemented

### 1. Real Data Display
The profile tab now shows actual user data instead of hardcoded values:

- **Day Streak**: Calculated based on consecutive days the app is opened
- **Total Time**: Accumulated time spent in yoga sessions (video camera feed)
- **Workouts**: Number of yoga sessions completed
- **Personal Information**: Data from user preferences journey (age, level, join date)

### 2. Session Tracking System
A new `SessionTracker` utility class tracks:
- App opening dates for streak calculation
- Yoga session start/end times
- Total session duration
- Pose completion counts
- Advanced pose attempts

### 3. Achievement System
Dynamic achievements based on user progress:
- **Newbie Achievements**: For users with <7 day streak and <10 workouts
- **Progress Achievements**: Based on streaks, workout counts, pose completions
- **Level-based Achievements**: Unlocked when reaching intermediate/advanced levels

### 4. Edit Profile Functionality
Users can now edit their profile by:
- Clicking "Edit Profile" in the settings section
- Navigating through the preference fragments (Preference1 → Preference2 → Preference3)
- Updating their information and returning to the profile tab

## Integration Points

### Session Tracking Integration
To track yoga sessions in other fragments:

```kotlin
// Start session when user begins yoga practice
SessionTracker.startYogaSession(context)

// End session when user finishes
SessionTracker.endYogaSession(context, posesCompleted = 5)

// Track pose completions
SessionTracker.addPosesCompleted(context, count = 1)

// Track advanced pose attempts
SessionTracker.trackAdvancedPoseAttempt(context)
```

### Achievement Integration
To check and display achievements:

```kotlin
// Check if user is a newbie
val isNewbie = AchievementManager.isNewbie(context)

// Get recent achievements
val achievements = if (isNewbie) {
    AchievementManager.getNewbieAchievements(context)
} else {
    AchievementManager.getRecentAchievements(context)
}
```

### Profile Data Access
To access user profile data:

```kotlin
val loginCache = LoginCache.getInstance(context)
val userProfile = loginCache.getUserProfile()
val userName = loginCache.getUserName()
val userEmail = loginCache.getUserEmail()
```

## Data Storage

All data is stored using Android SharedPreferences:
- **Session Data**: `session_tracker` preferences
- **User Profile**: `login_cache` preferences
- **Achievements**: Calculated dynamically from session data

## Navigation Flow

### Edit Profile Flow
1. Profile Tab → Edit Profile Button
2. Preference1Fragment (Problem Areas)
3. Preference2Fragment (Goals)
4. Preference3Fragment (Personal Info)
5. Back to Profile Tab (with updated data)

### Navigation Parameters
The preference fragments now accept a `from_profile` parameter to determine the return destination:
- `from_profile = true`: Returns to Profile Tab
- `from_profile = false`: Returns to Main App (Inside1Fragment)

## Files Modified

### New Files
- `SessionTracker.kt`: Session tracking utility
- `AchievementManager.kt`: Achievement system
- `PROFILE_INTEGRATION_GUIDE.md`: This guide

### Modified Files
- `ProfileFragment.kt`: Updated to display real data
- `fragment_profile.xml`: Added IDs to TextViews for dynamic updates
- `nav_graph.xml`: Added navigation actions for edit profile flow
- `Preference1Fragment.kt`: Added parameter passing for edit flow
- `Preference2Fragment.kt`: Added parameter passing for edit flow
- `Preference3Fragment.kt`: Added conditional navigation based on source
- `PoseDetectionFragment.kt`: Added session tracking integration

## Usage Examples

### Tracking a Yoga Session
```kotlin
class YogaPracticeFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Start tracking when user begins practice
        SessionTracker.startYogaSession(requireContext())
        
        // Track pose completions during practice
        SessionTracker.addPosesCompleted(requireContext(), 1)
        
        // Track advanced pose attempts
        SessionTracker.trackAdvancedPoseAttempt(requireContext())
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // End session when user finishes
        SessionTracker.endYogaSession(requireContext())
    }
}
```

### Displaying User Stats
```kotlin
class StatsFragment : Fragment() {
    private fun displayUserStats() {
        val context = requireContext()
        
        val streak = SessionTracker.getCurrentStreak(context)
        val totalTime = SessionTracker.getTotalSessionTimeMinutes(context)
        val workouts = SessionTracker.getTotalWorkouts(context)
        val poses = SessionTracker.getTotalPosesCompleted(context)
        
        // Display the stats in UI
        binding.tvStreak.text = streak.toString()
        binding.tvTotalTime.text = SessionTracker.formatTimeDuration(totalTime)
        binding.tvWorkouts.text = workouts.toString()
        binding.tvPoses.text = poses.toString()
    }
}
```

## Future Enhancements

1. **Database Integration**: Replace SharedPreferences with Room database for better data management
2. **Cloud Sync**: Sync user data across devices
3. **Advanced Analytics**: Track more detailed metrics like flexibility scores, pose accuracy
4. **Social Features**: Share achievements with friends
5. **Personalized Recommendations**: Use session data to improve yoga recommendations
6. **Progress Charts**: Visual representation of user progress over time

## Testing

To test the profile functionality:

1. **Fresh Install**: Create a new user account and complete the preference journey
2. **Session Tracking**: Use the pose detection feature to accumulate session time
3. **Achievements**: Complete various milestones to unlock achievements
4. **Edit Profile**: Test the edit profile flow and verify data updates
5. **Streak Calculation**: Open the app on consecutive days to build streaks

The profile tab will now display real, dynamic data that reflects the user's actual yoga journey and progress.
