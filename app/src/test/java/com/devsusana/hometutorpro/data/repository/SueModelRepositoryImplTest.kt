package com.devsusana.hometutorpro.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SueModelRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var inferenceRepository: InferenceRepository
    private lateinit var repository: SueModelRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var modelDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        inferenceRepository = mockk(relaxed = true)

        modelDir = File(context.filesDir, "sue_model").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        repository = SueModelRepositoryImpl(
            context = context,
            inferenceRepository = inferenceRepository,
            ioDispatcher = testDispatcher
        ).apply {
            minModelSizeOverride = 1L
        }
    }

    @After
    fun tearDown() {
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }
    }

    @Test
    fun isModelDownloaded_returnsFalse_whenNoModelFileExists() {
        assertFalse(repository.isModelDownloaded())
        assertNull(repository.getDownloadedModelSize())
    }

    @Test
    fun isModelDownloaded_returnsTrue_whenModelFileExists() {
        val dummyModel = File(modelDir, "gemma-3-1b-it-int4.bin")
        dummyModel.writeBytes(ByteArray(1024))

        assertTrue(repository.isModelDownloaded())
        assertEquals(1024L, repository.getDownloadedModelSize())
    }

    @Test
    fun isModelDownloaded_detectsTaskExtension() {
        val dummyModel = File(modelDir, "gemma-model.task")
        dummyModel.writeBytes(ByteArray(2048))

        assertTrue(repository.isModelDownloaded())
        assertEquals(2048L, repository.getDownloadedModelSize())
    }

    @Test
    fun resolveModelFileName_extractsTaskExtension_fromUrl() {
        val fileName = repository.resolveModelFileName("https://example.com/models/gemma-3-1b-it.task?download=true")
        assertEquals("gemma-3-1b-it.task", fileName)
    }

    @Test
    fun resolveModelFileName_extractsBinExtension_fromUrl() {
        val fileName = repository.resolveModelFileName("https://example.com/models/gemma-3-1b-it.bin")
        assertEquals("gemma-3-1b-it.bin", fileName)
    }

    @Test
    fun resolveModelFileName_extractsFromContentDisposition() {
        val fileName = repository.resolveModelFileName(
            urlStr = "https://example.com/download/model123",
            contentDisposition = "attachment; filename=\"my_custom_gemma.task\""
        )
        assertEquals("my_custom_gemma.task", fileName)
    }

    @Test
    fun resolveModelFileName_defaultsToTask_whenNoExtensionInUrl() {
        val fileName = repository.resolveModelFileName("https://example.com/models/gemma3")
        assertEquals(SueModelRepositoryImpl.DEFAULT_MODEL_NAME_TASK, fileName)
    }

    @Test
    fun deleteModel_releasesInference_andDeletesFiles() = testScope.runTest {
        val dummyBinModel = File(modelDir, "gemma-3-1b-it-int4.bin")
        dummyBinModel.writeBytes(ByteArray(1024))
        val dummyTaskModel = File(modelDir, "gemma-3-1b-it-int4.task")
        dummyTaskModel.writeBytes(ByteArray(2048))
        val tempFile = File(modelDir, "gemma-3-1b-it-int4.task.tmp")
        tempFile.writeBytes(ByteArray(512))

        val result = repository.deleteModel()

        assertTrue(result)
        assertFalse(dummyBinModel.exists())
        assertFalse(dummyTaskModel.exists())
        assertFalse(tempFile.exists())
        assertFalse(repository.isModelDownloaded())
        verify { inferenceRepository.release() }
        assertEquals(SueModelStatus.NotDownloaded, repository.modelStatusFlow.first())
    }

    @Test
    fun cancelDownload_deletesTempFile() {
        val tempTaskFile = File(modelDir, "gemma-3-1b-it-int4.task.tmp")
        tempTaskFile.writeBytes(ByteArray(512))
        val tempBinFile = File(modelDir, "gemma-3-1b-it-int4.bin.tmp")
        tempBinFile.writeBytes(ByteArray(512))

        repository.cancelDownload()

        assertFalse(tempTaskFile.exists())
        assertFalse(tempBinFile.exists())
    }

    @Test
    fun checkDeviceCompatibility_returnsUnsupported_whenSdkBelow28() {
        repository.sdkIntOverride = 27 // Android 8.1 Oreo

        val compatibility = repository.checkDeviceCompatibility()

        assertFalse(compatibility.isSupported)
        assertEquals(com.devsusana.hometutorpro.domain.entities.SueUnsupportedReason.UNSUPPORTED_ANDROID_VERSION, compatibility.reason)
        assertEquals(27, compatibility.sdkVersion)
    }

    @Test
    fun checkDeviceCompatibility_returnsSupported_whenSdk28OrAbove() {
        repository.sdkIntOverride = 34 // Android 14

        val compatibility = repository.checkDeviceCompatibility()

        assertTrue(compatibility.isSupported)
        assertNull(compatibility.reason)
        assertEquals(34, compatibility.sdkVersion)
    }

    @Test
    fun isModelDownloaded_returnsFalse_whenFileBelowMinimumSize() {
        repository.minModelSizeOverride = 50_000_000L
        val dummyModel = File(modelDir, "gemma-3-1b-it-int4.task")
        dummyModel.writeBytes(ByteArray(1024)) // only 1KB

        assertFalse(repository.isModelDownloaded())
        assertNull(repository.getDownloadedModelSize())
    }
}
