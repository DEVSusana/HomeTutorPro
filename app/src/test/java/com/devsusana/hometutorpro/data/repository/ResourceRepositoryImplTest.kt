package com.devsusana.hometutorpro.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.devsusana.hometutorpro.data.local.dao.ResourceDao
import com.devsusana.hometutorpro.data.local.dao.SharedResourceDao
import com.devsusana.hometutorpro.data.local.entities.ResourceEntity
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.Result
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

class ResourceRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var resourceDao: ResourceDao
    private lateinit var storage: FirebaseStorage
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var sharedResourceDao: SharedResourceDao
    private lateinit var repository: ResourceRepositoryImpl

    @Before
    fun setup() {
        context = mockk()
        resourceDao = mockk()
        storage = mockk()
        syncScheduler = mockk()
        sharedResourceDao = mockk()

        repository = ResourceRepositoryImpl(context, resourceDao, storage, syncScheduler, sharedResourceDao)
    }

    @Test
    fun uploadResource_shouldSaveLocallyAndScheduleSyncWithFileType() = runTest {
        // Given
        val professorId = "prof1"
        val name = "test_doc.pdf"
        val fileType = "pdf"
        val fileUri = "content://test/uri"

        val contentResolver = mockk<ContentResolver>()
        val inputStream = java.io.ByteArrayInputStream("dummy content".toByteArray())
        val filesDir = File(requireNotNull(System.getProperty("java.io.tmpdir"))) // mock files dir

        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        
        every { context.contentResolver } returns contentResolver
        every { context.filesDir } returns filesDir
        every { contentResolver.openInputStream(uriMock) } returns inputStream
        
        coEvery { resourceDao.insertResource(any()) } returns 1L
        every { syncScheduler.scheduleSyncNow() } just Runs

        // When
        val result = repository.uploadResource(professorId, name, fileType, fileUri)

        // Then
        unmockkStatic(Uri::class)
        assertTrue(result is Result.Success)
        
        val slot = slot<ResourceEntity>()
        coVerify { resourceDao.insertResource(capture(slot)) }
        assertTrue(slot.captured.fileType == fileType)
        assertTrue(slot.captured.name == name)
        
        verify { syncScheduler.scheduleSyncNow() }
    }
}
