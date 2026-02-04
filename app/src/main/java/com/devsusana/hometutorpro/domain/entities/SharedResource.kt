package com.devsusana.hometutorpro.domain.entities

import java.util.UUID

/**
 * Represents a resource (file) that has been shared with a student.
 * This entity tracks the metadata of shared files without storing the actual file content.
 */
data class SharedResource(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val fileName: String,
    val fileType: String, // "pdf", "image", "document", "text", etc.
    val fileSizeBytes: Long,
    val sharedVia: ShareMethod,
    val sharedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Method used to share a resource with a student.
 */
enum class ShareMethod {
    EMAIL,
    WHATSAPP
}
