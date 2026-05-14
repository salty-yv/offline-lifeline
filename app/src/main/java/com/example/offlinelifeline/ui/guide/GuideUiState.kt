package com.example.offlinelifeline.ui.guide

import com.example.offlinelifeline.data.db.GuideEntity

data class GuideUiState(
    val guides: List<GuideEntity> = emptyList(),
    val visibleGuides: List<GuideEntity> = emptyList(),
    val selectedGuide: GuideEntity? = null,
    val query: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
