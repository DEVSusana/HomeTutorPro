package com.devsusana.hometutorpro.presentation.settings

import android.app.Application
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.IUriReader
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
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

    private lateinit var getLanguageUseCase: IGetLanguageUseCase
    private lateinit var getThemeModeUseCase: IGetThemeModeUseCase
    private lateinit var getClassEndNotificationsUseCase: IGetClassEndNotificationsUseCase
    private lateinit var getDebugPremiumUseCase: IGetDebugPremiumUseCase
    private lateinit var getSueEnabledUseCase: IGetSueEnabledUseCase
    private lateinit var getSueFabVisibleUseCase: IGetSueFabVisibleUseCase
    private lateinit var setLanguageUseCase: ISetLanguageUseCase
    private lateinit var setThemeModeUseCase: ISetThemeModeUseCase
    private lateinit var setClassEndNotificationsUseCase: ISetClassEndNotificationsUseCase
    private lateinit var setDebugPremiumUseCase: ISetDebugPremiumUseCase
    private lateinit var setSueEnabledUseCase: ISetSueEnabledUseCase
    private lateinit var setSueFabVisibleUseCase: ISetSueFabVisibleUseCase
    private lateinit var getSueModelStatusUseCase: IGetSueModelStatusUseCase
    private lateinit var downloadSueModelUseCase: IDownloadSueModelUseCase
    private lateinit var cancelSueModelDownloadUseCase: ICancelSueModelDownloadUseCase
    private lateinit var deleteSueModelUseCase: IDeleteSueModelUseCase
    private lateinit var createBackupUseCase: ICreateBackupUseCase
    private lateinit var restoreBackupUseCase: IRestoreBackupUseCase
    private lateinit var showTestNotificationUseCase: IShowTestNotificationUseCase
    private lateinit var uriReader: IUriReader
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

        getLanguageUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf("es")
        }
        getThemeModeUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf(AppThemeMode.SYSTEM)
        }
        getClassEndNotificationsUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf(true)
        }
        getDebugPremiumUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf(false)
        }
        getSueEnabledUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf(true)
        }
        getSueFabVisibleUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf(true)
        }
        getSueModelStatusUseCase = mockk {
            every { this@mockk.invoke() } returns flowOf(com.devsusana.hometutorpro.domain.entities.SueModelStatus.NotDownloaded)
        }

        setLanguageUseCase = mockk(relaxed = true)
        setThemeModeUseCase = mockk(relaxed = true)
        setClassEndNotificationsUseCase = mockk(relaxed = true)
        setDebugPremiumUseCase = mockk(relaxed = true)
        setSueEnabledUseCase = mockk(relaxed = true)
        setSueFabVisibleUseCase = mockk(relaxed = true)
        downloadSueModelUseCase = mockk(relaxed = true) {
            coEvery { this@mockk.invoke(any()) } returns flowOf(com.devsusana.hometutorpro.domain.entities.SueModelStatus.NotDownloaded)
        }
        cancelSueModelDownloadUseCase = mockk(relaxed = true)
        deleteSueModelUseCase = mockk(relaxed = true)
        val checkSueCompatibilityUseCase = mockk<com.devsusana.hometutorpro.domain.usecases.ICheckSueCompatibilityUseCase> {
            every { this@mockk.invoke() } returns com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility(isSupported = true)
        }
        createBackupUseCase = mockk(relaxed = true)
        restoreBackupUseCase = mockk(relaxed = true)
        showTestNotificationUseCase = mockk(relaxed = true)
        uriReader = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        deleteAccountUseCase = mockk(relaxed = true)
        updatePasswordUseCase = mockk(relaxed = true)

        viewModel = SettingsViewModel(
            getLanguageUseCase = getLanguageUseCase,
            getThemeModeUseCase = getThemeModeUseCase,
            getClassEndNotificationsUseCase = getClassEndNotificationsUseCase,
            getDebugPremiumUseCase = getDebugPremiumUseCase,
            setLanguageUseCase = setLanguageUseCase,
            setThemeModeUseCase = setThemeModeUseCase,
            setClassEndNotificationsUseCase = setClassEndNotificationsUseCase,
            setDebugPremiumUseCase = setDebugPremiumUseCase,
            createBackupUseCase = createBackupUseCase,
            restoreBackupUseCase = restoreBackupUseCase,
            showTestNotificationUseCase = showTestNotificationUseCase,
            getSueEnabledUseCase = getSueEnabledUseCase,
            getSueFabVisibleUseCase = getSueFabVisibleUseCase,
            setSueEnabledUseCase = setSueEnabledUseCase,
            setSueFabVisibleUseCase = setSueFabVisibleUseCase,
            getSueModelStatusUseCase = getSueModelStatusUseCase,
            downloadSueModelUseCase = downloadSueModelUseCase,
            cancelSueModelDownloadUseCase = cancelSueModelDownloadUseCase,
            deleteSueModelUseCase = deleteSueModelUseCase,
            checkSueCompatibilityUseCase = checkSueCompatibilityUseCase,
            uriReader = uriReader,
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

    @Test
    fun `onSueEnabledToggle calls setSueEnabledUseCase`() = runTest {
        viewModel.onSueEnabledToggle(false)
        coVerify { setSueEnabledUseCase(false) }
    }

    @Test
    fun `onSueFabVisibleToggle calls setSueFabVisibleUseCase`() = runTest {
        viewModel.onSueFabVisibleToggle(false)
        coVerify { setSueFabVisibleUseCase(false) }
    }

    @Test
    fun `downloadSueModel calls downloadSueModelUseCase`() = runTest {
        viewModel.downloadSueModel("https://custom.url")
        coVerify { downloadSueModelUseCase("https://custom.url") }
    }

    @Test
    fun `cancelSueModelDownload calls cancelSueModelDownloadUseCase`() = runTest {
        viewModel.cancelSueModelDownload()
        coVerify { cancelSueModelDownloadUseCase() }
    }

    @Test
    fun `onDeleteModelClick sets showDeleteModelConfirmDialog to true`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onDeleteModelClick()
        assertEquals(true, viewModel.state.value.showDeleteModelConfirmDialog)
        collectJob.cancel()
    }

    @Test
    fun `onConfirmDeleteModel calls deleteSueModelUseCase and dismisses dialog`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onDeleteModelClick()
        assertEquals(true, viewModel.state.value.showDeleteModelConfirmDialog)

        viewModel.onConfirmDeleteModel()
        coVerify { deleteSueModelUseCase() }
        assertEquals(false, viewModel.state.value.showDeleteModelConfirmDialog)
        collectJob.cancel()
    }

    @Test
    fun `onDismissDeleteModelDialog dismisses dialog without deleting`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onDeleteModelClick()
        assertEquals(true, viewModel.state.value.showDeleteModelConfirmDialog)

        viewModel.onDismissDeleteModelDialog()
        assertEquals(false, viewModel.state.value.showDeleteModelConfirmDialog)
        coVerify(exactly = 0) { deleteSueModelUseCase() }
        collectJob.cancel()
    }

    @Test
    fun `state reflects device compatibility from checkSueCompatibilityUseCase`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertEquals(true, viewModel.state.value.sueDeviceCompatibility.isSupported)
        collectJob.cancel()
    }
}

