package com.example.imageresizer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.example.imageresizer.data.ImageMetadata
import com.example.imageresizer.data.OutputFormat
import com.example.imageresizer.data.ScaleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageResizer {

    suspend fun extractMetadata(context: Context, uri: Uri): ImageMetadata? = withContext(Dispatchers.IO) {
        try {
            var fileName = "image"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = context.contentResolver.getType(uri)

            // Read dimensions without full allocation
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var origWidth = options.outWidth
            var origHeight = options.outHeight

            // Check EXIF rotation
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270
                ) {
                    val temp = origWidth
                    origWidth = origHeight
                    origHeight = temp
                }
            }

            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            ImageMetadata(
                uri = uri,
                name = fileName,
                width = origWidth,
                height = origHeight,
                sizeBytes = fileSize,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun resizeBitmap(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        scaleMode: ScaleMode,
        outputFormat: OutputFormat,
        quality: Int
    ): Pair<ByteArray, Pair<Int, Int>>? = withContext(Dispatchers.IO) {
        try {
            val sourceBitmap = decodeSampledBitmap(context, uri, targetWidth, targetHeight)
                ?: return@withContext null

            val orientedBitmap = applyExifOrientation(context, uri, sourceBitmap)

            val finalBitmap = when (scaleMode) {
                ScaleMode.EXACT -> {
                    Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true)
                }
                ScaleMode.FIT_CENTER -> {
                    val scale = min(
                        targetWidth.toFloat() / orientedBitmap.width.toFloat(),
                        targetHeight.toFloat() / orientedBitmap.height.toFloat()
                    )
                    val outW = max(1, (orientedBitmap.width * scale).roundToInt())
                    val outH = max(1, (orientedBitmap.height * scale).roundToInt())
                    Bitmap.createScaledBitmap(orientedBitmap, outW, outH, true)
                }
                ScaleMode.CENTER_CROP -> {
                    val scale = max(
                        targetWidth.toFloat() / orientedBitmap.width.toFloat(),
                        targetHeight.toFloat() / orientedBitmap.height.toFloat()
                    )
                    val scaledW = max(1, (orientedBitmap.width * scale).roundToInt())
                    val scaledH = max(1, (orientedBitmap.height * scale).roundToInt())
                    val intermediate = Bitmap.createScaledBitmap(orientedBitmap, scaledW, scaledH, true)

                    val xOffset = max(0, (scaledW - targetWidth) / 2)
                    val yOffset = max(0, (scaledH - targetHeight) / 2)
                    val croppedW = min(targetWidth, scaledW - xOffset)
                    val croppedH = min(targetHeight, scaledH - yOffset)

                    val cropped = Bitmap.createBitmap(intermediate, xOffset, yOffset, croppedW, croppedH)
                    if (cropped != intermediate && intermediate != orientedBitmap) {
                        intermediate.recycle()
                    }
                    cropped
                }
            }

            if (finalBitmap != orientedBitmap && orientedBitmap != sourceBitmap) {
                orientedBitmap.recycle()
            }
            if (sourceBitmap != finalBitmap && !sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }

            val compressFormat = when (outputFormat) {
                OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                OutputFormat.WEBP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
            }

            val outStream = ByteArrayOutputStream()
            val finalQuality = if (outputFormat.supportsQuality) quality.coerceIn(1, 100) else 100
            finalBitmap.compress(compressFormat, finalQuality, outStream)
            val bytes = outStream.toByteArray()

            val dimensions = Pair(finalBitmap.width, finalBitmap.height)
            finalBitmap.recycle()

            Pair(bytes, dimensions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
}
