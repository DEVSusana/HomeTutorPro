package com.devsusana.hometutorpro.presentation.splash

import androidx.lifecycle.ViewModel
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase
) : ViewModel() {

    fun isUserLoggedIn(): Boolean {
        return getCurrentUserUseCase().value != null
    }
}
