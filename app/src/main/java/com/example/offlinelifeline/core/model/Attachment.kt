package com.example.offlinelifeline.core.model

sealed class Attachment {
    data class Image(
        val localPath: String,
        val width: Int? = null,
        val height: Int? = null
    ) : Attachment()
}
