package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
import com.devsusana.hometutorpro.data.local.entities.ScheduleEntity
import com.devsusana.hometutorpro.data.local.entities.ScheduleExceptionEntity
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import javax.inject.Inject

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates data synchronization between Room and Firestore.
 * Handles uploading pending changes and downloading remote updates.
 */
class DataSynchronizer @Inject constructor(
    private val studentDao: StudentDao,
    private val scheduleDao: ScheduleDao,
    private val scheduleExceptionDao: ScheduleExceptionDao,
    private val firestore: FirebaseFirestore,
    private val conflictResolver: ConflictResolver,
    private val auth: FirebaseAuth,
    private val syncMetadataDao: SyncMetadataDao
) {

    private val syncMutex = Mutex()

    suspend fun performSync() {
        // Prevent concurrent syncs
        if (syncMutex.isLocked) return
        
        syncMutex.withLock {
            val professorId = auth.currentUser?.uid ?: return
            
            try {
                syncMetadataDao.setSyncInProgress(true)
                
                // 0. Clean up duplicates in Firestore (only once per day to avoid blocking)
                val lastCleanupTimestamp = syncMetadataDao.getLastCleanupTimestamp()
                val oneDayInMillis = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastCleanupTimestamp > oneDayInMillis) {
                    cleanFirestoreDuplicates(professorId)
                    syncMetadataDao.setLastCleanupTimestamp(System.currentTimeMillis())
                }
                
                // 1. Upload local changes
                uploadPendingChanges(professorId)
                
                // 2. Download remote changes
                downloadRemoteChanges(professorId)
                
                // 3. Download schedules for all students
                downloadSchedules(professorId)
                
                // 4. Download schedule exceptions
                downloadScheduleExceptions(professorId)
                
                syncMetadataDao.setLastSyncTimestamp(System.currentTimeMillis())
            } catch (e: Exception) {
                // Log error or update sync status
                android.util.Log.e("DataSynchronizer", "Sync failed", e)
            } finally {
                syncMetadataDao.setSyncInProgress(false)
            }
        }
    }
    
    private suspend fun cleanFirestoreDuplicates(professorId: String) {
        try {
            android.util.Log.d("DataSynchronizer", "Starting Firestore duplicate cleanup")
            
            val allStudents = firestore.collection("professors/$professorId/students")
                .get()
                .await()
            
            // Group by name (case-insensitive)
            val studentsByName = allStudents.documents.groupBy { 
                it.getString("name")?.trim()?.lowercase() ?: ""
            }
            
            // Find and remove duplicates
            for ((name, docs) in studentsByName) {
                if (docs.size > 1) {
                    android.util.Log.w("DataSynchronizer", "Found ${docs.size} duplicates for '$name' in Firestore")
                    
                    // Keep the one with the most recent lastModified timestamp
                    val toKeep = docs.maxByOrNull { 
                        it.getLong("lastModified") ?: 0L 
                    }
                    
                    val toDelete = docs.filter { it.id != toKeep?.id }
                    
                    android.util.Log.d("DataSynchronizer", "Keeping student with cloudId: ${toKeep?.id}, deleting: ${toDelete.map { it.id }}")
                    
                    // Delete duplicates from Firestore
                    for (doc in toDelete) {
                        firestore.collection("professors/$professorId/students")
                            .document(doc.id)
                            .delete()
                            .await()
                        android.util.Log.d("DataSynchronizer", "Deleted duplicate student from Firestore: ${doc.id}")
                    }
                }
            }
            
            android.util.Log.d("DataSynchronizer", "Finished Firestore duplicate cleanup")
        } catch (e: Exception) {
            android.util.Log.e("DataSynchronizer", "Failed to clean Firestore duplicates", e)
        }
    }

    private suspend fun uploadPendingChanges(professorId: String) {
        // Upload students
        val pendingStudents = studentDao.getStudentsBySyncStatus(SyncStatus.PENDING_UPLOAD)
        val pendingDeletes = studentDao.getStudentsBySyncStatus(SyncStatus.PENDING_DELETE)
        
        // Handle student updates/inserts
        for (student in pendingStudents) {
            try {
                val docRef = if (student.cloudId != null) {
                    firestore.collection("professors/$professorId/students")
                        .document(student.cloudId)
                } else {
                    firestore.collection("professors/$professorId/students").document()
                }
                
                val data = student.toFirestoreMap()
                docRef.set(data).await()
                
                // Update local entity
                studentDao.updateStudent(student.copy(
                    cloudId = docRef.id,
                    syncStatus = SyncStatus.SYNCED
                ))
            } catch (e: Exception) {
                studentDao.updateSyncStatus(student.id, SyncStatus.ERROR)
            }
        }
        
        // Handle student deletions
        for (student in pendingDeletes) {
            try {
                if (student.cloudId != null) {
                    firestore.collection("professors/$professorId/students")
                        .document(student.cloudId)
                        .delete()
                        .await()
                }
                // Hard delete from local DB after successful cloud delete
                studentDao.deleteStudent(student)
            } catch (e: Exception) {
                // Keep as pending delete if failed
            }
        }
        
        // Upload schedules
        val pendingSchedules = scheduleDao.getSchedulesBySyncStatus(SyncStatus.PENDING_UPLOAD)
        val pendingScheduleDeletes = scheduleDao.getSchedulesBySyncStatus(SyncStatus.PENDING_DELETE)
        
        // Handle schedule updates/inserts
        for (schedule in pendingSchedules) {
            try {
                // Get student's cloudId
                val student = studentDao.getStudentById(schedule.studentId).first()
                if (student?.cloudId != null) {
                    val docRef = if (schedule.cloudId != null) {
                        firestore.collection("professors/$professorId/students/${student.cloudId}/schedules")
                            .document(schedule.cloudId)
                    } else {
                        firestore.collection("professors/$professorId/students/${student.cloudId}/schedules").document()
                    }
                    
                    val data = schedule.toFirestoreMap()
                    docRef.set(data).await()
                    
                    // Update local entity
                    scheduleDao.updateSchedule(schedule.copy(
                        cloudId = docRef.id,
                        syncStatus = SyncStatus.SYNCED
                    ))
                }
            } catch (e: Exception) {
                scheduleDao.updateSyncStatus(schedule.id, SyncStatus.ERROR)
            }
        }
        
        // Handle schedule deletions
        for (schedule in pendingScheduleDeletes) {
            try {
                val student = studentDao.getStudentById(schedule.studentId).first()
                if (student?.cloudId != null && schedule.cloudId != null) {
                    firestore.collection("professors/$professorId/students/${student.cloudId}/schedules")
                        .document(schedule.cloudId)
                        .delete()
                        .await()
                }
                // Hard delete from local DB after successful cloud delete
                scheduleDao.deleteSchedule(schedule)
            } catch (e: Exception) {
                // Keep as pending delete if failed
            }
        }
        
        // Upload schedule exceptions
        val pendingExceptions = scheduleExceptionDao.getExceptionsBySyncStatus(SyncStatus.PENDING_UPLOAD)
        val pendingExceptionDeletes = scheduleExceptionDao.getExceptionsBySyncStatus(SyncStatus.PENDING_DELETE)
        
        // Handle exception updates/inserts
        for (exception in pendingExceptions) {
            try {
                val student = studentDao.getStudentById(exception.studentId).first()
                if (student?.cloudId != null) {
                    val docRef = if (exception.cloudId != null) {
                        firestore.collection("professors/$professorId/students/${student.cloudId}/schedule_exceptions")
                            .document(exception.cloudId)
                    } else {
                        firestore.collection("professors/$professorId/students/${student.cloudId}/schedule_exceptions").document()
                    }
                    
                    val data = exception.toFirestoreMap()
                    docRef.set(data).await()
                    
                    scheduleExceptionDao.updateException(exception.copy(
                        cloudId = docRef.id,
                        syncStatus = SyncStatus.SYNCED
                    ))
                }
            } catch (e: Exception) {
                scheduleExceptionDao.updateSyncStatus(exception.id, SyncStatus.ERROR)
            }
        }
        
        // Handle exception deletions
        for (exception in pendingExceptionDeletes) {
            try {
                val student = studentDao.getStudentById(exception.studentId).first()
                if (student?.cloudId != null && exception.cloudId != null) {
                    firestore.collection("professors/$professorId/students/${student.cloudId}/schedule_exceptions")
                        .document(exception.cloudId)
                        .delete()
                        .await()
                }
                scheduleExceptionDao.deleteException(exception)
            } catch (e: Exception) {
                // Keep as pending delete if failed
            }
        }
    }

    private suspend fun downloadRemoteChanges(professorId: String) {
        val lastSyncTimestamp = syncMetadataDao.getLastSyncTimestamp()
        
        android.util.Log.d("DataSynchronizer", "Starting downloadRemoteChanges, lastSyncTimestamp: $lastSyncTimestamp")
        
        // Get ALL remote students to check for duplicates, not just modified ones
        val allRemoteSnapshot = firestore.collection("professors/$professorId/students")
            .get()
            .await()
        
        android.util.Log.d("DataSynchronizer", "Found ${allRemoteSnapshot.documents.size} remote students")
        
        // Group remote students by name to detect duplicates in Firestore
        val remoteStudentsByName = allRemoteSnapshot.documents.groupBy { 
            it.getString("name")?.trim()?.lowercase() ?: ""
        }
        
        // Log duplicates in Firestore
        remoteStudentsByName.forEach { (name, docs) ->
            if (docs.size > 1) {
                android.util.Log.w("DataSynchronizer", "Found ${docs.size} remote students with name '$name': ${docs.map { it.id }}")
            }
        }
        
        val snapshot = firestore.collection("professors/$professorId/students")
            .whereGreaterThan("lastModified", lastSyncTimestamp)
            .get()
            .await()
        
        android.util.Log.d("DataSynchronizer", "Found ${snapshot.documents.size} students modified since last sync")
        
        for (doc in snapshot.documents) {
            val remoteStudent = doc.toStudentEntity()
            android.util.Log.d("DataSynchronizer", "Processing remote student: ${remoteStudent.name} (cloudId: ${doc.id})")
            
            val localStudentByCloudId = studentDao.getStudentByCloudId(doc.id)
            
            if (localStudentByCloudId == null) {
                // Check if there's a local student with the same name but no cloudId
                val allLocalStudents = studentDao.getAllStudents().first()
                android.util.Log.d("DataSynchronizer", "Local students count: ${allLocalStudents.size}")
                
                val localStudentByName = allLocalStudents.find { 
                    it.cloudId == null && it.name.trim().equals(remoteStudent.name.trim(), ignoreCase = true)
                }
                
                if (localStudentByName != null) {
                    // Update existing local student with cloudId instead of creating duplicate
                    android.util.Log.d("DataSynchronizer", "Updating local student ${localStudentByName.name} (id: ${localStudentByName.id}) with cloudId: ${doc.id}")
                    studentDao.updateStudent(remoteStudent.copy(
                        id = localStudentByName.id, // Keep local ID
                        cloudId = doc.id,
                        syncStatus = SyncStatus.SYNCED,
                        // Merge data - prefer remote for most fields but keep local balance if more recent
                        pendingBalance = if (localStudentByName.lastModifiedTimestamp > remoteStudent.lastModifiedTimestamp) 
                            localStudentByName.pendingBalance 
                        else 
                            remoteStudent.pendingBalance,
                        lastPaymentDate = if (localStudentByName.lastPaymentDate != null && 
                            (remoteStudent.lastPaymentDate == null || localStudentByName.lastPaymentDate!! > remoteStudent.lastPaymentDate!!))
                            localStudentByName.lastPaymentDate
                        else
                            remoteStudent.lastPaymentDate
                    ))
                } else {
                    // Check if there's already a local student with the same name AND cloudId (duplicate from Firestore)
                    val existingWithSameName = allLocalStudents.find {
                        it.name.trim().equals(remoteStudent.name.trim(), ignoreCase = true)
                    }
                    
                    if (existingWithSameName != null) {
                        android.util.Log.w("DataSynchronizer", "DUPLICATE DETECTED: Local student '${existingWithSameName.name}' (id: ${existingWithSameName.id}, cloudId: ${existingWithSameName.cloudId}) already exists, but trying to insert another with cloudId: ${doc.id}")
                        // Skip inserting this duplicate
                        android.util.Log.w("DataSynchronizer", "Skipping duplicate insertion")
                    } else {
                        // Truly new remote student - insert with cloudId
                        android.util.Log.d("DataSynchronizer", "Inserting new remote student: ${remoteStudent.name} with cloudId: ${doc.id}")
                        studentDao.insertStudent(remoteStudent.copy(
                            id = 0, // Let Room auto-generate
                            cloudId = doc.id,
                            syncStatus = SyncStatus.SYNCED
                        ))
                    }
                }
            } else {
                android.util.Log.d("DataSynchronizer", "Found existing local student by cloudId: ${localStudentByCloudId.name} (id: ${localStudentByCloudId.id})")
                // Student exists - update if remote is newer
                if (localStudentByCloudId.lastModifiedTimestamp < remoteStudent.lastModifiedTimestamp) {
                    if (localStudentByCloudId.syncStatus == SyncStatus.PENDING_UPLOAD) {
                        // Conflict!
                        android.util.Log.w("DataSynchronizer", "Conflict detected for student: ${localStudentByCloudId.name}")
                        conflictResolver.resolveConflict(localStudentByCloudId, remoteStudent)
                    } else {
                        // Safe to update - preserve local id
                        android.util.Log.d("DataSynchronizer", "Updating student ${localStudentByCloudId.name} with remote data")
                        studentDao.updateStudent(remoteStudent.copy(
                            id = localStudentByCloudId.id,
                            cloudId = doc.id,
                            syncStatus = SyncStatus.SYNCED
                        ))
                    }
                }
            }
        }
        
        android.util.Log.d("DataSynchronizer", "Finished downloadRemoteChanges")
    }
    
    // Extension functions for mapping
    private fun StudentEntity.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "age" to age,
            "address" to address,
            "parentPhones" to parentPhones,
            "studentPhone" to studentPhone,
            "studentEmail" to studentEmail,
            "subjects" to subjects,
            "course" to course,
            "pricePerHour" to pricePerHour,
            "pendingBalance" to pendingBalance,
            "educationalAttention" to educationalAttention,
            "lastPaymentDate" to lastPaymentDate,
            "color" to color,
            "lastModified" to lastModifiedTimestamp
        )
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toStudentEntity(): StudentEntity {
        return StudentEntity(
            cloudId = id,
            name = getString("name") ?: "",
            age = getLong("age")?.toInt() ?: 0,
            address = getString("address") ?: "",
            parentPhones = getString("parentPhones") ?: "",
            studentPhone = getString("studentPhone") ?: "",
            studentEmail = getString("studentEmail") ?: "",
            subjects = getString("subjects") ?: "",
            course = getString("course") ?: "",
            pricePerHour = getDouble("pricePerHour") ?: 0.0,
            pendingBalance = getDouble("pendingBalance") ?: 0.0,
            educationalAttention = getString("educationalAttention") ?: "",
            lastPaymentDate = getLong("lastPaymentDate"),
            color = getLong("color")?.toInt(),
            lastModifiedTimestamp = getLong("lastModified") ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }
    
    private suspend fun downloadSchedules(professorId: String) {
        // Get all students with cloudId
        val students = studentDao.getAllStudents().first()
        
        for (student in students) {
            if (student.cloudId != null) {
                try {
                    val schedulesSnapshot = firestore
                        .collection("professors/$professorId/students/${student.cloudId}/schedules")
                        .get()
                        .await()
                    
                    for (doc in schedulesSnapshot.documents) {
                        val remoteSchedule = doc.toScheduleEntity(student.id)
                        val localSchedule = scheduleDao.getScheduleByCloudId(doc.id)
                        
                        if (localSchedule == null) {
                            // New remote schedule
                            scheduleDao.insertSchedule(remoteSchedule.copy(
                                cloudId = doc.id,
                                syncStatus = SyncStatus.SYNCED
                            ))
                        } else if (localSchedule.lastModifiedTimestamp < remoteSchedule.lastModifiedTimestamp) {
                            // Remote is newer, update local
                            scheduleDao.updateSchedule(remoteSchedule.copy(
                                id = localSchedule.id,
                                cloudId = doc.id,
                                syncStatus = SyncStatus.SYNCED
                            ))
                        }
                    }
                } catch (e: Exception) {
                    // Log error for this student's schedules
                }
            }
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toScheduleEntity(studentId: Long): ScheduleEntity {
        return ScheduleEntity(
            studentId = studentId,
            cloudId = id,
            dayOfWeek = DayOfWeek.valueOf(getString("dayOfWeek") ?: "MONDAY"),
            startTime = getString("startTime") ?: "",
            endTime = getString("endTime") ?: "",
            lastModifiedTimestamp = getLong("lastModified") ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }
    
    private fun ScheduleEntity.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "dayOfWeek" to dayOfWeek.name,
            "startTime" to startTime,
            "endTime" to endTime,
            "lastModified" to lastModifiedTimestamp
        )
    }
    
    private suspend fun downloadScheduleExceptions(professorId: String) {
        val students = studentDao.getAllStudents().first()
        
        for (student in students) {
            if (student.cloudId != null) {
                try {
                    val exceptionsSnapshot = firestore
                        .collection("professors/$professorId/students/${student.cloudId}/schedule_exceptions")
                        .get()
                        .await()
                    
                    for (doc in exceptionsSnapshot.documents) {
                        val remoteException = doc.toScheduleExceptionEntity(student.id)
                        val localException = scheduleExceptionDao.getExceptionByCloudId(doc.id)
                        
                        if (localException == null) {
                            scheduleExceptionDao.insertException(remoteException.copy(
                                cloudId = doc.id,
                                syncStatus = SyncStatus.SYNCED
                            ))
                        } else if (localException.lastModifiedTimestamp < remoteException.lastModifiedTimestamp) {
                            scheduleExceptionDao.updateException(remoteException.copy(
                                id = localException.id,
                                cloudId = doc.id,
                                syncStatus = SyncStatus.SYNCED
                            ))
                        }
                    }
                } catch (e: Exception) {
                    // Log error
                }
            }
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toScheduleExceptionEntity(studentId: Long): ScheduleExceptionEntity {
        return ScheduleExceptionEntity(
            studentId = studentId,
            cloudId = id,
            originalScheduleId = getString("originalScheduleId") ?: "",
            exceptionDate = getLong("exceptionDate") ?: 0L,
            reason = getString("reason") ?: "",
            isCancelled = getBoolean("isCancelled") ?: true,
            newStartTime = getString("newStartTime"),
            newEndTime = getString("newEndTime"),
            newDayOfWeek = getString("newDayOfWeek")?.let { DayOfWeek.valueOf(it) },
            lastModifiedTimestamp = getLong("lastModified") ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }
    
    private fun ScheduleExceptionEntity.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "originalScheduleId" to originalScheduleId,
            "exceptionDate" to exceptionDate,
            "reason" to reason,
            "isCancelled" to isCancelled,
            "newStartTime" to newStartTime,
            "newEndTime" to newEndTime,
            "newDayOfWeek" to newDayOfWeek?.name,
            "lastModified" to lastModifiedTimestamp
        )
    }
}
