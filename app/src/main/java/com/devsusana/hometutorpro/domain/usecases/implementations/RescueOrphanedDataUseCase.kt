package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RescueOrphanedDataUseCase @Inject constructor(
    private val studentRepository: StudentRepository,
    private val getCurrentUserUseCase: IGetCurrentUserUseCase
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        val currentUser = getCurrentUserUseCase().value ?: return@withContext
        val professorId = currentUser.uid
        
        studentRepository.rescueOrphanedData(professorId)
    }
}
