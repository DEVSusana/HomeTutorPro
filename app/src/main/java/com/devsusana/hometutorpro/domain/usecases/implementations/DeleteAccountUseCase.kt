package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IDeleteAccountUseCase
import javax.inject.Inject

/**
 * Default implementation of [IDeleteAccountUseCase].
 */
class DeleteAccountUseCase @Inject constructor(private val repository: AuthRepository) :
    IDeleteAccountUseCase {
    override suspend operator fun invoke() = repository.deleteAccount()
}
