package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

@Preview(showBackground = true, name = "Student Detail - View Student")
@Composable
fun StudentDetailContentNewStudentPreview() {
    HomeTutorProTheme {
        StudentDetailContent(
            state = StudentDetailState(student = Student(id = "new")),
            onEvent = {},
            onBack = {},
            isEditMode = true,
            onToggleEditMode = {},
            onSetEditMode = {},
            showPaymentDialog = false,
            selectedPaymentType = null,
            onPaymentClick = {},
            onDismissPaymentDialog = {},
            showStartClassDialog = false,
            onStartClassClick = {},
            onDismissStartClassDialog = {},
            showDeleteDialog = false,
            onShowDeleteDialog = {},
            onDismissDeleteDialog = {}
        )
    }
}

@Preview(showBackground = true, name = "Student Detail - View Student")
@Composable
fun StudentDetailContentViewStudentPreview() {
    val mockStudent = Student(
        id = "1",
        name = "Susana Gonzalez",
        age = 25,
        course = "Kotlin",
        pendingBalance = 150.0,
        pricePerHour = 50.0,
        address = "123 Main Street, Anytown",
        educationalAttention = "Extra time for exams",
        parentPhones = "123-456-7890",
        studentPhone = "987-654-3210",
        studentEmail = "student@example.com",
        subjects = "Math, Science"
    )
    HomeTutorProTheme {
        StudentDetailContent(
            state = StudentDetailState(student = mockStudent),
            onEvent = {},
            onBack = {},
            isEditMode = false,
            onToggleEditMode = {},
            onSetEditMode = {},
            showPaymentDialog = false,
            selectedPaymentType = null,
            onPaymentClick = {},
            onDismissPaymentDialog = {},
            showStartClassDialog = false,
            onStartClassClick = {},
            onDismissStartClassDialog = {},
            showDeleteDialog = false,
            onShowDeleteDialog = {},
            onDismissDeleteDialog = {}
        )
    }
}
