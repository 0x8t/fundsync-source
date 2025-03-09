package com.zero.fundsync.utils

import android.util.Log
import com.zero.fundsync.BuildConfig

/**
 * Custom logger that controls log output based on build type.
 * Debug logs are only shown in debug builds to improve performance in production.
 */
object Logger {
    private const val MAX_LOG_LENGTH = 4000

    /**
     * Log debug message - only shows in debug builds
     */
    fun d(tag: String, message: String) {
        // Use the standard BuildConfig.DEBUG flag which is always available
        if (BuildConfig.DEBUG) {
            log(Log.DEBUG, tag, message)
        }
    }

    /**
     * Log informational message
     */
    fun i(tag: String, message: String) {
        log(Log.INFO, tag, message)
    }

    /**
     * Log warning message
     */
    fun w(tag: String, message: String) {
        log(Log.WARN, tag, message)
    }

    /**
     * Log error message
     */
    fun e(tag: String, message: String) {
        log(Log.ERROR, tag, message)
    }

    /**
     * Log error message with exception
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        log(Log.ERROR, tag, "$message\n${throwable.stackTraceToString()}")
    }

    /**
     * Internal method to handle log splitting for long messages
     */
    private fun log(priority: Int, tag: String, message: String) {
        // Split by line to ensure each line is properly formatted
        for (line in message.split('\n')) {
            // Split the message if needed (Android has a log message limit)
            var i = 0
            val length = line.length
            while (i < length) {
                val end = (i + MAX_LOG_LENGTH).coerceAtMost(length)
                Log.println(priority, tag, line.substring(i, end))
                i = end
            }
        }
    }
} 