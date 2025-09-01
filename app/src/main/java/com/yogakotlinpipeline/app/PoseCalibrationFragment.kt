package com.yogakotlinpipeline.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.yogakotlinpipeline.app.databinding.FragmentPoseCalibrationBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

class PoseCalibrationFragment : Fragment() {
    
    private var _binding: FragmentPoseCalibrationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    
    private lateinit var poseDetector: PoseDetector
    private var poseName: String = ""
    private var poseDisplayName: String = ""
    private var poseDescription: String = ""
    private var poseDifficulty: String = ""
    private var poseThresholds: Map<String, PoseThreshold> = emptyMap()
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoseCalibrationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("PoseCalibration", "=== onViewCreated started ===")
        
        // Show immediate feedback that fragment is loaded
        Toast.makeText(context, "PoseCalibrationFragment loaded for: $poseDisplayName", Toast.LENGTH_SHORT).show()
        
        // Get pose information from arguments
        poseName = arguments?.getString("pose_name") ?: "Dandasana"
        poseDisplayName = arguments?.getString("pose_display_name") ?: "Staff Pose"
        poseDescription = arguments?.getString("pose_description") ?: "A foundational seated pose"
        poseDifficulty = arguments?.getString("pose_difficulty") ?: "Beginner"
        
        Log.d("PoseCalibration", "Pose info: name='$poseName', display='$poseDisplayName'")
        
        // Initialize pose detector
        initializePoseDetector()
        
        // Load pose thresholds
        loadPoseThresholds()
        
        // Set up click listeners
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Debug button for camera
        binding.root.findViewById<android.widget.Button>(R.id.btn_debug_camera)?.setOnClickListener {
            Log.d("PoseCalibration", "Debug button clicked, manually starting camera...")
            checkPermissionsAndStartCamera()
        }
        
        // Set up bottom navigation click listeners
        setupBottomNavigation()
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Update header with pose name
        updateHeaderWithPoseName()
        
        // Update pose information section
        updatePoseInfoSection()
        
        // Check permissions and start camera
        checkPermissionsAndStartCamera()
        
        Log.d("PoseCalibration", "=== onViewCreated completed ===")
    }
    
    private fun updateHeaderWithPoseName() {
        // Update the header text with the pose display name
        val headerText = "$poseDisplayName Calibration"
        
        // Update the header text in the layout
        binding.root.findViewById<android.widget.TextView>(R.id.tv_pose_title)?.text = headerText
    }
    
    private fun updatePoseInfoSection() {
        // Update the pose information section
        binding.root.findViewById<android.widget.TextView>(R.id.tv_pose_info_description)?.text = poseDescription
    }
    
    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_poseCalibrationFragment_to_inside1Fragment)
        }
        
        binding.btnFlows.setOnClickListener {
            findNavController().navigate(R.id.action_poseCalibrationFragment_to_inside3Fragment)
        }
        
        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_poseCalibrationFragment_to_inside4Fragment)
        }
        
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_poseCalibrationFragment_to_profileFragment)
        }
    }
    
    private fun checkPermissionsAndStartCamera() {
        Log.d("PoseCalibration", "Checking camera permissions...")
        
        if (allPermissionsGranted()) {
            Log.d("PoseCalibration", "Camera permissions granted, starting camera...")
            startCamera()
        } else {
            Log.d("PoseCalibration", "Camera permissions not granted, requesting...")
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun initializePoseDetector() {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        
        poseDetector = PoseDetection.getClient(options)
    }
    
    private fun loadPoseThresholds() {
        try {
            Log.d("PoseCalibration", "Loading thresholds for pose: '$poseName'")
            
            val inputStream = requireContext().assets.open("pose_thresholds.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val thresholds = mutableMapOf<String, PoseThreshold>()
            var isFirstLine = true
            var lineCount = 0
            var matchedCount = 0
            
            reader.forEachLine { line ->
                lineCount++
                if (isFirstLine) {
                    isFirstLine = false
                    Log.d("PoseCalibration", "CSV Header: $line")
                    return@forEachLine
                }
                
                val parts = line.split(",")
                val csvPoseName = parts[0].trim()
                
                Log.d("PoseCalibration", "Line $lineCount: CSV pose='$csvPoseName', Looking for='$poseName', Match: ${csvPoseName == poseName}")
                
                if (parts.size >= 6 && csvPoseName == poseName) {
                    val joint = parts[1].trim()
                    val minAngle = parts[4].toFloatOrNull() ?: 0f  // min_angle column (index 4)
                    val maxAngle = parts[5].toFloatOrNull() ?: 180f // max_angle column (index 5)
                    
                    // Ensure min is less than max
                    val actualMin = minOf(minAngle, maxAngle)
                    val actualMax = maxOf(minAngle, maxAngle)
                    
                    thresholds[joint] = PoseThreshold(actualMin, actualMax)
                    matchedCount++
                    
                    Log.d("PoseCalibration", "✓ Loaded threshold for $joint: $actualMin° - $actualMax°")
                }
            }
            
            poseThresholds = thresholds
            reader.close()
            inputStream.close()
            
            // Log summary
            Log.d("PoseCalibration", "=== CSV Loading Summary ===")
            Log.d("PoseCalibration", "Total lines processed: $lineCount")
            Log.d("PoseCalibration", "Pose name searched: '$poseName'")
            Log.d("PoseCalibration", "Thresholds loaded: $matchedCount")
            Log.d("PoseCalibration", "Final threshold map size: ${thresholds.size}")
            
            if (thresholds.isEmpty()) {
                Log.w("PoseCalibration", "⚠️ No thresholds found for pose: '$poseName'")
                // List all available poses in CSV for debugging
                listAvailablePosesInCSV()
            } else {
                Log.d("PoseCalibration", "✓ Successfully loaded thresholds:")
                thresholds.forEach { (joint, threshold) ->
                    Log.d("PoseCalibration", "  $joint: ${threshold.minAngle}° - ${threshold.maxAngle}°")
                }
            }
            
        } catch (e: Exception) {
            Log.e("PoseCalibration", "❌ Error loading pose thresholds: ${e.message}", e)
            Toast.makeText(context, "Error loading pose thresholds: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun listAvailablePosesInCSV() {
        try {
            val inputStream = requireContext().assets.open("pose_thresholds.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val availablePoses = mutableSetOf<String>()
            var isFirstLine = true
            
            reader.forEachLine { line ->
                if (isFirstLine) {
                    isFirstLine = false
                    return@forEachLine
                }
                
                val parts = line.split(",")
                if (parts.isNotEmpty()) {
                    availablePoses.add(parts[0].trim())
                }
            }
            
            reader.close()
            inputStream.close()
            
            Log.w("PoseCalibration", "Available poses in CSV:")
            availablePoses.sorted().forEach { pose ->
                Log.w("PoseCalibration", "  - '$pose'")
            }
            
        } catch (e: Exception) {
            Log.e("PoseCalibration", "Error listing available poses: ${e.message}", e)
        }
    }
    
    private fun startCamera() {
        Log.d("PoseCalibration", "=== startCamera called ===")
        
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            Log.d("PoseCalibration", "Camera provider future obtained")
            
            cameraProviderFuture.addListener({
                try {
                    Log.d("PoseCalibration", "Camera provider future listener executing...")
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    Log.d("PoseCalibration", "Camera provider obtained: $cameraProvider")
                    
                    val preview = Preview.Builder().build()
                    Log.d("PoseCalibration", "Preview built")
                    
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    Log.d("PoseCalibration", "ImageCapture built")
                    
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, PoseAnalyzer())
                        }
                    Log.d("PoseCalibration", "ImageAnalyzer built and configured")
                    
                    try {
                        Log.d("PoseCalibration", "Unbinding all previous camera uses...")
                        cameraProvider.unbindAll()
                        
                        Log.d("PoseCalibration", "Binding camera to lifecycle...")
                        camera = cameraProvider.bindToLifecycle(
                            this,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageCapture,
                            imageAnalyzer
                        )
                        Log.d("PoseCalibration", "Camera bound successfully: $camera")
                        
                        Log.d("PoseCalibration", "Setting surface provider...")
                        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                        Log.d("PoseCalibration", "Surface provider set successfully")
                        
                        Log.d("PoseCalibration", "=== Camera setup completed successfully ===")
                        
                        // Show success message
                        requireActivity().runOnUiThread {
                            Toast.makeText(context, "Camera started successfully", Toast.LENGTH_SHORT).show()
                        }
                        
                    } catch (exc: Exception) {
                        Log.e("PoseCalibration", "Camera binding failed: ${exc.message}", exc)
                        requireActivity().runOnUiThread {
                            Toast.makeText(context, "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (exc: Exception) {
                    Log.e("PoseCalibration", "Error in camera provider future listener: ${exc.message}", exc)
                    Toast.makeText(context, "Camera setup failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(requireContext()))
            
            Log.d("PoseCalibration", "Camera provider future listener added")
            
        } catch (exc: Exception) {
            Log.e("PoseCalibration", "Error in startCamera: ${exc.message}", exc)
            Toast.makeText(context, "Camera initialization failed: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun allPermissionsGranted(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val granted = cameraPermission == PackageManager.PERMISSION_GRANTED
        Log.d("PoseCalibration", "Camera permission check: $cameraPermission (granted: $granted)")
        return granted
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("PoseCalibration", "onRequestPermissionsResult: requestCode=$requestCode")
        Log.d("PoseCalibration", "Permissions: ${permissions.joinToString()}")
        Log.d("PoseCalibration", "Grant results: ${grantResults.joinToString()}")
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d("PoseCalibration", "Permissions granted, starting camera...")
                startCamera()
            } else {
                Log.w("PoseCalibration", "Camera permission denied")
                Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
    
    private inner class PoseAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val mediaImage = image.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                
                poseDetector.process(inputImage)
                    .addOnSuccessListener { pose ->
                        // Process pose landmarks and provide feedback
                        processPoseLandmarks(pose)
                    }
                    .addOnFailureListener { e ->
                        Log.e("PoseCalibration", "Pose detection failed: ${e.message}", e)
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            } else {
                image.close()
            }
        }
    }
    
    private fun processPoseLandmarks(pose: com.google.mlkit.vision.pose.Pose) {
        val landmarks = pose.allPoseLandmarks
        
        if (landmarks.isNotEmpty()) {
            Log.d("PoseCalibration", "Processing ${landmarks.size} pose landmarks")
            
            // Calculate angles and provide feedback
            val feedback = calculatePoseFeedback(landmarks)
            
            // Update UI with feedback
            updateFeedbackUI(feedback)
        } else {
            Log.w("PoseCalibration", "No pose landmarks detected")
        }
    }
    
    private fun calculatePoseFeedback(landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>): PoseFeedback {
        val feedback = PoseFeedback()
        
        // Calculate angles for each joint based on thresholds
        poseThresholds.forEach { (jointName, threshold) ->
            val angle = calculateJointAngle(jointName, landmarks)
            if (angle != null) {
                val isCorrect = angle in threshold.minAngle..threshold.maxAngle
                feedback.jointFeedback[jointName] = JointFeedback(angle, isCorrect, threshold)
            }
        }
        
        // Special handling for specific poses like in the notebook
        when (poseName) {
            "Warrior ii pose" -> handleWarriorIIFeedback(landmarks, feedback)
            "Vrksasana" -> handleVrksasanaFeedback(landmarks, feedback)
        }
        
        return feedback
    }
    
    private fun calculateJointAngle(jointName: String, landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>): Float? {
        val (p1Index, p2Index, p3Index) = when (jointName) {
            "left_shoulder_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW
            )
            "left_elbow_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST
            )
            "right_shoulder_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW
            )
            "right_elbow_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST
            )
            "left_hip_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE
            )
            "left_knee_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE
            )
            "right_hip_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE
            )
            "right_knee_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE
            )
            else -> return null
        }
        
        val p1 = landmarks.find { it.landmarkType == p1Index }
        val p2 = landmarks.find { it.landmarkType == p2Index }
        val p3 = landmarks.find { it.landmarkType == p3Index }
        
        if (p1 != null && p2 != null && p3 != null) {
            // Convert to 2D points like in the original notebook
            val point1 = floatArrayOf(p1.position.x, p1.position.y)
            val point2 = floatArrayOf(p2.position.x, p2.position.y)
            val point3 = floatArrayOf(p3.position.x, p3.position.y)
            return calculateAngle(point1, point2, point3)
        }
        
        return null
    }
    
    private fun calculateAngle(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val radians = atan2(c[1] - b[1], c[0] - b[0]) - atan2(a[1] - b[1], a[0] - b[0])
        var angle = abs(radians * 180.0f / Math.PI.toFloat())
        
        if (angle > 180.0f) {
            angle = 360.0f - angle
        }
        
        return angle
    }
    
    private fun handleWarriorIIFeedback(landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>, feedback: PoseFeedback) {
        // Special logic for Warrior II pose - same as in the notebook
        val leftKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }
        val rightKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }
        val leftAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }
        val rightAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE }
        
        if (leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null) {
            val leftHip = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }
            val rightHip = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }
            
            if (leftHip != null && rightHip != null) {
                val leftKneeAngle = calculateAngle(
                    floatArrayOf(leftHip.position.x, leftHip.position.y),
                    floatArrayOf(leftKnee.position.x, leftKnee.position.y),
                    floatArrayOf(leftAnkle.position.x, leftAnkle.position.y)
                )
                
                val rightKneeAngle = calculateAngle(
                    floatArrayOf(rightHip.position.x, rightHip.position.y),
                    floatArrayOf(rightKnee.position.x, rightKnee.position.y),
                    floatArrayOf(rightAnkle.position.x, rightAnkle.position.y)
                )
                
                // Determine which knee should be bent (same logic as notebook)
                if (leftKneeAngle < rightKneeAngle) {
                    val leftCorrect = leftKneeAngle in 85f..135f
                    val rightCorrect = rightKneeAngle in 165f..180f
                    
                    feedback.jointFeedback["left_knee_angle"] = JointFeedback(leftKneeAngle, leftCorrect, PoseThreshold(85f, 135f))
                    feedback.jointFeedback["right_knee_angle"] = JointFeedback(rightKneeAngle, rightCorrect, PoseThreshold(165f, 180f))
                } else {
                    val leftCorrect = leftKneeAngle in 165f..180f
                    val rightCorrect = rightKneeAngle in 85f..135f
                    
                    feedback.jointFeedback["left_knee_angle"] = JointFeedback(leftKneeAngle, leftCorrect, PoseThreshold(165f, 180f))
                    feedback.jointFeedback["right_knee_angle"] = JointFeedback(rightKneeAngle, rightCorrect, PoseThreshold(85f, 135f))
                }
            }
        }
    }
    
    private fun handleVrksasanaFeedback(landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>, feedback: PoseFeedback) {
        // Special logic for Vrksasana (Tree pose) - same as in the notebook
        val leftAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }
        val rightAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE }
        
        if (leftAnkle != null && rightAnkle != null) {
            // Determine which leg is raised based on ankle height (same logic as notebook)
            if (leftAnkle.position.y < rightAnkle.position.y) {
                // Left leg is raised
                val leftHip = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }
                val leftKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }
                val rightHip = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }
                val rightKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }
                
                if (leftHip != null && leftKnee != null && rightHip != null && rightKnee != null) {
                    val leftKneeAngle = calculateAngle(
                        floatArrayOf(leftHip.position.x, leftHip.position.y),
                        floatArrayOf(leftKnee.position.x, leftKnee.position.y),
                        floatArrayOf(leftAnkle.position.x, leftAnkle.position.y)
                    )
                    
                    val rightKneeAngle = calculateAngle(
                        floatArrayOf(rightHip.position.x, rightHip.position.y),
                        floatArrayOf(rightKnee.position.x, rightKnee.position.y),
                        floatArrayOf(rightAnkle.position.x, rightAnkle.position.y)
                    )
                    
                    feedback.jointFeedback["left_knee_angle"] = JointFeedback(leftKneeAngle, leftKneeAngle in 30f..100f, PoseThreshold(30f, 100f))
                    feedback.jointFeedback["right_knee_angle"] = JointFeedback(rightKneeAngle, rightKneeAngle in 165f..180f, PoseThreshold(165f, 180f))
                }
            } else {
                // Right leg is raised
                val leftHip = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }
                val leftKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }
                val rightHip = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }
                val rightKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }
                
                if (leftHip != null && leftKnee != null && rightHip != null && rightKnee != null) {
                    val leftKneeAngle = calculateAngle(
                        floatArrayOf(leftHip.position.x, leftHip.position.y),
                        floatArrayOf(leftKnee.position.x, leftKnee.position.y),
                        floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }!!.position.y)
                    )
                    
                    val rightKneeAngle = calculateAngle(
                        floatArrayOf(rightHip.position.x, rightHip.position.y),
                        floatArrayOf(rightKnee.position.x, rightKnee.position.y),
                        floatArrayOf(rightAnkle.position.x, rightAnkle.position.y)
                    )
                    
                    feedback.jointFeedback["left_knee_angle"] = JointFeedback(leftKneeAngle, leftKneeAngle in 165f..180f, PoseThreshold(165f, 180f))
                    feedback.jointFeedback["right_knee_angle"] = JointFeedback(rightKneeAngle, rightKneeAngle in 30f..100f, PoseThreshold(30f, 100f))
                }
            }
        }
    }
    
    private fun updateFeedbackUI(feedback: PoseFeedback) {
        // Update UI on main thread
        requireActivity().runOnUiThread {
            // Update feedback text with better formatting
            val feedbackText = buildString {
                appendLine("🎯 Pose: $poseDisplayName")
                appendLine("📝 Sanskrit: $poseName")
                appendLine("⭐ Difficulty: $poseDifficulty")
                appendLine()
                appendLine("📊 Joint Analysis:")
                appendLine()
                
                feedback.jointFeedback.forEach { (jointName, jointFeedback) ->
                    val status = if (jointFeedback.isCorrect) "✅" else "❌"
                    val jointDisplayName = getJointDisplayName(jointName)
                    appendLine("$status $jointDisplayName: ${jointFeedback.angle.toInt()}°")
                    if (!jointFeedback.isCorrect) {
                        appendLine("   🎯 Target: ${jointFeedback.threshold.minAngle.toInt()}° - ${jointFeedback.threshold.maxAngle.toInt()}°")
                        appendLine("   💡 Adjust: ${getAdjustmentHint(jointFeedback.angle, jointFeedback.threshold)}")
                    }
                    appendLine()
                }
            }
            
            binding.tvFeedback.text = feedbackText
            
            // Update overall pose status
            val correctJoints = feedback.jointFeedback.values.count { it.isCorrect }
            val totalJoints = feedback.jointFeedback.size
            val percentage = if (totalJoints > 0) (correctJoints * 100 / totalJoints) else 0
            
            binding.tvPoseStatus.text = "Pose Accuracy: $percentage%"
            binding.tvPoseStatus.setTextColor(
                ContextCompat.getColor(requireContext(), 
                    when {
                        percentage >= 80 -> android.R.color.holo_green_dark
                        percentage >= 60 -> android.R.color.holo_orange_dark
                        else -> android.R.color.holo_red_dark
                    }
                )
            )
        }
    }
    
    private fun getJointDisplayName(jointName: String): String {
        return when (jointName) {
            "left_shoulder_angle" -> "Left Shoulder"
            "left_elbow_angle" -> "Left Elbow"
            "right_shoulder_angle" -> "Right Shoulder"
            "right_elbow_angle" -> "Right Elbow"
            "left_hip_angle" -> "Left Hip"
            "left_knee_angle" -> "Left Knee"
            "right_hip_angle" -> "Right Hip"
            "right_knee_angle" -> "Right Knee"
            else -> jointName.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun getAdjustmentHint(currentAngle: Float, threshold: PoseThreshold): String {
        return when {
            currentAngle < threshold.minAngle -> "Increase the angle"
            currentAngle > threshold.maxAngle -> "Decrease the angle"
            else -> "Angle is correct"
        }
    }
}

// Data classes for pose calibration
data class PoseThreshold(val minAngle: Float, val maxAngle: Float)
data class JointFeedback(val angle: Float, val isCorrect: Boolean, val threshold: PoseThreshold)
data class PoseFeedback(val jointFeedback: MutableMap<String, JointFeedback> = mutableMapOf())
