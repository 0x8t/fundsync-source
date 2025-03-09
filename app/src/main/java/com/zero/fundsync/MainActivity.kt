package com.zero.fundsync

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.zero.fundsync.adapter.NotificationsAdapter
import com.zero.fundsync.databinding.ActivityMainBinding
import com.zero.fundsync.model.UpiNotification
import com.zero.fundsync.service.FundSyncNotificationListener
import com.zero.fundsync.service.StreamlabsService
import com.zero.fundsync.utils.Logger
import com.zero.fundsync.utils.PreferenceManager
import com.zero.fundsync.utils.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var streamlabsService: StreamlabsService
    private lateinit var storageManager: StorageManager
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private var isFirstNotificationPermissionCheck = true
    private var isStreamlabsDialogShown = false
    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notifications = mutableListOf<UpiNotification>()
    private var currentDialog: AlertDialog? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val DISCORD_INVITE_LINK = "https://discord.gg/Yjrq3wNhAs"
        private const val MAX_NOTIFICATIONS = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize preference manager first to apply theme
        preferenceManager = PreferenceManager(this)
        
        // Apply saved theme before inflating views
        preferenceManager.applyTheme()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        streamlabsService = StreamlabsService(this)
        storageManager = StorageManager(this)

        // Set up notification callback
        streamlabsService.setNotificationCallback { amount, sender, donationId, isSuccess, errorMessage ->
            runOnUiThread {
                addNotification(amount, sender, donationId, isSuccess, errorMessage)
            }
        }

        // Restore state
        isFirstNotificationPermissionCheck = !preferenceManager.getBoolean("notification_dialog_shown", false)
        
        // Load notifications from storage
        loadNotifications()

        setupToolbar()
        setupThemeToggle()
        setupDiscordButton()
        setupDebugButton()
        setupRecyclerView()
        setupTitleClick()
        
        // Only check notification permission if not changing configurations
        if (!isChangingConfigurations) {
            checkNotificationPermission()
        }
        
        // Handle OAuth callback
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        // No blur effect or alpha changes - using solid color
    }

    private fun setupThemeToggle() {
        binding.themeToggleButton.setOnClickListener {
            // Get current theme state
            val isDarkMode = preferenceManager.isDarkMode()
            // Toggle to opposite theme
            preferenceManager.setDarkMode(!isDarkMode)
            // Icon will update when activity recreates
        }
        
        // Set initial icon based on current theme
        updateThemeIcon()
    }
    
    /**
     * Update the theme toggle icon based on current theme
     */
    private fun updateThemeIcon() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES
        
        // Update theme toggle icon
        binding.themeToggleButton.setImageResource(
            if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_theme_toggle
        )
    }

    private fun setupDiscordButton() {
        binding.discordButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, DISCORD_INVITE_LINK.toUri())
            startActivity(intent)
        }
    }

    private fun setupDebugButton() {
        binding.debugButton.setOnClickListener {
            if (streamlabsService.isAuthenticated()) {
                binding.debugButton.isEnabled = false
                
                mainScope.launch {
                    try {
                        // Send test donation
                        streamlabsService.sendTestDonation()
                        
                        // Add to notification list
                        addNotification("1.00", "Test Donation")
                        
                        // Show success toast
                        Toast.makeText(
                            this@MainActivity,
                            "Test donation sent to Streamlabs",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        // Show error toast
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to send test donation: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Logger.e(TAG, "Test donation failed", e)
                    } finally {
                        binding.debugButton.isEnabled = true
                    }
                }
            } else {
                showConnectDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter()
        binding.notificationsRecyclerView.apply {
            adapter = notificationsAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            // No scroll listener needed for opacity changes
        }
        
        // Submit the loaded notifications to the adapter
        notificationsAdapter.submitList(ArrayList(notifications))
    }

    fun addNotification(
        amount: String,
        sender: String,
        donationId: String? = null,
        isSuccess: Boolean = true,
        errorMessage: String? = null
    ) {
        if (notifications.size >= MAX_NOTIFICATIONS) {
            notifications.removeAt(notifications.lastIndex)
        }
        
        notifications.add(0, UpiNotification(
            amount = amount,
            sender = sender,
            timestamp = System.currentTimeMillis(),
            donationId = donationId,
            isSuccess = isSuccess,
            errorMessage = errorMessage
        ))
        
        notificationsAdapter.submitList(ArrayList(notifications))
        storageManager.saveNotifications(notifications)
        
        // Scroll to top when new notification is added
        binding.notificationsRecyclerView.smoothScrollToPosition(0)
        
        // Ensure toolbar is expanded
        binding.appBarLayout.setExpanded(true, true)
    }

    private fun setupTitleClick() {
        binding.titleText.setOnClickListener {
            if (streamlabsService.isAuthenticated()) {
                showTokenWarningDialog()
            } else {
                showAboutDialog()
            }
        }
    }

    private fun showAboutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_layout, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val positiveButton = dialogView.findViewById<MaterialButton>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<MaterialButton>(R.id.negativeButton)
        
        dialogTitle.text = "About FundSync"
        dialogMessage.text = "FundSync monitors UPI payments and forwards them to Streamlabs. Connect your Streamlabs account to get started."
        positiveButton.text = "Connect"
        negativeButton.text = "Cancel"
        
        val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()
        
        positiveButton.setOnClickListener {
            dialog.dismiss()
            if (!streamlabsService.isAuthenticated()) {
                showConnectDialog()
            }
        }
        
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showTokenWarningDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_layout, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val positiveButton = dialogView.findViewById<MaterialButton>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<MaterialButton>(R.id.negativeButton)
        
        dialogTitle.text = "Warning"
        dialogMessage.text = "You already have a Streamlabs account connected. Continuing will disconnect the current account and require you to connect again."
        positiveButton.text = "Continue"
        negativeButton.text = "Cancel"
        
        val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()
        
        positiveButton.setOnClickListener {
            dialog.dismiss()
            showAboutDialog()
        }
        
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri?.scheme == "fundsync" && uri.host == "com.zero.fundsync") {
                val code = uri.getQueryParameter("code")
                val error = uri.getQueryParameter("error")
                
                if (error != null) {
                    Logger.e(TAG, "OAuth error: $error")
                    Toast.makeText(this, "Authentication failed: $error", Toast.LENGTH_LONG).show()
                    return
                }
                
                if (code != null) {
                    streamlabsService.handleAuthCode(code)
                    isStreamlabsDialogShown = true  // Mark that we've handled auth
                    
                    // Check authentication status after a delay
                    mainScope.launch {
                        delay(2000) // Wait for token exchange to complete
                        if (streamlabsService.isAuthenticated()) {
                            Toast.makeText(this@MainActivity, "Successfully connected to Streamlabs!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to connect to Streamlabs. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = "${packageName}/${FundSyncNotificationListener::class.java.canonicalName}"
        return enabledListeners?.contains(componentName) == true
    }

    private fun checkNotificationPermission() {
        val hasPermission = isNotificationServiceEnabled()
        
        if (!hasPermission) {
            showNotificationPermissionDialog()
        } else if (isFirstNotificationPermissionCheck && !isStreamlabsDialogShown) {
            // Only show Streamlabs dialog if we haven't shown it yet and haven't handled auth
            isFirstNotificationPermissionCheck = false
            if (!streamlabsService.isAuthenticated()) {
                mainScope.launch {
                    delay(500) // Short delay to ensure UI is ready
                    showConnectDialog()
                }
            }
        }
    }

    private fun showNotificationPermissionDialog() {
        // Check if dialog is already showing
        if (isFinishing || currentDialog?.isShowing == true) {
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_layout, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val positiveButton = dialogView.findViewById<MaterialButton>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<MaterialButton>(R.id.negativeButton)
        
        dialogTitle.text = "Notification Access Required"
        dialogMessage.text = "FundSync needs notification access to monitor UPI payments. Please grant access to continue."
        
        // Add extra bottom margin to the dialog message
        val layoutParams = dialogMessage.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.dialog_message_bottom_margin)
        dialogMessage.layoutParams = layoutParams
        
        positiveButton.text = "Grant Access"
        negativeButton.visibility = View.GONE
        
        currentDialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        positiveButton.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            currentDialog?.dismiss()
        }
        
        currentDialog?.show()
    }

    private fun showConnectDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_layout, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val positiveButton = dialogView.findViewById<MaterialButton>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<MaterialButton>(R.id.negativeButton)
        
        dialogTitle.text = "Connect to Streamlabs"
        dialogMessage.text = "You need to connect your Streamlabs account to use this feature."
        positiveButton.text = "Connect"
        negativeButton.text = "Cancel"
        
        val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()
        
        positiveButton.setOnClickListener {
            dialog.dismiss()
            initiateStreamlabsAuth()
        }
        
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun initiateStreamlabsAuth() {
        val clientId = getString(R.string.streamlabs_client_id)
        val redirectUri = getString(R.string.streamlabs_redirect_uri)
        val scope = "donations.read donations.create"
        val state = "123456"

        val authUrl = "https://streamlabs.com/api/v2.0/authorize?" +
                "client_id=$clientId&" +
                "redirect_uri=$redirectUri&" +
                "scope=$scope&" +
                "response_type=code&" +
                "state=$state"

        startActivity(Intent(Intent.ACTION_VIEW, authUrl.toUri()))
    }

    /**
     * Load notifications from storage file
     */
    private fun loadNotifications() {
        // Clear existing notifications to avoid duplicates
        notifications.clear()
        
        // Load from storage
        val storedNotifications = storageManager.getNotifications()
        if (storedNotifications.isNotEmpty()) {
            Logger.d(TAG, "Loaded ${storedNotifications.size} notifications from storage")
            notifications.addAll(storedNotifications)
            
            // Update adapter if it's initialized
            if (::notificationsAdapter.isInitialized) {
                notificationsAdapter.submitList(ArrayList(notifications))
            }
        } else {
            Logger.d(TAG, "No notifications found in storage")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Save notifications during configuration change
        storageManager.saveNotifications(notifications)
        
        // Handle UI updates for theme change
        if ((newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) != 
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)) {
            // Theme has changed, update UI elements
            updateThemeIcon()
            
            // Recreate all dialogs if they're showing
            if (currentDialog?.isShowing == true) {
                val oldDialog = currentDialog
                currentDialog = null
                oldDialog?.dismiss()
                
                // Re-show the appropriate dialog
                if (!isNotificationServiceEnabled()) {
                    showNotificationPermissionDialog()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Reload notifications when app is resumed
        loadNotifications()
        Logger.d(TAG, "Reloaded notifications in onResume")
        
        // Check notification permission status
        val notificationEnabled = isNotificationServiceEnabled()
        
        if (notificationEnabled) {
            // Permission granted, dismiss any showing dialog
            currentDialog?.dismiss()
            currentDialog = null
            
            // Only show Streamlabs dialog if this is the first launch and we haven't shown it yet
            if (isFirstNotificationPermissionCheck && !isChangingConfigurations && !isStreamlabsDialogShown) {
                isFirstNotificationPermissionCheck = false
                if (!streamlabsService.isAuthenticated()) {
                    mainScope.launch {
                        delay(500) // Short delay to ensure UI is ready
                        showConnectDialog()
                    }
                }
            }
        } else {
            // Still no permission, show dialog if not already showing
            showNotificationPermissionDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        // Save the current state
        preferenceManager.setBoolean("notification_dialog_shown", !isFirstNotificationPermissionCheck)
        // Save notifications when app is paused
        storageManager.saveNotifications(notifications)
        Logger.d(TAG, "Saved notifications in onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when activity is destroyed
        (mainScope.coroutineContext[Job] as? Job)?.cancel()
        // Clean up resources
        currentDialog?.dismiss()
        currentDialog = null
        streamlabsService.cleanup()
    }
}