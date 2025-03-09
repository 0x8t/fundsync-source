package com.zero.fundsync.service

import android.content.Context
import android.util.Log
import com.zero.fundsync.R
import com.zero.fundsync.utils.Logger
import com.zero.fundsync.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class StreamlabsService(private val context: Context) {
    private val client: OkHttpClient
    private val preferenceManager = PreferenceManager(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private var notificationCallback: ((String, String, String?, Boolean, String?) -> Unit)? = null

    companion object {
        private const val TAG = "StreamlabsService"
        private const val BASE_URL = "https://streamlabs.com/api/v2.0"
        private val FORM = "application/x-www-form-urlencoded".toMediaType()
        
        // OkHttp cache size: 10 MB
        private const val CACHE_SIZE = 10 * 1024 * 1024L
        
        // Timeouts
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 20L
        private const val WRITE_TIMEOUT = 15L
    }
    
    init {
        val cacheDir = context.cacheDir
        val cache = Cache(cacheDir, CACHE_SIZE)
        
        client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .cache(cache)
            .build()
    }

    fun setNotificationCallback(callback: (amount: String, sender: String, donationId: String?, isSuccess: Boolean, errorMessage: String?) -> Unit) {
        notificationCallback = callback
    }

    // Cancel all coroutines when service is no longer needed
    fun cleanup() {
        (scope.coroutineContext[Job] as? Job)?.cancel()
        notificationCallback = null
    }

    fun isAuthenticated(): Boolean {
        return preferenceManager.getAccessToken() != null
    }

    fun handleAuthCode(code: String) {
        scope.launch {
            try {
                val response = getAccessToken(code)
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "")
                    val accessToken = json.getString("access_token")
                    val refreshToken = json.optString("refresh_token", "")
                    preferenceManager.saveTokens(accessToken, refreshToken)
                    Logger.d(TAG, "Successfully obtained access token")
                } else {
                    Logger.e(TAG, "Failed to get access token: ${response.code}")
                    Logger.e(TAG, "Error response: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error exchanging code for token", e)
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun getAccessToken(code: String): Response {
        val clientId = context.getString(R.string.streamlabs_client_id)
        val clientSecret = context.getString(R.string.streamlabs_client_secret)
        val redirectUri = context.getString(R.string.streamlabs_redirect_uri)

        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("redirect_uri", redirectUri)
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/token")
            .post(formBody)
            .build()

        return client.newCall(request).execute()
    }

    fun sendDonation(name: String, amount: Double, message: String) {
        if (!isAuthenticated()) {
            Logger.e(TAG, "Cannot send donation: Not authenticated")
            return
        }

        scope.launch {
            try {
                val accessToken = preferenceManager.getAccessToken()

                val json = JSONObject().apply {
                    // Required fields as per API docs
                    put("name", name)
                    put("message", message)
                    put("identifier", System.currentTimeMillis().toString())
                    put("amount", amount) // Amount as number
                    put("currency", "INR")
                    put("skip_alert", "no")
                }

                Logger.d(TAG, "Sending donation request to Streamlabs API")
                Logger.d(TAG, "Request payload: ${json.toString()}")

                val mediaType = "application/json".toMediaType()
                val body = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL/donations")
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        Logger.d(TAG, "Successfully sent donation to Streamlabs")
                        Logger.d(TAG, "Response: $responseBody")
                        
                        // Parse donation ID from response
                        val responseJson = JSONObject(responseBody ?: "{}")
                        val donationId = responseJson.optString("donation_id")
                        
                        // Notify UI through callback
                        notificationCallback?.invoke(
                            String.format("%.2f", amount),
                            name,
                            donationId,
                            true,
                            null
                        )
                    } else {
                        Logger.e(TAG, "Failed to send donation. Status code: ${response.code}")
                        Logger.e(TAG, "Error response: $responseBody")
                        
                        // Parse error message from response
                        val errorJson = JSONObject(responseBody ?: "{}")
                        val errorMessage = errorJson.optString("message", "Unknown error")
                        
                        // Notify UI through callback
                        notificationCallback?.invoke(
                            String.format("%.2f", amount),
                            name,
                            null,
                            false,
                            errorMessage
                        )
                        
                        if (response.code == 401) {
                            Logger.e(TAG, "Authentication error - token may be invalid")
                            preferenceManager.clearTokens()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error sending donation to Streamlabs", e)
                // Notify UI through callback
                notificationCallback?.invoke(
                    String.format("%.2f", amount),
                    name,
                    null,
                    false,
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun sendTestDonation() {
        Logger.d(TAG, "Sending test donation")
        sendDonation("Test User", 1.0, "Test donation from FundSync")
    }
} 