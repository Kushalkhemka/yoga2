package com.yogakotlinpipeline.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.yogakotlinpipeline.app.databinding.ActivityMainBinding
import com.yogakotlinpipeline.app.utils.LoginCache

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var loginCache: LoginCache
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loginCache = LoginCache.getInstance(this)
        
        // Delay navigation to ensure NavHostFragment is ready
        binding.root.post {
            checkAndNavigate()
        }
    }
    
    private fun checkAndNavigate() {
        // Navigation is handled by the NavHostFragment in the layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Check if user has completed onboarding and has cached preferences
        if (shouldSkipOnboarding()) {
            android.util.Log.d("MainActivity", "Skipping onboarding, navigating to home")
            // Navigate directly to home screen and clear back stack
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.onboarding1Fragment, true)
                .build()
            navController.navigate(R.id.inside1Fragment, null, navOptions)
        } else {
            android.util.Log.d("MainActivity", "User needs to complete onboarding")
        }
    }
    
    private fun shouldSkipOnboarding(): Boolean {
        // Check if user is logged in and has completed profile
        val isLoggedIn = loginCache.isLoggedIn()
        val isProfileComplete = loginCache.isUserProfileComplete()
        val hasRecommendations = loginCache.hasRecentRecommendations()
        
        android.util.Log.d("MainActivity", "Login check: $isLoggedIn")
        android.util.Log.d("MainActivity", "Profile complete: $isProfileComplete")
        android.util.Log.d("MainActivity", "Has recommendations: $hasRecommendations")
        
        // Skip onboarding if user is logged in and has complete profile
        // Recommendations are optional and can be generated later
        return isLoggedIn && isProfileComplete
    }
}

