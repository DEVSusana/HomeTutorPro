package com.devsusana.hometutorpro.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.domain.usecases.ISetOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setOnboardingCompletedUseCase: ISetOnboardingCompletedUseCase
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            setOnboardingCompletedUseCase(true)
        }
    }
}
