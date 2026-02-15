package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.data.sync.DataSynchronizer
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
import com.devsusana.hometutorpro.data.billing.BillingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for AuthRepositoryImpl
 * Tests authentication logic, user management, and error handling
 */
@RunWith(RobolectricTestRunner::class)
class AuthRepositoryImplTest {

    private lateinit var repository: AuthRepositoryImpl
    private lateinit var authManager: SecureAuthManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var syncMetadataDao: SyncMetadataDao
    private lateinit var dataSynchronizer: DataSynchronizer
    private lateinit var billingManager: BillingManager
    private val testDispatcher = StandardTestDispatcher()
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authManager = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)
        syncMetadataDao = mockk(relaxed = true)
        dataSynchronizer = mockk(relaxed = true)
        billingManager = mockk(relaxed = true)
        
        // Mock billingManager.isPremium to return a StateFlow
        every { billingManager.isPremium } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        
        // Mock FirebaseAuth to return null current user and capture listener
        every { firebaseAuth.currentUser } returns null
        every { firebaseAuth.addAuthStateListener(any()) } answers {
            authStateListener = firstArg()
        }
        
        // Mock authManager to prevent checkLocalUser from running
        every { authManager.isUserLoggedIn() } returns false
        every { authManager.getWorkingStartTime() } returns "08:00"
        every { authManager.getWorkingEndTime() } returns "23:00"
        
        // Make Firebase fail by default to test local fallback
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(Exception("Firebase not available"))
        // Removed default mock for createUserWithEmailAndPassword, as it conflicts with specific test mocks.
        // If other tests need Firebase createUserWithEmailAndPassword to fail, they should mock it explicitly.
    }

    private fun createRepository(scope: kotlinx.coroutines.CoroutineScope) = AuthRepositoryImpl(
        authManager = authManager,
        firebaseAuth = firebaseAuth,
        syncScheduler = syncScheduler,
        syncMetadataDao = syncMetadataDao,
        dataSynchronizer = dataSynchronizer,
        billingManager = billingManager,
        internalScope = scope
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ============================================================================
    // Login Tests
    // ============================================================================

    @Test
    fun `login with valid local credentials returns success`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "password123"
        val userId = "user123"
        val name = "Test User"

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true
        every { authManager.validateCredentials(email, password) } returns true
        every { authManager.getUserId() } returns userId
        every { authManager.getUserName() } returns name

        repository = createRepository(backgroundScope)

        // When
        val result = repository.login(email, password)

        // Then
        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals(userId, user.uid)
        assertEquals(email, user.email)
        assertEquals(name, user.displayName)
    }

    @Test
    fun `login with invalid email returns error`() = runTest {
        // Given
        val email = "invalid-email"
        val password = "password123"

        every { authManager.validateEmail(email) } returns false

        repository = createRepository(backgroundScope)

        // When
        val result = repository.login(email, password)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.InvalidEmail, (result as Result.Error).error)
    }

    @Test
    fun `login with invalid password returns error`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "123" // Too short

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns false

        repository = createRepository(backgroundScope)

        // When
        val result = repository.login(email, password)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.InvalidPassword, (result as Result.Error).error)
    }

    @Test
    fun `login with invalid credentials returns error`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "wrongpassword"

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true
        every { authManager.validateCredentials(email, password) } returns false

        repository = createRepository(backgroundScope)

        // When
        val result = repository.login(email, password)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.InvalidCredentials, (result as Result.Error).error)
    }

    // ============================================================================
    // Register Tests
    // ============================================================================

    @Test
    fun `register with valid data returns success`() = runTest {
        // Given
        val email = "newuser@test.com"
        val password = "password123"
        val name = "New User"
        val userId = "user123"

        // Mock Firebase success for this specific test
        val mockFirebaseUser = mockk<FirebaseUser>()
        every { mockFirebaseUser.uid } returns userId
        every { mockFirebaseUser.email } returns email
        every { mockFirebaseUser.displayName } returns name
        every { mockFirebaseUser.updateProfile(any()) } returns Tasks.forResult<Void>(null)

        val mockAuthResult = mockk<AuthResult>()
        every { mockAuthResult.user } returns mockFirebaseUser

        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)

        // Mock AuthManager methods for user creation and saving
        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true
        every { authManager.userExists() } returns false
        every { authManager.saveCredentials(email, password, name, userId) } returns userId
        every { authManager.getWorkingStartTime() } returns "08:00"
        every { authManager.getWorkingEndTime() } returns "23:00"
        every { authManager.getNotes() } returns ""

        repository = createRepository(backgroundScope)

        // When
        val result = repository.register(email, password, name)

        // Then
        assertTrue(result is Result.Success)
        val actualUser = (result as Result.Success).data
        val expectedUser = User(
            uid = userId,
            email = email,
            displayName = name,
            workingStartTime = "08:00",
            workingEndTime = "23:00",
            notes = ""
        )
        
        assertEquals(expectedUser, actualUser)
        
        // Verify currentUser is updated
        assertEquals(expectedUser, repository.currentUser.first())

        verify { authManager.saveCredentials(email, password, name, userId) }
    }

    @Test
    fun `register with invalid email returns error`() = runTest {
        // Given
        val email = "invalid-email"
        val password = "password123"
        val name = "Test User"

        every { authManager.validateEmail(email) } returns false

        repository = createRepository(backgroundScope)

        // When
        val result = repository.register(email, password, name)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.InvalidEmail, (result as Result.Error).error)
    }

    @Test
    fun `register with short password returns error`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "123"
        val name = "Test User"

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns false

        repository = createRepository(backgroundScope)

        // When
        val result = repository.register(email, password, name)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.InvalidPassword, (result as Result.Error).error)
    }

    @Test
    fun `register with blank name returns error`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "password123"
        val name = ""

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true

        repository = createRepository(backgroundScope)

        // When
        val result = repository.register(email, password, name)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.InvalidName, (result as Result.Error).error)
    }

    @Test
    fun `register with existing user returns error`() = runTest {
        // Given
        val email = "existing@test.com"
        val password = "password123"
        val name = "Test User"

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true
        every { authManager.userExists() } returns true

        repository = createRepository(backgroundScope)

        // When
        val result = repository.register(email, password, name)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.UserAlreadyExists, (result as Result.Error).error)
    }

    // ============================================================================
    // Logout Tests
    // ============================================================================

    @Test
    fun `logout clears current user and calls auth manager`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "password123"
        
        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true
        every { authManager.validateCredentials(email, password) } returns true
        every { authManager.getUserId() } returns "user123"
        every { authManager.getUserName() } returns "Test User"
        
        repository = createRepository(backgroundScope)

        // Login first
        repository.login(email, password)
        
        // Verify user is logged in
        val userBeforeLogout = repository.currentUser.first()
        assertTrue(userBeforeLogout != null)

        // When
        repository.logout()

        // Then
        val userAfterLogout = repository.currentUser.first()
        assertTrue(userAfterLogout == null)
        verify { authManager.clearCredentials() }
    }

    // ============================================================================
    // Current User Tests
    // ============================================================================

    @Test
    fun `currentUser flow emits null initially`() = runTest {
        repository = createRepository(backgroundScope)

        // When
        val currentUser = repository.currentUser.first()

        // Then
        assertTrue(currentUser == null)
    }

    @Test
    fun `currentUser flow emits user after successful login`() = runTest {
        // Given
        val email = "test@test.com"
        val password = "password123"
        val userId = "user123"
        val name = "Test User"

        every { authManager.validateEmail(email) } returns true
        every { authManager.validatePassword(password) } returns true
        every { authManager.validateCredentials(email, password) } returns true
        every { authManager.getUserId() } returns userId
        every { authManager.getUserName() } returns name

        repository = createRepository(backgroundScope)

        // When
        repository.login(email, password)
        val currentUser = repository.currentUser.first()

        // Then
        assertTrue(currentUser != null)
        assertEquals(userId, currentUser?.uid)
        assertEquals(email, currentUser?.email)
        assertEquals(name, currentUser?.displayName)
    }
}
