package com.devsusana.hometutorpro.di

import android.content.Context
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.data.billing.BillingManager
import com.devsusana.hometutorpro.presentation.premium.BillingLauncher
import com.devsusana.hometutorpro.presentation.premium.PlayBillingLauncher
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

    @Binds
    @Singleton
    abstract fun bindBillingLauncher(playBillingLauncher: PlayBillingLauncher): BillingLauncher

    @Binds
    @Singleton
    abstract fun bindAppInitializer(appInitializer: com.devsusana.hometutorpro.presentation.utils.AppInitializerImpl): com.devsusana.hometutorpro.domain.usecases.AppInitializer

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

        @Provides
        @Singleton
        fun provideAuthValidator(): com.devsusana.hometutorpro.domain.core.AuthValidator =
            com.devsusana.hometutorpro.domain.core.AuthValidator
    }
}
