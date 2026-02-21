package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toStudentEntity
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for real-time changes in Firestore and updates the local Room database.
 * Ensures the UI stays up-to-date with changes from other devices.
 */
@Singleton
class FirestoreChangeListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val studentDao: StudentDao,
    private val auth: FirebaseAuth,
    private val secureAuthManager: SecureAuthManager
) {
    private var listenerRegistration: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startListening() {
        val professorId = auth.currentUser?.uid ?: return
        
        // Avoid multiple listeners
        if (listenerRegistration != null) return

        listenerRegistration = firestore
            .collection("professors/$professorId/students")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> {
                            val student = change.document.toStudentEntity(secureAuthManager, professorId)
                            scope.launch {
                                // Check if we have a local version
                                val localStudent = studentDao.getStudentByCloudId(change.document.id, professorId)
                                
                                // Only update if remote is newer or we don't have it
                                // This prevents overwriting local changes that haven't synced yet
                                if (localStudent == null || 
                                    (localStudent.syncStatus != SyncStatus.PENDING_UPLOAD && 
                                     localStudent.lastModifiedTimestamp < student.lastModifiedTimestamp)) {
                                    
                                    studentDao.insertStudent(student.copy(
                                        id = localStudent?.id ?: 0, // Keep local ID if exists
                                        cloudId = change.document.id,
                                        syncStatus = SyncStatus.SYNCED
                                    ))
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            scope.launch {
                                // Verify it's not a local pending delete that triggered this
                                val localStudent = studentDao.getStudentByCloudId(change.document.id, professorId)
                                if (localStudent != null && localStudent.syncStatus != SyncStatus.PENDING_DELETE) {
                                    studentDao.deleteStudent(localStudent)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
