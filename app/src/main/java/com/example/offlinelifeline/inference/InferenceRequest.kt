package com.example.offlinelifeline.inference

data class InferenceRequest(
    val text: String,
    val imagePaths: List<String> = emptyList(),
    val systemInstruction: String,
    val safetyInstruction: String
)
