<p align="center">
  <img src="assets/logo.png" alt="PicMeld Logo" width="200">
</p>

<h1 align="center">PicMeld</h1>

<p align="center">
  <strong>Batch photo collage — merge up to 200 photos into customizable grid layouts</strong><br>
  Lightweight, offline, zero permissions beyond storage
</p>

<p align="center">
  <a href="README_zh-CN.md">简体中文</a> · <a href="README_zh-TW.md">繁體中文</a>
</p>

---

## Features

- **Multiple layouts** — choose from 2x2, 3x3, or 1x3 grid templates
- **Drag to reorder** — long-press and drag thumbnails to rearrange photo order
- **Customizable output** — pick background color and adjust gap between images
- **Batch processing** — select up to 200 photos, automatically grouped by chosen layout
- **Smart scaling** — images are proportionally scaled to fit without cropping; background fills gaps
- **EXIF correction** — auto-rotates photos based on EXIF orientation metadata
- **Append mode** — add photos in multiple batches without duplicates
- **Live progress** — real-time progress bar with count and percentage
- **Fully offline** — no network calls, no analytics, no tracking
- **Gallery export** — output saved directly to system gallery as JPG

## Requirements

- Android 8.0 (API 26) or higher
- Storage permission only on Android 9 and below (auto-granted on Android 10+)

## Architecture

```
com.ha1baraa11.picmeld/
├── MainActivity.kt      # UI layer — photo picker, layout/color/gap controls, progress overlay
├── MainViewModel.kt     # State management — selected URIs, layout config, progress, errors
├── ImageProcessor.kt    # Core engine — bitmap scaling, EXIF fix, Canvas compositing
├── PhotoAdapter.kt      # RecyclerView adapter — thumbnail grid with drag-to-reorder
├── LayoutConfig.kt      # Layout data class — 2x2, 3x3, 1x3 presets
└── DragSwipeCallback.kt # ItemTouchHelper callback for drag-to-reorder
```

**Stack:** Kotlin · MVVM · ViewBinding · Coroutines · Material 3

## How It Works

1. Tap **Select Photos** to pick images from your gallery (multi-select supported)
2. Choose a layout: 2x2, 3x3, or 1x3
3. Long-press and drag thumbnails to reorder; tap to remove
4. Optionally adjust background color and gap between images
5. Tap **Generate** — the app groups photos by layout and composites each grid
6. Output JPGs are saved to `Pictures/PicMeld/` in your system gallery

## Build

```bash
git clone https://github.com/Ha1baraA11/PicMeld.git
cd PicMeld
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## License

MIT
