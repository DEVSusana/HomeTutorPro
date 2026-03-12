package com.devsusana.hometutorpro.presentation.resources

import com.devsusana.hometutorpro.domain.entities.Resource

data class ResourcesState(
    val resources: List<Resource> = emptyList(),
    val isLoading: Boolean = false,
    val error: Any? = null,
    val successMessage: Any? = null,
    val errorMessage: Any? = null
)
