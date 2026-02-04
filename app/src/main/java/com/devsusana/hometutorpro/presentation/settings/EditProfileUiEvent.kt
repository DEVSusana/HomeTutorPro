package com.devsusana.hometutorpro.presentation.settings

sealed class EditProfileUiEvent {
    data class NameChanged(val name: String) : EditProfileUiEvent()
    data class EmailChanged(val email: String) : EditProfileUiEvent()
    data class PasswordChanged(val password: String) : EditProfileUiEvent()
    data class WorkingStartTimeChanged(val time: String) : EditProfileUiEvent()
    data class WorkingEndTimeChanged(val time: String) : EditProfileUiEvent()
    object TogglePasswordVisibility : EditProfileUiEvent()
    object SaveProfile : EditProfileUiEvent()
    object DismissFeedback : EditProfileUiEvent()
}
