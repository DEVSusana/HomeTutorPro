package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession

interface IGetActiveSessionUseCase {
    operator fun invoke(): ActiveSession?
}
