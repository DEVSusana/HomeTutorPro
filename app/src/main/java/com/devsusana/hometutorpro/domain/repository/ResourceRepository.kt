package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.entities.ShareMethod
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for Resource data operations.
 */
interface ResourceRepository {
    fun getResources(professorId: String): Flow<List<Resource>>
    suspend fun uploadResource(professorId: String, name: String, fileType: String, fileUri: String): Result<Unit, DomainError>
    suspend fun deleteResource(professorId: String, resourceId: String): Result<Unit, DomainError>
    
    // Shared resources methods
    fun getSharedResources(professorId: String?, studentId: String): Flow<List<SharedResource>>
    suspend fun saveSharedResource(professorId: String?, resource: SharedResource): Result<Unit, DomainError>
    suspend fun deleteSharedResource(professorId: String?, resourceId: String): Result<Unit, DomainError>
    fun getSharedResourcesByType(professorId: String?, studentId: String, fileType: String): Flow<List<SharedResource>>
    fun getSharedResourcesByMethod(professorId: String?, studentId: String, method: ShareMethod): Flow<List<SharedResource>>
}
