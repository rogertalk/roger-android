package com.rogertalk.roger.ui.screens.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.rogertalk.roger.utils.extensions.isTablet
import com.rogertalk.roger.utils.log.logCrashlytics

open class BaseAppCompatActivity() : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set tablets to landscape
        if (isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
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
