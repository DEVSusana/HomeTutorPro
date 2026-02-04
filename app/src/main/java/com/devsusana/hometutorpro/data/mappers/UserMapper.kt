package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.models.UserDataModel
import com.devsusana.hometutorpro.domain.entities.User

/**
 * Mapper for converting between UserDataModel and User domain entity.
 */

fun UserDataModel.toDomain(): User {
    return User(
        uid = uid,
        email = email,
        displayName = displayName
    )
}

fun User.toData(): UserDataModel {
    return UserDataModel(
        uid = uid,
        email = email,
        displayName = displayName
    )
}
