package com.example.offlinelifeline.inference

import com.example.offlinelifeline.core.common.AppDispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

class ModelIntegrityChecker(
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    suspend fun verifyFile(file: File, manifest: ModelManifest): ModelIntegrityResult {
        return withContext(dispatchers.io) {
            if (!file.exists() || !file.isFile) {
                return@withContext ModelIntegrityResult.Missing
            }

            if (manifest.expectedSizeBytes > 0 && file.length() != manifest.expectedSizeBytes) {
                return@withContext ModelIntegrityResult.SizeMismatch(
                    actualSizeBytes = file.length(),
                    expectedSizeBytes = manifest.expectedSizeBytes
                )
            }

            verifySha256(file.inputStream(), manifest.expectedSha256)
        }
    }

    suspend fun verifyStream(openStream: () -> InputStream, manifest: ModelManifest): ModelIntegrityResult {
        return withContext(dispatchers.io) {
            verifySha256(openStream(), manifest.expectedSha256)
        }
    }

    private fun verifySha256(inputStream: InputStream, expectedSha256: String): ModelIntegrityResult {
        if (expectedSha256.isBlank()) {
            inputStream.close()
            return ModelIntegrityResult.Valid
        }

        val actualSha256 = inputStream.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

        return if (actualSha256.equals(expectedSha256, ignoreCase = true)) {
            ModelIntegrityResult.Valid
        } else {
            ModelIntegrityResult.ChecksumMismatch(actualSha256 = actualSha256, expectedSha256 = expectedSha256)
        }
    }
}

sealed class ModelIntegrityResult {
    data object Valid : ModelIntegrityResult()
    data object Missing : ModelIntegrityResult()
    data class SizeMismatch(
        val actualSizeBytes: Long,
        val expectedSizeBytes: Long
    ) : ModelIntegrityResult()
    data class ChecksumMismatch(
        val actualSha256: String,
        val expectedSha256: String
    ) : ModelIntegrityResult()
}
