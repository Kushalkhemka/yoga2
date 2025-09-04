package com.yogakotlinpipeline.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import kotlin.math.sqrt

class PoseCalibrationFragment : Fragment() {
    
    private var _binding: FragmentPoseCalibrationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    private lateinit var poseDetector: PoseDetector
    private var poseName: String = ""
    private var poseThresholds: Map<String, PoseThreshold> = emptyMap()
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    
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
        
        // Get pose name from arguments and normalize to match CSV entries
        poseName = arguments?.getString("pose_name") ?: "Dandasana"
        poseName = normalizePoseName(poseName)
        
        // Update the pose title display
        binding.tvPoseTitle.text = "Calibrating: $poseName"
        
        // Initialize pose detector
        initializePoseDetector()
        
        // Load pose thresholds
        loadPoseThresholds()
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        
        // Set up click listeners
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnCameraSwitch.setOnClickListener {
            switchCamera()
        }
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun initializePoseDetector() {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        
        poseDetector = PoseDetection.getClient(options)
    }
    
    private fun loadPoseThresholds() {
        try {
            val inputStream = requireContext().assets.open("pose_thresholds.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val thresholds = mutableMapOf<String, PoseThreshold>()
            var isFirstLine = true
            
            reader.forEachLine { line ->
                if (isFirstLine) {
                    isFirstLine = false
                    return@forEachLine
                }
                
                val parts = line.split(",")
                // CSV header: pose,joint,mean,std,learned_multiplier,min_angle,max_angle
                // We must read min_angle at index 5 and max_angle at index 6
                if (parts.size >= 7 && parts[0] == poseName) {
                    val joint = parts[1]
                    val minAngle = parts[5].toFloatOrNull() ?: 0f
                    val maxAngle = parts[6].toFloatOrNull() ?: 180f
                    
                    thresholds[joint] = PoseThreshold(minAngle, maxAngle)
                }
            }
            
            poseThresholds = thresholds
            reader.close()
            inputStream.close()
            
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading pose thresholds: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizePoseName(input: String): String {
        return when (input.trim()) {
            "Naukasana", "Naukasan", "Boat", "Boat Pose", "Boat pose" -> "Boat pose"
            // Add more aliases here if needed in future
            else -> input.trim()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Toast.makeText(context, "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        val preview = Preview.Builder().build()
        
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, PoseAnalyzer())
            }
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        try {
            cameraProvider.unbindAll()
            
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            
            // Update overlay with view size when available
            binding.viewFinder.post {
                binding.poseOverlayView.updateDimensions(
                    binding.viewFinder.width,
                    binding.viewFinder.height
                )
            }
            
        } catch (exc: Exception) {
            Toast.makeText(context, "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
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
                        processPoseLandmarks(pose, image)
                    }
                    .addOnFailureListener { e ->
                        // Handle any errors
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            } else {
                image.close()
            }
        }
    }
    
    private fun processPoseLandmarks(pose: com.google.mlkit.vision.pose.Pose, imageProxy: ImageProxy) {
        val landmarks = pose.allPoseLandmarks
        
        if (landmarks.isNotEmpty()) {
            if (poseThresholds.isEmpty()) {
                // Quick diagnostic if thresholds didn't load for this pose
                android.util.Log.w("PoseCalibration", "No thresholds loaded for pose '$poseName'")
            }
            // Provide camera/image info to overlay so coordinates map like ML Kit Examples
            val bufferWidth = imageProxy.width
            val bufferHeight = imageProxy.height
            val rotationDeg = imageProxy.imageInfo.rotationDegrees
            val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
            binding.poseOverlayView.setImageSourceInfo(bufferWidth, bufferHeight, rotationDeg, isFront)
            binding.poseOverlayView.updatePoseLandmarks(
                landmarks,
                bufferWidth,
                bufferHeight,
                binding.viewFinder.width.toFloat(),
                binding.viewFinder.height.toFloat()
            )
            // Calculate angles and provide feedback
            val feedback = calculatePoseFeedback(landmarks)
            
            // Update UI with feedback
            updateFeedbackUI(feedback)
            val convertedMap = mutableMapOf<String, com.yogakotlinpipeline.app.JointFeedback>()
            feedback.jointFeedback.forEach { (joint, jf) ->
                convertedMap[joint] = com.yogakotlinpipeline.app.JointFeedback(
                    jf.angle,
                    jf.isCorrect,
                    com.yogakotlinpipeline.app.PoseThreshold(jf.threshold.minAngle, jf.threshold.maxAngle)
                )
            }
            val pubFeedback = com.yogakotlinpipeline.app.PoseFeedback(convertedMap)
            val pubThresholds = poseThresholds.mapValues { com.yogakotlinpipeline.app.PoseThreshold(it.value.minAngle, it.value.maxAngle) }
            binding.poseOverlayView.updatePoseWithFeedback(landmarks, pubFeedback, pubThresholds)
        }
    }
    
    private fun calculatePoseFeedback(landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>): PoseFeedback {
        val feedback = PoseFeedback()
        
        // Calculate angles for each joint
        poseThresholds.forEach { (jointName, threshold) ->
            val angle = calculateJointAngle(jointName, landmarks)
            if (angle != null) {
                val isCorrect = angle in threshold.minAngle..threshold.maxAngle
                feedback.jointFeedback[jointName] = JointFeedback(angle, isCorrect, threshold)
            }
        }
        
        // Special handling for specific poses
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
        // Special logic for Warrior II pose
        val leftKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }
        val rightKnee = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }
        val leftAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }
        val rightAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE }
        
        if (leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null) {
            val leftKneeAngle = calculateAngle(
                floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }!!.position.y),
                floatArrayOf(leftKnee.position.x, leftKnee.position.y),
                floatArrayOf(leftAnkle.position.x, leftAnkle.position.y)
            )
            
            val rightKneeAngle = calculateAngle(
                floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }!!.position.y),
                floatArrayOf(rightKnee.position.x, rightKnee.position.y),
                floatArrayOf(rightAnkle.position.x, rightAnkle.position.y)
            )
            
            // Determine which knee should be bent
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
    
    private fun handleVrksasanaFeedback(landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>, feedback: PoseFeedback) {
        // Special logic for Vrksasana (Tree pose)
        val leftAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }
        val rightAnkle = landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE }
        
        if (leftAnkle != null && rightAnkle != null) {
            // Determine which leg is raised based on ankle height
            if (leftAnkle.position.y < rightAnkle.position.y) {
                // Left leg is raised
                val leftKneeAngle = calculateAngle(
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }!!.position.y),
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }!!.position.y),
                    floatArrayOf(leftAnkle.position.x, leftAnkle.position.y)
                )
                
                val rightKneeAngle = calculateAngle(
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }!!.position.y),
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }!!.position.y),
                    floatArrayOf(rightAnkle.position.x, rightAnkle.position.y)
                )
                
                feedback.jointFeedback["left_knee_angle"] = JointFeedback(leftKneeAngle, leftKneeAngle in 30f..100f, PoseThreshold(30f, 100f))
                feedback.jointFeedback["right_knee_angle"] = JointFeedback(rightKneeAngle, rightKneeAngle in 165f..180f, PoseThreshold(165f, 180f))
            } else {
                // Right leg is raised
                val leftKneeAngle = calculateAngle(
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP }!!.position.y),
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE }!!.position.y),
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE }!!.position.y)
                )
                
                val rightKneeAngle = calculateAngle(
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP }!!.position.y),
                    floatArrayOf(landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }!!.position.x, landmarks.find { it.landmarkType == com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE }!!.position.y),
                    floatArrayOf(rightAnkle.position.x, rightAnkle.position.y)
                )
                
                feedback.jointFeedback["left_knee_angle"] = JointFeedback(leftKneeAngle, leftKneeAngle in 165f..180f, PoseThreshold(165f, 180f))
                feedback.jointFeedback["right_knee_angle"] = JointFeedback(rightKneeAngle, rightKneeAngle in 30f..100f, PoseThreshold(30f, 100f))
            }
        }
    }
    
    private fun updateFeedbackUI(feedback: PoseFeedback) {
        // Update UI on main thread
        requireActivity().runOnUiThread {
            // Update feedback text
            val feedbackText = buildString {
                appendLine("Pose: $poseName")
                appendLine()
                feedback.jointFeedback.forEach { (jointName, jointFeedback) ->
                    val status = if (jointFeedback.isCorrect) "✅" else "❌"
                    appendLine("$status $jointName: ${jointFeedback.angle.toInt()}°")
                    if (!jointFeedback.isCorrect) {
                        appendLine("   Target: ${jointFeedback.threshold.minAngle.toInt()}° - ${jointFeedback.threshold.maxAngle.toInt()}°")
                    }
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
                    if (percentage >= 80) android.R.color.holo_green_dark
                    else if (percentage >= 60) android.R.color.holo_orange_dark
                    else android.R.color.holo_red_dark
                )
            )
        }
    }
    
    data class PoseThreshold(val minAngle: Float, val maxAngle: Float)
    data class JointFeedback(val angle: Float, val isCorrect: Boolean, val threshold: PoseThreshold)
    data class PoseFeedback(val jointFeedback: MutableMap<String, JointFeedback> = mutableMapOf())
}
