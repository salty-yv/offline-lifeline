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

    fun updateName(value: String) = update { copy(name = value, isSavedMessageVisible = false) }
    fun updateBloodType(value: String) = update { copy(bloodType = sanitizeBloodType(value, bloodType), isSavedMessageVisible = false) }
    fun updateAllergies(value: String) = update { copy(allergies = value, isSavedMessageVisible = false) }
    fun updateChronicConditions(value: String) = update { copy(chronicConditions = value, isSavedMessageVisible = false) }
    fun updateMedications(value: String) = update { copy(medications = value, isSavedMessageVisible = false) }
    fun updateEmergencyContact(value: String) = update { copy(emergencyContact = value, isSavedMessageVisible = false) }
    fun updateNotes(value: String) = update { copy(notes = sanitizeDateOfBirth(value), isSavedMessageVisible = false) }
    fun updateHideSensitiveFields(value: Boolean) = update { copy(hideSensitiveFields = value, isSavedMessageVisible = false) }

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
            _uiState.update { it.copy(isSavedMessageVisible = true) }
        }
    }

    private fun update(block: EmergencyCardUiState.() -> EmergencyCardUiState) {
        _uiState.update { it.block() }
    }

    private fun sanitizeDateOfBirth(value: String): String {
        val digits = value.filter(Char::isDigit).take(8)
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index == 4 || index == 6) append('-')
                append(char)
            }
        }
    }

    private fun sanitizeBloodType(value: String, previousValue: String): String {
        val normalized = value
            .uppercase()
            .filter { it == 'A' || it == 'B' || it == 'O' || it == '+' || it == '-' }
            .take(3)
        val allowedPrefixes = setOf(
            "",
            "A",
            "B",
            "O",
            "AB",
            "A+",
            "A-",
            "B+",
            "B-",
            "O+",
            "O-",
            "AB+",
            "AB-"
        )
        return if (normalized in allowedPrefixes) normalized else previousValue
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
