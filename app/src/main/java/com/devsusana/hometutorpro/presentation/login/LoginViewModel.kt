package com.devsusana.hometutorpro.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.ILoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: ILoginUseCase
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
