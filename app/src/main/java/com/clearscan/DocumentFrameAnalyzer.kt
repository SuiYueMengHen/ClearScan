package com.clearscan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class LiveDocumentFrame(
    val corners: List<Offset>,
    val confidence: Float,
    val imageAspectRatio: Float,
    val stable: Boolean,
)

/** Lightweight, throttled analysis for the camera guide. High-resolution detection still runs after capture. */
class DocumentFrameAnalyzer(
    private val profile: PageDetectionProfile,
    private val onFrame: (LiveDocumentFrame?) -> Unit,
) : ImageAnalysis.Analyzer {
    private var lastAnalysisAt = 0L
    private var lastAcceptedAt = 0L
    private var smoothedCorners: List<Offset> = emptyList()

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalysisAt < ANALYSIS_INTERVAL_MS) {
            image.close()
            return
        }
        lastAnalysisAt = now
        var bitmap: Bitmap? = null
        var oriented: Bitmap? = null
        try {
            bitmap = image.toBitmap()
            oriented = rotate(bitmap, image.imageInfo.rotationDegrees)
            val result = DocumentEdgeDetector.detectPreview(oriented, profile)
            if (result.corners.size == 4 && result.confidence >= PREVIEW_CONFIDENCE) {
                val movement = if (smoothedCorners.size == 4) meanDistance(smoothedCorners, result.corners) else 1f
                smoothedCorners = if (smoothedCorners.size == 4) {
                    smoothedCorners.zip(result.corners) { old, new ->
                        Offset(
                            old.x * SMOOTHING + new.x * (1f - SMOOTHING),
                            old.y * SMOOTHING + new.y * (1f - SMOOTHING),
                        )
                    }
                } else {
                    result.corners
                }
                lastAcceptedAt = now
                onFrame(
                    LiveDocumentFrame(
                        corners = smoothedCorners,
                        confidence = result.confidence,
                        imageAspectRatio = oriented.width.toFloat() / oriented.height.coerceAtLeast(1),
                        stable = movement < STABLE_DISTANCE,
                    )
                )
            } else if (now - lastAcceptedAt > LOST_FRAME_GRACE_MS) {
                smoothedCorners = emptyList()
                onFrame(null)
            }
        } catch (error: Throwable) {
            AppLogger.w("LiveDetect", "Preview analysis failed: ${error.message ?: error.javaClass.simpleName}")
            if (now - lastAcceptedAt > LOST_FRAME_GRACE_MS) onFrame(null)
        } finally {
            if (oriented !== bitmap) oriented?.recycle()
            bitmap?.recycle()
            image.close()
        }
    }

    private fun rotate(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun meanDistance(first: List<Offset>, second: List<Offset>): Float =
        first.zip(second).map { (a, b) -> hypot(a.x - b.x, a.y - b.y) }.average().toFloat()

    companion object {
        private const val ANALYSIS_INTERVAL_MS = 420L
        private const val LOST_FRAME_GRACE_MS = 900L
        private const val PREVIEW_CONFIDENCE = .42f
        private const val SMOOTHING = .62f
        private const val STABLE_DISTANCE = .018f
    }
}
