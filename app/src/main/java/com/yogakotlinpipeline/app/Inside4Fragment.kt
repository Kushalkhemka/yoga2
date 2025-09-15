package com.yogakotlinpipeline.app

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.yogakotlinpipeline.app.data.*
import com.yogakotlinpipeline.app.databinding.FragmentInside4Binding
import java.text.SimpleDateFormat
import java.util.*

class Inside4Fragment : Fragment() {

    private var _binding: FragmentInside4Binding? = null
    private val binding get() = _binding!!
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var isUpdating = false
    private lateinit var userProgressStorage: UserProgressStorage

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
        userProgressStorage = UserProgressStorage(requireContext())
        setupClickListeners()
        setupCharts()
        loadUserData()
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

        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside2Fragment)
        }
        
        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside3Fragment)
        }

        binding.btnProgress.setOnClickListener {
            // Already on progress screen, do nothing
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_profileFragment)
        }
        
        // Add a button to simulate adding a new workout (for testing)
        binding.btnBack.setOnLongClickListener {
            addNewWorkoutSession()
            true
        }
    }

    private fun setupCharts() {
        setupWeeklyChart()
        setupFlexibilityTrendChart()
    }

    private fun setupWeeklyChart() {
        val chart = binding.weeklyChart
        
        // Configure chart appearance
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(true)
        
        // Configure X-axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.textColor = Color.parseColor("#666666")
        xAxis.textSize = 12f
        
        // Configure Y-axis
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = Color.parseColor("#666666")
        leftAxis.textSize = 12f
        leftAxis.axisMinimum = 0f
        
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        
        // Configure legend
        val legend = chart.legend
        legend.isEnabled = false
        
        // Initial data
        updateWeeklyChart()
    }

    private fun setupFlexibilityTrendChart() {
        val chart = binding.flexibilityTrendChart
        
        // Configure chart appearance
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        
        // Configure X-axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.parseColor("#666666")
        xAxis.textSize = 12f
        
        // Configure Y-axis
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = Color.parseColor("#666666")
        leftAxis.textSize = 12f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        
        // Configure legend
        val legend = chart.legend
        legend.isEnabled = false
        
        // Initial data
        updateFlexibilityTrendChart()
    }

    private fun loadUserData() {
        // Add sample data if no data exists
        val sessions = userProgressStorage.getWorkoutSessions()
        if (sessions.isEmpty()) {
            userProgressStorage.addSampleData()
        }
        
        // Load and display user data
        updateAllData()
    }
    
    private fun updateWeeklyChart() {
        val weeklyData = userProgressStorage.getWeeklyProgress()
        val chart = binding.weeklyChart
        
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        weeklyData.dailySessions.forEachIndexed { index, session ->
            entries.add(BarEntry(index.toFloat(), session.duration.toFloat()))
            labels.add(dateFormat.format(session.date))
        }
        
        val dataSet = BarDataSet(entries, "Duration (min)")
        dataSet.color = Color.parseColor("#8B5CF6")
        dataSet.valueTextColor = Color.parseColor("#333333")
        dataSet.valueTextSize = 10f
        
        val data = BarData(dataSet)
        data.barWidth = 0.6f
        
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun updateFlexibilityTrendChart() {
        val weeklyData = userProgressStorage.getWeeklyProgress()
        val chart = binding.flexibilityTrendChart
        
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        weeklyData.dailySessions.forEachIndexed { index, session ->
            entries.add(Entry(index.toFloat(), session.flexibilityScore))
            labels.add(dateFormat.format(session.date))
        }
        
        val dataSet = LineDataSet(entries, "Flexibility Score")
        dataSet.color = Color.parseColor("#10B981")
        dataSet.lineWidth = 3f
        dataSet.setCircleColor(Color.parseColor("#10B981"))
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#10B981")
        dataSet.fillAlpha = 50
        dataSet.valueTextColor = Color.parseColor("#333333")
        dataSet.valueTextSize = 10f
        
        val data = LineData(dataSet)
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun updateAllData() {
        val weeklyData = userProgressStorage.getWeeklyProgress()
        val streakData = userProgressStorage.getStreakData()
        val goalProgress = userProgressStorage.getUserGoals()
        
        // Update weekly summary
        updateWeeklySummary(weeklyData)
        
        // Update streaks
        updateStreaks(streakData)
        
        // Update goals
        updateGoals(goalProgress)
        
        // Update charts
        updateWeeklyChart()
        updateFlexibilityTrendChart()
    }

    private fun updateWeeklySummary(data: WeeklyProgress) {
        // Update workout count with animation
        val workoutCount = data.totalWorkouts
        // Note: These would need proper view IDs in the layout
        
        // Update total time
        val hours = data.totalDuration / 60
        val minutes = data.totalDuration % 60
        val timeText = "${hours}h ${minutes}m"
        
        // Update calories
        val caloriesText = data.totalCalories.toString()
        
        // Update average duration
        val avgDuration = data.totalDuration / data.totalWorkouts
        val avgDurationText = "${avgDuration}m"
    }

    private fun updateStreaks(data: StreakData) {
        // Update current streak
        val currentStreakText = "${data.currentStreak} Days"
        
        // Update longest streak
        val longestStreakText = "${data.longestStreak} Days"
    }

    private fun updateGoals(data: GoalProgress) {
        // Update weekly workouts progress
        val progress = (data.currentWeeklyWorkouts.toFloat() / data.weeklyWorkoutsGoal * 100).toInt()
        
        // Update flexibility score
        val flexibilityProgress = (data.currentFlexibilityScore / data.flexibilityGoal * 100).toInt()
        binding.tvFlexibilityPercentage.text = "${flexibilityProgress}%"
        animateProgressBar(binding.progressFlexibility, flexibilityProgress)
    }

    private fun animateTextChange(textView: View?, newText: String) {
        textView?.let { view ->
            if (view is android.widget.TextView) {
                ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1.0f).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                view.text = newText
            }
        }
    }

    private fun animateProgressBar(progressBar: View?, progress: Int) {
        progressBar?.let { view ->
            ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.0f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun addNewWorkoutSession() {
        val newSession = WorkoutSession(
            id = "session_${System.currentTimeMillis()}",
            date = Date(),
            duration = (20..50).random(),
            calories = (150..350).random(),
            poseCount = (5..12).random(),
            flexibilityScore = (70..90).random().toFloat(),
            poseType = "Evening Flow"
        )
        
        userProgressStorage.saveWorkoutSession(newSession)
        userProgressStorage.updateGoalsProgress()
        
        // Refresh the data
        updateAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

