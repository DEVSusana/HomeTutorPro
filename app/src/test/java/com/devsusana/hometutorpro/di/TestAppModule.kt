package com.devsusana.hometutorpro.di

import android.content.Context
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.data.billing.BillingManager
import com.devsusana.hometutorpro.presentation.premium.BillingLauncher
import com.devsusana.hometutorpro.presentation.premium.PlayBillingLauncher
import com.devsusana.hometutorpro.core.utils.IUriReader
import com.devsusana.hometutorpro.core.utils.UriReader
import com.devsusana.hometutorpro.domain.usecases.AppInitializer
import com.devsusana.hometutorpro.presentation.utils.AppInitializerImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
abstract class TestAppModule {

    @Binds
    @Singleton
    abstract fun bindUriReader(uriReader: UriReader): IUriReader

    @Binds
    @Singleton
    abstract fun bindPremiumBillingService(billingManager: BillingManager): PremiumBillingService

    @Binds
    @Singleton
    abstract fun bindBillingLauncher(playBillingLauncher: PlayBillingLauncher): BillingLauncher

    @Binds
    @Singleton
    abstract fun bindAppInitializer(appInitializer: AppInitializerImpl): AppInitializer

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = mockk(relaxed = true)

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = mockk(relaxed = true)

        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage = mockk(relaxed = true)

        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = mockk(relaxed = true)

        @Provides
        @Singleton
        fun provideAuthValidator(): com.devsusana.hometutorpro.domain.core.AuthValidator =
            com.devsusana.hometutorpro.domain.core.AuthValidator
    }
}
