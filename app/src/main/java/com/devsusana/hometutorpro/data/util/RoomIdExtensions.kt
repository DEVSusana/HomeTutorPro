package com.devsusana.hometutorpro.data.util

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Converts a String ID to a Room Long ID.
 * Returns [Result.Error] with [defaultError] if the conversion fails.
 */
inline fun String.toRoomIdOrError(
    defaultError: DomainError = DomainError.Unknown
): Result<Long, DomainError> {
    val id = toLongOrNull()
    return if (id != null) Result.Success(id) else Result.Error(defaultError)
}

/**
 * Converts a String ID to a Room Long ID, returning null if the conversion fails.
 */
fun String.toRoomId(): Long? = toLongOrNull()
