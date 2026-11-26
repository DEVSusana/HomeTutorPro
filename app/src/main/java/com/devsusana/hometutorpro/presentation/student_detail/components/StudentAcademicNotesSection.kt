package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent

/**
 * Section cards containing student academic details (course, subjects) and general notes.
 *
 * @param student The student entity containing academic and notes information.
 * @param isEditMode Whether the form fields are editable or read-only.
 * @param onEvent Callback to dispatch UI events to the ViewModel.
 */
@Composable
fun StudentAcademicNotesSection(
    student: Student,
    isEditMode: Boolean,
    onEvent: (StudentDetailEvent) -> Unit
) {
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

    Spacer(modifier = Modifier.height(16.dp))

    // Notes Section
    SectionCard(
        title = stringResource(R.string.student_detail_notes),
        icon = Icons.AutoMirrored.Filled.Notes
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
                    Icons.AutoMirrored.Filled.Notes,
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
}
