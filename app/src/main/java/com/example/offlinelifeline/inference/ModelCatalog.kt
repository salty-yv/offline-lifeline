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
 * expectedSha256 / expectedSizeBytes 暂时留空，
 * [ModelIntegrityChecker] 在字段为空/0 时会跳过校验直接放行。
 * 正式发布前请填入实际值。
 */
object ModelCatalog {

    val E2B = ModelManifest(
        modelId = "e2b",
        modelName = "Gemma-4-E2B-it",
        modelVersion = "1.0",
        // 实际文件名（来自 HF 仓库文件列表）
        fileName = "gemma-4-E2B-it.litertlm",
        expectedSha256 = "",       // TODO: 填入实际 SHA-256
        expectedSizeBytes = 0L,    // TODO: 填入实际字节数（实测约 2.59 GB）
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
        expectedSha256 = "",       // TODO: 填入实际 SHA-256
        expectedSizeBytes = 0L,    // TODO: 填入实际字节数（实测约 3.66 GB）
        // HuggingFace CDN 直链，公开仓库无需 Token，支持 Range 断点续传
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        fallbackUrl = "https://hf-mirror.com/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
    )

    val all: List<ModelManifest> = listOf(E2B, E4B)

    fun findById(id: String): ModelManifest? = all.firstOrNull { it.modelId == id }
}
