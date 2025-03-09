package com.zero.fundsync.utils

import android.content.Context
import com.zero.fundsync.model.UpiNotification
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Manages persistent storage of notifications in a JSON file.
 * Uses read/write locks to ensure thread safety.
 */
class StorageManager(private val context: Context) {
    companion object {
        private const val TAG = "StorageManager"
        private const val NOTIFICATIONS_FILE = "notifications.json"
        private const val MAX_NOTIFICATIONS = 1000  // Store up to 1000 notifications
    }

    private val notificationsFile: File
        get() = File(context.filesDir, NOTIFICATIONS_FILE)
        
    // Lock for thread-safe file operations
    private val fileLock = ReentrantReadWriteLock()

    /**
     * Save notifications to JSON file
     */
    fun saveNotifications(notifications: List<UpiNotification>) {
        if (notifications.isEmpty()) {
            // No need to write an empty list
            return
        }
        
        fileLock.write {
            try {
                BufferedWriter(FileWriter(notificationsFile)).use { writer ->
                    val jsonArray = JSONArray()
                    notifications.forEach { notification ->
                        val jsonObject = JSONObject().apply {
                            put("amount", notification.amount)
                            put("sender", notification.sender)
                            put("timestamp", notification.timestamp)
                            put("donationId", notification.donationId)
                            put("isSuccess", notification.isSuccess)
                            put("errorMessage", notification.errorMessage)
                        }
                        jsonArray.put(jsonObject)
                    }
                    
                    writer.write(jsonArray.toString())
                    writer.flush()
                    Logger.d(TAG, "Saved ${notifications.size} notifications to file")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to save notifications", e)
            }
        }
    }

    /**
     * Load notifications from JSON file
     */
    fun getNotifications(): List<UpiNotification> {
        return fileLock.read {
            if (!notificationsFile.exists() || notificationsFile.length() == 0L) {
                return@read emptyList<UpiNotification>()
            }
            
            try {
                BufferedReader(FileReader(notificationsFile)).use { reader ->
                    val jsonString = reader.readText()
                    if (jsonString.isBlank()) {
                        return@read emptyList<UpiNotification>()
                    }
                    
                    val jsonArray = JSONArray(jsonString)
                    val notifications = mutableListOf<UpiNotification>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        notifications.add(
                            UpiNotification(
                                amount = jsonObject.getString("amount"),
                                sender = jsonObject.getString("sender"),
                                timestamp = jsonObject.getLong("timestamp"),
                                donationId = jsonObject.optString("donationId", null),
                                isSuccess = jsonObject.optBoolean("isSuccess", true),
                                errorMessage = jsonObject.optString("errorMessage", null)
                            )
                        )
                    }
                    
                    Logger.d(TAG, "Loaded ${notifications.size} notifications from file")
                    notifications
                }
            } catch (e: IOException) {
                Logger.e(TAG, "Failed to read from notification file", e)
                emptyList()
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to parse notifications", e)
                // If the file is corrupted, delete it
                try {
                    notificationsFile.delete()
                    Logger.d(TAG, "Deleted corrupted notifications file")
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to delete corrupted file", e)
                }
                emptyList()
            }
        }
    }

    /**
     * Clear all stored notifications
     */
    fun clearNotifications() {
        fileLock.write {
            try {
                if (notificationsFile.exists()) {
                    notificationsFile.delete()
                    Logger.d(TAG, "Cleared all notifications")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to clear notifications", e)
            }
        }
    }
} 