# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================================
# SECURITY: Code Obfuscation and Optimization
# ============================================================================

# Aggressive obfuscation
-repackageclasses 'o'
-allowaccessmodification
-optimizationpasses 5
-overloadaggressively

# Keep line numbers for better crash reports (but obfuscate everything else)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ============================================================================
# Firebase
# ============================================================================

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep data models for Firebase Firestore
-keep class com.devsusana.hometutorpro.data.models.** { *; }
-keep class com.devsusana.hometutorpro.domain.entities.** { *; }

# ============================================================================
# Kotlin Coroutines
# ============================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ============================================================================
# Jetpack Compose
# ============================================================================

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================================
# Hilt
# ============================================================================

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn com.google.errorprone.annotations.**

# Keep all classes that use @Inject
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}

# ============================================================================
# Google Play Services
# ============================================================================

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================================
# Room Database
# ============================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============================================================================
# Security: Encrypted SharedPreferences
# ============================================================================

-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ============================================================================
# Prevent stripping of native methods
# ============================================================================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# Keep custom exceptions for better crash reporting
# ============================================================================

-keep public class * extends java.lang.Exception