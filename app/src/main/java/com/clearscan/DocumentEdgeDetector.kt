package com.clearscan

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Offset
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class DocumentDetectionStatus {
    Idle,
    Detecting,
    Detected,
    LowConfidence,
    Failed,
}

enum class PageDetectionProfile { Document, Worksheet, Book, IdCard }

fun detectionProfileFor(mode: ScanMode): PageDetectionProfile = when (mode) {
    ScanMode.Book -> PageDetectionProfile.Document
    ScanMode.IdCard -> PageDetectionProfile.IdCard
    else -> PageDetectionProfile.Document
}

private object OpenCvRuntime {
    val loaded: Boolean by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { OpenCVLoader.initLocal() }
}

data class DocumentDetectionResult(
    val corners: List<Offset>,
    val confidence: Float,
    val status: DocumentDetectionStatus,
    val processingMs: Long,
    val candidateCount: Int = 0,
    val reason: String? = null,
)

object DocumentEdgeDetector {
    private const val ANALYSIS_LONG_SIDE = 1280.0
    private const val MIN_AREA_RATIO = 0.10
    private const val MIN_CONFIDENCE = 0.54f

    fun detectPreview(bitmap: Bitmap, profile: PageDetectionProfile = PageDetectionProfile.Document): DocumentDetectionResult {
        val started = System.currentTimeMillis()
        if (bitmap.width < 64 || bitmap.height < 64 || !OpenCvRuntime.loaded) return failed(started, "Preview is unavailable")
        val source = Mat()
        val scaled = Mat()
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        val adaptive = Mat()
        val combined = Mat()
        val closed = Mat()
        val hierarchy = Mat()
        val contours = mutableListOf<MatOfPoint>()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        return try {
            Utils.bitmapToMat(bitmap, source)
            val scale = min(1.0, 640.0 / max(source.cols(), source.rows()).toDouble())
            Imgproc.resize(source, scaled, Size(source.cols() * scale, source.rows() * scale), 0.0, 0.0, Imgproc.INTER_AREA)
            Imgproc.cvtColor(scaled, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val median = averageLuminance(blurred)
            Imgproc.Canny(blurred, edges, max(20.0, median * .48), min(235.0, median * 1.45), 3, true)
            Imgproc.adaptiveThreshold(blurred, adaptive, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 7.0)
            Core.bitwise_or(edges, adaptive, combined)
            Imgproc.morphologyEx(combined, closed, Imgproc.MORPH_CLOSE, kernel, Point(-1.0, -1.0), 2)
            Imgproc.findContours(closed, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val candidates = contours
                .sortedByDescending { abs(Imgproc.contourArea(it)) }
                .take(48)
                .mapNotNull { candidateFrom(it, edges, scaled.cols(), scaled.rows(), profile) }
            val best = candidates.maxByOrNull { it.score }
            if (best == null) failed(started, "No preview quadrilateral") else {
                val corners = best.points.map {
                    Offset((it.x / scaled.cols()).toFloat().coerceIn(0f, 1f), (it.y / scaled.rows()).toFloat().coerceIn(0f, 1f))
                }
                DocumentDetectionResult(corners, best.score, if (best.score >= .42f) DocumentDetectionStatus.Detected else DocumentDetectionStatus.LowConfidence, elapsed(started), candidates.size)
            }
        } catch (error: Throwable) {
            failed(started, error.message ?: error.javaClass.simpleName)
        } finally {
            contours.forEach(MatOfPoint::release)
            listOf(source, scaled, gray, blurred, edges, adaptive, combined, closed, hierarchy, kernel).forEach(Mat::release)
        }
    }

    fun detect(bitmap: Bitmap, profile: PageDetectionProfile = PageDetectionProfile.Document): DocumentDetectionResult {
        val started = System.currentTimeMillis()
        if (bitmap.width < 64 || bitmap.height < 64) {
            return failed(started, "Image is too small")
        }
        if (!OpenCvRuntime.loaded) {
            return failed(started, "OpenCV initialization failed")
        }

        val source = Mat()
        val scaled = Mat()
        val rgb = Mat()
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        val closed = Mat()
        val hierarchy = Mat()
        val equalized = Mat()
        val adaptive = Mat()
        val gradX = Mat()
        val gradY = Mat()
        val absX = Mat()
        val absY = Mat()
        val gradient = Mat()
        val channels = mutableListOf<Mat>()
        val contours = mutableListOf<MatOfPoint>()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
        return try {
            Utils.bitmapToMat(bitmap, source)
            val scale = min(1.0, ANALYSIS_LONG_SIDE / max(source.cols(), source.rows()).toDouble())
            if (scale < 1.0) {
                Imgproc.resize(source, scaled, Size(source.cols() * scale, source.rows() * scale), 0.0, 0.0, Imgproc.INTER_AREA)
            } else {
                source.copyTo(scaled)
            }
            Imgproc.cvtColor(scaled, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY)
            val clahe = Imgproc.createCLAHE(2.2, Size(8.0, 8.0))
            clahe.apply(gray, equalized)
            clahe.collectGarbage()
            Core.split(rgb, channels)

            val analysisPlanes = listOf(gray, equalized) + channels
            val candidates = mutableListOf<QuadCandidate>()
            fun collectCandidates(mask: Mat) {
                Imgproc.morphologyEx(mask, closed, Imgproc.MORPH_CLOSE, kernel, Point(-1.0, -1.0), 2)
                contours.clear()
                Imgproc.findContours(closed, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
                contours.sortedByDescending { abs(Imgproc.contourArea(it)) }.take(80).forEach { contour ->
                    candidateFrom(contour, closed, scaled.cols(), scaled.rows(), profile)?.let(candidates::add)
                }
                contours.forEach(MatOfPoint::release)
                contours.clear()
            }
            analysisPlanes.forEach { plane ->
                Imgproc.GaussianBlur(plane, blurred, Size(5.0, 5.0), 0.0)
                val luminance = averageLuminance(blurred)
                val low = max(18.0, luminance * 0.55)
                val high = min(245.0, max(low + 30.0, luminance * 1.35))
                Imgproc.Canny(blurred, edges, low, high, 3, true)
                collectCandidates(edges)
            }
            Imgproc.adaptiveThreshold(equalized, adaptive, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 9.0)
            collectCandidates(adaptive)
            Core.bitwise_not(adaptive, adaptive)
            collectCandidates(adaptive)
            Imgproc.Scharr(equalized, gradX, CvType.CV_16S, 1, 0)
            Imgproc.Scharr(equalized, gradY, CvType.CV_16S, 0, 1)
            Core.convertScaleAbs(gradX, absX)
            Core.convertScaleAbs(gradY, absY)
            Core.addWeighted(absX, .5, absY, .5, 0.0, gradient)
            Imgproc.threshold(gradient, gradient, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
            collectCandidates(gradient)

            val best = candidates.maxByOrNull { it.score }
            if (best == null) {
                DocumentDetectionResult(emptyList(), 0f, DocumentDetectionStatus.Failed, elapsed(started), 0, "No document quadrilateral found")
            } else {
                val normalized = refineCorners(equalized, best.points).map {
                    Offset(
                        (it.x / scaled.cols()).toFloat().coerceIn(0f, 1f),
                        (it.y / scaled.rows()).toFloat().coerceIn(0f, 1f),
                    )
                }
                val status = if (best.score >= MIN_CONFIDENCE) DocumentDetectionStatus.Detected else DocumentDetectionStatus.LowConfidence
                DocumentDetectionResult(
                    corners = normalized,
                    confidence = best.score,
                    status = status,
                    processingMs = elapsed(started),
                    candidateCount = candidates.size,
                    reason = if (status == DocumentDetectionStatus.LowConfidence) "Document boundary confidence is low" else null,
                )
            }
        } catch (error: Throwable) {
            DocumentDetectionResult(emptyList(), 0f, DocumentDetectionStatus.Failed, elapsed(started), 0, error.message ?: error.javaClass.simpleName)
        } finally {
            contours.forEach(MatOfPoint::release)
            channels.forEach(Mat::release)
            listOf(source, scaled, rgb, gray, blurred, edges, closed, hierarchy, equalized, adaptive, gradX, gradY, absX, absY, gradient, kernel).forEach(Mat::release)
        }
    }

    private fun candidateFrom(contour: MatOfPoint, edgeMask: Mat, width: Int, height: Int, profile: PageDetectionProfile): QuadCandidate? {
        val area = abs(Imgproc.contourArea(contour))
        val imageArea = width.toDouble() * height.toDouble()
        val areaRatio = area / imageArea
        if (areaRatio < MIN_AREA_RATIO || areaRatio > 0.985) return null

        val curve = MatOfPoint2f(*contour.toArray())
        return try {
            val perimeter = Imgproc.arcLength(curve, true)
            listOf(.012, .017, .022, .030, .040).mapNotNull { epsilon ->
                val polygon = MatOfPoint2f()
                try {
                    Imgproc.approxPolyDP(curve, polygon, perimeter * epsilon, true)
                    scoreQuad(polygon.toArray().toList(), area, imageArea, edgeMask, width, height, profile)
                } finally {
                    polygon.release()
                }
            }.maxByOrNull { it.score }
        } finally {
            curve.release()
        }
    }

    private fun scoreQuad(points: List<Point>, contourArea: Double, imageArea: Double, edgeMask: Mat, width: Int, height: Int, profile: PageDetectionProfile): QuadCandidate? {
        if (points.size != 4) return null
        val integerPolygon = MatOfPoint(*points.toTypedArray())
        val convex = try { Imgproc.isContourConvex(integerPolygon) } finally { integerPolygon.release() }
        if (!convex) return null
        val ordered = orderCorners(points)
        val quadArea = boundingQuadArea(ordered)
        val areaRatio = quadArea / imageArea
        if (areaRatio < MIN_AREA_RATIO || areaRatio > .985) return null
        val angleScore = rightAngleScore(ordered)
        if (angleScore < .30) return null
        val rectangularity = (contourArea / quadArea).coerceIn(0.0, 1.0)
        val centerX = ordered.sumOf { it.x } / 4.0
        val centerY = ordered.sumOf { it.y } / 4.0
        val centerDistance = hypot(centerX / width - .5, centerY / height - .5) / .707
        val centerScore = (1.0 - centerDistance).coerceIn(0.0, 1.0)
        val borderScore = ordered.map { point ->
            min(min(point.x / width, 1.0 - point.x / width), min(point.y / height, 1.0 - point.y / height))
        }.average().let { (it / .06).coerceIn(0.0, 1.0) }
        val areaScore = ((areaRatio - MIN_AREA_RATIO) / (.72 - MIN_AREA_RATIO)).coerceIn(0.0, 1.0)
        val edgeScore = edgeSupport(edgeMask, ordered)
        val topWidth = hypot(ordered[1].x - ordered[0].x, ordered[1].y - ordered[0].y)
        val bottomWidth = hypot(ordered[2].x - ordered[3].x, ordered[2].y - ordered[3].y)
        val leftHeight = hypot(ordered[3].x - ordered[0].x, ordered[3].y - ordered[0].y)
        val rightHeight = hypot(ordered[2].x - ordered[1].x, ordered[2].y - ordered[1].y)
        val ratio = max(topWidth, bottomWidth) / max(1.0, max(leftHeight, rightHeight))
        val profileScore = when (profile) {
            PageDetectionProfile.IdCard -> (1.0 - abs(ratio - 1.586) / 1.2).coerceIn(0.0, 1.0)
            PageDetectionProfile.Book -> (1.0 - abs(ratio - .72) / 1.4).coerceIn(0.0, 1.0)
            PageDetectionProfile.Worksheet -> (1.0 - abs(ratio - .707) / 1.2).coerceIn(0.0, 1.0)
            PageDetectionProfile.Document -> (1.0 - min(abs(ratio - .707), abs(ratio - 1.414)) / 1.8).coerceIn(.55, 1.0)
        }
        val score = (areaScore * .29 + angleScore * .18 + rectangularity * .10 + centerScore * .09 + borderScore * .03 + edgeScore * .22 + profileScore * .09).toFloat()
        return QuadCandidate(ordered, score.coerceIn(0f, 1f))
    }

    private fun averageLuminance(gray: Mat): Double = Core.mean(gray).`val`[0].coerceIn(1.0, 254.0)

    private fun refineCorners(gray: Mat, points: List<Point>): List<Point> {
        val corners = MatOfPoint2f(*points.toTypedArray())
        return try {
            Imgproc.cornerSubPix(gray, corners, Size(7.0, 7.0), Size(-1.0, -1.0), TermCriteria(TermCriteria.EPS or TermCriteria.MAX_ITER, 30, .05))
            orderCorners(corners.toArray().toList())
        } catch (_: Throwable) {
            points
        } finally {
            corners.release()
        }
    }

    private fun edgeSupport(mask: Mat, points: List<Point>): Double {
        var hits = 0
        var total = 0
        points.indices.forEach { index ->
            val from = points[index]
            val to = points[(index + 1) % points.size]
            repeat(40) { step ->
                val t = step / 39.0
                val x = (from.x + (to.x - from.x) * t).toInt().coerceIn(1, mask.cols() - 2)
                val y = (from.y + (to.y - from.y) * t).toInt().coerceIn(1, mask.rows() - 2)
                total++
                var found = false
                for (dy in -1..1) for (dx in -1..1) {
                    if (mask.get(y + dy, x + dx)?.firstOrNull()?.let { it > 0 } == true) found = true
                }
                if (found) hits++
            }
        }
        return if (total == 0) 0.0 else hits.toDouble() / total
    }

    @VisibleForTesting
    fun orderNormalizedCorners(points: List<Offset>): List<Offset> {
        require(points.size == 4)
        return orderCorners(points.map { Point(it.x.toDouble(), it.y.toDouble()) }).map { Offset(it.x.toFloat(), it.y.toFloat()) }
    }

    private fun orderCorners(points: List<Point>): List<Point> {
        require(points.size == 4)
        val topLeft = points.minBy { it.x + it.y }
        val bottomRight = points.maxBy { it.x + it.y }
        val topRight = points.maxBy { it.x - it.y }
        val bottomLeft = points.minBy { it.x - it.y }
        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    private fun rightAngleScore(points: List<Point>): Double = points.indices.map { index ->
        val previous = points[(index + 3) % 4]
        val current = points[index]
        val next = points[(index + 1) % 4]
        val ax = previous.x - current.x
        val ay = previous.y - current.y
        val bx = next.x - current.x
        val by = next.y - current.y
        val denominator = hypot(ax, ay) * hypot(bx, by)
        if (denominator < 1.0) 0.0 else {
            val angle = Math.toDegrees(acos(((ax * bx + ay * by) / denominator).coerceIn(-1.0, 1.0)))
            (1.0 - abs(angle - 90.0) / 70.0).coerceIn(0.0, 1.0)
        }
    }.average()

    private fun boundingQuadArea(points: List<Point>): Double {
        var sum = 0.0
        for (index in points.indices) {
            val next = points[(index + 1) % points.size]
            sum += points[index].x * next.y - next.x * points[index].y
        }
        return max(1.0, abs(sum) / 2.0)
    }

    private fun failed(started: Long, reason: String) = DocumentDetectionResult(
        corners = emptyList(),
        confidence = 0f,
        status = DocumentDetectionStatus.Failed,
        processingMs = elapsed(started),
        reason = reason,
    )

    private fun elapsed(started: Long) = System.currentTimeMillis() - started

    private data class QuadCandidate(val points: List<Point>, val score: Float)
}

object DocumentPerspectiveCorrector {
    private const val MAX_OUTPUT_LONG_SIDE = 4096

    fun crop(bitmap: Bitmap, normalizedCorners: List<Offset>): Bitmap {
        require(normalizedCorners.size == 4) { "Four crop corners are required" }
        check(OpenCvRuntime.loaded) { "OpenCV initialization failed" }
        val ordered = DocumentEdgeDetector.orderNormalizedCorners(normalizedCorners)
        val points = ordered.map {
            Point(
                it.x.coerceIn(0f, 1f).toDouble() * (bitmap.width - 1),
                it.y.coerceIn(0f, 1f).toDouble() * (bitmap.height - 1),
            )
        }
        val measuredWidth = max(distance(points[0], points[1]), distance(points[3], points[2]))
        val measuredHeight = max(distance(points[0], points[3]), distance(points[1], points[2]))
        require(measuredWidth >= 32.0 && measuredHeight >= 32.0) { "Crop area is too small" }
        val outputScale = min(1.0, MAX_OUTPUT_LONG_SIDE / max(measuredWidth, measuredHeight))
        val outputWidth = (measuredWidth * outputScale).toInt().coerceAtLeast(32)
        val outputHeight = (measuredHeight * outputScale).toInt().coerceAtLeast(32)

        val source = Mat()
        val output = Mat(outputHeight, outputWidth, CvType.CV_8UC4)
        val sourcePoints = MatOfPoint2f(*points.toTypedArray())
        val destinationPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point((outputWidth - 1).toDouble(), 0.0),
            Point((outputWidth - 1).toDouble(), (outputHeight - 1).toDouble()),
            Point(0.0, (outputHeight - 1).toDouble()),
        )
        val transform = Imgproc.getPerspectiveTransform(sourcePoints, destinationPoints)
        return try {
            Utils.bitmapToMat(bitmap, source)
            Imgproc.warpPerspective(
                source,
                output,
                transform,
                Size(outputWidth.toDouble(), outputHeight.toDouble()),
                Imgproc.INTER_CUBIC,
                Core.BORDER_CONSTANT,
                Scalar(255.0, 255.0, 255.0, 255.0),
            )
            Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888).also {
                Utils.matToBitmap(output, it)
            }
        } finally {
            source.release()
            output.release()
            sourcePoints.release()
            destinationPoints.release()
            transform.release()
        }
    }

    private fun distance(first: Point, second: Point): Double = hypot(first.x - second.x, first.y - second.y)
}

object BookPageSplitter {
    fun split(bitmap: Bitmap): List<Bitmap> {
        if (bitmap.width < bitmap.height * .82f) return listOf(bitmap)
        val start = (bitmap.width * .34f).toInt()
        val end = (bitmap.width * .66f).toInt()
        val stepY = max(1, bitmap.height / 240)
        var bestX = bitmap.width / 2
        var bestScore = Double.MAX_VALUE
        for (x in start..end step max(1, bitmap.width / 400)) {
            var luminance = 0.0
            var edge = 0.0
            var count = 0
            for (y in 0 until bitmap.height step stepY) {
                val color = bitmap.getPixel(x, y)
                val left = bitmap.getPixel((x - 3).coerceAtLeast(0), y)
                fun gray(value: Int) = Color.red(value) * .299 + Color.green(value) * .587 + Color.blue(value) * .114
                val current = gray(color)
                luminance += current
                edge += abs(current - gray(left))
                count++
            }
            val average = luminance / count.coerceAtLeast(1)
            val edgeAverage = edge / count.coerceAtLeast(1)
            val centerPenalty = abs(x - bitmap.width / 2.0) / bitmap.width * 24.0
            val score = average * .65 - edgeAverage * .35 + centerPenalty
            if (score < bestScore) { bestScore = score; bestX = x }
        }
        if (bestX < bitmap.width * .28f || bestX > bitmap.width * .72f) bestX = bitmap.width / 2
        return listOf(
            Bitmap.createBitmap(bitmap, 0, 0, bestX, bitmap.height),
            Bitmap.createBitmap(bitmap, bestX, 0, bitmap.width - bestX, bitmap.height),
        )
    }
}
