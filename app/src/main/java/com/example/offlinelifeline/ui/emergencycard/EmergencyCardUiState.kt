package com.example.offlinelifeline.ui.emergencycard

data class EmergencyCardUiState(
    val name: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val medications: String = "",
    val emergencyContact: String = "",
    val notes: String = "",
    val hideSensitiveFields: Boolean = true,
    val isLoading: Boolean = true,
    val isSavedMessageVisible: Boolean = false
)
