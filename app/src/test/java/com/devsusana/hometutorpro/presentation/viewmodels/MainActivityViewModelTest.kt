package com.devsusana.hometutorpro.presentation.viewmodels

import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.usecases.IGetThemeModeUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [MainActivityViewModel].
 */
class MainActivityViewModelTest {

    private val getThemeModeUseCase = mockk<IGetThemeModeUseCase>()

    /**
     * Verifies that the themeModeFlow emits the exact theme mode value returned by the use case.
     */
    @Test
    fun themeModeFlow_emitsValueFromUseCase() = runBlocking {
        val expectedTheme = AppThemeMode.DARK
        every { getThemeModeUseCase() } returns flowOf(expectedTheme)

        val viewModel = MainActivityViewModel(getThemeModeUseCase)
        val actualTheme = viewModel.themeModeFlow.first()

        assertEquals(expectedTheme, actualTheme)
        verify { getThemeModeUseCase() }
    }
}
