package com.devsusana.hometutorpro.presentation.settings

data class EditProfileState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val workingStartTime: String = "08:00",
    val workingEndTime: String = "23:00",
    val successMessage: String? = null,
    val errorMessage: String? = null
)
