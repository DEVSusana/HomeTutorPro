package com.devsusana.hometutorpro.presentation.student_detail.delegates

import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

interface IStudentResourceDelegate {
    fun loadSharedResources(
        professorId: String,
        studentId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun shareResource(
        professorId: String,
        studentId: String,
        fileType: String,
        size: Long,
        method: ShareMethod,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )

    fun deleteSharedResource(
        professorId: String,
        resourceId: String,
        state: MutableStateFlow<StudentDetailState>,
        scope: CoroutineScope
    )
}
