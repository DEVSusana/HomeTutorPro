package com.devsusana.hometutorpro.presentation.splash

import androidx.lifecycle.ViewModel
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IRestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val restoreSessionUseCase: IRestoreSessionUseCase,
    private val settingsManager: SettingsManager
) : ViewModel() {

    fun isUserLoggedIn(): Boolean {
        return getCurrentUserUseCase().value != null
    }

    suspend fun attemptZeroTapRestore(): Boolean {
        return when (restoreSessionUseCase()) {
            is Result.Success -> true
            is Result.Error -> false
        }
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return settingsManager.isOnboardingCompletedFlow.first()
    }
}
