package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.User
import kotlinx.coroutines.flow.StateFlow

interface IGetCurrentUserUseCase {
    operator fun invoke(): StateFlow<User?>
}
