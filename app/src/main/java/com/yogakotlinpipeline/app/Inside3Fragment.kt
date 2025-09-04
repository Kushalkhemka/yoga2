package com.yogakotlinpipeline.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside3Binding

class Inside3Fragment : Fragment() {

    private var _binding: FragmentInside3Binding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupClickListeners() {
        // Menu Button
        binding.btnMenu.setOnClickListener {
            android.widget.Toast.makeText(context, "Menu", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Play Buttons
        binding.btnPlayMountain.setOnClickListener {
            navigateToPoseCalibration("Mountain pose", "Mountain Pose", "A foundational standing pose that improves posture and balance", "Beginner")
        }

        binding.btnPlayWarrior.setOnClickListener {
            navigateToPoseCalibration("Warrior ii pose", "Warrior II", "A powerful standing pose that builds strength and stamina", "Intermediate")
        }

        binding.btnPlayCrow.setOnClickListener {
            navigateToPoseCalibration("Crow pose", "Crow Pose", "An arm balance that builds strength and focus", "Advanced")
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside1Fragment)
        }

        binding.btnExplore.setOnClickListener {
            // Navigate to explore screen (inside2)
            findNavController().navigate(R.id.action_inside3Fragment_to_inside2Fragment)
        }

        binding.btnAi.setOnClickListener {
            // Already on AI screen, do nothing
        }

        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside4Fragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_profileFragment)
        }
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
        _binding = null
    }
}

