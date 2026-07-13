package com.devsusana.hometutorpro.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.ILoginUseCase
import com.devsusana.hometutorpro.domain.usecases.ISendPasswordResetEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: ILoginUseCase,
    private val sendPasswordResetEmailUseCase: ISendPasswordResetEmailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.OnEmailChange -> {
                _state.update { it.copy(email = event.email) } 
            }
            is LoginUiEvent.OnPasswordChange -> {
                _state.update { it.copy(password = event.password) }
            }
            is LoginUiEvent.OnTogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is LoginUiEvent.OnLoginClick -> {
                login()
            }
            is LoginUiEvent.OnRegisterClick -> {
            }
            is LoginUiEvent.OnForgotPasswordClick -> {
                _state.update { it.copy(showForgotPasswordDialog = true, error = null, errorMessage = null) }
            }
            is LoginUiEvent.OnDismissForgotPasswordDialog -> {
                _state.update { it.copy(showForgotPasswordDialog = false, error = null, errorMessage = null) }
            }
            is LoginUiEvent.OnSendPasswordResetEmail -> {
                sendPasswordReset(event.email)
            }
            is LoginUiEvent.OnClearResetStatus -> {
                _state.update { it.copy(passwordResetSuccessMessage = null, error = null, errorMessage = null) }
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _state.update { 
                it.copy(
                    error = R.string.login_forgot_password_error_invalid_email,
                    errorMessage = R.string.login_forgot_password_error_invalid_email
                )
            }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isSendingPasswordReset = true, error = null, errorMessage = null) }
            when (val result = sendPasswordResetEmailUseCase(trimmedEmail)) {
                is Result.Success -> {
                    _state.update { 
                        it.copy(
                            isSendingPasswordReset = false,
                            showForgotPasswordDialog = false,
                            passwordResetSuccessMessage = R.string.login_forgot_password_success
                        ) 
                    }
                }
                is Result.Error -> {
                    val errorMsg = when (result.error) {
                        com.devsusana.hometutorpro.domain.core.DomainError.UserNotFound -> R.string.login_forgot_password_error_user_not_found
                        com.devsusana.hometutorpro.domain.core.DomainError.NetworkError -> R.string.login_forgot_password_error_network
                        else -> R.string.login_forgot_password_error_generic
                    }
                    _state.update { 
                        it.copy(
                            isSendingPasswordReset = false,
                            error = errorMsg,
                            errorMessage = errorMsg
                        ) 
                    }
                }
            }
        }
    }

    private fun login() {
        viewModelScope.launch {
            val email = _state.value.email.trim()
            val password = _state.value.password
            
            if (email.isBlank() || password.isBlank()) {
                _state.update { 
                    it.copy(
                        error = R.string.login_error_empty_fields,
                        errorMessage = R.string.login_error_empty_fields
                    ) 
                }
                return@launch
            }
            
            _state.update { it.copy(isLoading = true, error = null, errorMessage = null) }
            
            when (loginUseCase(email, password)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = R.string.login_error_invalid_credentials,
                            errorMessage = R.string.login_error_failed
                        ) 
                    }
                }
            }
        }
    }

    fun clearFeedback() {
        _state.update { it.copy(errorMessage = null) }
    }
}
