# Image Resizer - Modern Android App

A modern, high-performance Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** that enables users to resize images to custom dimensions or standard aspect-ratio presets, with options for scaling strategies, output formats (JPEG, PNG, WEBP), and quality compression.

---

## ✨ Features

- **Custom Dimension Selection**:
  - Direct pixel input for target **Width** and **Height**.
  - **Aspect Ratio Lock / Unlock**: Toggle aspect ratio preservation. When locked, typing in either dimension automatically calculates the proportional counterpart based on original dimensions.
- **Quick Dimension Presets**:
  - **Social & Display Presets**:
    - Square 1:1 (`1080 × 1080 px`) - Instagram / Profile Avatars
    - Story / Reel 9:16 (`1080 × 1920 px`) - TikTok / Instagram Reels
    - Portrait Post 4:5 (`1080 × 1350 px`)
    - Full HD 16:9 (`1920 × 1080 px`)
    - 4K Ultra HD (`3840 × 2160 px`)
    - Twitter / X Banner (`1500 × 500 px`)
    - Avatar / Profile (`500 × 500 px`)
    - Passport Photo (`600 × 600 px`)
  - **Percentage Scaling**: Quick one-tap buttons for `25%`, `50%`, `75%`, `150%`, and `200%` of original resolution.
- **Scaling Strategies**:
  - **Exact Stretch**: Resizes precisely to the selected width and height.
  - **Aspect Fit (Letterbox / Contain)**: Preserves original aspect ratio while ensuring the image fits inside target bounds without cropping.
  - **Crop to Fill (Center Crop)**: Fills target dimensions completely by cropping excess from the center without distortion.
- **Output Formats & Compression**:
  - Support for **JPEG**, **PNG**, and **WEBP**.
  - Interactive **Compression Quality Slider** (10% to 100%) with real-time percentage feedback.
- **Modern Android Standards**:
  - **Android Photo Picker**: Uses `ActivityResultContracts.PickVisualMedia` for privacy without demanding intrusive storage permissions on Android 10+.
  - **Scoped Storage & MediaStore API**: Saves resized images directly to the user's gallery in `Pictures/ImageResizer/`.
  - **Safe Memory Allocation**: Uses `BitmapFactory.Options.inSampleSize` calculation to prevent `OutOfMemoryError` on high-resolution smartphone camera photos (e.g. 48MP, 108MP).
  - **EXIF Orientation Support**: Preserves orientation metadata so camera photos don't get accidentally rotated.
  - **One-Tap Share**: Direct integration with Android Share Sheet via `FileProvider` to share directly to WhatsApp, Telegram, Gmail, Instagram, Drive, etc.

---

## 🛠 Project Structure

```
PDFConverter/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/imageresizer/
│   │   │   ├── MainActivity.kt                  # Entry point with edge-to-edge Compose
│   │   │   ├── data/
│   │   │   │   └── Models.kt                   # Metadata, presets, formats, and results
│   │   │   ├── utils/
│   │   │   │   ├── ImageResizer.kt             # Bitmap processing, EXIF handling, safe sampling
│   │   │   │   └── FileSaver.kt                # MediaStore gallery export & FileProvider sharing
│   │   │   └── ui/
│   │   │       ├── ImageResizerViewModel.kt    # State management, aspect ratio calculations
│   │   │       ├── ImageResizerScreen.kt       # Material 3 UI with Compose
│   │   │       └── theme/                      # Typography, Color, and Theme definitions
│   │   ├── res/                                # XML resources, icons, and FileProvider paths
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml                      # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 How to Run the Project

### Option 1: Open in Android Studio (Recommended)

1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, or newer).
2. Select **Open** and browse to `/Users/manendersingh/Desktop/dev/PDFConverter`.
3. Allow Gradle to sync the dependencies.
4. Connect an Android device with USB debugging enabled or launch an Android Virtual Device (AVD).
5. Click the green **Run ▶** button (or press `Shift + F10`).

### Option 2: Build from Terminal

Ensure you have a Java 17+ JDK and Android SDK installed on your system:

```bash
# Build debug APK
./gradlew assembleDebug

# Output APK will be generated at:
# app/build/outputs/apk/debug/app-debug.apk

# Install to connected device
./gradlew installDebug
```

---

## 📱 Requirements
- **Minimum SDK**: API 24 (Android 7.0 Nougat) — Compatible with 99%+ of Android devices.
- **Target SDK**: API 34 (Android 14).
- **Language / Runtime**: Kotlin 1.9+, Java 17.
- **UI Framework**: Jetpack Compose with Material 3.
