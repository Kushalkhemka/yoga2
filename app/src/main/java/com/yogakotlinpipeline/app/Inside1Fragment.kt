package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.util.Log
import android.widget.TextView
import com.yogakotlinpipeline.app.utils.NetworkService
import com.yogakotlinpipeline.app.utils.UserProfile
import com.yogakotlinpipeline.app.utils.RecommendationCacheStore
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside1Binding
import com.yogakotlinpipeline.app.utils.LoginCache
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Inside1Fragment : Fragment() {

    private var _binding: FragmentInside1Binding? = null
    private val binding get() = _binding!!

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
        setupClickListeners()
        populateHomeRecommendations()
    }

    private fun setupClickListeners() {
        // Start Yoga Session Button
        binding.btnStartSession.setOnClickListener {
            // Navigate to explore section fragment (Inside2Fragment)
            findNavController().navigate(R.id.action_inside1Fragment_to_inside2Fragment)
        }

        // Notification Button
        binding.btnNotifications.setOnClickListener {
            android.widget.Toast.makeText(context, "Notifications", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            // Already on home, do nothing
        }

        binding.btnExplore.setOnClickListener {
            // Navigate to original Explore (inside2) with images and Sanskrit names
            findNavController().navigate(R.id.action_inside1Fragment_to_inside2Fragment)
        }

        binding.btnAi.setOnClickListener {
            // Navigate to AI section (inside3) for recommendations
            findNavController().navigate(R.id.action_inside1Fragment_to_inside3Fragment)
        }

        binding.btnProgress.setOnClickListener {
            // Navigate to progress screen (inside4)
            findNavController().navigate(R.id.action_inside1Fragment_to_inside4Fragment)
        }

        binding.btnProfile.setOnClickListener {
            // Navigate to profile or show profile options
            android.widget.Toast.makeText(context, "Profile", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun populateHomeRecommendations() {
        try {
            val appContext = requireContext().applicationContext
            // Prefer saved user profile; fallback to sample if incomplete
            val loginCache = LoginCache.getInstance(appContext)
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

            val networkService = NetworkService.getInstance()

            // Launch background load so home can hydrate from cache or API
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 1) In-memory cache
                    val inMemory = networkService.getCachedRecommendations(userProfile)
                    val cachedOrPersisted = inMemory ?: RecommendationCacheStore.loadIfFresh(appContext, userProfile)

                    if (!cachedOrPersisted.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded) {
                                setHomeRecommendationTitles(cachedOrPersisted.map { it.name })
                            }
                        }
                    } else {
                        // 2) Hit API which will also persist cache via NetworkService
                        val fresh = networkService.getRecommendations(userProfile, context = appContext)
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded && fresh.isNotEmpty()) {
                                setHomeRecommendationTitles(fresh.map { it.name })
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("Inside1Fragment", "Background load failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.w("Inside1Fragment", "Failed to populate home recommendations: ${e.message}", e)
        }
    }

    private fun setHomeRecommendationTitles(titles: List<String>) {
        val tv1: TextView = binding.tvRecommendation1Title
        val tv2: TextView = binding.tvRecommendation2Title
        val tv3: TextView = binding.tvRecommendation3Title
        val tv4: TextView = binding.tvRecommendation4Title

        if (titles.isNotEmpty()) tv1.text = titles.getOrNull(0) ?: tv1.text
        if (titles.size > 1) tv2.text = titles.getOrNull(1) ?: tv2.text
        if (titles.size > 2) tv3.text = titles.getOrNull(2) ?: tv3.text
        if (titles.size > 3) tv4.text = titles.getOrNull(3) ?: tv4.text
    }
}

