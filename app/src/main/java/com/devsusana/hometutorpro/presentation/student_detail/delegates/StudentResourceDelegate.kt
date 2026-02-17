package com.devsusana.hometutorpro.presentation.student_detail.delegates

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.usecases.IDeleteSharedResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetSharedResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveSharedResourceUseCase
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class StudentResourceDelegate @Inject constructor(
    private val getSharedResourcesUseCase: IGetSharedResourcesUseCase,
    private val saveSharedResourceUseCase: ISaveSharedResourceUseCase,
    private val deleteSharedResourceUseCase: IDeleteSharedResourceUseCase,
    private val application: Application
) : IStudentResourceDelegate {

    override fun loadSharedResources(
        professorId: String,
        studentId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            getSharedResourcesUseCase(professorId, studentId).collect { resources ->
                state.value = state.value.copy(sharedResources = resources)
            }
        }
    }

    override fun shareResource(
        professorId: String,
        studentId: String,
        fileType: String,
        size: Long,
        method: ShareMethod,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            val fileName = state.value.selectedFileName
            val notes = state.value.shareNotes

            if (fileName.isBlank()) {
                state.value = state.value.copy(errorMessage = application.getString(R.string.student_detail_error_no_file_selected))
                return@launch
            }

            state.value = state.value.copy(isLoading = true)

            val sharedResource = SharedResource(
                studentId = studentId,
                fileName = fileName,
                fileType = fileType,
                fileSizeBytes = size,
                sharedVia = method,
                notes = notes
            )

            when (saveSharedResourceUseCase(professorId, sharedResource)) {
                is Result.Success -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        showShareDialog = false,
                        selectedFileUri = null,
                        selectedFileName = "",
                        shareNotes = "",
                        successMessage = application.getString(R.string.student_detail_success_resource_shared)
                    )
                }
                is Result.Error -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_share_failed)
                    )
                }
            }
        }
    }

    override fun deleteSharedResource(
        professorId: String,
        resourceId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true)
            when (deleteSharedResourceUseCase(professorId, resourceId)) {
                is Result.Success -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        successMessage = application.getString(R.string.student_detail_success_resource_deleted)
                    )
                }
                is Result.Error -> {
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = application.getString(R.string.student_detail_error_delete_resource_failed)
                    )
                }
            }
        }
    }
}
