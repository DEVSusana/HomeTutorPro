package com.devsusana.hometutorpro.domain.entities

/**
 * Data class representing the parameters to update a user profile.
 */
data class UpdateUserParams(
    val name: String,
    val email: String,
    val workingStartTime: String,
    val workingEndTime: String,
    val notes: String
)
