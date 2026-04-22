package com.accessswitch.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton in-app startup logger.
 *
 * Captures timestamped log entries during app startup so they can be
 * displayed in the in-app DiagnosticsPanel when logcat is unavailable.
 *
 * Usage: StartupLogger.log("Step complete")
 */
object StartupLogger {

    private const val TAG = "AccessSwitch"
    private const val MAX_ENTRIES = 100

    data class Entry(
        val elapsedMs: Long,
        val message: String,
        val isError: Boolean = false
    )

    private val startTimeMs = System.currentTimeMillis()

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun log(message: String) {
        val entry = Entry(
            elapsedMs = System.currentTimeMillis() - startTimeMs,
            message = message
        )
        Log.d(TAG, "[+${entry.elapsedMs}ms] $message")
        _entries.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        val entry = Entry(
            elapsedMs = System.currentTimeMillis() - startTimeMs,
            message = if (throwable != null) "$message: ${throwable.message}" else message,
            isError = true
        )
        Log.e(TAG, "[+${entry.elapsedMs}ms] $message", throwable)
        _entries.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
