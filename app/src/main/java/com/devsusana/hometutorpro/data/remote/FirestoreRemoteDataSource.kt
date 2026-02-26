package com.devsusana.hometutorpro.data.remote

import com.devsusana.hometutorpro.domain.repository.RemoteDataSource
import com.devsusana.hometutorpro.domain.repository.RemoteDocument
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source backed by Firestore.
 */
@Singleton
class FirestoreRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : RemoteDataSource {

    override suspend fun uploadDocument(
        path: String,
        data: com.devsusana.hometutorpro.domain.repository.CloudData,
        idempotencyKey: String?,
        idempotencyField: String
    ): String {
        val collectionRef = firestore.collection(path)
        
        return firestore.runTransaction { transaction ->
            val docRef = if (idempotencyKey != null) {
                // Atomic check for idempotency
                val existing = collectionRef.whereEqualTo(idempotencyField, idempotencyKey).limit(1).get().result
                if (existing != null && !existing.isEmpty) {
                    existing.documents.first().reference
                } else {
                    collectionRef.document()
                }
            } else {
                collectionRef.document()
            }
            
            transaction.set(docRef, data)
            docRef.id
        }.await()
    }

    override suspend fun deleteDocument(path: String) {
        val docRef = firestore.document(path)
        docRef.delete().await()
    }

    override suspend fun downloadCollection(
        path: String, 
        lastSyncTimestamp: Long
    ): List<RemoteDocument> {
        return firestore.collection(path)
            .whereGreaterThan("lastModified", lastSyncTimestamp)
            .get()
            .await()
            .documents
            .map { RemoteDocument(it.id, it.data ?: emptyMap()) }
    }

    override suspend fun deleteStudentData(professorId: String, studentCloudId: String) {
        val studentPath = "professors/$professorId/students/$studentCloudId"
        val batch = firestore.batch()

        // 1. Delete Schedules subcollection
        val schedules = firestore.collection("$studentPath/schedules").get().await()
        schedules.documents.forEach { batch.delete(it.reference) }

        // 2. Delete Exceptions subcollection
        val exceptions = firestore.collection("$studentPath/schedule_exceptions").get().await()
        exceptions.documents.forEach { batch.delete(it.reference) }

        // 3. Delete the student document itself
        batch.delete(firestore.document(studentPath))

        batch.commit().await()
    }
}
