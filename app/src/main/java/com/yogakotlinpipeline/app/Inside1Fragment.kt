package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside1Binding
import com.yogakotlinpipeline.app.utils.LoginCache
import com.yogakotlinpipeline.app.utils.YogaRecommendationService
import kotlinx.coroutines.*

class Inside1Fragment : Fragment() {

    private var _binding: FragmentInside1Binding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache
    private lateinit var yogaRecommendationService: YogaRecommendationService
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loginCache = LoginCache.getInstance(requireContext())
        yogaRecommendationService = YogaRecommendationService(requireContext())
        
        setupClickListeners()
        loadRecommendedPoses()
    }

    private fun setupClickListeners() {
        // Start Yoga Session Button
        binding.btnStartSession.setOnClickListener {
            // Navigate to pose detection camera screen
            findNavController().navigate(R.id.action_inside1Fragment_to_poseDetectionFragment)
        }

        // Notification Button
        binding.btnNotifications.setOnClickListener {
            android.widget.Toast.makeText(context, "Notifications", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Recommendation Card Click Listeners
        binding.cardRecommendation1.setOnClickListener {
            val recommendations = loginCache.getRecommendations()
            if (recommendations.isNotEmpty()) {
                val asanaName = recommendations[0].name
                android.widget.Toast.makeText(context, "Starting: $asanaName", android.widget.Toast.LENGTH_SHORT).show()
                // Navigate to pose detection with the selected asana
                findNavController().navigate(R.id.action_inside1Fragment_to_poseDetectionFragment)
            } else {
                android.widget.Toast.makeText(context, "Starting: Morning Flow", android.widget.Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_inside1Fragment_to_poseDetectionFragment)
            }
        }

        binding.cardRecommendation2.setOnClickListener {
            val recommendations = loginCache.getRecommendations()
            if (recommendations.size > 1) {
                val asanaName = recommendations[1].name
                android.widget.Toast.makeText(context, "Starting: $asanaName", android.widget.Toast.LENGTH_SHORT).show()
                // Navigate to pose detection with the selected asana
                findNavController().navigate(R.id.action_inside1Fragment_to_poseDetectionFragment)
            } else {
                android.widget.Toast.makeText(context, "Starting: Relaxation", android.widget.Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_inside1Fragment_to_poseDetectionFragment)
            }
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            // Already on home, do nothing
        }

        binding.btnExplore.setOnClickListener {
            // Navigate to explore screen (inside2)
            findNavController().navigate(R.id.action_inside1Fragment_to_inside2Fragment)
        }

        binding.btnProgress.setOnClickListener {
            // Navigate to progress screen (inside4)
            findNavController().navigate(R.id.action_inside1Fragment_to_inside4Fragment)
        }

        binding.btnProfile.setOnClickListener {
            // Navigate to profile screen
            findNavController().navigate(R.id.action_inside1Fragment_to_profileFragment)
        }
    }
    
    private fun loadRecommendedPoses() {
        // Check if we have recent recommendations
        if (loginCache.hasRecentRecommendations()) {
            val recommendations = loginCache.getRecommendations()
            displayRecommendations(recommendations)
        } else {
            // Generate new recommendations if profile is complete
            val userProfile = loginCache.getUserProfile()
            if (loginCache.isUserProfileComplete()) {
                generateNewRecommendations(userProfile)
            } else {
                // Show placeholder or default recommendations
                showDefaultRecommendations()
            }
        }
    }
    
    private fun generateNewRecommendations(userProfile: com.yogakotlinpipeline.app.utils.UserProfile) {
        coroutineScope.launch {
            try {
                val recommendations = yogaRecommendationService.getTopRecommendations(userProfile)
                
                if (recommendations.isNotEmpty()) {
                    loginCache.saveRecommendations(recommendations)
                    displayRecommendations(recommendations)
                } else {
                    showDefaultRecommendations()
                }
            } catch (e: Exception) {
                android.util.Log.e("Inside1Fragment", "Error generating recommendations: ${e.message}", e)
                showDefaultRecommendations()
            }
        }
    }
    
    private fun displayRecommendations(recommendations: List<com.yogakotlinpipeline.app.utils.YogaRecommendation>) {
        if (recommendations.isNotEmpty()) {
            // Update the first recommendation
            val firstRecommendation = recommendations[0]
            binding.tvRecommendation1Title.text = firstRecommendation.name
            binding.tvRecommendation1Duration.text = "${firstRecommendation.level.replaceFirstChar { it.uppercase() }} Level"
            
            // Update the second recommendation if available
            if (recommendations.size > 1) {
                val secondRecommendation = recommendations[1]
                binding.tvRecommendation2Title.text = secondRecommendation.name
                binding.tvRecommendation2Duration.text = "${secondRecommendation.level.replaceFirstChar { it.uppercase() }} Level"
            }
            
            // Show the recommendations section
            binding.recommendationsSection.visibility = android.view.View.VISIBLE
        } else {
            showDefaultRecommendations()
        }
    }
    
    private fun showDefaultRecommendations() {
        // Show default recommendations
        binding.tvRecommendation1Title.text = "Morning Flow"
        binding.tvRecommendation1Duration.text = "15 min"
        binding.tvRecommendation2Title.text = "Relaxation"
        binding.tvRecommendation2Duration.text = "20 min"
        binding.recommendationsSection.visibility = android.view.View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
        _binding = null
    }
}

