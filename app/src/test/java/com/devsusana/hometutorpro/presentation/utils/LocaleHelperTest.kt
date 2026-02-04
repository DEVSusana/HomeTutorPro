package com.devsusana.hometutorpro.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Unit tests for LocaleHelper.
 * Uses Robolectric to simulate Android environment.
 */
@RunWith(AndroidJUnit4::class)
class LocaleHelperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun getCurrentLanguage_returnsCorrectLanguage_legacy() {
        // Given: A context with specific locale (legacy way)
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()
        val config = Configuration()
        config.setLocale(Locale("es"))
        
        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns config

        // When: Get current language
        // Force legacy path by checking SDK_INT if possible, but Robolectric defaults to high SDK.
        // We can just verify it returns what's in configuration.
        val language = LocaleHelper.getCurrentLanguage(mockContext)

        // Then: Should return "es"
        assertEquals("es", language)
    }

    @Test
    fun setLocale_recreatesActivity() {
        // Given: A mock activity
        val mockActivity = mockk<Activity>(relaxed = true)

        // When: Set locale
        LocaleHelper.setLocale(mockActivity, "en")

        // Then: Activity should be recreated
        verify { mockActivity.recreate() }
    }
    
    // Note: onAttach is difficult to test because it relies on DataStore extension property
    // which is hard to mock without full integration test or dependency injection.
    // We skip it here as it's covered by LanguageSettingsPersistenceTest integration test.
}
