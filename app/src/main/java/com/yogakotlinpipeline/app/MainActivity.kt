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
        
        // Navigation is handled by the NavHostFragment in the layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Check if user has completed onboarding and has cached preferences
        if (shouldSkipOnboarding()) {
            // Navigate directly to home screen
            navController.navigate(R.id.inside1Fragment)
        }
    }
    
    private fun shouldSkipOnboarding(): Boolean {
        // Check if user is logged in and has completed profile
        return loginCache.isLoggedIn() && 
               loginCache.isUserProfileComplete() &&
               loginCache.hasRecentRecommendations()
    }
}

