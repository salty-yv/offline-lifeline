package com.example.offlinelifeline.inference

/**
 * 所有可用本地模型的注册表。
 *
 * ## 下载源
 * 使用 Hugging Face CDN 直链（公开仓库，无需 Token）：
 * `https://huggingface.co/{owner}/{repo}/resolve/main/{filename}`
 *
 * HuggingFace 的 resolve 端点会自动重定向到 CDN，支持 Range 请求断点续传。
 *
 * ## 注意
 * expectedSha256 / expectedSizeBytes 来自 Hugging Face raw pointer，
 * 用于下载完成后做本地完整性校验。
 */
object ModelCatalog {

    val E2B = ModelManifest(
        modelId = "e2b",
        modelName = "Gemma-4-E2B-it",
        modelVersion = "1.0",
        // 实际文件名（来自 HF 仓库文件列表）
        fileName = "gemma-4-E2B-it.litertlm",
        expectedSha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
        expectedSizeBytes = 2_588_147_712L,
        // HuggingFace CDN 直链，公开仓库无需 Token，支持 Range 断点续传
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        fallbackUrl = "https://hf-mirror.com/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        alternateFileNames = listOf("gemma-4-E2B-it-litert-lm.litertlm")
    )

    val E4B = ModelManifest(
        modelId = "e4b",
        modelName = "Gemma-4-E4B-it",
        modelVersion = "1.0",
        // 实际文件名（来自 HF 仓库文件列表）
        fileName = "gemma-4-E4B-it.litertlm",
        expectedSha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
        expectedSizeBytes = 3_659_530_240L,
        // HuggingFace CDN 直链，公开仓库无需 Token，支持 Range 断点续传
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        fallbackUrl = "https://hf-mirror.com/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
    )

    val all: List<ModelManifest> = listOf(E2B, E4B)

    fun findById(id: String): ModelManifest? = all.firstOrNull { it.modelId == id }
}
