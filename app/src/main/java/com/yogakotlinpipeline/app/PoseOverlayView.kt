package com.yogakotlinpipeline.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.PoseLandmark

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val poseLandmarks = mutableListOf<PoseLandmark>()
    private var imageWidth = 1
    private var imageHeight = 1
    private var screenWidth = 1f
    private var screenHeight = 1f
    
    // Paint settings for clear visibility
    private val landmarkPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val connectionPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f // Optimal thickness for visibility
        isAntiAlias = true
    }

    fun updatePoseLandmarks(landmarks: List<PoseLandmark>, width: Int, height: Int, canvasWidth: Float, canvasHeight: Float) {
        poseLandmarks.clear()
        poseLandmarks.addAll(landmarks)
        imageWidth = width
        imageHeight = height
        screenWidth = canvasWidth
        screenHeight = canvasHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (poseLandmarks.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return
        
        // Use the exact same scaling approach as ML Kit Examples
        val scaleX = screenWidth / imageWidth
        val scaleY = screenHeight / imageHeight
        
        // Draw connections first (behind landmarks)
        drawConnections(canvas, scaleX, scaleY)
        
        // Draw landmarks on top
        for (landmark in poseLandmarks) {
            try {
                val adjustedX = landmark.position.x * scaleX
                val adjustedY = landmark.position.y * scaleY
                
                // Check if coordinates are within screen bounds
                if (adjustedX >= 0 && adjustedX <= screenWidth && 
                    adjustedY >= 0 && adjustedY <= screenHeight) {
                    // Same radius as ML Kit Examples (8f)
                    canvas.drawCircle(adjustedX, adjustedY, 8f, landmarkPaint)
                }
            } catch (e: Exception) {
                // Skip this landmark if there's an error
                continue
            }
        }
    }
    
    private fun drawConnections(canvas: Canvas, scaleX: Float, scaleY: Float) {
        val connections = listOf(
            // Face connections (same as ML Kit Examples)
            PoseLandmark.LEFT_EYE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EYE to PoseLandmark.LEFT_EAR,
            PoseLandmark.RIGHT_EYE to PoseLandmark.RIGHT_EAR,
            PoseLandmark.NOSE to PoseLandmark.LEFT_EYE,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.NOSE to PoseLandmark.LEFT_MOUTH,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_MOUTH,
            
            // Torso - main body structure
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            
            // Arms - main arm structure
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            
            // Hands & Fingers (Basic) - same as ML Kit Examples
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_INDEX,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_PINKY,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_THUMB,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_INDEX,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_PINKY,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_THUMB,
            
            // Legs - main leg structure
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            
            // Feet - same as ML Kit Examples
            PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_HEEL,
            PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_FOOT_INDEX,
            PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_HEEL,
            PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_FOOT_INDEX
        )
        
        for ((start, end) in connections) {
            try {
                val startLandmark = poseLandmarks.find { it.landmarkType == start }
                val endLandmark = poseLandmarks.find { it.landmarkType == end }
                
                if (startLandmark != null && endLandmark != null) {
                    val startX = startLandmark.position.x * scaleX
                    val startY = startLandmark.position.y * scaleY
                    val endX = endLandmark.position.x * scaleX
                    val endY = endLandmark.position.y * scaleY
                    
                    // Check if coordinates are within screen bounds
                    if (startX >= 0 && startX <= screenWidth && startY >= 0 && startY <= screenHeight &&
                        endX >= 0 && endX <= screenWidth && endY >= 0 && endY <= screenHeight) {
                        canvas.drawLine(startX, startY, endX, endY, connectionPaint)
                    }
                }
            } catch (e: Exception) {
                // Skip this connection if there's an error
                continue
            }
        }
    }
}
