package com.devsusana.hometutorpro.presentation.register

sealed interface RegisterUiEvent {
    data class OnNameChange(val name: String) : RegisterUiEvent
    data class OnEmailChange(val email: String) : RegisterUiEvent
    data class OnPasswordChange(val password: String) : RegisterUiEvent
    object OnTogglePasswordVisibility : RegisterUiEvent
    object OnRegisterClick : RegisterUiEvent
    object OnBackClick : RegisterUiEvent
}
