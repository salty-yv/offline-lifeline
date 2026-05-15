package com.example.offlinelifeline.ui.emergencycard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun EmergencyCardScreen(
    viewModel: EmergencyCardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isRescueView) {
        RescueCardView(
            uiState = uiState,
            onBack = { viewModel.setRescueView(false) },
            modifier = modifier
        )
    } else {
        EmergencyCardEditor(
            uiState = uiState,
            onNameChanged = viewModel::updateName,
            onBloodTypeChanged = viewModel::updateBloodType,
            onAllergiesChanged = viewModel::updateAllergies,
            onChronicConditionsChanged = viewModel::updateChronicConditions,
            onMedicationsChanged = viewModel::updateMedications,
            onEmergencyContactChanged = viewModel::updateEmergencyContact,
            onNotesChanged = viewModel::updateNotes,
            onHideSensitiveFieldsChanged = viewModel::updateHideSensitiveFields,
            onSave = viewModel::save,
            onShowRescueView = { viewModel.setRescueView(true) },
            modifier = modifier
        )
    }
}

@Composable
private fun EmergencyCardEditor(
    uiState: EmergencyCardUiState,
    onNameChanged: (String) -> Unit,
    onBloodTypeChanged: (String) -> Unit,
    onAllergiesChanged: (String) -> Unit,
    onChronicConditionsChanged: (String) -> Unit,
    onMedicationsChanged: (String) -> Unit,
    onEmergencyContactChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHideSensitiveFieldsChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onShowRescueView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.emergencyCardEditorTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        EmergencyTextField(strings.name, uiState.name, onNameChanged)
        EmergencyTextField(strings.bloodType, uiState.bloodType, onBloodTypeChanged)
        EmergencyTextField(strings.allergies, uiState.allergies, onAllergiesChanged)
        EmergencyTextField(strings.chronicConditions, uiState.chronicConditions, onChronicConditionsChanged)
        EmergencyTextField(strings.medications, uiState.medications, onMedicationsChanged)
        EmergencyTextField(strings.emergencyContact, uiState.emergencyContact, onEmergencyContactChanged)
        EmergencyTextField(strings.notes, uiState.notes, onNotesChanged, minLines = 3)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.hideSensitiveFields,
                onCheckedChange = onHideSensitiveFieldsChanged
            )
            Text(
                text = strings.hideSensitiveFields,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onSave) {
                Text(strings.save)
            }
            TextButton(onClick = onShowRescueView) {
                Text(strings.showToRescuers)
            }
        }

        uiState.message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmergencyTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines
    )
}

@Composable
private fun RescueCardView(
    uiState: EmergencyCardUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(strings.backToEdit)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.emergencyInformation,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                RescueLine(strings.name, uiState.name)
                RescueLine(strings.bloodType, uiState.bloodType)
                RescueLine(strings.emergencyContact, uiState.emergencyContact)
                if (!uiState.hideSensitiveFields) {
                    RescueLine(strings.allergies, uiState.allergies)
                    RescueLine(strings.chronicConditions, uiState.chronicConditions)
                    RescueLine(strings.medications, uiState.medications)
                    RescueLine(strings.notes, uiState.notes)
                }
            }
        }
    }
}

@Composable
private fun RescueLine(label: String, value: String) {
    val strings = LocalAppStrings.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f)
        )
        Text(
            text = value.ifBlank { strings.notFilled },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
