package com.devsusana.hometutorpro.presentation.resources

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.domain.usecases.IDeleteResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.IUploadResourceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResourcesState(
    val resources: List<Resource> = emptyList(),
    val isLoading: Boolean = false,
    val error: Any? = null,
    val successMessage: Any? = null,
    val errorMessage: Any? = null
)

@HiltViewModel
class ResourceViewModel @Inject constructor(
    private val getResourcesUseCase: IGetResourcesUseCase,
    private val uploadResourceUseCase: IUploadResourceUseCase,
    private val deleteResourceUseCase: IDeleteResourceUseCase,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ResourcesState())
    val state: StateFlow<ResourcesState> = _state.asStateFlow()

    private val professorId: String?
        get() = getCurrentUserUseCase.invoke().value?.uid

    init {
        loadResources()
    }

    private fun loadResources() {
        val uid = professorId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getResourcesUseCase(uid).collect { resources ->
                _state.value = _state.value.copy(resources = resources, isLoading = false)
            }
        }
    }

    fun uploadResource(uri: Uri, name: String) {
        val uid = professorId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = uploadResourceUseCase(uid, name, uri.toString())
            when (result) {
                is Result.Success -> {
                    // Reload handled by Flow
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = R.string.resources_success_upload
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false, 
                        errorMessage = R.string.resources_error_upload_failed
                    )
                }
            }
        }
    }

    fun deleteResource(resourceId: String) {
        val uid = professorId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = deleteResourceUseCase(uid, resourceId)
            when (result) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = R.string.resources_success_delete
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = R.string.resources_error_delete_failed
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(successMessage = null, errorMessage = null, error = null)
    }
}
