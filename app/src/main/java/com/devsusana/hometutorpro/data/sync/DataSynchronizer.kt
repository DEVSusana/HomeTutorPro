package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
import com.devsusana.hometutorpro.data.local.entities.ScheduleEntity
import com.devsusana.hometutorpro.data.local.entities.ScheduleExceptionEntity
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toFirestoreMap
import com.devsusana.hometutorpro.data.mappers.toScheduleEntity
import com.devsusana.hometutorpro.data.mappers.toScheduleExceptionEntity
import com.devsusana.hometutorpro.data.mappers.toStudentEntity
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.domain.repository.RemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
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
    private val remoteDataSource: RemoteDataSource,
    private val conflictResolver: ConflictResolver,
    private val auth: FirebaseAuth,
    private val syncMetadataDao: SyncMetadataDao,
    private val secureAuthManager: SecureAuthManager
) {

    private val syncMutex = Mutex()

    suspend fun performSync() {
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
                SafeLogger.e(TAG, "Sync failed", e)
            } finally {
                syncMetadataDao.setSyncInProgress(false)
            }
        }
    }
    
    private suspend fun cleanFirestoreDuplicates(professorId: String) {
        try {
            val path = "professors/$professorId/students"
            val allStudents = remoteDataSource.downloadCollection(path, 0L)
            
            val studentsByName = allStudents.groupBy { 
                it.data["name"]?.toString()?.trim()?.lowercase() ?: ""
            }
            
            for ((_, docs) in studentsByName) {
                if (docs.size > 1) {
                    val toKeep = docs.maxByOrNull { 
                        it.data["lastModified"]?.toString()?.toLong() ?: 0L 
                    }
                    
                    val toDelete = docs.filter { it.id != toKeep?.id }
                    
                    for (doc in toDelete) {
                        remoteDataSource.deleteDocument("$path/${doc.id}")
                    }
                }
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Failed to clean Firestore duplicates", e)
        }
    }

    private suspend fun uploadPendingChanges(professorId: String) {
        // Upload students
        val pendingStudents = studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_UPLOAD)
        val pendingDeletes = studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_DELETE)
        
        // Handle student updates/inserts (NOT deletions yet — children need the student cloudId)
        for (student in pendingStudents) {
            try {
                val data = student.toFirestoreMap(secureAuthManager)
                val path = "professors/$professorId/students"
                
                val finalCloudId = remoteDataSource.uploadDocument(
                    path = path,
                    data = data,
                    idempotencyKey = if (student.cloudId == null) student.name else null,
                    idempotencyField = "name"
                )
                
                // Update local entity
                studentDao.updateStudent(student.copy(
                    cloudId = finalCloudId,
                    syncStatus = SyncStatus.SYNCED
                ))
            } catch (e: Exception) {
                studentDao.updateSyncStatus(student.id, professorId, SyncStatus.ERROR)
            }
        }
        
        // Upload schedules
        val pendingSchedules = scheduleDao.getSchedulesBySyncStatus(professorId, SyncStatus.PENDING_UPLOAD)
        val pendingScheduleDeletes = scheduleDao.getSchedulesBySyncStatus(professorId, SyncStatus.PENDING_DELETE)
        
        // Handle schedule updates/inserts
        for (schedule in pendingSchedules) {
            try {
                // Get student's cloudId
                val student = studentDao.getStudentById(schedule.studentId, professorId).first()
                if (student?.cloudId != null) {
                    val data = schedule.toFirestoreMap()
                    val path = "professors/$professorId/students/${student.cloudId}/schedules"
                    
                    val finalCloudId = remoteDataSource.uploadDocument(
                        path = path,
                        data = data,
                        idempotencyKey = if (schedule.cloudId == null) schedule.id.toString() else null,
                        idempotencyField = "localId"
                    )
                    
                    // Update local entity
                    scheduleDao.updateSchedule(schedule.copy(
                        cloudId = finalCloudId,
                        syncStatus = SyncStatus.SYNCED
                    ))
                }
            } catch (e: Exception) {
                scheduleDao.updateSyncStatus(schedule.id, professorId, SyncStatus.ERROR)
            }
        }
        
        // Handle schedule deletions
        for (schedule in pendingScheduleDeletes) {
            try {
                val student = studentDao.getStudentById(schedule.studentId, professorId).first()
                    ?: studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_DELETE)
                        .find { it.id == schedule.studentId }
                if (student?.cloudId != null && schedule.cloudId != null) {
                    remoteDataSource.deleteDocument("professors/$professorId/students/${student.cloudId}/schedules/${schedule.cloudId}")
                }
                // Hard delete from local DB after successful cloud delete
                scheduleDao.deleteSchedule(schedule)
            } catch (e: Exception) {
                // Keep as pending delete if failed
            }
        }
        
        // Upload schedule exceptions
        val pendingExceptions = scheduleExceptionDao.getExceptionsBySyncStatus(professorId, SyncStatus.PENDING_UPLOAD)
        val pendingExceptionDeletes = scheduleExceptionDao.getExceptionsBySyncStatus(professorId, SyncStatus.PENDING_DELETE)
        
        // Handle exception updates/inserts
        for (exception in pendingExceptions) {
            try {
                val student = studentDao.getStudentById(exception.studentId, professorId).first()
                if (student?.cloudId != null) {
                    val data = exception.toFirestoreMap()
                    val path = "professors/$professorId/students/${student.cloudId}/schedule_exceptions"
                    
                    val finalCloudId = remoteDataSource.uploadDocument(
                        path = path,
                        data = data,
                        idempotencyKey = if (exception.cloudId == null) exception.id.toString() else null,
                        idempotencyField = "localId"
                    )
                    
                    scheduleExceptionDao.updateException(exception.copy(
                        cloudId = finalCloudId,
                        syncStatus = SyncStatus.SYNCED
                    ))
                }
            } catch (e: Exception) {
                scheduleExceptionDao.updateSyncStatus(exception.id, professorId, SyncStatus.ERROR)
            }
        }
        
        // Handle exception deletions
        for (exception in pendingExceptionDeletes) {
            try {
                val student = studentDao.getStudentById(exception.studentId, professorId).first()
                    ?: studentDao.getStudentsBySyncStatus(professorId, SyncStatus.PENDING_DELETE)
                        .find { it.id == exception.studentId }
                if (student?.cloudId != null && exception.cloudId != null) {
                    remoteDataSource.deleteDocument("professors/$professorId/students/${student.cloudId}/schedule_exceptions/${exception.cloudId}")
                }
                scheduleExceptionDao.deleteException(exception)
            } catch (e: Exception) {
                // Keep as pending delete if failed
            }
        }

        // Handle student deletions LAST — after all children are cleaned up
        for (student in pendingDeletes) {
            try {
                if (student.cloudId != null) {
                    // Recursive delete: Student + Schedules + Exceptions
                    remoteDataSource.deleteStudentData(professorId, student.cloudId)
                }
                studentDao.deleteStudent(student)
            } catch (e: Exception) {
                // Keep as pending delete if failed
            }
        }
    }

    private suspend fun downloadRemoteChanges(professorId: String) {
        val lastSyncTimestamp = syncMetadataDao.getLastSyncTimestamp()
        
        // Single filtered query using RemoteDataSource
        val remoteDocs = remoteDataSource.downloadCollection(
            path = "professors/$professorId/students",
            lastSyncTimestamp = lastSyncTimestamp
        )
        
        // Get all local students once for duplicate detection
        val allLocalStudents = studentDao.getAllStudents(professorId).first()
        
        for (doc in remoteDocs) {
            val remoteStudent = doc.toStudentEntity(secureAuthManager, professorId)
            val localStudentByCloudId = studentDao.getStudentByCloudId(doc.id, professorId)
            
            if (localStudentByCloudId == null) {
                val localStudentByName = allLocalStudents.find { 
                    it.cloudId == null && it.name.trim().equals(remoteStudent.name.trim(), ignoreCase = true)
                }
                
                if (localStudentByName != null) {
                    studentDao.updateStudent(remoteStudent.copy(
                        id = localStudentByName.id,
                        cloudId = doc.id,
                        syncStatus = SyncStatus.SYNCED,
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
                    val existingWithSameName = allLocalStudents.find {
                        it.name.trim().equals(remoteStudent.name.trim(), ignoreCase = true)
                    }
                    
                    if (existingWithSameName == null) {
                        studentDao.insertStudent(remoteStudent.copy(
                            id = 0,
                            cloudId = doc.id,
                            syncStatus = SyncStatus.SYNCED
                        ))
                    }
                    // else: skip duplicate insertion
                }
            } else {
                if (localStudentByCloudId.lastModifiedTimestamp < remoteStudent.lastModifiedTimestamp) {
                    if (localStudentByCloudId.syncStatus == SyncStatus.PENDING_UPLOAD) {
                        conflictResolver.resolveConflict(localStudentByCloudId, remoteStudent)
                    } else {
                        studentDao.updateStudent(remoteStudent.copy(
                            id = localStudentByCloudId.id,
                            cloudId = doc.id,
                            syncStatus = SyncStatus.SYNCED
                        ))
                    }
                }
            }
        }
    }
    
    private suspend fun downloadSchedules(professorId: String) {
        // Get all students with cloudId
        val students = studentDao.getAllStudents(professorId).first()
        
        for (student in students) {
            if (student.cloudId != null) {
                try {
                    val remoteDocs = remoteDataSource.downloadCollection(
                        path = "professors/$professorId/students/${student.cloudId}/schedules",
                        lastSyncTimestamp = 0L // Download all for now
                    )
                    
                    for (doc in remoteDocs) {
                        val remoteSchedule = doc.toScheduleEntity(student.id, professorId)
                        val localSchedule = scheduleDao.getScheduleByCloudId(doc.id, professorId)
                        
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
    
    private suspend fun downloadScheduleExceptions(professorId: String) {
        val students = studentDao.getAllStudents(professorId).first()
        
        for (student in students) {
            if (student.cloudId != null) {
                try {
                    val remoteDocs = remoteDataSource.downloadCollection(
                        path = "professors/$professorId/students/${student.cloudId}/schedule_exceptions",
                        lastSyncTimestamp = 0L
                    )
                    
                    for (doc in remoteDocs) {
                        val remoteException = doc.toScheduleExceptionEntity(student.id, professorId)
                        val localException = scheduleExceptionDao.getExceptionByCloudId(doc.id, professorId)
                        
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

    companion object {
        private const val TAG = "DataSynchronizer"
    }
}
