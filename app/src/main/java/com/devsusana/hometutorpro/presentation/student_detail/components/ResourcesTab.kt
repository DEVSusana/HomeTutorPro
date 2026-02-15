package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailEvent
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Tab for managing shared resources with a student.
 */
@Composable
fun ResourcesTab(
    student: Student,
    isNewStudent: Boolean,
    sharedResources: List<SharedResource>,
    onSelectFileClick: () -> Unit,
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
        if (isNewStudent) {
            // Show message for new students
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = stringResource(R.string.student_detail_save_first_resource),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Shared Resources for existing students
            SectionCard(
                title = stringResource(R.string.student_detail_shared_resources),
                icon = Icons.Default.Folder
            ) {
                Button(
                    onClick = onSelectFileClick,
                    modifier = Modifier.fillMaxWidth().testTag("select_file_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.cd_upload_icon))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.student_detail_select_file))
                }

                Spacer(modifier = Modifier.height(16.dp))

                SharedResourcesList(
                    sharedResources = sharedResources,
                    onDeleteResource = { onEvent(StudentDetailEvent.DeleteSharedResource(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true, name = "Resources Tab - New Student")
@Composable
private fun ResourcesTabNewStudentPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        ResourcesTab(
            student = mockStudent,
            isNewStudent = true,
            sharedResources = emptyList(),
            onSelectFileClick = {},
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Resources Tab - With Resources")
@Composable
private fun ResourcesTabWithResourcesPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    val mockResources = listOf(
        SharedResource(
            id = "1",
            studentId = "1",
            fileName = "Ejercicios_Algebra.pdf",
            fileType = "application/pdf",
            fileSizeBytes = 1024000,
            sharedAt = System.currentTimeMillis(),
            sharedVia = ShareMethod.EMAIL,
            notes = "Ejercicios para practicar"
        ),
        SharedResource(
            id = "2",
            studentId = "1",
            fileName = "Apuntes_Geometria.pdf",
            fileType = "application/pdf",
            fileSizeBytes = 2048000,
            sharedAt = System.currentTimeMillis() - 86400000,
            sharedVia = ShareMethod.WHATSAPP,
            notes = ""
        )
    )
    
    HomeTutorProTheme {
        ResourcesTab(
            student = mockStudent,
            isNewStudent = false,
            sharedResources = mockResources,
            onSelectFileClick = {},
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Resources Tab - No Resources")
@Composable
private fun ResourcesTabNoResourcesPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana González",
        age = 25,
        course = "Matemáticas",
        pendingBalance = 0.0,
        pricePerHour = 20.0,
        studentEmail = "susana@example.com",
        isActive = true
    )
    
    HomeTutorProTheme {
        ResourcesTab(
            student = mockStudent,
            isNewStudent = false,
            sharedResources = emptyList(),
            onSelectFileClick = {},
            onEvent = {}
        )
    }
}
