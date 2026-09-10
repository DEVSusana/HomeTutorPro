package com.devsusana.hometutorpro.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
import com.devsusana.hometutorpro.data.sync.DataSynchronizer
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.devsusana.hometutorpro.di.ApplicationScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.devsusana.hometutorpro.data.billing.BillingManager

/**
 * Data layer implementation of [AuthRepository].
 */
class AuthRepositoryImpl @Inject constructor(
    private val authManager: SecureAuthManager,
    private val firebaseAuth: FirebaseAuth,
    private val syncScheduler: SyncScheduler,
    private val syncMetadataDao: SyncMetadataDao,
    private val dataSynchronizer: DataSynchronizer,
    private val billingManager: BillingManager,
    private val restoreCredentialManager: com.devsusana.hometutorpro.core.auth.IRestoreCredentialManager,
    @param:ApplicationScope private val internalScope: CoroutineScope
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        internalScope.launch {
            billingManager.isPremium.collect { isPremium ->
                if (isPremium && _currentUser.value != null) {
                    try {
                        dataSynchronizer.performSync()
                    } catch (e: Exception) {
                        syncScheduler.scheduleSyncNow()
                    }
                }
            }
        }

        firebaseAuth.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                _currentUser.value = buildUser(
                    firebaseUser.uid,
                    firebaseUser.email ?: "",
                    firebaseUser.displayName ?: ""
                )
                
                internalScope.launch {
                    try {
                        restoreCredentialManager.saveRestoreCredential(
                            userId = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = firebaseUser.displayName
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to auto-register restore credential on startup: ${e.message}")
                    }
                }

                if (billingManager.isPremium.value) {
                    internalScope.launch {
                        try {
                            dataSynchronizer.performSync()
                        } catch (e: Exception) {
                            syncScheduler.scheduleSyncNow()
                        }
                    }
                }
            } else {
                checkLocalUser()
            }
        }
        
        if (firebaseAuth.currentUser == null) {
            checkLocalUser()
        }
    }

    private fun checkLocalUser() {
        if (authManager.isUserLoggedIn()) {
            val userId = authManager.getUserId()
            val name = authManager.getUserName()
            val email = authManager.getEmail()
            if (userId != null && name != null && email != null) {
                _currentUser.value = buildUser(userId, email, name)
                internalScope.launch {
                    try {
                        restoreCredentialManager.saveRestoreCredential(
                            userId = userId,
                            email = email,
                            displayName = name
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to auto-register restore credential for local user: ${e.message}")
                    }
                }
            } else {
                _currentUser.value = null
            }
        } else {
            _currentUser.value = null
        }
    }

    override suspend fun login(email: String, password: String): Result<User, DomainError> {
        var firebaseError: Exception? = null
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val domainUser = buildUser(user.uid, user.email ?: "", user.displayName ?: "")
                _currentUser.value = domainUser
                
                // Save locally for offline fallback
                authManager.saveCredentials(email, password, user.displayName ?: "", user.uid)
                
                internalScope.launch {
                    restoreCredentialManager.saveRestoreCredential(domainUser.uid, domainUser.email, domainUser.displayName)
                }

                if (billingManager.isPremium.value) {
                    internalScope.launch {
                        try {
                            dataSynchronizer.performSync()
                        } catch (e: Exception) {
                            syncScheduler.scheduleSyncNow()
                        }
                    }
                }
                
                return Result.Success(domainUser)
            }
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            // Firebase explicitly rejected the credentials — do NOT fall through to local
            return Result.Error(DomainError.InvalidCredentials)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // User account disabled or deleted in Firebase — do NOT fall through
            return Result.Error(DomainError.UserNotFound)
        } catch (e: Exception) {
            // Network error or other transient failure — try local fallback
            android.util.Log.e("AuthRepositoryImpl", "Firebase login failed, trying local fallback", e)
            firebaseError = e
        }

        return try {
            if (!authManager.validateEmail(email)) return Result.Error(DomainError.InvalidEmail)
            if (!authManager.validatePassword(password)) return Result.Error(DomainError.InvalidPassword)
            
            if (authManager.validateCredentials(email, password)) {
                val userId = authManager.getUserId() ?: return Result.Error(DomainError.UserNotFound)
                val name = authManager.getUserName() ?: return Result.Error(DomainError.UserNotFound)
                
                val user = buildUser(userId, email, name)
                _currentUser.value = user
                internalScope.launch {
                    restoreCredentialManager.saveRestoreCredential(user.uid, user.email, user.displayName)
                }
                Result.Success(user)
            } else {
                Result.Error(DomainError.InvalidCredentials)
            }
        } catch (e: Exception) {
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<User, DomainError> {
        return try {
            if (!authManager.validateEmail(email)) return Result.Error(DomainError.InvalidEmail)
            if (!authManager.validatePassword(password)) return Result.Error(DomainError.InvalidPassword)
            if (name.isBlank()) return Result.Error(DomainError.InvalidName)
            if (authManager.userExists()) return Result.Error(DomainError.UserAlreadyExists)
            

            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Save locally as fallback/offline with Firebase UID
                authManager.saveCredentials(email, password, name, firebaseUser.uid)
                
                val domainUser = buildUser(firebaseUser.uid, firebaseUser.email ?: "", name)
                _currentUser.value = domainUser
                internalScope.launch {
                    restoreCredentialManager.saveRestoreCredential(domainUser.uid, domainUser.email, domainUser.displayName)
                }
                Result.Success(domainUser)
            } else {
                android.util.Log.e("AuthRepositoryImpl", "Registration failed: firebaseUser is null")
                Result.Error(DomainError.Unknown)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            // Explicit error: do NOT silently login
            android.util.Log.e("AuthRepositoryImpl", "Registration failed: user already exists", e)
            Result.Error(DomainError.UserAlreadyExists)
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            android.util.Log.e("AuthRepositoryImpl", "Registration failed: network error", e)
            Result.Error(DomainError.NetworkError)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Registration failed with unexpected exception", e)
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        authManager.clearCredentials()
        internalScope.launch {
            restoreCredentialManager.clearRestoreCredential()
        }
        _currentUser.value = null
        syncScheduler.cancelAllSync()
        internalScope.launch(Dispatchers.IO) {
            try {
                syncMetadataDao.deleteAllMetadata()
            } catch (e: Exception) {
                android.util.Log.e("AuthRepositoryImpl", "Failed to clear sync metadata on logout", e)
            }
        }
    }
    
    override suspend fun updateProfile(
        name: String, 
        email: String, 
        workingStartTime: String, 
        workingEndTime: String,
        notes: String
    ): Result<Unit, DomainError> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Update Firebase Email if it changed
                if (firebaseUser.email != email) {
                    firebaseUser.verifyBeforeUpdateEmail(email).await()
                }

                // Update Firebase Profile Name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            // Always update local manager (for offline flavor or mirroring firebase)
            authManager.updateName(name)
            authManager.updateEmail(email)
            authManager.updateWorkingStartTime(workingStartTime)
            authManager.updateWorkingEndTime(workingEndTime)
            authManager.updateNotes(notes)

            // Update local StateFlow
            val current = _currentUser.value
            if (current != null) {
                _currentUser.value = current.copy(
                    displayName = name, 
                    email = email,
                    workingStartTime = workingStartTime,
                    workingEndTime = workingEndTime,
                    notes = notes
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit, DomainError> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val email = firebaseUser.email ?: return Result.Error(DomainError.UserNotFound)
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                firebaseUser.reauthenticate(credential).await()
                firebaseUser.updatePassword(newPassword).await()
            } else {
                val localEmail = authManager.getEmail()
                if (localEmail != null) {
                    if (!authManager.validateCredentials(localEmail, currentPassword)) {
                        return Result.Error(DomainError.InvalidCredentials)
                    }
                }
            }

            // Always update local manager
            authManager.updatePassword(newPassword)

            Result.Success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to update password: invalid credentials", e)
            Result.Error(DomainError.InvalidCredentials)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to update password", e)
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun deleteAccount(password: String): Result<Unit, DomainError> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val email = firebaseUser.email ?: return Result.Error(DomainError.UserNotFound)
                val credential = EmailAuthProvider.getCredential(email, password)
                firebaseUser.reauthenticate(credential).await()
                firebaseUser.delete().await()
            } else {
                val localEmail = authManager.getEmail()
                if (localEmail != null) {
                    if (!authManager.validateCredentials(localEmail, password)) {
                        return Result.Error(DomainError.InvalidCredentials)
                    }
                }
            }

            authManager.clearCredentials()
            internalScope.launch {
                restoreCredentialManager.clearRestoreCredential()
            }
            _currentUser.value = null

            internalScope.launch(Dispatchers.IO) {
                try {
                    syncMetadataDao.deleteAllMetadata()
                    syncScheduler.cancelAllSync()
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepositoryImpl", "Failed to clear DB on delete", e)
                }
            }
            Result.Success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to delete account: invalid credentials", e)
            Result.Error(DomainError.InvalidCredentials)
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to delete account: recent login required", e)
            Result.Error(DomainError.RecentLoginRequired)
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to delete account: network error", e)
            Result.Error(DomainError.NetworkError)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to delete account", e)
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit, DomainError> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to send reset email: user not found", e)
            Result.Error(DomainError.UserNotFound)
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to send reset email: network error", e)
            Result.Error(DomainError.NetworkError)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to send reset email", e)
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User, DomainError> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.Error(DomainError.UserNotFound)
            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: ""
            )

            authManager.saveCredentials(user.email ?: "", "", user.displayName ?: "", user.uid)

            _currentUser.value = user
            internalScope.launch {
                restoreCredentialManager.saveRestoreCredential(user.uid, user.email, user.displayName)
            }
            Result.Success(user)
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to sign in with Google: network error", e)
            Result.Error(DomainError.NetworkError)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Failed to sign in with Google", e)
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun restoreSessionSilently(): Result<User, DomainError> {
        return try {
            val current = _currentUser.value
            if (current != null && (firebaseAuth.currentUser != null || !billingManager.isPremium.value)) {
                return Result.Success(current)
            }

            val payload = restoreCredentialManager.getRestoreCredential()
            if (payload == null || payload.email.isBlank()) {
                return Result.Error(DomainError.UserNotFound)
            }

            // Verify that Firebase has an active authenticated session for this user.
            // On a new device or fresh install where Firebase session storage was not transferred,
            // Firebase Auth cannot authenticate without user credentials.
            // Returning success without Firebase Auth would cause Firestore queries and sync to fail.
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null && firebaseUser.uid == payload.userId) {
                authManager.saveCredentials(
                    email = payload.email,
                    password = "",
                    name = payload.displayName,
                    userId = payload.userId
                )

                val restoredUser = buildUser(payload.userId, payload.email, payload.displayName)
                _currentUser.value = restoredUser
                android.util.Log.d("AuthRepositoryImpl", "Zero-Tap restore successful with active Firebase session for: ${payload.email}")

                if (billingManager.isPremium.value) {
                    internalScope.launch {
                        try {
                            dataSynchronizer.performSync()
                        } catch (e: Exception) {
                            syncScheduler.scheduleSyncNow()
                        }
                    }
                }

                return Result.Success(restoredUser)
            }

            // If Firebase is not authenticated on this device, leave the user in the login flow
            android.util.Log.w(
                "AuthRepositoryImpl",
                "Restore credential found for ${payload.email} but Firebase session is null. Directing to login flow."
            )
            Result.Error(DomainError.UserNotFound)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepositoryImpl", "Silent session restoration failed", e)
            Result.Error(DomainError.Unknown)
        }
    }

    suspend fun linkToFirebase(email: String, password: String, name: String): Result<User, DomainError> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
                
                val domainUser = buildUser(user.uid, user.email ?: "", name)
                _currentUser.value = domainUser
                Result.Success(domainUser)
            } else {
                Result.Error(DomainError.Unknown)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.Error(DomainError.UserAlreadyExists)
        } catch (e: Exception) {
            Result.Error(DomainError.Unknown)
        }
    }

    /**
     * Single factory for constructing [User] from auth manager state.
     * Eliminates 6× duplication of User construction.
     */
    private fun buildUser(uid: String, email: String, displayName: String): User {
        return User(
            uid = uid,
            email = email,
            displayName = displayName,
            workingStartTime = authManager.getWorkingStartTime(),
            workingEndTime = authManager.getWorkingEndTime(),
            notes = authManager.getNotes()
        )
    }
}

