---
up:
author: Ha1baraA11
date: 2026-04-01 15:45
relate: []
aliases:
  - Android Photo Collage App
  - Photo Collage
tags:
  - Android
  - Kotlin
  - Image Processing
down:
---

# Android Photo Collage App — Final Requirements

## 1. App Overview

- Platform: Native Android app
- Target device: Xiaomi phones
- Target system: Android 8.0 and above (focus on compatibility with 8.0 / 9.0)
- Usage: Personal use
- Network: Fully offline, no cloud integration

## 2. Core Features

- Only supports selecting images from the system gallery
- Supports selecting up to 200 images at once
- Duplicate image selection is not allowed
- Selected images are displayed as a thumbnail grid (3 per row)
- Supports deleting individual images from the selection (delete button in the top-right corner)
- Re-selecting images uses an append mode

## 3. Collage Rules

- Every 4 images form one group
- Each group produces 1 collage in a 2x2 layout
- Image placement order: top-left, top-right, bottom-left, bottom-right
- If the last group has fewer than 4 images: empty positions are filled with pure white
- No gap between collage images
- Fill color: white
- Image processing: proportional scaling, full display, allow padding, no cropping

## 4. Image Characteristics and Processing

- Default user scenario is primarily screenshots
- Default assumption is that four images usually have similar dimensions
- Automatically read and correct image orientation (EXIF)
- If any image fails to read, is corrupted, or has an abnormal format: terminate the task and show an error dialog

## 5. Output Rules

- Output format: JPG
- Save method: Save directly to the system gallery, no separate album directory
- After generation completes: Show a "Done" prompt and automatically clear the selected list

## 6. Progress and Interaction

- Show a progress bar
- Progress calculation: processed image count / total image count, updated after each image is processed
- Recommend showing both percentage and "processed count / total count"
- During processing: disable the "Select Images" and "Start Generation" buttons
- Allow background processing, but foreground processing is the most stable

## 7. UI Requirements

- Interface language: Chinese
- Style: Clean and easy to use
- Page structure: Single-page minimal layout
- Includes: title, selected count, estimated collage count, thumbnail list, select images button, start generation button, progress bar and progress text

## 8. Implementation Details

- minSdk = 26
- Development language: Kotlin
- Development tool: Android Studio
- UI approach: Native XML + ViewModel
- Image selection: Compatible approach ensuring Android 8/9 support
- Thumbnail layout: 3 per row
- Delete method: Delete button in the top-right corner of each thumbnail
- Completion: brief prompt; errors: dialog prompt

## 9. Estimated Collage Count Calculation

Estimated collage count = ceil(total images / 4)

Examples:
- 1-4 images -> 1 collage
- 5-8 images -> 2 collages
- 9-12 images -> 3 collages
