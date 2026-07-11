package com.clearscan

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class OverlayTransform(
    val centerX: Float = .5f,
    val centerY: Float = .5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

data class WatermarkOptions(
    val text: String,
    val sizeFraction: Float,
    val opacity: Float,
    val transform: OverlayTransform,
    val applyAllPages: Boolean,
)

data class SignatureOptions(
    val bitmap: Bitmap,
    val transform: OverlayTransform,
    val applyAllPages: Boolean,
)

@Composable
fun WatermarkEditorScreen(state: UiState, model: ClearScanViewModel) {
    val document = state.selected ?: return
    val settings = state.settings
    var preview by remember(document.id) { mutableStateOf<Bitmap?>(null) }
    var text by remember { mutableStateOf("ClearScan") }
    var size by remember { mutableFloatStateOf(.08f) }
    var opacity by remember { mutableFloatStateOf(.35f) }
    var transform by remember { mutableStateOf(OverlayTransform()) }
    var applyAll by remember { mutableStateOf(false) }
    LaunchedEffect(document.id) { preview = ImageProcessor.documentPages(document).firstOrNull() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(tr(settings, "Watermark", "添加水印"), model::back, tr(settings, "Apply", "应用"), {
            model.applyWatermark(document, WatermarkOptions(text, size, opacity, transform, applyAll))
        })
        OverlayPreview(preview, transform, { transform = it }, Modifier.weight(1f)) {
            Text(text.ifBlank { "ClearScan" }, color = ComposeColor(15, 167, 160).copy(alpha = opacity), fontSize = (size * 180).coerceIn(14f, 48f).sp)
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(text, { text = it.take(80) }, Modifier.fillMaxWidth(), label = { Text(tr(settings, "Watermark text", "水印文字")) }, singleLine = true)
            Text(tr(settings, "Text size", "字号")); Slider(size, { size = it }, valueRange = .02f.. .18f)
            Text(tr(settings, "Opacity", "透明度")); Slider(opacity, { opacity = it }, valueRange = .1f..1f)
            Text(tr(settings, "Angle ${transform.rotation.roundToInt()}°", "角度 ${transform.rotation.roundToInt()}°")); Slider(transform.rotation, { transform = transform.copy(rotation = it) }, valueRange = -180f..180f)
            ApplyAllRow(settings, applyAll) { applyAll = it }
        }
    }
}

@Composable
fun SignatureEditorScreen(state: UiState, model: ClearScanViewModel) {
    val document = state.selected ?: return
    val settings = state.settings
    val context = LocalContext.current
    var preview by remember(document.id) { mutableStateOf<Bitmap?>(null) }
    var signature by remember { mutableStateOf<Bitmap?>(null) }
    var transform by remember { mutableStateOf(OverlayTransform(centerY = .78f, scale = .7f)) }
    var applyAll by remember { mutableStateOf(false) }
    var drawingOpen by remember { mutableStateOf(false) }
    val importSignature = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) signature = ImageProcessor.decodeUriBitmap(context, uri, 1600)?.let(OverlayRenderer::removeNearWhiteBackground)
    }
    LaunchedEffect(document.id) { preview = ImageProcessor.documentPages(document).firstOrNull() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(tr(settings, "Add Signature", "添加签名"), model::back, tr(settings, "Apply", "应用"), {
            signature?.let { model.applySignature(document, SignatureOptions(it, transform, applyAll)) }
        })
        OverlayPreview(preview, transform, { transform = it }, Modifier.weight(1f)) {
            signature?.let { Image(it.asImageBitmap(), null, Modifier.size(180.dp, 72.dp), contentScale = ContentScale.Fit) }
        }
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ importSignature.launch("image/*") }, Modifier.weight(1f)) { Text(tr(settings, "Import", "导入签名")) }
            OutlinedButton({ drawingOpen = true }, Modifier.weight(1f)) { Text(tr(settings, "Draw", "手写签名")) }
        }
        ApplyAllRow(settings, applyAll, Modifier.padding(horizontal = 18.dp)) { applyAll = it }
    }
    if (drawingOpen) SignaturePadDialog(settings, onDismiss = { drawingOpen = false }) {
        signature = it
        drawingOpen = false
    }
}

@Composable
private fun OverlayPreview(bitmap: Bitmap?, transform: OverlayTransform, onTransform: (OverlayTransform) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var size by remember { mutableStateOf(Size.Zero) }
    val currentTransform by rememberUpdatedState(transform)
    val displaySize = remember(size, bitmap?.width, bitmap?.height) {
        if (bitmap == null || size.width <= 0f || size.height <= 0f) size else {
            val imageRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
            val viewRatio = size.width / size.height.coerceAtLeast(1f)
            if (imageRatio > viewRatio) Size(size.width, size.width / imageRatio) else Size(size.height * imageRatio, size.height)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .background(ComposeColor(0xFF23272D))
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(size, bitmap?.width, bitmap?.height) {
                detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotation ->
                    val current = currentTransform
                    onTransform(
                        current.copy(
                            centerX = (current.centerX + pan.x / displaySize.width.coerceAtLeast(1f)).coerceIn(-.05f, 1.05f),
                            centerY = (current.centerY + pan.y / displaySize.height.coerceAtLeast(1f)).coerceIn(-.05f, 1.05f),
                            scale = (current.scale * zoom).coerceIn(.15f, 5f),
                            rotation = normalizeAngle(current.rotation + rotation),
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        Box(
            Modifier
                .offset {
                    IntOffset(
                        ((transform.centerX - .5f) * displaySize.width).roundToInt(),
                        ((transform.centerY - .5f) * displaySize.height).roundToInt(),
                    )
                }
                .graphicsLayer(scaleX = transform.scale, scaleY = transform.scale, rotationZ = transform.rotation)
        ) { content() }
    }
}

private fun normalizeAngle(angle: Float): Float = ((angle + 180f) % 360f + 360f) % 360f - 180f

@Composable
private fun ApplyAllRow(settings: AppSettings, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = onChange)
        Text(tr(settings, "Apply to all pages", "应用到全部页面"))
    }
}

@Composable
private fun SignaturePadDialog(settings: AppSettings, onDismiss: () -> Unit, onDone: (Bitmap) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val current = remember { mutableStateListOf<Offset>() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(settings, "Draw signature", "手写签名")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(
                    Modifier.fillMaxWidth().height(220.dp).background(ComposeColor.White, RoundedCornerShape(6.dp)).pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { current.clear(); current.add(it) },
                            onDragEnd = { if (current.size > 1) strokes.add(current.toList()); current.clear() },
                            onDragCancel = { current.clear() },
                            onDrag = { change, _ -> change.consume(); current.add(change.position) },
                        )
                    }
                ) {
                    (strokes.toList() + listOf(current.toList())).forEach { stroke ->
                        if (stroke.size > 1) {
                            val path = Path().apply { moveTo(stroke.first().x, stroke.first().y); stroke.drop(1).forEach { lineTo(it.x, it.y) } }
                            drawPath(path, ComposeColor.Black, style = Stroke(width = 5f))
                        }
                    }
                }
                Row {
                    IconButton({ if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) { Icon(Icons.Default.Undo, null) }
                    IconButton({ strokes.clear(); current.clear() }) { Icon(Icons.Default.Clear, null) }
                }
            }
        },
        confirmButton = { Button(onClick = { if (strokes.isNotEmpty()) onDone(OverlayRenderer.signatureBitmap(strokes.toList(), 1000, 400)) }) { Text(tr(settings, "Use signature", "使用签名")) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(tr(settings, "Cancel", "取消")) } },
    )
}

object OverlayRenderer {
    fun watermark(bitmap: Bitmap, options: WatermarkOptions): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((options.opacity * 255).roundToInt(), 15, 167, 160)
            textSize = bitmap.width * options.sizeFraction * options.transform.scale
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(options.transform.rotation, bitmap.width * options.transform.centerX, bitmap.height * options.transform.centerY)
        canvas.drawText(options.text.ifBlank { "ClearScan" }, bitmap.width * options.transform.centerX, bitmap.height * options.transform.centerY, paint)
        canvas.restore()
        return out
    }

    fun signature(bitmap: Bitmap, options: SignatureOptions): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(out)
        val width = bitmap.width * .34f * options.transform.scale
        val height = width * options.bitmap.height / options.bitmap.width
        canvas.save()
        canvas.rotate(options.transform.rotation, bitmap.width * options.transform.centerX, bitmap.height * options.transform.centerY)
        val left = bitmap.width * options.transform.centerX - width / 2f
        val top = bitmap.height * options.transform.centerY - height / 2f
        canvas.drawBitmap(options.bitmap, null, android.graphics.RectF(left, top, left + width, top + height), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        return out
    }

    fun removeNearWhiteBackground(bitmap: Bitmap): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(out.width * out.height)
        out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        pixels.indices.forEach { index ->
            val color = pixels[index]
            val min = minOf(Color.red(color), Color.green(color), Color.blue(color))
            if (min > 235) pixels[index] = Color.TRANSPARENT
        }
        out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        return out
    }

    fun signatureBitmap(strokes: List<List<Offset>>, width: Int, height: Int): Bitmap {
        val all = strokes.flatten()
        val minX = all.minOf { it.x }; val maxX = all.maxOf { it.x }
        val minY = all.minOf { it.y }; val maxY = all.maxOf { it.y }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val scale = minOf((width - 40f) / (maxX - minX).coerceAtLeast(1f), (height - 40f) / (maxY - minY).coerceAtLeast(1f))
        strokes.forEach { stroke ->
            val path = AndroidPath()
            stroke.forEachIndexed { index, point ->
                val x = 20f + (point.x - minX) * scale; val y = 20f + (point.y - minY) * scale
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
        }
        return out
    }
}
