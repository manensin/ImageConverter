package com.example.imageresizer

import android.net.Uri
import com.example.imageresizer.data.DimensionPreset
import com.example.imageresizer.data.ImageMetadata
import com.example.imageresizer.data.OutputFormat
import com.example.imageresizer.data.PresetDimensions
import com.example.imageresizer.data.ResizeResult
import com.example.imageresizer.data.ScaleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class DimensionCalculationsTest {

    @Test
    fun testAspectRatioCalculation() {
        val width = 1920
        val height = 1080
        val aspectRatio = width.toFloat() / height.toFloat()

        assertEquals(1.7777778f, aspectRatio, 0.001f)

        // If target width is changed to 1280, calculate proportional height
        val targetWidth = 1280
        val proportionalHeight = (targetWidth / aspectRatio).roundToInt()
        assertEquals(720, proportionalHeight)

        // If target height is changed to 540, calculate proportional width
        val targetHeight = 540
        val proportionalWidth = (targetHeight * aspectRatio).roundToInt()
        assertEquals(960, proportionalWidth)
    }

    @Test
    fun testPercentageScaling() {
        val origW = 4000
        val origH = 3000

        val scale50W = (origW * 0.50f).roundToInt()
        val scale50H = (origH * 0.50f).roundToInt()

        assertEquals(2000, scale50W)
        assertEquals(1500, scale50H)

        val scale25W = (origW * 0.25f).roundToInt()
        val scale25H = (origH * 0.25f).roundToInt()

        assertEquals(1000, scale25W)
        assertEquals(750, scale25H)
    }

    @Test
    fun testPresetDimensionsIntegrity() {
        val presets = PresetDimensions.presets
        assertTrue(presets.isNotEmpty())

        val square = presets.first { it.label.contains("Square") }
        assertEquals(1080, square.width)
        assertEquals(1080, square.height)

        val story = presets.first { it.label.contains("Story") }
        assertEquals(1080, story.width)
        assertEquals(1920, story.height)

        val hd = presets.first { it.label.contains("Landscape HD") }
        assertEquals(1920, hd.width)
        assertEquals(1080, hd.height)
    }

    @Test
    fun testFileSizeFormatting() {
        val smallSize = 512L
        val kbSize = 150 * 1024L
        val mbSize = 3500 * 1024L

        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024f)
            else -> String.format("%.2f MB", bytes / (1024f * 1024f))
        }

        assertEquals("512 B", formatBytes(smallSize))
        assertEquals("150.0 KB", formatBytes(kbSize))
        assertEquals("3.42 MB", formatBytes(mbSize))
    }

    @Test
    fun testScaleModeEnumCoverage() {
        assertEquals(3, ScaleMode.values().size)
        assertTrue(ScaleMode.values().contains(ScaleMode.EXACT))
        assertTrue(ScaleMode.values().contains(ScaleMode.FIT_CENTER))
        assertTrue(ScaleMode.values().contains(ScaleMode.CENTER_CROP))
    }

    @Test
    fun testOutputFormatCapabilities() {
        assertTrue(OutputFormat.JPEG.supportsQuality)
        assertTrue(OutputFormat.WEBP.supportsQuality)
        assertEquals(false, OutputFormat.PNG.supportsQuality)
    }
}
