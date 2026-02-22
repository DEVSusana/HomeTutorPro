package com.devsusana.hometutorpro.presentation.resources

import android.net.Uri
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IDeleteResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.IUploadResourceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceViewModelTest {

    private lateinit var getResourcesUseCase: IGetResourcesUseCase
    private lateinit var uploadResourceUseCase: IUploadResourceUseCase
    private lateinit var deleteResourceUseCase: IDeleteResourceUseCase
    private lateinit var getCurrentUserUseCase: IGetCurrentUserUseCase
    private lateinit var viewModel: ResourceViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getResourcesUseCase = mockk()
        uploadResourceUseCase = mockk()
        deleteResourceUseCase = mockk()
        getCurrentUserUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load resources when user is logged in`() = runTest {
        // Given
        val userId = "user123"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test User")
        val resource = Resource(id = "res1", name = "Resource 1", professorId = userId)

        every { getCurrentUserUseCase() } returns MutableStateFlow<User?>(user)
        every { getResourcesUseCase(userId) } returns flowOf(listOf(resource))

        // When
        viewModel = ResourceViewModel(
            getResourcesUseCase,
            uploadResourceUseCase,
            deleteResourceUseCase,
            getCurrentUserUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.state.value.resources.size)
        assertEquals(resource, viewModel.state.value.resources.first())
    }

    @Test
    fun `uploadResource should call use case and update state on success`() = runTest {
        // Given
        val userId = "user123"
        val user = User(uid = userId, email = "test@test.com", displayName = "Test User")
        val uri = mockk<Uri>()
        val uriString = "content://test/file"
        val resourceName = "Test File"
        val fileType = "pdf"

        every { uri.toString() } returns uriString
        every { getCurrentUserUseCase() } returns MutableStateFlow<User?>(user)
        every { getResourcesUseCase(userId) } returns flowOf(emptyList())
        coEvery { uploadResourceUseCase(userId, resourceName, fileType, uriString) } returns Result.Success(Unit)

        viewModel = ResourceViewModel(
            getResourcesUseCase,
            uploadResourceUseCase,
            deleteResourceUseCase,
            getCurrentUserUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.uploadResource(uri, resourceName, fileType)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { uploadResourceUseCase(userId, resourceName, fileType, uriString) }
        assertEquals(com.devsusana.hometutorpro.R.string.resources_success_upload, viewModel.state.value.successMessage)
    }
}
