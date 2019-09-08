package com.rogertalk.roger.utils.android

import android.os.Build
import android.view.Display
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.utils.extensions.appController
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.windowManager

object ScreenUtils {

    /**
     * @return True if screen is on
     */
    fun screenOn(): Boolean {
        if (AndroidVersion.fromApiVal(Build.VERSION_CODES.KITKAT_WATCH, inclusive = true)) {
            // Use new APIs
            val screenState = appController().windowManager.defaultDisplay.state
            return arrayOf(Display.STATE_ON, Display.STATE_DOZE, Display.STATE_DOZE_SUSPEND).contains(screenState)
        } else {
            return appController().powerManager.isScreenOn
        }
    }

}