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

The following poses are mapped to YouTube videos with exact Sanskrit name matching:

- **Gomukhasana (Cow Face Pose)**: `CwUw_2HpTdM`
- **Vrksasana (Tree Pose)**: `SPJYQDkaZ3w`
- **Urdhva Prasarita Eka Padasana (Standing Split)**: `c30T6q7AVsU`
- **Virabhadrasana II (Warrior II Pose)**: `azCEB_BDWxg` (starts at 106s)
- **Paschimottanasana (Seated Forward Bend)**: `qsJMLBcvcU0`
- **Naukasana (Boat Pose)**: `SFzxXr-if68`
- **Dandasana (Staff Pose)**: `yIt2lNAcVeY`
- **Trikonasana (Triangle Pose)**: `wRqvn2N9V7g` (YouTube Shorts)
- **Chakrasana (Wheel Pose)**: `NiDhg35OCxI` (Yoga for Back pain - Chakrasana The Wheel Pose)
- **Parsvottanasana (Pyramid Pose)**: `cQJTNWWEH-Y` (YouTube Shorts)
- **Yoganidrasana (Yogic Sleep Pose)**: `htGkI9ALWow` (YouTube Shorts)
- **Raja Kapotasana (King Pigeon Pose)**: `TfR3e-5PGJU` (YouTube Shorts)
- **Prasarita Padottanasana (Wide-Legged Forward Bend)**: `cnyUaieabic`

**Fallback**: If no specific mapping is found, the default video (`CwUw_2HpTdM`) is used.

### Technical Implementation

- Uses Android WebView to embed YouTube videos
- Implements proper WebView settings for video playback
- Handles JavaScript and mixed content for YouTube compatibility
- Includes comprehensive error handling and logging
- **Enhanced Error Handling**: Detects video unavailability with specific error messages
- **Timeout Mechanism**: 15-second timeout to detect failed video loads
- **Fallback Options**: Skip button becomes prominent when videos fail to load
- **Improved WebView Settings**: Enhanced user agent and caching for better YouTube compatibility

### Usage

1. Navigate to the Explore section
2. Tap on any yoga pose card
3. The video player screen will open with the corresponding video
4. Video will automatically start playing
5. **Skip Video**: Click "Skip Video" to go directly to pose correction practice
6. **After Video Completion**: The "Start Practice" button appears automatically
7. **Start Practice**: Click "Start Practice" to begin pose correction with real-time feedback
8. Use the back button to return to the explore section

### Troubleshooting Video Issues

If videos show as "unavailable", this can be due to:

1. **Video Restrictions**: Some videos may have embedding disabled or regional restrictions
2. **Network Issues**: Poor internet connection or network restrictions
3. **Video Removal**: Videos may have been removed or made private by the uploader
4. **YouTube Policy Changes**: YouTube may have changed embedding policies

**Solutions Implemented**:
- Comprehensive error handling with specific error messages
- 15-second timeout detection for failed loads
- Prominent "Skip Video & Continue" button when videos fail
- Enhanced WebView settings for better compatibility
- Detailed logging for debugging

### Future Enhancements

- Implement video quality selection
- Add video progress tracking
- Include video duration and description
- Add favorite/bookmark functionality
- Implement offline video caching
- Add alternative video sources for unavailable videos

## Notes

- Internet permission is already included in the AndroidManifest.xml
- The WebView is configured to handle YouTube's embed requirements
- Autoplay may be blocked by some browsers/devices due to user gesture requirements
