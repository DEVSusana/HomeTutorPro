package com.devsusana.hometutorpro.di

import android.content.Context
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.data.billing.BillingManager
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

    @Binds
    @Singleton
    abstract fun bindUriReader(uriReader: com.devsusana.hometutorpro.core.utils.UriReader): com.devsusana.hometutorpro.core.utils.IUriReader

    @Binds
    @Singleton
    abstract fun bindPremiumBillingService(billingManager: BillingManager): PremiumBillingService

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
