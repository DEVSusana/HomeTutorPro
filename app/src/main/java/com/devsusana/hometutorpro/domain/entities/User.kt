package com.devsusana.hometutorpro.domain.entities

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val workingStartTime: String = "08:00",
    val workingEndTime: String = "23:00",
    val notes: String = ""
)
