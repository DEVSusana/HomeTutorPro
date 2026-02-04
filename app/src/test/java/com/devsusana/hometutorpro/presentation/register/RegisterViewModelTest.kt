package com.devsusana.hometutorpro.presentation.register

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IRegisterUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private lateinit var registerUseCase: IRegisterUseCase
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        registerUseCase = mockk()
        viewModel = RegisterViewModel(registerUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onEvent OnNameChange should update name state`() {
        val name = "Test User"
        viewModel.onEvent(RegisterUiEvent.OnNameChange(name))
        assertEquals(name, viewModel.state.value.name)
    }

    @Test
    fun `onEvent OnRegisterClick should validate empty name`() = runTest {
        viewModel.onEvent(RegisterUiEvent.OnNameChange(""))
        viewModel.onEvent(RegisterUiEvent.OnRegisterClick)
        assertEquals(com.devsusana.hometutorpro.R.string.register_error_name_empty, viewModel.state.value.error)
    }

    @Test
    fun `onEvent OnRegisterClick should validate invalid email`() = runTest {
        viewModel.onEvent(RegisterUiEvent.OnNameChange("Test User"))
        viewModel.onEvent(RegisterUiEvent.OnEmailChange("invalid-email"))
        viewModel.onEvent(RegisterUiEvent.OnRegisterClick)
        assertEquals(com.devsusana.hometutorpro.R.string.register_error_invalid_email, viewModel.state.value.error)
    }

    @Test
    fun `onEvent OnRegisterClick should validate short password`() = runTest {
        viewModel.onEvent(RegisterUiEvent.OnNameChange("Test User"))
        viewModel.onEvent(RegisterUiEvent.OnEmailChange("test@test.com"))
        viewModel.onEvent(RegisterUiEvent.OnPasswordChange("123"))
        viewModel.onEvent(RegisterUiEvent.OnRegisterClick)
        assertEquals(com.devsusana.hometutorpro.R.string.register_error_short_password, viewModel.state.value.error)
    }

    @Test
    fun `onEvent OnRegisterClick should call registerUseCase and update state on success`() = runTest {
        // Given
        val name = "Test User"
        val email = "test@test.com"
        val password = "password123"
        viewModel.onEvent(RegisterUiEvent.OnNameChange(name))
        viewModel.onEvent(RegisterUiEvent.OnEmailChange(email))
        viewModel.onEvent(RegisterUiEvent.OnPasswordChange(password))
        
        val user = com.devsusana.hometutorpro.domain.entities.User(uid = "123", email = email, displayName = name)
        coEvery { registerUseCase(email, password, name) } returns Result.Success(user)

        // When
        viewModel.onEvent(RegisterUiEvent.OnRegisterClick)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { registerUseCase(email, password, name) }
        assertTrue(viewModel.state.value.registerSuccess)
        assertFalse(viewModel.state.value.isLoading)
    }
}
