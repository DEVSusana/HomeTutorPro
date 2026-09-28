package com.devsusana.hometutorpro
 
import android.content.Context
import android.content.ContextWrapper
import androidx.hilt.work.HiltWorkerFactory
import com.devsusana.hometutorpro.domain.usecases.AppInitializer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
 
/**
 * Unit tests for [HiltApplication].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HiltApplicationTest {
 
    /**
     * Verifies that the WorkManager configuration is created and provided correctly with the injected factory.
     */
    @Test
    fun testWorkManagerConfiguration() {
        val app = HiltApplication()
        app.workerFactory = mockk()
        
        val config = app.workManagerConfiguration
        assertNotNull(config)
    }
 
    /**
     * Verifies that app initializer is triggered on application onCreate.
     */
    @Test
    fun testOnCreate() {
        val app = HiltApplication()
        
        // Attach base context to avoid NullPointerException inside super.onCreate()
        val context = RuntimeEnvironment.getApplication()
        val attachBaseContextMethod = ContextWrapper::class.java.getDeclaredMethod(
            "attachBaseContext", 
            Context::class.java
        )
        attachBaseContextMethod.isAccessible = true
        attachBaseContextMethod.invoke(app, context)
        
        // Bypass Hilt internal injection to test with a mock AppInitializer
        try {
            val injectedField = app.javaClass.superclass.getDeclaredField("injected")
            injectedField.isAccessible = true
            injectedField.set(app, true)
        } catch (e: Exception) {
            // In case the field name is different or doesn't exist
        }
        
        val mockInitializer = mockk<AppInitializer>(relaxed = true)
        app.appInitializer = mockInitializer
        
        app.onCreate()
        
        verify { mockInitializer.initialize() }
    }
}

