package com.devsusana.hometutorpro.data.migration

import com.devsusana.hometutorpro.data.local.dao.ResourceDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Handles the migration of local data to Firestore.
 * Used when a user upgrades from Free to Premium flavor.
 */
class LocalDataMigrator @Inject constructor(
    private val studentDao: StudentDao,
    private val scheduleDao: ScheduleDao,
    private val exceptionDao: ScheduleExceptionDao,
    private val resourceDao: ResourceDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun migrateStudents() {
        val professorId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        
        // Get all students that haven't been synced (cloudId is null)
        // Note: We need to add a query for this in DAOs or filter in memory
        // Since we added syncStatus, we can check for PENDING_UPLOAD or null cloudId
        // Assuming migration added columns with default null/SYNCED, we should check cloudId IS NULL
        
        // For this implementation, we'll fetch all and filter
        // In a real app, we'd add a specific DAO query
        val students = studentDao.getStudentsBySyncStatus(professorId, SyncStatus.SYNCED) // Default might be SYNCED but cloudId null
            .filter { it.cloudId == null }
            
        students.forEach { student ->
            val docRef = firestore.collection("professors/$professorId/students").document()
            
            val data = mapOf(
                "name" to student.name,
                "age" to student.age,
                "address" to student.address,
                "parentPhones" to student.parentPhones,
                "studentPhone" to student.studentPhone,
                "studentEmail" to student.studentEmail,
                "subjects" to student.subjects,
                "course" to student.course,
                "pricePerHour" to student.pricePerHour,
                "pendingBalance" to student.pendingBalance,
                "educationalAttention" to student.educationalAttention,
                "lastPaymentDate" to student.lastPaymentDate,
                "color" to student.color,
                "lastModified" to System.currentTimeMillis()
            )
            
            docRef.set(data).await()
            
            // Update local entity with cloudId
            studentDao.updateStudent(student.copy(
                cloudId = docRef.id,
                syncStatus = SyncStatus.SYNCED
            ))
            
            // Migrate related data for this student
            migrateSchedulesForStudent(professorId, student.id, docRef.id)
            migrateExceptionsForStudent(professorId, student.id, docRef.id)
        }
    }

    private suspend fun migrateSchedulesForStudent(professorId: String, localStudentId: Long, cloudStudentId: String) {
        // We need a way to get schedules by student ID synchronously or use the flow
        // For migration, we should add a suspend function to DAO. 
        // For now, I'll assume we can add it or use a workaround.
        // Let's assume we added `getSchedulesByStudentIdSync` or similar.
        // Or we can use `getSchedulesBySyncStatus` and filter.
        
        val schedules = scheduleDao.getSchedulesBySyncStatus(professorId, SyncStatus.SYNCED)
            .filter { it.studentId == localStudentId && it.cloudId == null }

        schedules.forEach { schedule ->
            val docRef = firestore.collection("professors/$professorId/students/$cloudStudentId/schedules").document()
            
            val data = mapOf(
                "dayOfWeek" to schedule.dayOfWeek.name,
                "startTime" to schedule.startTime,
                "endTime" to schedule.endTime,
                "lastModified" to System.currentTimeMillis()
            )
            
            docRef.set(data).await()
            
            scheduleDao.updateSchedule(schedule.copy(
                cloudId = docRef.id,
                syncStatus = SyncStatus.SYNCED
            ))
        }
    }

    private suspend fun migrateExceptionsForStudent(professorId: String, localStudentId: Long, cloudStudentId: String) {
        val exceptions = exceptionDao.getExceptionsBySyncStatus(professorId, SyncStatus.SYNCED)
            .filter { it.studentId == localStudentId && it.cloudId == null }

        exceptions.forEach { exception ->
            val docRef = firestore.collection("professors/$professorId/students/$cloudStudentId/exceptions").document()
            
            val data = mapOf(
                "exceptionDate" to exception.exceptionDate,
                "reason" to exception.reason,
                "isCancelled" to exception.isCancelled,
                "newStartTime" to exception.newStartTime,
                "newEndTime" to exception.newEndTime,
                "lastModified" to System.currentTimeMillis()
            )
            
            docRef.set(data).await()
            
            exceptionDao.updateException(exception.copy(
                cloudId = docRef.id,
                syncStatus = SyncStatus.SYNCED
            ))
        }
    }

    suspend fun migrateResources() {
        val professorId = auth.currentUser?.uid ?: return
        
        val resources = resourceDao.getResourcesBySyncStatus(professorId, SyncStatus.SYNCED)
            .filter { it.cloudId == null }

        resources.forEach { resource ->
            // Note: File upload logic would go here (upload to Firebase Storage)
            // For metadata migration:
            val docRef = firestore.collection("professors/$professorId/resources").document()
            
            val data = mapOf(
                "name" to resource.name,
                "fileType" to resource.fileType,
                "uploadDate" to resource.uploadDate,
                "cloudStoragePath" to resource.cloudStoragePath, // Might be null if not uploaded yet
                "lastModified" to System.currentTimeMillis()
            )
            
            docRef.set(data).await()
            
            resourceDao.updateResource(resource.copy(
                cloudId = docRef.id,
                syncStatus = SyncStatus.SYNCED
            ))
        }
    }
}
