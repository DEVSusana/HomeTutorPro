package com.devsusana.hometutorpro.presentation.notes

data class NotesState(
    val notes: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    // Internal state to hold user profile data for update
    val name: String = "",
    val email: String = "",
    val workingStartTime: String = "08:00",
    val workingEndTime: String = "23:00"
)
