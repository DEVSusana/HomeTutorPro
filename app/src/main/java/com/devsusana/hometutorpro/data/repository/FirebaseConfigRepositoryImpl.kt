package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConfigRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig
) : IConfigRepository {

    companion object {
        private const val TAG = "FirebaseConfigRepo"

        const val KEY_ENABLE_WIDGET_ACTIONS = "enable_widget_actions"
        const val KEY_ENABLE_SUE_LOCAL_AI = "enable_sue_local_ai"
        const val KEY_SUE_MODEL_URL = "sue_model_url"
        const val KEY_MAINTENANCE_MESSAGE = "maintenance_message"

        private const val FETCH_TIMEOUT_SECONDS = 10L
        private const val MIN_FETCH_INTERVAL_DEBUG_SECONDS = 0L
        private const val MIN_FETCH_INTERVAL_RELEASE_SECONDS = 3600L // 1 hour
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _featureFlags = MutableStateFlow(readCurrentFlags())
    override val featureFlags: StateFlow<AppFeatureFlags> = _featureFlags.asStateFlow()

    init {
        initializeConfig()
    }

    private fun initializeConfig() {
        try {
            val minInterval = if (BuildConfig.DEBUG) {
                MIN_FETCH_INTERVAL_DEBUG_SECONDS
            } else {
                MIN_FETCH_INTERVAL_RELEASE_SECONDS
            }

            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS)
                .setMinimumFetchIntervalInSeconds(minInterval)
                .build()

            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            // Activate previous fetched values immediately for offline-first fast startup
            remoteConfig.activate().addOnCompleteListener {
                _featureFlags.value = readCurrentFlags()
                SafeLogger.d(TAG, "Activated cached config on startup: ${_featureFlags.value}")
            }

            // Real-time remote config updates
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    SafeLogger.d(TAG, "Remote config updated in real time for keys: ${configUpdate.updatedKeys}")
                    remoteConfig.activate().addOnCompleteListener {
                        _featureFlags.value = readCurrentFlags()
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    SafeLogger.w(TAG, "Config update listener error: ${error.message}")
                }
            })

            // Async background fetch
            scope.launch {
                fetchAndActivate()
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Error initializing FirebaseRemoteConfig: ${e.message}", e)
        }
    }

    override suspend fun fetchAndActivate(): Boolean {
        return try {
            val updated = remoteConfig.fetchAndActivate().await()
            _featureFlags.value = readCurrentFlags()
            SafeLogger.d(TAG, "fetchAndActivate completed (updated=$updated), current flags: ${_featureFlags.value}")
            true
        } catch (e: Exception) {
            SafeLogger.w(TAG, "fetchAndActivate failed: ${e.message}")
            false
        }
    }

    override fun isWidgetActionsEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ENABLE_WIDGET_ACTIONS)

    override fun isSueLocalAiEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_ENABLE_SUE_LOCAL_AI)

    override fun getSueModelUrl(): String =
        remoteConfig.getString(KEY_SUE_MODEL_URL)

    override fun getMaintenanceMessage(): String =
        remoteConfig.getString(KEY_MAINTENANCE_MESSAGE)

    private fun readCurrentFlags(): AppFeatureFlags {
        return try {
            AppFeatureFlags(
                isWidgetActionsEnabled = remoteConfig.getBoolean(KEY_ENABLE_WIDGET_ACTIONS),
                isSueLocalAiEnabled = remoteConfig.getBoolean(KEY_ENABLE_SUE_LOCAL_AI),
                sueModelUrl = remoteConfig.getString(KEY_SUE_MODEL_URL),
                maintenanceMessage = remoteConfig.getString(KEY_MAINTENANCE_MESSAGE)
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Error reading flags from RemoteConfig, falling back to defaults", e)
            AppFeatureFlags()
        }
    }
}
