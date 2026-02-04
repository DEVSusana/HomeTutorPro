package com.devsusana.hometutorpro.domain.entities

import java.util.Date

/**
 * Represents a file resource uploaded by the professor.
 */
data class Resource(
    val id: String = "",
    val professorId: String = "",
    val name: String = "",
    val url: String = "",
    val type: String = "", // e.g., "application/pdf", "image/jpeg"
    val uploadDate: Date = Date()
)
