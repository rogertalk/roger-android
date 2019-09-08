package com.rogertalk.roger.ui.screens.base

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.rogertalk.roger.utils.extensions.isTablet
import com.rogertalk.roger.utils.log.logCrashlytics
import org.greenrobot.eventbus.EventBus


open class EventActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set tablets to landscape
        if (isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onPause() {
        super.onPause()
        logCrashlytics { "onPause on ${this.localClassName}" }
    }

    override fun onPostResume() {
        super.onPostResume()
        logCrashlytics { "PostResume on ${this.localClassName}" }
    }
}