package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentPreference2Binding

class Preference2Fragment : Fragment() {
    
    private var _binding: FragmentPreference2Binding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreference2Binding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        setupRadioButtons()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnContinue.setOnClickListener {
            // Navigate to next preference screen
            findNavController().navigate(R.id.action_preference2Fragment_to_preference3Fragment)
        }
    }
    
    private fun setupRadioButtons() {
        // Set up radio button listeners for visual feedback
        val radioButtons = listOf(
            binding.rbWeightLoss,
            binding.rbFlexibility,
            binding.rbCoreStrength,
            binding.rbStressRelief,
            binding.rbBetterPosture,
            binding.rbDigestion,
            binding.rbEndurance,
            binding.rbRelaxation
        )
        
        radioButtons.forEach { radioButton ->
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck all other radio buttons
                    radioButtons.forEach { rb ->
                        if (rb != radioButton) {
                            rb.isChecked = false
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

