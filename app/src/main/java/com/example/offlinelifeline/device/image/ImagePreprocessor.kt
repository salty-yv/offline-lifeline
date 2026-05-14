package com.example.offlinelifeline.device.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.offlinelifeline.core.common.AppDispatchers
import com.example.offlinelifeline.core.model.Attachment
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImagePreprocessor(
    context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val appContext = context.applicationContext

    suspend fun processUri(uri: Uri): Result<Attachment.Image> {
        return withContext(dispatchers.io) {
            runCatching {
                val sourceFile = createTempFile(prefix = "selected_raw_", suffix = ".jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    sourceFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Cannot open selected image")

                processFileInternal(sourceFile, deleteSource = true)
            }
        }
    }

    suspend fun processFile(file: File): Result<Attachment.Image> {
        return withContext(dispatchers.io) {
            runCatching {
                processFileInternal(file, deleteSource = true)
            }
        }
    }

    fun deleteProcessedImage(image: Attachment.Image) {
        runCatching {
            File(image.localPath).delete()
        }
    }

    fun createCameraRawFile(): File {
        return createTempFile(prefix = "camera_raw_", suffix = ".jpg")
    }

    private fun processFileInternal(sourceFile: File, deleteSource: Boolean): Attachment.Image {
        val orientation = readOrientation(sourceFile)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            ?: error("Cannot decode image")
        val normalized = rotateIfNeeded(decoded, orientation)
        val scaled = scaleToMaxEdge(normalized)

        val outputFile = createTempFile(prefix = "processed_", suffix = ".jpg")
        FileOutputStream(outputFile).use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        }

        val result = Attachment.Image(
            localPath = outputFile.absolutePath,
            width = scaled.width,
            height = scaled.height
        )

        if (scaled !== normalized) scaled.recycle()
        if (normalized !== decoded) normalized.recycle()
        decoded.recycle()
        if (deleteSource) sourceFile.delete()

        return result
    }

    private fun readOrientation(file: File): Int {
        return runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / sampleSize >= MAX_EDGE && halfHeight / sampleSize >= MAX_EDGE) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMaxEdge(bitmap: Bitmap): Bitmap {
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        if (maxEdge <= MAX_EDGE) return bitmap

        val scale = MAX_EDGE.toFloat() / maxEdge.toFloat()
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun createTempFile(prefix: String, suffix: String): File {
        val dir = File(appContext.cacheDir, "image-input").apply { mkdirs() }
        return File.createTempFile(prefix, suffix, dir)
    }

    private companion object {
        const val MAX_EDGE = 1280
        const val JPEG_QUALITY = 86
    }
}
