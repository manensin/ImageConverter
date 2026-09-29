package com.example.imageresizer.data

import android.net.Uri

data class ImageMetadata(
    val uri: Uri,
    val name: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String?
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format("%.1f KB", sizeBytes / 1024f)
            else -> String.format("%.2f MB", sizeBytes / (1024f * 1024f))
        }
}

enum class ScaleMode(val title: String, val description: String) {
    EXACT("Exact Stretch", "Resize directly to width & height"),
    FIT_CENTER("Aspect Fit", "Fit inside dimensions without cropping"),
    CENTER_CROP("Crop to Fill", "Fill exact dimensions, cropping excess")
}

enum class OutputFormat(val displayName: String, val extension: String, val mimeType: String, val supportsQuality: Boolean) {
    JPEG("JPEG (.jpg)", "jpg", "image/jpeg", true),
    PNG("PNG (.png)", "png", "image/png", false),
    WEBP("WEBP (.webp)", "webp", "image/webp", true)
}

data class DimensionPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val category: String
)

object PresetDimensions {
    val presets = listOf(
        DimensionPreset("Square (1:1)", 1080, 1080, "Social"),
        DimensionPreset("Story / Reel (9:16)", 1080, 1920, "Social"),
        DimensionPreset("Portrait Post (4:5)", 1080, 1350, "Social"),
        DimensionPreset("Landscape HD (16:9)", 1920, 1080, "Display"),
        DimensionPreset("4K Ultra HD", 3840, 2160, "Display"),
        DimensionPreset("Twitter/X Banner", 1500, 500, "Header"),
        DimensionPreset("Avatar / Profile", 500, 500, "Profile"),
        DimensionPreset("Passport Photo", 600, 600, "ID / Doc")
    )
}

data class ResizeResult(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val format: OutputFormat,
    val isShared: Boolean = false
) {
    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format("%.1f KB", sizeBytes / 1024f)
            else -> String.format("%.2f MB", sizeBytes / (1024f * 1024f))
        }
}
