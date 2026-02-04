package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.SharedResource

/**
 * Use case for saving a shared resource record after sharing a file.
 */
interface ISaveSharedResourceUseCase {
    suspend operator fun invoke(professorId: String?, resource: SharedResource): Result<Unit, DomainError>
}
