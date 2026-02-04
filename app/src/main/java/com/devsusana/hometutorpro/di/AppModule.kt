package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.data.repository.AuthRepositoryImpl
import android.content.Context
import com.devsusana.hometutorpro.data.repository.ResourceRepositoryImpl
import com.devsusana.hometutorpro.data.repository.ScheduleExceptionRepositoryImpl
import com.devsusana.hometutorpro.data.repository.StudentRepositoryImpl
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    // Repositories are bound in RepositoryModule.kt

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage = 
            FirebaseStorage.getInstance("gs://hometutorpro.firebasestorage.app")

        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = 
            WorkManager.getInstance(context)
    }
}
