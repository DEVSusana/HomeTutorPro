package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

@Preview(showBackground = true, name = "Personal Info Tab - Edit Mode")
@Composable
fun PersonalInfoTabEditPreview() {
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
fun PersonalInfoTabViewPreview() {
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
