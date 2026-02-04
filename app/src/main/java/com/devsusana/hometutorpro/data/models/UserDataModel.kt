package com.devsusana.hometutorpro.data.models

/**
 * Data model for Firebase User representation.
 * Used in the data layer for Firestore operations.
 */
data class UserDataModel(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null
)
