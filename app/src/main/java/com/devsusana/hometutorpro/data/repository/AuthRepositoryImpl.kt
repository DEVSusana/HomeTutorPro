package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.UpdateUserParams
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Data layer implementation of [AuthRepository].
 */
class AuthRepositoryImpl @Inject constructor(
    private val authManager: SecureAuthManager,
    private val firebaseAuth: FirebaseAuth,
    private val authValidator: com.devsusana.hometutorpro.domain.core.AuthValidator
    // Removed sync orchestration dependencies to respect SRP
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    /** The current authenticated user. */
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Load local user initially or register auth state listener
        checkLocalUser()
    }

    /** Verifies local authentication state and updates the currentUser flow. */
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

    /** Authenticates user with Firebase or falls back to local storage credentials. */
    override suspend fun login(email: String, password: String): Result<User, DomainError> {
        var firebaseError: Exception? = null
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val domainUser = buildUser(user.uid, email, user.displayName ?: "")
                _currentUser.value = domainUser
                
                // Save locally for offline fallback
                val credentialsToken = password
                authManager.saveCredentials(email, credentialsToken, user.displayName ?: "", user.uid)
                

                
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
            if (!authValidator.isValidEmail(email)) return Result.Error(DomainError.InvalidEmail)
            val credentialsToken = password
            if (!authValidator.isValidPassword(credentialsToken)) return Result.Error(DomainError.InvalidPassword)
            
            if (authManager.validateCredentials(email, credentialsToken)) {
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

    /** Registers a new user with Firebase and persists credentials locally. */
    override suspend fun register(email: String, password: String, name: String): Result<User, DomainError> {
        return try {
            if (!authValidator.isValidEmail(email)) return Result.Error(DomainError.InvalidEmail)
            if (!authValidator.isValidPassword(password)) return Result.Error(DomainError.InvalidPassword)
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
                val credentialsToken = password
                authManager.saveCredentials(email, credentialsToken, name, firebaseUser.uid)
                
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

    /** Signs out the current user, clears local credentials, and resets sync metadata. */
    override suspend fun logout() {
        firebaseAuth.signOut()
        authManager.clearCredentials()
        _currentUser.value = null
        

    }
    
    /** Updates user profile data in both Firebase and local persistent storage. */
    override suspend fun updateProfile(
        params: UpdateUserParams
    ): Result<Unit, DomainError> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Update Firebase Email if it changed
                if (firebaseUser.email != params.email) {
                    firebaseUser.verifyBeforeUpdateEmail(params.email).await()
                }

                // Update Firebase Profile Name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(params.name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            // Always update local manager (for offline flavor or mirroring firebase)
            authManager.updateName(params.name)
            authManager.updateEmail(params.email)
            authManager.updateWorkingStartTime(params.workingStartTime)
            authManager.updateWorkingEndTime(params.workingEndTime)
            authManager.updateNotes(params.notes)

            // Update local StateFlow
            val current = _currentUser.value
            if (current != null) {
                _currentUser.value = current.copy(
                    displayName = params.name,
                    email = params.email,
                    workingStartTime = params.workingStartTime,
                    workingEndTime = params.workingEndTime,
                    notes = params.notes
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DomainError.Unknown)
        }
    }

    /** Updates the user\'s password in Firebase and local storage. */
    override suspend fun updatePassword(newPassword: String): Result<Unit, DomainError> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                firebaseUser.updatePassword(newPassword).await()
            }

            // Always update local manager
            val newCredentialsToken = newPassword
            authManager.updatePassword(newCredentialsToken)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DomainError.Unknown)
        }
    }

    /** Links current local session to a new Firebase account. */
    suspend fun linkAccountToFirebase(email: String, password: String, name: String): Result<User, DomainError> {
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

