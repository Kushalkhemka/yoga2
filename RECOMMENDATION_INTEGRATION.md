# Yoga Recommendation System Integration

This document describes the integration of the AI-powered yoga recommendation system with the YogaKotlinPipeline Android app.

## Overview

The recommendation system uses a FastAPI backend with sentence transformers to provide personalized yoga asana recommendations based on user preferences collected during the onboarding process.

## Architecture

### Backend (FastAPI)
- **File**: `recommendation_backend.py`
- **Endpoint**: `https://20cc2d34dff0.ngrok-free.app/recommend/`
- **Model**: Sentence Transformers (all-MiniLM-L6-v2)
- **Data**: `yoga_embeddings.pkl` (1.6MB embeddings file)

### Android App Integration
- **API Service**: `RecommendationApiService.kt`
- **Network Layer**: `NetworkService.kt`
- **User Profile**: `UserProfile.kt`
- **Cache**: `LoginCache.kt`

## User Preference Collection

The app collects user preferences through three screens:

### Preference1Fragment
- **Physical Issues**: back pain, knee pain, shoulder pain, neck pain, joint stiffness
- **Mental Issues**: stress, low flexibility, digestive issues, balance issues

### Preference2Fragment
- **Goals**: weight loss, flexibility, core strength, stress relief, better posture, digestion, endurance, relaxation

### Preference3Fragment
- **Demographics**: age, height, weight
- **Experience Level**: beginner, intermediate, advanced
- **Mental Health**: additional mental health considerations
- **Pregnancy Status**: pregnant/not pregnant

## API Integration

### Request Format
```json
{
  "age": 25,
  "height": 170,
  "weight": 65,
  "goals": ["flexibility", "stress relief"],
  "physical_issues": ["back pain"],
  "mental_issues": ["stress", "anxiety"],
  "level": "beginner"
}
```

### Response Format
```json
{
  "recommended_asanas": [
    {
      "name": "Supta Baddha Konasana",
      "score": 0.386,
      "benefits": "Stretches the hips and inner thighs, promotes relaxation, relieves stress.",
      "contraindications": "Not for hip injuries or high BP."
    }
  ]
}
```

## Implementation Details

### Network Layer
- **Retrofit2**: HTTP client for API calls
- **OkHttp3**: Network interceptor for logging
- **Gson**: JSON serialization/deserialization
- **Coroutines**: Asynchronous operations

### Caching Strategy
- **SharedPreferences**: Local storage for user profile and recommendations
- **7-day validity**: Recommendations are cached for 7 days
- **Auto-refresh**: New recommendations generated when cache expires

### UI Integration
- **Home Screen**: "Recommended For You" section displays top 2 recommendations
- **Dynamic Content**: Recommendations update based on user profile
- **Click Handlers**: Users can tap recommendations to start yoga sessions

## Files Modified/Added

### New Files
- `app/src/main/java/com/yogakotlinpipeline/app/utils/RecommendationApiService.kt`
- `app/src/main/java/com/yogakotlinpipeline/app/utils/NetworkService.kt`
- `app/src/main/assets/yoga_embeddings.pkl`

### Modified Files
- `app/build.gradle.kts` - Added Retrofit and OkHttp dependencies
- `app/src/main/java/com/yogakotlinpipeline/app/utils/YogaRecommendationService.kt` - Updated to use API
- `app/src/main/java/com/yogakotlinpipeline/app/Preference3Fragment.kt` - Updated to use new service
- `app/src/main/java/com/yogakotlinpipeline/app/Inside1Fragment.kt` - Updated to display recommendations
- `app/src/main/res/layout/fragment_inside1.xml` - Added recommendation card IDs

## Dependencies Added

```kotlin
// Retrofit for HTTP requests
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.9.3")
implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

// Coroutines for async operations
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
```

## Usage Flow

1. **User Onboarding**: User completes preference screens (1-3)
2. **Profile Completion**: App sends user data to recommendation API
3. **Recommendation Generation**: Backend processes user profile and returns personalized asanas
4. **Caching**: Recommendations stored locally for 7 days
5. **Display**: Home screen shows top 2 recommendations
6. **Interaction**: Users can tap recommendations to start yoga sessions

## Testing

### API Test
```bash
curl -X POST "https://20cc2d34dff0.ngrok-free.app/recommend/" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 25,
    "height": 170,
    "weight": 65,
    "goals": ["flexibility"],
    "physical_issues": ["back pain"],
    "mental_issues": ["stress"],
    "level": "beginner"
  }'
```

### Build Test
```bash
cd YogaKotlinPipeline
./gradlew assembleDebug
```

## Error Handling

- **Network Errors**: Fallback to default recommendations
- **API Failures**: Graceful degradation with user feedback
- **Cache Miss**: Auto-regeneration of recommendations
- **Invalid Data**: Validation and error messages

## Performance Considerations

- **Async Operations**: All API calls use coroutines
- **Caching**: Reduces API calls and improves response time
- **Background Processing**: Recommendations generated in background
- **Memory Management**: Proper lifecycle management for coroutines

## Security

- **HTTPS**: All API calls use secure connections
- **Data Privacy**: User data stored locally, not transmitted unnecessarily
- **Input Validation**: All user inputs validated before API calls

## Future Enhancements

- **Offline Mode**: Local recommendation generation
- **Real-time Updates**: Push notifications for new recommendations
- **A/B Testing**: Different recommendation algorithms
- **Analytics**: Track recommendation effectiveness
- **Personalization**: Machine learning for better recommendations over time

