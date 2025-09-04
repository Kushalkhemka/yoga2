package com.yogakotlinpipeline.app.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkService {
    companion object {
        private const val TAG = "NetworkService"
        private const val BASE_URL = "https://20cc2d34dff0.ngrok-free.app/"
        
        @Volatile
        private var INSTANCE: NetworkService? = null
        
        fun getInstance(): NetworkService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkService().also { INSTANCE = it }
            }
        }
    }
    
    private val apiService: RecommendationApiService by lazy {
        createApiService()
    }
    
    private fun createApiService(): RecommendationApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(RecommendationApiService::class.java)
    }
    
    suspend fun getRecommendations(userProfile: UserProfile): List<YogaRecommendation> {
        return try {
            val userInput = UserInputRequest(
                age = userProfile.age,
                height = userProfile.height,
                weight = userProfile.weight,
                goals = userProfile.goals,
                physical_issues = userProfile.getPhysicalIssues(),
                mental_issues = userProfile.getAllMentalIssues(),
                level = userProfile.level
            )
            
            val response = apiService.getRecommendations(userInput)
            
            if (response.isSuccessful) {
                val recommendationResponse = response.body()
                if (recommendationResponse != null) {
                    recommendationResponse.recommended_asanas.map { asana ->
                        YogaRecommendation(
                            name = asana.name,
                            score = asana.score,
                            benefits = asana.benefits,
                            contraindications = asana.contraindications,
                            level = userProfile.level,
                            description = asana.benefits
                        )
                    }
                } else {
                    Log.w(TAG, "Empty response body")
                    emptyList()
                }
            } else {
                Log.e(TAG, "API call failed: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations: ${e.message}", e)
            emptyList()
        }
    }
}

