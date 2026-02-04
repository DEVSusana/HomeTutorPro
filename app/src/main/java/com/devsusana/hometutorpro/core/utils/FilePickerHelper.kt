package com.devsusana.hometutorpro.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Helper object for file picking operations.
 * Provides utilities to create file picker intents and extract file information.
 */
object FilePickerHelper {
    
    /**
     * Creates an intent to pick files from device storage.
     * Supports PDF, images, documents, and text files.
     */
    fun createFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "image/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain"
            ))
        }
    }
    
    /**
     * Extracts file information from a URI.
     * Returns FileInfo with name, type, and size, or null if extraction fails.
     */
    fun getFileInfo(context: Context, uri: Uri): FileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "unknown"
            var fileSize = 0L
            
            // Get file name and size from content resolver
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
            
            // Get MIME type
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            
            // Determine file type category
            val fileType = when {
                mimeType.startsWith("image/") -> "image"
                mimeType == "application/pdf" -> "pdf"
                mimeType.contains("document") || mimeType.contains("word") -> "document"
                mimeType.contains("spreadsheet") || mimeType.contains("excel") -> "spreadsheet"
                mimeType.startsWith("text/") -> "text"
                else -> "other"
            }
            
            FileInfo(
                uri = uri,
                name = fileName,
                type = fileType,
                sizeBytes = fileSize
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Formats file size in human-readable format (B, KB, MB, GB).
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

/**
 * Data class representing file information extracted from a URI.
 */
data class FileInfo(
    val uri: Uri,
    val name: String,
    val type: String,
    val sizeBytes: Long
)
