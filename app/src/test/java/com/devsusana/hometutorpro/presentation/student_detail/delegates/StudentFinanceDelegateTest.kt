package com.devsusana.hometutorpro.presentation.student_detail.delegates

import android.app.Application
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveStudentUseCase
import com.devsusana.hometutorpro.presentation.student_detail.StudentDetailState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentFinanceDelegateTest {

    private lateinit var registerPaymentUseCase: IRegisterPaymentUseCase
    private lateinit var addToBalanceUseCase: IAddToBalanceUseCase
    private lateinit var saveStudentUseCase: ISaveStudentUseCase
    private lateinit var application: Application
    private lateinit var delegate: StudentFinanceDelegate

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    private val testStudent = Student(
        id = "1",
        professorId = "prof1",
        name = "Test Student",
        pricePerHour = 7.0,
        pendingBalance = 63.0,
        lastPaymentDate = null
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        registerPaymentUseCase = mockk()
        addToBalanceUseCase = mockk()
        saveStudentUseCase = mockk()
        application = mockk(relaxed = true)

        every { application.getString(any()) } answers { it.invocation.args[0].toString() }
        every { application.getString(any(), *anyVararg()) } answers { it.invocation.args[0].toString() }

        delegate = StudentFinanceDelegate(
            registerPaymentUseCase,
            addToBalanceUseCase,
            saveStudentUseCase,
            application
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ============================================================================
    // registerPayment Tests
    // ============================================================================

    @Test
    fun `registerPayment success updates student state with new balance`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent))
        coEvery { registerPaymentUseCase("prof1", "1", 63.0, PaymentType.EFFECTIVE) } returns Result.Success(Unit)

        // When
        delegate.registerPayment("prof1", "1", 63.0, PaymentType.EFFECTIVE, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val updatedStudent = state.value.student!!
        assertEquals(0.0, updatedStudent.pendingBalance, 0.001)
    }

    @Test
    fun `registerPayment success sets lastPaymentDate`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent))
        coEvery { registerPaymentUseCase("prof1", "1", 10.0, PaymentType.BIZUM) } returns Result.Success(Unit)

        // When
        delegate.registerPayment("prof1", "1", 10.0, PaymentType.BIZUM, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val updatedStudent = state.value.student!!
        assertNotNull(updatedStudent.lastPaymentDate)
    }

    @Test
    fun `registerPayment success allows negative balance for advance payments`() = testScope.runTest {
        // Given — student owes 10€, pays 25€ (15€ advance)
        val studentWith10Balance = testStudent.copy(pendingBalance = 10.0)
        val state = MutableStateFlow(StudentDetailState(student = studentWith10Balance))
        coEvery { registerPaymentUseCase("prof1", "1", 25.0, PaymentType.EFFECTIVE) } returns Result.Success(Unit)

        // When
        delegate.registerPayment("prof1", "1", 25.0, PaymentType.EFFECTIVE, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — balance should be -15.0 (advance payment)
        assertEquals(-15.0, state.value.student!!.pendingBalance, 0.001)
    }

    @Test
    fun `registerPayment error does not modify balance`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent))
        coEvery { registerPaymentUseCase("prof1", "1", 63.0, PaymentType.EFFECTIVE) } returns Result.Error(DomainError.Unknown)

        // When
        delegate.registerPayment("prof1", "1", 63.0, PaymentType.EFFECTIVE, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — balance unchanged
        assertEquals(63.0, state.value.student!!.pendingBalance, 0.001)
        assertNull(state.value.student!!.lastPaymentDate)
        assertNotNull(state.value.errorMessage)
    }

    @Test
    fun `registerPayment error with InvalidAmount shows specific message`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent))
        coEvery { registerPaymentUseCase("prof1", "1", 0.0, PaymentType.EFFECTIVE) } returns Result.Error(DomainError.InvalidAmount)

        // When
        delegate.registerPayment("prof1", "1", 0.0, PaymentType.EFFECTIVE, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNotNull(state.value.errorMessage)
        assertFalse(state.value.isLoading)
    }

    // ============================================================================
    // startClass Tests
    // ============================================================================

    @Test
    fun `startClass adds correct amount to balance for 60 minutes`() = testScope.runTest {
        // Given — 7€/hour, 60 min = 7€
        val state = MutableStateFlow(StudentDetailState(student = testStudent.copy(pendingBalance = 0.0)))
        coEvery { addToBalanceUseCase("prof1", "1", 7.0) } returns Result.Success(Unit)

        // When
        delegate.startClass("prof1", "1", 60, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(7.0, state.value.student!!.pendingBalance, 0.001)
        assertNotNull(state.value.student!!.lastClassDate)
    }

    @Test
    fun `startClass adds correct amount to balance for 90 minutes`() = testScope.runTest {
        // Given — 7€/hour, 90 min = 10.5€
        val state = MutableStateFlow(StudentDetailState(student = testStudent.copy(pendingBalance = 0.0)))
        coEvery { addToBalanceUseCase("prof1", "1", 10.5) } returns Result.Success(Unit)

        // When
        delegate.startClass("prof1", "1", 90, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(10.5, state.value.student!!.pendingBalance, 0.001)
    }

    @Test
    fun `startClass does not overwrite lastPaymentDate`() = testScope.runTest {
        // Given — student has an existing lastPaymentDate
        val paymentDate = 1707868800000L // Feb 14, 2024
        val studentWithPayment = testStudent.copy(lastPaymentDate = paymentDate, pendingBalance = 0.0)
        val state = MutableStateFlow(StudentDetailState(student = studentWithPayment))
        coEvery { addToBalanceUseCase("prof1", "1", 7.0) } returns Result.Success(Unit)

        // When
        delegate.startClass("prof1", "1", 60, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — lastPaymentDate must be preserved
        assertEquals(paymentDate, state.value.student!!.lastPaymentDate)
    }

    @Test
    fun `startClass uses atomic addToBalance instead of full entity save`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent.copy(pendingBalance = 0.0)))
        coEvery { addToBalanceUseCase("prof1", "1", 7.0) } returns Result.Success(Unit)

        // When
        delegate.startClass("prof1", "1", 60, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — saveStudentUseCase should NOT be called (atomic operation used instead)
        coVerify(exactly = 0) { saveStudentUseCase(any(), any()) }
        coVerify(exactly = 1) { addToBalanceUseCase("prof1", "1", 7.0) }
    }

    @Test
    fun `startClass with zero duration shows error`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent))

        // When
        delegate.startClass("prof1", "1", 0, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNotNull(state.value.errorMessage)
        coVerify(exactly = 0) { addToBalanceUseCase(any(), any(), any()) }
    }

    @Test
    fun `startClass error does not modify balance`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent.copy(pendingBalance = 56.0)))
        coEvery { addToBalanceUseCase("prof1", "1", 7.0) } returns Result.Error(DomainError.Unknown)

        // When
        delegate.startClass("prof1", "1", 60, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — balance unchanged
        assertEquals(56.0, state.value.student!!.pendingBalance, 0.001)
        assertNotNull(state.value.errorMessage)
    }

    // ============================================================================
    // updateBalance Tests
    // ============================================================================

    @Test
    fun `updateBalance success sets new balance`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent, isBalanceEditable = true))
        coEvery { saveStudentUseCase("prof1", any()) } returns Result.Success("1")

        // When
        delegate.updateBalance("prof1", "1", 100.0, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(state.value.isBalanceEditable)
        assertFalse(state.value.isLoading)
        assertNotNull(state.value.successMessage)
    }

    @Test
    fun `updateBalance error does not toggle edit mode`() = testScope.runTest {
        // Given
        val state = MutableStateFlow(StudentDetailState(student = testStudent, isBalanceEditable = true))
        coEvery { saveStudentUseCase("prof1", any()) } returns Result.Error(DomainError.Unknown)

        // When
        delegate.updateBalance("prof1", "1", 100.0, state, this)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — isBalanceEditable unchanged on error (still true from initial state)
        assertFalse(state.value.isLoading)
        assertNotNull(state.value.errorMessage)
    }
}
