# ClearScan

ClearScan is a privacy-focused Android document scanner built with Kotlin and Jetpack Compose. It combines camera-based scanning, perspective correction, image enhancement, local document management, PDF utilities, and on-device multilingual translation in one clean application.

## Features

- CameraX document capture with orientation correction and compressed image output
- Automatic document-edge detection and manually adjustable four-corner cropping
- Perspective correction, rotation, enhancement, filters, and image adjustments
- Multi-page PDF and image document preview
- PDF merge, split, compression, image conversion, watermark, and signature tools
- QR code scanning and image format conversion
- Local document library with search, rename, delete, share, and print actions
- Offline multilingual translation powered by Hy-MT2 and a bundled llama.cpp Android runtime
- Model downloads from ModelScope or Hugging Face with progress reporting and validation
- English and Simplified Chinese interfaces
- Light and dark themes, configurable save locations, per-document passwords, and in-app logs

## Technology

- Kotlin and Jetpack Compose
- CameraX and ExifInterface
- Room
- ML Kit Barcode Scanning
- llama.cpp Android runtime
- Hy-MT2 GGUF model downloaded on demand

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Android NDK and CMake supported by the Android Gradle Plugin
- Android 8.0 (API 26) or newer
- An ARM64 Android device for local translation

## Build

Clone the repository and build the download-model variant:

```bash
git clone https://github.com/SuiYueMengHen/ClearScan.git
cd ClearScan
./gradlew testDownloadModelDebugUnitTest :app:assembleDownloadModelDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/downloadModel/debug/app-downloadModel-debug.apk
```

The Hy-MT2 model is not stored in this repository or packaged in the APK. ClearScan downloads it into app-private storage when translation is configured for the first time.

## Privacy

Documents, settings, logs, and downloaded translation models are stored locally. ClearScan does not require an account or upload scanned documents to a ClearScan server. Files leave the device only when the user explicitly invokes Android sharing or another external action.

## Project Status

ClearScan is under active development. Camera behavior, native inference performance, and file-provider integrations may vary between Android vendors, so physical-device testing is recommended before production deployment.

## Third-Party Software

This repository includes a modified llama.cpp Android runtime under `third_party/llama.cpp`. Third-party components remain subject to their respective licenses and notices. Hy-MT2 model files are downloaded separately and are governed by the model publisher's terms.

