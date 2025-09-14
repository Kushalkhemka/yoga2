# YouTube Video Feature

## Overview
The YouTube video feature allows users to watch instructional videos for each yoga pose in the explore section. When a user clicks on any pose in the explore section, they are taken to a dedicated video player screen that automatically starts playing the corresponding YouTube video.

## Implementation Details

### Files Created/Modified

1. **YoutubeVideoFragment.kt** - New fragment for displaying YouTube videos
2. **fragment_youtube_video.xml** - Layout for the video player screen
3. **ic_arrow_back.xml** - Back arrow icon for navigation
4. **nav_graph.xml** - Updated navigation graph to include the new fragment
5. **ExploreFragment.kt** - Modified to navigate to video player instead of pose calibration
6. **fragment_explore.xml** - Updated description text
7. **item_pose.xml** - Updated call-to-action text

### Features

- **Autoplay Videos**: Videos automatically start playing when the screen loads
- **Skip Video Option**: Users can skip the video and go directly to pose correction
- **Video Completion Detection**: Automatically detects when video ends and shows practice button
- **Practice Flow**: Seamless transition from video learning to pose correction practice
- **Pose Information Display**: Shows pose name, Sanskrit name, description, and difficulty level
- **Responsive Design**: Video player adapts to different screen sizes
- **Loading Indicator**: Shows progress while video is loading
- **Navigation**: Back button to return to explore section
- **Bottom Navigation**: Consistent navigation bar across all screens

### Video Mapping

Currently, the following poses are mapped to YouTube videos:

- **Gomukhasana (Cow Face Pose)**: Uses the specific video ID you provided (`CwUw_2HpTdM`)
- **All other poses**: Currently use a placeholder video ID for demonstration

### Technical Implementation

- Uses Android WebView to embed YouTube videos
- Implements proper WebView settings for video playback
- Handles JavaScript and mixed content for YouTube compatibility
- Includes proper error handling and logging

### Usage

1. Navigate to the Explore section
2. Tap on any yoga pose card
3. The video player screen will open with the corresponding video
4. Video will automatically start playing
5. **Skip Video**: Click "Skip Video" to go directly to pose correction practice
6. **After Video Completion**: The "Start Practice" button appears automatically
7. **Start Practice**: Click "Start Practice" to begin pose correction with real-time feedback
8. Use the back button to return to the explore section

### Future Enhancements

- Add real YouTube video IDs for all 13 poses
- Implement video quality selection
- Add video progress tracking
- Include video duration and description
- Add favorite/bookmark functionality
- Implement offline video caching

## Notes

- Internet permission is already included in the AndroidManifest.xml
- The WebView is configured to handle YouTube's embed requirements
- Autoplay may be blocked by some browsers/devices due to user gesture requirements
