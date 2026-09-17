package com.devsusana.hometutorpro.domain.entities

/**
 * Represents the hardware and OS compatibility status for running the on-device Sue AI assistant.
 *
 * @property isSupported Whether the device meets minimum OS (Android 9.0+ / API 28) and RAM (3.5+ GB) requirements.
 * @property reason The reason why the device is not supported, or null if supported.
 * @property totalRamBytes The total physical RAM in bytes detected on the device.
 * @property sdkVersion The Android SDK version of the device.
 */
data class SueDeviceCompatibility(
    val isSupported: Boolean,
    val reason: SueUnsupportedReason? = null,
    val totalRamBytes: Long = 0L,
    val sdkVersion: Int = 0
)

/**
 * Enumeration of reasons why a device cannot run the Sue on-device LLM assistant.
 */
enum class SueUnsupportedReason {
    /** The Android OS version is lower than Android 9.0 (API 28). */
    UNSUPPORTED_ANDROID_VERSION,

    /** The device has less than the minimum required physical RAM (e.g. < 3.5 GB) or is low-RAM. */
    INSUFFICIENT_RAM
}
