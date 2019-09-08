package com.rogertalk.roger.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import com.rogertalk.roger.android.services.talkhead.FloatingRogerService
import com.rogertalk.roger.event.broadcasts.AppVisibilityChangeEvent
import com.rogertalk.roger.repo.ClearTextPrefRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logInfo

class BackgroundManager constructor(val application: Application) : Application.ActivityLifecycleCallbacks {

    companion object {
        val BACKGROUND_DELAY: Long = 500
    }

    var isInBackground = true
        private set

    private val mBackgroundDelayHandler = Handler()
    private var mBackgroundTransition: Runnable? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
    }


    override fun onActivityResumed(activity: Activity) {
        if (mBackgroundTransition != null) {
            mBackgroundDelayHandler.removeCallbacks(mBackgroundTransition)
            mBackgroundTransition = null
        }

        if (isInBackground) {
            isInBackground = false
            appBecameForeground()
            logInfo { "Application went to foreground" }
        }
    }

    private fun appBecameForeground() {
        postEvent(AppVisibilityChangeEvent(true))
    }

    override fun onActivityPaused(activity: Activity) {
        if (!isInBackground && mBackgroundTransition == null) {
            mBackgroundTransition = Runnable {
                isInBackground = true
                mBackgroundTransition = null
                appBecameBackground()
                logDebug { "Application went to background" }
            }
            mBackgroundDelayHandler.postDelayed(mBackgroundTransition, BACKGROUND_DELAY)
        }
    }

    private fun appBecameBackground() {
        if (!ClearTextPrefRepo.dismissedTalkHeads && PrefRepo.completedOnboarding && PrefRepo.talkHeads) {
            application.startService(FloatingRogerService.start(application))
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

}
