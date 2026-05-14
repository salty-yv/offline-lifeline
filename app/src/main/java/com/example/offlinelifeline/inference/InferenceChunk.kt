package com.example.offlinelifeline.inference

data class InferenceChunk(
    val text: String,
    val isFinal: Boolean = false
)
