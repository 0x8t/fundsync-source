package com.zero.fundsync

import android.app.Activity
import android.app.Application
import android.os.Bundle

class FundSyncApplication : Application() {
    private var currentActivity: Activity? = null

    companion object {
        lateinit var instance: FundSyncApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun getCurrentActivity(): Activity? = currentActivity

    fun runOnUiThread(action: () -> Unit) {
        currentActivity?.runOnUiThread(action)
    }
} 