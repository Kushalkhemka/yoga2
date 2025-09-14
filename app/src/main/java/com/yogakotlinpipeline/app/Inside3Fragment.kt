package com.yogakotlinpipeline.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside3Binding
import com.yogakotlinpipeline.app.utils.NetworkService
import com.yogakotlinpipeline.app.utils.UserProfile
import com.yogakotlinpipeline.app.utils.RecommendationCacheStore
import com.yogakotlinpipeline.app.utils.LoginCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Inside3Fragment : Fragment() {

    private var _binding: FragmentInside3Binding? = null
    private val binding get() = _binding!!
    
    private var recommendationJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadRecommendations()
    }

    private fun setupClickListeners() {
        // Menu Button
        binding.btnMenu.setOnClickListener {
            Toast.makeText(context, "Menu", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside1Fragment)
        }
        
        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside2Fragment)
        }
        
        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside4Fragment)
        }
        
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_profileFragment)
        }
    }

    private fun loadRecommendations() {
        // Cancel any existing job
        recommendationJob?.cancel()
        
        binding.progressLoading.visibility = View.VISIBLE
        
        recommendationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Prefer saved user profile; fallback to sample if incomplete
                val loginCache = LoginCache.getInstance(requireContext().applicationContext)
                val savedProfile = loginCache.getUserProfile()
                val userProfile = if (savedProfile.age > 0 && savedProfile.height > 0 && savedProfile.weight > 0) {
                    savedProfile
                } else {
                    UserProfile(
                        age = 25,
                        height = 170,
                        weight = 65,
                        level = "beginner",
                        pregnant = false,
                        problemAreas = listOf("back pain", "stress"),
                        goals = listOf("flexibility", "stress relief"),
                        mentalIssues = listOf("stress", "anxiety")
                    )
                }
                
                Log.d("Inside3Fragment", "UserProfile created: age=${userProfile.age}, height=${userProfile.height}, weight=${userProfile.weight}")
                Log.d("Inside3Fragment", "Goals: ${userProfile.goals}")
                Log.d("Inside3Fragment", "Physical issues: ${userProfile.getPhysicalIssues()}")
                Log.d("Inside3Fragment", "Mental issues: ${userProfile.getAllMentalIssues()}")
                Log.d("Inside3Fragment", "Level: ${userProfile.level}")

                val networkService = NetworkService.getInstance()
                // Check in-memory cache first for fastest hits
                networkService.getCachedRecommendations(userProfile)?.let { cached ->
                    if (cached.isNotEmpty()) {
                        Log.d("Inside3Fragment", "Loaded ${cached.size} recommendations from in-memory cache")
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded) {
                                binding.progressLoading.visibility = View.GONE
                                displayRecommendations(cached)
                            }
                        }
                        return@launch
                    }
                }
                // Try persistent cache to avoid API calls on restart
                RecommendationCacheStore.loadIfFresh(requireContext().applicationContext, userProfile)?.let { cached ->
                    if (cached.isNotEmpty()) {
                        Log.d("Inside3Fragment", "Loaded ${cached.size} recommendations from persistent cache")
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded) {
                                binding.progressLoading.visibility = View.GONE
                                displayRecommendations(cached)
                            }
                        }
                        return@launch
                    }
                }
                
                Log.d("Inside3Fragment", "Starting API call for recommendations")
                val recommendations = networkService.getRecommendations(userProfile, context = requireContext().applicationContext)
                Log.d("Inside3Fragment", "Received ${recommendations.size} recommendations")
                
                withContext(Dispatchers.Main) {
                    if (_binding != null && isAdded) {
                        binding.progressLoading.visibility = View.GONE
                        displayRecommendations(recommendations)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("Inside3Fragment", "Error loading recommendations: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null && isAdded) {
                        binding.progressLoading.visibility = View.GONE
                        Toast.makeText(context, "Failed to load recommendations", Toast.LENGTH_SHORT).show()
                        // Show fallback recommendations
                        displayFallbackRecommendations()
                    }
                }
            }
        }
    }

    private fun displayRecommendations(recommendations: List<com.yogakotlinpipeline.app.utils.YogaRecommendation>) {
        if (_binding == null || !isAdded) return
        
        val container = binding.recommendationsContainer
        container.removeAllViews()
        
        if (recommendations.isEmpty()) {
            displayFallbackRecommendations()
            return
        }
        
        recommendations.take(5).forEach { recommendation ->
            val cardView = createRecommendationCard(recommendation)
            container.addView(cardView)
        }
    }

    private fun displayFallbackRecommendations() {
        if (_binding == null || !isAdded) return
        
        val container = binding.recommendationsContainer
        container.removeAllViews()
        
        val fallbackRecommendations = listOf(
            Triple("Mountain Pose", "Grounding, improves posture", "BEGINNER"),
            Triple("Warrior II", "Builds strength and stamina", "INTERMEDIATE"),
            Triple("Crow Pose", "Builds arm strength and focus", "ADVANCED")
        )
        
        fallbackRecommendations.forEach { (name, description, level) ->
            val cardView = createFallbackCard(name, description, level)
            container.addView(cardView)
        }
    }

    private fun createRecommendationCard(recommendation: com.yogakotlinpipeline.app.utils.YogaRecommendation): View {
        val cardLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 48 // 16dp spacing
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(64, 64, 64, 64) // 16dp padding
            setBackgroundResource(R.drawable.asana_card_background)
        }
        
        // Image
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(384, 384).apply { // 96dp
                marginEnd = 64 // 16dp margin
            }
            setImageResource(R.drawable.recommendation_placeholder)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.rounded_image_background)
        }
        cardLayout.addView(imageView)
        
        // Text content
        val textLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        
        // Level badge
        val levelText = TextView(requireContext()).apply {
            text = recommendation.level.uppercase()
            setTextColor(resources.getColor(R.color.primary_color, null))
            textSize = 12f
            try {
                typeface = resources.getFont(R.font.inter_semibold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_semibold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(levelText)
        
        // Name
        val nameText = TextView(requireContext()).apply {
            text = recommendation.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 18f
            try {
                typeface = resources.getFont(R.font.inter_bold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_bold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(nameText)
        
        // Description
        val descText = TextView(requireContext()).apply {
            text = recommendation.benefits
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            try {
                typeface = resources.getFont(R.font.inter_regular)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_regular font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT
            }
        }
        textLayout.addView(descText)
        
        cardLayout.addView(textLayout)
        
        // Play button
        val playButton = ImageButton(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_play_circle)
            // Use attribute correctly via setBackgroundResource requires a resource id.
            // Fall back to transparent background if attribute id is not a valid resource.
            try {
                setBackgroundResource(android.R.drawable.list_selector_background)
            } catch (e: Exception) {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            setColorFilter(resources.getColor(R.color.primary_color, null))
            scaleX = 1.5f
            scaleY = 1.5f
            setOnClickListener {
                navigateToPoseCalibration(
                    recommendation.name.lowercase(),
                    recommendation.name,
                    recommendation.benefits,
                    recommendation.level
                )
            }
        }
        cardLayout.addView(playButton)
        
        return cardLayout
    }

    private fun createFallbackCard(name: String, description: String, level: String): View {
        val cardLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 48 // 16dp spacing
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(64, 64, 64, 64) // 16dp padding
            setBackgroundResource(R.drawable.asana_card_background)
        }
        
        // Image
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(384, 384).apply { // 96dp
                marginEnd = 64 // 16dp margin
            }
            setImageResource(R.drawable.recommendation_placeholder)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.rounded_image_background)
        }
        cardLayout.addView(imageView)
        
        // Text content
        val textLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        
        // Level badge
        val levelText = TextView(requireContext()).apply {
            text = level
            setTextColor(resources.getColor(R.color.primary_color, null))
            textSize = 12f
            try {
                typeface = resources.getFont(R.font.inter_semibold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_semibold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(levelText)
        
        // Name
        val nameText = TextView(requireContext()).apply {
            text = name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 18f
            try {
                typeface = resources.getFont(R.font.inter_bold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_bold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(nameText)
        
        // Description
        val descText = TextView(requireContext()).apply {
            text = description
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            try {
                typeface = resources.getFont(R.font.inter_regular)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_regular font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT
            }
        }
        textLayout.addView(descText)
        
        cardLayout.addView(textLayout)
        
        // Play button
        val playButton = ImageButton(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_play_circle)
            try {
                setBackgroundResource(android.R.drawable.list_selector_background)
            } catch (e: Exception) {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            setColorFilter(resources.getColor(R.color.primary_color, null))
            scaleX = 1.5f
            scaleY = 1.5f
            setOnClickListener {
                navigateToPoseCalibration(
                    name.lowercase(),
                    name,
                    description,
                    level.lowercase()
                )
            }
        }
        cardLayout.addView(playButton)
        
        return cardLayout
    }
    
    private fun navigateToPoseCalibration(poseName: String, displayName: String, description: String, difficulty: String) {
        Log.d("Inside3Fragment", "=== Navigating to pose calibration ===")
        Log.d("Inside3Fragment", "Pose: $poseName, Display: $displayName")
        
        val bundle = Bundle().apply {
            putString("pose_name", poseName)
            putString("pose_display_name", displayName)
            putString("pose_description", description)
            putString("pose_difficulty", difficulty)
        }
        
        try {
            findNavController().navigate(R.id.poseCalibrationFragment, bundle)
            Log.d("Inside3Fragment", "Navigation successful!")
        } catch (e: Exception) {
            Log.e("Inside3Fragment", "Navigation failed: ${e.message}", e)
            android.widget.Toast.makeText(context, "Navigation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recommendationJob?.cancel()
        recommendationJob = null
        _binding = null
    }
}

