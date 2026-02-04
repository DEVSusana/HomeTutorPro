package com.devsusana.hometutorpro.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IRegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: IRegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.OnNameChange -> {
                _state.update { it.copy(name = event.name) }
            }
            is RegisterUiEvent.OnEmailChange -> {
                _state.update { it.copy(email = event.email) }
            }
            is RegisterUiEvent.OnPasswordChange -> {
                _state.update { it.copy(password = event.password) }
            }
            is RegisterUiEvent.OnTogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is RegisterUiEvent.OnRegisterClick -> {
                register()
            }
            is RegisterUiEvent.OnBackClick -> {
                // Handled by UI
            }
        }
    }

    private fun register() {
        val name = _state.value.name
        val email = _state.value.email
        val password = _state.value.password

        if (name.isBlank()) {
            _state.update { it.copy(error = R.string.register_error_name_empty, errorMessage = R.string.register_error_name_empty) }
            return
        }

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        if (!emailRegex.matches(email)) {
            _state.update { it.copy(error = R.string.register_error_invalid_email, errorMessage = R.string.register_error_invalid_email) }
            return
        }

        if (password.length < 6) {
            _state.update { it.copy(error = R.string.register_error_short_password, errorMessage = R.string.register_error_short_password) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, errorMessage = null) }
            
            when (registerUseCase(email, password, name)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = R.string.register_error_registration_failed,
                            errorMessage = R.string.register_error_registration_failed_message
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
