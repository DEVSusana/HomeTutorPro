package com.devsusana.hometutorpro.domain.core

sealed class DomainError {
    object NetworkError : DomainError()
    object UnknownError : DomainError()
    object StudentNotFound : DomainError()
    object ScheduleConflict : DomainError()
    data class ConflictingStudent(val studentName: String, val time: String) : DomainError()
    object Unknown : DomainError()
    
    // Authentication errors
    object InvalidEmail : DomainError()
    object InvalidPassword : DomainError()
    object InvalidName : DomainError()
    object InvalidCredentials : DomainError()
    object UserNotFound : DomainError()
    object UserAlreadyExists : DomainError()
    
    // Resource errors
    object FileNotFound : DomainError()
    object ResourceNotFound : DomainError()
}
