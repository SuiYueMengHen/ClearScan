package com.clearscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.geometry.Offset

class ClearScanUnitTest {
    @Test
    fun formatSize_usesReadableUnits() {
        assertEquals("0 KB", formatSize(0))
        assertEquals("1 KB", formatSize(1024))
        assertEquals("2.5 MB", formatSize(2_621_440))
    }

    @Test
    fun formatDate_usesDocumentListPattern() {
        val formatted = formatDate(1_717_243_800_000L)
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun appSettings_defaultsMatchClearScanPlan() {
        val settings = AppSettings()
        assertEquals("English", settings.language)
        assertEquals("Light", settings.theme)
        assertEquals("Internal Storage", settings.defaultSavePath)
        assertFalse(settings.loggedIn)
        assertTrue(settings.passwordMap.isEmpty())
    }

    @Test
    fun mimeTypeFor_supportsConvertedImageFormats() {
        assertEquals("application/pdf", mimeTypeFor("PDF"))
        assertEquals("image/png", mimeTypeFor("PNG"))
        assertEquals("image/webp", mimeTypeFor("WEBP"))
        assertEquals("image/bmp", mimeTypeFor("BMP"))
        assertEquals("image/jpeg", mimeTypeFor("JPG"))
        assertEquals("image/jpeg", mimeTypeFor("IMAGE"))
    }

    @Test
    fun perspectiveOutputSize_usesDraggedCornerGeometry() {
        val points = listOf(
            Offset(0.10f, 0.10f),
            Offset(0.90f, 0.12f),
            Offset(0.82f, 0.84f),
            Offset(0.16f, 0.88f),
        )
        val (width, height) = ImageProcessor.perspectiveOutputSize(1000, 1400, points)
        assertTrue(width in 650..810)
        assertTrue(height in 1000..1120)
    }

    @Test
    fun bitmapSampleSize_limitsHighResolutionHuaweiCapture() {
        assertEquals(4, bitmapSampleSize(6144, 8192, 2048))
        assertEquals(3, bitmapSampleSize(6144, 8192, 3072))
        assertEquals(1, bitmapSampleSize(1080, 1920, 2048))
        assertEquals(1, bitmapSampleSize(0, 0, 2048))
    }

    @Test
    fun hyMtPrompt_keepsChineseToEnglishDirection() {
        val prompt = buildHyMt2Prompt("你好，世界", "Chinese", "English")
        assertTrue(prompt.contains("翻译为英语"))
        assertFalse(prompt.contains("翻译为中文"))
        assertTrue(prompt.endsWith("你好，世界"))
    }

    @Test
    fun hyMtPrompt_autoDetectsChineseSource() {
        assertEquals("Chinese", detectTranslationLanguage("扫描这份文档"))
        assertEquals("English", detectTranslationLanguage("Scan this document"))
        assertTrue(buildHyMt2Prompt("Hello", "Auto", "Chinese").contains("翻译为中文"))
    }

    @Test
    fun hyMtModelValidation_rejectsLegacyAndTruncatedFiles() {
        assertTrue(isExpectedHyMt2Model(1_133_080_448L, "GGUF"))
        assertFalse(isExpectedHyMt2Model(440_000_000L, "GGUF"))
        assertFalse(isExpectedHyMt2Model(1_133_080_448L, "<htm"))
    }

    @Test
    fun hyMtPrompt_supportsAdditionalTargetLanguages() {
        assertTrue(buildHyMt2Prompt("你好", "Chinese", "Arabic").contains("翻译为阿拉伯语"))
        assertTrue(buildHyMt2Prompt("Hello", "English", "Vietnamese").contains("into Vietnamese"))
    }

    @Test
    fun translationChunks_respectMobileContextLimit() {
        val input = ("这是一个用于测试长文本分块翻译的句子。 ").repeat(100)
        val chunks = splitTranslationText(input, maxChars = 200)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 200 })
        assertTrue(chunks.joinToString("").replace(" ", "").isNotBlank())
    }
}
