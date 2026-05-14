package com.example.offlinelifeline.inference

data class ModelManifest(
    val modelName: String,
    val modelVersion: String,
    val fileName: String,
    val expectedSha256: String,
    val expectedSizeBytes: Long,
    val format: String = "litertlm",
    val alternateFileNames: List<String> = emptyList()
) {
    companion object {
        val Default = ModelManifest(
            modelName = "Gemma-4-E2B-it-litert-lm",
            modelVersion = "development-placeholder",
            fileName = "gemma-4-E2B-it-litert-lm.litertlm",
            expectedSha256 = "",
            expectedSizeBytes = 0L,
            alternateFileNames = listOf("gemma-4-E2B-it.litertlm")
        )
    }
}
