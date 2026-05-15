package com.example.offlinelifeline.ui.emergencycard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offlinelifeline.core.common.TimeProvider
import com.example.offlinelifeline.data.db.EmergencyCardEntity
import com.example.offlinelifeline.data.repository.EmergencyCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EmergencyCardViewModel(
    private val repository: EmergencyCardRepository,
    private val timeProvider: TimeProvider = TimeProvider.System
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmergencyCardUiState())
    val uiState: StateFlow<EmergencyCardUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeCard().collect { card ->
                if (card == null) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            name = card.name,
                            bloodType = card.bloodType,
                            allergies = card.allergies,
                            chronicConditions = card.chronicConditions,
                            medications = card.medications,
                            emergencyContact = card.emergencyContact,
                            notes = card.notes,
                            hideSensitiveFields = card.hideSensitiveFields,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun updateName(value: String) = update { copy(name = value, message = null) }
    fun updateBloodType(value: String) = update { copy(bloodType = value, message = null) }
    fun updateAllergies(value: String) = update { copy(allergies = value, message = null) }
    fun updateChronicConditions(value: String) = update { copy(chronicConditions = value, message = null) }
    fun updateMedications(value: String) = update { copy(medications = value, message = null) }
    fun updateEmergencyContact(value: String) = update { copy(emergencyContact = value, message = null) }
    fun updateNotes(value: String) = update { copy(notes = value, message = null) }
    fun updateHideSensitiveFields(value: Boolean) = update { copy(hideSensitiveFields = value, message = null) }
    fun setRescueView(enabled: Boolean) = update { copy(isRescueView = enabled) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveCard(
                EmergencyCardEntity(
                    name = state.name,
                    bloodType = state.bloodType,
                    allergies = state.allergies,
                    chronicConditions = state.chronicConditions,
                    medications = state.medications,
                    emergencyContact = state.emergencyContact,
                    notes = state.notes,
                    hideSensitiveFields = state.hideSensitiveFields,
                    updatedAtMillis = timeProvider.nowMillis()
                )
            )
            _uiState.update {
                it.copy(
                    message = "信息卡已保存在本机",
                    isRescueView = true
                )
            }
        }
    }

    private fun update(block: EmergencyCardUiState.() -> EmergencyCardUiState) {
        _uiState.update { it.block() }
    }

    class Factory(
        private val repository: EmergencyCardRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EmergencyCardViewModel::class.java)) {
                return EmergencyCardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
