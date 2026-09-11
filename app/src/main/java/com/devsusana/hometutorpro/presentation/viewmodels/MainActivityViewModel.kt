package com.devsusana.hometutorpro.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.usecases.IGetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel associated with MainActivity.
 * Exposes application configuration states such as the theme mode flow.
 *
 * @property getThemeModeUseCase Use case to observe the current application theme mode setting.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val getThemeModeUseCase: IGetThemeModeUseCase
) : ViewModel() {

    /**
     * Flow emitting the current [AppThemeMode] configuration.
     */
    val themeModeFlow: Flow<AppThemeMode> = getThemeModeUseCase()
}
