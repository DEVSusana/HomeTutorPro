package com.devsusana.hometutorpro.core.utils

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FilePickerHelperTest {

    @Test
    fun getFileInfo_shouldExtractNameTypeAndSize() {
        // Given
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()

        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        cursor.addRow(arrayOf<Any>("test_document.pdf", 1024L))

        every { context.contentResolver } returns contentResolver
        every { contentResolver.query(uri, null, null, null, null) } returns cursor
        every { contentResolver.getType(uri) } returns "application/pdf"

        // When
        val fileInfo = FilePickerHelper.getFileInfo(context, uri)

        // Then
        assertNotNull(fileInfo)
        assertEquals("test_document.pdf", fileInfo?.name)
        assertEquals(1024L, fileInfo?.sizeBytes)
        assertEquals("pdf", fileInfo?.type)
    }

    @Test
    fun formatFileSize_shouldFormatBytesCorrectly() {
        assertEquals("500 B", FilePickerHelper.formatFileSize(500L))
        assertEquals("1.00 KB", FilePickerHelper.formatFileSize(1024L))
        assertEquals("1.50 MB", FilePickerHelper.formatFileSize(1572864L))
    }
}
