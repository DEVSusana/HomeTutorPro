package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.domain.usecases.*
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.ToggleScheduleCompletionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetStudentByIdUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.LoginUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.LogoutUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.RegisterPaymentUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.RegisterUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveScheduleExceptionUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveStudentUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.UploadResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.GetSharedResourcesUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveSharedResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteSharedResourceUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.MigrateDataFromLocalUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.UpdateProfileUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.UpdatePasswordUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindDeleteResourceUseCase(impl: DeleteResourceUseCase): IDeleteResourceUseCase

    @Binds
    abstract fun bindDeleteScheduleExceptionUseCase(impl: DeleteScheduleExceptionUseCase): IDeleteScheduleExceptionUseCase

    @Binds
    abstract fun bindDeleteScheduleUseCase(impl: DeleteScheduleUseCase): IDeleteScheduleUseCase

    @Binds
    abstract fun bindDeleteStudentUseCase(impl: DeleteStudentUseCase): IDeleteStudentUseCase

    @Binds
    abstract fun bindGetCurrentUserUseCase(impl: GetCurrentUserUseCase): IGetCurrentUserUseCase

    @Binds
    abstract fun bindGetResourcesUseCase(impl: GetResourcesUseCase): IGetResourcesUseCase

    @Binds
    abstract fun bindGetScheduleExceptionsUseCase(impl: GetScheduleExceptionsUseCase): IGetScheduleExceptionsUseCase

    @Binds
    abstract fun bindGetSchedulesUseCase(impl: GetSchedulesUseCase): IGetSchedulesUseCase

    @Binds
    abstract fun bindGetAllSchedulesUseCase(impl: GetAllSchedulesUseCase): IGetAllSchedulesUseCase

    @Binds
    abstract fun bindGetStudentByIdUseCase(impl: GetStudentByIdUseCase): IGetStudentByIdUseCase

    @Binds
    abstract fun bindGetStudentsUseCase(impl: GetStudentsUseCase): IGetStudentsUseCase

    @Binds
    abstract fun bindLoginUseCase(impl: LoginUseCase): ILoginUseCase

    @Binds
    abstract fun bindLogoutUseCase(impl: LogoutUseCase): ILogoutUseCase

    @Binds
    abstract fun bindRegisterPaymentUseCase(impl: RegisterPaymentUseCase): IRegisterPaymentUseCase

    @Binds
    abstract fun bindRegisterUseCase(impl: RegisterUseCase): IRegisterUseCase

    @Binds
    abstract fun bindSaveScheduleExceptionUseCase(impl: SaveScheduleExceptionUseCase): ISaveScheduleExceptionUseCase

    @Binds
    abstract fun bindSaveScheduleUseCase(impl: SaveScheduleUseCase): ISaveScheduleUseCase

    @Binds
    abstract fun bindSaveStudentUseCase(impl: SaveStudentUseCase): ISaveStudentUseCase

    @Binds
    abstract fun bindUploadResourceUseCase(impl: UploadResourceUseCase): IUploadResourceUseCase
    
    // Shared Resources Use Cases
    @Binds
    abstract fun bindGetSharedResourcesUseCase(impl: GetSharedResourcesUseCase): IGetSharedResourcesUseCase
    
    @Binds
    abstract fun bindSaveSharedResourceUseCase(impl: SaveSharedResourceUseCase): ISaveSharedResourceUseCase
    
    @Binds
    abstract fun bindDeleteSharedResourceUseCase(impl: DeleteSharedResourceUseCase): IDeleteSharedResourceUseCase
    
    @Binds
    abstract fun bindMigrateDataFromLocalUseCase(impl: MigrateDataFromLocalUseCase): IMigrateDataFromLocalUseCase

    @Binds
    abstract fun bindToggleScheduleCompletionUseCase(impl: ToggleScheduleCompletionUseCase): IToggleScheduleCompletionUseCase

    @Binds
    abstract fun bindUpdateProfileUseCase(impl: UpdateProfileUseCase): IUpdateProfileUseCase

    @Binds
    abstract fun bindUpdatePasswordUseCase(impl: UpdatePasswordUseCase): IUpdatePasswordUseCase

    @Binds
    abstract fun bindValidateStudentUseCase(impl: com.devsusana.hometutorpro.domain.usecases.implementations.ValidateStudentUseCase): IValidateStudentUseCase

    @Binds
    abstract fun bindCheckScheduleConflictUseCase(impl: com.devsusana.hometutorpro.domain.usecases.implementations.CheckScheduleConflictUseCase): ICheckScheduleConflictUseCase
}
