package com.example.imageresizer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageresizer.data.DimensionPreset
import com.example.imageresizer.data.ImageMetadata
import com.example.imageresizer.data.OutputFormat
import com.example.imageresizer.data.ResizeResult
import com.example.imageresizer.data.ScaleMode
import com.example.imageresizer.utils.FileSaver
import com.example.imageresizer.utils.ImageResizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ImageResizerUiState(
    val selectedImage: ImageMetadata? = null,
    val targetWidthText: String = "",
    val targetHeightText: String = "",
    val maintainAspectRatio: Boolean = true,
    val scaleMode: ScaleMode = ScaleMode.EXACT,
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val quality: Int = 85,
    val isProcessing: Boolean = false,
    val lastResult: ResizeResult? = null,
    val statusMessage: String? = null
)

class ImageResizerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageResizerUiState())
    val uiState: StateFlow<ImageResizerUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val metadata = ImageResizer.extractMetadata(getApplication(), uri)
            _uiState.update { state ->
                if (metadata != null) {
                    state.copy(
                        selectedImage = metadata,
                        targetWidthText = metadata.width.toString(),
                        targetHeightText = metadata.height.toString(),
                        isProcessing = false,
                        lastResult = null,
                        statusMessage = null
                    )
                } else {
                    state.copy(
                        isProcessing = false,
                        statusMessage = "Failed to load image details."
                    )
                }
            }
        }
    }

    fun onWidthChanged(newWidthStr: String) {
        val filtered = newWidthStr.filter { it.isDigit() }.take(5)
        _uiState.update { state ->
            val orig = state.selectedImage
            if (state.maintainAspectRatio && orig != null && orig.height > 0 && filtered.isNotEmpty()) {
                val w = filtered.toIntOrNull()
                if (w != null && w > 0) {
                    val calcH = (w / orig.aspectRatio).roundToInt().coerceAtLeast(1)
                    state.copy(targetWidthText = filtered, targetHeightText = calcH.toString())
                } else {
                    state.copy(targetWidthText = filtered)
                }
            } else {
                state.copy(targetWidthText = filtered)
            }
        }
    }

    fun onHeightChanged(newHeightStr: String) {
        val filtered = newHeightStr.filter { it.isDigit() }.take(5)
        _uiState.update { state ->
            val orig = state.selectedImage
            if (state.maintainAspectRatio && orig != null && orig.height > 0 && filtered.isNotEmpty()) {
                val h = filtered.toIntOrNull()
                if (h != null && h > 0) {
                    val calcW = (h * orig.aspectRatio).roundToInt().coerceAtLeast(1)
                    state.copy(targetHeightText = filtered, targetWidthText = calcW.toString())
                } else {
                    state.copy(targetHeightText = filtered)
                }
            } else {
                state.copy(targetHeightText = filtered)
            }
        }
    }

    fun toggleMaintainAspectRatio() {
        _uiState.update { it.copy(maintainAspectRatio = !it.maintainAspectRatio) }
    }

    fun applyPreset(preset: DimensionPreset) {
        _uiState.update {
            it.copy(
                targetWidthText = preset.width.toString(),
                targetHeightText = preset.height.toString()
            )
        }
    }

    fun applyPercentage(percentage: Int) {
        val orig = _uiState.value.selectedImage ?: return
        val factor = percentage / 100f
        val newW = (orig.width * factor).roundToInt().coerceAtLeast(1)
        val newH = (orig.height * factor).roundToInt().coerceAtLeast(1)
        _uiState.update {
            it.copy(
                targetWidthText = newW.toString(),
                targetHeightText = newH.toString()
            )
        }
    }

    fun setScaleMode(mode: ScaleMode) {
        _uiState.update { it.copy(scaleMode = mode) }
    }

    fun setOutputFormat(format: OutputFormat) {
        _uiState.update { it.copy(outputFormat = format) }
    }

    fun setQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(lastResult = null) }
    }

    fun processImage(forSharing: Boolean = false, onReadyToShare: ((Uri, String) -> Unit)? = null) {
        val state = _uiState.value
        val image = state.selectedImage ?: run {
            _uiState.update { it.copy(statusMessage = "Please select an image first") }
            return
        }

        val targetW = state.targetWidthText.toIntOrNull()
        val targetH = state.targetHeightText.toIntOrNull()

        if (targetW == null || targetH == null || targetW <= 0 || targetH <= 0) {
            _uiState.update { it.copy(statusMessage = "Dimensions must be greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = null) }

            val result = ImageResizer.resizeBitmap(
                context = getApplication(),
                uri = image.uri,
                targetWidth = targetW,
                targetHeight = targetH,
                scaleMode = state.scaleMode,
                outputFormat = state.outputFormat,
                quality = state.quality
            )

            if (result != null) {
                val (bytes, dimensions) = result
                val (finalW, finalH) = dimensions

                if (forSharing) {
                    val shareUri = FileSaver.saveToCacheForSharing(
                        context = getApplication(),
                        bytes = bytes,
                        format = state.outputFormat,
                        suggestedName = image.name
                    )
                    _uiState.update { it.copy(isProcessing = false) }
                    if (shareUri != null) {
                        onReadyToShare?.invoke(shareUri, state.outputFormat.mimeType)
                    } else {
                        _uiState.update { it.copy(statusMessage = "Failed to prepare image for sharing") }
                    }
                } else {
                    val savedUri = FileSaver.saveToGallery(
                        context = getApplication(),
                        bytes = bytes,
                        format = state.outputFormat,
                        suggestedName = image.name
                    )

                    _uiState.update {
                        if (savedUri != null) {
                            it.copy(
                                isProcessing = false,
                                lastResult = ResizeResult(
                                    uri = savedUri,
                                    width = finalW,
                                    height = finalH,
                                    sizeBytes = bytes.size.toLong(),
                                    format = state.outputFormat,
                                    isShared = false
                                ),
                                statusMessage = "Saved to Gallery / Pictures / ImageResizer!"
                            )
                        } else {
                            it.copy(
                                isProcessing = false,
                                statusMessage = "Failed to save resized image."
                            )
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Image processing failed. Try a smaller dimension."
                    )
                }
            }
        }
    }
}
