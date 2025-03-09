package com.zero.fundsync.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.zero.fundsync.FundSyncApplication
import com.zero.fundsync.MainActivity
import com.zero.fundsync.utils.Logger
import java.util.regex.Pattern

class FundSyncNotificationListener : NotificationListenerService() {
    companion object {
        private const val TAG = "NotificationListener"
        private val UPI_APPS = setOf(
            "com.google.android.apps.nbu.paisa.user",  // GPay
            "net.one97.paytm",                         // Paytm
            "com.phonepe.app",                         // PhonePe
            "in.amazon.mShop.android.shopping"         // Amazon
        )
        
        private val AMOUNT_PATTERN = Pattern.compile("(?:Rs\\.?|₹)\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)")
        private val NAME_PATTERN = Pattern.compile("from\\s+([\\w\\s]+)")
    }

    private lateinit var streamlabsService: StreamlabsService

    override fun onCreate() {
        super.onCreate()
        streamlabsService = StreamlabsService(applicationContext)
        Logger.d(TAG, "NotificationListener service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        streamlabsService.cleanup()
        Logger.d(TAG, "NotificationListener service destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!UPI_APPS.contains(sbn.packageName)) return

        val notification = sbn.notification
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        
        // Check if it's a payment received notification
        if (!text.contains("received", ignoreCase = true)) return

        val amountMatcher = AMOUNT_PATTERN.matcher(text)
        val nameMatcher = NAME_PATTERN.matcher(text)

        if (amountMatcher.find() && nameMatcher.find()) {
            try {
                val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return
                val amount = amountStr.toDouble() // Convert to Double for proper formatting
                val sender = nameMatcher.group(1)?.trim() ?: "Unknown"

                Logger.d(TAG, "Payment received: ₹${String.format("%.2f", amount)} from $sender")

                // Send to Streamlabs
                if (streamlabsService.isAuthenticated()) {
                    try {
                        Logger.d(TAG, "Attempting to send UPI payment to Streamlabs")
                        streamlabsService.sendDonation(
                            name = sender,
                            amount = amount,
                            message = "UPI Payment received"
                        )
                        Logger.d(TAG, "Successfully forwarded UPI payment to Streamlabs")
                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to send UPI payment to Streamlabs API", e)
                    }
                } else {
                    Logger.w(TAG, "Streamlabs not authenticated, skipping API call")
                }

                // Update UI if MainActivity is active
                FundSyncApplication.instance.runOnUiThread {
                    (FundSyncApplication.instance.getCurrentActivity() as? MainActivity)?.let { activity ->
                        activity.addNotification(String.format("%.2f", amount), sender)
                    }
                }
            } catch (e: NumberFormatException) {
                Logger.e(TAG, "Failed to parse amount from notification", e)
                Logger.d(TAG, "Notification text: $text")
            }
        } else {
            Logger.w(TAG, "Could not extract amount or sender from notification")
            Logger.d(TAG, "Notification text: $text")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for our use case
    }
} 