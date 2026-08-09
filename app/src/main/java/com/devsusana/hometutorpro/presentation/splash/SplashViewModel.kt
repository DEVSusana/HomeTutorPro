package com.devsusana.hometutorpro.presentation.splash

import androidx.lifecycle.ViewModel
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getOnboardingCompletedUseCase: IGetOnboardingCompletedUseCase
) : ViewModel() {

    fun isUserLoggedIn(): Boolean {
        return getCurrentUserUseCase().value != null
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return getOnboardingCompletedUseCase().first()
    }
}
