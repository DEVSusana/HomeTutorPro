package com.devsusana.hometutorpro.presentation.student_detail.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
        }

        // Contact Section
        SectionCard(
            title = stringResource(R.string.student_detail_parent_phone),
            icon = Icons.Default.Phone
        ) {
            OutlinedTextField(
                value = student.parentPhones,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(parentPhones = it))) },
                label = { Text(stringResource(id = R.string.student_detail_parent_phone)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("parent_phones_field"),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.cd_phone_icon)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = student.studentPhone,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(studentPhone = it))) },
                label = { Text(stringResource(id = R.string.student_detail_student_phone)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("student_phone_field"),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.cd_phone_icon)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            student.studentEmail?.let {
                OutlinedTextField(
                    value = it,
                    onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(studentEmail = it))) },
                    label = { Text(stringResource(id = R.string.student_detail_student_email)) },
                    enabled = isEditMode,
                    modifier = Modifier.fillMaxWidth().testTag("student_email_field"),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = stringResource(R.string.cd_email_icon)) }
                )
            }
        }

        // Academic Section
        SectionCard(
            title = stringResource(R.string.student_detail_course),
            icon = Icons.Default.School
        ) {
            OutlinedTextField(
                value = student.course,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(course = it))) },
                label = { Text(stringResource(id = R.string.student_detail_course)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("course_field"),
                leadingIcon = { Icon(Icons.Default.School, contentDescription = stringResource(R.string.cd_school_icon)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = student.subjects,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(subjects = it))) },
                label = { Text(stringResource(id = R.string.student_detail_subjects)) },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().testTag("subjects_field"),
                leadingIcon = { Icon(Icons.Default.Book, contentDescription = stringResource(R.string.cd_book_icon)) }
            )
        }
        
        // Notes Section
        SectionCard(
            title = stringResource(R.string.student_detail_notes),
            icon = Icons.Default.Notes
        ) {
            OutlinedTextField(
                value = student.notes,
                onValueChange = { onEvent(StudentDetailEvent.StudentChange(student.copy(notes = it))) },
                label = { Text(stringResource(id = R.string.student_detail_notes)) },
                readOnly = !isEditMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 150.dp)
                    .testTag("notes_field"),
                placeholder = { Text(stringResource(R.string.student_detail_notes_hint)) },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Notes, 
                        contentDescription = stringResource(R.string.cd_file_icon)
                    ) 
                },
                colors = if (!isEditMode) {
                    OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextFieldDefaults.colors()
                }
            )
        }
        
        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true, name = "Personal Info Tab - Edit Mode")
@Composable
private fun PersonalInfoTabEditPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        address = "Calle Principal 123",
        parentPhones = "+34 600 123 456",
        studentPhone = "+34 600 654 321",
        studentEmail = "susana@example.com",
        subjects = "Álgebra, Geometría",
        notes = "Estudiante destacada",
        educationalAttention = "",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        isActive = true
    )
    
    HomeTutorProTheme {
        PersonalInfoTab(
            student = mockStudent,
            isEditMode = true,
            isNewStudent = false,
            context = LocalContext.current,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Personal Info Tab - View Mode")
@Composable
private fun PersonalInfoTabViewPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        address = "Calle Principal 123",
        parentPhones = "+34 600 123 456",
        studentPhone = "+34 600 654 321",
        studentEmail = "susana@example.com",
        subjects = "Álgebra, Geometría",
        notes = "Estudiante destacada",
        educationalAttention = "",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        isActive = true
    )
    
    HomeTutorProTheme {
        PersonalInfoTab(
            student = mockStudent,
            isEditMode = false,
            isNewStudent = false,
            context = LocalContext.current,
            onEvent = {}
        )
    }
}
