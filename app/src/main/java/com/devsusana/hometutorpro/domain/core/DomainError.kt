package com.devsusana.hometutorpro.domain.core

sealed class DomainError {
    data object NetworkError : DomainError()
    data object UnknownError : DomainError()
    data object StudentNotFound : DomainError()
    data object ScheduleConflict : DomainError()
    data class ConflictingStudent(val studentName: String, val time: String) : DomainError()
    data object Unknown : DomainError()
    
    // Authentication errors
    data object InvalidEmail : DomainError()
    data object InvalidPassword : DomainError()
    data object InvalidName : DomainError()
    data object InvalidCredentials : DomainError()
    data object UserNotFound : DomainError()
    data object UserAlreadyExists : DomainError()
    
    // Resource errors
    data object FileNotFound : DomainError()
    data object ResourceNotFound : DomainError()

    // Validation errors
    data object StudentNameRequired : DomainError()
    data object InvalidPrice : DomainError()
    data object InvalidBalance : DomainError()
}
