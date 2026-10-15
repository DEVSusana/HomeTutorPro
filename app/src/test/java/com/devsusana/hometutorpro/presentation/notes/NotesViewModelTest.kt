package com.devsusana.hometutorpro.presentation.notes

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IUpdateProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [NotesViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getCurrentUserUseCase: IGetCurrentUserUseCase = mockk()
    private val updateProfileUseCase: IUpdateProfileUseCase = mockk()
    private val application: Application = mockk(relaxed = true)
    private val currentUserFlow = MutableStateFlow<User?>(null)
    private lateinit var viewModel: NotesViewModel

    private val testUser = User(
        uid = "user-1",
        email = "test@example.com",
        displayName = "Test User",
        workingStartTime = "09:00",
        workingEndTime = "18:00",
        notes = "Initial notes"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getCurrentUserUseCase() } returns currentUserFlow
        every { application.getString(R.string.notes_save_success) } returns "Notes saved successfully"
        every { application.getString(R.string.notes_save_error) } returns "Failed to save notes"
        currentUserFlow.value = testUser
        viewModel = NotesViewModel(getCurrentUserUseCase, updateProfileUseCase, application)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load updates state with user details and notes`() {
        val state = viewModel.state.value
        assertEquals("Initial notes", state.notes)
        assertEquals("Test User", state.name)
        assertEquals("test@example.com", state.email)
        assertEquals("09:00", state.workingStartTime)
        assertEquals("18:00", state.workingEndTime)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onNotesChange updates notes in state`() {
        viewModel.onNotesChange("Updated notes content")
        assertEquals("Updated notes content", viewModel.state.value.notes)
    }

    @Test
    fun `saveNotes emits successMessage when updateProfileUseCase succeeds`() = runTest {
        coEvery { updateProfileUseCase(any()) } returns Result.Success(Unit)

        viewModel.onNotesChange("New notes to save")
        viewModel.saveNotes()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Notes saved successfully", state.successMessage)
        assertNull(state.errorMessage)

        coVerify(exactly = 1) {
            updateProfileUseCase(
                match {
                    it.notes == "New notes to save" &&
                        it.name == "Test User" &&
                        it.email == "test@example.com"
                }
            )
        }
    }

    @Test
    fun `saveNotes emits errorMessage when updateProfileUseCase fails`() = runTest {
        coEvery { updateProfileUseCase(any()) } returns Result.Error(com.devsusana.hometutorpro.domain.core.DomainError.Unknown)

        viewModel.saveNotes()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.successMessage)
        assertEquals("Failed to save notes", state.errorMessage)
    }

    @Test
    fun `clearFeedback clears successMessage and errorMessage`() = runTest {
        coEvery { updateProfileUseCase(any()) } returns Result.Success(Unit)
        viewModel.saveNotes()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Notes saved successfully", viewModel.state.value.successMessage)

        viewModel.clearFeedback()

        assertNull(viewModel.state.value.successMessage)
        assertNull(viewModel.state.value.errorMessage)
    }
}
