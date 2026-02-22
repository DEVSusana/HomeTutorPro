package com.devsusana.hometutorpro.data.util

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Utility to clean up duplicate students in the database.
 * This can happen if sync runs multiple times or if there were issues with cloudId matching.
 */
class DuplicateCleanupUtil @Inject constructor(
    private val studentDao: StudentDao,
    private val auth: FirebaseAuth
) {
    /**
     * Removes duplicate students keeping only the one with the most recent lastModifiedTimestamp.
     * Duplicates are identified by having the same cloudId.
     */
    suspend fun removeDuplicateStudents() {
        val professorId = auth.currentUser?.uid ?: return
        val allStudents = studentDao.getAllStudents(professorId).first()
        
        // Group by cloudId
        val grouped = allStudents.groupBy { it.cloudId }
        
        for ((cloudId, students) in grouped) {
            if (cloudId != null && students.size > 1) {
                // Keep the most recently modified one
                val toKeep = students.maxByOrNull { it.lastModifiedTimestamp }
                val toDelete = students.filter { it.id != toKeep?.id }
                
                // Delete duplicates
                toDelete.forEach { student ->
                    studentDao.deleteStudent(student)
                }
            }
        }
    }
    
    /**
     * Removes students with null cloudId that have duplicates with the same name.
     * This handles cases where local-only students might have been duplicated.
     */
    suspend fun removeLocalDuplicates() {
        val professorId = auth.currentUser?.uid ?: return
        val allStudents = studentDao.getAllStudents(professorId).first()
        
        // Group by name (case-insensitive)
        val grouped = allStudents.groupBy { it.name.trim().lowercase() }
        
        for ((name, students) in grouped) {
            if (students.size > 1) {
                // Prefer students with cloudId
                val withCloudId = students.filter { it.cloudId != null }
                val withoutCloudId = students.filter { it.cloudId == null }
                
                if (withCloudId.isNotEmpty() && withoutCloudId.isNotEmpty()) {
                    // Delete local-only duplicates
                    withoutCloudId.forEach { student ->
                        studentDao.deleteStudent(student)
                    }
                }
            }
        }
    }
}
