package com.devsusana.hometutorpro.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.ClearCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.GetCredentialCustomException
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RestoreCredentialManagerTest {

    private lateinit var context: Context
    private lateinit var credentialManager: CredentialManager
    private lateinit var restoreCredentialManager: RestoreCredentialManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        credentialManager = mockk(relaxed = true)
        restoreCredentialManager = RestoreCredentialManager(context, credentialManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saveRestoreCredential returns true on success`() = runTest {
        val mockResponse = mockk<androidx.credentials.CreateCredentialResponse>(relaxed = true)
        coEvery { credentialManager.createCredential(any<Context>(), any<CreateCredentialRequest>()) } returns mockResponse

        val result = restoreCredentialManager.saveRestoreCredential(
            userId = "user_123",
            email = "user@example.com",
            displayName = "Jane Doe"
        )

        assertTrue(result)
        coVerify(exactly = 1) { credentialManager.createCredential(any<Context>(), any<CreateCredentialRequest>()) }
    }

    @Test
    fun `saveRestoreCredential returns false when CreateCredentialException is thrown`() = runTest {
        coEvery {
            credentialManager.createCredential(any<Context>(), any<CreateCredentialRequest>())
        } throws CreateCredentialCustomException("custom_type", "error")

        val result = restoreCredentialManager.saveRestoreCredential(
            userId = "user_123",
            email = "user@example.com",
            displayName = "Jane Doe"
        )

        assertFalse(result)
    }

    @Test
    fun `getRestoreCredential returns RestorePayload when RestoreCredential found`() = runTest {
        val mockResponse = mockk<GetCredentialResponse>()
        val mockCredential = mockk<RestoreCredential>()
        val jsonPayload = """{"user":{"id":"user_123","name":"user@example.com","displayName":"Jane Doe"},"token":""}"""

        every { mockCredential.authenticationResponseJson } returns jsonPayload
        every { mockResponse.credential } returns mockCredential
        coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns mockResponse

        val payload = restoreCredentialManager.getRestoreCredential()

        assertNotNull(payload)
        assertEquals("user_123", payload!!.userId)
        assertEquals("user@example.com", payload.email)
        assertEquals("Jane Doe", payload.displayName)
    }

    @Test
    fun `getRestoreCredential returns null when GetCredentialException is thrown`() = runTest {
        coEvery {
            credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
        } throws GetCredentialCustomException("custom_type", "error")

        val payload = restoreCredentialManager.getRestoreCredential()

        assertNull(payload)
    }

    @Test
    fun `clearRestoreCredential returns true on success`() = runTest {
        coEvery { credentialManager.clearCredentialState(any()) } returns mockk()

        val result = restoreCredentialManager.clearRestoreCredential()

        assertTrue(result)
        coVerify(exactly = 1) { credentialManager.clearCredentialState(any()) }
    }

    @Test
    fun `clearRestoreCredential returns false on exception`() = runTest {
        coEvery {
            credentialManager.clearCredentialState(any())
        } throws ClearCredentialCustomException("custom_type", "error")

        val result = restoreCredentialManager.clearRestoreCredential()

        assertFalse(result)
    }
}
