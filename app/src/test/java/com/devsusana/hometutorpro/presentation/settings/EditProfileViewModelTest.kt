package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdatePasswordUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdateProfileUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EditProfileViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var updateProfileUseCase: IUpdateProfileUseCase
    private lateinit var updatePasswordUseCase: IUpdatePasswordUseCase
    private lateinit var application: Application
    private lateinit var viewModel: EditProfileViewModel
    private val dispatcher = UnconfinedTestDispatcher()
    private val userFlow = MutableStateFlow<User?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        application = mockk(relaxed = true)

        getCurrentUserUseCase = mockk()
        updateProfileUseCase = mockk()
        updatePasswordUseCase = mockk()

        every { application.getString(R.string.edit_profile_success) } returns "Profile updated successfully!"
        every { application.getString(R.string.edit_profile_email_verify_notice) } returns
            "Profile updated. Please verify the new email to complete the change."
        every { getCurrentUserUseCase.invoke() } returns userFlow
        coEvery { updateProfileUseCase(any(), any(), any(), any(), any()) } returns Result.Success(Unit)
        coEvery { updatePasswordUseCase(any()) } returns Result.Success(Unit)

        userFlow.value = User(uid = "u1", email = "user@domain.com", displayName = "User")
        viewModel = EditProfileViewModel(
            getCurrentUserUseCase = getCurrentUserUseCase,
            updateProfileUseCase = updateProfileUseCase,
            updatePasswordUseCase = updatePasswordUseCase,
            application = application
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveProfile uses standard success message when email did not change`() = runTest {
        viewModel.onEvent(EditProfileUiEvent.EmailChanged("user@domain.com"))
        viewModel.onEvent(EditProfileUiEvent.SaveProfile)

        advanceUntilIdle()

        val expected = application.getString(R.string.edit_profile_success)
        assertEquals(expected, viewModel.state.value.successMessage)
    }

    @Test
    fun `saveProfile uses verification message when email changed`() = runTest {
        viewModel.onEvent(EditProfileUiEvent.EmailChanged("new@domain.com"))
        viewModel.onEvent(EditProfileUiEvent.SaveProfile)

        advanceUntilIdle()

        val expected = application.getString(R.string.edit_profile_email_verify_notice)
        assertEquals(expected, viewModel.state.value.successMessage)
    }
}
