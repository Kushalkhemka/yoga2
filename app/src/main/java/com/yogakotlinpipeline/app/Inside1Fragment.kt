package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside1Binding

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

