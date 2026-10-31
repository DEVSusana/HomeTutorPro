package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import com.devsusana.hometutorpro.domain.usecases.IGetAppFeatureFlagsUseCase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetAppFeatureFlagsUseCase @Inject constructor(
    private val repository: IConfigRepository
) : IGetAppFeatureFlagsUseCase {

    override operator fun invoke(): StateFlow<AppFeatureFlags> {
        return repository.featureFlags
    }
}
