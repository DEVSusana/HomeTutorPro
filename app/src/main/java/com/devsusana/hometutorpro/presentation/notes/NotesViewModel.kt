package com.devsusana.hometutorpro.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesState(
    val notes: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    // Internal state to hold user profile data for update
    val name: String = "",
    val email: String = "",
    val workingStartTime: String = "08:00",
    val workingEndTime: String = "23:00"
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val updateProfileUseCase: IUpdateProfileUseCase,
    private val application: android.app.Application
) : ViewModel() {

    private val _state = MutableStateFlow(NotesState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getCurrentUserUseCase().collect { user ->
                user?.let { currentUser ->
                    _state.update { s ->
                        s.copy(
                            notes = currentUser.notes,
                            name = currentUser.displayName ?: "",
                            email = currentUser.email ?: "",
                            workingStartTime = currentUser.workingStartTime,
                            workingEndTime = currentUser.workingEndTime,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onNotesChange(newNotes: String) {
        _state.update { it.copy(notes = newNotes) }
    }

    fun saveNotes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentState = state.value
            
            val result = updateProfileUseCase(
                currentState.name,
                currentState.email,
                currentState.workingStartTime,
                currentState.workingEndTime,
                currentState.notes
            )

            when (result) {
                is Result.Success -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = application.getString(R.string.notes_save_success)
                        ) 
                    }
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = application.getString(R.string.notes_save_error)
                        ) 
                    }
                }
            }
        }
    }

    fun clearFeedback() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
