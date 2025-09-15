package com.yogakotlinpipeline.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentYoutubeVideoBinding

class YoutubeVideoFragment : Fragment() {
    
    private var _binding: FragmentYoutubeVideoBinding? = null
    private val binding get() = _binding!!
    private var timeoutHandler: android.os.Handler? = null
    private var timeoutRunnable: Runnable? = null
    private var hasTriedFallback = false
    private var currentVideoId = ""
    private var currentStartSeconds = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeVideoBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("YoutubeVideoFragment", "onViewCreated called")
        
        // Get pose information from arguments
        val poseName = arguments?.getString("pose_name") ?: "Dandasana"
        val poseDisplayName = arguments?.getString("pose_display_name") ?: "Staff Pose"
        val poseDescription = arguments?.getString("pose_description") ?: ""
        val poseDifficulty = arguments?.getString("pose_difficulty") ?: "Beginner"
        
        Log.d("YoutubeVideoFragment", "Pose: $poseDisplayName ($poseName)")
        
        // Set the title
        binding.tvPoseTitle.text = poseDisplayName
        binding.tvSanskritName.text = poseName
        binding.tvPoseDescription.text = poseDescription
        binding.tvPoseDifficulty.text = poseDifficulty
        
        // Get video ID and optional start time for the pose
        val (videoId, startSeconds) = getVideoInfoForPose(poseName)
        Log.d("YoutubeVideoFragment", "Video ID: $videoId start: $startSeconds")
        
        // Store current video info for fallback
        currentVideoId = videoId
        currentStartSeconds = startSeconds
        
        // Load YouTube video directly
        loadYouTubeVideo(videoId, startSeconds)
        
        // Add a fallback button in case video doesn't load
        binding.btnSkipVideo.text = "Watched Tutorial? Let's Do It"
        
        // Note: Retry functionality is handled automatically via fallback mechanism
        
        // Set up back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up action buttons
        setupActionButtons()
        
        // Set up bottom navigation
        setupBottomNavigation()
    }
    
    private fun getVideoInfoForPose(poseName: String): Pair<String, Int> {
        val url = when (poseName.lowercase()) {
            // Map exact Sanskrit names from YogaAsanaDataProvider
            "gomukhasana", "gomukh", "cow face pose" -> "https://www.youtube.com/watch?v=CwUw_2HpTdM"
            "vrksasana", "vrikshasan", "tree pose" -> "https://www.youtube.com/watch?v=SPJYQDkaZ3w"
            "urdhva prasarita eka padasana", "standing split", "standing splits" -> "https://www.youtube.com/watch?v=c30T6q7AVsU"
            "virabhadrasana ii", "warrior pose 2", "warrior ii pose", "warrior 2" -> "https://youtu.be/azCEB_BDWxg?t=106"
            "paschimottanasana", "pashimottanasana", "seated forward bend" -> "https://www.youtube.com/watch?v=qsJMLBcvcU0"
            "naukasana", "boat pose", "navasana", "boat" -> "https://www.youtube.com/watch?v=SFzxXr-if68"
            "dandasana", "staff pose" -> "https://www.youtube.com/watch?v=yIt2lNAcVeY"
            "trikonasana", "trikonasan", "triangle pose" -> "https://www.youtube.com/shorts/wRqvn2N9V7g"
            "chakrasana", "chakrasan", "wheel pose" -> "https://www.youtube.com/watch?v=NiDhg35OCxI" // Original video - will show embedding restriction message
            "parsvottanasana", "pyramid pose" -> "https://www.youtube.com/shorts/cQJTNWWEH-Y"
            "yoganidrasana", "yogaindrasana", "yogic sleep pose" -> "https://www.youtube.com/shorts/htGkI9ALWow"
            "raja kapotasana", "king pigeon", "king pigeon pose", "eka pada rajakapotasana", "rajakapotasana", "pigeon pose" -> "https://www.youtube.com/shorts/TfR3e-5PGJU"
            "prasarita padottanasana", "wide-legged forward bend" -> "https://www.youtube.com/watch?v=cnyUaieabic"
            else -> {
                Log.w("YoutubeVideoFragment", "No video mapping found for pose: $poseName, using default")
                "https://www.youtube.com/watch?v=CwUw_2HpTdM"
            }
        }
        Log.d("YoutubeVideoFragment", "Mapped pose '$poseName' to URL: $url")
        return parseYouTubeUrl(url)
    }

    private fun parseYouTubeUrl(url: String): Pair<String, Int> {
        return try {
            val lower = url.lowercase()
            var videoId = ""
            var startSeconds = 0

            // Extract start time (t= or start=) if present
            val uri = android.net.Uri.parse(url)
            val tParam = uri.getQueryParameter("t")
            val startParam = uri.getQueryParameter("start")
            val parsedT = tParam?.toIntOrNull()
            val parsedStart = startParam?.toIntOrNull()
            if (parsedT != null && parsedT >= 0) startSeconds = parsedT
            if (parsedStart != null && parsedStart >= 0) startSeconds = parsedStart

            // Extract ID patterns
            when {
                // Standard watch URL
                lower.contains("youtube.com/watch") -> {
                    videoId = uri.getQueryParameter("v") ?: ""
                }
                // Short link youtu.be/<id>
                lower.contains("youtu.be/") -> {
                    val path = uri.path ?: ""
                    videoId = path.trim('/').split('/').firstOrNull().orEmpty()
                }
                // Shorts URL youtube.com/shorts/<id>
                lower.contains("youtube.com/shorts/") -> {
                    val segments = uri.pathSegments
                    val idx = segments.indexOf("shorts")
                    if (idx >= 0 && idx + 1 < segments.size) {
                        videoId = segments[idx + 1]
                    }
                }
            }

            if (videoId.isBlank()) videoId = "CwUw_2HpTdM"
            videoId to startSeconds
        } catch (_: Exception) {
            "CwUw_2HpTdM" to 0
        }
    }
    
    private fun loadTestPage() {
        _binding?.webViewVideo?.loadUrl("file:///android_asset/test_video.html")
    }
    
    private fun loadYouTubeVideo(videoId: String, startSeconds: Int = 0) {
        loadYouTubeVideoWithUrl(videoId, startSeconds, false)
    }
    
    private fun loadYouTubeVideoWithUrl(videoId: String, startSeconds: Int = 0, isFallback: Boolean = false) {
        val startParam = if (startSeconds > 0) "&start=$startSeconds" else ""
        val embedUrl = if (isFallback) {
            // Fallback: Use direct YouTube watch URL instead of embed
            "https://www.youtube.com/watch?v=$videoId$startParam"
        } else {
            // Try multiple approaches to avoid Error 153
            when {
                // First try: youtube-nocookie.com with minimal parameters
                !hasTriedFallback -> "https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&playsinline=1$startParam"
                // Second try: Regular youtube.com with minimal parameters
                else -> "https://www.youtube.com/embed/$videoId?enablejsapi=1&playsinline=1$startParam"
            }
        }
        
        Log.d("YoutubeVideoFragment", "Loading YouTube video with embed URL: $embedUrl (fallback: $isFallback)")
        
        // Set up a timeout to detect if video fails to load
        timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        timeoutRunnable = Runnable {
            if (_binding?.progressLoading?.visibility == View.VISIBLE) {
                Log.w("YoutubeVideoFragment", "Video loading timeout after 15 seconds")
                _binding?.progressLoading?.visibility = View.GONE
                android.widget.Toast.makeText(context, "Video loading timeout - you can skip and continue", android.widget.Toast.LENGTH_LONG).show()
                _binding?.btnSkipVideo?.visibility = View.VISIBLE
                _binding?.btnSkipVideo?.text = "Skip Video & Continue"
            }
        }
        timeoutHandler?.postDelayed(timeoutRunnable!!, 15000) // 15 second timeout
        
        _binding?.webViewVideo?.apply {
            // Enable cookies for YouTube auth/consent flows
            try {
                val cm = android.webkit.CookieManager.getInstance()
                cm.setAcceptCookie(true)
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            } catch (_: Throwable) {}

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            
            // Enhanced YouTube-specific settings
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            settings.databaseEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            
            // Additional settings for better video playback
            settings.setGeolocationEnabled(false)
            
            // Add JavaScript interface for video completion and error detection
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onVideoCompleted() {
                    this@YoutubeVideoFragment.onVideoCompleted()
                }
                
                @android.webkit.JavascriptInterface
                fun onVideoError(errorMessage: String) {
                    this@YoutubeVideoFragment.onVideoError(errorMessage)
                }
            }, "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    _binding?.progressLoading?.visibility = View.VISIBLE
                    Log.d("YoutubeVideoFragment", "Starting to load video: $url")
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("YoutubeVideoFragment", "YouTube video loaded successfully")
                    _binding?.progressLoading?.visibility = View.GONE
                    
                    // Cancel timeout since video loaded successfully
                    timeoutRunnable?.let { runnable ->
                        timeoutHandler?.removeCallbacks(runnable)
                    }
                    
                    // Inject JavaScript to detect video completion and configuration errors
                    if (_binding != null && isAdded) {
                        injectVideoCompletionDetection()
                        injectVideoErrorDetection()
                        injectYouTubeIFrameAPI()
                    }
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Allow YouTube URLs to load
                    Log.d("YoutubeVideoFragment", "Loading URL: $url")
                    return false
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e("YoutubeVideoFragment", "WebView error: $errorCode - $description for URL: $failingUrl")
                    _binding?.progressLoading?.visibility = View.GONE
                    
                    // Show user-friendly error message
                    val errorMessage = when (errorCode) {
                        -2 -> "Video unavailable - may be restricted or removed"
                        -6 -> "Video unavailable - embedding may be disabled"
                        -8 -> "Video unavailable - network error"
                        153 -> "Video player configuration error - YouTube restrictions"
                        else -> "Video player configuration error: $description"
                    }
                    
                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                    
                    // Try multiple fallback approaches for error 153
                    if (errorCode == 153 && !hasTriedFallback) {
                        hasTriedFallback = true
                        Log.d("YoutubeVideoFragment", "Attempting fallback for error 153")
                        // Try different embed approach first, then direct URL
                        loadYouTubeVideoWithUrl(getCurrentVideoId(), getCurrentStartSeconds(), false)
                        return
                    }
                    
                    // Show skip button more prominently when video fails
                    _binding?.btnSkipVideo?.visibility = View.VISIBLE
                    _binding?.btnSkipVideo?.text = "Skip Video & Continue"
                }
                
                override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.e("YoutubeVideoFragment", "HTTP error: ${errorResponse?.statusCode} for URL: ${request?.url}")
                    
                    if (errorResponse?.statusCode == 403 || errorResponse?.statusCode == 404) {
                        _binding?.progressLoading?.visibility = View.GONE
                        android.widget.Toast.makeText(context, "Video player configuration error - video may be restricted", android.widget.Toast.LENGTH_LONG).show()
                        _binding?.btnSkipVideo?.visibility = View.VISIBLE
                        _binding?.btnSkipVideo?.text = "Skip Video & Continue"
                    }
                }
            }

            // Needed for proper JS dialogs/fullscreen handling and better compatibility
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                    Log.d("YoutubeVideoFragment", "console: ${message?.message()}")
                    return super.onConsoleMessage(message)
                }
            }
            
            loadUrl(embedUrl)
        }
    }
    
    private fun injectVideoCompletionDetection() {
        val javascript = """
            javascript:
            (function() {
                // Function to check if video is completed
                function checkVideoCompletion() {
                    var video = document.querySelector('video');
                    if (video) {
                        // Check if video has ended
                        if (video.ended || video.currentTime >= video.duration - 1) {
                            // Video is completed
                            window.Android.onVideoCompleted();
                            return;
                        }
                        
                        // Check for YouTube's end screen
                        var endScreen = document.querySelector('.ytp-endscreen-content');
                        if (endScreen) {
                            window.Android.onVideoCompleted();
                            return;
                        }
                    }
                    
                    // Check again in 2 seconds
                    setTimeout(checkVideoCompletion, 2000);
                }
                
                // Start checking after a delay to ensure video is loaded
                setTimeout(checkVideoCompletion, 3000);
            })();
        """.trimIndent()
        
        _binding?.webViewVideo?.loadUrl(javascript)
    }
    
    private fun injectVideoErrorDetection() {
        val javascript = """
            javascript:
            (function() {
                // Function to check for video player errors
                function checkVideoErrors() {
                    // Check for YouTube error messages
                    var errorElements = document.querySelectorAll('.ytp-error, .ytp-error-content, .ytp-error-text');
                    if (errorElements.length > 0) {
                        var errorText = '';
                        errorElements.forEach(function(element) {
                            errorText += element.textContent + ' ';
                        });
                        if (errorText.trim()) {
                            window.Android.onVideoError('YouTube Error: ' + errorText.trim());
                            return;
                        }
                    }
                    
                    // Check for video player configuration errors
                    var player = document.querySelector('#player');
                    if (player) {
                        var iframe = player.querySelector('iframe');
                        if (iframe && iframe.src.includes('embed')) {
                            // Check if iframe is blocked or has errors
                            if (iframe.style.display === 'none' || iframe.style.visibility === 'hidden') {
                                window.Android.onVideoError('Video player configuration error - iframe blocked');
                                return;
                            }
                        }
                    }
                    
                    // Check for "Video unavailable" text
                    var unavailableText = document.querySelector('*');
                    if (unavailableText && unavailableText.textContent && 
                        unavailableText.textContent.toLowerCase().includes('video unavailable')) {
                        window.Android.onVideoError('Video unavailable - may be restricted or removed');
                        return;
                    }
                    
                    // Check for error 153 or configuration error messages
                    var errorText = document.body ? document.body.textContent : '';
                    if (errorText && (
                        errorText.includes('error 153') || 
                        errorText.includes('video player configuration error') ||
                        errorText.includes('An error occurred') ||
                        errorText.includes('Playback error')
                    )) {
                        window.Android.onVideoError('Video player configuration error detected');
                        return;
                    }
                    
                    // Check again in 3 seconds
                    setTimeout(checkVideoErrors, 3000);
                }
                
                // Start checking after a delay to ensure page is loaded
                setTimeout(checkVideoErrors, 2000);
            })();
        """.trimIndent()
        
        _binding?.webViewVideo?.loadUrl(javascript)
    }
    
    private fun getCurrentVideoId(): String = currentVideoId
    private fun getCurrentStartSeconds(): Int = currentStartSeconds
    
    private fun setupActionButtons() {
        // Skip Video Button
        binding.btnSkipVideo.setOnClickListener {
            Log.d("YoutubeVideoFragment", "Skip video button clicked")
            // Navigate directly to pose correction
            navigateToPoseCorrection()
        }
        
        // Start Pose Correction Button
        binding.btnStartPoseCorrection.setOnClickListener {
            Log.d("YoutubeVideoFragment", "Start pose correction button clicked")
            navigateToPoseCorrection()
        }
        
        // Initially hide the "Start Practice" button until video completes
        binding.btnStartPoseCorrection.visibility = View.GONE
    }
    
    // This method will be called from JavaScript when video completes
    fun onVideoCompleted() {
        if (_binding == null || !isAdded || viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.DESTROYED) && !viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
        activity?.runOnUiThread {
            Log.d("YoutubeVideoFragment", "Video completed!")
            // Show the "Start Practice" button and make it more prominent
            _binding?.btnStartPoseCorrection?.visibility = View.VISIBLE
            _binding?.btnStartPoseCorrection?.text = "🎯 Start Practice Now!"
            _binding?.btnStartPoseCorrection?.setBackgroundResource(R.drawable.neumorphic_button_primary)
            
            // Show a completion message
            android.widget.Toast.makeText(
                context,
                "Video completed! Ready to practice?",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // This method will be called from JavaScript when video has errors
    fun onVideoError(errorMessage: String) {
        if (_binding == null || !isAdded || viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.DESTROYED) && !viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
        activity?.runOnUiThread {
            Log.e("YoutubeVideoFragment", "Video error detected: $errorMessage")
            _binding?.progressLoading?.visibility = View.GONE
            
            // Show error message to user
            android.widget.Toast.makeText(
                context,
                errorMessage,
                android.widget.Toast.LENGTH_LONG
            ).show()
            
            // Show skip button prominently
            _binding?.btnSkipVideo?.visibility = View.VISIBLE
            _binding?.btnSkipVideo?.text = "Skip Video & Continue"
        }
    }
    
    private fun navigateToPoseCorrection() {
        val poseName = arguments?.getString("pose_name") ?: "Dandasana"
        val poseDisplayName = arguments?.getString("pose_display_name") ?: "Staff Pose"
        val poseDescription = arguments?.getString("pose_description") ?: ""
        val poseDifficulty = arguments?.getString("pose_difficulty") ?: "Beginner"
        
        val bundle = Bundle().apply {
            putString("pose_name", poseName)
            putString("pose_display_name", poseDisplayName)
            putString("pose_description", poseDescription)
            putString("pose_difficulty", poseDifficulty)
        }
        
        try {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_poseCalibrationFragment, bundle)
            Log.d("YoutubeVideoFragment", "Navigation to pose correction successful!")
        } catch (e: Exception) {
            Log.e("YoutubeVideoFragment", "Navigation to pose correction failed: ${e.message}", e)
            // Fallback to home screen
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside1Fragment)
        }
    }
    
    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside1Fragment)
        }
        
        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside3Fragment)
        }
        
        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside4Fragment)
        }
        
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_profileFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Clean up timeout handler
        timeoutRunnable?.let { runnable ->
            timeoutHandler?.removeCallbacks(runnable)
        }
        timeoutHandler = null
        timeoutRunnable = null
        
        try {
            _binding?.webViewVideo?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        } catch (_: Throwable) {}
        _binding = null
    }
    
    private fun injectYouTubeIFrameAPI() {
        val javascript = """
            javascript:
            (function() {
                // Load YouTube IFrame API if not already loaded
                if (typeof YT === 'undefined') {
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
                    
                    // Set up the API ready callback
                    window.onYouTubeIframeAPIReady = function() {
                        console.log('YouTube IFrame API loaded successfully');
                        // Try to find and initialize any YouTube players
                        var iframes = document.querySelectorAll('iframe[src*="youtube.com/embed"]');
                        iframes.forEach(function(iframe) {
                            try {
                                var player = new YT.Player(iframe);
                                console.log('YouTube player initialized');
                            } catch (e) {
                                console.log('Could not initialize YouTube player:', e);
                            }
                        });
                    };
                }
            })();
        """.trimIndent()

        _binding?.webViewVideo?.loadUrl(javascript)
    }
}
