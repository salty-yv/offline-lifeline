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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "本地个人应急信息卡",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        EmergencyTextField("姓名", uiState.name, onNameChanged)
        EmergencyTextField("血型", uiState.bloodType, onBloodTypeChanged)
        EmergencyTextField("过敏史", uiState.allergies, onAllergiesChanged)
        EmergencyTextField("慢性病", uiState.chronicConditions, onChronicConditionsChanged)
        EmergencyTextField("常用药", uiState.medications, onMedicationsChanged)
        EmergencyTextField("紧急联系人", uiState.emergencyContact, onEmergencyContactChanged)
        EmergencyTextField("备注", uiState.notes, onNotesChanged, minLines = 3)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.hideSensitiveFields,
                onCheckedChange = onHideSensitiveFieldsChanged
            )
            Text(
                text = "默认隐藏敏感字段",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onSave) {
                Text("保存")
            }
            TextButton(onClick = onShowRescueView) {
                Text("展示给救援人员")
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("返回编辑")
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
                    text = "应急信息",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                RescueLine("姓名", uiState.name)
                RescueLine("血型", uiState.bloodType)
                RescueLine("紧急联系人", uiState.emergencyContact)
                if (!uiState.hideSensitiveFields) {
                    RescueLine("过敏史", uiState.allergies)
                    RescueLine("慢性病", uiState.chronicConditions)
                    RescueLine("常用药", uiState.medications)
                    RescueLine("备注", uiState.notes)
                }
            }
        }
    }
}

@Composable
private fun RescueLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f)
        )
        Text(
            text = value.ifBlank { "未填写" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
