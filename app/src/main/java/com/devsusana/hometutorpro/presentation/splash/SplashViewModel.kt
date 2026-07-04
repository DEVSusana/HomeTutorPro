package com.devsusana.hometutorpro.presentation.splash

import androidx.lifecycle.ViewModel
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val settingsManager: SettingsManager
) : ViewModel() {

    fun isUserLoggedIn(): Boolean {
        return getCurrentUserUseCase().value != null
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return settingsManager.isOnboardingCompletedFlow.first()
    }
}
