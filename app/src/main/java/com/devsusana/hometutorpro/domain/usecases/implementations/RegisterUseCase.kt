package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IRegisterUseCase

import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) :
    IRegisterUseCase {
    override suspend operator fun invoke(email: String, password: String, name: String) = repository.register(email, password, name)
}
