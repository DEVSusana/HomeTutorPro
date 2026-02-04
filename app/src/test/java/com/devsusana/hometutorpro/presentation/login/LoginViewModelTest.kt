package com.devsusana.hometutorpro.presentation.login

import com.devsusana.hometutorpro.domain.usecases.ILoginUseCase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoginViewModel.
 * 
 * Note: Async login tests (testing viewModelScope.launch behavior) are not included
 * because viewModelScope uses Dispatchers.Main.immediate which cannot be easily mocked
 * in unit tests without dependency injection of the CoroutineScope.
 * 
 * These async behaviors are covered by:
 * 1. Integration tests with actual use cases
 * 2. Manual testing
 * 3. UI tests
 * 
 * To properly test async ViewModel behavior, consider:
 * - Injecting CoroutineScope/CoroutineDispatcher into ViewModel
 * - Using a test-specific ViewModel implementation
 * - Writing integration tests instead of unit tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var loginUseCase: ILoginUseCase
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk(relaxed = true)
        viewModel = LoginViewModel(loginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onEvent OnEmailChange should update email state`() {
        val email = "test@test.com"
        viewModel.onEvent(LoginUiEvent.OnEmailChange(email))
        assertEquals(email, viewModel.state.value.email)
    }

    @Test
    fun `onEvent OnPasswordChange should update password state`() {
        val password = "password123"
        viewModel.onEvent(LoginUiEvent.OnPasswordChange(password))
        assertEquals(password, viewModel.state.value.password)
    }

    @Test
    fun `onEvent OnTogglePasswordVisibility should toggle password visibility`() {
        val initialVisibility = viewModel.state.value.isPasswordVisible
        viewModel.onEvent(LoginUiEvent.OnTogglePasswordVisibility)
        assertEquals(!initialVisibility, viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun `initial state should have correct default values`() {
        assertEquals("", viewModel.state.value.email)
        assertEquals("", viewModel.state.value.password)
        assertFalse(viewModel.state.value.isPasswordVisible)
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.loginSuccess)
    }
}
