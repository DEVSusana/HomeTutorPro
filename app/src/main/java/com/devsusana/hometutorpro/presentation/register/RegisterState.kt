package com.devsusana.hometutorpro.presentation.register

data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val registerSuccess: Boolean = false,
    val error: Int? = null,
    val errorMessage: Int? = null
)
