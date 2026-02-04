package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.models.ResourceDataModel
import com.devsusana.hometutorpro.domain.entities.Resource
import java.util.Date

/**
 * Mapper for converting between ResourceDataModel and Resource domain entity.
 * Handles Date to Long timestamp conversion.
 */

fun ResourceDataModel.toDomain(): Resource {
    return Resource(
        id = id,
        professorId = professorId,
        name = name,
        url = url,
        type = type,
        uploadDate = Date(uploadDate)
    )
}

fun Resource.toData(): ResourceDataModel {
    return ResourceDataModel(
        id = id,
        professorId = professorId,
        name = name,
        url = url,
        type = type,
        uploadDate = uploadDate.time
    )
}
