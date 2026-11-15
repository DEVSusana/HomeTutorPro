package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
 * Section card containing parent and student phone numbers and email address.
 */
@Composable
fun StudentContactSection(
    student: Student,
    isEditMode: Boolean,
    onEvent: (StudentDetailEvent) -> Unit
) {
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
}
