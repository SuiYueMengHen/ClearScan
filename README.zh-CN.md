# ClearScan

**一款本地优先的 Android 文档扫描应用，支持实时边缘引导、多页扫描、PDF 工具与端侧翻译。**

[English](README.md) | [简体中文](README.zh-CN.md)

[下载最新版本](https://github.com/SuiYueMengHen/ClearScan/releases/latest) | [提交问题](https://github.com/SuiYueMengHen/ClearScan/issues) | [查看源码](https://github.com/SuiYueMengHen/ClearScan)

ClearScan 使用 Kotlin 和 Jetpack Compose 原生开发。应用在设备本地完成扫描与文档处理，提供自动及手动透视校正，并将常用 PDF 工具与可选的离线多语言翻译整合在一个应用中。

> 当前版本：**v1.0.3**。公开 APK 面向运行 Android 8.0 或更高版本的 ARM64 设备。

## 主要功能

| 模块 | 功能 |
| --- | --- |
| 拍摄 | CameraX 相机预览、实时文档边界引导、闪光灯与镜头控制、单页及多页扫描 |
| 对齐 | OpenCV 边缘检测、置信度回退、四角自由调整、高清透视拉直 |
| 编辑 | 旋转、亮度、对比度、饱和度、文档增强与高质量缓存滤镜预览 |
| 滤镜 | 自动、净化、白纸、黑白、墨迹、魔法彩色、照片、灰度、柔和灰度和高对比 |
| 文档 | 本地文档库、搜索、多级文件夹、重命名、移动、删除、分享、打印和密码保护 |
| PDF 工具 | 图片转 PDF、PDF 转图片、合并、拆分、压缩、页面级编辑、水印和签名 |
| 编码识别 | 内置 ML Kit 二维码与条形码识别、安全打开链接、复制和网页搜索 |
| 翻译 | 可选下载 Hy-MT2 GGUF 模型，通过 llama.cpp Android 运行时进行本地推理 |
| 应用能力 | 中英文界面、明暗主题、应用内更新检测、TXT 与 DOCX 日志导出 |

## 扫描流程

1. 相机画面在独立工作线程中按受控频率分析，过期帧会被丢弃，避免预览积压。
2. 轻量检测器实时绘制文档边框，不阻塞拍摄操作。
3. 拍照后，ClearScan 会在方向校正后的较高清图像上运行完整 OpenCV 检测。
4. 自动识别的四边形仍可自由调整，再执行透视拉直。
5. 拉直后的页面可以增强、添加滤镜、排序，并导出为图片或多页 PDF。

如果设备无法同时绑定 CameraX 预览、拍照和图像分析，ClearScan 会自动降级为预览与拍照模式，避免相机流程直接崩溃。

## 翻译模型

Hy-MT2 模型不会打包进 APK。首次使用翻译功能时，用户可以选择下载源；模型下载后存储在应用私有目录，加载前会验证 GGUF 文件。

本地翻译目前需要 ARM64 设备，并需要为约 1.1 GB 的 Q4 模型预留足够存储空间和运行内存。扫描、PDF 工具及文档管理功能不依赖翻译模型。

## 隐私说明

- 文档、页面图片、设置、日志及下载的模型均保存在本地。
- ClearScan 不要求注册 ClearScan 云端账号。
- 扫描文档不会上传到 ClearScan 服务器。
- 只有在用户主动分享、导出、打开链接、下载模型或检查更新时，应用才会访问外部目标。
- 应用日志记录运行信息与错误，不保存扫描页面内容副本。

## 兼容性

| 要求 | 配置 |
| --- | --- |
| 最低 Android 版本 | Android 8.0，API 26 |
| 目标 Android 版本 | Android 16，API 36 |
| 公开版本 ABI | `arm64-v8a` |
| 构建 JDK | JDK 17 |
| Android SDK | SDK 36 |
| 原生工具链 | Android NDK 与 CMake |

自动化测试与仪器测试已在运行 Android 16 的 ARM64 真机上执行。不同厂商的相机能力和本地推理性能仍可能存在差异，生产使用前建议在目标真机上完成验证。

## 安装

从 [v1.0.3 Release](https://github.com/SuiYueMengHen/ClearScan/releases/tag/v1.0.3) 下载正式签名 APK 与校验文件：

- `ClearScan-v1.0.3-arm64-v8a.apk`
- `ClearScan-v1.0.3-arm64-v8a.apk.sha256`

v1.0.3 开始使用长期正式签名证书。使用 Debug 签名的 v1.0.2 无法直接覆盖升级。请先导出重要文档，卸载旧 Debug 版本，再安装 v1.0.3。此后使用同一证书签名的版本可以正常覆盖升级。

## 从源码构建

```bash
git clone https://github.com/SuiYueMengHen/ClearScan.git
cd ClearScan
./gradlew testDownloadModelDebugUnitTest :app:assembleDownloadModelDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/downloadModel/debug/app-downloadModel-debug.apk
```

运行完整本地验证：

```bash
./gradlew testDownloadModelDebugUnitTest lintDownloadModelDebug :app:assembleDownloadModelDebug
```

连接并授权 Android 真机后，可运行仪器测试：

```bash
./gradlew connectedDownloadModelDebugAndroidTest
```

## 正式签名

Release 构建通过以下环境变量读取签名信息：

```text
CLEARSCAN_KEYSTORE_PATH
CLEARSCAN_KEYSTORE_PASSWORD
CLEARSCAN_KEY_ALIAS
CLEARSCAN_KEY_PASSWORD
```

签名密钥不得提交到仓库。只有在仓库变量 `RELEASE_SIGNING_CONFIGURED` 设置为 `true` 时，tag 触发的 GitHub 工作流才会运行。维护者也可以在本地签名，仅上传签名后的 APK 与校验文件。

## 项目结构

```text
app/src/main/java/com/clearscan/
  MainActivity.kt             Compose UI 与应用业务流程
  DocumentEdgeDetector.kt     OpenCV 检测和透视校正
  DocumentFrameAnalyzer.kt    CameraX 实时帧限频分析
  ClearScanDatabase.kt        Room 实体、DAO 与数据库迁移
  OverlayEditors.kt           水印与签名编辑器
  BarcodeAnalyzer.kt          ML Kit 二维码和条形码分析
  AppUpdater.kt               GitHub Release 更新流程
  SettingsRepository.kt       基于 DataStore 的应用设置
  LogExporter.kt              TXT 与 DOCX 日志导出

third_party/llama.cpp/         本地推理运行时源码
```

## 已知限制

- 公开 Release APK 目前仅支持 ARM64。
- PDF 编辑属于页面级编辑，不是 Acrobat 式文字排版编辑器。
- 在部分 Camera2 能力受限的设备上，实时边缘引导可能降级为仅预览与拍照。
- Hy-MT2 的启动速度与翻译吞吐量高度依赖设备内存带宽和 CPU 支持。

## 参与贡献

欢迎提交错误报告、可复现的厂商相机日志、检测测试素材及范围明确的 Pull Request。提交问题时，请附上 ClearScan 版本、Android 版本、设备型号、复现步骤，并在可以获取时附上应用导出的运行日志。

## 第三方软件

ClearScan 使用 CameraX、Jetpack Compose、Room、OpenCV、ML Kit，以及修改后的 llama.cpp Android 运行时。Hy-MT2 模型需要单独下载，并受模型发布者条款约束。重新分发应用前，请检查所有适用的第三方许可与声明。

## 许可证

ClearScan 使用 [MIT License](LICENSE) 发布。第三方组件与下载的模型文件仍分别遵循其自身许可证和使用条款。
