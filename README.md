# ClearScan

**A local-first Android document scanner with real-time edge guidance, multi-page workflows, PDF tools, and on-device translation.**

[English](README.md) | [简体中文](README.zh-CN.md)

[Download the latest release](https://github.com/SuiYueMengHen/ClearScan/releases/latest) | [Report an issue](https://github.com/SuiYueMengHen/ClearScan/issues) | [View source](https://github.com/SuiYueMengHen/ClearScan)

ClearScan is a native Android scanner built with Kotlin and Jetpack Compose. It keeps scans and document processing on the device, provides automatic and manual perspective correction, and combines everyday PDF utilities with optional offline multilingual translation.

> Current release: **v1.0.3**. Public APKs target ARM64 devices running Android 8.0 or newer.

## Highlights

| Area | Capabilities |
| --- | --- |
| Capture | CameraX preview, real-time document boundary guidance, flash and lens controls, single-page and multi-page sessions |
| Alignment | OpenCV edge detection, confidence-based fallback, four-corner manual adjustment, high-resolution perspective correction |
| Editing | Rotation, brightness, contrast, saturation, document enhancement, high-quality cached filter previews |
| Filters | Auto, Clean, White Paper, B&W, Ink, Magic Color, Photo, Gray, Soft Gray, and High Contrast |
| Documents | Local library, search, nested folders, rename, move, delete, share, print, and password protection |
| PDF tools | Image to PDF, PDF to image, merge, split, compress, page-level editing, watermark, and signature overlays |
| Codes | Bundled ML Kit QR and barcode recognition, safe URL opening, copy, and web search actions |
| Translation | Optional Hy-MT2 GGUF download and local inference through a llama.cpp Android runtime |
| Application | English and Simplified Chinese, light and dark themes, in-app update checks, TXT and DOCX log export |

## Scan Pipeline

1. Camera frames are analyzed at a controlled rate on a dedicated worker. Old frames are discarded to keep the preview responsive.
2. A lightweight detector draws the live document guide without blocking capture.
3. After capture, ClearScan runs a higher-resolution OpenCV detector on the orientation-corrected image.
4. The detected quadrilateral remains fully adjustable before perspective correction.
5. The corrected page can be enhanced, filtered, reordered, and exported as an image or a multi-page PDF.

If a device cannot bind CameraX preview, capture, and analysis simultaneously, ClearScan falls back to preview and capture instead of terminating the camera workflow.

## Translation Model

The Hy-MT2 model is not bundled in the APK. It is downloaded on first use from a selectable source and stored in the application-private model directory. The application validates the GGUF file before loading it.

Local translation currently requires an ARM64 device with enough free storage and memory for the approximately 1.1 GB Q4 model. Scanning, PDF tools, and document management do not require the translation model.

## Privacy

- Documents, page images, settings, logs, and downloaded models are stored locally.
- ClearScan does not require a ClearScan cloud account.
- Scanned documents are not uploaded to a ClearScan server.
- Files leave the application only after an explicit share, export, link-opening, model-download, or update action.
- Application logs record operational metadata and errors, not copies of scanned page content.

## Compatibility

| Requirement | Value |
| --- | --- |
| Minimum Android version | Android 8.0, API 26 |
| Target Android version | Android 16, API 36 |
| Public release ABI | `arm64-v8a` |
| Build JDK | JDK 17 |
| Android SDK | SDK 36 |
| Native toolchain | Android NDK and CMake |

The automated and instrumented test suite has been exercised on an ARM64 device running Android 16. Camera capabilities and native inference performance can still vary by manufacturer, so physical-device verification is recommended before production deployment.

## Install

Download the signed APK and checksum from the [v1.0.3 release](https://github.com/SuiYueMengHen/ClearScan/releases/tag/v1.0.3):

- `ClearScan-v1.0.3-arm64-v8a.apk`
- `ClearScan-v1.0.3-arm64-v8a.apk.sha256`

Version 1.0.3 introduces the permanent ClearScan release certificate. A debug-signed v1.0.2 installation cannot be upgraded in place. Export important documents, uninstall the old debug build, and then install v1.0.3. Later releases signed with the same certificate can upgrade normally.

## Build From Source

```bash
git clone https://github.com/SuiYueMengHen/ClearScan.git
cd ClearScan
./gradlew testDownloadModelDebugUnitTest :app:assembleDownloadModelDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/downloadModel/debug/app-downloadModel-debug.apk
```

Run the full local verification suite with:

```bash
./gradlew testDownloadModelDebugUnitTest lintDownloadModelDebug :app:assembleDownloadModelDebug
```

With an authorized Android device connected, run instrumented tests with:

```bash
./gradlew connectedDownloadModelDebugAndroidTest
```

## Release Signing

Release builds read signing material from environment variables:

```text
CLEARSCAN_KEYSTORE_PATH
CLEARSCAN_KEYSTORE_PASSWORD
CLEARSCAN_KEY_ALIAS
CLEARSCAN_KEY_PASSWORD
```

The signing key must never be committed to the repository. The tag-triggered GitHub workflow runs only when the repository variable `RELEASE_SIGNING_CONFIGURED` is set to `true`. Maintainers may instead sign locally and upload only the signed APK and checksum.

## Project Structure

```text
app/src/main/java/com/clearscan/
  MainActivity.kt             Compose UI and application workflows
  DocumentEdgeDetector.kt     OpenCV detection and perspective correction
  DocumentFrameAnalyzer.kt    Throttled CameraX live-frame analysis
  ClearScanDatabase.kt        Room entities, DAO, and migration
  OverlayEditors.kt           Watermark and signature editors
  BarcodeAnalyzer.kt          ML Kit QR and barcode analysis
  AppUpdater.kt               GitHub release update flow
  SettingsRepository.kt       DataStore-backed application settings
  LogExporter.kt              TXT and DOCX log export

third_party/llama.cpp/         Native inference runtime source
```

## Known Limitations

- Public release APKs currently target ARM64 only.
- PDF editing is page-oriented; it is not an Acrobat-style text-layout editor.
- Real-time edge guidance may fall back to capture-only mode on constrained Camera2 implementations.
- Hy-MT2 startup time and throughput depend heavily on device memory bandwidth and CPU support.

## Contributing

Bug reports, reproducible device-specific camera logs, detection fixtures, and focused pull requests are welcome. Before opening an issue, include the ClearScan version, Android version, device model, steps to reproduce, and an exported application log when available.

## Third-Party Software

ClearScan uses CameraX, Jetpack Compose, Room, OpenCV, ML Kit, and a modified llama.cpp Android runtime. The Hy-MT2 model is downloaded separately and remains subject to the model publisher's terms. Review all applicable third-party notices before redistributing the application.

## License

ClearScan is released under the [MIT License](LICENSE). Third-party components and downloaded model files remain subject to their respective licenses and terms.
