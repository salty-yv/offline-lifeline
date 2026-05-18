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
import kotlin.math.roundToInt

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

        check(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "Cannot read image dimensions"
        }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            ?: error("Cannot decode image")
        val normalized = rotateIfNeeded(decoded, orientation)
        val scaled = scaleToMaxEdge(normalized)

        val outputFile = createTempFile(prefix = "processed_", suffix = ".png")
        FileOutputStream(outputFile).use { output ->
            check(scaled.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                "Cannot encode processed image"
            }
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
        if (width <= MAX_EDGE && height <= MAX_EDGE) return 1

        val widthRatio = (width.toFloat() / MAX_EDGE.toFloat()).roundToInt()
        val heightRatio = (height.toFloat() / MAX_EDGE.toFloat()).roundToInt()
        return maxOf(widthRatio, heightRatio).coerceAtLeast(1)
    }

    private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
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
        const val MAX_EDGE = 1024
        const val PNG_QUALITY = 100
    }
}
