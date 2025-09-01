package com.yogakotlinpipeline.app.utils

data class UserProfile(
    val age: Int,
    val height: Int, // in cm
    val weight: Int, // in kg
    val level: String, // beginner, intermediate, advanced
    val pregnant: Boolean,
    val problemAreas: List<String>, // from preference1
    val goals: List<String>, // from preference2
    val mentalIssues: List<String> // mental health considerations
) {
    companion object {
        fun createEmpty(): UserProfile {
            return UserProfile(
                age = 0,
                height = 0,
                weight = 0,
                level = "beginner",
                pregnant = false,
                problemAreas = emptyList(),
                goals = emptyList(),
                mentalIssues = emptyList()
            )
        }
    }
}
