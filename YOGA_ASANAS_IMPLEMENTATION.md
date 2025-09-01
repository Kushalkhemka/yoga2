# Yoga Asanas Implementation

This document describes the implementation of the 13 yoga asanas in the explore section of the YogaKotlinPipeline app.

## Overview

The explore section now displays 13 carefully selected yoga asanas with proper difficulty level classification, search functionality, and filtering capabilities.

## Asanas Included

### Beginner Level (3 asanas)
1. **Prasarita Padottanasana** - Wide-Legged Forward Bend
2. **Dandasana** - Staff Pose  
3. **Boat Pose** - Navasana

### Intermediate Level (6 asanas)
4. **Urdhva Prasarita Eka Padasana** - Standing Split
5. **Vrksasana** - Tree Pose
6. **Warrior II Pose** - Virabhadrasana II
7. **Paschimottanasana** - Seated Forward Bend
8. **Trikonsana** - Triangle Pose
9. **Gomukhasana** - Cow Face Pose

### Advanced Level (4 asanas)
10. **Chakrasana** - Wheel Pose
11. **Parsvottanasana** - Intense Side Stretch
12. **Yoganidrasana** - Yogic Sleep Pose
13. **King Pigeon** - Kapotasana

## Technical Implementation

### Files Created/Modified

1. **YogaAsana.kt** - Data class representing a yoga asana
2. **YogaAsanaDataProvider.kt** - Provider class with all asana data
3. **YogaAsanaAdapter.kt** - RecyclerView adapter for displaying asanas
4. **AssetImageHelper.kt** - Helper for loading images from assets
5. **item_yoga_asana.xml** - Layout for individual asana items
6. **fragment_inside2.xml** - Updated explore section layout
7. **Inside2Fragment.kt** - Updated fragment with RecyclerView implementation
8. **colors.xml** - Added difficulty level colors
9. **placeholder_asana.xml** - Placeholder drawable for missing images

### Features Implemented

- **Dynamic Asana Display**: Uses RecyclerView instead of static layouts
- **Difficulty Level Filtering**: Filter by Beginner, Intermediate, or Advanced
- **Search Functionality**: Search asanas by name, Sanskrit name, or description
- **Asset Image Loading**: Loads images directly from the assets folder
- **Responsive Design**: Proper handling of missing images with placeholders
- **Color-Coded Difficulty**: Green for Beginner, Orange for Intermediate, Red for Advanced

### Asset Usage

The implementation uses the PNG images from the `assets/` folder:
- All 13 asana images are loaded from assets
- Images are cropped and displayed in a 96x96dp format
- Fallback to drawable resources if asset loading fails
- Placeholder images for any missing assets

## Usage

1. Navigate to the Explore section (Inside2Fragment)
2. Use the search bar to find specific asanas
3. Use the filter tabs to view asanas by difficulty level
4. Tap on any asana to view details (currently shows a toast message)

## Future Enhancements

- Add detailed asana information screens
- Include video demonstrations
- Add pose detection integration
- Include benefits and contraindications
- Add favorite/bookmark functionality
- Include pose difficulty ratings from users
