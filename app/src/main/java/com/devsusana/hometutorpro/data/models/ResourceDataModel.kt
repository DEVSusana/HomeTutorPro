package com.devsusana.hometutorpro.data.models

/**
 * Data model for Firestore Resource document.
 * Used in the data layer for Firestore operations.
 * Upload date is stored as Long timestamp for Firestore compatibility.
 */
data class ResourceDataModel(
    val id: String = "",
    val professorId: String = "",
    val name: String = "",
    val url: String = "",
    val type: String = "",
    val uploadDate: Long = 0L
)
