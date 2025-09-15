package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentProfileBinding
import com.yogakotlinpipeline.app.utils.LoginCache
import com.yogakotlinpipeline.app.utils.SessionTracker
import com.yogakotlinpipeline.app.utils.AchievementManager
import com.yogakotlinpipeline.app.utils.Achievement
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loginCache = LoginCache.getInstance(requireContext())
        
        // Track app opening for streak calculation
        SessionTracker.trackAppOpen(requireContext())
        
        setupClickListeners()
        loadProfileData()
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside1Fragment)
        }

        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside3Fragment)
        }

        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside2Fragment)
        }

        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside4Fragment)
        }

        // Profile Actions
        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.btnNotifications.setOnClickListener {
            Toast.makeText(context, "Notifications settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrivacy.setOnClickListener {
            Toast.makeText(context, "Privacy settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun loadProfileData() {
        // Load user data from cache
        val userEmail = loginCache.getUserEmail()
        val userName = loginCache.getUserName()
        val userProfile = loginCache.getUserProfile()
        
        // Update profile header with real data
        updateProfileHeader(userName, userProfile)
        
        // Update stats with real session data
        updateStats()
        
        // Update personal information
        updatePersonalInformation(userProfile)
        
        // Update achievements
        updateAchievements()
    }
    
    private fun updateProfileHeader(userName: String?, userProfile: com.yogakotlinpipeline.app.utils.UserProfile) {
        // Update name
        val displayName = userName ?: "Yoga Practitioner"
        binding.tvProfileName.text = displayName
        
        // Update level display
        val levelText = when (userProfile.level.lowercase()) {
            "beginner" -> "Yoga Beginner • Level: Beginner"
            "intermediate" -> "Yoga Enthusiast • Level: Intermediate"
            "advanced" -> "Yoga Expert • Level: Advanced"
            else -> "Yoga Practitioner • Level: Beginner"
        }
        binding.tvProfileLevel.text = levelText
    }
    
    private fun updateStats() {
        val context = requireContext()
        
        // Get real session data
        val currentStreak = SessionTracker.getCurrentStreak(context)
        val totalTimeMinutes = SessionTracker.getTotalSessionTimeMinutes(context)
        val totalWorkouts = SessionTracker.getTotalWorkouts(context)
        
        // Format time for display
        val timeDisplay = SessionTracker.formatTimeDuration(totalTimeMinutes)
        
        // Update stats TextViews
        binding.tvStreakCount.text = currentStreak.toString()
        binding.tvTotalTime.text = timeDisplay
        binding.tvWorkoutsCount.text = totalWorkouts.toString()
        
        // Store the values for reference
        profileStats = ProfileStats(
            streak = currentStreak,
            totalTime = timeDisplay,
            workouts = totalWorkouts
        )
    }
    
    private fun updatePersonalInformation(userProfile: com.yogakotlinpipeline.app.utils.UserProfile) {
        // Update personal information TextViews
        binding.tvAgeValue.text = if (userProfile.age > 0) "${userProfile.age} years" else "Not specified"
        binding.tvLevelValue.text = userProfile.level.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        binding.tvJoinDateValue.text = getJoinDate()
        
        // Store the values for reference
        personalInfo = PersonalInfo(
            age = if (userProfile.age > 0) "${userProfile.age} years" else "Not specified",
            level = userProfile.level.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            joinDate = getJoinDate(),
            height = if (userProfile.height > 0) "${userProfile.height} cm" else "Not specified",
            weight = if (userProfile.weight > 0) "${userProfile.weight} kg" else "Not specified",
            goals = userProfile.goals.joinToString(", ").takeIf { it.isNotEmpty() } ?: "Not specified",
            problemAreas = userProfile.problemAreas.joinToString(", ").takeIf { it.isNotEmpty() } ?: "None"
        )
    }
    
    private fun updateAchievements() {
        val context = requireContext()
        val isNewbie = AchievementManager.isNewbie(context)
        
        achievements = if (isNewbie) {
            AchievementManager.getNewbieAchievements(context)
        } else {
            AchievementManager.getRecentAchievements(context)
        }
    }
    
    private fun getJoinDate(): String {
        val loginTimestamp = loginCache.getLoginTimestamp()
        if (loginTimestamp > 0) {
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return dateFormat.format(Date(loginTimestamp))
        }
        return "Recently"
    }
    
    // Data classes to hold the updated information
    private data class ProfileStats(
        val streak: Int,
        val totalTime: String,
        val workouts: Int
    )
    
    private data class PersonalInfo(
        val age: String,
        val level: String,
        val joinDate: String,
        val height: String,
        val weight: String,
        val goals: String,
        val problemAreas: String
    )
    
    private var profileStats: ProfileStats? = null
    private var personalInfo: PersonalInfo? = null
    private var achievements: List<Achievement> = emptyList()

    private fun navigateToEditProfile() {
        // Navigate to preference fragments to allow editing
        // Pass a parameter to indicate we're coming from profile edit
        val bundle = Bundle().apply {
            putBoolean("from_profile", true)
        }
        findNavController().navigate(R.id.action_profileFragment_to_preference1Fragment, bundle)
    }

    private fun performLogout() {
        // Clear login cache
        loginCache.logout()
        
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back to login screen
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



