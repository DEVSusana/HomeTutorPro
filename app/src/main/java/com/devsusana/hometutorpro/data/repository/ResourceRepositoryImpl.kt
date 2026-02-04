package com.devsusana.hometutorpro.data.repository

import android.content.Context
import android.net.Uri
import com.devsusana.hometutorpro.data.local.dao.ResourceDao
import com.devsusana.hometutorpro.data.local.dao.SharedResourceDao
import com.devsusana.hometutorpro.data.local.entities.ResourceEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.mappers.toEntity
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * Hybrid implementation of ResourceRepository for Premium flavor.
 * Handles local file storage and schedules uploads to Firebase Storage.
 */
class ResourceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourceDao: ResourceDao,
    private val storage: FirebaseStorage,
    private val syncScheduler: SyncScheduler,
    private val sharedResourceDao: SharedResourceDao
) : ResourceRepository {

    override fun getResources(professorId: String): Flow<List<Resource>> {
        return resourceDao.getAllResources().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun uploadResource(
        professorId: String,
        name: String,
        fileUri: String
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Save file locally first
                val contentResolver = context.contentResolver
                val uri = Uri.parse(fileUri)
                val inputStream = contentResolver.openInputStream(uri)
                    ?: return@withContext Result.Error(DomainError.FileNotFound)

                val fileName = "${UUID.randomUUID()}_${name}"
                val file = File(context.filesDir, fileName)

                inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Save to Room with PENDING status
                val entity = ResourceEntity(
                    name = name,
                    localFilePath = file.absolutePath,
                    fileType = contentResolver.getType(uri) ?: "application/octet-stream",
                    uploadDate = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    cloudId = null,
                    cloudStoragePath = null
                )
                resourceDao.insertResource(entity)

                // 3. Schedule sync
                syncScheduler.scheduleSyncNow()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun deleteResource(professorId: String, resourceId: String): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = resourceId.toLongOrNull() ?: return@withContext Result.Error(DomainError.ResourceNotFound)
                
                // 1. Mark for deletion in Room
                resourceDao.markForDeletion(id)
                
                // 2. Schedule sync
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    // Shared resources methods
    // Shared resources methods
    override fun getSharedResources(professorId: String?, studentId: String): Flow<List<SharedResource>> {
        val id = studentId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return sharedResourceDao.getSharedResourcesByStudent(id).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveSharedResource(professorId: String?, resource: SharedResource): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val sId = resource.studentId.toLongOrNull() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                val existingId = if (resource.id.isNotEmpty()) resource.id.toLongOrNull() ?: 0L else 0L
                
                sharedResourceDao.insertSharedResource(
                    resource.toEntity(
                        existingId = existingId,
                        studentId = sId,
                        syncStatus = SyncStatus.PENDING_UPLOAD
                    )
                )
                // Schedule sync for premium flavor
                syncScheduler.scheduleSyncNow()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun deleteSharedResource(professorId: String?, resourceId: String): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = resourceId.toLongOrNull() ?: return@withContext Result.Error(DomainError.ResourceNotFound)
                sharedResourceDao.markForDeletion(id)
                syncScheduler.scheduleSyncNow()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override fun getSharedResourcesByType(professorId: String?, studentId: String, fileType: String): Flow<List<SharedResource>> {
        val id = studentId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return sharedResourceDao.getSharedResourcesByType(id, fileType).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSharedResourcesByMethod(professorId: String?, studentId: String, method: ShareMethod): Flow<List<SharedResource>> {
        val id = studentId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return sharedResourceDao.getSharedResourcesByMethod(id, method.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
