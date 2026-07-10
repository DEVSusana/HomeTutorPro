package com.devsusana.hometutorpro.presentation.login

sealed interface LoginUiEvent {
    data class OnEmailChange(val email: String) : LoginUiEvent
    data class OnPasswordChange(val password: String) : LoginUiEvent
    object OnTogglePasswordVisibility : LoginUiEvent
    object OnLoginClick : LoginUiEvent
    object OnRegisterClick : LoginUiEvent
    object OnForgotPasswordClick : LoginUiEvent
    object OnDismissForgotPasswordDialog : LoginUiEvent
    data class OnSendPasswordResetEmail(val email: String) : LoginUiEvent
    object OnClearResetStatus : LoginUiEvent
}
