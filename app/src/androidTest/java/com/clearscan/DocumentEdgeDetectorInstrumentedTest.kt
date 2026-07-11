package com.clearscan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.hypot

@RunWith(AndroidJUnit4::class)
class DocumentEdgeDetectorInstrumentedTest {
    @Test
    fun detectsPerspectivePaperOnDarkTable() {
        val expected = listOf(
            Offset(.18f, .12f),
            Offset(.84f, .18f),
            Offset(.76f, .88f),
            Offset(.12f, .80f),
        )
        val bitmap = fixtureBitmap(expected)

        val result = DocumentEdgeDetector.detect(bitmap)

        assertEquals(DocumentDetectionStatus.Detected, result.status)
        assertEquals(4, result.corners.size)
        result.corners.zip(expected).forEach { (actual, target) ->
            assertTrue("Detected corner $actual is too far from $target", hypot(actual.x - target.x, actual.y - target.y) < .07f)
        }
    }

    @Test
    fun previewDetectorFindsPaperAtCameraResolution() {
        val expected = listOf(Offset(.14f, .10f), Offset(.88f, .15f), Offset(.82f, .91f), Offset(.10f, .84f))
        val source = fixtureBitmap(expected)
        val preview = Bitmap.createScaledBitmap(source, 480, 640, true)

        val result = DocumentEdgeDetector.detectPreview(preview)

        assertTrue(result.status == DocumentDetectionStatus.Detected || result.status == DocumentDetectionStatus.LowConfidence)
        assertEquals(4, result.corners.size)
        assertTrue(result.processingMs < 1500L)
    }

    @Test
    fun allDocumentFiltersKeepPreviewDimensions() {
        val source = fixtureBitmap(listOf(Offset(.1f, .1f), Offset(.9f, .1f), Offset(.9f, .9f), Offset(.1f, .9f)))
        listOf("Original", "Auto", "Clean", "White Paper", "B&W", "Ink", "Magic Color", "Photo", "Gray", "Soft Gray", "High Contrast").forEach { filter ->
            val output = ImageProcessor.filter(source, filter)
            assertEquals("width changed for $filter", source.width, output?.width)
            assertEquals("height changed for $filter", source.height, output?.height)
        }
    }

    @Test
    fun perspectiveCorrectionProducesUprightDocument() {
        val corners = listOf(
            Offset(.18f, .12f),
            Offset(.84f, .18f),
            Offset(.76f, .88f),
            Offset(.12f, .80f),
        )
        val corrected = DocumentPerspectiveCorrector.crop(fixtureBitmap(corners), corners)

        assertTrue(corrected.width > 500)
        assertTrue(corrected.height > 600)
        val center = corrected.getPixel(corrected.width / 2, corrected.height / 2)
        assertTrue(Color.red(center) > 220 && Color.green(center) > 220 && Color.blue(center) > 220)
    }

    @Test
    fun bookSplitter_findsOffCenterDarkGutter() {
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(235, 232, 220))
            drawRect(500f, 0f, 535f, 800f, Paint().apply { color = Color.rgb(55, 52, 48) })
        }
        val pages = BookPageSplitter.split(bitmap)
        assertEquals(2, pages.size)
        assertTrue(pages[0].width in 470..570)
        assertEquals(bitmap.width, pages.sumOf { it.width })
    }

    private fun fixtureBitmap(corners: List<Offset>): Bitmap {
        val width = 900
        val height = 1200
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(48, 58, 64))
            val path = Path().apply {
                moveTo(corners[0].x * width, corners[0].y * height)
                corners.drop(1).forEach { lineTo(it.x * width, it.y * height) }
                close()
            }
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(246, 244, 238) })
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(210, 210, 205)
                style = Paint.Style.STROKE
                strokeWidth = 5f
            })
        }
    }
}
