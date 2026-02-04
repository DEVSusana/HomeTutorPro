package com.devsusana.hometutorpro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdatePasswordUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val updateProfileUseCase: IUpdateProfileUseCase,
    private val updatePasswordUseCase: IUpdatePasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                user?.let { currentUser ->
                    _state.update { s ->
                        s.copy(
                            name = currentUser.displayName ?: "",
                            email = currentUser.email ?: "",
                            workingStartTime = currentUser.workingStartTime,
                            workingEndTime = currentUser.workingEndTime
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: EditProfileUiEvent) {
        when (event) {
            is EditProfileUiEvent.NameChanged -> {
                _state.update { it.copy(name = event.name) }
            }
            is EditProfileUiEvent.EmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is EditProfileUiEvent.PasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            is EditProfileUiEvent.TogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is EditProfileUiEvent.WorkingStartTimeChanged -> {
                _state.update { it.copy(workingStartTime = event.time) }
            }
            is EditProfileUiEvent.WorkingEndTimeChanged -> {
                _state.update { it.copy(workingEndTime = event.time) }
            }
            is EditProfileUiEvent.SaveProfile -> {
                saveProfile()
            }
            is EditProfileUiEvent.DismissFeedback -> {
                _state.update { it.copy(successMessage = null, errorMessage = null) }
            }
        }
    }

    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 1. Update Profile (Name, Email, and Working Hours)
            val profileResult = updateProfileUseCase(
                state.value.name, 
                state.value.email,
                state.value.workingStartTime,
                state.value.workingEndTime
            )
            
            if (profileResult is Result.Error) {
                _state.update { it.copy(isLoading = false, errorMessage = "Error updating profile") }
                return@launch
            }

            // 2. Update Password if provided
            if (state.value.password.isNotEmpty()) {
                val passwordResult = updatePasswordUseCase(state.value.password)
                if (passwordResult is Result.Error) {
                    _state.update { it.copy(isLoading = false, errorMessage = "Error updating password") }
                    return@launch
                }
            }

            _state.update { 
                it.copy(
                    isLoading = false, 
                    successMessage = "Profile updated successfully!",
                    errorMessage = null 
                ) 
            }
        }
    }
}
