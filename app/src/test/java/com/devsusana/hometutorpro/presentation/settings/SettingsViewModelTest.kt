package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsManager: SettingsManager
    private lateinit var setLanguageUseCase: ISetLanguageUseCase
    private lateinit var setThemeModeUseCase: ISetThemeModeUseCase
    private lateinit var setClassEndNotificationsUseCase: ISetClassEndNotificationsUseCase
    private lateinit var setDebugPremiumUseCase: ISetDebugPremiumUseCase
    private lateinit var createBackupUseCase: ICreateBackupUseCase
    private lateinit var restoreBackupUseCase: IRestoreBackupUseCase
    private lateinit var showTestNotificationUseCase: IShowTestNotificationUseCase
    private lateinit var logoutUseCase: ILogoutUseCase
    private lateinit var deleteAccountUseCase: IDeleteAccountUseCase
    private lateinit var updatePasswordUseCase: IUpdatePasswordUseCase
    private lateinit var application: Application
    
    private lateinit var viewModel: SettingsViewModel
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        application = mockk(relaxed = true)
        settingsManager = mockk()

        // Stub SettingsManager flows
        every { settingsManager.languageFlow } returns flowOf("es")
        every { settingsManager.themeModeFlow } returns flowOf(SettingsManager.ThemeMode.SYSTEM)
        every { settingsManager.classEndNotificationsFlow } returns flowOf(true)
        every { settingsManager.isDebugPremiumFlow } returns flowOf(false)

        setLanguageUseCase = mockk(relaxed = true)
        setThemeModeUseCase = mockk(relaxed = true)
        setClassEndNotificationsUseCase = mockk(relaxed = true)
        setDebugPremiumUseCase = mockk(relaxed = true)
        createBackupUseCase = mockk(relaxed = true)
        restoreBackupUseCase = mockk(relaxed = true)
        showTestNotificationUseCase = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        deleteAccountUseCase = mockk(relaxed = true)
        updatePasswordUseCase = mockk(relaxed = true)

        viewModel = SettingsViewModel(
            settingsManager = settingsManager,
            setLanguageUseCase = setLanguageUseCase,
            setThemeModeUseCase = setThemeModeUseCase,
            setClassEndNotificationsUseCase = setClassEndNotificationsUseCase,
            setDebugPremiumUseCase = setDebugPremiumUseCase,
            createBackupUseCase = createBackupUseCase,
            restoreBackupUseCase = restoreBackupUseCase,
            showTestNotificationUseCase = showTestNotificationUseCase,
            logoutUseCase = logoutUseCase,
            deleteAccountUseCase = deleteAccountUseCase,
            updatePasswordUseCase = updatePasswordUseCase,
            application = application
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `changePassword sets error current blank when currentPassword is blank`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.changePassword("", "newPassword", "newPassword")
        assertEquals(R.string.settings_change_password_error_current_blank, viewModel.state.value.changePasswordError)
        collectJob.cancel()
    }

    @Test
    fun `changePassword sets error mismatch when passwords do not match`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.changePassword("current", "newPassword", "different")
        assertEquals(R.string.settings_change_password_error_mismatch, viewModel.state.value.changePasswordError)
        collectJob.cancel()
    }

    @Test
    fun `changePassword sets error short when password is too short`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.changePassword("current", "123", "123")
        assertEquals(R.string.settings_change_password_error_short, viewModel.state.value.changePasswordError)
        collectJob.cancel()
    }

    @Test
    fun `changePassword calls updatePasswordUseCase and sets success when inputs are valid`() = runTest {
        coEvery { updatePasswordUseCase("current", "newPassword") } returns Result.Success(Unit)
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        viewModel.changePassword("current", "newPassword", "newPassword")

        coVerify { updatePasswordUseCase("current", "newPassword") }
        assertEquals(true, viewModel.state.value.changePasswordSuccess)
        assertNull(viewModel.state.value.changePasswordError)
        collectJob.cancel()
    }

    @Test
    fun `changePassword sets error incorrect current when repository returns InvalidCredentials`() = runTest {
        coEvery { updatePasswordUseCase("wrongCurrent", "newPassword") } returns Result.Error(DomainError.InvalidCredentials)
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        viewModel.changePassword("wrongCurrent", "newPassword", "newPassword")

        assertEquals(R.string.settings_change_password_error_incorrect_current, viewModel.state.value.changePasswordError)
        collectJob.cancel()
    }
}
