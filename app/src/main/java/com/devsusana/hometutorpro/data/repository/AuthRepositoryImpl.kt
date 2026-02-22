package com.devsusana.hometutorpro.data.repository

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.devsusana.hometutorpro.data.billing.BillingManager

class AuthRepositoryImpl @Inject constructor(
    private val authManager: SecureAuthManager,
    private val firebaseAuth: FirebaseAuth,
    private val syncScheduler: SyncScheduler,
    private val syncMetadataDao: SyncMetadataDao,
    private val dataSynchronizer: DataSynchronizer,
    private val billingManager: BillingManager,
    @ApplicationScope private val internalScope: CoroutineScope
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
                Result.Success(domainUser)
            } else {
                Result.Error(DomainError.Unknown)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            // Explicit error: do NOT silently login
            Result.Error(DomainError.UserAlreadyExists)
        } catch (e: Exception) {
            Result.Error(DomainError.Unknown)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        authManager.clearCredentials()
        _currentUser.value = null
        
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            syncMetadataDao.deleteAllMetadata()
            syncScheduler.cancelAllSync()
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
                    firebaseUser.updateEmail(email).await()
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

    override suspend fun updatePassword(newPassword: String): Result<Unit, DomainError> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                firebaseUser.updatePassword(newPassword).await()
            }

            // Always update local manager
            authManager.updatePassword(newPassword)

            Result.Success(Unit)
        } catch (e: Exception) {
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
