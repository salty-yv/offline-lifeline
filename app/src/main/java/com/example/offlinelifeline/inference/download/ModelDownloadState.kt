package com.example.offlinelifeline.inference.download

import com.example.offlinelifeline.inference.ModelManifest

/**
 * 描述单个模型的下载状态。
 */
sealed class ModelDownloadState {

    /** 尚未开始，或不知晓当前状态 */
    data object Idle : ModelDownloadState()

    /** 正在等待（WorkManager 已入队，但尚未开始） */
    data object Queued : ModelDownloadState()

    /**
     * 下载中
     * @param downloadedBytes 已下载字节数
     * @param totalBytes      文件总字节数（-1 表示未知）
     * @param progressFraction 0.0f – 1.0f，若 totalBytes 未知则为 -1f
     */
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progressFraction: Float
    ) : ModelDownloadState()

    /** 下载完成并校验通过 */
    data class Completed(val manifest: ModelManifest) : ModelDownloadState()

    /** 已暂停（后台 Worker 被取消，临时文件保留） */
    data object Paused : ModelDownloadState()

    /**
     * 下载或校验失败
     * @param reason 失败原因描述
     */
    data class Failed(val reason: String) : ModelDownloadState()
}
