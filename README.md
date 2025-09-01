# YogaKotlinPipeline

Skeleton Kotlin JVM multi-module project aligned with PRD pipeline in `PRD.md`.

Structure:
- `pipeline-core`: reusable core (math, angles, data models)
- `data-prep`: CLI/tools for data → CSV, angles; depends on `pipeline-core`
- `data/`: raw_images, keypoints_csv, angles_csv (placeholders)
- `models/`: training artifacts and exported `tflite/` (placeholders)

This repo currently contains only folder structure and minimal Gradle setup.


