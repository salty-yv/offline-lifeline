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
    val alternateFileNames: List<String> = emptyList(),
    val supportsImageInput: Boolean = false
) {
    companion object {
        val Default = ModelManifest(
            modelId = "e2b",
            modelName = "Gemma-4-E2B-it-litert-lm",
            modelVersion = "development-placeholder",
            fileName = "gemma-4-E2B-it.litertlm",
            expectedSha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            expectedSizeBytes = 2_588_147_712L,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            fallbackUrl = "https://hf-mirror.com/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            alternateFileNames = listOf("gemma-4-E2B-it-litert-lm.litertlm"),
            supportsImageInput = true
        )
    }
}
