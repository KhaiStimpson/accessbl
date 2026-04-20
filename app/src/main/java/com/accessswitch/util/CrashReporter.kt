package com.accessswitch.util

import android.util.Log

/**
 * Placeholder for crash reporting integration.
 *
 * In production, this would be replaced with Firebase Crashlytics or similar.
 * Currently logs to Logcat for development purposes.
 *
 * Usage:
 * - CrashReporter.log("message") for breadcrumbs
 * - CrashReporter.recordException(e) for non-fatal errors
 * - CrashReporter.setUserId(id) for user identification
 *
 * To enable Firebase Crashlytics:
 * 1. Add firebase-crashlytics dependency to build.gradle.kts
 * 2. Replace implementations below with Firebase.crashlytics calls
 * 3. Add google-services.json to app/
 */
object CrashReporter {

    private const val TAG = "AccessSwitch"

    /**
     * Log a message as a breadcrumb for crash reports.
     */
    fun log(message: String) {
        Log.d(TAG, message)
        // Firebase: Firebase.crashlytics.log(message)
    }

    /**
     * Record a non-fatal exception.
     */
    fun recordException(throwable: Throwable) {
        Log.e(TAG, "Non-fatal exception", throwable)
        // Firebase: Firebase.crashlytics.recordException(throwable)
    }

    /**
     * Record a non-fatal exception with a message.
     */
    fun recordException(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
        // Firebase: Firebase.crashlytics.log(message)
        // Firebase: Firebase.crashlytics.recordException(throwable)
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    fun setCustomKey(key: String, value: String) {
        Log.d(TAG, "Custom key: $key = $value")
        // Firebase: Firebase.crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    fun setCustomKey(key: String, value: Boolean) {
        Log.d(TAG, "Custom key: $key = $value")
        // Firebase: Firebase.crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    fun setCustomKey(key: String, value: Int) {
        Log.d(TAG, "Custom key: $key = $value")
        // Firebase: Firebase.crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a user identifier (anonymized).
     */
    fun setUserId(userId: String) {
        Log.d(TAG, "User ID set (not logged for privacy)")
        // Firebase: Firebase.crashlytics.setUserId(userId)
    }

    /**
     * Report that a critical section started.
     * Helps identify which operation was running during a crash.
     */
    fun beginCriticalSection(name: String) {
        log("BEGIN: $name")
        setCustomKey("current_operation", name)
    }

    /**
     * Report that a critical section ended.
     */
    fun endCriticalSection(name: String) {
        log("END: $name")
        setCustomKey("current_operation", "idle")
    }
}
