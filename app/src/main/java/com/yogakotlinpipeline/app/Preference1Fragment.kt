package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentPreference1Binding

class Preference1Fragment : Fragment() {
    
    private var _binding: FragmentPreference1Binding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreference1Binding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        setupCheckBoxes()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnContinue.setOnClickListener {
            // Navigate to next preference screen
            findNavController().navigate(R.id.action_preference1Fragment_to_preference2Fragment)
        }
    }
    
    private fun setupCheckBoxes() {
        // Set up checkbox listeners for visual feedback
        val checkBoxes = listOf(
            binding.cbBackPain,
            binding.cbKneePain,
            binding.cbShoulderPain,
            binding.cbNeckPain,
            binding.cbJointStiffness,
            binding.cbStress,
            binding.cbLowFlexibility,
            binding.cbDigestiveIssues,
            binding.cbBalanceIssues
        )
        
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                // Update visual state
                checkBox.isChecked = isChecked
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

