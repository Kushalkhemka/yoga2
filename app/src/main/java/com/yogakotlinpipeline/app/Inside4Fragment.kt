package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside4Binding

class Inside4Fragment : Fragment() {

    private var _binding: FragmentInside4Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside1Fragment)
        }

        binding.btnFlows.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside3Fragment)
        }

        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside2Fragment)
        }

        binding.btnProgress.setOnClickListener {
            // Already on progress screen, do nothing
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_profileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

