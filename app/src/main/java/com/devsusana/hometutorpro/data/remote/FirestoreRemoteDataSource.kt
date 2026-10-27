package com.devsusana.hometutorpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Remote data source backed by Firestore.
 */
class FirestoreRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : RemoteDataSource {

    override suspend fun uploadDocument(
        path: String,
        data: CloudData,
        idempotencyKey: String?,
        idempotencyField: String
    ): String {
        val collectionRef = firestore.collection(path)
        val docRef = if (idempotencyKey != null) {
            val existing = collectionRef.whereEqualTo(idempotencyField, idempotencyKey).limit(1).get().await()
            if (!existing.isEmpty) {
                existing.documents.first().reference
            } else {
                collectionRef.document()
            }
        } else {
            collectionRef.document()
        }

        docRef.set(data).await()
        return docRef.id
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
