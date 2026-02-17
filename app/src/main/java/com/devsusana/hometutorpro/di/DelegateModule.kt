package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.presentation.student_detail.delegates.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class DelegateModule {

    @Binds
    @ViewModelScoped
    abstract fun bindStudentFinanceDelegate(impl: StudentFinanceDelegate): IStudentFinanceDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindStudentScheduleDelegate(impl: StudentScheduleDelegate): IStudentScheduleDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindStudentResourceDelegate(impl: StudentResourceDelegate): IStudentResourceDelegate
}
