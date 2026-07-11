package com.clearscan

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.exifinterface.media.ExifInterface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.Tasks
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private val Teal = ComposeColor(0xFF0FA7A0)
private val TealDark = ComposeColor(0xFF07847F)
private val TextDark = ComposeColor(0xFF111827)
private val Muted = ComposeColor(0xFF737B8C)
private val Soft = ComposeColor(0xFFF4F6F8)
private const val HY_MT_MODEL_NAME = "Hy-MT2-1.8B-Q4_K_M.gguf"
private const val HY_MT_MODEL_BYTES = 1_133_080_448L

@VisibleForTesting
fun isExpectedHyMt2Model(sizeBytes: Long, magic: String): Boolean {
    return sizeBytes == HY_MT_MODEL_BYTES && magic == "GGUF"
}

object AppLogger {
    private const val MAX_LOG_BYTES = 512 * 1024
    private var appContext: Context? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private var initialized = false

    fun init(context: Context) {
        appContext = context.applicationContext
        if (initialized) return
        initialized = true
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("Crash", "Uncaught exception on ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        i("App", "Logger initialized")
    }

    fun file(context: Context? = appContext): File {
        val root = File((context ?: error("Logger context missing")).filesDir, "logs")
        root.mkdirs()
        return File(root, "clearscan.log")
    }

    fun read(): String = runCatching {
        val log = file()
        if (log.exists()) log.readText() else ""
    }.getOrDefault("")

    fun clear() {
        runCatching { file().writeText("") }
        i("Log", "Log cleared")
    }

    fun i(tag: String, message: String) = write("INFO", tag, message)
    fun w(tag: String, message: String) = write("WARN", tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val detail = if (throwable == null) message else "$message\n${throwable.stackTraceToString()}"
        write("ERROR", tag, detail)
    }

    private fun write(level: String, tag: String, message: String) {
        val context = appContext ?: return
        runCatching {
            val log = file(context)
            rotateIfNeeded(log)
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            log.appendText("$time [$level] $tag: $message\n")
        }
    }

    private fun rotateIfNeeded(log: File) {
        if (!log.exists() || log.length() <= MAX_LOG_BYTES) return
        val text = log.readText()
        log.writeText(text.takeLast(MAX_LOG_BYTES / 2))
    }
}

private fun defaultCropPoints() = listOf(
    Offset(0.06f, 0.06f),
    Offset(0.94f, 0.06f),
    Offset(0.94f, 0.94f),
    Offset(0.06f, 0.94f),
)

private fun Offset.coerceCropPoint() = Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))

private fun centeredCropRatio(widthOverHeight: Float): List<Offset> {
    val margin = 0.06f
    val maxW = 1f - margin * 2f
    val maxH = 1f - margin * 2f
    var w = maxW
    var h = w / widthOverHeight
    if (h > maxH) {
        h = maxH
        w = h * widthOverHeight
    }
    val left = (1f - w) / 2f
    val top = (1f - h) / 2f
    return listOf(
        Offset(left, top),
        Offset(left + w, top),
        Offset(left + w, top + h),
        Offset(left, top + h),
    )
}

private fun requiredTypesFor(tool: String?): Set<String> = when (tool) {
    "PDF to Image", "PDF Edit", "Merge PDF", "Split PDF", "Compress PDF" -> setOf("PDF")
    "Image to PDF", "Image Format Converter" -> setOf("JPG", "JPEG", "PNG", "WEBP", "BMP", "IMAGE")
    "Watermark", "Add Signature" -> setOf("PDF", "JPG", "JPEG", "PNG", "WEBP", "BMP", "IMAGE")
    else -> emptySet()
}

private fun minSelectionFor(tool: String?): Int = if (tool == "Merge PDF") 2 else 1

private fun maxSelectionFor(tool: String?): Int = if (tool == "Merge PDF") Int.MAX_VALUE else 1

private fun defaultToolOption(tool: String): String = when (tool) {
    "Split PDF" -> "All pages"
    "Compress PDF" -> "Medium"
    "Image Format Converter" -> "PNG"
    else -> "Standard"
}

private fun toolOptions(tool: String?): List<String> = when (tool) {
    "Split PDF" -> listOf("All pages", "First page")
    "Compress PDF" -> listOf("Low", "Medium", "High")
    "Image Format Converter" -> listOf("JPEG", "PNG", "WEBP", "BMP", "PDF")
    else -> emptyList()
}

private fun selectionHint(tool: String): String {
    val min = minSelectionFor(tool)
    val types = requiredTypesFor(tool).joinToString("/")
    return if (min > 1) "Select at least $min $types documents first." else "Select a $types document first."
}

private fun selectionHint(tool: String, settings: AppSettings): String {
    if (settings.language != "中文") return selectionHint(tool)
    val min = minSelectionFor(tool)
    val types = requiredTypesFor(tool).joinToString("/")
    return if (min > 1) "请先选择至少 $min 个 $types 文件。" else "请先选择一个 $types 文件。"
}

private fun toolLabel(tool: String, settings: AppSettings): String = when (tool) {
    "Merge PDF" -> tr(settings, "Merge PDF", "合并 PDF")
    "Split PDF" -> tr(settings, "Split PDF", "拆分 PDF")
    "Compress PDF" -> tr(settings, "Compress PDF", "压缩 PDF")
    "PDF to Image" -> tr(settings, "PDF to Image", "PDF 转图片")
    "Image to PDF" -> tr(settings, "Image to PDF", "图片转 PDF")
    "Image Format Converter" -> tr(settings, "Image Format Converter", "图片格式转换")
    "PDF Edit" -> tr(settings, "PDF Edit", "PDF 编辑")
    "Watermark" -> tr(settings, "Watermark", "添加水印")
    "Add Signature" -> tr(settings, "Add Signature", "添加签名")
    "QR Code Scan" -> tr(settings, "QR Code Scan", "二维码扫描")
    "ID Card Scan" -> tr(settings, "ID Card Scan", "证件扫描")
    "Translate" -> tr(settings, "Translate", "翻译")
    else -> tool
}

private fun tr(settings: AppSettings, english: String, chinese: String): String {
    return if (settings.language == "中文") chinese else english
}

class MainActivity : ComponentActivity() {
    private val model: ClearScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(this)
        AppLogger.i("MainActivity", "onCreate")
        setContent {
            ClearScanTheme {
                ClearScanApp(model)
            }
        }
    }
}

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey val id: Long,
    val title: String,
    val type: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val pageCount: Int,
    val thumbnailPath: String,
    val exportPath: String,
)

@Entity(tableName = "scan_pages")
data class ScanPage(
    @PrimaryKey val id: Long,
    val documentId: Long,
    val originalPath: String,
    val processedPath: String,
    val cropPoints: String,
    val filter: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val rotation: Int,
)

data class AppSettings(
    val language: String = "English",
    val theme: String = "Light",
    val loggedIn: Boolean = false,
    val accountName: String = "Guest",
    val accountEmail: String = "",
    val passwordMap: Map<Long, String> = emptyMap(),
    val defaultSavePath: String = "Internal Storage",
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun document(id: Long): Document?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: Document)

    @Query("UPDATE documents SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(page: ScanPage)

    @Query("SELECT * FROM scan_pages WHERE documentId = :documentId ORDER BY id")
    suspend fun pages(documentId: Long): List<ScanPage>
}

@Database(entities = [Document::class, ScanPage::class], version = 1, exportSchema = false)
abstract class ClearScanDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}

enum class Tab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Docs("Docs", Icons.Default.Description),
    Camera("Scan", Icons.Default.CameraAlt),
    Tools("Tools", Icons.Default.GridView),
    Me("Me", Icons.Default.AccountCircle),
}

enum class Screen {
    Shell, Camera, Crop, Edit, Filter, Adjust, Save, Detail, Share, ToolSelect, Translate, Settings, Account, Help, About, Legal, AppLogs
}

data class ModelSource(
    val id: String,
    val label: String,
    val url: String,
    val mirrorType: String,
)

data class ModelDownloadState(
    val status: String = "missing",
    val sourceId: String = "modelscope",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val progress: Float = 0f,
    val error: String? = null,
)

data class TranslationState(
    val modelStatus: String = "missing",
    val selectedSource: String = "modelscope",
    val sourceLang: String = "Auto",
    val targetLang: String = "Chinese",
    val inputText: String = "",
    val outputText: String = "",
    val isTranslating: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val download: ModelDownloadState = ModelDownloadState(),
)

data class UiState(
    val tab: Tab = Tab.Home,
    val screen: Screen = Screen.Shell,
    val documents: List<Document> = emptyList(),
    val query: String = "",
    val selected: Document? = null,
    val scanBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val scanSourcePath: String? = null,
    val cropPoints: List<Offset> = defaultCropPoints(),
    val qrMode: Boolean = false,
    val backStack: List<Screen> = emptyList(),
    val activeTool: String? = null,
    val selectedToolIds: Set<Long> = emptySet(),
    val toolOption: String = "Medium",
    val cropPreset: String = "Original",
    val selectedFilter: String = "Original",
    val translationState: TranslationState = TranslationState(),
    val savedResultDetail: Boolean = false,
    val legalTitle: String = "",
    val logText: String = "",
    val captureMessage: String? = null,
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
)

class ClearScanViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(application, ClearScanDatabase::class.java, "clearscan.db").build()
    private val dao = database.documentDao()
    private val prefs = application.getSharedPreferences("clearscan-settings", Context.MODE_PRIVATE)
    private val settingsFlow = MutableStateFlow(loadSettings())
    private val queryFlow = MutableStateFlow("")
    private val navFlow = MutableStateFlow(UiState())
    private var allDocuments: List<Document> = emptyList()
    private var downloadCancelRequested = false
    private var translationEngine: InferenceEngine? = null

    private val modelSources = listOf(
            ModelSource(
                id = "modelscope",
                label = "国内镜像源 / ModelScope",
                url = "https://modelscope.cn/models/Tencent-Hunyuan/Hy-MT2-1.8B-GGUF/resolve/master/Hy-MT2-1.8B-Q4_K_M.gguf",
                mirrorType = "mirror",
            ),
            ModelSource(
                id = "hfmirror",
                label = "国内镜像源 / HF Mirror",
                url = "https://hf-mirror.com/tencent/Hy-MT2-1.8B-GGUF/resolve/main/Hy-MT2-1.8B-Q4_K_M.gguf",
                mirrorType = "mirror",
            ),
            ModelSource(
                id = "huggingface",
                label = "官方源 / Hugging Face",
                url = "https://huggingface.co/tencent/Hy-MT2-1.8B-GGUF/resolve/main/Hy-MT2-1.8B-Q4_K_M.gguf",
                mirrorType = "official",
            ),
            ModelSource(
                id = "local",
                label = "本地模型文件",
                url = "",
                mirrorType = "local",
            ),
    )

    val ui: StateFlow<UiState> = combine(dao.observeDocuments(), settingsFlow, queryFlow, navFlow) { docs, settings, query, nav ->
        allDocuments = docs
        nav.copy(
            documents = docs.filter { it.title.contains(query, ignoreCase = true) },
            query = query,
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        AppLogger.init(application)
        AppLogger.i("ViewModel", "ClearScanViewModel created")
        refreshTranslationModelState()
        viewModelScope.launch {
            seedIfEmpty()
        }
    }

    private fun go(screen: Screen, update: UiState.() -> UiState = { this }) {
        val current = navFlow.value
        navFlow.value = current.update().copy(screen = screen, backStack = current.backStack + current.screen)
    }

    private fun replace(screen: Screen, update: UiState.() -> UiState = { this }) {
        navFlow.value = navFlow.value.update().copy(screen = screen)
    }

    fun selectTab(tab: Tab) {
        AppLogger.i("Navigation", "Select tab ${tab.name}")
        navFlow.value = navFlow.value.copy(tab = tab, screen = Screen.Shell, backStack = emptyList(), activeTool = null, selectedToolIds = emptySet())
    }

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun openCamera() {
        AppLogger.i("Scan", "Open camera")
        go(Screen.Camera) { copy(captureMessage = null, qrMode = false, activeTool = null, selectedToolIds = emptySet()) }
    }

    fun openQrScanner() {
        AppLogger.i("QR", "Open QR scanner")
        go(Screen.Camera) { copy(captureMessage = null, qrMode = true, activeTool = "QR Code Scan", selectedToolIds = emptySet()) }
    }

    fun back() {
        val state = navFlow.value
        AppLogger.i("Navigation", "Back from ${state.screen}")
        if (state.screen == Screen.Detail && state.savedResultDetail) {
            navFlow.value = state.copy(
                screen = Screen.Shell,
                tab = Tab.Docs,
                selected = null,
                backStack = emptyList(),
                savedResultDetail = false,
                activeTool = null,
                selectedToolIds = emptySet(),
            )
            return
        }
        val previous = state.backStack.lastOrNull()
        navFlow.value = if (previous != null) {
            state.copy(
                screen = previous,
                backStack = state.backStack.dropLast(1),
                activeTool = if (state.screen == Screen.ToolSelect) null else state.activeTool,
                selectedToolIds = if (state.screen == Screen.ToolSelect) emptySet() else state.selectedToolIds,
            )
        } else {
            when (state.screen) {
                Screen.Shell -> state
                else -> state.copy(screen = Screen.Shell, selected = null, activeTool = null, selectedToolIds = emptySet())
            }
        }
    }

    fun captureSample() {
        viewModelScope.launch {
            AppLogger.i("Scan", "Capture sample document")
            val bitmap = withContext(Dispatchers.Default) { ImageProcessor.sampleDocumentBitmap() }
            go(Screen.Crop) {
                copy(
                scanBitmap = bitmap,
                processedBitmap = bitmap,
                scanSourcePath = null,
                cropPoints = defaultCropPoints(),
                captureMessage = "Capturing... Please hold steady",
                )
            }
        }
    }

    fun capturePhotoFile(file: File) {
        viewModelScope.launch {
            AppLogger.i("Camera", "CameraX photo file: ${file.absolutePath}, bytes=${file.length()}")
            val sourceFile = withContext(Dispatchers.IO) {
                val optimized = File(file.parentFile, "${file.nameWithoutExtension}-optimized.jpg")
                ImageProcessor.optimizeCapturedPhoto(file, optimized) ?: file
            }
            val bitmap = withContext(Dispatchers.IO) {
                ImageProcessor.decodeCameraBitmap(sourceFile.absolutePath, maxDimension = 2048)
            }
            if (bitmap == null) {
                AppLogger.w("Camera", "Photo decode failed")
                navFlow.value = navFlow.value.copy(captureMessage = "Photo capture failed. Please try again.")
            } else if (navFlow.value.qrMode) {
                val result = withContext(Dispatchers.Default) { ImageProcessor.scanQr(bitmap) }
                AppLogger.i("QR", "QR scan result: ${result ?: "none"}")
                replace(Screen.Camera) {
                    copy(
                    captureMessage = result ?: "No QR code found. Try again.",
                    )
                }
            } else {
                AppLogger.i(
                    "Scan",
                    "Photo preview decoded ${bitmap.width}x${bitmap.height}, optimizedBytes=${sourceFile.length()}, opening crop",
                )
                go(Screen.Crop) {
                    copy(
                    scanBitmap = bitmap,
                    processedBitmap = bitmap,
                    scanSourcePath = sourceFile.absolutePath,
                    cropPoints = ImageProcessor.detectDocumentCorners(bitmap),
                    captureMessage = "Captured. Adjust the crop corners.",
                    )
                }
            }
        }
    }

    fun importBitmap(uri: Uri, context: Context) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImageProcessor.decodeUriBitmap(context, uri, maxDimension = 2048)
            }
            if (bitmap == null) {
                AppLogger.w("Scan", "Unable to decode imported image: $uri")
                navFlow.value = navFlow.value.copy(captureMessage = "Unable to open this image.")
                return@launch
            }
            go(Screen.Crop) {
                copy(
                    scanBitmap = bitmap,
                    processedBitmap = bitmap,
                    scanSourcePath = null,
                    cropPoints = ImageProcessor.detectDocumentCorners(bitmap),
                )
            }
        }
    }

    fun toEdit() {
        go(Screen.Edit) { copy(processedBitmap = processedBitmap ?: scanBitmap) }
    }

    fun setCropPoint(index: Int, point: Offset) {
        val points = navFlow.value.cropPoints.toMutableList()
        if (index in points.indices) {
            points[index] = Offset(point.x.coerceIn(0f, 1f), point.y.coerceIn(0f, 1f))
            navFlow.value = navFlow.value.copy(cropPoints = points)
        }
    }

    fun setCropPoints(points: List<Offset>) {
        if (points.size >= 4) {
            navFlow.value = navFlow.value.copy(cropPoints = points.take(4).map { it.coerceCropPoint() })
        }
    }

    fun setCropPreset(label: String) {
        val ratio = when (label) {
            "A4" -> 210f / 297f
            "Letter" -> 8.5f / 11f
            "Legal" -> 8.5f / 14f
            else -> null
        }
        navFlow.value = navFlow.value.copy(cropPreset = label, cropPoints = ratio?.let { centeredCropRatio(it) } ?: defaultCropPoints())
    }

    fun applyCropAndEdit() {
        val previewBitmap = navFlow.value.processedBitmap ?: navFlow.value.scanBitmap ?: return
        val sourcePath = navFlow.value.scanSourcePath
        val points = navFlow.value.cropPoints.toList()
        viewModelScope.launch {
            navFlow.value = navFlow.value.copy(busy = true)
            val cropped = runCatching {
                withContext(Dispatchers.Default) {
                    val workingBitmap = sourcePath
                        ?.let { ImageProcessor.decodeCameraBitmap(it, maxDimension = 3072) }
                        ?: previewBitmap
                    ImageProcessor.enhanceDocument(ImageProcessor.perspectiveCrop(workingBitmap, points))
                }
            }.onFailure { AppLogger.e("Scan", "Perspective crop failed", it) }.getOrNull()
            if (cropped == null) {
                navFlow.value = navFlow.value.copy(
                    busy = false,
                    captureMessage = "Unable to process this photo. Please try again.",
                )
                return@launch
            }
            val state = navFlow.value
            val stack = if (state.backStack.lastOrNull() == Screen.Edit) state.backStack.dropLast(1) else state.backStack
            AppLogger.i("Scan", "Perspective crop completed ${cropped.width}x${cropped.height}")
            navFlow.value = state.copy(
                screen = Screen.Edit,
                backStack = stack,
                processedBitmap = cropped,
                scanBitmap = cropped,
                scanSourcePath = null,
                cropPoints = defaultCropPoints(),
                busy = false,
            )
        }
    }

    fun toFilter() {
        go(Screen.Filter)
    }

    fun toAdjust() {
        go(Screen.Adjust)
    }

    fun toCrop() {
        go(Screen.Crop)
    }

    fun toSave() {
        go(Screen.Save)
    }

    fun rotate() {
        val bitmap = navFlow.value.processedBitmap ?: return
        navFlow.value = navFlow.value.copy(processedBitmap = ImageProcessor.rotate(bitmap))
    }

    fun deleteScan() {
        replace(Screen.Camera) { copy(scanBitmap = null, processedBitmap = null, scanSourcePath = null) }
    }

    fun applyFilter(filter: String) {
        val base = navFlow.value.scanBitmap ?: navFlow.value.processedBitmap ?: return
        val state = navFlow.value
        val stack = if (state.backStack.lastOrNull() == Screen.Edit) state.backStack.dropLast(1) else state.backStack
        navFlow.value = state.copy(screen = Screen.Edit, backStack = stack, processedBitmap = ImageProcessor.filter(base, filter))
    }

    fun applyAdjust(brightness: Float, contrast: Float, saturation: Float) {
        val base = navFlow.value.processedBitmap ?: navFlow.value.scanBitmap ?: return
        val state = navFlow.value
        val stack = if (state.backStack.lastOrNull() == Screen.Edit) state.backStack.dropLast(1) else state.backStack
        navFlow.value = state.copy(screen = Screen.Edit, backStack = stack, processedBitmap = ImageProcessor.adjust(base, brightness, contrast, saturation))
    }

    fun saveDocument(title: String, type: String, quality: String) {
        val bitmap = navFlow.value.processedBitmap ?: navFlow.value.scanBitmap ?: return
        AppLogger.i("Document", "Save document title=$title type=$type quality=$quality bitmap=${bitmap.width}x${bitmap.height}")
        viewModelScope.launch {
            navFlow.value = navFlow.value.copy(busy = true)
            val saved = withContext(Dispatchers.IO) {
                val id = System.currentTimeMillis()
                val files = saveDirectory()
                val imageFile = File(files, "$id-page.jpg")
                ImageProcessor.writeJpeg(bitmap, imageFile, quality)
                val export = if (type == "PDF") {
                    val pdf = File(files, "$id.pdf")
                    ImageProcessor.writePdf(bitmap, pdf)
                    pdf
                } else {
                    File(files, "$id.jpg").also { ImageProcessor.writeJpeg(bitmap, it, quality) }
                }
                val doc = Document(
                    id = id,
                    title = title.ifBlank { "Untitled Scan" },
                    type = type,
                    createdAt = id,
                    sizeBytes = export.length(),
                    pageCount = 1,
                    thumbnailPath = imageFile.absolutePath,
                    exportPath = export.absolutePath,
                )
                dao.upsert(doc)
                dao.upsertPage(
                    ScanPage(id, id, imageFile.absolutePath, imageFile.absolutePath, "auto", "Auto", 0f, 1f, 1f, 0)
                )
                AppLogger.i("Document", "Saved document id=$id export=${export.absolutePath} size=${export.length()}")
                doc
            }
            navFlow.value = navFlow.value.copy(
                screen = Screen.Detail,
                backStack = emptyList(),
                busy = false,
                selected = saved,
                tab = Tab.Docs,
                savedResultDetail = true,
            )
        }
    }

    fun openDocument(document: Document) {
        AppLogger.i("Document", "Open document id=${document.id} title=${document.title} type=${document.type}")
        val bitmap = ImageProcessor.readBitmap(document.thumbnailPath, 3072)
        go(Screen.Detail) {
            copy(
            selected = document,
            tab = Tab.Docs,
            scanBitmap = bitmap,
            processedBitmap = bitmap,
            savedResultDetail = false,
            )
        }
    }

    fun shareSelected() {
        AppLogger.i("Share", "Open share for document ${navFlow.value.selected?.id}")
        go(Screen.Share)
    }

    fun renameSelected(title: String) {
        val doc = navFlow.value.selected ?: return
        AppLogger.i("Document", "Rename document id=${doc.id} title=$title")
        viewModelScope.launch {
            dao.rename(doc.id, title)
            navFlow.value = navFlow.value.copy(selected = doc.copy(title = title))
        }
    }

    fun deleteSelected() {
        val doc = navFlow.value.selected ?: return
        AppLogger.i("Document", "Delete document id=${doc.id} title=${doc.title}")
        viewModelScope.launch {
            dao.deleteDocument(doc.id)
            replace(Screen.Shell) { copy(selected = null, tab = Tab.Docs, backStack = emptyList()) }
        }
    }

    fun openSettings() {
        AppLogger.i("Settings", "Open settings")
        go(Screen.Settings) { copy(tab = Tab.Me) }
    }

    fun openAccount() {
        go(Screen.Account)
    }

    fun openAbout() {
        go(Screen.About)
    }

    fun openHelp() {
        go(Screen.Help)
    }

    fun openLegal(title: String) {
        go(Screen.Legal) { copy(legalTitle = title) }
    }

    fun openLogs() {
        AppLogger.i("Log", "Open app logs")
        go(Screen.AppLogs) { copy(logText = AppLogger.read()) }
    }

    fun refreshLogs() {
        navFlow.value = navFlow.value.copy(logText = AppLogger.read())
    }

    fun clearLogs() {
        AppLogger.clear()
        navFlow.value = navFlow.value.copy(logText = AppLogger.read(), captureMessage = "Logs cleared")
    }

    fun updateSettings(settings: AppSettings) {
        AppLogger.i("Settings", "Update settings language=${settings.language} theme=${settings.theme} path=${settings.defaultSavePath}")
        settingsFlow.value = settings
        saveSettings(settings)
    }

    fun login(name: String, email: String) {
        AppLogger.i("Account", "Login/update account name=${name.ifBlank { "ClearScan User" }} email=$email")
        settingsFlow.value = settingsFlow.value.copy(
            loggedIn = true,
            accountName = name.ifBlank { "ClearScan User" },
            accountEmail = email.ifBlank { "user@clearscan.local" },
        )
        saveSettings(settingsFlow.value)
        navFlow.value = navFlow.value.copy(captureMessage = "Signed in")
    }

    fun logout() {
        AppLogger.i("Account", "Logout")
        settingsFlow.value = settingsFlow.value.copy(loggedIn = false, accountName = "Guest", accountEmail = "")
        saveSettings(settingsFlow.value)
        navFlow.value = navFlow.value.copy(screen = Screen.Shell, tab = Tab.Me, selected = null, backStack = emptyList(), captureMessage = "Logged out")
    }

    fun setDocumentPassword(documentId: Long, password: String) {
        AppLogger.i("Security", if (password.isBlank()) "Remove password for document $documentId" else "Set password for document $documentId")
        val current = settingsFlow.value.passwordMap
        settingsFlow.value = settingsFlow.value.copy(passwordMap = if (password.isBlank()) current - documentId else current + (documentId to password))
        saveSettings(settingsFlow.value)
        navFlow.value = navFlow.value.copy(captureMessage = if (password.isBlank()) "Password removed" else "Password set")
    }

    fun runTool(name: String) {
        AppLogger.i("Tool", "Run tool entry $name")
        when (name) {
            "ID Card Scan" -> openCamera()
            "QR Code Scan" -> openQrScanner()
            "Translate" -> openTranslate()
            else -> beginTool(name)
        }
    }

    fun openTranslate() {
        AppLogger.i("Translate", "Open translate")
        refreshTranslationModelState()
        go(Screen.Translate)
    }

    fun modelSources(): List<ModelSource> = modelSources

    fun selectModelSource(sourceId: String) {
        AppLogger.i("Translate", "Select model source $sourceId")
        val state = navFlow.value.translationState
        prefs.edit().putString("translationSource", sourceId).apply()
        navFlow.value = navFlow.value.copy(translationState = state.copy(selectedSource = sourceId, download = state.download.copy(sourceId = sourceId)))
    }

    fun setTranslationInput(text: String) {
        val state = navFlow.value.translationState
        navFlow.value = navFlow.value.copy(translationState = state.copy(inputText = text, error = null))
    }

    fun setTranslationLanguages(source: String? = null, target: String? = null) {
        val state = navFlow.value.translationState
        navFlow.value = navFlow.value.copy(translationState = state.copy(sourceLang = source ?: state.sourceLang, targetLang = target ?: state.targetLang))
    }

    fun swapTranslationLanguages() {
        val state = navFlow.value.translationState
        val nextTarget = if (state.sourceLang == "Auto") detectTranslationLanguage(state.inputText) else state.sourceLang
        navFlow.value = navFlow.value.copy(
            translationState = state.copy(
                sourceLang = state.targetLang,
                targetLang = nextTarget,
                inputText = state.outputText.ifBlank { state.inputText },
                outputText = "",
                error = null,
            )
        )
    }

    fun clearTranslation() {
        val state = navFlow.value.translationState
        navFlow.value = navFlow.value.copy(translationState = state.copy(inputText = "", outputText = "", error = null))
    }

    fun cancelModelDownload() {
        AppLogger.i("Translate", "Cancel model download")
        downloadCancelRequested = true
        val state = navFlow.value.translationState
        navFlow.value = navFlow.value.copy(
            translationState = state.copy(
                download = state.download.copy(status = "paused", error = "Download paused. Tap Download Model to resume."),
                modelStatus = if (isValidHyMt2Model(translationModelFile())) "ready" else "missing",
            )
        )
    }

    fun startModelDownload() {
        val source = modelSources.firstOrNull { it.id == navFlow.value.translationState.selectedSource } ?: modelSources.first()
        AppLogger.i("Translate", "Start model install/download source=${source.id}")
        if (source.id == "local") {
            refreshTranslationModelState()
            val valid = isValidHyMt2Model(translationModelFile())
            val state = navFlow.value.translationState
            navFlow.value = navFlow.value.copy(
                translationState = state.copy(
                    error = if (valid) null else "Place $HY_MT_MODEL_NAME (${formatSize(HY_MT_MODEL_BYTES)}) in ${translationModelFile().parentFile?.absolutePath}",
                    modelStatus = if (valid) "ready" else "missing",
                )
            )
            return
        }
        viewModelScope.launch {
            downloadCancelRequested = false
            val target = translationModelFile()
            val temp = File(target.parentFile, "${target.name}.download")
            target.parentFile?.mkdirs()
            withContext(Dispatchers.IO) {
                runCatching {
                    val existingBytes = temp.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L
                    val connection = (URL(source.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        if (existingBytes > 0L) setRequestProperty("Range", "bytes=$existingBytes-")
                    }
                    val responseCode = connection.responseCode
                    check(responseCode in 200..299) { "Model server returned HTTP $responseCode" }
                    val supportsResume = existingBytes > 0L && responseCode == HttpURLConnection.HTTP_PARTIAL
                    val resumeBytes = if (supportsResume) existingBytes else 0L
                    if (!supportsResume && temp.exists()) temp.delete()
                    val contentLength = connection.contentLengthLong
                    val total = if (contentLength > 0L) resumeBytes + contentLength else 0L
                    if (total > 0L) {
                        check(total == HY_MT_MODEL_BYTES) {
                            "Unexpected model size ${formatSize(total)}; expected ${formatSize(HY_MT_MODEL_BYTES)}. Choose another source."
                        }
                    }
                    var downloaded = resumeBytes
                    var windowBytes = 0L
                    var lastUiUpdate = System.currentTimeMillis()
                    var lastSpeedUpdate = lastUiUpdate
                    connection.inputStream.use { input ->
                        FileOutputStream(temp, supportsResume).use { output ->
                            val buffer = ByteArray(1024 * 512)
                            navFlow.value = navFlow.value.copy(
                                translationState = navFlow.value.translationState.copy(
                                    modelStatus = "downloading",
                                    download = ModelDownloadState("downloading", source.id, downloaded, total, 0L, 0L, if (total > 0L) downloaded.toFloat() / total.toFloat() else 0f, null),
                                )
                            )
                            while (true) {
                                if (downloadCancelRequested) error("Download paused")
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                windowBytes += read
                                val now = System.currentTimeMillis()
                                if (now - lastUiUpdate >= 250L || (total > 0L && downloaded >= total)) {
                                    val speedWindowMs = max(1L, now - lastSpeedUpdate)
                                    val speed = windowBytes * 1000L / speedWindowMs
                                    val eta = if (total > 0L && speed > 0L) (total - downloaded).coerceAtLeast(0L) / speed else 0L
                                    val progress = if (total > 0L) downloaded.toFloat() / total.toFloat() else 0f
                                    navFlow.value = navFlow.value.copy(
                                        translationState = navFlow.value.translationState.copy(
                                            modelStatus = "downloading",
                                            download = ModelDownloadState("downloading", source.id, downloaded, total, speed, eta, progress, null),
                                        )
                                    )
                                    lastUiUpdate = now
                                    lastSpeedUpdate = now
                                    windowBytes = 0L
                                }
                            }
                            output.flush()
                        }
                    }
                    check(isValidHyMt2Model(temp)) {
                        "Downloaded model is invalid: ${formatSize(temp.length())}, expected ${formatSize(HY_MT_MODEL_BYTES)} GGUF Q4_K_M. Please retry with another source."
                    }
                    if (target.exists()) target.delete()
                    check(temp.renameTo(target)) { "Unable to finalize model file." }
                    translationEngine?.let { engine ->
                        if (engine.state.value is InferenceEngine.State.ModelReady || engine.state.value is InferenceEngine.State.Error) {
                            engine.cleanUp()
                        }
                    }
                    prefs.edit()
                        .putString("translationSource", source.id)
                        .putLong("translationModelSize", target.length())
                        .putBoolean("translationModelReady", true)
                        .apply()
                    AppLogger.i("Translate", "Validated model download complete source=${source.id} bytes=${target.length()} path=${target.absolutePath}")
                    navFlow.value = navFlow.value.copy(
                        translationState = navFlow.value.translationState.copy(
                            modelStatus = "ready",
                            selectedSource = source.id,
                            download = ModelDownloadState("ready", source.id, target.length(), target.length(), 0L, 0L, 1f, null),
                            error = null,
                        )
                    )
                }.onFailure { error ->
                    if (error.message != "Download paused") temp.delete()
                    navFlow.value = navFlow.value.copy(
                        translationState = navFlow.value.translationState.copy(
                            modelStatus = if (isValidHyMt2Model(target)) "ready" else "missing",
                            download = navFlow.value.translationState.download.copy(
                                status = if (error.message == "Download paused") "paused" else "error",
                                error = error.message ?: "Download failed",
                            ),
                            error = error.message ?: "Download failed",
                        )
                    )
                }
            }
        }
    }

    fun translateText() {
        val state = navFlow.value.translationState
        val chinese = settingsFlow.value.language == "中文"
        if (state.inputText.isBlank()) {
            AppLogger.w("Translate", "Translate requested with blank input")
            navFlow.value = navFlow.value.copy(translationState = state.copy(error = if (chinese) "请输入要翻译的文本。" else "Enter text to translate."))
            return
        }
        val modelFile = translationModelFile()
        if (!isValidHyMt2Model(modelFile)) {
            AppLogger.w("Translate", "Translate requested but model invalid: ${modelFile.absolutePath}, bytes=${modelFile.takeIf { it.exists() }?.length() ?: 0L}")
            navFlow.value = navFlow.value.copy(
                translationState = state.copy(
                    modelStatus = "missing",
                    error = if (chinese) "模型文件缺失或不完整，请重新下载 Q4_K_M 模型。" else "The Q4_K_M model is missing or incomplete. Download it again.",
                )
            )
            return
        }
        viewModelScope.launch {
            AppLogger.i("Translate", "Translate start source=${state.sourceLang} target=${state.targetLang} chars=${state.inputText.length}")
            navFlow.value = navFlow.value.copy(
                translationState = state.copy(
                    isTranslating = true,
                    progress = .2f,
                    outputText = "",
                    error = null,
                )
            )
            val translated = runCatching {
                translateWithHyMt2(state.inputText, state.sourceLang, state.targetLang)
            }.onFailure { AppLogger.e("Translate", "Hy-MT2 inference failed", it) }.getOrElse { error ->
                navFlow.value = navFlow.value.copy(
                    translationState = navFlow.value.translationState.copy(
                        isTranslating = false,
                        progress = 0f,
                        error = if (chinese) "翻译失败：${error.message ?: "模型运行错误"}" else "Translation failed: ${error.message ?: "model runtime error"}",
                    )
                )
                return@launch
            }
            navFlow.value = navFlow.value.copy(
                translationState = navFlow.value.translationState.copy(
                    isTranslating = false,
                    progress = 1f,
                    outputText = translated,
                    error = null,
                )
            )
            AppLogger.i("Translate", "Translate complete outputChars=${translated.length}")
        }
    }

    private suspend fun translateWithHyMt2(input: String, sourceLang: String, targetLang: String): String {
        require(sourceLang != targetLang || sourceLang == "Auto") { "Source and target languages must be different." }
        val engine = translationEngine ?: AiChat.getInferenceEngine(getApplication()).also { translationEngine = it }
        var engineState = engine.state.value
        if (engineState is InferenceEngine.State.Uninitialized || engineState is InferenceEngine.State.Initializing) {
            engineState = engine.state.first {
                it is InferenceEngine.State.Initialized || it is InferenceEngine.State.ModelReady || it is InferenceEngine.State.Error
            }
        }
        if (engineState is InferenceEngine.State.Error) {
            engine.cleanUp()
            engineState = engine.state.value
        }
        val diagnostics = runCatching { engine.diagnostics() }.getOrElse { "unavailable: ${it.message}" }
        AppLogger.i("TranslateNative", diagnostics.take(2_000))
        if (engineState is InferenceEngine.State.Initialized) {
            navFlow.value = navFlow.value.copy(
                translationState = navFlow.value.translationState.copy(progress = .35f)
            )
            val modelFile = translationModelFile()
            AppLogger.i("Translate", "Loading GGUF model bytes=${modelFile.length()} path=${modelFile.absolutePath}")
            engine.loadModel(modelFile.absolutePath)
        }
        check(engine.state.value is InferenceEngine.State.ModelReady) { "Hy-MT2 model could not be loaded." }
        navFlow.value = navFlow.value.copy(
            translationState = navFlow.value.translationState.copy(progress = .55f)
        )
        val chunks = splitTranslationText(input)
        check(chunks.isNotEmpty()) { "Text is empty." }
        val completed = StringBuilder()
        var lastUiUpdate = 0L
        chunks.forEachIndexed { index, chunk ->
            engine.resetConversation()
            val chunkOutput = StringBuilder()
            val prompt = buildHyMt2Prompt(chunk, sourceLang, targetLang)
            val predictLength = (chunk.length * 2).coerceIn(128, 900)
            engine.sendUserPrompt(prompt, predictLength).collect { token ->
                chunkOutput.append(token)
                val now = System.currentTimeMillis()
                if (now - lastUiUpdate >= 80L) {
                    val preview = buildString {
                        append(completed)
                        if (isNotEmpty() && chunkOutput.isNotEmpty()) append('\n')
                        append(chunkOutput.toString().trimStart())
                    }
                    navFlow.value = navFlow.value.copy(
                        translationState = navFlow.value.translationState.copy(
                            progress = .55f + .4f * (index.toFloat() / chunks.size.toFloat()),
                            outputText = preview,
                        )
                    )
                    lastUiUpdate = now
                }
            }
            if (completed.isNotEmpty()) completed.append('\n')
            completed.append(chunkOutput.toString().trim().removeSurrounding("\""))
        }
        return completed.toString().trim()
    }

    private fun refreshTranslationModelState() {
        val file = translationModelFile()
        val legacyFile = File(file.parentFile, "hy-mt2-1.8b-1.25bit.gguf")
        if (legacyFile.exists()) {
            AppLogger.w("Translate", "Removing legacy incompatible model bytes=${legacyFile.length()}")
            legacyFile.delete()
        }
        val storedSource = prefs.getString("translationSource", "modelscope") ?: "modelscope"
        val sourceId = storedSource.takeIf { id -> modelSources.any { it.id == id } } ?: "modelscope"
        val ready = isValidHyMt2Model(file)
        AppLogger.i("Translate", "Model validation ready=$ready path=${file.absolutePath} bytes=${file.takeIf { it.exists() }?.length() ?: 0L}")
        navFlow.value = navFlow.value.copy(
            translationState = navFlow.value.translationState.copy(
                modelStatus = if (ready) "ready" else "missing",
                selectedSource = sourceId,
                download = ModelDownloadState(if (ready) "ready" else "missing", sourceId, if (ready) file.length() else 0L, if (ready) file.length() else 0L, progress = if (ready) 1f else 0f),
                error = if (ready) null else navFlow.value.translationState.error,
            )
        )
    }

    private fun translationModelFile(): File {
        return File(getApplication<Application>().filesDir, "models/hy-mt2/$HY_MT_MODEL_NAME")
    }

    private fun isValidHyMt2Model(file: File): Boolean {
        if (!file.isFile) return false
        return runCatching {
            file.inputStream().buffered().use { input ->
                val magic = ByteArray(4)
                input.read(magic) == magic.size && isExpectedHyMt2Model(file.length(), magic.decodeToString())
            }
        }.getOrDefault(false)
    }

    fun beginTool(name: String) {
        AppLogger.i("Tool", "Begin tool selection $name")
        go(Screen.ToolSelect) {
            copy(activeTool = name, selectedToolIds = emptySet(), toolOption = defaultToolOption(name), captureMessage = null)
        }
    }

    fun toggleToolDocument(documentId: Long) {
        val state = navFlow.value
        val maxSelection = maxSelectionFor(state.activeTool)
        val current = state.selectedToolIds
        val next = if (documentId in current) {
            current - documentId
        } else if (current.size < maxSelection) {
            current + documentId
        } else {
            setOf(documentId)
        }
        navFlow.value = state.copy(selectedToolIds = next)
    }

    fun setToolOption(option: String) {
        navFlow.value = navFlow.value.copy(toolOption = option)
    }

    fun executeActiveTool() {
        val state = navFlow.value
        val tool = state.activeTool ?: return
        val selectedIds = state.selectedToolIds
        AppLogger.i("Tool", "Execute tool=$tool selected=${selectedIds.joinToString()} option=${state.toolOption}")
        if (selectedIds.size < minSelectionFor(tool)) {
            AppLogger.w("Tool", "Not enough selection for $tool: ${selectedIds.size}")
            navFlow.value = state.copy(captureMessage = selectionHint(tool, settingsFlow.value))
            return
        }
        viewModelScope.launch {
            val selected = allDocuments.filter { it.id in selectedIds }
            if (selected.size < minSelectionFor(tool)) {
                AppLogger.w("Tool", "Selected documents missing for $tool")
                navFlow.value = navFlow.value.copy(captureMessage = selectionHint(tool, settingsFlow.value))
                return@launch
            }
            navFlow.value = navFlow.value.copy(busy = true)
            val result = withContext(Dispatchers.IO) {
                runToolOperation(tool, selected, navFlow.value.toolOption)
            }
            if (result != null) {
                AppLogger.i("Tool", "$tool complete outputId=${result.id} path=${result.exportPath}")
                replace(Screen.Detail) {
                    copy(
                        busy = false,
                        selected = result,
                        tab = Tab.Docs,
                        activeTool = null,
                        selectedToolIds = emptySet(),
                        scanBitmap = ImageProcessor.readBitmap(result.thumbnailPath),
                        processedBitmap = ImageProcessor.readBitmap(result.thumbnailPath),
                        captureMessage = "${toolLabel(tool, settingsFlow.value)} ${if (settingsFlow.value.language == "中文") "已完成" else "complete"}",
                    )
                }
            } else {
                AppLogger.w("Tool", "$tool failed")
                navFlow.value = navFlow.value.copy(busy = false, captureMessage = if (settingsFlow.value.language == "中文") "${toolLabel(tool, settingsFlow.value)} 失败，请选择有效文件。" else "$tool failed. Please choose a valid file.")
            }
        }
    }

    private suspend fun runToolOperation(tool: String, selected: List<Document>, option: String): Document? {
        val first = selected.firstOrNull() ?: return null
        return when (tool) {
            "PDF Edit" -> first
            "PDF to Image" -> {
                val bitmap = ImageProcessor.renderPdfFirstPage(first.exportPath) ?: return null
                writeDocumentFiles("Image from ${first.title}", "JPG", bitmap, "High")
            }
            "Image to PDF" -> {
                val bitmap = ImageProcessor.readBitmap(first.exportPath) ?: ImageProcessor.readBitmap(first.thumbnailPath) ?: return null
                writeDocumentFiles("${first.title} PDF", "PDF", bitmap, "High")
            }
            "Image Format Converter" -> {
                val bitmap = ImageProcessor.readBitmap(first.exportPath) ?: ImageProcessor.readBitmap(first.thumbnailPath) ?: return null
                val targetType = when (option.uppercase()) {
                    "JPG", "JPEG" -> "JPEG"
                    "WEBP" -> "WEBP"
                    "BMP" -> "BMP"
                    "PDF" -> "PDF"
                    else -> "PNG"
                }
                writeDocumentFiles("${first.title} - $targetType", targetType, bitmap, "High")
            }
            "Merge PDF" -> {
                val pages = selected.flatMap { doc -> ImageProcessor.documentPages(doc) }
                if (pages.isEmpty()) return null
                writeDocumentFiles("Merged PDF", "PDF", pages.first(), "Medium", pageCount = pages.size, pdfPages = pages)
            }
            "Split PDF" -> {
                val pages = ImageProcessor.renderPdfPages(first.exportPath, maxPages = if (option == "First page") 1 else Int.MAX_VALUE)
                if (pages.isEmpty()) return null
                writeDocumentFiles("${first.title} - Split", "PDF", pages.first(), "Medium", pageCount = pages.size, pdfPages = pages)
            }
            "Compress PDF" -> {
                val pages = ImageProcessor.renderPdfPages(first.exportPath).ifEmpty { return null }
                val quality = when (option) {
                    "Low" -> "Low"
                    "High" -> "High"
                    else -> "Medium"
                }
                val compressed = pages.map { ImageProcessor.downsampleForPdf(it, option) }
                writeDocumentFiles("${first.title} - Compressed", "PDF", compressed.first(), quality, pageCount = compressed.size, pdfPages = compressed)
            }
            "Watermark" -> {
                val pages = ImageProcessor.documentPages(first).ifEmpty { return null }
                val watermarked = pages.map { ImageProcessor.watermark(it, "ClearScan") }
                writeDocumentFiles("${first.title} - Watermark", first.type, watermarked.first(), "High", pageCount = watermarked.size, pdfPages = if (first.type == "PDF") watermarked else null)
            }
            "Add Signature" -> {
                val pages = ImageProcessor.documentPages(first).ifEmpty { return null }
                val signed = pages.mapIndexed { index, bitmap -> if (index == 0) ImageProcessor.addSignature(bitmap) else bitmap }
                writeDocumentFiles("${first.title} - Signed", first.type, signed.first(), "High", pageCount = signed.size, pdfPages = if (first.type == "PDF") signed else null)
            }
            else -> null
        }
    }

    private suspend fun writeDocumentFiles(
        title: String,
        type: String,
        bitmap: Bitmap,
        quality: String,
        pageCount: Int = 1,
        pdfPages: List<Bitmap>? = null,
    ): Document {
        val id = System.currentTimeMillis()
        val files = saveDirectory()
        val imageFile = File(files, "$id-page.jpg")
        ImageProcessor.writeJpeg(bitmap, imageFile, quality)
        val normalizedType = when (type.uppercase()) {
            "JPG" -> "JPEG"
            else -> type.uppercase()
        }
        val export = when (normalizedType) {
            "PDF" -> File(files, "$id.pdf").also { ImageProcessor.writePdf(pdfPages ?: listOf(bitmap), it) }
            "PNG" -> File(files, "$id.png").also { ImageProcessor.writePng(bitmap, it) }
            "WEBP" -> File(files, "$id.webp").also { ImageProcessor.writeWebp(bitmap, it) }
            "BMP" -> File(files, "$id.bmp").also { ImageProcessor.writeBmp(bitmap, it) }
            else -> File(files, "$id.jpg").also { ImageProcessor.writeJpeg(bitmap, it, quality) }
        }
        val doc = Document(id, title, normalizedType, id, export.length(), pageCount, imageFile.absolutePath, export.absolutePath)
        dao.upsert(doc)
        dao.upsertPage(ScanPage(id, id, imageFile.absolutePath, imageFile.absolutePath, "auto", "Auto", 0f, 1f, 1f, 0))
        return doc
    }

    private fun saveDirectory(): File {
        val app = getApplication<Application>()
        return if (settingsFlow.value.defaultSavePath == "Documents") {
            app.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: app.filesDir
        } else {
            app.filesDir
        }
    }

    private fun loadSettings(): AppSettings {
        val passwords = prefs.getString("passwords", "").orEmpty()
            .split("|")
            .mapNotNull { item ->
                val parts = item.split(":", limit = 2)
                parts.firstOrNull()?.toLongOrNull()?.let { id -> id to parts.getOrElse(1) { "" } }
            }
            .filter { it.second.isNotBlank() }
            .toMap()
        return AppSettings(
            language = prefs.getString("language", "English") ?: "English",
            theme = prefs.getString("theme", "Light") ?: "Light",
            loggedIn = prefs.getBoolean("loggedIn", false),
            accountName = prefs.getString("accountName", "Guest") ?: "Guest",
            accountEmail = prefs.getString("accountEmail", "") ?: "",
            passwordMap = passwords,
            defaultSavePath = prefs.getString("defaultSavePath", "Internal Storage") ?: "Internal Storage",
        )
    }

    private fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("language", settings.language)
            .putString("theme", settings.theme)
            .putBoolean("loggedIn", settings.loggedIn)
            .putString("accountName", settings.accountName)
            .putString("accountEmail", settings.accountEmail)
            .putString("defaultSavePath", settings.defaultSavePath)
            .putString("passwords", settings.passwordMap.entries.joinToString("|") { "${it.key}:${it.value}" })
            .apply()
    }

    private suspend fun seedIfEmpty() {
        if (ui.value.documents.isNotEmpty()) return
        val context = getApplication<Application>()
        val names = listOf("Contract Agreement" to "PDF", "Lecture Notes" to "PDF", "Invoice_0528" to "PDF", "ID Card" to "JPG", "Book Summary" to "PDF", "Whiteboard Notes" to "JPG")
        names.forEachIndexed { index, pair ->
            val id = System.currentTimeMillis() - index * 86_400_000L
            val bitmap = ImageProcessor.sampleDocumentBitmap(pair.first)
            val thumb = File(context.filesDir, "$id-seed.jpg")
            ImageProcessor.writeJpeg(bitmap, thumb, "Medium")
            val export = File(context.filesDir, if (pair.second == "PDF") "$id-seed.pdf" else "$id-seed-out.jpg")
            if (pair.second == "PDF") ImageProcessor.writePdf(bitmap, export) else ImageProcessor.writeJpeg(bitmap, export, "Medium")
            dao.upsert(Document(id, pair.first, pair.second, id, export.length(), 1, thumb.absolutePath, export.absolutePath))
        }
    }
}

@Composable
fun ClearScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Teal,
            secondary = TealDark,
            background = ComposeColor.White,
            surface = ComposeColor.White,
            onSurface = TextDark,
        ),
        content = content,
    )
}

@Composable
fun ClearScanApp(model: ClearScanViewModel) {
    val state by model.ui.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    BackHandler(enabled = true) {
        model.back()
    }
    LaunchedEffect(state.captureMessage) {
        state.captureMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density * 0.88f, fontScale = density.fontScale * 0.9f)
    ) {
        val colors = if (state.settings.theme == "Dark") {
            androidx.compose.material3.darkColorScheme(primary = Teal, secondary = TealDark, background = ComposeColor(0xFF111317), surface = ComposeColor(0xFF181B20), onSurface = ComposeColor(0xFFF4F6F8))
        } else {
            androidx.compose.material3.lightColorScheme(primary = Teal, secondary = TealDark, background = ComposeColor.White, surface = ComposeColor.White, onSurface = TextDark)
        }
        MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            EdgeSwipeBackBox(onBack = model::back) {
                AnimatedContent(targetState = state.screen, label = "screen") { screen ->
                    when (screen) {
                        Screen.Shell -> ShellScreen(state, model)
                        Screen.Camera -> CameraScreen(state, model)
                        Screen.Crop -> CropScreen(state, model)
                        Screen.Edit -> EditScreen(state, model)
                        Screen.Filter -> FilterScreen(state, model)
                        Screen.Adjust -> AdjustScreen(state, model)
                        Screen.Save -> SaveScreen(state, model)
                        Screen.Detail -> DetailScreen(state, model)
                        Screen.Share -> ShareScreen(state, model)
                        Screen.ToolSelect -> ToolSelectScreen(state, model)
                        Screen.Translate -> TranslateScreen(state, model)
                        Screen.Settings -> SettingsScreen(state, model)
                        Screen.Account -> AccountScreen(state, model)
                        Screen.Help -> HelpScreen(model)
                        Screen.About -> AboutScreen(state, model)
                        Screen.Legal -> LegalScreen(state, model)
                        Screen.AppLogs -> AppLogsScreen(state, model)
                    }
                }
            }
        }
        }
    }
}

@Composable
fun EdgeSwipeBackBox(onBack: () -> Unit, content: @Composable () -> Unit) {
    var edgeDrag by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start -> edgeDrag = start.x < 56f },
                    onDragEnd = { edgeDrag = false },
                    onDragCancel = { edgeDrag = false },
                    onDrag = { change, drag ->
                        if (edgeDrag && drag.x > 78f && abs(drag.y) < 64f) {
                            change.consume()
                            edgeDrag = false
                            onBack()
                        }
                    },
                )
            }
    ) {
        content()
    }
}

@Composable
fun ShellScreen(state: UiState, model: ClearScanViewModel) {
    Scaffold(
        bottomBar = { BottomNav(state.tab, model::selectTab, model::openCamera) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.tab) {
                Tab.Home -> HomeScreen(state, model)
                Tab.Docs -> DocsScreen(state, model)
                Tab.Camera -> CameraScreen(state, model)
                Tab.Tools -> ToolsScreen(state, model)
                Tab.Me -> MeScreen(state, model)
            }
        }
    }
}

@Composable
fun BottomNav(current: Tab, onTab: (Tab) -> Unit, onCamera: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(Tab.Home, Tab.Docs).forEach { NavItem(it, current == it) { onTab(it) } }
            Spacer(Modifier.width(72.dp))
            listOf(Tab.Tools, Tab.Me).forEach { NavItem(it, current == it) { onTab(it) } }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Teal, TealDark)))
                .clickable { onCamera() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = ComposeColor.White, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, tab.title, tint = if (selected) Teal else Muted, modifier = Modifier.size(27.dp))
        Text(tab.title, color = if (selected) Teal else Muted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun HomeScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ClearScan", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = model::openSettings) { Icon(Icons.Default.Settings, null, modifier = Modifier.size(30.dp)) }
            }
        }
        item { HeroCard(settings) }
        item {
            Button(
                onClick = model::openCamera,
                modifier = Modifier.fillMaxWidth().height(76.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(30.dp))
                Spacer(Modifier.width(16.dp))
                Text(tr(settings, "Scan Document", "扫描文档"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickAction(tr(settings, "Import Images", "导入图片"), Icons.Default.Image, Modifier.weight(1f)) { model.openCamera() }
                QuickAction(tr(settings, "ID Card Scan", "证件扫描"), Icons.Outlined.Badge, Modifier.weight(1f)) { model.openCamera() }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tr(settings, "Recent", "最近文档"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { model.selectTab(Tab.Docs) }) { Text(tr(settings, "View All  ›", "查看全部  ›"), color = Muted) }
            }
        }
        item {
            DocumentListCard(state.documents.take(3), model)
        }
    }
}

@Composable
fun HeroCard(settings: AppSettings) {
    val dark = settings.theme == "Dark"
    Card(
        Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (dark) ComposeColor(0xFF162321) else ComposeColor(0xFFE9FAF7)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr(settings, "Go Paperless,\nBe Productive.", "告别纸张，\n高效办公。"), Modifier.weight(1f), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            ScannerIllustration(Modifier.size(130.dp), dark)
        }
    }
}

@Composable
fun ScannerIllustration(modifier: Modifier = Modifier, dark: Boolean = false) {
    Canvas(modifier) {
        drawRoundRect(ComposeColor(0xFF23B7AE), topLeft = Offset(size.width * .35f, size.height * .05f), size = Size(size.width * .48f, size.height * .72f), cornerRadius = CornerRadius(10f, 10f))
        drawRoundRect(if (dark) ComposeColor(0xFF232A32) else ComposeColor.White, topLeft = Offset(size.width * .18f, size.height * .18f), size = Size(size.width * .48f, size.height * .64f), cornerRadius = CornerRadius(8f, 8f))
        repeat(5) { y ->
            drawRoundRect(if (dark) ComposeColor(0xFF6A7A89) else ComposeColor(0xFFB6C7D4), topLeft = Offset(size.width * .27f, size.height * (.30f + y * .09f)), size = Size(size.width * .26f, 5f), cornerRadius = CornerRadius(3f, 3f))
        }
        drawCircle(ComposeColor(0xFFFFB39F), radius = size.width * .13f, center = Offset(size.width * .16f, size.height * .64f))
        drawCircle(ComposeColor(0xFFFFB39F), radius = size.width * .13f, center = Offset(size.width * .82f, size.height * .65f))
    }
}

@Composable
fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, label, tint = Teal, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun DocumentListCard(documents: List<Document>, model: ClearScanViewModel) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column {
            documents.forEach { DocumentRow(it, model) }
            if (documents.isEmpty()) EmptyState("No scans yet", "Tap the camera button to create your first document.")
        }
    }
}

@Composable
fun DocumentRow(document: Document, model: ClearScanViewModel) {
    Row(
        Modifier.fillMaxWidth().clickable { model.openDocument(document) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(document.thumbnailPath, Modifier.size(58.dp, 74.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(document.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(formatDate(document.createdAt), color = Muted, fontSize = 15.sp)
        }
        Text(document.type, color = if (document.type == "PDF") ComposeColor(0xFFFF6258) else ComposeColor(0xFF36B36A), modifier = Modifier.border(1.dp, if (document.type == "PDF") ComposeColor(0xFFFF6258) else ComposeColor(0xFF36B36A), RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Thumbnail(path: String, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { ImageProcessor.readBitmap(path, 256) }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), null, modifier.clip(RoundedCornerShape(5.dp)).background(Soft), contentScale = ContentScale.Crop)
    } else {
        Box(modifier.clip(RoundedCornerShape(5.dp)).background(Soft), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Description, null, tint = Muted)
        }
    }
}

@Composable
fun DocsScreen(state: UiState, model: ClearScanViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 122.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Docs⌄", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, null) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = model::setQuery,
                placeholder = { Text("Search documents") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )
        }
        items(state.documents, key = { it.id }) { doc -> DocumentRow(doc, model) }
    }
    Box(Modifier.fillMaxSize().padding(bottom = 26.dp, end = 26.dp), contentAlignment = Alignment.BottomEnd) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Teal).clickable { model.openCamera() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CameraAlt, null, tint = ComposeColor.White, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
fun CameraScreen(state: UiState, model: ClearScanViewModel) {
    val context = LocalContext.current
    val settings = state.settings
    val imageCapture = remember {
        ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) model.importBitmap(uri, context)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) Toast.makeText(context, "Camera permission is needed to scan.", Toast.LENGTH_SHORT).show()
    }
    Column(Modifier.fillMaxSize().background(ComposeColor.Black).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = model::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ComposeColor.White) }
            Icon(Icons.Default.AutoAwesome, null, tint = ComposeColor.White)
            Icon(Icons.Default.CameraAlt, null, tint = ComposeColor.White)
            Icon(Icons.Default.Settings, null, tint = ComposeColor.White)
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(ComposeColor(0xFF6E4E32)), contentAlignment = Alignment.Center) {
            if (hasCameraPermission) {
                CameraPreview(imageCapture = imageCapture, modifier = Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxWidth(.78f).aspectRatio(.72f)) {
                    drawRect(Teal, style = Stroke(width = 4f))
                    val handle = 40f
                    listOf(
                        Offset.Zero to Size(handle, handle),
                        Offset(size.width - handle, 0f) to Size(handle, handle),
                        Offset(0f, size.height - handle) to Size(handle, handle),
                        Offset(size.width - handle, size.height - handle) to Size(handle, handle),
                    ).forEach { (offset, size) -> drawRect(Teal, offset, size, style = Stroke(width = 9f)) }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DocumentOnTable()
                    Spacer(Modifier.height(16.dp))
                    Text(tr(settings, "Allow camera access to scan real documents", "允许相机权限后即可扫描真实文档"), color = ComposeColor.White, fontSize = 15.sp)
                }
            }
            if (state.captureMessage != null) {
                Text(tr(settings, "Capturing...\nPlease hold steady", "正在拍摄...\n请保持稳定"), color = ComposeColor.White, textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ComposeColor(0x99000000)).padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.fillMaxWidth().height(210.dp).background(ComposeColor.Black).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf(
                    "ID Card" to tr(settings, "ID Card", "证件"),
                    "Document" to tr(settings, "Document", "文档"),
                    "Book" to tr(settings, "Book", "书籍"),
                    "QR Code" to tr(settings, "QR Code", "二维码"),
                ).forEach {
                    Text(it.second, color = if (it.first == "Document") Teal else ComposeColor.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                CameraSmallButton(Icons.Default.PhotoLibrary) { pickImage.launch("image/*") }
                Box(Modifier.size(78.dp).clip(CircleShape).background(ComposeColor.White).border(5.dp, Teal, CircleShape).clickable {
                    if (hasCameraPermission) takeRealPhoto(context, imageCapture, model) else permission.launch(Manifest.permission.CAMERA)
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, tint = Teal, modifier = Modifier.size(34.dp))
                }
                CameraSmallButton(Icons.Default.DocumentScanner) {
                    if (hasCameraPermission) takeRealPhoto(context, imageCapture, model) else permission.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}

@Composable
fun CameraPreview(imageCapture: ImageCapture, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }.onFailure { error ->
                    AppLogger.e("Camera", "Unable to open camera", error)
                    Toast.makeText(ctx, "Unable to open camera: ${error.message ?: "camera unavailable"}", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

fun takeRealPhoto(context: Context, imageCapture: ImageCapture, model: ClearScanViewModel) {
    val outputFile = File(context.cacheDir, "clearscan-capture-${System.currentTimeMillis()}.jpg")
    outputFile.parentFile?.mkdirs()
    if (context is android.app.Activity) {
        @Suppress("DEPRECATION")
        imageCapture.targetRotation = context.windowManager.defaultDisplay.rotation
    }
    val output = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    val executor: Executor = ContextCompat.getMainExecutor(context)
    runCatching {
        imageCapture.takePicture(
            output,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    model.capturePhotoFile(outputFile)
                }

            override fun onError(exception: ImageCaptureException) {
                AppLogger.e("Camera", "ImageCapture failed", exception)
                Toast.makeText(context, "Photo failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
            },
        )
    }.onFailure { error ->
        AppLogger.e("Camera", "takePicture invocation failed", error)
        Toast.makeText(context, "Photo failed: ${error.message ?: "camera not ready"}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CameraSmallButton(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(50.dp).clip(RoundedCornerShape(11.dp)).background(ComposeColor(0xFF171717)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = ComposeColor.White, modifier = Modifier.size(27.dp))
    }
}

@Composable
fun DocumentOnTable() {
    Box(Modifier.fillMaxWidth(.78f).aspectRatio(.72f).clip(RoundedCornerShape(3.dp)).background(ComposeColor(0xFFF8F8F8)).border(3.dp, Teal)) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AGREEMENT", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            repeat(5) { index ->
                Text("${index + 1}. Terms of Agreement", Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(ComposeColor(0xFFE0E0E0)))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CropScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    val dark = settings.theme == "Dark"
    Column(Modifier.fillMaxSize().background(if (dark) ComposeColor(0xFF111317) else MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(tr(settings, "Crop", "裁剪"), onBack = model::back, action = tr(settings, "Next", "下一步"), onAction = model::applyCropAndEdit, dark = dark)
        Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            CropEditor(
                bitmap = state.processedBitmap ?: state.scanBitmap,
                points = state.cropPoints,
                onPointsChange = model::setCropPoints,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyRow(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(listOf("Original", "A4", "Letter", "Legal", "More")) { label ->
                val selected = state.cropPreset == label
                val display = when (label) {
                    "Original" -> tr(settings, "Original", "原始")
                    "Letter" -> tr(settings, "Letter", "信纸")
                    "Legal" -> tr(settings, "Legal", "法律")
                    "More" -> tr(settings, "More", "更多")
                    else -> label
                }
                Card(
                    modifier = Modifier.clickable { model.setCropPreset(label) },
                    colors = CardDefaults.cardColors(containerColor = if (selected) Teal else if (dark) ComposeColor(0xFF1D2025) else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(Modifier.size(86.dp).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Crop, null, tint = if (selected || dark) ComposeColor.White else Teal)
                        Spacer(Modifier.height(8.dp))
                        Text(display, color = if (selected || dark) ComposeColor.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Icon(Icons.Default.RotateRight, null, tint = ComposeColor.White, modifier = Modifier.size(36.dp).clickable { model.rotate() })
            Icon(Icons.Default.RotateRight, null, tint = ComposeColor.White, modifier = Modifier.size(36.dp).clickable { model.rotate() })
            Icon(Icons.Default.Tune, null, tint = ComposeColor.White, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun CropEditor(
    bitmap: Bitmap?,
    points: List<Offset>,
    onPointsChange: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bitmap == null) {
        ScanBitmap(null, modifier.aspectRatio(.72f))
        return
    }
    val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    var localPoints by remember { mutableStateOf(points) }
    var canvasSize by remember { mutableStateOf(Size(1f, 1f)) }
    var selectedHandle by remember { mutableStateOf(-1) }
    LaunchedEffect(points) {
        if (selectedHandle < 0) localPoints = points
    }
    val currentPoints by rememberUpdatedState(localPoints)
    Box(
        modifier
            .aspectRatio(aspect)
            .background(ComposeColor.White)
            .pointerInput(canvasSize) {
                detectDragGestures(
                    onDragStart = { start ->
                        val nearest = currentPoints
                            .mapIndexed { index, p ->
                                val handle = Offset(p.x * canvasSize.width, p.y * canvasSize.height)
                                index to hypot(start.x - handle.x, start.y - handle.y)
                            }
                            .minByOrNull { it.second }
                        selectedHandle = if (nearest != null && nearest.second < 180f) nearest.first else -1
                    },
                    onDragEnd = {
                        val selected = selectedHandle
                        if (selected >= 0) onPointsChange(currentPoints)
                        selectedHandle = -1
                    },
                    onDragCancel = {
                        localPoints = points
                        selectedHandle = -1
                    },
                    onDrag = { change, drag ->
                        val selected = selectedHandle
                        if (selected >= 0) {
                            change.consume()
                            val current = currentPoints[selected]
                            val next = Offset(
                                current.x + drag.x / canvasSize.width.coerceAtLeast(1f),
                                current.y + drag.y / canvasSize.height.coerceAtLeast(1f),
                            )
                            localPoints = currentPoints.toMutableList().also { it[selected] = next.coerceCropPoint() }
                        }
                    },
                )
            }
    ) {
        Box(
            Modifier.fillMaxSize()
        ) {
            Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            ) {
                val px = currentPoints.map { Offset(it.x * size.width, it.y * size.height) }
                val path = Path().apply {
                    moveTo(px[0].x, px[0].y)
                    lineTo(px[1].x, px[1].y)
                    lineTo(px[2].x, px[2].y)
                    lineTo(px[3].x, px[3].y)
                    close()
                }
                drawPath(path, Teal, style = Stroke(width = 5f))
                px.forEach {
                    drawCircle(ComposeColor.White, 25f, it)
                    drawCircle(Teal, 25f, it, style = Stroke(width = 5f))
                }
            }
        }
    }
}

@Composable
fun EditScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    var previewZoom by remember { mutableFloatStateOf(1f) }
    var previewPan by remember { mutableStateOf(Offset.Zero) }
    var toolsExpanded by remember { mutableStateOf(false) }
    var rotationTarget by remember { mutableFloatStateOf(0f) }
    var rotationKick by remember { mutableStateOf(0) }
    val animatedRotation by animateFloatAsState(rotationTarget, animationSpec = tween(260), label = "scan-rotation")
    LaunchedEffect(rotationKick) {
        if (rotationKick > 0) {
            rotationTarget = -90f
            delay(16)
            rotationTarget = 0f
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(tr(settings, "Edit", "编辑"), onBack = model::back, action = tr(settings, "Next", "下一步"), onAction = model::toSave)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        previewZoom = (previewZoom * zoomChange).coerceIn(1f, 4f)
                        previewPan += panChange
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            ScanBitmap(
                state.processedBitmap ?: state.scanBitmap,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(.72f)
                    .graphicsLayer(scaleX = previewZoom, scaleY = previewZoom, translationX = previewPan.x, translationY = previewPan.y, rotationZ = animatedRotation),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(if (toolsExpanded) 168.dp else 112.dp)
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        if (abs(drag.y) > abs(drag.x)) {
                            change.consume()
                            if (drag.y < -20f) toolsExpanded = true
                            if (drag.y > 20f) toolsExpanded = false
                        }
                    }
                },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditTool(tr(settings, "Filter", "滤镜"), Icons.Default.Palette, model::toFilter)
            EditTool(tr(settings, "Enhance", "增强"), Icons.Default.AutoAwesome, model::toAdjust)
            EditTool(tr(settings, "Crop", "裁剪"), Icons.Default.Crop, model::toCrop)
            EditTool(tr(settings, "Rotate", "旋转"), Icons.Default.RotateRight) {
                model.rotate()
                rotationKick += 1
            }
            EditTool(tr(settings, "Delete", "删除"), Icons.Default.Delete, model::deleteScan)
        }
    }
}

@Composable
fun EditTool(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun FilterScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    val source = state.scanBitmap ?: state.processedBitmap
    val mainSource = remember(source) { ImageProcessor.previewBitmap(source, 1600) }
    val thumbSource = remember(source) { ImageProcessor.previewBitmap(source, 280) }
    val filters = remember { listOf("Original", "Auto", "B&W", "Magic Color", "Gray") }
    var selectedFilter by remember { mutableStateOf("Original") }
    var mainPreview by remember(mainSource) { mutableStateOf(mainSource) }
    var thumbPreviews by remember(thumbSource) { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    LaunchedEffect(thumbSource) {
        thumbPreviews = withContext(Dispatchers.Default) {
            filters.associateWith { filter -> ImageProcessor.filter(thumbSource, filter) }
        }
    }
    LaunchedEffect(mainSource, selectedFilter) {
        mainPreview = withContext(Dispatchers.Default) { ImageProcessor.filter(mainSource, selectedFilter) }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(tr(settings, "Filter", "滤镜"), onBack = model::back, action = tr(settings, "Apply", "应用"), onAction = { model.applyFilter(selectedFilter) })
        Box(Modifier.weight(1f).fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            ScanBitmap(mainPreview ?: state.processedBitmap ?: state.scanBitmap, Modifier.fillMaxWidth().aspectRatio(.72f))
        }
        LazyRow(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(filters) { filter ->
                val selected = filter == selectedFilter
                val chipAlpha by animateFloatAsState(if (selected) 1f else 0f, animationSpec = tween(180), label = "filter-chip")
                Column(Modifier.width(92.dp).clickable { selectedFilter = filter }, horizontalAlignment = Alignment.CenterHorizontally) {
                    ScanBitmap(thumbPreviews[filter], Modifier.size(84.dp, 108.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(filter, color = if (selected) ComposeColor.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) Teal.copy(alpha = chipAlpha) else ComposeColor.Transparent).padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AdjustScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    var brightness by remember { mutableFloatStateOf(0.1f) }
    var contrast by remember { mutableFloatStateOf(1.15f) }
    var saturation by remember { mutableFloatStateOf(1.2f) }
    val source = state.processedBitmap ?: state.scanBitmap
    val preview = remember(source) { ImageProcessor.previewBitmap(source, 900) }
    var adjustedPreview by remember(preview) { mutableStateOf(preview) }
    LaunchedEffect(preview, brightness, contrast, saturation) {
        delay(45)
        adjustedPreview = withContext(Dispatchers.Default) {
            ImageProcessor.adjust(preview, brightness, contrast, saturation)
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(tr(settings, "Adjust", "调节"), onBack = model::back, action = tr(settings, "Apply", "应用"), onAction = { model.applyAdjust(brightness, contrast, saturation) })
        Box(Modifier.fillMaxWidth().height(430.dp).padding(36.dp), contentAlignment = Alignment.Center) {
            ScanBitmap(adjustedPreview, Modifier.fillMaxHeight().aspectRatio(.72f))
        }
        Adjustment(tr(settings, "Brightness", "亮度"), brightness, -0.4f..0.4f) { brightness = it }
        Adjustment(tr(settings, "Contrast", "对比度"), contrast, 0.5f..1.8f) { contrast = it }
        Adjustment(tr(settings, "Saturation", "饱和度"), saturation, 0f..2f) { saturation = it }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().height(96.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            EditTool(tr(settings, "Adjust", "调节"), Icons.Default.Brightness6) {}
            EditTool(tr(settings, "Crop", "裁剪"), Icons.Default.Crop, model::toCrop)
            EditTool(tr(settings, "Filter", "滤镜"), Icons.Default.Palette, model::toFilter)
        }
    }
}

@Composable
fun Adjustment(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(124.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SaveScreen(state: UiState, model: ClearScanViewModel) {
    var title by remember { mutableStateOf("Contract Agreement") }
    var type by remember { mutableStateOf("PDF") }
    var quality by remember { mutableStateOf("High") }
    val settings = state.settings
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(tr(settings, "Save", "保存"), onBack = model::back)
        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
            ScanBitmap(state.processedBitmap ?: state.scanBitmap, Modifier.width(150.dp).aspectRatio(.72f))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(tr(settings, "Title", "标题"), color = Muted, fontSize = 18.sp)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            SelectField(tr(settings, "File Type", "文件类型"), type, listOf("PDF", "JPG")) { type = it }
            SelectField(tr(settings, "Image Quality", "图片质量"), quality, listOf("High", "Medium", "Low")) { quality = it }
            Button(onClick = { model.saveDocument(title, type, quality) }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(12.dp))
                Text(if (state.busy) tr(settings, "Saving...", "保存中...") else tr(settings, "Save", "保存"), fontSize = 18.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr(settings, "Save to", "保存到"), color = Muted, fontSize = 17.sp)
                Text("${state.settings.defaultSavePath}  ›", color = Muted, fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun SelectField(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    displayValue: (String) -> String = { it },
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, color = Muted, fontSize = 18.sp)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (MaterialTheme.colorScheme.background == ComposeColor.White) ComposeColor(0xFFE8EAEE) else ComposeColor(0xFF2A313A), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(displayValue(value), fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("⌄", color = Muted)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(displayValue(option)) }, onClick = { onSelect(option); expanded = false }) }
            }
        }
    }
}

@Composable
fun DetailScreen(state: UiState, model: ClearScanViewModel) {
    val doc = state.selected ?: return
    val context = LocalContext.current
    var renameOpen by remember { mutableStateOf(false) }
    var passwordOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = model::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(doc.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${doc.type}    ${formatDate(doc.createdAt)}  •  ${formatSize(doc.sizeBytes)}", color = Muted, fontSize = 13.sp)
            }
            IconButton(onClick = { renameOpen = true }) { Icon(Icons.Default.Edit, null) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            DocumentPreviewPages(doc)
        }
        Row(Modifier.fillMaxWidth().height(104.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            EditTool("Share", Icons.Default.Share, model::shareSelected)
            EditTool("Edit", Icons.Default.Edit) { model.toEdit() }
            EditTool("Print", Icons.Default.Print) { printDocument(context, doc) }
            EditTool(if (state.settings.passwordMap.containsKey(doc.id)) "Locked" else "Password", Icons.Default.Lock) { passwordOpen = true }
            EditTool("Delete", Icons.Default.Delete, model::deleteSelected)
        }
    }
    if (renameOpen) RenameDialog(doc.title, onDismiss = { renameOpen = false }, onRename = { model.renameSelected(it); renameOpen = false })
    if (passwordOpen) PasswordDialog(
        hasPassword = state.settings.passwordMap.containsKey(doc.id),
        onDismiss = { passwordOpen = false },
        onSave = { model.setDocumentPassword(doc.id, it); passwordOpen = false },
    )
}

@Composable
fun DocumentPreviewPages(document: Document) {
    var pages by remember(document.id, document.exportPath, document.pageCount) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loaded by remember(document.id, document.exportPath, document.pageCount) { mutableStateOf(false) }
    LaunchedEffect(document.id, document.exportPath, document.pageCount) {
        loaded = false
        pages = withContext(Dispatchers.IO) {
            if (document.type == "PDF") {
                ImageProcessor.renderPdfPages(document.exportPath, maxPages = 24)
            } else {
                listOfNotNull(ImageProcessor.readBitmap(document.exportPath, 3072) ?: ImageProcessor.readBitmap(document.thumbnailPath, 3072))
            }
        }
        loaded = true
    }
    if (pages.isEmpty()) {
        if (loaded) {
            Thumbnail(document.thumbnailPath, Modifier.fillMaxWidth().aspectRatio(.72f))
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading preview...", color = Muted)
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        items(pages.size) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScanBitmap(pages[index], Modifier.fillMaxWidth().aspectRatio(pages[index].width.toFloat() / pages[index].height.toFloat()))
                if (pages.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Text("${index + 1} / ${pages.size}", color = Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun RenameDialog(current: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rename document") }, text = { OutlinedTextField(text, { text = it }, singleLine = true) }, confirmButton = { TextButton(onClick = { onRename(text) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun PasswordDialog(hasPassword: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPassword) "Update password" else "Set document password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This password is stored locally and protects this document inside ClearScan.", color = Muted, fontSize = 14.sp)
                OutlinedTextField(password, { password = it }, singleLine = true, placeholder = { Text("Enter password") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(password) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (hasPassword) TextButton(onClick = { onSave("") }) { Text("Remove") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun ShareScreen(state: UiState, model: ClearScanViewModel) {
    val doc = state.selected ?: return
    val context = LocalContext.current
    val settings = state.settings
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { TopTitle(tr(settings, "Share", "分享"), model::back) }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(doc.thumbnailPath, Modifier.size(96.dp, 126.dp))
                    Spacer(Modifier.width(24.dp))
                    Column {
                        Text("${doc.title}.${doc.type.lowercase()}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("${doc.type} Document • ${formatSize(doc.sizeBytes)}", color = Muted)
                        Text(formatDate(doc.createdAt), color = Muted)
                    }
                }
            }
        }
        item { Text(tr(settings, "Share to", "分享到"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ShareCard(tr(settings, "WeChat", "微信好友"), Icons.Default.Share) {
                    shareFile(context, doc, "com.tencent.mm", tr(settings, "Send to WeChat", "发送给微信好友"))
                }
                ShareCard(tr(settings, "QQ", "QQ 好友"), Icons.Default.Share) {
                    shareFile(context, doc, "com.tencent.mobileqq", tr(settings, "Send to QQ", "发送给 QQ 好友"))
                }
                ShareCard(tr(settings, "More", "更多"), Icons.Default.MoreVert) {
                    shareFile(context, doc, chooserTitle = tr(settings, "Share document", "分享文档"))
                }
            }
        }
        item { Text(tr(settings, "More options", "更多操作"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
        item {
            Column(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, ComposeColor(0xFFE8EAEE), RoundedCornerShape(14.dp))) {
                OptionRow(tr(settings, "Save to Files", "保存到文件"), Icons.Default.Folder) { saveToGallery(context, doc) }
                OptionRow(tr(settings, "Print", "打印"), Icons.Default.Print) { printDocument(context, doc) }
                OptionRow(tr(settings, "Open with another app", "用其他应用打开"), Icons.Default.FileOpen) { shareFile(context, doc) }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { BottomNav(Tab.Docs, model::selectTab, model::openCamera) }
}

@Composable
fun ShareCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(96.dp).height(112.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ComposeColor.White) }
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
fun ToolSelectScreen(state: UiState, model: ClearScanViewModel) {
    val tool = state.activeTool ?: "Tool"
    val settings = state.settings
    val required = requiredTypesFor(tool)
    val options = toolOptions(tool)
    val candidates = state.documents.filter { doc -> required.isEmpty() || doc.type.uppercase() in required }
    val enoughSelection = state.selectedToolIds.size >= minSelectionFor(tool)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(toolLabel(tool, settings), onBack = model::back, action = tr(settings, "Run", "执行"), onAction = model::executeActiveTool)
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
            Text(selectionHint(tool, settings), color = Muted, fontSize = 15.sp)
            if (options.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(options) { option ->
                        val selected = state.toolOption == option
                        Text(
                            option,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) Teal else Soft)
                                .clickable { model.setToolOption(option) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            color = if (selected) ComposeColor.White else TextDark,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (candidates.isEmpty()) {
                item { EmptyState(tr(settings, "No matching documents", "没有匹配的文档"), tr(settings, "Create or import a ${required.joinToString("/")} document first.", "请先创建或导入 ${required.joinToString("/")} 文档。")) }
            } else {
                items(candidates) { document ->
                    SelectableDocumentRow(
                        document = document,
                        selected = document.id in state.selectedToolIds,
                        onClick = { model.toggleToolDocument(document.id) },
                    )
                }
            }
        }
        Button(
            onClick = model::executeActiveTool,
            enabled = enoughSelection && !state.busy,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(if (state.busy) tr(settings, "Processing...", "处理中...") else tr(settings, "Continue", "继续"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SelectableDocumentRow(document: Document, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ComposeColor(0xFFE8FAF8) else ComposeColor.White)
            .border(1.dp, if (selected) Teal else ComposeColor(0xFFE8EAEE), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(document.thumbnailPath, Modifier.size(54.dp, 68.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(document.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text("${document.type} • ${document.pageCount} page • ${formatSize(document.sizeBytes)}", color = Muted, fontSize = 13.sp)
        }
        Checkbox(checked = selected, onCheckedChange = null)
    }
}

@Composable
fun ToolsScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 122.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tr(settings, "Tools", "工具"), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.RotateRight, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp))
            }
        }
        item { Text(tr(settings, "Popular Tools", "常用工具"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToolCard("ID Card Scan", tr(settings, "ID Card Scan", "证件扫描"), tr(settings, "Scan ID cards quickly\nand accurately", "快速准确扫描\n身份证件"), Icons.Outlined.Badge, Modifier.weight(1f), model)
                    ToolCard("PDF to Image", tr(settings, "PDF to Image", "PDF 转图片"), tr(settings, "Convert PDF pages\ninto images", "将 PDF 页面\n转换为图片"), Icons.Default.PictureAsPdf, Modifier.weight(1f), model)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToolCard("Image to PDF", tr(settings, "Image to PDF", "图片转 PDF"), tr(settings, "Convert images\ninto PDF files", "将图片转换为\nPDF 文件"), Icons.Default.Image, Modifier.weight(1f), model)
                    ToolCard("PDF Edit", tr(settings, "PDF Edit", "PDF 编辑"), tr(settings, "Edit pages in\nPDF files", "编辑 PDF\n页面"), Icons.Default.Edit, Modifier.weight(1f), model)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToolCard("Translate", tr(settings, "Translate", "翻译"), tr(settings, "Local Hy-MT2\nmulti-language MT", "本地 Hy-MT2\n多语言翻译"), Icons.Default.Language, Modifier.weight(1f), model)
                    ToolCard("Image Format Converter", tr(settings, "Image Format\nConverter", "图片格式\n转换"), tr(settings, "JPEG, PNG, WebP,\nBMP and PDF", "支持 JPEG、PNG、\nWebP、BMP、PDF"), Icons.Default.PhotoLibrary, Modifier.weight(1f), model)
                }
            }
        }
        item { Text(tr(settings, "More Tools", "更多工具"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
        item {
            Column(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, ComposeColor(0xFFE8EAEE), RoundedCornerShape(12.dp))) {
                listOf(
                    Triple("Merge PDF", tr(settings, "Merge PDF", "合并 PDF"), Icons.Default.ContentCopy),
                    Triple("Split PDF", tr(settings, "Split PDF", "拆分 PDF"), Icons.Default.FileOpen),
                    Triple("Compress PDF", tr(settings, "Compress PDF", "压缩 PDF"), Icons.Default.PictureAsPdf),
                    Triple("QR Code Scan", tr(settings, "QR Code Scan", "二维码扫描"), Icons.Default.QrCodeScanner),
                    Triple("Watermark", tr(settings, "Watermark", "添加水印"), Icons.Default.WaterDrop),
                    Triple("Add Signature", tr(settings, "Add Signature", "添加签名"), Icons.Default.Edit),
                ).forEach { item -> OptionRow(item.second, item.third) { model.runTool(item.first) } }
            }
        }
    }
}

@Composable
fun ToolCard(toolName: String, title: String, subtitle: String, icon: ImageVector, modifier: Modifier, model: ClearScanViewModel) {
    Card(modifier.height(170.dp).clickable { model.runTool(toolName) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, title, tint = Teal, modifier = Modifier.size(46.dp))
            Column {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, color = Muted, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun TranslateScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    val translation = state.translationState
    val sources = remember { model.modelSources() }
    val languages = listOf(
        "Auto", "Chinese", "English", "Japanese", "Korean", "French", "German", "Spanish",
        "Portuguese", "Italian", "Russian", "Arabic", "Thai", "Vietnamese", "Indonesian",
        "Malay", "Turkish", "Polish", "Dutch", "Czech", "Ukrainian", "Hindi",
    )
    val sourceTrack = if (settings.theme == "Dark") ComposeColor(0xFF20262D) else Soft
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TopBar(tr(settings, "Translate", "翻译"), onBack = model::back) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr(settings, "Hy-MT2 Model", "Hy-MT2 模型"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        when (translation.modelStatus) {
                            "ready" -> tr(settings, "Model is ready for local use.", "模型已就绪，可本地使用。")
                            "downloading" -> tr(settings, "Downloading model. Keep this page open.", "正在下载模型，请保持页面打开。")
                            else -> tr(settings, "Choose a source, then install or download the model.", "选择来源后安装或下载模型。")
                        },
                        color = Muted,
                        fontSize = 13.sp,
                    )
                    sources.forEach { source ->
                        val selected = translation.selectedSource == source.id
                        val selectedAlpha by animateFloatAsState(if (selected) 1f else 0f, animationSpec = tween(220), label = "model-source-bg")
                        val rowBg = if (selected) Teal.copy(alpha = if (settings.theme == "Dark") .18f else .12f * selectedAlpha) else sourceTrack.copy(alpha = if (settings.theme == "Dark") .5f else .45f)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(rowBg)
                                .border(1.dp, if (selected) Teal.copy(alpha = .35f) else ComposeColor.Transparent, RoundedCornerShape(10.dp))
                                .clickable { model.selectModelSource(source.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(source.label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            if (selected) Icon(Icons.Default.Check, null, tint = Teal)
                        }
                    }
                    ModelDownloadProgress(translation.download, settings)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = model::startModelDownload, enabled = translation.download.status != "downloading", colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                            Text(if (translation.modelStatus == "ready") tr(settings, "Verify / Redownload", "验证/重新下载") else tr(settings, "Download Model", "下载模型"))
                        }
                        OutlinedButton(onClick = model::cancelModelDownload, enabled = translation.download.status == "downloading") {
                            Text(tr(settings, "Cancel", "取消"))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectField(tr(settings, "From", "源语言"), translation.sourceLang, languages, Modifier.weight(1f), displayValue = { translationLanguageLabel(it, settings) }) { model.setTranslationLanguages(source = it) }
                IconButton(onClick = model::swapTranslationLanguages, modifier = Modifier.size(48.dp).align(Alignment.Bottom)) {
                    Icon(Icons.Default.SwapHoriz, tr(settings, "Swap languages", "交换语言"), tint = Teal)
                }
                SelectField(tr(settings, "To", "目标语言"), translation.targetLang, languages.filter { it != "Auto" }, Modifier.weight(1f), displayValue = { translationLanguageLabel(it, settings) }) { model.setTranslationLanguages(target = it) }
            }
        }
        item {
            OutlinedTextField(
                value = translation.inputText,
                onValueChange = model::setTranslationInput,
                modifier = Modifier.fillMaxWidth().height(150.dp),
                label = { Text(tr(settings, "Text to translate", "待翻译文本")) },
                placeholder = { Text(tr(settings, "Enter text here", "在此输入文本")) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = model::translateText, enabled = !translation.isTranslating, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                    Text(if (translation.isTranslating) tr(settings, "Translating...", "翻译中...") else tr(settings, "Translate", "翻译"))
                }
                OutlinedButton(onClick = model::clearTranslation, modifier = Modifier.weight(1f).height(54.dp)) {
                    Text(tr(settings, "Clear", "清空"))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr(settings, "Result", "结果"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        when {
                            translation.isTranslating -> tr(settings, "Translating locally...", "正在本地翻译...")
                            translation.outputText.isNotBlank() -> translation.outputText
                            translation.error != null -> translation.error
                            else -> tr(settings, "Translation output will appear here.", "翻译结果会显示在这里。")
                        },
                        color = if (translation.error != null) ComposeColor(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun ModelDownloadProgress(download: ModelDownloadState, settings: AppSettings) {
    val percent = (download.progress * 100).roundToInt().coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(download.progress.coerceIn(0f, 1f), animationSpec = tween(260), label = "model-progress")
    val infinite = rememberInfiniteTransition(label = "model-download")
    val indeterminateOffset by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(950, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "model-indeterminate",
    )
    val isUnknownTotal = download.status == "downloading" && download.totalBytes <= 0L
    val trackColor = if (settings.theme == "Dark") ComposeColor(0xFF2A313A) else Soft
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(trackColor)) {
            if (isUnknownTotal) {
                Box(
                    Modifier
                        .fillMaxWidth(.36f)
                        .fillMaxHeight()
                        .offset(x = (indeterminateOffset * 260).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Teal)
                )
            } else {
                Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(Teal))
            }
        }
        Text(
            when (download.status) {
                "downloading" -> if (download.totalBytes > 0L) {
                    tr(settings, "$percent% • ${formatSize(download.downloadedBytes)} / ${formatSize(download.totalBytes)} • ${formatSize(download.speedBytesPerSec)}/s • ETA ${download.etaSeconds}s", "$percent% • ${formatSize(download.downloadedBytes)} / ${formatSize(download.totalBytes)} • ${formatSize(download.speedBytesPerSec)}/秒 • 剩余 ${download.etaSeconds} 秒")
                } else {
                    tr(settings, "Downloading • ${formatSize(download.downloadedBytes)} • ${formatSize(download.speedBytesPerSec)}/s", "下载中 • 已下载 ${formatSize(download.downloadedBytes)} • ${formatSize(download.speedBytesPerSec)}/秒")
                }
                "ready" -> tr(settings, "Model ready • ${formatSize(download.totalBytes)}", "模型已就绪 • ${formatSize(download.totalBytes)}")
                "paused" -> tr(settings, "Download paused • ${formatSize(download.downloadedBytes)} saved, tap Download Model to resume", "下载已暂停 • 已保留 ${formatSize(download.downloadedBytes)}，点击下载模型继续")
                "error" -> download.error ?: tr(settings, "Download failed", "下载失败")
                else -> tr(settings, "Model not downloaded", "模型未下载")
            },
            color = Muted,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun MeScreen(state: UiState, model: ClearScanViewModel) {
    SettingsScreen(state, model, embedded = true)
}

@Composable
fun SettingsScreen(state: UiState, model: ClearScanViewModel, embedded: Boolean = false) {
    val settings = state.settings
    var activeDialog by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = if (embedded) 24.dp else 12.dp, bottom = 122.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopTitle(tr(settings, "Settings", "设置"), if (embedded) null else model::back) }
        item { SettingRow(label = tr(settings, "My Account", "我的账号"), icon = Icons.Default.AccountCircle, value = if (settings.loggedIn) settings.accountName else tr(settings, "Sign in", "登录"), onClick = model::openAccount) }
        item { SettingRow(label = tr(settings, "Language", "语言"), icon = Icons.Default.Language, value = settings.language, onClick = { activeDialog = "language" }) }
        item { SettingRow(label = tr(settings, "Theme", "主题"), icon = Icons.Default.Brightness6, value = tr(settings, settings.theme, if (settings.theme == "Light") "日间" else "夜间"), onClick = { activeDialog = "theme" }) }
        item { SettingRow(label = tr(settings, "Default Save Path", "默认保存路径"), icon = Icons.Default.Folder, value = settings.defaultSavePath, onClick = { activeDialog = "path" }) }
        item { SettingRow(label = tr(settings, "Password Lock", "文件密码锁"), icon = Icons.Default.Lock, value = tr(settings, "${settings.passwordMap.size} protected", "已保护 ${settings.passwordMap.size} 个文件"), onClick = { activeDialog = "password" }) }
        item { SettingRow(label = tr(settings, "App Logs", "运行日志"), icon = Icons.Default.Description, value = tr(settings, "View", "查看"), onClick = model::openLogs) }
        item { SettingRow(label = tr(settings, "Help & Feedback", "帮助与反馈"), icon = Icons.Default.Info, onClick = model::openHelp) }
        item { SettingRow(label = tr(settings, "About ClearScan", "关于 ClearScan"), icon = Icons.Default.Info, value = "v1.0.0", onClick = model::openAbout) }
        item {
            OutlinedButton(onClick = model::logout, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = ComposeColor(0xFFE53935)), shape = RoundedCornerShape(9.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(10.dp))
                Text(tr(settings, "Log Out", "退出登录"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (activeDialog == "language") ChoiceDialog(
        title = tr(settings, "Language", "语言"),
        options = listOf("English", "中文"),
        selected = settings.language,
        onDismiss = { activeDialog = null },
        onSelect = { model.updateSettings(settings.copy(language = it)); activeDialog = null },
    )
    if (activeDialog == "theme") ChoiceDialog(
        title = tr(settings, "Theme", "主题"),
        options = listOf("Light", "Dark"),
        selected = settings.theme,
        onDismiss = { activeDialog = null },
        onSelect = { model.updateSettings(settings.copy(theme = it)); activeDialog = null },
    )
    if (activeDialog == "path") ChoiceDialog(
        title = tr(settings, "Default Save Path", "默认保存路径"),
        options = listOf("Internal Storage", "Documents"),
        selected = settings.defaultSavePath,
        onDismiss = { activeDialog = null },
        onSelect = { model.updateSettings(settings.copy(defaultSavePath = it)); activeDialog = null },
    )
    if (activeDialog == "password") PasswordManagerDialog(state, model, onDismiss = { activeDialog = null })
}

@Composable
fun ChoiceDialog(title: String, options: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (option == selected) Soft.copy(alpha = if (MaterialTheme.colorScheme.background == ComposeColor.White) 1f else .16f) else ComposeColor.Transparent).clickable { onSelect(option) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option, Modifier.weight(1f), fontSize = 17.sp, fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                        if (option == selected) Icon(Icons.Default.Check, null, tint = Teal)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun PasswordManagerDialog(state: UiState, model: ClearScanViewModel, onDismiss: () -> Unit) {
    var target by remember { mutableStateOf<Document?>(null) }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(state.settings, "Password Lock", "文件密码锁")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr(state.settings, "Choose a document, then set or remove its local password.", "选择一个文件，然后设置或移除本地密码。"), color = Muted, fontSize = 14.sp)
                state.documents.take(6).forEach { document ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (target?.id == document.id) Soft else ComposeColor.Transparent).clickable { target = document; password = state.settings.passwordMap[document.id].orEmpty() }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (state.settings.passwordMap.containsKey(document.id)) Icons.Default.Lock else Icons.Default.Description, null, tint = Teal)
                        Spacer(Modifier.width(10.dp))
                        Text(document.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), enabled = target != null, singleLine = true, placeholder = { Text(tr(state.settings, "Password", "密码")) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                target?.let { model.setDocumentPassword(it.id, password) }
                onDismiss()
            }, enabled = target != null) { Text(tr(state.settings, "Save", "保存")) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    target?.let { model.setDocumentPassword(it.id, "") }
                    onDismiss()
                }, enabled = target != null) { Text(tr(state.settings, "Remove", "移除")) }
                TextButton(onClick = onDismiss) { Text(tr(state.settings, "Cancel", "取消")) }
            }
        },
    )
}

@Composable
fun AccountScreen(state: UiState, model: ClearScanViewModel) {
    var name by remember { mutableStateOf(state.settings.accountName.takeIf { it != "Guest" } ?: "") }
    var email by remember { mutableStateOf(state.settings.accountEmail) }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { TopTitle("My Account", model::back) }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(ComposeColor(0xFFE8FAF8)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountCircle, null, tint = Teal, modifier = Modifier.size(72.dp))
                }
            }
        }
        item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true) }
        item { OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true) }
        item {
            Button(onClick = { model.login(name, email) }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal), shape = RoundedCornerShape(10.dp)) {
                Text(if (state.settings.loggedIn) "Update Account" else "Sign In", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (state.settings.loggedIn) {
            item {
                OutlinedButton(onClick = model::logout, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = ComposeColor(0xFFE53935))) {
                    Text("Log Out")
                }
            }
        }
    }
}

@Composable
fun HelpScreen(model: ClearScanViewModel) {
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TopTitle("Help", model::back) }
        item { HelpCard("How do I scan a document?", "Tap Scan, align the page inside the frame, capture, drag the four crop corners, then tap Next and Save.") }
        item { HelpCard("Why can I drag the crop corners?", "Each corner can be moved across the whole image. Pinch to zoom, pan the image, then drag the nearest corner to the document edge.") }
        item { HelpCard("How do PDF tools work?", "Merge, split, compress, watermark and signature tools always ask you to select files first. The output is saved as a new document.") }
        item { HelpCard("How do passwords work?", "Open a document, tap Password, and set a local password for that file. ClearScan stores it only on this device.") }
        item { HelpCard("Feedback", "For issues, include your phone model, Android version, and the action you were taking. This local demo keeps feedback inside the app.") }
    }
}

@Composable
fun HelpCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(body, color = Muted, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
fun AboutScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(horizontal = 24.dp)) {
        TopTitle(tr(settings, "About", "关于"), model::back)
        Spacer(Modifier.height(80.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(156.dp).clip(RoundedCornerShape(34.dp)).background(ComposeColor(0xFFE8FAF8)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DocumentScanner, null, tint = Teal, modifier = Modifier.size(92.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("ClearScan", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text("Version 1.0.0", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted, fontSize = 18.sp)
        Spacer(Modifier.height(36.dp))
        Text(tr(settings, "Scan Everything, Clearly", "清晰扫描一切"), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TealDark, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(tr(settings, "ClearScan helps you scan, save and\nmanage your documents easily.", "ClearScan 帮你轻松扫描、保存\n和管理所有文档。"), Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, color = Muted, fontSize = 18.sp, lineHeight = 27.sp)
        Spacer(Modifier.height(58.dp))
        OptionRow(tr(settings, "Privacy Policy", "隐私政策"), Icons.Default.Lock) { model.openLegal("Privacy Policy") }
        OptionRow(tr(settings, "Terms of Use", "使用条款"), Icons.Default.Description) { model.openLegal("Terms of Use") }
        Spacer(Modifier.weight(1f))
        Text(tr(settings, "© 2024 ClearScan. All rights reserved.", "© 2024 ClearScan。保留所有权利。"), Modifier.fillMaxWidth().padding(bottom = 34.dp), textAlign = TextAlign.Center, color = Muted)
    }
}

@Composable
fun LegalScreen(state: UiState, model: ClearScanViewModel) {
    val privacy = state.legalTitle == "Privacy Policy"
    val settings = state.settings
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TopTitle(if (privacy) tr(settings, "Privacy Policy", "隐私政策") else tr(settings, "Terms of Use", "使用条款"), model::back) }
        item {
            Text(
                if (privacy) {
                    tr(settings, "ClearScan stores scans, passwords, account profile data, and settings locally on this device. Files are not uploaded to a server in this clean local edition. Camera and media permissions are used only for scanning, importing, exporting, sharing, and printing documents. You can delete documents from the Docs page and clear local account state with Log Out.", "ClearScan 会将扫描文件、密码、账号资料和设置保存在本设备本地。纯净本地版不会把文件上传到服务器。相机和媒体权限仅用于扫描、导入、导出、分享和打印文档。你可以在文档页删除文件，也可以通过退出登录清除本地账号状态。")
                } else {
                    tr(settings, "ClearScan is provided as a local document scanning tool. You are responsible for the content you scan, export, share, or print. PDF tools create new local files and do not modify originals unless you delete them. Password protection is local to this app and should not be treated as enterprise encryption. By using the app, you agree to use it lawfully and keep backups of important documents.", "ClearScan 是一个本地文档扫描工具。你需要对自己扫描、导出、分享或打印的内容负责。PDF 工具会生成新的本地文件，不会覆盖原文件，除非你主动删除。文件密码保护仅在本应用内本地生效，不应视为企业级加密。使用本应用即表示你同意合法使用，并自行备份重要文档。")
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 25.sp,
            )
        }
    }
}

@Composable
fun AppLogsScreen(state: UiState, model: ClearScanViewModel) {
    val settings = state.settings
    val context = LocalContext.current
    LaunchedEffect(Unit) { model.refreshLogs() }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TopBar(tr(settings, "App Logs", "运行日志"), onBack = model::back, action = tr(settings, "Refresh", "刷新"), onAction = model::refreshLogs) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { copyLogsToClipboard(context, state.logText) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(tr(settings, "Copy", "复制"))
                }
                OutlinedButton(onClick = { shareLogFile(context) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(tr(settings, "Share", "分享"))
                }
                OutlinedButton(onClick = model::clearLogs, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = ComposeColor(0xFFE53935))) {
                    Text(tr(settings, "Clear", "清空"))
                }
            }
        }
        item {
            Text(
                tr(settings, "Path: ${AppLogger.file(context).absolutePath}", "路径：${AppLogger.file(context).absolutePath}"),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = if (settings.theme == "Dark") ComposeColor(0xFF111820) else ComposeColor(0xFFF8FAFC)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Text(
                    state.logText.ifBlank { tr(settings, "No logs yet.", "暂无日志。") },
                    Modifier.fillMaxWidth().padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, action: String? = null, onAction: (() -> Unit)? = null, dark: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = if (dark) ComposeColor.White else MaterialTheme.colorScheme.onSurface) }
        Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (dark) ComposeColor.White else MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = { onAction?.invoke() }, enabled = action != null) { Text(action ?: "", color = Teal, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TopTitle(title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) } else Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun OptionRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).clickable(onClick = onClick).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = Teal, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(22.dp))
        Text(label, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Default.ArrowForwardIos, null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingRow(label: String, icon: ImageVector, value: String? = null, onClick: () -> Unit = {}, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, ComposeColor(0xFFECEFF2), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = Teal, modifier = Modifier.size(31.dp))
        Spacer(Modifier.width(22.dp))
        Text(label, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        if (trailing != null) trailing() else {
            if (value != null) Text(value, color = Muted, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForwardIos, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.DocumentScanner, null, tint = Muted, modifier = Modifier.size(48.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(body, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun ScanBitmap(bitmap: Bitmap?, modifier: Modifier = Modifier) {
    if (bitmap != null) {
        val displayBitmap = remember(bitmap) { ImageProcessor.previewBitmap(bitmap, 3072) ?: bitmap }
        Image(displayBitmap.asImageBitmap(), null, modifier.clip(RoundedCornerShape(4.dp)).background(ComposeColor.White), contentScale = ContentScale.Fit)
    } else {
        Box(modifier.clip(RoundedCornerShape(4.dp)).background(ComposeColor.White), contentAlignment = Alignment.Center) {
            DocumentOnTable()
        }
    }
}

@VisibleForTesting
fun detectTranslationLanguage(input: String): String {
    return if (input.any { it.code in 0x3400..0x9FFF }) "Chinese" else "English"
}

private val translationLanguageChineseNames = mapOf(
    "Auto" to "自动检测",
    "Chinese" to "中文",
    "English" to "英语",
    "Japanese" to "日语",
    "Korean" to "韩语",
    "French" to "法语",
    "German" to "德语",
    "Spanish" to "西班牙语",
    "Portuguese" to "葡萄牙语",
    "Italian" to "意大利语",
    "Russian" to "俄语",
    "Arabic" to "阿拉伯语",
    "Thai" to "泰语",
    "Vietnamese" to "越南语",
    "Indonesian" to "印度尼西亚语",
    "Malay" to "马来语",
    "Turkish" to "土耳其语",
    "Polish" to "波兰语",
    "Dutch" to "荷兰语",
    "Czech" to "捷克语",
    "Ukrainian" to "乌克兰语",
    "Hindi" to "印地语",
)

fun translationLanguageLabel(language: String, settings: AppSettings): String {
    return if (settings.language == "中文") translationLanguageChineseNames[language] ?: language else language
}

@VisibleForTesting
fun splitTranslationText(input: String, maxChars: Int = 800): List<String> {
    val remaining = StringBuilder(input.trim())
    if (remaining.isEmpty()) return emptyList()
    val chunks = mutableListOf<String>()
    val boundaries = charArrayOf('\n', '。', '！', '？', '.', '!', '?', ';', '；', ' ')
    while (remaining.isNotEmpty()) {
        if (remaining.length <= maxChars) {
            remaining.toString().trim().takeIf { it.isNotEmpty() }?.let(chunks::add)
            break
        }
        val candidate = remaining.substring(0, maxChars)
        val boundary = candidate.lastIndexOfAny(boundaries)
        val end = if (boundary >= maxChars / 2) boundary + 1 else maxChars
        candidate.substring(0, end).trim().takeIf { it.isNotEmpty() }?.let(chunks::add)
        remaining.delete(0, end)
        while (remaining.isNotEmpty() && remaining.first().isWhitespace()) remaining.deleteCharAt(0)
    }
    return chunks
}

@VisibleForTesting
fun buildHyMt2Prompt(input: String, sourceLang: String, targetLang: String): String {
    val source = if (sourceLang == "Auto") detectTranslationLanguage(input) else sourceLang
    return if (source == "Chinese" || targetLang == "Chinese") {
        val target = translationLanguageChineseNames[targetLang] ?: targetLang
        "将以下文本翻译为${target}，注意只需要输出翻译后的结果，不要额外解释：\n\n$input"
    } else {
        "Translate the following segment into $targetLang, without additional explanation.\n\n$input"
    }
}

object ImageProcessor {
    fun sampleDocumentBitmap(title: String = "AGREEMENT"): Bitmap {
        val bitmap = Bitmap.createBitmap(900, 1250, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(20, 24, 32)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 46f
        paint.isFakeBoldText = true
        canvas.drawText(title.uppercase(), 450f, 120f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 27f
        repeat(5) { i ->
            val top = 210f + i * 150f
            paint.isFakeBoldText = true
            canvas.drawText("${i + 1}. Terms of Agreement", 110f, top, paint)
            paint.isFakeBoldText = false
            paint.textSize = 23f
            canvas.drawText("This agreement shall commence on the effective date and continue", 110f, top + 42f, paint)
            canvas.drawText("for a period of twelve months unless terminated earlier.", 110f, top + 78f, paint)
            paint.textSize = 27f
        }
        paint.strokeWidth = 3f
        canvas.drawLine(190f, 1030f, 530f, 1030f, paint)
        canvas.drawLine(145f, 1110f, 490f, 1110f, paint)
        paint.textSize = 25f
        canvas.drawText("Signature:", 110f, 1022f, paint)
        canvas.drawText("Date:", 110f, 1102f, paint)
        return bitmap
    }

    fun readBitmap(path: String): Bitmap? = runCatching { BitmapFactoryCompat.decode(path) }.getOrNull()

    fun readBitmap(path: String, maxDimension: Int): Bitmap? = runCatching {
        BitmapFactoryCompat.decodeSampled(path, maxDimension)
    }.onFailure { AppLogger.e("Image", "Failed to decode sampled bitmap: $path", it) }.getOrNull()

    fun renderPdfFirstPage(path: String): Bitmap? = renderPdfPages(path, maxPages = 1).firstOrNull()

    fun renderPdfPages(path: String, maxPages: Int = Int.MAX_VALUE): List<Bitmap> = runCatching {
        val file = File(path)
        if (!file.exists()) return@runCatching emptyList()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            android.graphics.pdf.PdfRenderer(descriptor).use { renderer ->
                val pages = mutableListOf<Bitmap>()
                val count = minOf(renderer.pageCount, maxPages)
                for (index in 0 until count) {
                    renderer.openPage(index).use { page ->
                        val scale = 2
                        val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        val canvas = AndroidCanvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages += bitmap
                    }
                }
                pages
            }
        }
    }.getOrDefault(emptyList())

    fun documentPages(document: Document): List<Bitmap> {
        return if (document.type == "PDF") {
            renderPdfPages(document.exportPath)
        } else {
            listOfNotNull(readBitmap(document.exportPath) ?: readBitmap(document.thumbnailPath))
        }
    }

    fun downsampleForPdf(bitmap: Bitmap, level: String): Bitmap {
        val maxWidth = when (level) {
            "Low" -> 820
            "High" -> 1600
            else -> 1200
        }
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
        val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    fun decodeCameraBitmap(path: String, maxDimension: Int = 3072): Bitmap? = runCatching {
        val orientation = ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val bitmap = BitmapFactoryCompat.decodeSampled(path, maxDimension) ?: return@runCatching null
        applyExifOrientation(bitmap, orientation)
    }.onFailure { AppLogger.e("Image", "Failed to decode camera bitmap: $path", it) }.getOrNull()

    fun decodeUriBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = bitmapSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val bitmap = resolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null
        applyExifOrientation(bitmap, orientation)
    }.onFailure { AppLogger.e("Image", "Failed to decode imported bitmap: $uri", it) }.getOrNull()

    fun optimizeCapturedPhoto(
        input: File,
        output: File,
        maxDimension: Int = 3072,
        targetBytes: Long = 4_500_000L,
    ): File? = runCatching {
        val decoded = decodeCameraBitmap(input.absolutePath, maxDimension) ?: return@runCatching null
        var encoded = decoded
        for (quality in listOf(88, 82, 76)) {
            writeJpeg(encoded, output, quality)
            if (output.length() <= targetBytes) break
        }
        if (output.length() > targetBytes && maxOf(decoded.width, decoded.height) > 2400) {
            encoded = previewBitmap(decoded, 2400) ?: decoded
            writeJpeg(encoded, output, 80)
        }
        if (!output.exists() || output.length() <= 0L) return@runCatching null
        val originalBytes = input.length()
        if (input.absolutePath != output.absolutePath) input.delete()
        AppLogger.i(
            "Camera",
            "Capture optimized ${decoded.width}x${decoded.height}, $originalBytes -> ${output.length()} bytes",
        )
        output
    }.onFailure { AppLogger.e("Camera", "Capture compression failed", it) }.getOrNull()

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }

    fun writeJpeg(bitmap: Bitmap, file: File, quality: String) {
        val percent = when (quality) { "Low" -> 55; "Medium" -> 74; else -> 88 }
        writeJpeg(bitmap, file, percent)
    }

    fun writeJpeg(bitmap: Bitmap, file: File, quality: Int) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 95), out)) { "JPEG encoding failed" }
        }
    }

    fun writePng(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Suppress("DEPRECATION")
    fun writeWebp(bitmap: Bitmap, file: File, quality: Int = 88) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            val format = if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            check(bitmap.compress(format, quality.coerceIn(40, 95), out)) { "WebP encoding failed" }
        }
    }

    fun writeBmp(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        val width = bitmap.width
        val height = bitmap.height
        val rowSize = ((width * 3 + 3) / 4) * 4
        val pixelBytes = rowSize * height
        FileOutputStream(file).buffered().use { out ->
            fun writeLe16(value: Int) {
                out.write(value and 0xFF)
                out.write((value ushr 8) and 0xFF)
            }
            fun writeLe32(value: Int) {
                out.write(value and 0xFF)
                out.write((value ushr 8) and 0xFF)
                out.write((value ushr 16) and 0xFF)
                out.write((value ushr 24) and 0xFF)
            }
            out.write('B'.code)
            out.write('M'.code)
            writeLe32(54 + pixelBytes)
            writeLe32(0)
            writeLe32(54)
            writeLe32(40)
            writeLe32(width)
            writeLe32(height)
            writeLe16(1)
            writeLe16(24)
            writeLe32(0)
            writeLe32(pixelBytes)
            writeLe32(2835)
            writeLe32(2835)
            writeLe32(0)
            writeLe32(0)
            val pixels = IntArray(width)
            val row = ByteArray(rowSize)
            for (y in height - 1 downTo 0) {
                bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
                var offset = 0
                for (color in pixels) {
                    row[offset++] = Color.blue(color).toByte()
                    row[offset++] = Color.green(color).toByte()
                    row[offset++] = Color.red(color).toByte()
                }
                while (offset < rowSize) row[offset++] = 0
                out.write(row)
            }
        }
    }

    fun writePdf(bitmap: Bitmap, file: File) {
        writePdf(listOf(bitmap), file)
    }

    fun writePdf(bitmaps: List<Bitmap>, file: File) {
        file.parentFile?.mkdirs()
        val pdf = PdfDocument()
        bitmaps.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = pdf.startPage(pageInfo)
            val rect = RectF(32f, 32f, 563f, 810f)
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG))
            pdf.finishPage(page)
        }
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
    }

    fun rotate(bitmap: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(bitmap.height, bitmap.width, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        canvas.rotate(90f, out.width / 2f, out.height / 2f)
        canvas.drawBitmap(bitmap, (out.width - bitmap.width) / 2f, (out.height - bitmap.height) / 2f, Paint(Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun perspectiveCrop(bitmap: Bitmap, normalizedPoints: List<Offset>): Bitmap {
        if (normalizedPoints.size < 4) return bitmap
        val src = normalizedPoints.map {
            Offset(
                it.x.coerceIn(0f, 1f) * bitmap.width,
                it.y.coerceIn(0f, 1f) * bitmap.height,
            )
        }
        val top = distance(src[0], src[1])
        val right = distance(src[1], src[2])
        val bottom = distance(src[2], src[3])
        val left = distance(src[3], src[0])
        val width = max(32f, max(top, bottom)).roundToInt()
        val height = max(32f, max(left, right)).roundToInt()
        val matrix = Matrix()
        val srcArray = floatArrayOf(
            src[0].x, src[0].y,
            src[1].x, src[1].y,
            src[2].x, src[2].y,
            src[3].x, src[3].y,
        )
        val dstArray = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat(),
        )
        matrix.setPolyToPoly(srcArray, 0, dstArray, 0, 4)
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return out
    }

    fun enhanceDocument(bitmap: Bitmap): Bitmap {
        val minLongSide = 1800
        val longSide = max(bitmap.width, bitmap.height)
        val scaled = if (longSide < minLongSide) {
            val scale = minLongSide.toFloat() / longSide.toFloat()
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
        } else {
            bitmap
        }
        val balanced = grayWorldWhiteBalance(scaled)
        val adjusted = adjust(balanced, .04f, 1.18f, 1.0f) ?: balanced
        return sharpen(adjusted, amount = 1.25f)
    }

    private fun grayWorldWhiteBalance(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val step = max(1, minOf(width, height) / 360)
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val color = bitmap.getPixel(x, y)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val maxChannel = maxOf(r, g, b)
                val minChannel = minOf(r, g, b)
                if (maxChannel - minChannel < 80 && maxChannel > 48) {
                    rSum += r
                    gSum += g
                    bSum += b
                    count++
                }
                x += step
            }
            y += step
        }
        if (count == 0L) return bitmap
        val rAvg = rSum.toFloat() / count
        val gAvg = gSum.toFloat() / count
        val bAvg = bSum.toFloat() / count
        val gray = (rAvg + gAvg + bAvg) / 3f
        val rScale = (gray / rAvg).coerceIn(.82f, 1.18f)
        val gScale = (gray / gAvg).coerceIn(.82f, 1.18f)
        val bScale = (gray / bAvg).coerceIn(.82f, 1.18f)
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        val matrix = ColorMatrix(floatArrayOf(
            rScale, 0f, 0f, 0f, 0f,
            0f, gScale, 0f, 0f, 0f,
            0f, 0f, bScale, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return out
    }

    fun previewBitmap(bitmap: Bitmap?, maxLongSide: Int): Bitmap? {
        if (bitmap == null) return null
        val longSide = max(bitmap.width, bitmap.height)
        if (longSide <= maxLongSide) return bitmap
        val scale = maxLongSide.toFloat() / longSide.toFloat()
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt().coerceAtLeast(1), (bitmap.height * scale).roundToInt().coerceAtLeast(1), true)
    }

    private fun sharpen(bitmap: Bitmap, amount: Float = 1f): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val result = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        fun clamp(value: Int) = value.coerceIn(0, 255)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val left = pixels[y * width + x - 1]
                val right = pixels[y * width + x + 1]
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val blurR = (Color.red(left) + Color.red(right) + Color.red(top) + Color.red(bottom)) / 4
                val blurG = (Color.green(left) + Color.green(right) + Color.green(top) + Color.green(bottom)) / 4
                val blurB = (Color.blue(left) + Color.blue(right) + Color.blue(top) + Color.blue(bottom)) / 4
                val r = clamp((Color.red(center) + (Color.red(center) - blurR) * amount).roundToInt())
                val g = clamp((Color.green(center) + (Color.green(center) - blurG) * amount).roundToInt())
                val b = clamp((Color.blue(center) + (Color.blue(center) - blurB) * amount).roundToInt())
                result[y * width + x] = Color.rgb(r, g, b)
            }
        }
        for (x in 0 until width) {
            result[x] = pixels[x]
            result[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            result[y * width] = pixels[y * width]
            result[y * width + width - 1] = pixels[y * width + width - 1]
        }
        out.setPixels(result, 0, width, 0, 0, width, height)
        return out
    }

    fun detectDocumentCorners(bitmap: Bitmap): List<Offset> {
        val step = max(2, minOf(bitmap.width, bitmap.height) / 220)
        var sum = 0L
        var samples = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                sum += ((Color.red(color) * 0.299f) + (Color.green(color) * 0.587f) + (Color.blue(color) * 0.114f)).roundToInt()
                samples++
                x += step
            }
            y += step
        }
        val average = if (samples == 0) 128 else (sum / samples).toInt()
        val threshold = max(150, average + 18)
        var left = bitmap.width
        var top = bitmap.height
        var right = 0
        var bottom = 0
        y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                val luminance = ((Color.red(color) * 0.299f) + (Color.green(color) * 0.587f) + (Color.blue(color) * 0.114f)).roundToInt()
                val colorSpread = maxOf(Color.red(color), Color.green(color), Color.blue(color)) - minOf(Color.red(color), Color.green(color), Color.blue(color))
                if (luminance >= threshold && colorSpread < 86) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
                x += step
            }
            y += step
        }
        val detectedWidth = right - left
        val detectedHeight = bottom - top
        if (detectedWidth < bitmap.width * .28f || detectedHeight < bitmap.height * .28f) return defaultCropPoints()
        val padX = detectedWidth * .025f
        val padY = detectedHeight * .025f
        return listOf(
            Offset(((left - padX) / bitmap.width).coerceIn(0f, 1f), ((top - padY) / bitmap.height).coerceIn(0f, 1f)),
            Offset(((right + padX) / bitmap.width).coerceIn(0f, 1f), ((top - padY) / bitmap.height).coerceIn(0f, 1f)),
            Offset(((right + padX) / bitmap.width).coerceIn(0f, 1f), ((bottom + padY) / bitmap.height).coerceIn(0f, 1f)),
            Offset(((left - padX) / bitmap.width).coerceIn(0f, 1f), ((bottom + padY) / bitmap.height).coerceIn(0f, 1f)),
        )
    }

    @VisibleForTesting
    fun perspectiveOutputSize(bitmapWidth: Int, bitmapHeight: Int, normalizedPoints: List<Offset>): Pair<Int, Int> {
        if (normalizedPoints.size < 4) return bitmapWidth to bitmapHeight
        val src = normalizedPoints.map { Offset(it.x * bitmapWidth, it.y * bitmapHeight) }
        val top = distance(src[0], src[1])
        val right = distance(src[1], src[2])
        val bottom = distance(src[2], src[3])
        val left = distance(src[3], src[0])
        return max(32f, max(top, bottom)).roundToInt() to max(32f, max(left, right)).roundToInt()
    }

    private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

    fun filter(bitmap: Bitmap?, filter: String): Bitmap? {
        if (bitmap == null) return null
        return when (filter) {
            "B&W" -> blackAndWhite(bitmap)
            "Gray" -> adjust(grayWorldWhiteBalance(bitmap), 0f, 1.08f, 0f)
            "Magic Color" -> adjust(grayWorldWhiteBalance(bitmap), .04f, 1.22f, 1.22f)
            "Auto" -> enhanceDocument(bitmap)
            else -> bitmap
        }
    }

    private fun blackAndWhite(bitmap: Bitmap): Bitmap {
        val balanced = grayWorldWhiteBalance(bitmap)
        val out = Bitmap.createBitmap(balanced.width, balanced.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(balanced.width * balanced.height)
        balanced.getPixels(pixels, 0, balanced.width, 0, 0, balanced.width, balanced.height)
        var sum = 0L
        pixels.forEach { color ->
            sum += ((Color.red(color) * 0.299f) + (Color.green(color) * 0.587f) + (Color.blue(color) * 0.114f)).roundToInt()
        }
        val threshold = ((sum / pixels.size).toInt() - 8).coerceIn(96, 190)
        pixels.indices.forEach { index ->
            val color = pixels[index]
            val lum = ((Color.red(color) * 0.299f) + (Color.green(color) * 0.587f) + (Color.blue(color) * 0.114f)).roundToInt()
            val value = if (lum > threshold) 255 else 24
            pixels[index] = Color.rgb(value, value, value)
        }
        out.setPixels(pixels, 0, balanced.width, 0, 0, balanced.width, balanced.height)
        return sharpen(out, .6f)
    }

    fun adjust(bitmap: Bitmap?, brightness: Float, contrast: Float, saturation: Float): Bitmap? {
        if (bitmap == null) return null
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        val matrix = ColorMatrix()
        matrix.setSaturation(saturation)
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f + brightness) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(scale, 0f, 0f, 0f, translate, 0f, scale, 0f, 0f, translate, 0f, 0f, scale, 0f, translate, 0f, 0f, 0f, 1f, 0f))
        matrix.postConcat(contrastMatrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return out
    }

    fun watermark(bitmap: Bitmap, text: String): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(72, 15, 167, 160)
            textSize = (bitmap.width * .08f).coerceAtLeast(44f)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.rotate(-28f, bitmap.width / 2f, bitmap.height / 2f)
        canvas.drawText(text, bitmap.width / 2f, bitmap.height / 2f, paint)
        return out
    }

    fun addSignature(bitmap: Bitmap): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 24, 32)
            strokeWidth = (bitmap.width * .004f).coerceAtLeast(4f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val y = bitmap.height * .86f
        val left = bitmap.width * .52f
        val right = bitmap.width * .84f
        canvas.drawLine(left, y, right, y, paint)
        paint.style = Paint.Style.FILL
        paint.textSize = (bitmap.width * .035f).coerceAtLeast(28f)
        canvas.drawText("Signed", left, y - 18f, paint)
        return out
    }

    fun scanQr(bitmap: Bitmap): String? {
        val scanner = BarcodeScanning.getClient()
        return runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            val barcodes = Tasks.await(scanner.process(image))
            barcodes.firstOrNull()?.rawValue
        }.getOrNull()
    }
}

object BitmapFactoryCompat {
    fun decode(path: String): Bitmap? = android.graphics.BitmapFactory.decodeFile(path)

    fun decodeSampled(path: String, maxDimension: Int): Bitmap? {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = bitmapSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return android.graphics.BitmapFactory.decodeFile(path, options)
    }
}

@VisibleForTesting
fun bitmapSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    return ceil(maxOf(width, height).toDouble() / maxDimension.toDouble()).toInt().coerceAtLeast(1)
}

@VisibleForTesting
fun formatSize(size: Long): String {
    if (size <= 0) return "0 KB"
    val kb = size / 1024.0
    return if (kb < 1024) "${kb.roundToInt()} KB" else "${(kb / 1024.0 * 10).roundToInt() / 10.0} MB"
}

fun formatDate(time: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(time))
}

fun shareFile(
    context: Context,
    doc: Document,
    targetPackage: String? = null,
    chooserTitle: String = "Share ${doc.title}",
) {
    val file = File(doc.exportPath)
    if (!file.exists()) {
        Toast.makeText(context, "File is no longer available", Toast.LENGTH_SHORT).show()
        AppLogger.w("Share", "Missing file for document ${doc.id}: ${doc.exportPath}")
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeTypeFor(doc.type)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, doc.title)
        putExtra(Intent.EXTRA_SUBJECT, doc.title)
        clipData = ClipData.newUri(context.contentResolver, doc.title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (targetPackage != null) setPackage(targetPackage)
    }
    runCatching {
        if (targetPackage != null) {
            context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
        AppLogger.i("Share", "Share document ${doc.id} target=${targetPackage ?: "system"} mime=${intent.type}")
    }.onFailure { error ->
        AppLogger.e("Share", "Unable to share to ${targetPackage ?: "system"}", error)
        if (targetPackage != null) {
            Toast.makeText(context, "Target app is unavailable. Opening system share.", Toast.LENGTH_SHORT).show()
            shareFile(context, doc, targetPackage = null, chooserTitle = chooserTitle)
        } else {
            Toast.makeText(context, "No compatible sharing app found", Toast.LENGTH_SHORT).show()
        }
    }
}

fun saveToGallery(context: Context, doc: Document) {
    val file = File(doc.exportPath)
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeFor(doc.type))
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ClearScan")
    }
    val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
    if (uri != null) {
        context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        Toast.makeText(context, "Saved to Files", Toast.LENGTH_SHORT).show()
    }
}

fun printDocument(context: Context, doc: Document) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val webView = WebView(context)
    webView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    val file = File(doc.exportPath)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            printManager.print(doc.title, view.createPrintDocumentAdapter(doc.title), PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, "<html><body><h2>${doc.title}</h2><p>${file.name}</p><p>ClearScan document ready for printing.</p></body></html>", "text/html", "utf-8", null)
}

fun mimeTypeFor(type: String): String = when (type.uppercase()) {
    "PDF" -> "application/pdf"
    "PNG" -> "image/png"
    "WEBP" -> "image/webp"
    "BMP" -> "image/bmp"
    else -> "image/jpeg"
}

fun copyLogsToClipboard(context: Context, logs: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ClearScan Logs", logs))
    AppLogger.i("Log", "Logs copied to clipboard")
    Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
}

fun shareLogFile(context: Context) {
    val file = AppLogger.file(context)
    if (!file.exists()) file.writeText("")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, AppLogger.read().takeLast(12_000))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    AppLogger.i("Log", "Share log file")
    context.startActivity(Intent.createChooser(intent, "Share ClearScan logs"))
}
