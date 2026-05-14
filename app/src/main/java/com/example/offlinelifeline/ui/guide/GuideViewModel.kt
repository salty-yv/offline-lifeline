package com.example.offlinelifeline.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offlinelifeline.data.db.GuideEntity
import com.example.offlinelifeline.data.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuideViewModel(
    private val guideRepository: GuideRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState

    init {
        viewModelScope.launch {
            runCatching {
                guideRepository.seedDefaultGuidesIfNeeded()
                guideRepository.observeGuides().collect { guides ->
                    _uiState.update { state ->
                        val selected = state.selectedGuide
                            ?.let { current -> guides.firstOrNull { it.id == current.id } }
                        state.copy(
                            guides = guides,
                            visibleGuides = filterGuides(guides, state.query),
                            selectedGuide = selected,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "离线指南加载失败"
                    )
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query
            )
        }
        viewModelScope.launch {
            val results = if (query.isBlank()) {
                _uiState.value.guides
            } else {
                guideRepository.search(query)
            }
            _uiState.update { it.copy(visibleGuides = results) }
        }
    }

    fun selectGuide(guide: GuideEntity) {
        _uiState.update { it.copy(selectedGuide = guide) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedGuide = null) }
    }

    private fun filterGuides(guides: List<GuideEntity>, query: String): List<GuideEntity> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return guides

        return guides.filter { guide ->
            guide.title.contains(normalizedQuery, ignoreCase = true) ||
                guide.summary.contains(normalizedQuery, ignoreCase = true) ||
                guide.tags.contains(normalizedQuery, ignoreCase = true) ||
                guide.body.contains(normalizedQuery, ignoreCase = true)
        }
    }

    class Factory(
        private val guideRepository: GuideRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GuideViewModel::class.java)) {
                return GuideViewModel(guideRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
