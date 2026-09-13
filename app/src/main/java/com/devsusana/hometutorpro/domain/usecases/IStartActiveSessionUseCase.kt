package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession

interface IStartActiveSessionUseCase {
    operator fun invoke(session: ActiveSession)
}
