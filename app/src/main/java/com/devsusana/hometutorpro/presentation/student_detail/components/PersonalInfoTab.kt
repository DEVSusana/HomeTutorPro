package com.devsusana.hometutorpro.presentation.student_detail.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Tab displaying personal information of a student.
 */
@Composable
fun PersonalInfoTab(
    student: Student,
    isEditMode: Boolean,
    isNewStudent: Boolean,
    context: Context,
    onEvent: (StudentDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
    onToggleEditMode: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isEditMode && !isNewStudent) {
            OutlinedButton(
                onClick = onToggleEditMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.student_detail_edit_student_info))
            }
        }

        // Personal Info Section
        SectionCard(
            title = stringResource(R.string.student_detail_student_name),
            icon = Icons.Default.Person
        ) {
            // Active Status Toggle
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val stateOnDescription = stringResource(R.string.cd_state_on)
                    val stateOffDescription = stringResource(R.string.cd_state_off)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.student_detail_active_status),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.student_detail_active_status_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = student.isActive,
                        onCheckedChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(isActive = it))) },
                        modifier = Modifier
                            .testTag("active_status_switch")
                            .semantics {
                                stateDescription = if (student.isActive) {
                                    stateOnDescription
                                } else {
                                    stateOffDescription
                                }
                            }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            }

            OutlinedTextField(
                value = student.name,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(name = it))) },
                label = { Text(stringResource(id = R.string.student_detail_student_name)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("name_field"),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.cd_person_icon)) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = if (student.age > 0) student.age.toString() else "",
                onValueChange = {
                    val age = it.toIntOrNull() ?: 0
                    onEvent(StudentDetailEvent.StudentChange(student.copy(age = age)))
                },
                label = { Text(stringResource(id = R.string.student_detail_age)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("age_field"),
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = stringResource(R.string.cd_age_icon)) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = student.address,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(address = it))) },
                label = { Text(stringResource(id = R.string.student_detail_address)) },
                placeholder = { Text(stringResource(id = R.string.student_detail_address_hint)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("address_field"),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.cd_location_icon)) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = if (!isNewStudent && student.address.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(student.address))
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage(context.getString(R.string.google_maps_package))
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                }
                            },
                            modifier = Modifier.testTag("navigate_button")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = stringResource(R.string.student_detail_navigate_to_address))
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = student.educationalAttention,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(educationalAttention = it))) },
                label = { Text(stringResource(id = R.string.student_detail_special_needs)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("educational_attention_field"),
                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.cd_warning_icon)) }
            )

            if (isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                StudentColorPickerSection(
                    selectedColor = student.color,
                    onColorSelected = { onEvent(StudentDetailEvent.StudentChange(student.copy(color = it))) }
                )
            }
        }

        // Contact Section
        StudentContactSection(
            student = student,
            isEditMode = isEditMode,
            onEvent = onEvent
        )

        // Academic and Notes Section
        StudentAcademicNotesSection(
            student = student,
            isEditMode = isEditMode,
            onEvent = onEvent
        )

        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
}
