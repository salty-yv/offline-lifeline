package com.example.offlinelifeline.ui.emergencycard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.R
import com.example.offlinelifeline.ui.components.LifelineCard
import com.example.offlinelifeline.ui.components.LifelineTopBar
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun EmergencyCardScreen(
    viewModel: EmergencyCardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by rememberSaveable { mutableStateOf(false) }
    val strings = LocalAppStrings.current
    val labels = emergencyCardLabelsFor(strings.languageTag)
    BackHandler(enabled = isEditing) {
        isEditing = false
    }

    if (isEditing) {
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
            onBack = { isEditing = false },
            onSave = {
                viewModel.save()
                isEditing = false
            },
            modifier = modifier
        )
    } else {
        EmergencyCardSummary(
            uiState = uiState,
            onEdit = { isEditing = true },
            modifier = modifier
        )
    }
}

@Composable
private fun EmergencyCardSummary(
    uiState: EmergencyCardUiState,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val labels = emergencyCardLabelsFor(strings.languageTag)
    val contact = emergencyContactParts(uiState.emergencyContact, strings.notFilled, labels.primaryContact)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(
            title = labels.emergencyCard,
            actionIcon = Icons.Default.Edit,
            actionContentDescription = strings.emergencyCardEditorTitle,
            onActionClick = onEdit
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LifelineCard {
                Text(
                    text = labels.personalInformation,
                    style = MaterialTheme.typography.titleLarge
                )
                SummaryLine(labels.name, uiState.name)
                SummaryLine(labels.dateOfBirth, uiState.notes)
            }
            LifelineCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = labels.medicalProfile,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                SummaryLine(labels.bloodType, uiState.bloodType, valueColor = MaterialTheme.colorScheme.error)
                SummaryLine(labels.allergies, uiState.allergies)
            }
            LifelineCard {
                Text(
                    text = labels.emergencyContacts,
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = contact.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0x2010B981),
                        contentColor = Color(0xFF10B981)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_call_24),
                                contentDescription = labels.emergencyContactPhone,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
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
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val labels = emergencyCardLabelsFor(strings.languageTag)
    var showBloodTypePicker by rememberSaveable { mutableStateOf(false) }
    if (showBloodTypePicker) {
        BloodTypePickerDialog(
            title = labels.bloodType,
            selectedBloodType = uiState.bloodType,
            onDismiss = { showBloodTypePicker = false },
            onBloodTypeSelected = {
                onBloodTypeChanged(it)
                showBloodTypePicker = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(
            title = strings.emergencyCardEditorTitle,
            navigationIcon = Icons.Default.ArrowBack,
            navigationContentDescription = strings.backToEdit,
            onNavigationClick = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EmergencyTextField(labels.fullName, uiState.name, onNameChanged)
            EmergencyTextField(
                label = labels.dateOfBirth,
                value = uiState.notes,
                onValueChanged = onNotesChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            BloodTypeField(
                label = labels.bloodType,
                value = uiState.bloodType,
                placeholder = strings.notFilled,
                onClick = { showBloodTypePicker = true }
            )
            EmergencyTextField(labels.allergiesAndConditions, uiState.allergies, onAllergiesChanged, minLines = 3)
            EmergencyTextField(labels.emergencyContactPhone, uiState.emergencyContact, onEmergencyContactChanged)

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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.save)
                }
            }

            if (uiState.isSavedMessageVisible) {
                Text(
                    text = labels.savedLocally,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BloodTypeField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.ifBlank { placeholder },
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        enabled = false,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = label
            )
        },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = if (value.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledBorderColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun BloodTypePickerDialog(
    title: String,
    selectedBloodType: String,
    onDismiss: () -> Unit,
    onBloodTypeSelected: (String) -> Unit
) {
    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bloodTypes.forEach { bloodType ->
                    val selected = bloodType == selectedBloodType
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        onClick = { onBloodTypeSelected(bloodType) }
                    ) {
                        Text(
                            text = bloodType,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun EmergencyTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { strings.notFilled },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

private data class EmergencyContactParts(
    val name: String,
    val phone: String
)

private fun emergencyContactParts(
    raw: String,
    fallback: String,
    primaryContactLabel: String
): EmergencyContactParts {
    val parts = raw
        .split('\n', ',', ';')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> EmergencyContactParts(parts.first(), parts.drop(1).joinToString(" "))
        parts.size == 1 -> EmergencyContactParts(primaryContactLabel, parts.first())
        else -> EmergencyContactParts(primaryContactLabel, fallback)
    }
}

private data class EmergencyCardLabels(
    val emergencyCard: String,
    val personalInformation: String,
    val name: String,
    val fullName: String,
    val dateOfBirth: String,
    val medicalProfile: String,
    val bloodType: String,
    val allergies: String,
    val allergiesAndConditions: String,
    val emergencyContacts: String,
    val emergencyContactPhone: String,
    val primaryContact: String,
    val savedLocally: String = "信息卡已保存在本地"
)

private fun emergencyCardLabelsFor(languageTag: String): EmergencyCardLabels {
    return if (languageTag.startsWith("en")) {
        EmergencyCardLabels(
            emergencyCard = "Emergency Card",
            personalInformation = "Personal Information",
            name = "Name",
            fullName = "Full Name",
            dateOfBirth = "Date of Birth",
            medicalProfile = "Medical Profile",
            bloodType = "Blood Type",
            allergies = "Allergies",
            allergiesAndConditions = "Allergies & Medical Conditions",
            emergencyContacts = "Emergency Contacts",
            emergencyContactPhone = "Emergency Contact Phone",
            primaryContact = "Primary Contact",
            savedLocally = "Emergency card saved locally"
        )
    } else {
        EmergencyCardLabels(
            emergencyCard = "个人信息卡",
            personalInformation = "个人信息",
            name = "姓名",
            fullName = "姓名",
            dateOfBirth = "出生日期",
            medicalProfile = "医疗信息",
            bloodType = "血型",
            allergies = "过敏史",
            allergiesAndConditions = "过敏史与既往病史",
            emergencyContacts = "紧急联系人",
            emergencyContactPhone = "紧急联系人电话",
            primaryContact = "主要联系人"
        )
    }
}
