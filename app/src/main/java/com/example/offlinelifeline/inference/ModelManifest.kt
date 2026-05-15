package com.example.offlinelifeline.inference

data class ModelManifest(
    val modelId: String,               // 唯一标识，如 "e2b" / "e4b"
    val modelName: String,
    val modelVersion: String,
    val fileName: String,
    val expectedSha256: String,
    val expectedSizeBytes: Long,
    val downloadUrl: String,           // 首选下载链接 (ModelScope)
    val fallbackUrl: String? = null,   // 备用下载链接
    val format: String = "litertlm",
    val alternateFileNames: List<String> = emptyList()
) {
    companion object {
        val Default = ModelManifest(
            modelId = "e2b",
            modelName = "Gemma-4-E2B-it-litert-lm",
            modelVersion = "development-placeholder",
            fileName = "gemma-4-E2B-it.litertlm",
            expectedSha256 = "",
            expectedSizeBytes = 0L,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            fallbackUrl = "https://hf-mirror.com/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            alternateFileNames = listOf("gemma-4-E2B-it-litert-lm.litertlm")
        )
    }
}
