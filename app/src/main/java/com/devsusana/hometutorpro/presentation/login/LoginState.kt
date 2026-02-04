package com.devsusana.hometutorpro.presentation.login

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: Int? = null,
    val errorMessage: Int? = null
)
