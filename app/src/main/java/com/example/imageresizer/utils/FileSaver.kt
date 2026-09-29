package com.example.imageresizer.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.imageresizer.data.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileSaver {

    suspend fun saveToGallery(
        context: Context,
        bytes: ByteArray,
        format: OutputFormat,
        suggestedName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val timeStamp = System.currentTimeMillis()
        val cleanName = suggestedName.substringBeforeLast(".")
        val fileName = "Resized_${cleanName}_$timeStamp.${format.extension}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/ImageResizer"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, contentValues) ?: return@withContext null

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                context.contentResolver.delete(uri, null, null)
                null
            }
        } else {
            // Legacy Android (Pie and below)
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "ImageResizer")
            if (!appDir.exists()) appDir.mkdirs()

            val file = File(appDir, fileName)
            try {
                FileOutputStream(file).use { out ->
                    out.write(bytes)
                    out.flush()
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun saveToCacheForSharing(
        context: Context,
        bytes: ByteArray,
        format: OutputFormat,
        suggestedName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheFolder = File(context.cacheDir, "shared_images").apply {
                if (!exists()) mkdirs()
            }
            val cleanName = suggestedName.substringBeforeLast(".")
            val file = File(cacheFolder, "Shared_${cleanName}_${System.currentTimeMillis()}.${format.extension}")
            FileOutputStream(file).use { out ->
                out.write(bytes)
                out.flush()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareImage(context: Context, uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Resized Image"))
    }
}
